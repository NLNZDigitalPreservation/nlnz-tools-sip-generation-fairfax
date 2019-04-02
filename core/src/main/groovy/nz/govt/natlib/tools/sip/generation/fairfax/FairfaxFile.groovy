package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.Sortable
import groovy.transform.ToString
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.SipFileWrapperFactory

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher

@Canonical
@Sortable(includes = ['titleCode', 'editionCode', 'dateYear', 'dateMonthOfYear', 'dateDayOfMonth', 'sequenceLetter',
        'sequenceNumber', 'qualifier' ])
@ToString(includeNames=true, includePackage=false, excludes=[ ])
@EqualsAndHashCode(excludes = [ 'file', 'filename', 'qualifier', 'sequenceNumberString', 'validForProcessing', 'validPdf' ])
class FairfaxFile {
    static String REGEX_PATTERN = "(?<titleCode>[a-zA-Z0-9]{3})(?<editionCode>[a-zA-Z0-9]{3})-(?<date>\\d{8})-" +
            "(?<sequenceLetter>[A-Za-z]{0,2})(?<sequenceNumber>\\d{1,4})(?<qualifier>.*?)\\.pdf"
    static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

    File file
    String filename
    String titleCode
    String editionCode
    Integer dateYear
    Integer dateMonthOfYear
    Integer dateDayOfMonth
    String sequenceLetter
    String sequenceNumberString
    Integer sequenceNumber
    String qualifier
    boolean validForProcessing
    boolean validPdf

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
            this.editionCode = matcher.group('editionCode')
            String dateString = matcher.group('date')
            LocalDate localDate = LocalDate.parse(dateString, LOCAL_DATE_TIME_FORMATTER)
            this.dateYear = localDate.year
            this.dateMonthOfYear = localDate.monthValue
            this.dateDayOfMonth = localDate.dayOfMonth
            this.sequenceLetter = matcher.group('sequenceLetter')
            this.sequenceNumberString = matcher.group('sequenceNumber')
            this.sequenceNumber = Integer.parseInt(sequenceNumberString)
            this.qualifier = matcher.group('qualifier')
        }
    }

    boolean isValidName() {
        return this.file != null && this.titleCode != null && this.editionCode != null && this.dateYear != null &&
                this.dateMonthOfYear != null && this.dateDayOfMonth != null && this.sequenceNumber != null
    }

    boolean matches(FairfaxFile fairfaxFile) {
        return this.matches(fairfaxFile.titleCode, fairfaxFile.editionCode, fairfaxFile.dateYear, fairfaxFile.dateMonthOfYear,
            fairfaxFile.dateDayOfMonth)
    }

    boolean matches(String comparisonTitleCode, String comparisonEditionCode, Integer comparisonYear,
                    Integer comparisonMonthOfYear, Integer comparisonDayOfMonth) {
        if (isValidName()) {
            if (this.titleCode == comparisonTitleCode && this.editionCode == comparisonEditionCode) {
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

    LocalDate dateAsLocalDate() {
        return new LocalDate(this.dateYear, this.dateMonthOfYear, this.dateDayOfMonth)
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
}
