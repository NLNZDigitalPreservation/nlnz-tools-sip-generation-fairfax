package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.Sortable
import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.state.SipProcessingException

@Canonical
@Sortable(includes = ['titleCode', 'editionCode', 'dateYear', 'dateMonthOfYear', 'dateDayOfMonth'])
@Slf4j
class FairfaxFileGroupKey {

    String titleCode
    String editionCode
    Integer dateYear
    Integer dateMonthOfYear
    Integer dateDayOfMonth

    static FairfaxFileGroupKey fromFairfaxFile(FairfaxFile fairfaxFile) {
        return new FairfaxFileGroupKey(fairfaxFile.titleCode, fairfaxFile.editionCode, fairfaxFile.dateYear,
            fairfaxFile.dateMonthOfYear, fairfaxFile.dateDayOfMonth)
    }

    FairfaxFileGroupKey(String titleCode, String editionCode, Integer dateYear, Integer dateMonthOfYear,
                        Integer dateDayOfMonth) throws SipProcessingException {
        this.titleCode = titleCode
        this.editionCode = editionCode
        this.dateYear = dateYear
        this.dateMonthOfYear = dateMonthOfYear
        this.dateDayOfMonth = dateDayOfMonth
        if (!isValid()) {
            String errorMessage = "Cannot create FairfaxFileGroupKey, all parameters must be non-null:" +
                    " titleCode=${titleCode}, editionCode=${editionCode}, dateYear=${dateYear}," +
                    " dateMonthOfYear=${dateMonthOfYear}, dateDayOfMonth=${dateDayOfMonth}"
            log.error(errorMessage)
            throw new SipProcessingException(errorMessage)
        }
    }

    boolean isValid() {
        return this.titleCode != null && this.editionCode != null && this.dateYear != null && this.dateMonthOfYear != null &&
                this.dateDayOfMonth != null
    }
}
