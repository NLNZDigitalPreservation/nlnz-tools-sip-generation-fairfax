package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.Sortable
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.pdf.PdfInformationExtractor

/**
 * A {@code FairfaxFileGroup} has the same titleCode, editionCode and date. In other words, they belong to the same physical
 * publication.
 *
 * TODO It may be that a FairfaxFileGroup has the same titleCode and date and then there is some logic to figure out
 * how that relates to the different editions.
 */
@Canonical(excludes = [ 'files' ])
@Sortable(includes = ['fairfaxFileGroupKey'])
@ToString(includeNames=true, includePackage=false, excludes=[ 'files' ])
@Slf4j
class FairfaxFileGroup {
    FairfaxFileGroupKey fairfaxFileGroupKey
    Set<FairfaxFile> files = [ ]

    FairfaxFileGroup(FairfaxFileGroupKey fairfaxFileGroupKey) {
        this.fairfaxFileGroupKey = fairfaxFileGroupKey
    }

    boolean addFile(FairfaxFile fairfaxFile) {
        FairfaxFileGroupKey fairfaxFileKey = FairfaxFileGroupKey.fromFairfaxFile(fairfaxFile)
        if (this.fairfaxFileGroupKey == fairfaxFileKey) {
            this.files.add(fairfaxFile)
            return true
        } else {
            return false
        }
    }

    boolean determineParameterMap(List<Map<String, String>> candidateParameterMaps) {
        candidateParameterMaps.each { Map<String, String> parameterMap -> // could really use any or find?
            String title = parameterMap.get(FairfaxSpreadsheet.TITLE_PARENT_KEY)
            List<Tuple2> likelyMatches = [ ]
            this.files.sort().each { FairfaxFile fairfaxFile ->
                List<String> matchingLines = PdfInformationExtractor.matchText(fairfaxFile.getFile(), "${title}")
                if (matchingLines.size() > 0) {
                    likelyMatches.add([ fairfaxFile : matchingLines ])
                }
                log.info("    file=${fairfaxFile}, ")
            }
        }
    }
}
