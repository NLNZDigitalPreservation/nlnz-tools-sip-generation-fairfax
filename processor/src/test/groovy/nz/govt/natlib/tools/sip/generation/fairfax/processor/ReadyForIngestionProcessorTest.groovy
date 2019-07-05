package nz.govt.natlib.tools.sip.generation.fairfax.processor

import org.junit.Test

import java.time.LocalDate

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat

class ReadyForIngestionProcessorTest {

    @Test
    void correctlyAssemblesSipAndFilesFolderName() {
        validateAssemblySipAndFilesFolderName("20190524", "ABC", "this-type", null,
                "20190524_ABC_this-type")
        validateAssemblySipAndFilesFolderName("20190524", "ABC", "this-type", "my-identifier",
                "20190524_ABC_this-type_my-identifier")
        validateAssemblySipAndFilesFolderName("NO-DATE-NECESSARY", "1234", "this-type", "my-identifier",
                "NO-DATE-NECESSARY_1234_this-type_my-identifier")
    }

    void validateAssemblySipAndFilesFolderName(String dateString, String titleCode, String type, String identifier,
                                               String expectedValue) {
        String actualValue = ReadyForIngestionProcessor.assembleSipAndFilesFolderName(dateString, titleCode, type, identifier)

        assertThat("Correctly assembleSipAndFilesFolderName", actualValue, is(expectedValue))
    }

    @Test
    void correctlyParsesSipAndFilesFolderNameForTitleCodeAndDate() {
        validateParsingSipAndFilesFolderName("20190524_ABC_this-type", "ABC", new LocalDate(2019, 5, 24))
        validateParsingSipAndFilesFolderName("20190524_ABC_this-type_my-identifier", "ABC", new LocalDate(2019, 5, 24))
        validateParsingSipAndFilesFolderName("BAD-DATE_JUNK", "JUNK", null)
        validateParsingSipAndFilesFolderName("2019-05-24_SOME_WHERE", "SOME", null)
        validateParsingSipAndFilesFolderName("JUNK", null, null)
        validateParsingSipAndFilesFolderName("_JUNK", "JUNK", null)
    }

    void validateParsingSipAndFilesFolderName(String sipAndFilesFolderName, String expectedTitleCode, LocalDate expectedDate) {
        Tuple2<String, LocalDate> actualTitleCodeAndDate = ReadyForIngestionProcessor.parseFolderNameForTitleCodeAndDate(sipAndFilesFolderName)
        Tuple2<String, LocalDate> expectedTitleCodeAndDate = new Tuple2<>(expectedTitleCode, expectedDate)

        assertThat("Correctly parses titleCodeAndDate", actualTitleCodeAndDate, is(expectedTitleCodeAndDate))
    }
}
