package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.pdf.PdfInformationExtractor
import nz.govt.natlib.tools.sip.state.SipProcessingFailure
import nz.govt.natlib.tools.sip.state.SipProcessingFailureReason
import nz.govt.natlib.tools.sip.state.SipProcessingFailureReasonType
import nz.govt.natlib.tools.sip.state.SipProcessingState

@Slf4j
class FairfaxFileGroupMatcher {
    SipProcessingState sipProcessingState

    FairfaxFileGroupMatcher(SipProcessingState sipProcessingState) {
        this.sipProcessingState = sipProcessingState
    }

    List<FairfaxFileGroupMatch> findMatches(FairfaxFileGroup fairfaxFileGroup, FairfaxSpreadsheet spreadsheet) {
        List<FairfaxFileGroupMatch> fairfaxFileGroupMatches = [ ]
        List<Map<String, String>> candidateParameterMaps = spreadsheet.matchingParameterMaps(
                fairfaxFileGroup.fairfaxFileGroupKey.name, fairfaxFileGroup.fairfaxFileGroupKey.edition)

        candidateParameterMaps.each { Map<String, String> parameterMap -> // could really use any or find?

            String title = parameterMap.get("Title")

            List<FairfaxFileMatch> matches = [ ]
            // We do every file so we can tell the relative strength of the match
            fairfaxFileGroup.files.each { FairfaxFile fairfaxFile ->
                if (fairfaxFile.file.length() == 0) {
                    SipProcessingFailureReason failureReason = new SipProcessingFailureReason(
                            SipProcessingFailureReasonType.FILE_OF_LENGTH_ZERO,  null,
                            fairfaxFile.file.getCanonicalPath())
                    sipProcessingState.addFailure(SipProcessingFailure.createWithReason(failureReason))
                } else {
                    // we wrap the regex pattern with '(?i:<pattern>)' to make it case insensitive
                    List<String> matchingLines = PdfInformationExtractor.matchText(fairfaxFile.getFile(), "(?i:.*?${title}.*?)")
                    FairfaxFileMatch fairfaxFileMatch = new FairfaxFileMatch(fairfaxFile: fairfaxFile,
                            stringMatches: matchingLines)
                    matches.add(fairfaxFileMatch)
                }
            }
            Sip sip = SipFactory.fromMap(parameterMap)
            sip.year = fairfaxFileGroup.fairfaxFileGroupKey.dateYear
            sip.month = fairfaxFileGroup.fairfaxFileGroupKey.dateMonthOfYear
            sip.dayOfMonth = fairfaxFileGroup.fairfaxFileGroupKey.dateDayOfMonth
            FairfaxFileGroupMatch fairfaxFileGroupMatch = new FairfaxFileGroupMatch(sip, matches)

            fairfaxFileGroupMatches.add(fairfaxFileGroupMatch)
        }

        return fairfaxFileGroupMatches
    }

    FairfaxFileGroupMatch mostLikelyMatch(FairfaxFileGroup fairfaxFileGroup, FairfaxSpreadsheet fairfaxSpreadsheet,
                                                 boolean allowZeroRatio = true) {
        List<FairfaxFileGroupMatch> fairfaxFileGroupMatches = findMatches(fairfaxFileGroup, fairfaxSpreadsheet)
        log.info("Possible matches for fairfaxFileGroup=${fairfaxFileGroup}, matches=${fairfaxFileGroupMatches}")
        if (fairfaxFileGroupMatches.size() == 0) {
            log.error("Unable to find matching parameters for fairfaxFileGroup=${fairfaxFileGroup}")
            return null
        }

        FairfaxFileGroupMatch bestMatch = fairfaxFileGroupMatches.sort().first()
        if (bestMatch == null || (!allowZeroRatio && bestMatch.relativeMatchRatio == 0.0F)) {
            log.error("Unable to find matching parameters for fairfaxFileGroup=${fairfaxFileGroup}")
            return null
        }
        log.info("Found best match for fairfaxFileGroup=${fairfaxFileGroup}, bestMatch=${bestMatch}")

        return bestMatch
    }
}
