package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.Sortable
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.SipFileWrapperFactory
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import org.apache.commons.collections4.CollectionUtils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher

@Canonical
@Sortable(includes = ['titleCode', 'sectionCode', 'dateYear', 'dateMonthOfYear', 'dateDayOfMonth', 'sequenceLetter',
        'sequenceNumber', 'qualifier' ])
@ToString(includeNames=true, includePackage=false, excludes=[ ])
@EqualsAndHashCode(excludes = [ 'file', 'filename', 'qualifier', 'sequenceNumberString', 'validForProcessing', 'validPdf' ])
@Slf4j
class FairfaxFile {
    // Note that the titleCode appears to be, in some cases 4 characters long (eg. JAZZTAB), but for most cases it is 3.
    // The populate() method attempts to correct any issues with the titleCode/sectionCode grouping.
    // Note that the pdf extension can be upper or lower case (and we handle the mixed case as well
    static String REGEX_PATTERN = "(?<titleCode>[a-zA-Z0-9]{3,4})(?<sectionCode>[a-zA-Z0-9]{2,3})-(?<date>\\d{8})-" +
            "(?<sequenceLetter>[A-Za-z]{0,2})(?<sequenceNumber>\\d{1,4})(?<qualifier>.*?)\\.[pP]{1}[dD]{1}[fF]{1}"
    static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

    File file
    String filename
    String titleCode
    String sectionCode
    Integer dateYear
    Integer dateMonthOfYear
    Integer dateDayOfMonth
    LocalDate date
    String sequenceLetter
    String sequenceNumberString
    Integer sequenceNumber
    String qualifier
    boolean validForProcessing
    boolean validPdf

    static List<FairfaxFile> differences(List<FairfaxFile> list1, List<FairfaxFile> list2) {
        List<FairfaxFile> list1MinusList2 = CollectionUtils.subtract(list1, list2)
        List<FairfaxFile> list2MinusList1 = CollectionUtils.subtract(list2, list1)
        List<FairfaxFile> differences = [ ]
        differences.addAll(list1MinusList2)
        differences.addAll(list2MinusList1)

        return differences
    }

    static Set<String> allSectionCodes(List<FairfaxFile> files) {
        Set<String> sectionCodes = [ ]
        files.each { FairfaxFile fairfaxFile ->
            if (fairfaxFile.sectionCode != null && !fairfaxFile.sectionCode.isEmpty()) {
                sectionCodes.add(fairfaxFile.sectionCode)
            }
        }
        return sectionCodes
    }

    // The assumption here is that the list of files is only different in sectionCode, sequenceLetter, sequenceNumber
    // and qualifier.
    // Except in the case of a substitution, where the original sectionCode and the substitute sectionCode are treated
    // as if they are the same.
    static List<FairfaxFile> sortWithSameTitleCodeAndDate(List<FairfaxFile> files,
                                                          FairfaxProcessingParameters processingParameters) {
        // FIRST: Order by sectionCode as per processingParameters
        Map<String, List<FairfaxFile>> filesBySection = [ : ]
        processingParameters.sectionCodes.each { String sectionCode ->
            List<FairfaxFile> sectionFiles = [ ]
            filesBySection.put(sectionCode, sectionFiles)
            files.each { FairfaxFile fairfaxFile ->
                if (sectionCode == fairfaxFile.sectionCode ||
                        processingParameters.matchesCurrentSection(sectionCode, fairfaxFile.sectionCode)) {
                    sectionFiles.add(fairfaxFile)
                }
            }
        }
        // NEXT: Sort each sectionCode by numberAndAlpha
        boolean alphaBeforeNumeric = processingParameters.processingOptions.contains(ProcessingOption.AlphaBeforeNumericSequencing)
        processingParameters.sectionCodes.each { String sectionCode ->
            List<FairfaxFile> sectionFiles = filesBySection.get(sectionCode)
            sectionFiles = sortNumericAndAlpha(sectionFiles, alphaBeforeNumeric)
            filesBySection.put(sectionCode, sectionFiles)
        }
        List<FairfaxFile> sorted = [ ]
        processingParameters.sectionCodes.each { String sectionCode ->
            sorted.addAll(filesBySection.get(sectionCode))
        }
        if (sorted.size() != files.size()) {
            log.warn("Not all sorted files exist in final list, differences=${differences(sorted, files)}")
        }
        return sorted
    }

    static FairfaxFile substituteFor(String sourceSectionCode, String replacementSectionCode, FairfaxFile fairfaxFile,
                                     List<FairfaxFile> possibleFiles) {
        if (fairfaxFile.sectionCode == sourceSectionCode) {
            FairfaxFile replacementFile = possibleFiles.find { FairfaxFile candidateFile ->
                if (candidateFile.sectionCode == replacementSectionCode) {
                    candidateFile.canSubstituteFor(fairfaxFile)
                } else {
                    false
                }
            }
            return replacementFile == null ? fairfaxFile : replacementFile
        } else if (fairfaxFile.sectionCode == replacementSectionCode) {
            // We add it if the substitution does not exist (in other words, the substitute doesn't map to the source
            FairfaxFile replacementFile = possibleFiles.find { FairfaxFile candidateFile ->
                if (candidateFile.sectionCode == sourceSectionCode) {
                    candidateFile.canSubstituteFor(fairfaxFile)
                } else {
                    false
                }
            }
            return replacementFile == null ? fairfaxFile : null
        } else {
            return fairfaxFile
        }
    }

    static List<FairfaxFile> substituteAllFor(String sourceSectionCode, String replacementSectionCode,
                                              List<String> allSectionCodes, List<FairfaxFile> possibleFiles) {
        List<FairfaxFile> substituted = []
        List<String> otherSectionCodes = allSectionCodes.findAll { String sectionCode ->
            sectionCode != sourceSectionCode && sectionCode != replacementSectionCode
        }
        possibleFiles.each { FairfaxFile fairfaxFile ->
            if (!otherSectionCodes.contains(fairfaxFile.sectionCode)) {
                FairfaxFile replacementFile = substituteFor(sourceSectionCode, replacementSectionCode, fairfaxFile,
                        possibleFiles)
                if (replacementFile != null) {
                    substituted.add(replacementFile)
                }
            }
        }
        return substituted
    }

    static boolean hasSubstitutions(String replacementSectionCode, List<FairfaxFile> possibleFiles) {
        return possibleFiles.any { FairfaxFile fairfaxFile ->
            fairfaxFile.sectionCode == replacementSectionCode
        }
    }

    static List<FairfaxFile> filterAllFor(List<String> sectionCodes, List<FairfaxFile> possibleFiles) {
        List<FairfaxFile> filtered = possibleFiles.findAll { FairfaxFile fairfaxFile ->
            sectionCodes.contains(fairfaxFile.sectionCode)
        }
        if (possibleFiles.size() != filtered.size()) {
            log.warn("Not all filtered files exist in final list, differences=${differences(possibleFiles, filtered)}")
        }
        return filtered
    }

    static List<FairfaxFile> filterSubstituteAndSort(FairfaxProcessingParameters processingParameters,
                                                     List<FairfaxFile> allPossibleFiles) {
        List<FairfaxFile> filteredSubstitutedAndSorted
        if (processingParameters.currentEdition != null && !processingParameters.editionDiscriminators.isEmpty()) {
            // First we filter so we only have the files we want to process
            List<FairfaxFile> filtered = filterAllFor(processingParameters.validSectionCodes(), allPossibleFiles)
            // Then we do the substitutions
            // Substitutions happen if the FIRST editionDiscriminator has a substitution with the same date/sequenceLetter/sequenceNumber
            String firstDiscriminatorCode = processingParameters.editionDiscriminators.first()
            boolean hasSubstitutions = hasSubstitutions(processingParameters.currentEdition, filtered)
            if (hasSubstitutions) {
                List<FairfaxFile> substituted = substituteAllFor(firstDiscriminatorCode,
                        processingParameters.currentEdition, processingParameters.editionDiscriminators, filtered)
                // Then we sort so the ordering is correct
                filteredSubstitutedAndSorted = sortWithSameTitleCodeAndDate(substituted, processingParameters)
            } else {
                // If there are no substitutions (including the first for itself) then there is nothing to process
                filteredSubstitutedAndSorted = [ ]
            }
        } else {
            filteredSubstitutedAndSorted =  sortWithSameTitleCodeAndDate(allPossibleFiles, processingParameters)
        }
        return filteredSubstitutedAndSorted
    }

    // The assumption is that all these files share the same: title_code, section_code and date
    static List<FairfaxFile> sortNumericAndAlpha(List<FairfaxFile> files, boolean alphaBeforeNumeric = false) {
        List<FairfaxFile> sorted = files.sort() { FairfaxFile file1, FairfaxFile file2 ->
            // TODO Not taking into account sectionCode (or date, for that matter)
            if (alphaBeforeNumeric) {
                if (file1.sequenceLetter.isEmpty()) {
                    if (file2.sequenceLetter.isEmpty()) {
                        // file1 and file2 are numeric
                        file1.sequenceNumber <=> file2.sequenceNumber
                    } else {
                        // file1 is numeric-only, file2 is alpha-numeric
                        +1
                    }
                } else {
                    if (file2.sequenceLetter.isEmpty()) {
                        // file1 is alpha-numeric, file2 is numeric
                        -1
                    } else {
                        // file1 and file2 are alpha-numeric
                        file1.sequenceLetter <=> file2.sequenceLetter ?: file1.sequenceNumber <=> file2.sequenceNumber
                    }
                }
            } else {
                if (file1.sequenceLetter.isEmpty()) {
                    if (file2.sequenceLetter.isEmpty()) {
                        // file1 and file2 are numeric
                        file1.sequenceNumber <=> file2.sequenceNumber
                    } else {
                        // file1 is numeric-only, file2 is alpha-numeric
                        -1
                    }
                } else {
                    if (file2.sequenceLetter.isEmpty()) {
                        // file1 is alpha-numeric, file2 is numeric
                        +1
                    } else {
                        // file1 and file2 are alpha-numeric
                        file1.sequenceLetter <=> file2.sequenceLetter ?: file1.sequenceNumber <=> file2.sequenceNumber
                    }
                }
            }
        }
        return sorted
    }

    FairfaxFile(File file) {
        this.file = file
        populate()
    }

    Sip.FileWrapper generateFileWrapper() {
        return SipFileWrapperFactory.generate(this.file)
    }

    private populate() {
        this.filename = file.getName()
        // TODO Maybe the pattern comes from a resource or properties file?
        Matcher matcher = filename =~ /${REGEX_PATTERN}/
        if (matcher.matches()) {
            this.titleCode = matcher.group('titleCode')
            this.sectionCode = matcher.group('sectionCode')
            // In some situations the titleCode will take too many characters
            if ((this.titleCode.length() == 4) && (this.sectionCode.length() == 2)) {
                this.sectionCode = "${this.titleCode.substring(3, 4)}${this.sectionCode}"
                this.titleCode = this.titleCode.substring(0, 3)
             }
            String dateString = matcher.group('date')
            this.date = LocalDate.parse(dateString, LOCAL_DATE_TIME_FORMATTER)
            this.dateYear = date.year
            this.dateMonthOfYear = date.monthValue
            this.dateDayOfMonth = date.dayOfMonth
            this.sequenceLetter = matcher.group('sequenceLetter')
            this.sequenceNumberString = matcher.group('sequenceNumber')
            this.sequenceNumber = Integer.parseInt(sequenceNumberString)
            this.qualifier = matcher.group('qualifier')
        }
    }

    boolean isValidName() {
        return this.file != null && this.titleCode != null && this.sectionCode != null && this.dateYear != null &&
                this.dateMonthOfYear != null && this.dateDayOfMonth != null && this.sequenceNumber != null
    }

    boolean matches(FairfaxFile fairfaxFile) {
        return this.matches(fairfaxFile.titleCode, fairfaxFile.sectionCode, fairfaxFile.dateYear, fairfaxFile.dateMonthOfYear,
            fairfaxFile.dateDayOfMonth)
    }

    boolean matches(String comparisonTitleCode, String comparisonSectionCode, Integer comparisonYear,
                    Integer comparisonMonthOfYear, Integer comparisonDayOfMonth) {
        if (isValidName()) {
            if (this.titleCode == comparisonTitleCode && this.sectionCode == comparisonSectionCode) {
                return (this.dateYear == comparisonYear && this.dateMonthOfYear == comparisonMonthOfYear &&
                        this.dateDayOfMonth == comparisonDayOfMonth)
            } else {
                return false
            }
        } else {
            return false
        }
    }

    boolean matchesWithSequence(FairfaxFile fairfaxFile) {
        if (matches(fairfaxFile)) {
            return (this.sequenceLetter == fairfaxFile.sequenceLetter) &&
                    (this.sequenceNumber == fairfaxFile.sequenceNumber)
        } else {
            return false
        }
    }

    boolean comesDirectlyAfter(FairfaxFile fairfaxFile) {
        // this file's sequence number must be greater (or a letter starting at 1)
        int sequenceDifference = this.sequenceNumber - fairfaxFile.sequenceNumber
        if (this.sequenceLetter == fairfaxFile.sequenceLetter) {
            return sequenceDifference == 1
        } else {
            return this.sequenceNumber == 1
        }
    }

    // Substitutions can happen if the file has the same date, sequence letter and sequence number
    boolean canSubstituteFor(FairfaxFile fairfaxFile) {
        return this.date == fairfaxFile.date && this.sequenceLetter == fairfaxFile.sequenceLetter &&
                this.sequenceNumber == fairfaxFile.sequenceNumber
    }
}
