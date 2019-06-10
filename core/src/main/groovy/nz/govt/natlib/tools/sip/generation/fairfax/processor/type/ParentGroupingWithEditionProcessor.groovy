package nz.govt.natlib.tools.sip.generation.fairfax.processor.type

import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters

class ParentGroupingWithEditionProcessor {
    static List<FairfaxFile> selectAndSort(FairfaxProcessingParameters processingParameters,
                                           List<FairfaxFile> allPossibleFiles) {
        return FairfaxFile.filterSubstituteAndSort(allPossibleFiles, processingParameters)
    }

}
