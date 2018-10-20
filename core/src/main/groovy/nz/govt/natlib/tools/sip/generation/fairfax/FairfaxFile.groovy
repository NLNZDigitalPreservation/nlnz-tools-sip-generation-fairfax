package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.Sortable

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.regex.Matcher

@Canonical
@Sortable(includes = ['prefix', 'date', 'sequenceLetter', 'sequenceNumber'])
class FairfaxFile {
    static String REGEX_PATTERN = "(?<prefix>\\w+)-(?<date>\\d{8})-(?<sequenceLetter>[A-Za-z]{0,2})(?<sequenceNumber>\\d{1,4})\\.pdf"
    static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd")

    File file
    String filename
    String prefix
    Date date
    String sequenceLetter
    String sequenceNumberString
    Integer sequenceNumber

    FairfaxFile(File file) {
        this.file = file
        populate()
    }

    private populate() {
        this.filename = file.getName()
        // TODO Maybe the pattern comes from a resource or properties file?
        Matcher matcher = filename =~ /${REGEX_PATTERN}/
        if (matcher.matches()) {
            this.prefix = matcher.group('prefix')
            String dateString = matcher.group('date')
            this.date = DATE_FORMAT.parse(dateString)
            this.sequenceLetter = matcher.group('sequenceLetter')
            this.sequenceNumberString = matcher.group('sequenceNumber')
            this.sequenceNumber = Integer.parseInt(sequenceNumberString)
        }
    }

    boolean isValid() {
        return this.file != null && this.prefix != null && this.date != null && this.sequenceNumber != null
    }

    boolean matches(FairfaxFile fairfaxFile) {
        return this.matches(fairfaxFile.prefix, fairfaxFile.date)
    }

    boolean matches(String prefix, Date comparisonDate) {
        if (isValid()) {
            if (this.prefix == prefix) {
                Calendar calendar1 = Calendar.getInstance()
                Calendar calendar2 = Calendar.getInstance()
                calendar1.setTime(this.date)
                calendar2.setTime(comparisonDate)
                return (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)) &&
                        (calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR))
            } else {
                return false
            }
        } else {
            return false
        }
    }
}
