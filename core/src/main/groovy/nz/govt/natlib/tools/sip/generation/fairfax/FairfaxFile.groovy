package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.Sortable
import groovy.transform.ToString
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.SipFileWrapperFactory

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher

@Canonical
@Sortable(includes = ['name', 'edition', 'dateYear', 'dateMonthOfYear', 'dateDayOfMonth', 'sequenceLetter', 'sequenceNumber'])
@ToString(includeNames=true, includePackage=false, excludes=[ ])
class FairfaxFile {
    static String REGEX_PATTERN = "(?<name>[a-zA-Z0-9]{3})(?<edition>[a-zA-Z0-9]{3})-(?<date>\\d{8})-(?<sequenceLetter>[A-Za-z]{0,2})(?<sequenceNumber>\\d{1,4})\\.pdf"
    static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

    File file
    String filename
    String name
    String edition
    Integer dateYear
    Integer dateMonthOfYear
    Integer dateDayOfMonth
    String sequenceLetter
    String sequenceNumberString
    Integer sequenceNumber

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
            this.name = matcher.group('name')
            this.edition = matcher.group('edition')
            String dateString = matcher.group('date')
            LocalDate localDate = LocalDate.parse(dateString, LOCAL_DATE_TIME_FORMATTER)
            this.dateYear = localDate.year
            this.dateMonthOfYear = localDate.monthValue
            this.dateDayOfMonth = localDate.dayOfMonth
            this.sequenceLetter = matcher.group('sequenceLetter')
            this.sequenceNumberString = matcher.group('sequenceNumber')
            this.sequenceNumber = Integer.parseInt(sequenceNumberString)
        }
    }

    boolean isValid() {
        return this.file != null && this.name != null && this.edition != null && this.dateYear != null &&
                this.dateMonthOfYear != null && this.dateDayOfMonth != null && this.sequenceNumber != null
    }

    boolean matches(FairfaxFile fairfaxFile) {
        return this.matches(fairfaxFile.name, fairfaxFile.edition, fairfaxFile.dateYear, fairfaxFile.dateMonthOfYear,
            fairfaxFile.dateDayOfMonth)
    }

    boolean matches(String comparisonName, String comparisonEdition, Integer comparisonYear,
                    Integer comparisonMonthOfYear, Integer comparisonDayOfMonth) {
        if (isValid()) {
            if (this.name == comparisonName && this.edition == comparisonEdition) {
                return (this.dateYear == comparisonYear && this.dateMonthOfYear == comparisonMonthOfYear &&
                        this.dateDayOfMonth == comparisonDayOfMonth)
            } else {
                return false
            }
        } else {
            return false
        }
    }
}
