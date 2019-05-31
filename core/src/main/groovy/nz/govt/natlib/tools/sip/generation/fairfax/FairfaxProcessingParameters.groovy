package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.AutoClone
import groovy.transform.Canonical
import groovy.transform.ToString
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType
import nz.govt.natlib.tools.sip.state.SipProcessingException
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReason
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType

import java.time.LocalDate

@Canonical
@ToString(includeNames=true, includePackage=false, excludes=[ 'spreadsheetRow' ])
@AutoClone(excludes = [ 'currentEdition' ])
class FairfaxProcessingParameters {
    boolean valid = true
    String titleCode
    ProcessingType processingType
    List<ProcessingRule> processingRules = [ ]
    List<ProcessingOption> processingOptions = [ ]
    LocalDate processingDate
    Map<String, String> spreadsheetRow = [ : ]
    List<String> editionCodes = [ ]
    List<String> editionDiscriminators = [ ]
    boolean isMagazine = false
    String currentEdition
    List<SipProcessingException> sipProcessingExceptions = [ ]

    static FairfaxProcessingParameters build(String titleCode, String processingTypeString, LocalDate processingDate,
                                             FairfaxSpreadsheet spreadsheet) {
        ProcessingType processingType = ProcessingType.forFieldValue(processingTypeString)
        List<Map<String, String>> matchingRows = spreadsheet.matchingProcessingTypeParameterMaps(processingTypeString, titleCode)
        if (matchingRows.size() > 1) {
            // TODO Might want to construct using SipProcessingExceptionReason|Type
            throw new SipProcessingException("Multiple spreadsheet rows for processingType=${processingTypeString} and titleCode=${titleCode}. Unable to generate parameters")
        } else if (matchingRows.size() == 0) {
            if (ProcessingType.CreateSipForFolder == processingType) {
                Map<String, String> BLANK_ROW = [ : ]
                return new FairfaxProcessingParameters(titleCode: titleCode, processingType: processingType,
                        processingDate: processingDate, spreadsheetRow: BLANK_ROW)
            }
        } else if (processingType == null) {
            SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                    SipProcessingExceptionReasonType.INVALID_PARAMETERS, null,
                    "ProcessingType must be set.")
            return new FairfaxProcessingParameters(valid: false,
                    sipProcessingExceptions: [ SipProcessingException.createWithReason(exceptionReason) ])
        } else {
            Map<String, String> matchingRow = matchingRows.first()
            String rules = matchingRow.get(FairfaxSpreadsheet.PROCESSING_RULES_KEY)
            String options = matchingRow.get(FairfaxSpreadsheet.PROCESSING_OPTIONS_KEY)
            // TODO Throw exception if processingType is null?
            return new FairfaxProcessingParameters(titleCode: titleCode, processingType: processingType,
                    processingRules: ProcessingRule.extract(rules, ","),
                    processingOptions: ProcessingOption.extract(options, ","),
                    processingDate: processingDate, spreadsheetRow: matchingRow,
                    editionCodes: extractSeparatedValues(matchingRow, FairfaxSpreadsheet.EDITION_CODE_KEY),
                    editionDiscriminators: extractSeparatedValues(matchingRow, FairfaxSpreadsheet.EDITION_DISCRIMINATOR_KEY),
                    isMagazine: FairfaxSpreadsheet.extractBooleanValue(matchingRow, FairfaxSpreadsheet.IS_MAGAZINE_KEY))
        }
    }

    static List<String> extractSeparatedValues(Map<String, String> spreadsheetRow, String columnKey,
                                               String regex = "\\+|,|-") {
        List<String> extractedValues = spreadsheetRow.get(columnKey).split(regex).collect { String value ->
                    value.strip()
                }

        return extractedValues
    }

    String getTitleParent() {
        String titleParent = spreadsheetRow.get(FairfaxSpreadsheet.TITLE_PARENT_KEY)
        if (titleParent == null || titleParent.strip().isEmpty()) {
            titleParent = "NO-TITLE-GIVEN"
        }
        return titleParent
    }

    boolean hasCurrentEdition() {
        return (currentEdition != null && !currentEdition.isEmpty())
    }

    boolean matchesCurrentEdition(String matchEditionCode, String fileEditionCode) {
        if (this.hasCurrentEdition()) {
            return this.editionDiscriminators.first() == matchEditionCode &&
                    (editionDiscriminators.first() == fileEditionCode ||
                            this.currentEdition == fileEditionCode)
        } else {
            return false
        }
    }

    List<String> validEditionCodes() {
        if (!hasCurrentEdition()) {
            return this.editionCodes.clone()
        }
        List<String> validEditionCodes = [ ]
        if (this.currentEdition != this.editionDiscriminators.first()) {
            validEditionCodes.add(this.editionDiscriminators.first())
        }
        validEditionCodes.add(this.currentEdition)
        this.editionCodes.each { String editionCode ->
            if (editionCode != currentEdition && editionCode != editionDiscriminators.first()) {
                validEditionCodes.add(editionCode)
            }
        }
        return validEditionCodes
    }
}
