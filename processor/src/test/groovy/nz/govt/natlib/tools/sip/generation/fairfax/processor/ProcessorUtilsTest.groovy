package nz.govt.natlib.tools.sip.generation.fairfax.processor

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat

import org.junit.Test

class ProcessorUtilsTest {

    @Test
    void convertsFilenamesProperly() {
        checkAndConvertFilename("/this/is/a/path", "_this_is_a_path")
        checkAndConvertFilename("\\this\\is\\a\\path", "_this_is_a_path")
        checkAndConvertFilename("dollars/\$are\$/removed", "dollars_-are-_removed")
        checkAndConvertFilename("/asterisks*have-/dashes*instead", "_asterisks-have-_dashes-instead")
        checkAndConvertFilename("no more spaces either ", "no-more-spaces-either-")
    }

    static void checkAndConvertFilename(String filePath, String expectedConversion) {
        assertThat("Correctly coverts=${filePath} to ${expectedConversion}",
                ProcessorUtils.fileNameAsSafeString(filePath), is(expectedConversion))
    }
}
