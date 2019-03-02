package nz.govt.natlib.tools.sip.generation.fairfax

import nz.govt.natlib.tools.sip.generation.parameters.Spreadsheet

class FairfaxSpreadsheet {
    // Note that the CSV 'standard' generally only allows 1 character as a separator
    static String DEFAULT_FIELD_SEPARATOR = "|"
    static String ID_COLUMN_NAME = "MMSID"
    static String NAME_KEY = "names_dict"
    static String EDITION_KEY = "edition_dict"

    Spreadsheet spreadsheet
    Map<FairfaxFileNameEditionKey, List<Map<String, String>>> nameEditionToRowsMap = [ : ]
    Map<String, List<Map<String, String>>> nameToRowsMap = [ : ]
    Set<FairfaxFileNameEditionKey> allNameEditionKeys = [ ]
    Set<String> allNameKeys = [ ]

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

    List<Map<String, String>> matchingParameterMaps(String name, String edition) {
        List<Map<String, String>> matchingMaps = [ ]
        spreadsheet.rows.each { Map<String, String> rowMap ->
            if (name == rowMap.get(NAME_KEY) && edition == rowMap.get(EDITION_KEY)) {
                matchingMaps.add(rowMap)
            }
        }
        return matchingMaps
    }

    void index() {
        spreadsheet.rows.each { Map<String, String> rowMap ->
            String name = rowMap.get(NAME_KEY)
            String edition = rowMap.get(EDITION_KEY)
            FairfaxFileNameEditionKey fairfaxFileNameEditionKey = new FairfaxFileNameEditionKey(
                    name: name, edition: edition)
            if (nameEditionToRowsMap.containsKey(fairfaxFileNameEditionKey)) {
                List<Map<String, String>> rowsForNameEdition = nameEditionToRowsMap.get(fairfaxFileNameEditionKey)
                rowsForNameEdition.add(rowMap)
            } else {
                nameEditionToRowsMap.put(fairfaxFileNameEditionKey, [ rowMap ])
            }
            allNameEditionKeys.add(fairfaxFileNameEditionKey)
            if (nameToRowsMap.containsKey(name)) {
                List<Map<String, String>> rowsForName = nameToRowsMap.get(name)
                rowsForName.add(rowMap)
            } else {
                nameToRowsMap.put(name, [ rowMap ])
            }
            allNameKeys.add(name)
        }
    }


}
