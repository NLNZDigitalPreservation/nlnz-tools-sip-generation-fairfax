package nz.govt.natlib.tools.sip.generation.fairfax.processor.type

import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule

class ParentGroupingWithEditionProcessor {
    static List<FairfaxFile> selectAndSort(FairfaxProcessingParameters processingParameters,
                                           List<FairfaxFile> allPossibleFiles) {
        // We need to make adjustments to the processing parameters
        // currentEdition will have been set when iterating through editionDiscriminators
        boolean smartSubstitution = processingParameters.rules.contains(ProcessingRule.EditionDiscriminatorsUsingSmartSubstitute)
        if (smartSubstitution) {
            // Currently smart substitution is for something like QCM where we want to substitute, but each
            // edition discriminator has its own section code.
            // So we would have titleCode: QCM, with 3 separate editions:
            //       editionDiscriminator: ED1, section_codes: ED1
            //       editionDiscriminator: ED2, section_codes: ED2
            //       editionDiscriminator: ED3, section_codes: ED3
            // But we still want to substitute the pages in ED2 and ED3 over the ED1 pages
            // In order to do that, we find the FIRST edition discriminator and set the editionDiscriminators to the
            // FIRST edition discriminator and the current one
            String currentEdition = processingParameters.editionDiscriminators.first()
            List<String> sectionCodesMatchingEditionCode = sectionCodesMatchingEditionCode(currentEdition,
                    allPossibleFiles)
            if (!sectionCodesMatchingEditionCode.isEmpty()) {
                String firstEdition = sectionCodesMatchingEditionCode.first()
                if (firstEdition != currentEdition) {
                    List<String> oldDiscriminators = processingParameters.editionDiscriminators
                    processingParameters.editionDiscriminators = [ firstEdition ]
                    processingParameters.editionDiscriminators.addAll(oldDiscriminators)
                    Collections.replaceAll(processingParameters.sectionCodes, currentEdition, firstEdition)
                }
                processingParameters.currentEdition = currentEdition
            }
        }

        return FairfaxFile.filterSubstituteAndSort(allPossibleFiles, processingParameters)
    }

    static List<String> sectionCodesMatchingEditionCode(String editionCode, List<FairfaxFile> allPossibleFiles) {
        List<String> allPossibleSectionCodes = allPossibleFiles.collect { FairfaxFile fairfaxFile ->
            fairfaxFile.sectionCode
        }.unique()
        // remove the last character, matchEdition would turn ED1 --> ED
        String matchEdition = editionCode.substring(0, editionCode.length() - 1)
        List<String> matchingCodes = allPossibleSectionCodes.findAll { String sectionCode ->
            matchEdition == sectionCode.substring(0, sectionCode.length() - 1)
        }
        return matchingCodes.sort()
    }
}
