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
import nz.govt.natlib.tools.sip.state.SipProcessingState
import org.apache.commons.lang3.StringUtils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Canonical
@ToString(includeNames=true, includePackage=false, excludes=[ 'spreadsheetRow', 'sipProcessingState' ])
@AutoClone(excludes = [ 'currentEdition' ])
class FairfaxProcessingParameters {
    static DateTimeFormatter READABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    boolean valid = true
    String titleCode
    ProcessingType processingType
    List<ProcessingRule> processingRules = [ ]
    List<ProcessingOption> processingOptions = [ ]
    LocalDate processingDate
    Map<String, String> spreadsheetRow = [ : ]
    List<String> sectionCodes = [ ]
    List<String> editionDiscriminators = [ ]
    boolean isMagazine = false
    String currentEdition
    SipProcessingState sipProcessingState = new SipProcessingState()

    static FairfaxProcessingParameters build(String titleCode, ProcessingType processingType, LocalDate processingDate,
                                             FairfaxSpreadsheet spreadsheet) {
        List<Map<String, String>> matchingRows = spreadsheet.matchingProcessingTypeParameterMaps(
                processingType.fieldValue, titleCode)
        if (matchingRows.size() > 1) {
            String message = "Multiple spreadsheet rows for processingType=${processingType.fieldValue} and titleCode=${titleCode}. Unable to generate parameters".toString()
            SipProcessingException exception = new SipProcessingExceptionReason(
                    SipProcessingExceptionReasonType.INVALID_PARAMETERS, null, message)
            throw exception
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
            SipProcessingState replacementSipProcessingState = new SipProcessingState()
            replacementSipProcessingState.exceptions = [ SipProcessingException.createWithReason(exceptionReason) ]
            return new FairfaxProcessingParameters(valid: false,
                    sipProcessingState: replacementSipProcessingState)
        } else {
            Map<String, String> matchingRow = matchingRows.first()
            String rules = matchingRow.get(FairfaxSpreadsheet.PROCESSING_RULES_KEY)
            String options = matchingRow.get(FairfaxSpreadsheet.PROCESSING_OPTIONS_KEY)
            // TODO Throw exception if processingType is null?
            return new FairfaxProcessingParameters(titleCode: titleCode, processingType: processingType,
                    processingRules: ProcessingRule.extract(rules, ",", processingType.defaultRules),
                    processingOptions: ProcessingOption.extract(options, ",", processingType.defaultOptions),
                    processingDate: processingDate, spreadsheetRow: matchingRow,
                    sectionCodes: extractSeparatedValues(matchingRow, FairfaxSpreadsheet.SECTION_CODE_KEY),
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

    void overrideProcessingRules(List<ProcessingRule> overrides) {
        processingRules = ProcessingRule.mergeOverrides(processingRules, overrides)
    }

    void overrideProcessingOptions(List<ProcessingOption> overrides) {
        processingOptions = ProcessingOption.mergeOverrides(processingOptions, overrides)
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

    boolean matchesCurrentSection(String matchSectionCode, String fileSectionCode) {
        if (this.hasCurrentEdition()) {
            return this.editionDiscriminators.first() == matchSectionCode &&
                    (editionDiscriminators.first() == fileSectionCode ||
                            this.currentEdition == fileSectionCode)
        } else {
            return false
        }
    }

    List<String> validSectionCodes() {
        if (!hasCurrentEdition()) {
            return this.sectionCodes.clone()
        }
        List<String> validSectionCodes = [ ]
        if (this.currentEdition != this.editionDiscriminators.first()) {
            validSectionCodes.add(this.editionDiscriminators.first())
        }
        validSectionCodes.add(this.currentEdition)
        this.sectionCodes.each { String sectionCode ->
            if (sectionCode != currentEdition && sectionCode != editionDiscriminators.first()) {
                validSectionCodes.add(sectionCode)
            }
        }
        return validSectionCodes
    }

    String processingDifferentiator() {
        String baseDifferentiator = "${titleCode}_${processingDate.format(READABLE_DATE_FORMAT)}_${processingType.fieldValue}"
        if (currentEdition == null) {
            return baseDifferentiator
        } else {
            return "${baseDifferentiator}_${currentEdition}"
        }
    }

    String detailedDisplay(int offset = 0, boolean includeSipProcessingState = false) {
        String initialOffset = StringUtils.repeat(' ', offset)
        StringBuilder stringBuilder = new StringBuilder(initialOffset)
        stringBuilder.append("${this.getClass().getName()}:")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    processingType=${processingType.fieldValue}")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    processingDate=${processingDate.format(READABLE_DATE_FORMAT)}")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    processingRules=${processingRules}")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    processingOptions=${processingOptions}")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    valid=${valid}")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    titleCode=${titleCode}")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    sectionCodes=${sectionCodes}")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    editionDiscriminators=${editionDiscriminators}")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    currentEdition=${currentEdition}")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    isMagazine=${isMagazine}")
        stringBuilder.append(System.lineSeparator())
        stringBuilder.append("${initialOffset}    spreadsheetRow=${spreadsheetRow}")
        stringBuilder.append(System.lineSeparator())
        if (includeSipProcessingState) {
            stringBuilder.append(this.sipProcessingState.toString())
        }

        return stringBuilder.toString()
    }

}
