package nz.govt.natlib.tools.sip.generation.fairfax.processor

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.SystemUtils

import java.util.regex.Pattern

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

import org.junit.Test

class ProcessorUtilsTest {
    static final File ZERO_SEGMENT_FILE = new File("")
    static final File ZERO_SEGMENT_FILE_SLASH = new File(FilenameUtils.separatorsToSystem("/"))
    static final File ONE_SEGMENT_FILE = new File(FilenameUtils.separatorsToSystem("filename.txt"))
    static final File ONE_SEGMENT_FILE_SLASH = new File(FilenameUtils.separatorsToSystem("/filename.txt"))
    static final File TWO_SEGMENT_FILE = new File(FilenameUtils.separatorsToSystem("parent1/filename.txt"))
    static final File TWO_SEGMENT_FILE_SLASH = new File(FilenameUtils.separatorsToSystem("/parent1/filename.txt"))
    static final File THREE_SEGMENT_FILE = new File(FilenameUtils.separatorsToSystem("parent2/parent1/filename.txt"))
    static final File THREE_SEGMENT_FILE_SLASH = new File(FilenameUtils.separatorsToSystem("/parent2/parent1/filename.txt"))
    static final File FOUR_SEGMENT_FILE = new File(FilenameUtils.separatorsToSystem("parent3/parent2/parent1/filename.txt"))
    static final File FOUR_SEGMENT_FILE_SLASH = new File(FilenameUtils.separatorsToSystem("/parent3/parent2/parent1/filename.txt"))
    static final File FIVE_SEGMENT_FILE = new File(FilenameUtils.separatorsToSystem("parent4/parent3/parent2/parent1/filename.txt"))
    static final File FIVE_SEGMENT_FILE_SLASH = new File(FilenameUtils.separatorsToSystem("/parent4/parent3/parent2/parent1/filename.txt"))

    @Test
    void verifyThatFilePathAsSafeStringWorksForDirectoryOnly() {
        List<ProcessorOption> testOptions = [ ProcessorOption.ShowDirectoryOnly ]

        String expected = "filename.txt"
        String osPrefix = getOsPrefix()
        // Can't really test ZERO_SEGMENT_FILE, since it will automatically have its parent because of how File is constructed.
        // For ZERO_SEGMENT_FILE_SLASH, it's treated as the root folder, which means it is a blank
        assertThat("Full=${ZERO_SEGMENT_FILE_SLASH} with option=${testOptions} is='${osPrefix}'",
                ProcessorUtils.filePathAsSafeString(ZERO_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}".toString()))
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
        String osPrefix = getOsPrefix()
        assertTrue("Full=${ONE_SEGMENT_FILE} with option=${testOptions} ends with=filename.txt, actual=${ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions).endsWith("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_filename.txt".toString()))
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
        String osPrefix = getOsPrefix()
        assertTrue("Full=${ONE_SEGMENT_FILE} with option=${testOptions} ends with=filename.txt, actual=${ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions).endsWith("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_filename.txt".toString()))
        assertTrue("Full=${TWO_SEGMENT_FILE} with option=${testOptions} ends with=parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions).endsWith("parent1_filename.txt"))
        assertThat("Full=${TWO_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_parent1_filename.txt",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent1_filename.txt".toString()))
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
        String osPrefix = getOsPrefix()
        assertTrue("Full=${ONE_SEGMENT_FILE} with option=${testOptions} ends with=filename.txt, actual=${ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions).endsWith("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_filename.txt".toString()))
        assertTrue("Full=${TWO_SEGMENT_FILE} with option=${testOptions} ends with=parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions).endsWith("parent1_filename.txt"))
        assertThat("Full=${TWO_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_parent1_filename.txt",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent1_filename.txt".toString()))
        assertTrue("Full=${THREE_SEGMENT_FILE} with option=${testOptions} ends with=parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions).endsWith("parent2_parent1_filename.txt"))
        assertThat("Full=${THREE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent2_parent1_filename.txt".toString()))
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

        String osPrefix = getOsPrefix()
        assertTrue("Full=${ONE_SEGMENT_FILE} with option=${testOptions} ends with=filename.txt, actual=${ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions).endsWith("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_filename.txt".toString()))
        assertTrue("Full=${TWO_SEGMENT_FILE} with option=${testOptions} ends with=parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions).endsWith("parent1_filename.txt"))
        assertThat("Full=${TWO_SEGMENT_FILE_SLASH} with option=${testOptions} is={osPrefix}_parent1_filename.txt",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent1_filename.txt".toString()))
        assertTrue("Full=${THREE_SEGMENT_FILE} with option=${testOptions} ends with=parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions).endsWith("parent2_parent1_filename.txt"))
        assertThat("Full=${THREE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent2_parent1_filename.txt".toString()))
        assertTrue("Full=${FOUR_SEGMENT_FILE} with option=${testOptions} ends with=parent3_parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions).endsWith("parent3_parent2_parent1_filename.txt"))
        assertThat("Full=${FOUR_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_parent3_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent3_parent2_parent1_filename.txt".toString()))
        assertThat("Full=${FIVE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_parent4_parent3_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent4_parent3_parent2_parent1_filename.txt".toString()))
    }

    @Test
    void verifyThatFilePathAsSafeStringWorksForNoOption() {
        List<ProcessorOption> testOptions = [ ]

        String osPrefix = getOsPrefix()
        assertTrue("Full=${ONE_SEGMENT_FILE} with option=${testOptions} ends with=filename.txt, actual=${ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE, testOptions).endsWith("filename.txt"))
        assertThat("Full=${ONE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_filename.txt",
                ProcessorUtils.filePathAsSafeString(ONE_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_filename.txt".toString()))
        assertTrue("Full=${TWO_SEGMENT_FILE} with option=${testOptions} ends with=parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE, testOptions).endsWith("parent1_filename.txt"))
        assertThat("Full=${TWO_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_parent1_filename.txt",
                ProcessorUtils.filePathAsSafeString(TWO_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent1_filename.txt".toString()))
        assertTrue("Full=${THREE_SEGMENT_FILE} with option=${testOptions} ends with=parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE, testOptions).endsWith("parent2_parent1_filename.txt"))
        assertThat("Full=${THREE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(THREE_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent2_parent1_filename.txt".toString()))
        assertTrue("Full=${FOUR_SEGMENT_FILE} with option=${testOptions} ends with=parent3_parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE, testOptions).endsWith("parent3_parent2_parent1_filename.txt"))
        assertThat("Full=${FOUR_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_parent3_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(FOUR_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent3_parent2_parent1_filename.txt".toString()))
        assertTrue("Full=${FIVE_SEGMENT_FILE} with option=${testOptions} ends with=parent4_parent3_parent2_parent1_filename.txt, actual=${ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE, testOptions)}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE, testOptions).endsWith("parent4_parent3_parent2_parent1_filename.txt"))
        assertThat("Full=${FIVE_SEGMENT_FILE_SLASH} with option=${testOptions} is=${osPrefix}_parent4_parent3_parent2_parent1_filename.txt}",
                ProcessorUtils.filePathAsSafeString(FIVE_SEGMENT_FILE_SLASH, testOptions), is("${osPrefix}_parent4_parent3_parent2_parent1_filename.txt".toString()))
    }

    static String getOsPrefix() {
        String osPrefix
        if (SystemUtils.IS_OS_WINDOWS) {
            File testFile = new File("abc.txt")
            List<String> splitPath = testFile.getCanonicalPath().split(Pattern.quote(File.separator))
            osPrefix = splitPath.first().replace(":", "_")
        } else {
            osPrefix = ""
        }

        return osPrefix
    }
}
