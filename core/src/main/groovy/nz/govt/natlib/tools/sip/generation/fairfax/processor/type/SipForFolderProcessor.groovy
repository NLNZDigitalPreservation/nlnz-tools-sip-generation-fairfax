package nz.govt.natlib.tools.sip.generation.fairfax.processor.type

import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet

/**
 * Does processing operations specific to the processing type
 * {@link nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType#CreateSipForFolder}
 */
class SipForFolderProcessor {
    static List<FairfaxFile> selectAndSort(FairfaxProcessingParameters processingParameters,
                                           List<FairfaxFile> allPossibleFiles) {
        List<String> sectionCodes = FairfaxFile.allSectionCodes(allPossibleFiles).toList()
        processingParameters.sectionCodes = sectionCodes
        if (processingParameters.spreadsheetRow.isEmpty()) {
            processingParameters.spreadsheetRow = FairfaxSpreadsheet.BLANK_ROW
        }
        List<FairfaxFile> selectedAndSorted = FairfaxFile.sortWithSameTitleCodeAndDate(allPossibleFiles,
                processingParameters)

        return selectedAndSorted
    }
}
