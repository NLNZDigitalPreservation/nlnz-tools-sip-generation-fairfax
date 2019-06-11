package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.IEEntityType
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType
import nz.govt.natlib.tools.sip.generation.parameters.Spreadsheet
import nz.govt.natlib.tools.sip.state.SipProcessingException
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReason
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType

@Log4j2
class FairfaxSpreadsheet {
    // Note that the CSV 'standard' generally only allows 1 character as a separator
    static String DEFAULT_FIELD_SEPARATOR = "|"
    static String MMSID_COLUMN_NAME = "MMSID"
    static String PROCESSING_TYPE_KEY = "processing_type"
    static String PROCESSING_RULES_KEY = "processing_rule"
    static String PROCESSING_OPTIONS_KEY = "processing_option"
    static String TITLE_CODE_KEY = "title_code"
    static String SECTION_CODE_KEY = "section_code"
    static String TITLE_PARENT_KEY = "title_parent"
    static String EDITION_DISCRIMINATOR_KEY = "edition_discriminator"
    static String IS_MAGAZINE_KEY = "Magazine"

    Spreadsheet spreadsheet
    Map<FairfaxFileTitleEditionKey, List<Map<String, String>>> titleCodeSectionCodeToRowsMap = [ : ]
    Map<String, List<Map<String, String>>> titleCodeToRowsMap = [ : ]
    Set<FairfaxFileTitleEditionKey> allTitleCodeSectionCodeKeys = [ ]
    Set<String> allTitleCodeKeys = [ ]

    static final Map<String, String> BLANK_ROW = [
        "MMSID": "UNKNOWN_MMSID",
        "title_parent": "UNKNOWN_TITLE",
        "processing_type": "NO_PROCESSING_TYPE_GIVEN",
        "processing_rule": "",
        "processing_option": "",
        "publication_key": "",
        "title_code": "NO_TITLE_CODE_GIVEN",
        "edition_discriminator": "",
        "section_code": "",
        "Access": "200",
        "Magazine": "1"
    ].asImmutable()

    static Sip getBlankSip() {
        Sip sip = new Sip(title: 'UNKNOWN_TITLE', ieEntityType: IEEntityType.UNKNOWN,
                objectIdentifierType: 'UNKNOWN_OBJECT_IDENTIFIER_TYPE',
                objectIdentifierValue: 'UNKNOWN_OBJECT_IDENTIFIER_VALUE', policyId: 'UNKNOWN_POLICY_ID',
                preservationType: 'UNKNOWN_PRESERVATION_TYPE', usageType: 'UNKNOWN_USAGE_TYPE',
                digitalOriginal: true, revisionNumber: 1,
                year: 2038, month: 12, dayOfMonth: 31)

    }

    /**
     * Load and return the FairfaxSpreadsheet from default resources.
     */
    static FairfaxSpreadsheet defaultInstance() {
        // TODO Either a root class to get resourceAsStream, move the json file to the same package or do ../../.. etc
        // or do what SipTestHelper does.
        InputStream defaultSpreadsheetInputStream = FairfaxSpreadsheet.getResourceAsStream("default-fairfax-import-parameters.json")
        Spreadsheet spreadsheet = Spreadsheet.fromJson(Spreadsheet.GENERATE_ID_VALUE, defaultSpreadsheetInputStream.text, true, true)

        return new FairfaxSpreadsheet(spreadsheet)
    }

    static boolean extractBooleanValue(Map<String, String> spreadsheetRow, String columnId) {
        String columnValue = spreadsheetRow.get(columnId)
        if (columnValue == null) {
            // No value is false
            columnValue = "0"
        } else {
            columnValue = columnValue.strip()
        }
        return "1" == columnValue || "y".equalsIgnoreCase(columnValue) || "yes".equalsIgnoreCase(columnValue)
    }

    FairfaxSpreadsheet(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet
        index()
    }

    List<Map<String, String>> matchingParameterMaps(String titleCode, String sectionCode) {
        List<Map<String, String>> matchingMaps = [ ]
        spreadsheet.rows.each { Map<String, String> rowMap ->
            if (titleCode == rowMap.get(TITLE_CODE_KEY) && sectionCode == rowMap.get(SECTION_CODE_KEY)) {
                matchingMaps.add(rowMap)
            }
        }
        return matchingMaps
    }

    List<Map<String, String>> matchingProcessingTypeParameterMaps(String processingType, String titleCode) {
        List<Map<String, String>> matchingMaps = [ ]
        spreadsheet.rows.each { Map<String, String> rowMap ->
            if (processingType == rowMap.get(PROCESSING_TYPE_KEY) &&
                    titleCode == rowMap.get(TITLE_CODE_KEY)) {
                matchingMaps.add(rowMap)
            }
        }
        return matchingMaps
    }

    List<String> getTitleParentsForTitleCodeSectionCode(String titleCode, String sectionCode) {
        List<String> titles = [ ]
        matchingParameterMaps(titleCode, sectionCode).each { Map<String, String> rowMap ->
            titles.add(rowMap.get(TITLE_PARENT_KEY))
        }

        return titles
    }

    String getTitleParentForTitleCodeSectionCode(String titleCode, String sectionCode) {
        List<String> titles = getTitleParentsForTitleCodeSectionCode(titleCode, sectionCode)
        if (titles.size() == 1) {
            return titles.first()
        } else if (titles.size() > 1) {
            log.info("Found multiple titles for titleCode=${titleCode}, sectionCode=${sectionCode}, titles=${titles}. Using first title.")
            return titles.first()
        } else {
            return "NO-TITLE-GIVEN"
        }
    }

    void index() {
        spreadsheet.rows.each { Map<String, String> rowMap ->
            String titleCode = rowMap.get(TITLE_CODE_KEY)
            String sectionCode = rowMap.get(SECTION_CODE_KEY)
            FairfaxFileTitleEditionKey fairfaxFileTitleEditionKey = new FairfaxFileTitleEditionKey(
                    titleCode: titleCode, sectionCode: sectionCode)
            if (titleCodeSectionCodeToRowsMap.containsKey(fairfaxFileTitleEditionKey)) {
                List<Map<String, String>> rowsForNameEdition = titleCodeSectionCodeToRowsMap.get(fairfaxFileTitleEditionKey)
                rowsForNameEdition.add(rowMap)
            } else {
                titleCodeSectionCodeToRowsMap.put(fairfaxFileTitleEditionKey, [rowMap ])
            }
            allTitleCodeSectionCodeKeys.add(fairfaxFileTitleEditionKey)
            if (titleCodeToRowsMap.containsKey(titleCode)) {
                List<Map<String, String>> rowsForName = titleCodeToRowsMap.get(titleCode)
                rowsForName.add(rowMap)
            } else {
                titleCodeToRowsMap.put(titleCode, [rowMap ])
            }
            allTitleCodeKeys.add(titleCode)
        }
    }

    List<SipProcessingException> validate() {
        List<SipProcessingException> validationErrors = [ ]
        spreadsheet.rows.each { Map<String, String> rowMap ->
            String processingTypeString = rowMap.get(PROCESSING_TYPE_KEY)
            ProcessingType processingType = ProcessingType.forFieldValue(processingTypeString)
            if (processingType == null && processingTypeString.strip().isEmpty()) {
                String message = "No acceptable value for ProcessingType=${processingTypeString}, row=${rowMap}".toString()
                SipProcessingException exception = new SipProcessingExceptionReason(
                        SipProcessingExceptionReasonType.INVALID_PARAMETERS, null, message)
                validationErrors.add(exception)
            }
        }
        return validationErrors
    }

}
