package nz.govt.natlib.tools.sip.generation.fairfax

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

/**
 * Tests the {@link FairfaxSpreadsheet}.
 */
@RunWith(MockitoJUnitRunner.class)
class FairfaxSpreadsheetTest {

    @Test
    void loadsTheDefaultSpreadsheetCorrectly() {
        FairfaxSpreadsheet fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

        assertTrue("Spreadsheet is valid", fairfaxSpreadsheet.spreadsheet.isValid(false, false))
        List<Map<String, String>> mapsForBayChronicleList =
                fairfaxSpreadsheet.spreadsheet.mapsForColumn(FairfaxSpreadsheet.MMSID_COLUMN_NAME,
                        "9917952983502836")

        assertThat("The Bay Chronicle only has one entry", mapsForBayChronicleList.size(), is(new Integer(1)))
        Map<String, String> mapsForBayChronicle = mapsForBayChronicleList.first()
        assertThat("'title_parent' is 'The Bay Chronicle'", mapsForBayChronicle.get("title_parent"), is("The Bay Chronicle"))
        assertThat("'MMSID' is 9917952983502836", mapsForBayChronicle.get("MMSID"), is("9917952983502836"))
        assertThat("'title_code' is 'BAC'", mapsForBayChronicle.get("title_code"), is("BAC"))
        assertThat("'section_codes' is 'ED1'", mapsForBayChronicle.get("section_codes"), is("ED1"))

        assertThat("titleParent for titleCode: BAC sectionCode: ED1 is 'The Bay Chronicle'", fairfaxSpreadsheet.getTitleParentForTitleCodeSectionCode('BAC', 'ED1'),
                is('The Bay Chronicle'))
        assertFalse("isMagazine is false forThe Bay Chronicle",
                FairfaxSpreadsheet.extractBooleanValue(mapsForBayChronicle, FairfaxSpreadsheet.IS_MAGAZINE_KEY))
    }
}
