package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.Sortable
import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.state.SipProcessingException

@Canonical
@Sortable(includes = ['name', 'edition', 'dateYear', 'dateMonthOfYear', 'dateDayOfMonth'])
@Slf4j
class FairfaxFileGroupKey {

    String name
    String edition
    Integer dateYear
    Integer dateMonthOfYear
    Integer dateDayOfMonth

    static FairfaxFileGroupKey fromFairfaxFile(FairfaxFile fairfaxFile) {
        return new FairfaxFileGroupKey(fairfaxFile.name, fairfaxFile.edition, fairfaxFile.dateYear,
            fairfaxFile.dateMonthOfYear, fairfaxFile.dateDayOfMonth)
    }

    FairfaxFileGroupKey(String name, String edition, Integer dateYear, Integer dateMonthOfYear, Integer dateDayOfMonth)
            throws SipProcessingException {
        this.name = name
        this.edition = edition
        this.dateYear = dateYear
        this.dateMonthOfYear = dateMonthOfYear
        this.dateDayOfMonth = dateDayOfMonth
        if (!isValid()) {
            String errorMessage = "Cannot create FairfaxFileGroupKey, all parameters must be non-null:" +
                    " name=${name}, edition=${edition}, dateYear=${dateYear}, dateMonthOfYear=${dateMonthOfYear}, dateDayOfMonth=${dateDayOfMonth}"
            log.error(errorMessage)
            throw new SipProcessingException(errorMessage)
        }
    }

    boolean isValid() {
        return this.name != null && this.edition != null && this.dateYear != null && this.dateMonthOfYear != null &&
                this.dateDayOfMonth != null
    }
}
