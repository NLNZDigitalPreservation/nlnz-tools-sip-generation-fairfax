package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.generation.parameters.Spreadsheet

@Slf4j
class FairfaxSpreadsheet {
    // Note that the CSV 'standard' generally only allows 1 character as a separator
    static String DEFAULT_FIELD_SEPARATOR = "|"
    static String ID_COLUMN_NAME = "MMSID"
    static String TITLE_CODE_KEY = "title_code"
    static String EDITION_CODE_KEY = "edition_code"
    static String TITLE_PARENT_KEY = "title_parent"
    static String IS_MAGAZINE_KEY = "Magazine"

    Spreadsheet spreadsheet
    Map<FairfaxFileTitleEditionKey, List<Map<String, String>>> titleCodeEditionCodeToRowsMap = [ : ]
    Map<String, List<Map<String, String>>> titleCodeToRowsMap = [ : ]
    Set<FairfaxFileTitleEditionKey> allTitleCodeEditionCodeKeys = [ ]
    Set<String> allTitleCodeKeys = [ ]

    /**
     * Load and return the FairfaxSpreadsheet from default resources.
     */
    static FairfaxSpreadsheet defaultInstance() {
        // TODO Either a root class to get resourceAsStream, move the json file to the same package or do ../../.. etc
        // or do what SipTestHelper does.
        InputStream defaultSpreadsheetInputStream = FairfaxSpreadsheet.getResourceAsStream("default-fairfax-import-parameters.json")
        Spreadsheet spreadsheet = Spreadsheet.fromJson(ID_COLUMN_NAME, defaultSpreadsheetInputStream.text, true, true)

        return new FairfaxSpreadsheet(spreadsheet)
    }

    FairfaxSpreadsheet(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet
        index()
    }

    boolean isMagazineForTitleCodeEditionCode(String titleCode, String editionCode) {
        List<String> isMagazines = [ ]
        matchingParameterMaps(titleCode, editionCode).each { Map<String, String> rowMap ->
            isMagazines.add(rowMap.get(IS_MAGAZINE_KEY))
        }

        if (isMagazines.size() == 1) {
            return "1" == isMagazines.first()
        } else if (isMagazines.size() > 1) {
            log.info("Found multiple rows for titleCode=${titleCode}, editionCode=${editionCode}, isMagazines=${isMagazines}. Using first row.")
            return "1" == isMagazines.first()
        } else {
            log.info("Found NO rows for titleCode=${titleCode}, editionCode=${editionCode}, isMagazines=${isMagazines}. Defaulting to false.")
            return false
        }
    }

    boolean isNewspaperForNameEdition(String titleCode, String editionCode) {
        return !isMagazineForTitleCodeEditionCode(titleCode, editionCode)
    }

    List<Map<String, String>> matchingParameterMaps(String titleCode, String editionCode) {
        List<Map<String, String>> matchingMaps = [ ]
        spreadsheet.rows.each { Map<String, String> rowMap ->
            if (titleCode == rowMap.get(TITLE_CODE_KEY) && editionCode == rowMap.get(EDITION_CODE_KEY)) {
                matchingMaps.add(rowMap)
            }
        }
        return matchingMaps
    }

    List<String> getTitleParentsForTitleCodeEditionCode(String titleCode, String editionCode) {
        List<String> titles = [ ]
        matchingParameterMaps(titleCode, editionCode).each { Map<String, String> rowMap ->
            titles.add(rowMap.get(TITLE_PARENT_KEY))
        }

        return titles
    }

    String getTitleParentForTitleCodeEditionCode(String titleCode, String editionCode) {
        List<String> titles = getTitleParentsForTitleCodeEditionCode(titleCode, editionCode)
        if (titles.size() == 1) {
            return titles.first()
        } else if (titles.size() > 1) {
            log.info("Found multiple titles for titleCode=${titleCode}, editionCode=${editionCode}, titles=${titles}. Using first title.")
            return titles.first()
        } else {
            return "NO-TITLE-GIVEN"
        }
    }

    void index() {
        spreadsheet.rows.each { Map<String, String> rowMap ->
            String titleCode = rowMap.get(TITLE_CODE_KEY)
            String editionCode = rowMap.get(EDITION_CODE_KEY)
            FairfaxFileTitleEditionKey fairfaxFileTitleEditionKey = new FairfaxFileTitleEditionKey(
                    titleCode: titleCode, editionCode: editionCode)
            if (titleCodeEditionCodeToRowsMap.containsKey(fairfaxFileTitleEditionKey)) {
                List<Map<String, String>> rowsForNameEdition = titleCodeEditionCodeToRowsMap.get(fairfaxFileTitleEditionKey)
                rowsForNameEdition.add(rowMap)
            } else {
                titleCodeEditionCodeToRowsMap.put(fairfaxFileTitleEditionKey, [rowMap ])
            }
            allTitleCodeEditionCodeKeys.add(fairfaxFileTitleEditionKey)
            if (titleCodeToRowsMap.containsKey(titleCode)) {
                List<Map<String, String>> rowsForName = titleCodeToRowsMap.get(titleCode)
                rowsForName.add(rowMap)
            } else {
                titleCodeToRowsMap.put(titleCode, [rowMap ])
            }
            allTitleCodeKeys.add(titleCode)
        }
    }


}
