package nz.govt.natlib.tools.sip.generation.fairfax

import static org.hamcrest.core.Is.is
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
        List<Map<String, String>> mapsForCountryLivingList =
                fairfaxSpreadsheet.spreadsheet.mapsForColumn(FairfaxSpreadsheet.MMSID_COLUMN_NAME,
                        "9918150268002836")

        assertThat("Country Living only has one entry", mapsForCountryLivingList.size(), is(new Integer(1)))
        Map<String, String> mapsForCountryLiving = mapsForCountryLivingList.first()
        assertThat("'title_parent' is 'Country Living'", mapsForCountryLiving.get("title_parent"), is("Country Living"))
        assertThat("'MMSID' is 9918150268002836", mapsForCountryLiving.get("MMSID"), is("9918150268002836"))
        assertThat("'title_code' is 'CL4'", mapsForCountryLiving.get("title_code"), is("CL4"))
        assertThat("'edition_code' is 'ED1'", mapsForCountryLiving.get("edition_code"), is("ED1"))

        assertThat("titleParent for titleCode: CL4 editionCode: ED1 is 'Country Living'", fairfaxSpreadsheet.getTitleParentForTitleCodeEditionCode('CL4', 'ED1'),
                is('Country Living'))
        assertTrue("isMagazine is true for Country Living",
                FairfaxSpreadsheet.extractBooleanValue(mapsForCountryLiving, FairfaxSpreadsheet.IS_MAGAZINE_KEY))
    }
}
