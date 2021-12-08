package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.Sortable
import groovy.transform.ToString
import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.SipFileWrapperFactory
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule
import nz.govt.natlib.tools.sip.pdf.PdfDimensionFinder
import nz.govt.natlib.tools.sip.utils.PathUtils
import org.apache.commons.collections4.CollectionUtils

import java.awt.Point
import java.awt.geom.Point2D
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher

/**
 * Encapsulates the characteristics of Fairfax file. Includes methods for operating on lists of such files.
 */
@Canonical
@Sortable(includes = ['titleCode', 'sectionCode', 'dateYear', 'dateMonthOfYear', 'dateDayOfMonth', 'sequenceLetter',
        'sequenceNumber', 'qualifier' ])
@ToString(includeNames = true, includePackage = false, includes = [ 'filename', 'file' ])
@EqualsAndHashCode(includes = [ 'titleCode', 'sectionCode', 'date', 'sequenceLetter', 'sequenceNumber' ])
@Log4j2
class FairfaxFile {
    // Note that the titleCode appears to be, in some cases 4 characters long (eg. JAZZTAB), but for most cases it is 3.
    // The populate() method attempts to correct any issues with the titleCode/sectionCode grouping.
    // Note that the pdf extension can be upper or lower case (and we handle the mixed case as well
    static final String PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_GROUPING_PATTERN = "(?<titleCode>[a-zA-Z0-9]{3,4})" +
            "(?<sectionCode>[a-zA-Z0-9]{2,3})-(?<date>\\d{8})-(?<sequenceLetter>[A-Za-z]{0,2})" +
            "(?<sequenceNumber>\\d{1,4})(?<qualifier>.*?)\\.[pP]{1}[dD]{1}[fF]{1}"
    static final String PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_PATTERN = '\\w{5,7}-\\d{8}-\\w{1,4}.*?\\.[pP]{1}[dD]{1}[fF]{1}'
    static final String PDF_FILE_WITH_TITLE_SECTION_DATE_PATTERN = '\\w{5,7}-\\d{8}-.*?\\.[pP]{1}[dD]{1}[fF]{1}'
    static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    static final Point UNDIMENSIONED = new Point(-1, -1)
    static final String FOREVER_PROJECT_PREFIX = "FP"
    static final String[] PROPERTY_TITLES = ["HON", "SOP", "HOC", "HOW", "HWE", "PRB", "CHM", "HOK"]
    static final String[] LIFE_SUPPLEMENTS = ["LID", "LIP", "LIW"]

    Path file
    // This is for when the file gets replaced, such as when a zero-length pdf is replaced by another file.
    Path originalFile
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
    Point dimensionsInPoints = UNDIMENSIONED
    boolean zeroLengthFile = false

    static List<FairfaxFile> differences(List<FairfaxFile> list1, List<FairfaxFile> list2) {
        List<FairfaxFile> list1MinusList2 = CollectionUtils.subtract(list1, list2)
        List<FairfaxFile> list2MinusList1 = CollectionUtils.subtract(list2, list1)
        List<FairfaxFile> differences = [ ]
        differences.addAll(list1MinusList2)
        differences.addAll(list2MinusList1)

        return differences
    }

    static List<String> allSectionCodes(List<FairfaxFile> files) {
        List<String> sectionCodes = [ ]
        files.each { FairfaxFile fairfaxFile ->
            if (fairfaxFile.sectionCode != null && !fairfaxFile.sectionCode.isEmpty()) {
                if (!sectionCodes.contains(fairfaxFile.sectionCode)) {
                    sectionCodes.add(fairfaxFile.sectionCode)
                }
            }
        }
        return sectionCodes
    }
    
    static boolean currentSectionCodeFile(String key, FairfaxFile file, FairfaxProcessingParameters parameters) {
		if (!key.equals(file.sectionCode) && !parameters.matchesCurrentSection(key, file.sectionCode)) {
			return false
		}
		// Do not process sequence letter file as part of the section code files
		if (!parameters.sequenceLetters.isEmpty() && parameters.sequenceLetters.contains(file.sequenceLetter)) {
			return false
		}
		return true
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
                if (currentSectionCodeFile(sectionCode, fairfaxFile, processingParameters)) {
                    sectionFiles.add(fairfaxFile)
                }
            }
        }

        // Create section for sequence letter
        if (!processingParameters.sequenceLetters.isEmpty()) {
            processingParameters.sequenceLetters.each { String sequenceLetter ->
                List<FairfaxFile> sequenceLetterFiles = []
                filesBySection.put(sequenceLetter, sequenceLetterFiles)
                files.each { FairfaxFile fairfaxFile ->
                    if (sequenceLetter == fairfaxFile.sequenceLetter) {
                        sequenceLetterFiles.add(fairfaxFile)
                    }
                }
            }
        }

        // NEXT: Sort each sectionCode by numberAndAlpha
        boolean alphaBeforeNumeric = processingParameters.options.contains(ProcessingOption.AlphaBeforeNumericSequencing)
        filesBySection.keySet().each { String key ->
            List<FairfaxFile> sectionFiles = filesBySection.get(key)
            sectionFiles = sortNumericAndAlpha(sectionFiles, alphaBeforeNumeric)
            filesBySection.put(key, sectionFiles)
        }
        List<FairfaxFile> sorted = [ ]
        filesBySection.keySet().each { String key ->
            sorted.addAll(filesBySection.get(key))
        }
        if (sorted.size() != files.size()) {
            log.warn("Not all sorted files exist in final list, differences=${differences(sorted, files)}")
        }
        return sorted
    }

    static List<FairfaxFile> postMissingSequenceFiles(List<FairfaxFile> files,
                                                      FairfaxProcessingParameters processingParameters) {
        List<FairfaxFile> sorted = sortWithSameTitleCodeAndDate(files, processingParameters)
        FairfaxFile previousFile = null
        List<FairfaxFile> postMissingFiles = [ ]
        sorted.each { FairfaxFile testFile ->
            if (previousFile != null) {
                if (!testFile.canComeDirectlyAfter(previousFile, processingParameters.editionDiscriminators)) {
                    if (testFile.isAHundredsSequenceStart() &&
                            processingParameters.rules.contains(ProcessingRule.NumericStartsInHundredsNotConsideredSequenceSkips)) {
                        // We don't consider this a skip in the sequence.
                        // Note that there's a small edge case where there are hundreds of pages, such as:
                        // 397, 398, 400, 401, ... -> this would be considered okay, even though there is a page missing.
                    } else if (testFile.filename.startsWith(FOREVER_PROJECT_PREFIX) ||
                            PROPERTY_TITLES.contains(testFile.filename.substring(0,3)) ||
                            (LIFE_SUPPLEMENTS.contains(testFile.filename.substring(0,3)))) {
                        // This not a skip in sequence, these files have a different a title code to their
                        // parent_publication
                    } else {
                        postMissingFiles.add(testFile)
                    }
                }
                previousFile = testFile
            } else {
                previousFile = testFile
                if (testFile.sequenceNumber != 1) {
                    postMissingFiles.add(testFile)
                }
            }
        }
        return postMissingFiles
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

    static List<FairfaxFile> substituteAllFor(String sourceSectionCode, String replacementSectionCode, String titleCode,
                                              List<String> allSectionCodes, List<FairfaxFile> possibleFiles) {
        List<FairfaxFile> substituted = []
        List<String> otherSectionCodes = allSectionCodes.findAll { String sectionCode ->
            sectionCode != sourceSectionCode && sectionCode != replacementSectionCode
        }
        // Do not substitute Forever Project/Property/Life files unless the file has a substitute
        possibleFiles.each { FairfaxFile fairfaxFile ->
            if ( (fairfaxFile.filename.startsWith(FOREVER_PROJECT_PREFIX) ||
                    PROPERTY_TITLES.contains(fairfaxFile.filename.substring(0,3)) ||
                    (LIFE_SUPPLEMENTS.contains(fairfaxFile.filename.substring(0,3)) &&
                            fairfaxFile.filename.substring(0,3) != titleCode) )
                    && fairfaxFile.sectionCode == replacementSectionCode) {
                substituted.add(fairfaxFile)
            } else if (!otherSectionCodes.contains(fairfaxFile.sectionCode) ) {
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

    static List<FairfaxFile> filterSubstituteAndSort(List<FairfaxFile> allPossibleFiles,
                                                     FairfaxProcessingParameters processingParameters) {
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
                        processingParameters.currentEdition, processingParameters.titleCode,
                        processingParameters.editionDiscriminators, filtered)
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

    static List<FairfaxFile> fromSourceFolder(Path sourceFolder,
                                              String pattern = PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_PATTERN) {
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true

        log.info("Processing for pattern=${pattern}, sourceFolder=${sourceFolder.normalize()}")

        // Note that we only want the current directory and we don't want info messages
        List<Path> allFiles = PathUtils.findFiles(sourceFolder.normalize().toString(),
                isRegexNotGlob, matchFilenameOnly, sortFiles, pattern, null, false, true)
        List<FairfaxFile> onlyFairfaxFiles = [ ]
        allFiles.each { Path file ->
            FairfaxFile fairfaxFile = new FairfaxFile(file)
            // TODO We have no checks here for FairfaxFile validity -- the pattern supposedly selects only validly named ones.
            onlyFairfaxFiles.add(fairfaxFile)
        }
        return onlyFairfaxFiles
    }

    static List<String> uniqueSectionCodes(List<FairfaxFile> fairfaxFiles) {
        Set<String> uniqueCodes = [ ]
        fairfaxFiles.each { FairfaxFile file ->
            uniqueCodes.add(file.sectionCode)
        }
        return uniqueCodes.toList()
    }

    static List<String> asFilenames(List<FairfaxFile> files) {
        return files.collect { FairfaxFile fairfaxFile ->
            fairfaxFile.file.fileName.toString()
        }
    }

    FairfaxFile(Path file) {
        this.file = file
        populate()
    }

    Sip.FileWrapper generateFileWrapper() {
        return SipFileWrapperFactory.generate(this.file)
    }

    private populate() {
        this.filename = file.fileName.toString()
        // TODO Maybe the pattern comes from a resource or properties file?
        Matcher matcher = filename =~ /${PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_GROUPING_PATTERN}/
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

    boolean canComeDirectlyAfter(FairfaxFile fairfaxFile, List<String> editionDiscriminators = [ ]) {
        // this file's sequence number must be greater (or a letter starting at 1)
        int sequenceDifference = this.sequenceNumber - fairfaxFile.sequenceNumber
        if (this.sequenceLetter == fairfaxFile.sequenceLetter) {
            boolean sameSectionCode = this.sectionCode == fairfaxFile.sectionCode ||
                    (editionDiscriminators.contains(this.sectionCode) &&
                            editionDiscriminators.contains(fairfaxFile.sectionCode))
            if (sameSectionCode) {
                return sequenceDifference == 1
            } else {
                return this.sequenceNumber == 1
            }
        } else {
            return this.sequenceNumber == 1
        }
    }

    // Substitutions can happen if the file has the same date, sequence letter and sequence number
    boolean canSubstituteFor(FairfaxFile fairfaxFile) {
        return this.date == fairfaxFile.date && this.sequenceLetter == fairfaxFile.sequenceLetter &&
                this.sequenceNumber == fairfaxFile.sequenceNumber
    }

    boolean isDimensioned() {
        return dimensionsInPoints.x > 0 && dimensionsInPoints.y > 0
    }

    void updateDimensions(boolean whenNotDimensioned = true) {
        if ((whenNotDimensioned && !isDimensioned()) || !whenNotDimensioned) {
            this.dimensionsInPoints = PdfDimensionFinder.getDimensions(this.file, 0)
        }
    }

    boolean isSameHeightDoubleWidth(FairfaxFile otherFile) {
        updateDimensions(true)
        otherFile.updateDimensions(true)

        Point2D.Double ratio = PdfDimensionFinder.getDimensionalRatio(this.dimensionsInPoints, otherFile.dimensionsInPoints)
        boolean isSameHeightDoubleWidth = PdfDimensionFinder.isSameHeightDoubleWidth(ratio, 0.1)
        if (!isSameHeightDoubleWidth) {
            log.info("Not same height/double width dimensions1=${this.dimensionsInPoints}, dimensions2=${otherFile.dimensionsInPoints} file1=${this.file.normalize()}, file2=${otherFile.file.normalize()}")
        }
        return isSameHeightDoubleWidth
    }

    boolean isSameHeightHalfWidth(FairfaxFile otherFile) {
        updateDimensions(true)
        otherFile.updateDimensions(true)

        Point2D.Double ratio = PdfDimensionFinder.getDimensionalRatio(this.dimensionsInPoints, otherFile.dimensionsInPoints)
        boolean isSameHeightHalfWidth = PdfDimensionFinder.isSameHeightHalfWidth(ratio, 0.1)
        if (!isSameHeightHalfWidth) {
            log.info("Not same height/half width dimensions1=${this.dimensionsInPoints}, dimensions2=${otherFile.dimensionsInPoints} file1=${this.file.normalize()}, file2=${otherFile.file.normalize()}")
        }
        return isSameHeightHalfWidth
    }

    Path getOriginalFileOrFile() {
        return originalFile == null ? file : originalFile
    }

    boolean isAHundredsSequenceStart() {
        // TODO the case that we've seen is STL/SOT (Southland Times) with 401, 402 sequences
        // so we have set this at 400 so that cases like 98, 100, 101 will catch missing files.
        if (sequenceNumber < 400) {
            return false
        }
        int hundredRemainder = sequenceNumber % 100

        return hundredRemainder == 0 || hundredRemainder == 1
    }
}
