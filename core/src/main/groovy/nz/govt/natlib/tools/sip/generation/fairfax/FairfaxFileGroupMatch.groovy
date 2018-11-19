package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.Sortable
import groovy.transform.ToString
import nz.govt.natlib.tools.sip.Sip

@Canonical
@Sortable(includes = 'relativeMatchRatio')
@ToString(includeNames=true, includePackage=false, excludes=[ 'relativeMatchRatioTuple', 'absoluteMatchRatioTuple' ])
class FairfaxFileGroupMatch {
    Sip sip
    List<FairfaxFileMatch> fairfaxFileMatches
    Tuple2 relativeMatchRatioTuple
    Tuple2 absoluteMatchRatioTuple
    float relativeMatchRatio
    float absoluteMatchRatio

    FairfaxFileGroupMatch(Sip sip, List<FairfaxFileMatch> fairfaxFileMatches) {
        this.sip = sip
        this.fairfaxFileMatches = fairfaxFileMatches
        calculateRatios()
    }

    private void calculateRatios() {
        relativeMatchRatioTuple = calculateRelativeMatchRatio()
        absoluteMatchRatioTuple = calculateAbsoluteMatchRatio()

        relativeMatchRatio = relativeMatchRatioTuple.second > 0 ?
                (relativeMatchRatioTuple.first / relativeMatchRatioTuple.second) : 0
        absoluteMatchRatio = absoluteMatchRatioTuple.second > 0 ?
                (absoluteMatchRatioTuple.first / absoluteMatchRatioTuple.second) : 0
    }

    /**
     * Counts a maximum of 1 match per file.
     *
     * @return
     */
    Tuple2 calculateRelativeMatchRatio() {
        int totalFiles = fairfaxFileMatches.size()
        int totalMatches = 0
        fairfaxFileMatches.each { FairfaxFileMatch fairfaxFileMatch ->
            if (fairfaxFileMatch.stringMatches.size() > 0) {
                totalMatches += 1
            }
        }
        return new Tuple2(new Integer(totalMatches), new Integer(totalFiles))
    }

    /**
     * Counts each match, which means a file that matches multiple times
     * will have each match counted.
     *
     * @return the absolute match ratio
     */
    Tuple2 calculateAbsoluteMatchRatio() {
        int totalFiles = fairfaxFileMatches.size()
        int allMatches = 0
        fairfaxFileMatches.each { FairfaxFileMatch fairfaxFileMatch ->
            allMatches += fairfaxFileMatch.stringMatches.size()

        }
        return new Tuple2(new Integer(allMatches), new Integer(totalFiles))
    }
}
