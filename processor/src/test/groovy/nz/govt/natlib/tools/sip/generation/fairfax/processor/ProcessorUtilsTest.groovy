package nz.govt.natlib.tools.sip.generation.fairfax.processor

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

import org.junit.Test

class ProcessorUtilsTest {
    static final File ZERO_SEGMENT_FILE = new File("")
    static final File ZERO_SEGMENT_FILE_SLASH = new File("/")
    static final File ONE_SEGMENT_FILE = new File("filename.txt")
    static final File ONE_SEGMENT_FILE_SLASH = new File("/filename.txt")
    static final File TWO_SEGMENT_FILE = new File("parent1/filename.txt")
    static final File TWO_SEGMENT_FILE_SLASH = new File("/parent1/filename.txt")
    static final File THREE_SEGMENT_FILE = new File("parent2/parent1/filename.txt")
    static final File THREE_SEGMENT_FILE_SLASH = new File("/parent2/parent1/filename.txt")
    static final File FOUR_SEGMENT_FILE = new File("parent3/parent2/parent1/filename.txt")
    static final File FOUR_SEGMENT_FILE_SLASH = new File("/parent3/parent2/parent1/filename.txt")
    static final File FIVE_SEGMENT_FILE = new File("parent4/parent3/parent2/parent1/filename.txt")
    static final File FIVE_SEGMENT_FILE_SLASH = new File("/parent4/parent3/parent2/parent1/filename.txt")

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

    @Test
    void verifyThatFilePathAsSafeStringWorksForDirectoryOnly() {
        List<ProcessorOption> testOptions = [ ProcessorOption.ShowDirectoryOnly ]

        String expected = "filename.txt"
        // Can't really test ZERO_SEGMENT_FILE, since it will automatically have its parent because of how File is constructed.
        // For ZERO_SEGMENT_FILE_SLASH, it's treated as the root folder, which means it is a blank
        assertThat("Full=${ZERO_SEGMENT_FILE_SLASH} with option=${testOptions} is=''",
                ProcessorUtils.filePathAsSafeString(ZERO_SEGMENT_FILE_SLASH, testOptions), is(""))
        assertThat("Full=${ONE_SEGMENT_FILE} with option=${testOptions} is=filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions), is("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is(expected))
        assertThat("Full=${TWO_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${TWO_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE_SLASH, testOptions), is(expected))
        assertThat("Full=${THREE_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${THREE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE_SLASH, testOptions), is(expected))
        assertThat("Full=${FOUR_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${FOUR_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE_SLASH, testOptions), is(expected))
        assertThat("Full=${FIVE_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${FIVE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE_SLASH, testOptions), is(expected))
    }

    @Test
    void verifyThatFilePathAsSafeStringWorksForDirectoryAndOneParent() {
        List<ProcessorOption> testOptions = [ ProcessorOption.ShowDirectoryAndOneParent ]

        String expected = "parent1_filename.txt"
        assertTrue("Full=${ONE_SEGMENT_FILE} with option=${testOptions} ends with=filename.txt, actual=${ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions).endsWith("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=_filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is("_filename.txt"))
        assertThat("Full=${TWO_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${TWO_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE_SLASH, testOptions), is(expected))
        assertThat("Full=${THREE_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${THREE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE_SLASH, testOptions), is(expected))
        assertThat("Full=${FOUR_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${FOUR_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE_SLASH, testOptions), is(expected))
        assertThat("Full=${FIVE_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${FIVE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE_SLASH, testOptions), is(expected))
    }

    @Test
    void verifyThatFilePathAsSafeStringWorksForDirectoryAndTwoParents() {
        List<ProcessorOption> testOptions = [ ProcessorOption.ShowDirectoryAndTwoParents ]

        String expected = "parent2_parent1_filename.txt"
        assertTrue("Full=${ONE_SEGMENT_FILE} with option=${testOptions} ends with=filename.txt, actual=${ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions).endsWith("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=_filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is("_filename.txt"))
        assertTrue("Full=${TWO_SEGMENT_FILE} with option=${testOptions} ends with=parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions).endsWith("parent1_filename.txt"))
        assertThat("Full=${TWO_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent1_filename.txt",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE_SLASH, testOptions), is("_parent1_filename.txt"))
        assertThat("Full=${THREE_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${THREE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE_SLASH, testOptions), is(expected))
        assertThat("Full=${FOUR_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${FOUR_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE_SLASH, testOptions), is(expected))
        assertThat("Full=${FIVE_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${FIVE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE_SLASH, testOptions), is(expected))
    }

    @Test
    void verifyThatFilePathAsSafeStringWorksForDirectoryAndThreeParents() {
        List<ProcessorOption> testOptions = [ ProcessorOption.ShowDirectoryAndThreeParents ]

        String expected = "parent3_parent2_parent1_filename.txt"
        assertTrue("Full=${ONE_SEGMENT_FILE} with option=${testOptions} ends with=filename.txt, actual=${ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions).endsWith("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=_filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is("_filename.txt"))
        assertTrue("Full=${TWO_SEGMENT_FILE} with option=${testOptions} ends with=parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions).endsWith("parent1_filename.txt"))
        assertThat("Full=${TWO_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent1_filename.txt",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE_SLASH, testOptions), is("_parent1_filename.txt"))
        assertTrue("Full=${THREE_SEGMENT_FILE} with option=${testOptions} ends with=parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions).endsWith("parent2_parent1_filename.txt"))
        assertThat("Full=${THREE_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE_SLASH, testOptions), is("_parent2_parent1_filename.txt"))
        assertThat("Full=${FOUR_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${FOUR_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE_SLASH, testOptions), is(expected))
        assertThat("Full=${FIVE_SEGMENT_FILE} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE, testOptions), is(expected))
        assertThat("Full=${FIVE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${expected}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE_SLASH, testOptions), is(expected))
    }

    @Test
    void verifyThatFilePathAsSafeStringWorksForFullPath() {
        List<ProcessorOption> testOptions = [ ProcessorOption.ShowFullPath ]

        assertTrue("Full=${ONE_SEGMENT_FILE} with option=${testOptions} ends with=filename.txt, actual=${ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions).endsWith("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=_filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is("_filename.txt"))
        assertTrue("Full=${TWO_SEGMENT_FILE} with option=${testOptions} ends with=parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions).endsWith("parent1_filename.txt"))
        assertThat("Full=${TWO_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent1_filename.txt",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE_SLASH, testOptions), is("_parent1_filename.txt"))
        assertTrue("Full=${THREE_SEGMENT_FILE} with option=${testOptions} ends with=parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions).endsWith("parent2_parent1_filename.txt"))
        assertThat("Full=${THREE_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE_SLASH, testOptions), is("_parent2_parent1_filename.txt"))
        assertTrue("Full=${FOUR_SEGMENT_FILE} with option=${testOptions} ends with=parent3_parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions).endsWith("parent3_parent2_parent1_filename.txt"))
        assertThat("Full=${FOUR_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent3_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE_SLASH, testOptions), is("_parent3_parent2_parent1_filename.txt"))
        assertThat("Full=${FIVE_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent4_parent3_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE_SLASH, testOptions), is("_parent4_parent3_parent2_parent1_filename.txt"))
    }

    @Test
    void verifyThatFilePathAsSafeStringWorksForNoOption() {
        List<ProcessorOption> testOptions = [ ]

        assertTrue("Full=${ONE_SEGMENT_FILE} with option=${testOptions} ends with=filename.txt, actual=${ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions).endsWith("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=_filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is("_filename.txt"))
        assertTrue("Full=${TWO_SEGMENT_FILE} with option=${testOptions} ends with=parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions).endsWith("parent1_filename.txt"))
        assertThat("Full=${TWO_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent1_filename.txt",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE_SLASH, testOptions), is("_parent1_filename.txt"))
        assertTrue("Full=${THREE_SEGMENT_FILE} with option=${testOptions} ends with=parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions).endsWith("parent2_parent1_filename.txt"))
        assertThat("Full=${THREE_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE_SLASH, testOptions), is("_parent2_parent1_filename.txt"))
        assertTrue("Full=${FOUR_SEGMENT_FILE} with option=${testOptions} ends with=parent3_parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions).endsWith("parent3_parent2_parent1_filename.txt"))
        assertThat("Full=${FOUR_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent3_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE_SLASH, testOptions), is("_parent3_parent2_parent1_filename.txt"))
        assertTrue("Full=${FIVE_SEGMENT_FILE} with option=${testOptions} ends with=parent4_parent3_parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE, testOptions).endsWith("parent4_parent3_parent2_parent1_filename.txt"))
        assertThat("Full=${FIVE_SEGMENT_FILE_SLASH} with option=${testOptions} is=_parent4_parent3_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE_SLASH, testOptions), is("_parent4_parent3_parent2_parent1_filename.txt"))
    }


}
