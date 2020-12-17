package nz.govt.natlib.tools.sip.generation.fairfax.processor.type

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters

/**
 * Does processing operations specific to the processing type
 * {@link nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType#ParentGroupingWithEdition}
 */
@Log4j2
class SupplementGroupingProcessor {
    static List<FairfaxFile> selectAndSort(FairfaxProcessingParameters processingParameters,
                                           List<FairfaxFile> allPossibleFiles) {
        List<FairfaxFile> fairfaxFiles = FairfaxFile.filterSubstituteAndSort(allPossibleFiles, processingParameters)

        List<String> allFileSectionCodes = FairfaxFile.allSectionCodes(allPossibleFiles)
        boolean hasAtLeastOneMissingSectionCode = processingParameters.sectionCodes.any { String sectionCode ->
            !allFileSectionCodes.contains(sectionCode)
        }
        if (hasAtLeastOneMissingSectionCode) {
            processingParameters.skip = true
            log.info("files sectionCodes=${allFileSectionCodes} does not contain all required section " +
                    "codes=${processingParameters.sectionCodes}, skipping processing for " +
                    "processingParameters=${processingParameters}")
        }

        // Return files if there are no sequence letters to process
        if (processingParameters.sequenceLetters.size() == 0) {
            return fairfaxFiles
        }

        // Check if there are any files with sequence letter
        // If so, do not skip and only process sequence letter files
        List<FairfaxFile> sequenceLetterFiles = [ ]
        fairfaxFiles.each { FairfaxFile  fairfaxFile ->
            if (processingParameters.sequenceLetters.contains(fairfaxFile.sequenceLetter)) {
                sequenceLetterFiles.add(fairfaxFile)
            }
        }

        if (sequenceLetterFiles.size() > 0) {
            fairfaxFiles = sequenceLetterFiles
            processingParameters.skip = false
        } else {
            processingParameters.skip = true
            log.info("no sequence letter ${processingParameters.sequenceLetters} files, " +
                    "skipping processing for processingParameters=${processingParameters}")
        }

        return fairfaxFiles
    }
}
