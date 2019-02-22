package nz.govt.natlib.tools.sip.generation.fairfax

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.when

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
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
        List<Map<String, String>> mapsForCountryLivingList = fairfaxSpreadsheet.spreadsheet.mapsForId("9918150268002836")

        assertThat("Country Living only has one entry", mapsForCountryLivingList.size(), is(new Integer(1)))
        Map<String, String> mapsForCountryLiving = mapsForCountryLivingList.first()
        assertThat("'Title' is 'Country living'", mapsForCountryLiving.get("Title"), is("Country living"))
        assertThat("'MMSID' is 9918150268002836", mapsForCountryLiving.get("MMSID"), is("9918150268002836"))
        assertThat("'names_dict' is 'CL4'", mapsForCountryLiving.get("names_dict"), is("CL4"))
        assertThat("'edition_dict' is 'ED1'", mapsForCountryLiving.get("edition_dict"), is("ED1"))
    }
}
