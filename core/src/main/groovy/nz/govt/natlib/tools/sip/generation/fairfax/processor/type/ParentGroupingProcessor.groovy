package nz.govt.natlib.tools.sip.generation.fairfax.processor.type

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule

/**
 * Does processing operations specific to the processing type
 * {@link nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType#ParentGrouping}
 */
@Log4j2
class ParentGroupingProcessor {
    static List<FairfaxFile> selectAndSort(FairfaxProcessingParameters processingParameters,
                                           List<FairfaxFile> allPossibleFiles) {
        List<FairfaxFile> fairfaxFiles = FairfaxFile.filterSubstituteAndSort(allPossibleFiles, processingParameters)

        if (processingParameters.rules.contains(ProcessingRule.FirstSectionCodeRequiredForMatch)) {
            List<String> sectionCodes = FairfaxFile.allSectionCodes(fairfaxFiles)
            if (fairfaxFiles.size() > 0) {
                String firstSpreadsheetSectionCode = processingParameters.sectionCodes.first()
                if (firstSpreadsheetSectionCode != sectionCodes.first()) {
                    processingParameters.skip = true
                    log.info("firstSpreadsheetSectionCode=${firstSpreadsheetSectionCode} " +
                            "NOT equal to first file section code=${sectionCodes.first()}, " +
                            "skipping processing for processingParameters=${processingParameters}")
                }
            }
        }

        return fairfaxFiles
    }

}
