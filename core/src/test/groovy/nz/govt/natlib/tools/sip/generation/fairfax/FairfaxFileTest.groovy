package nz.govt.natlib.tools.sip.generation.fairfax

import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule

import java.nio.file.Path
import java.time.LocalDate

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.when

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

/**
 * Tests {@link FairfaxFile}.
 */
@RunWith(MockitoJUnitRunner.class)
class FairfaxFileTest {

    @Mock
    Path mockFile, mockFile1, mockFile2, mockFile3

    @Test
    void correctlyMatchesFilenamesUsingDifferentRegexPatterns() {
        checkRegexFilenamePatternMatches("abcDE-20180425-1.pDF", true, true, true)
        checkRegexFilenamePatternMatches("abcDE-20180425-A1.pDF", true, true, true)
        checkRegexFilenamePatternMatches("abcDEF-20180425-A01.pDF", true, true, true)
        checkRegexFilenamePatternMatches("abcDEFG-20180425-A001.PDF", true, true, true)
        checkRegexFilenamePatternMatches("abcDEFG-20180425-0001.PDF", true, true, true)
        checkRegexFilenamePatternMatches("abcDEFG-20180425-A001.PDF", true, true, true)
        checkRegexFilenamePatternMatches("abcDE-20180425-A1some-qualifier.pDF", true, true, true)
        checkRegexFilenamePatternMatches("abcDE-20180425-A1-another-qualifier.pDF", true, true, true)
        // NOTE: This does match, but the '1' at the end is included in the qualifier.
        checkRegexFilenamePatternMatches("abcDEF-20180425-A0001.pdf", true, true, true)

        checkRegexFilenamePatternMatches("abcDE201804251.pDF", false, false, false)
        checkRegexFilenamePatternMatches("abcDE-201804250-1.pDF", false, false, false)
        checkRegexFilenamePatternMatches("abcd-20180425-1.pDF", false, false, false)
        checkRegexFilenamePatternMatches("abcDEFG-nodate-A001.PDF", false, false, false)
        checkRegexFilenamePatternMatches("abcDEFGH-20180425-A001.PDF", false, false, false)
        checkRegexFilenamePatternMatches("abcDEFGH-20180425-A001", false, false, false)
        checkRegexFilenamePatternMatches("abcDEF-20180425-A001.pdf2", false, false, false)
        checkRegexFilenamePatternMatches("abc4-20180425-A001.pdf", false, false, false)
        checkRegexFilenamePatternMatches("abc-20180425-A001.pdf", false, false, false)
        checkRegexFilenamePatternMatches("ab-20180425-A001.pdf", false, false, false)
        checkRegexFilenamePatternMatches("", false, false, false)
    }

    void checkRegexFilenamePatternMatches(String valueToCheck, boolean matchesWithGroupingRegex,
                                          boolean matchesWithDateSequencePattern, boolean matchesWithDateOnlyPattern) {
        if (matchesWithGroupingRegex) {
            assertTrue("value=${valueToCheck} matches pattern=${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_GROUPING_PATTERN}",
                    valueToCheck ==~ /${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_GROUPING_PATTERN}/)
        } else {
            assertFalse("value=${valueToCheck} does NOT match pattern=${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_GROUPING_PATTERN}",
                    valueToCheck ==~ /${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_GROUPING_PATTERN}/)
        }
        if (matchesWithDateSequencePattern) {
            assertTrue("value=${valueToCheck} matches pattern=${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_PATTERN}",
                    valueToCheck ==~ /${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_PATTERN}/)
        } else {
            assertFalse("value=${valueToCheck} does NOT match pattern=${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_PATTERN}",
                    valueToCheck ==~ /${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_PATTERN}/)
        }
        if (matchesWithDateOnlyPattern) {
            assertTrue("value=${valueToCheck} matches pattern=${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_PATTERN}",
                    valueToCheck ==~ /${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_PATTERN}/)
        } else {
            assertFalse("value=${valueToCheck} does NOT match pattern=${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_PATTERN}",
                    valueToCheck ==~ /${FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_PATTERN}/)
        }
    }

    @Test
    void createsCorrectlyWithLetterSequence() {
        String originalFilename = "TSTED1-20181022-B024.pdf"
        when(mockFile.fileName).thenReturn(Path.of(originalFilename))

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("Filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertThat("TitleCode parsed correctly", testFairfaxFile.titleCode, is("TST"))
        assertThat("SectionCode parsed correctly", testFairfaxFile.sectionCode, is("ED1"))
        assertNotNull("Year extracted", testFairfaxFile.dateYear)
        assertThat("dateYear parsed correctly", testFairfaxFile.dateYear, is(new Integer(2018)))
        assertThat("dateMonthOfYear parsed correctly", testFairfaxFile.dateMonthOfYear, is(new Integer(10)))
        assertThat("dateDayOfMonth parsed correctly", testFairfaxFile.dateDayOfMonth, is(new Integer(22)))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceLetter, is("B"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumberString, is("024"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumber, is(24))
        assertThat("Qualifier parsed correctly", testFairfaxFile.qualifier, is(""))
        assertTrue("FairfaxFile is valid", testFairfaxFile.isValidName())
    }

    @Test
    void createsCorrectlyWithNumberOnlySequence() {
        String originalFilename = "t20ABC-20181022-024.pdf"
        when(mockFile.fileName).thenReturn(Path.of(originalFilename))

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertThat("TitleCode parsed correctly", testFairfaxFile.titleCode, is("t20"))
        assertThat("SectionCode parsed correctly", testFairfaxFile.sectionCode, is("ABC"))
        assertThat("dateYear parsed correctly", testFairfaxFile.dateYear, is(new Integer(2018)))
        assertThat("dateMonthOfYear parsed correctly", testFairfaxFile.dateMonthOfYear, is(new Integer(10)))
        assertThat("dateDayOfMonth parsed correctly", testFairfaxFile.dateDayOfMonth, is(new Integer(22)))
        assertThat("sequenceLetter parsed correctly", testFairfaxFile.sequenceLetter, is(""))
        assertThat("sequenceNumber parsed correctly", testFairfaxFile.sequenceNumber, is(24))
        assertThat("Qualifier parsed correctly", testFairfaxFile.qualifier, is(""))
        assertTrue("FairfaxFile is valid", testFairfaxFile.isValidName())
    }

    @Test
    void createsCorrectlyWithLetterSequenceQualifier() {
        String originalFilename = "TSTED1-20181022-B024a qualifier.pdf"
        when(mockFile.fileName).thenReturn(Path.of(originalFilename))

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("Filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertThat("TitleCode parsed correctly", testFairfaxFile.titleCode, is("TST"))
        assertThat("SectionCode parsed correctly", testFairfaxFile.sectionCode, is("ED1"))
        assertNotNull("Year extracted", testFairfaxFile.dateYear)
        assertThat("dateYear parsed correctly", testFairfaxFile.dateYear, is(new Integer(2018)))
        assertThat("dateMonthOfYear parsed correctly", testFairfaxFile.dateMonthOfYear, is(new Integer(10)))
        assertThat("dateDayOfMonth parsed correctly", testFairfaxFile.dateDayOfMonth, is(new Integer(22)))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceLetter, is("B"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumberString, is("024"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumber, is(24))
        assertThat("Qualifier parsed correctly", testFairfaxFile.qualifier, is("a qualifier"))
        assertTrue("FairfaxFile is valid", testFairfaxFile.isValidName())
    }

    @Test
    void createsCorrectlyWithMixedCaseExtension() {
        String originalFilename = "TSTED1-20181022-B024a qualifier.pDf"
        when(mockFile.fileName).thenReturn(Path.of(originalFilename))

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("Filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertThat("TitleCode parsed correctly", testFairfaxFile.titleCode, is("TST"))
        assertThat("SectionCode parsed correctly", testFairfaxFile.sectionCode, is("ED1"))
        assertNotNull("Year extracted", testFairfaxFile.dateYear)
        assertThat("dateYear parsed correctly", testFairfaxFile.dateYear, is(new Integer(2018)))
        assertThat("dateMonthOfYear parsed correctly", testFairfaxFile.dateMonthOfYear, is(new Integer(10)))
        assertThat("dateDayOfMonth parsed correctly", testFairfaxFile.dateDayOfMonth, is(new Integer(22)))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceLetter, is("B"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumberString, is("024"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumber, is(24))
        assertThat("Qualifier parsed correctly", testFairfaxFile.qualifier, is("a qualifier"))
        assertTrue("FairfaxFile is valid", testFairfaxFile.isValidName())
    }

    @Test
    void createsCorrectlyWithUpperCaseExtension() {
        String originalFilename = "TSTED1-20181022-B024a qualifier.PDF"
        when(mockFile.fileName).thenReturn(Path.of(originalFilename))

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("Filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertThat("TitleCode parsed correctly", testFairfaxFile.titleCode, is("TST"))
        assertThat("SectionCode parsed correctly", testFairfaxFile.sectionCode, is("ED1"))
        assertNotNull("Year extracted", testFairfaxFile.dateYear)
        assertThat("dateYear parsed correctly", testFairfaxFile.dateYear, is(new Integer(2018)))
        assertThat("dateMonthOfYear parsed correctly", testFairfaxFile.dateMonthOfYear, is(new Integer(10)))
        assertThat("dateDayOfMonth parsed correctly", testFairfaxFile.dateDayOfMonth, is(new Integer(22)))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceLetter, is("B"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumberString, is("024"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumber, is(24))
        assertThat("Qualifier parsed correctly", testFairfaxFile.qualifier, is("a qualifier"))
        assertTrue("FairfaxFile is valid", testFairfaxFile.isValidName())
    }

    @Test
    void createsCorrectlyWithFourCharacterTitleCode() {
        String originalFilename = "JAZZED1-20181022-B024a qualifier.pDf"
        when(mockFile.fileName).thenReturn(Path.of(originalFilename))

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("Filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertThat("TitleCode parsed correctly", testFairfaxFile.titleCode, is("JAZZ"))
        assertThat("SectionCode parsed correctly", testFairfaxFile.sectionCode, is("ED1"))
        assertNotNull("Year extracted", testFairfaxFile.dateYear)
        assertThat("dateYear parsed correctly", testFairfaxFile.dateYear, is(new Integer(2018)))
        assertThat("dateMonthOfYear parsed correctly", testFairfaxFile.dateMonthOfYear, is(new Integer(10)))
        assertThat("dateDayOfMonth parsed correctly", testFairfaxFile.dateDayOfMonth, is(new Integer(22)))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceLetter, is("B"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumberString, is("024"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumber, is(24))
        assertThat("Qualifier parsed correctly", testFairfaxFile.qualifier, is("a qualifier"))
        assertTrue("FairfaxFile is valid", testFairfaxFile.isValidName())
    }

    @Test
    void createsCorrectlyWithTwoCharacterSectionCode() {
        String originalFilename = "TSTAB-20181022-B024a qualifier.pDf"
        when(mockFile.fileName).thenReturn(Path.of(originalFilename))

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("Filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertThat("TitleCode parsed correctly", testFairfaxFile.titleCode, is("TST"))
        assertThat("SectionCode parsed correctly", testFairfaxFile.sectionCode, is("AB"))
        assertNotNull("Year extracted", testFairfaxFile.dateYear)
        assertThat("dateYear parsed correctly", testFairfaxFile.dateYear, is(new Integer(2018)))
        assertThat("dateMonthOfYear parsed correctly", testFairfaxFile.dateMonthOfYear, is(new Integer(10)))
        assertThat("dateDayOfMonth parsed correctly", testFairfaxFile.dateDayOfMonth, is(new Integer(22)))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceLetter, is("B"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumberString, is("024"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumber, is(24))
        assertThat("Qualifier parsed correctly", testFairfaxFile.qualifier, is("a qualifier"))
        assertTrue("FairfaxFile is valid", testFairfaxFile.isValidName())
    }

    @Test
    void createsCorrectlyWithNumberOnlySequenceQualifier() {
        String originalFilename = "t20ABC-20181022-024crop.pdf"
        when(mockFile.fileName).thenReturn(Path.of(originalFilename))

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertThat("TitleCode parsed correctly", testFairfaxFile.titleCode, is("t20"))
        assertThat("SectionCode parsed correctly", testFairfaxFile.sectionCode, is("ABC"))
        assertThat("dateYear parsed correctly", testFairfaxFile.dateYear, is(new Integer(2018)))
        assertThat("dateMonthOfYear parsed correctly", testFairfaxFile.dateMonthOfYear, is(new Integer(10)))
        assertThat("dateDayOfMonth parsed correctly", testFairfaxFile.dateDayOfMonth, is(new Integer(22)))
        assertThat("sequenceLetter parsed correctly", testFairfaxFile.sequenceLetter, is(""))
        assertThat("sequenceNumber parsed correctly", testFairfaxFile.sequenceNumber, is(24))
        assertThat("Qualifier parsed correctly", testFairfaxFile.qualifier, is("crop"))
        assertTrue("FairfaxFile is valid", testFairfaxFile.isValidName())
    }

    @Test
    void createsCorrectlyWithInvalidFilename() {
        String originalFilename = "abc20181022-024.pdf"
        when(mockFile.fileName).thenReturn(Path.of(originalFilename))

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertFalse("FairfaxFile is invalid", testFairfaxFile.isValidName())
    }

    @Test
    void matchesWhenSamePrefixAndDate() {
        String filename1 = "Mixy2k-20181022-023.pdf"
        String filename2 = "Mixy2k-20181022-001.pdf"
        when(mockFile1.fileName).thenReturn(Path.of(filename1))
        when(mockFile2.fileName).thenReturn(Path.of(filename2))

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)

        assertTrue("Same prefix and date in filename matches", fairfaxFile1.matches(fairfaxFile2))
        assertFalse("Same prefix and date but different sequence does not sequence match",
                fairfaxFile1.matchesWithSequence(fairfaxFile2))
    }

    @Test
    void matchesWhenSamePrefixDateAndSequence() {
        String filename1 = "Mixy2k-20181022-023.pdf"
        String filename2 = "Mixy2k-20181022-023withQualifier.pdf"
        when(mockFile1.fileName).thenReturn(Path.of(filename1))
        when(mockFile2.fileName).thenReturn(Path.of(filename2))

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)

        assertTrue("Same prefix and date in filename matches", fairfaxFile1.matches(fairfaxFile2))
        assertTrue("Matches with sequence", fairfaxFile1.matchesWithSequence(fairfaxFile2))
    }

    @Test
    void doesNotMatchWhenSamePrefixButDifferentDate() {
        String filename1 = "123456-20181022-023.pdf"
        String filename2 = "123456-20181021-023.pdf"
        when(mockFile1.fileName).thenReturn(Path.of(filename1))
        when(mockFile2.fileName).thenReturn(Path.of(filename2))

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)

        assertFalse("Same prefix but different dates does not match", fairfaxFile1.matches(fairfaxFile2))
    }

    @Test
    void doesNotMatchWhenDifferentPrefix() {
        String filename1 = "NAMed1-20181022-023.pdf"
        String filename2 = "NAMed2-20181022-023.pdf"
        when(mockFile1.fileName).thenReturn(Path.of(filename1))
        when(mockFile2.fileName).thenReturn(Path.of(filename2))

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)

        assertFalse("Different prefixes does not match", fairfaxFile1.matches(fairfaxFile2))
    }

    @Test
    void sortsCorrectlyWithSameDateButDifferentSequenceNumbers() {
        String filename1 = "NAMed1-20181022-023.pdf"
        String filename2 = "NAMed1-20181022-022.pdf"
        String filename3 = "NAMed1-20181022-021.pdf"
        when(mockFile1.fileName).thenReturn(Path.of(filename1))
        when(mockFile2.fileName).thenReturn(Path.of(filename2))
        when(mockFile3.fileName).thenReturn(Path.of(filename3))

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(mockFile3)

        assertEquals("Sorts correctly with same date but different sequence numbers",
                [fairfaxFile1, fairfaxFile2, fairfaxFile3].sort(), [fairfaxFile3, fairfaxFile2, fairfaxFile1])
    }

    @Test
    void sortsCorrectlyWithDifferentDates() {
        String filename1 = "NAMed1-20181023-021.pdf"
        String filename2 = "NAMed1-20181022-022.pdf"
        String filename3 = "NAMed1-20181021-023.pdf"
        when(mockFile1.fileName).thenReturn(Path.of(filename1))
        when(mockFile2.fileName).thenReturn(Path.of(filename2))
        when(mockFile3.fileName).thenReturn(Path.of(filename3))

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(mockFile3)

        assertEquals("Sorts correctly with same date but different sequence numbers",
                [fairfaxFile1, fairfaxFile2, fairfaxFile3].sort(), [fairfaxFile3, fairfaxFile2, fairfaxFile1])
    }

    @Test
    void sortsCorrectlyWithSameDateAndSequenceStringButDifferentNumbers() {
        String filename1 = "NAMed1-20181022-C023.pdf"
        String filename2 = "NAMed1-20181022-C022.pdf"
        String filename3 = "NAMed1-20181022-C021.pdf"
        when(mockFile1.fileName).thenReturn(Path.of(filename1))
        when(mockFile2.fileName).thenReturn(Path.of(filename2))
        when(mockFile3.fileName).thenReturn(Path.of(filename3))

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(mockFile3)

        assertEquals("Sorts correctly with same date and sequence string but different sequence numbers",
                [fairfaxFile1, fairfaxFile2, fairfaxFile3].sort(), [fairfaxFile3, fairfaxFile2, fairfaxFile1])
    }

    @Test
    void sortsCorrectlyWithSameDateAndDifferentSequenceStringButDifferentNumbers() {
        String filename1 = "NAMed1-20181022-M023.pdf"
        String filename2 = "NAMed1-20181022-C022.pdf"
        String filename3 = "NAMed1-20181022-A021.pdf"
        when(mockFile1.fileName).thenReturn(Path.of(filename1))
        when(mockFile2.fileName).thenReturn(Path.of(filename2))
        when(mockFile3.fileName).thenReturn(Path.of(filename3))

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(mockFile3)

        assertEquals("Sorts correctly with same date but different sequence numbers",
                [fairfaxFile1, fairfaxFile2, fairfaxFile3].sort(), [fairfaxFile3, fairfaxFile2, fairfaxFile1])
    }

    @Test
    void correctlyCreatesLocalDateFromFilename() {
        String filename1 = "NAMed1-20180101-M023.pdf"
        String filename2 = "NAMed1-20180630-A021.pdf"
        String filename3 = "NAMed1-20181231-C022.pdf"
        when(mockFile1.fileName).thenReturn(Path.of(filename1))
        when(mockFile2.fileName).thenReturn(Path.of(filename2))
        when(mockFile3.fileName).thenReturn(Path.of(filename3))

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(mockFile3)

        LocalDate january12018 = LocalDate.of(2018, 1, 1)
        LocalDate june302018 = LocalDate.of(2018, 6, 30)
        LocalDate december312018 = LocalDate.of(2018, 12, 31)
        assertThat("Creates date correctly for ${january12018}", fairfaxFile1.date, is(january12018))
        assertThat("Creates date correctly for ${june302018}", fairfaxFile2.date, is(june302018))
        assertThat("Creates date correctly for ${december312018}", fairfaxFile3.date, is(december312018))
    }

    @Test
    void sortsCorrectlyUsingNumericBeforeAlpha() {
        Path file1 = Path.of("NAMed1-20180131-M023.pdf")
        Path file2 = Path.of("NAMed1-20180131-A01.pdf")
        Path file3 = Path.of("NAMed1-20180131-A02.pdf")
        Path file4 = Path.of("NAMed1-20180131-02.pdf")
        Path file5 = Path.of("NAMed1-20180131-01.pdf")
        Path file6 = Path.of("NAMed1-20180131-C1.pdf")
        Path file7 = Path.of("NAMed1-20180131-C2.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)

        List<FairfaxFile> unsorted = [ fairfaxFile1, fairfaxFile2, fairfaxFile3, fairfaxFile4, fairfaxFile5,
                                       fairfaxFile6, fairfaxFile7 ]
        List<FairfaxFile> expected = [ fairfaxFile5, fairfaxFile4, fairfaxFile2, fairfaxFile3, fairfaxFile6,
                                       fairfaxFile7, fairfaxFile1 ]
        List<FairfaxFile> sorted = FairfaxFile.sortNumericAndAlpha(unsorted, false)
        assertThat("Numeric comes before alpha for sorted=${sorted}", sorted, is(expected))
    }

    @Test
    void sortsCorrectlyUsingAlphaBeforeNumeric() {
        Path file1 = Path.of("NAMed1-20180131-M023.pdf")
        Path file2 = Path.of("NAMed1-20180131-A01.pdf")
        Path file3 = Path.of("NAMed1-20180131-A02.pdf")
        Path file4 = Path.of("NAMed1-20180131-02.pdf")
        Path file5 = Path.of("NAMed1-20180131-01.pdf")
        Path file6 = Path.of("NAMed1-20180131-C1.pdf")
        Path file7 = Path.of("NAMed1-20180131-C2.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)

        List<FairfaxFile> unsorted = [ fairfaxFile1, fairfaxFile2, fairfaxFile3, fairfaxFile4, fairfaxFile5,
                                       fairfaxFile6, fairfaxFile7 ]
        List<FairfaxFile> expected = [ fairfaxFile2, fairfaxFile3, fairfaxFile6, fairfaxFile7, fairfaxFile1,
                                       fairfaxFile5, fairfaxFile4 ]
        List<FairfaxFile> sorted = FairfaxFile.sortNumericAndAlpha(unsorted, true)
        assertThat("Alpha comes before numeric for sorted=${sorted}", sorted, is(expected))
    }

    @Test
    void correctlyFiltersSubstitutesAndSortsOnFirstSectionCode() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed2-20180131-A02.pdf")
        Path file5 = Path.of("NAMed1-20180131-A04.pdf")
        Path file6 = Path.of("NAMed1-20180131-01.pdf")
        Path file7 = Path.of("NAMed2-20180131-01.pdf")
        Path file8 = Path.of("NAMed2-20180131-02.pdf")
        Path file9 = Path.of("NAMed2-20180131-03.pdf")
        Path file10 = Path.of("NAMed3-20180131-A01.pdf")
        Path file11 = Path.of("NAMFEE-20180131-A01.pdf")
        Path file12 = Path.of("NAMFEE-20180131-A02.pdf")
        Path file13 = Path.of("NAMNOT-20180131-A01.pdf")
        Path file14 = Path.of("NAMNOT-20180131-A02.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile10 = new FairfaxFile(file10)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        FairfaxProcessingParameters processingParameters = new FairfaxProcessingParameters(currentEdition: "ed1",
                editionDiscriminators: [ "ed1", "ed2", "ed3" ], sectionCodes: [ "ed1", "TWO", "FEE" ])

        List<FairfaxFile> original = [ fairfaxFile14, fairfaxFile13, fairfaxFile12, fairfaxFile11, fairfaxFile10,
                                       fairfaxFile9, fairfaxFile8, fairfaxFile7, fairfaxFile6, fairfaxFile5,
                                       fairfaxFile4, fairfaxFile3, fairfaxFile2, fairfaxFile1 ]
        List<FairfaxFile> expected = [ fairfaxFile6, fairfaxFile1, fairfaxFile2, fairfaxFile3, fairfaxFile5,
                                       fairfaxFile11, fairfaxFile12 ]
        List<FairfaxFile> substituted = FairfaxFile.filterSubstituteAndSort(original, processingParameters)
        assertTrue("There are substitutions possible", FairfaxFile.hasSubstitutions("ed1", original))
        assertThat("Filter substitute and sort done correctly from=${original} to=${substituted}", substituted,
                is(expected))
    }

    @Test
    void correctlyFiltersSubstitutesAndSortsWithSecondSectionCode() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed2-20180131-A02.pdf")
        Path file5 = Path.of("NAMed1-20180131-A04.pdf")
        Path file6 = Path.of("NAMed1-20180131-01.pdf")
        Path file7 = Path.of("NAMed2-20180131-01.pdf")
        Path file8 = Path.of("NAMed2-20180131-02.pdf")
        Path file9 = Path.of("NAMed2-20180131-03.pdf")
        Path file10 = Path.of("NAMed3-20180131-A01.pdf")
        Path file11 = Path.of("NAMFEE-20180131-A01.pdf")
        Path file12 = Path.of("NAMFEE-20180131-A02.pdf")
        Path file13 = Path.of("NAMNOT-20180131-A01.pdf")
        Path file14 = Path.of("NAMNOT-20180131-A02.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile10 = new FairfaxFile(file10)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        FairfaxProcessingParameters processingParameters = new FairfaxProcessingParameters(currentEdition: "ed2",
                editionDiscriminators: [ "ed1", "ed2", "ed3" ], sectionCodes: [ "ed1", "TWO", "FEE" ])

        List<FairfaxFile> original = [ fairfaxFile14, fairfaxFile13, fairfaxFile12, fairfaxFile11, fairfaxFile10,
                                       fairfaxFile9, fairfaxFile8, fairfaxFile7, fairfaxFile6, fairfaxFile5,
                                       fairfaxFile4, fairfaxFile3, fairfaxFile2, fairfaxFile1 ]
        List<FairfaxFile> expected = [ fairfaxFile7, fairfaxFile8, fairfaxFile9, fairfaxFile1, fairfaxFile4,
                                       fairfaxFile3, fairfaxFile5, fairfaxFile11, fairfaxFile12 ]
        List<FairfaxFile> substituted = FairfaxFile.filterSubstituteAndSort(original, processingParameters)
        assertTrue("There are substitutions possible", FairfaxFile.hasSubstitutions("ed1", original))
        assertThat("Filter substitute and sort done correctly from=${original} to=${substituted}", substituted,
                is(expected))
    }

    @Test
    void correctlyFiltersSubstitutesAndSortsWithSecondSectionCodeAndAlphaBeforeNumeric() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed2-20180131-A02.pdf")
        Path file5 = Path.of("NAMed1-20180131-A04.pdf")
        Path file6 = Path.of("NAMed1-20180131-01.pdf")
        Path file7 = Path.of("NAMed2-20180131-01.pdf")
        Path file8 = Path.of("NAMed2-20180131-02.pdf")
        Path file9 = Path.of("NAMed2-20180131-03.pdf")
        Path file10 = Path.of("NAMed3-20180131-A01.pdf")
        Path file11 = Path.of("NAMFEE-20180131-A01.pdf")
        Path file12 = Path.of("NAMFEE-20180131-A02.pdf")
        Path file13 = Path.of("NAMNOT-20180131-A01.pdf")
        Path file14 = Path.of("NAMNOT-20180131-A02.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile10 = new FairfaxFile(file10)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        FairfaxProcessingParameters processingParameters = new FairfaxProcessingParameters(currentEdition: "ed2",
                editionDiscriminators: [ "ed1", "ed2", "ed3" ], sectionCodes: [ "ed1", "TWO", "FEE" ],
                options: [ ProcessingOption.AlphaBeforeNumericSequencing ] )

        List<FairfaxFile> original = [ fairfaxFile14, fairfaxFile13, fairfaxFile12, fairfaxFile11, fairfaxFile10,
                                       fairfaxFile9, fairfaxFile8, fairfaxFile7, fairfaxFile6, fairfaxFile5,
                                       fairfaxFile4, fairfaxFile3, fairfaxFile2, fairfaxFile1 ]
        List<FairfaxFile> expected = [ fairfaxFile1, fairfaxFile4, fairfaxFile3, fairfaxFile5, fairfaxFile7,
                                       fairfaxFile8, fairfaxFile9, fairfaxFile11, fairfaxFile12 ]
        List<FairfaxFile> substituted = FairfaxFile.filterSubstituteAndSort(original, processingParameters)
        assertTrue("There are substitutions possible", FairfaxFile.hasSubstitutions("ed1", original))
        assertThat("Filter substitute and sort done correctly from=${original} to=${substituted}", substituted,
                is(expected))
    }

    @Test
    void correctlyFiltersSubstitutesAndSortsWhenThereAreNoSubstitutes() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed2-20180131-A02.pdf")
        Path file5 = Path.of("NAMed1-20180131-A04.pdf")
        Path file6 = Path.of("NAMed1-20180131-01.pdf")
        Path file7 = Path.of("NAMed2-20180131-01.pdf")
        Path file8 = Path.of("NAMed2-20180131-02.pdf")
        Path file9 = Path.of("NAMed2-20180131-03.pdf")
        Path file11 = Path.of("NAMFEE-20180131-A01.pdf")
        Path file12 = Path.of("NAMFEE-20180131-A02.pdf")
        Path file13 = Path.of("NAMNOT-20180131-A01.pdf")
        Path file14 = Path.of("NAMNOT-20180131-A02.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        FairfaxProcessingParameters processingParameters = new FairfaxProcessingParameters(currentEdition: "ed3",
                editionDiscriminators: [ "ed1", "ed2", "ed3" ], sectionCodes: [ "ed1", "TWO", "FEE" ])

        List<FairfaxFile> original = [ fairfaxFile14, fairfaxFile13, fairfaxFile12, fairfaxFile11,
                                       fairfaxFile9, fairfaxFile8, fairfaxFile7, fairfaxFile6, fairfaxFile5,
                                       fairfaxFile4, fairfaxFile3, fairfaxFile2, fairfaxFile1 ]
        List<FairfaxFile> expected = [ ]
        List<FairfaxFile> substituted = FairfaxFile.filterSubstituteAndSort(original, processingParameters)
        assertFalse("There are substitutions possible", FairfaxFile.hasSubstitutions("ed3", original))
        assertThat("Filter substitute and sort done correctly from=${original} to=${substituted}", substituted,
                is(expected))
    }

    @Test
    void correctlySortsWithSameTitleCodeAndDate() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed1-20180131-A04.pdf")
        Path file5 = Path.of("NAMed1-20180131-A06.pdf")
        Path file6 = Path.of("NAMed1-20180131-01.pdf")
        Path file7 = Path.of("NAMed1-20180131-02.pdf")
        Path file8 = Path.of("NAMed1-20180131-03.pdf")
        Path file9 = Path.of("NAMed1-20180131-05.pdf")
        Path file10 = Path.of("NAMed1-20180131-09.pdf")
        Path file11 = Path.of("NAMFEE-20180131-B02.pdf")
        Path file12 = Path.of("NAMFEE-20180131-B04.pdf")
        Path file13 = Path.of("NAMFEE-20180131-C06.pdf")
        Path file14 = Path.of("NAMFEE-20180131-C08.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile10 = new FairfaxFile(file10)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        FairfaxProcessingParameters processingParameters = new FairfaxProcessingParameters(
                sectionCodes: [ "ed1", "TWO", "FEE" ])

        List<FairfaxFile> original = [ fairfaxFile14, fairfaxFile13, fairfaxFile12, fairfaxFile11,
                                       fairfaxFile9, fairfaxFile8, fairfaxFile7, fairfaxFile6, fairfaxFile5,
                                       fairfaxFile10, fairfaxFile4, fairfaxFile3, fairfaxFile2, fairfaxFile1 ]
        List<FairfaxFile> expected = [ fairfaxFile6, fairfaxFile7, fairfaxFile8, fairfaxFile9, fairfaxFile10,
                                       fairfaxFile1, fairfaxFile2, fairfaxFile3, fairfaxFile4, fairfaxFile5,
                                       fairfaxFile11, fairfaxFile12, fairfaxFile13, fairfaxFile14 ]
        List<FairfaxFile> sorted = FairfaxFile.sortWithSameTitleCodeAndDate(original, processingParameters)
        assertThat("Sort done correctly from=${original} to=${sorted}", sorted, is(expected))
    }

    @Test
    void correctDeterminesIfOneFileComesAfterAnother() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed1-20180131-A02.pdf")
        Path file5 = Path.of("NAMed1-20180131-A04.pdf")
        Path file6 = Path.of("NAMed1-20180131-01.pdf")
        Path file7 = Path.of("NAMed1-20180131-02.pdf")
        Path file8 = Path.of("NAMed2-20180131-03.pdf")
        Path file9 = Path.of("NAMed2-20180131-04.pdf")
        Path file11 = Path.of("NAMFEE-20180131-B01.pdf")
        Path file12 = Path.of("NAMFEE-20180131-B02.pdf")
        Path file13 = Path.of("NAMNOT-20180131-C01.pdf")
        Path file14 = Path.of("NAMNOT-20180131-C02.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        assertTrue("FairfaxFile=${fairfaxFile2.file.fileName} comes directly after FairfaxFile=${fairfaxFile1.file.fileName}",
                fairfaxFile2.canComeDirectlyAfter(fairfaxFile1))
        assertTrue("FairfaxFile=${fairfaxFile3.file.fileName} comes directly after FairfaxFile=${fairfaxFile2.file.fileName}",
                fairfaxFile3.canComeDirectlyAfter(fairfaxFile2))
        assertTrue("FairfaxFile=${fairfaxFile4.file.fileName} comes directly after FairfaxFile=${fairfaxFile1.file.fileName}",
                fairfaxFile4.canComeDirectlyAfter(fairfaxFile1))
        assertTrue("FairfaxFile=${fairfaxFile6.file.fileName} comes directly after FairfaxFile=${fairfaxFile5.file.fileName}",
                fairfaxFile6.canComeDirectlyAfter(fairfaxFile5))
        assertTrue("FairfaxFile=${fairfaxFile7.file.fileName} comes directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile7.canComeDirectlyAfter(fairfaxFile6))
        assertTrue("FairfaxFile=${fairfaxFile9.file.fileName} comes directly after FairfaxFile=${fairfaxFile8.file.fileName}",
                fairfaxFile9.canComeDirectlyAfter(fairfaxFile8))
        assertTrue("FairfaxFile=${fairfaxFile11.file.fileName} comes directly after FairfaxFile=${fairfaxFile9.file.fileName}",
                fairfaxFile11.canComeDirectlyAfter(fairfaxFile9))
        assertTrue("FairfaxFile=${fairfaxFile12.file.fileName} comes directly after FairfaxFile=${fairfaxFile11.file.fileName}",
                fairfaxFile12.canComeDirectlyAfter(fairfaxFile11))
        assertTrue("FairfaxFile=${fairfaxFile13.file.fileName} comes directly after FairfaxFile=${fairfaxFile12.file.fileName}",
                fairfaxFile13.canComeDirectlyAfter(fairfaxFile12))
        assertTrue("FairfaxFile=${fairfaxFile13.file.fileName} comes directly after FairfaxFile=${fairfaxFile9.file.fileName}",
                fairfaxFile13.canComeDirectlyAfter(fairfaxFile9))
        assertTrue("FairfaxFile=${fairfaxFile11.file.fileName} comes directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile11.canComeDirectlyAfter(fairfaxFile6))

        assertFalse("FairfaxFile=${fairfaxFile1.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile2.file.fileName}",
                fairfaxFile1.canComeDirectlyAfter(fairfaxFile2))
        assertFalse("FairfaxFile=${fairfaxFile4.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile5.file.fileName}",
                fairfaxFile4.canComeDirectlyAfter(fairfaxFile5))
        assertFalse("FairfaxFile=${fairfaxFile7.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile5.file.fileName}",
                fairfaxFile7.canComeDirectlyAfter(fairfaxFile5))
        assertFalse("FairfaxFile=${fairfaxFile8.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile7.file.fileName}",
                fairfaxFile8.canComeDirectlyAfter(fairfaxFile7))
        assertFalse("FairfaxFile=${fairfaxFile2.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile3.file.fileName}",
                fairfaxFile2.canComeDirectlyAfter(fairfaxFile3))
        assertFalse("FairfaxFile=${fairfaxFile3.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile1.file.fileName}",
                fairfaxFile3.canComeDirectlyAfter(fairfaxFile1))
        assertFalse("FairfaxFile=${fairfaxFile14.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile11.file.fileName}",
                fairfaxFile14.canComeDirectlyAfter(fairfaxFile11))
        assertFalse("FairfaxFile=${fairfaxFile9.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile9.canComeDirectlyAfter(fairfaxFile6))
        assertFalse("FairfaxFile=${fairfaxFile9.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile7.file.fileName}",
                fairfaxFile9.canComeDirectlyAfter(fairfaxFile7))
        assertFalse("FairfaxFile=${fairfaxFile7.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile9.file.fileName}",
                fairfaxFile7.canComeDirectlyAfter(fairfaxFile9))
        assertFalse("FairfaxFile=${fairfaxFile14.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile12.file.fileName}",
                fairfaxFile14.canComeDirectlyAfter(fairfaxFile12))
    }

    @Test
    void correctDeterminesIfOneFileComesAfterAnotherWithMultipleNumericSequences() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed1-20180131-A04.pdf")
        Path file5 = Path.of("NAMed1-20180131-A05.pdf")
        Path file6 = Path.of("NAMed2-20180131-01.pdf")
        Path file7 = Path.of("NAMed2-20180131-02.pdf")
        Path file8 = Path.of("NAMed2-20180131-03.pdf")
        Path file9 = Path.of("NAMed2-20180131-04.pdf")
        Path file11 = Path.of("NAMFEE-20180131-01.pdf")
        Path file12 = Path.of("NAMFEE-20180131-02.pdf")
        Path file13 = Path.of("NAMNOT-20180131-C01.pdf")
        Path file14 = Path.of("NAMNOT-20180131-C02.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        assertTrue("FairfaxFile=${fairfaxFile2.file.fileName} comes directly after FairfaxFile=${fairfaxFile1.file.fileName}",
                fairfaxFile2.canComeDirectlyAfter(fairfaxFile1))
        assertTrue("FairfaxFile=${fairfaxFile3.file.fileName} comes directly after FairfaxFile=${fairfaxFile2.file.fileName}",
                fairfaxFile3.canComeDirectlyAfter(fairfaxFile2))
        assertTrue("FairfaxFile=${fairfaxFile4.file.fileName} comes directly after FairfaxFile=${fairfaxFile3.file.fileName}",
                fairfaxFile4.canComeDirectlyAfter(fairfaxFile3))
        assertTrue("FairfaxFile=${fairfaxFile6.file.fileName} comes directly after FairfaxFile=${fairfaxFile5.file.fileName}",
                fairfaxFile6.canComeDirectlyAfter(fairfaxFile5))
        assertTrue("FairfaxFile=${fairfaxFile7.file.fileName} comes directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile7.canComeDirectlyAfter(fairfaxFile6))
        assertTrue("FairfaxFile=${fairfaxFile9.file.fileName} comes directly after FairfaxFile=${fairfaxFile8.file.fileName}",
                fairfaxFile9.canComeDirectlyAfter(fairfaxFile8))
        assertTrue("FairfaxFile=${fairfaxFile11.file.fileName} comes directly after FairfaxFile=${fairfaxFile9.file.fileName}",
                fairfaxFile11.canComeDirectlyAfter(fairfaxFile9))
        assertTrue("FairfaxFile=${fairfaxFile12.file.fileName} comes directly after FairfaxFile=${fairfaxFile11.file.fileName}",
                fairfaxFile12.canComeDirectlyAfter(fairfaxFile11))
        assertTrue("FairfaxFile=${fairfaxFile13.file.fileName} comes directly after FairfaxFile=${fairfaxFile12.file.fileName}",
                fairfaxFile13.canComeDirectlyAfter(fairfaxFile12))
        assertTrue("FairfaxFile=${fairfaxFile13.file.fileName} comes directly after FairfaxFile=${fairfaxFile9.file.fileName}",
                fairfaxFile13.canComeDirectlyAfter(fairfaxFile9))
        assertTrue("FairfaxFile=${fairfaxFile11.file.fileName} comes directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile11.canComeDirectlyAfter(fairfaxFile6))

        assertFalse("FairfaxFile=${fairfaxFile1.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile2.file.fileName}",
                fairfaxFile1.canComeDirectlyAfter(fairfaxFile2))
        assertFalse("FairfaxFile=${fairfaxFile4.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile5.file.fileName}",
                fairfaxFile4.canComeDirectlyAfter(fairfaxFile5))
        assertFalse("FairfaxFile=${fairfaxFile8.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile8.canComeDirectlyAfter(fairfaxFile6))
        assertFalse("FairfaxFile=${fairfaxFile2.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile3.file.fileName}",
                fairfaxFile2.canComeDirectlyAfter(fairfaxFile3))
        assertFalse("FairfaxFile=${fairfaxFile3.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile1.file.fileName}",
                fairfaxFile3.canComeDirectlyAfter(fairfaxFile1))
        assertFalse("FairfaxFile=${fairfaxFile14.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile11.file.fileName}",
                fairfaxFile14.canComeDirectlyAfter(fairfaxFile11))
        assertFalse("FairfaxFile=${fairfaxFile9.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile9.canComeDirectlyAfter(fairfaxFile6))
        assertFalse("FairfaxFile=${fairfaxFile9.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile7.file.fileName}",
                fairfaxFile9.canComeDirectlyAfter(fairfaxFile7))
        assertFalse("FairfaxFile=${fairfaxFile7.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile9.file.fileName}",
                fairfaxFile7.canComeDirectlyAfter(fairfaxFile9))
        assertFalse("FairfaxFile=${fairfaxFile12.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile12.canComeDirectlyAfter(fairfaxFile6))
        assertFalse("FairfaxFile=${fairfaxFile14.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile12.file.fileName}",
                fairfaxFile14.canComeDirectlyAfter(fairfaxFile12))
    }

    @Test
    void correctDeterminesIfOneFileComesAfterAnotherWithMultipleEditions() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed2-20180131-A02.pdf")
        Path file5 = Path.of("NAMed1-20180131-A04.pdf")
        Path file6 = Path.of("NAMed1-20180131-01.pdf")
        Path file7 = Path.of("NAMed2-20180131-01.pdf")
        Path file8 = Path.of("NAMed2-20180131-02.pdf")
        Path file9 = Path.of("NAMed2-20180131-03.pdf")
        Path file11 = Path.of("NAMFEE-20180131-001.pdf")
        Path file12 = Path.of("NAMFEE-20180131-002.pdf")
        Path file13 = Path.of("NAMNOT-20180131-C01.pdf")
        Path file14 = Path.of("NAMNOT-20180131-C02.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        List<String> editionDiscriminators = [ 'ed1', 'ed2' ]
        assertTrue("FairfaxFile=${fairfaxFile2.file.fileName} comes directly after FairfaxFile=${fairfaxFile1.file.fileName}",
                fairfaxFile2.canComeDirectlyAfter(fairfaxFile1, editionDiscriminators))
        assertTrue("FairfaxFile=${fairfaxFile3.file.fileName} comes directly after FairfaxFile=${fairfaxFile2.file.fileName}",
                fairfaxFile3.canComeDirectlyAfter(fairfaxFile2, editionDiscriminators))
        assertTrue("FairfaxFile=${fairfaxFile4.file.fileName} comes directly after FairfaxFile=${fairfaxFile1.file.fileName}",
                fairfaxFile4.canComeDirectlyAfter(fairfaxFile1, editionDiscriminators))
        assertTrue("FairfaxFile=${fairfaxFile6.file.fileName} comes directly after FairfaxFile=${fairfaxFile5.file.fileName}",
                fairfaxFile6.canComeDirectlyAfter(fairfaxFile5, editionDiscriminators))
        assertTrue("FairfaxFile=${fairfaxFile7.file.fileName} comes directly after FairfaxFile=${fairfaxFile5.file.fileName}",
                fairfaxFile7.canComeDirectlyAfter(fairfaxFile5, editionDiscriminators))
        assertTrue("FairfaxFile=${fairfaxFile9.file.fileName} comes directly after FairfaxFile=${fairfaxFile8.file.fileName}",
                fairfaxFile9.canComeDirectlyAfter(fairfaxFile8, editionDiscriminators))
        assertTrue("FairfaxFile=${fairfaxFile11.file.fileName} comes directly after FairfaxFile=${fairfaxFile9.file.fileName}",
                fairfaxFile11.canComeDirectlyAfter(fairfaxFile9, editionDiscriminators))
        assertTrue("FairfaxFile=${fairfaxFile12.file.fileName} comes directly after FairfaxFile=${fairfaxFile11.file.fileName}",
                fairfaxFile12.canComeDirectlyAfter(fairfaxFile11, editionDiscriminators))
        assertTrue("FairfaxFile=${fairfaxFile13.file.fileName} comes directly after FairfaxFile=${fairfaxFile12.file.fileName}",
                fairfaxFile13.canComeDirectlyAfter(fairfaxFile12, editionDiscriminators))
        assertTrue("FairfaxFile=${fairfaxFile13.file.fileName} comes directly after FairfaxFile=${fairfaxFile9.file.fileName}",
                fairfaxFile13.canComeDirectlyAfter(fairfaxFile9, editionDiscriminators))
        assertTrue("FairfaxFile=${fairfaxFile11.file.fileName} comes directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile11.canComeDirectlyAfter(fairfaxFile6, editionDiscriminators))

        assertFalse("FairfaxFile=${fairfaxFile1.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile2.file.fileName}",
                fairfaxFile1.canComeDirectlyAfter(fairfaxFile2, editionDiscriminators))
        assertFalse("FairfaxFile=${fairfaxFile4.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile5.file.fileName}",
                fairfaxFile4.canComeDirectlyAfter(fairfaxFile5, editionDiscriminators))
        assertFalse("FairfaxFile=${fairfaxFile7.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile7.canComeDirectlyAfter(fairfaxFile6, editionDiscriminators))
        assertFalse("FairfaxFile=${fairfaxFile2.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile3.file.fileName}",
                fairfaxFile2.canComeDirectlyAfter(fairfaxFile3, editionDiscriminators))
        assertFalse("FairfaxFile=${fairfaxFile3.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile1.file.fileName}",
                fairfaxFile3.canComeDirectlyAfter(fairfaxFile1, editionDiscriminators))
        assertFalse("FairfaxFile=${fairfaxFile14.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile11.file.fileName}",
                fairfaxFile14.canComeDirectlyAfter(fairfaxFile11, editionDiscriminators))
        assertFalse("FairfaxFile=${fairfaxFile9.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile6.file.fileName}",
                fairfaxFile9.canComeDirectlyAfter(fairfaxFile6, editionDiscriminators))
        assertFalse("FairfaxFile=${fairfaxFile9.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile7.file.fileName}",
                fairfaxFile9.canComeDirectlyAfter(fairfaxFile7, editionDiscriminators))
        assertFalse("FairfaxFile=${fairfaxFile7.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile9.file.fileName}",
                fairfaxFile7.canComeDirectlyAfter(fairfaxFile9, editionDiscriminators))
        assertFalse("FairfaxFile=${fairfaxFile14.file.fileName} DOES NOT come directly after FairfaxFile=${fairfaxFile12.file.fileName}",
                fairfaxFile14.canComeDirectlyAfter(fairfaxFile12, editionDiscriminators))
    }

    @Test
    void correctFindsPostMissingSequenceFiles() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed1-20180131-A04.pdf")
        Path file5 = Path.of("NAMed1-20180131-A06.pdf")
        Path file6 = Path.of("NAMed1-20180131-01.pdf")
        Path file7 = Path.of("NAMed1-20180131-02.pdf")
        Path file8 = Path.of("NAMed1-20180131-03.pdf")
        Path file9 = Path.of("NAMed1-20180131-05.pdf")
        Path file10 = Path.of("NAMed1-20180131-09.pdf")
        Path file11 = Path.of("NAMFEE-20180131-B02.pdf")
        Path file12 = Path.of("NAMFEE-20180131-B04.pdf")
        Path file13 = Path.of("NAMFEE-20180131-C06.pdf")
        Path file14 = Path.of("NAMFEE-20180131-C08.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile10 = new FairfaxFile(file10)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        FairfaxProcessingParameters processingParameters = new FairfaxProcessingParameters(
                sectionCodes: [ "ed1", "TWO", "FEE" ])

        List<FairfaxFile> original = [ fairfaxFile14, fairfaxFile13, fairfaxFile12, fairfaxFile11,
                                       fairfaxFile9, fairfaxFile8, fairfaxFile7, fairfaxFile6, fairfaxFile5,
                                       fairfaxFile10, fairfaxFile4, fairfaxFile3, fairfaxFile2, fairfaxFile1 ]
        List<FairfaxFile> expected = [ fairfaxFile9, fairfaxFile10, fairfaxFile5, fairfaxFile11, fairfaxFile12,
                                       fairfaxFile13, fairfaxFile14 ]
        List<FairfaxFile> postMissing = FairfaxFile.postMissingSequenceFiles(original, processingParameters)
        assertThat("Post missing correctly found from=${FairfaxFile.asFilenames(original)} postMissing=${FairfaxFile.asFilenames(postMissing)}",
                postMissing, is(expected))
    }

    @Test
    void correctlyDeterminesAHundredsSequenceStart() {
        Path file1 = Path.of("NAMed1-20180131-100.pdf")
        Path file2 = Path.of("NAMed1-20180131-101.pdf")
        Path file3 = Path.of("NAMed1-20180131-300.pdf")
        Path file4 = Path.of("NAMed1-20180131-399.pdf")
        Path file5 = Path.of("NAMed1-20180131-400.pdf")
        Path file6 = Path.of("NAMed1-20180131-401.pdf")
        Path file7 = Path.of("NAMed1-20180131-402.pdf")
        Path file8 = Path.of("NAMed1-20180131-501.pdf")
        Path file9 = Path.of("NAMed1-20180131-502.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)

        assertTrue("file=${fairfaxFile5.file} is isAHundredsSequenceStart", fairfaxFile5.isAHundredsSequenceStart())
        assertTrue("file=${fairfaxFile6.file} is isAHundredsSequenceStart", fairfaxFile6.isAHundredsSequenceStart())
        assertTrue("file=${fairfaxFile8.file} is isAHundredsSequenceStart", fairfaxFile8.isAHundredsSequenceStart())

        assertFalse("file=${fairfaxFile1.file} is NOT isAHundredsSequenceStart", fairfaxFile1.isAHundredsSequenceStart())
        assertFalse("file=${fairfaxFile2.file} is NOT isAHundredsSequenceStart", fairfaxFile2.isAHundredsSequenceStart())
        assertFalse("file=${fairfaxFile3.file} is NOT isAHundredsSequenceStart", fairfaxFile3.isAHundredsSequenceStart())
        assertFalse("file=${fairfaxFile4.file} is NOT isAHundredsSequenceStart", fairfaxFile4.isAHundredsSequenceStart())
        assertFalse("file=${fairfaxFile7.file} is NOT isAHundredsSequenceStart", fairfaxFile7.isAHundredsSequenceStart())
        assertFalse("file=${fairfaxFile9.file} is NOT isAHundredsSequenceStart", fairfaxFile9.isAHundredsSequenceStart())
    }

    @Test
    void correctFindsPostMissingSequenceFilesWhenHundredsSkipOkay() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed1-20180131-A04.pdf")
        Path file5 = Path.of("NAMed1-20180131-400.pdf")
        Path file6 = Path.of("NAMed1-20180131-01.pdf")
        Path file7 = Path.of("NAMed1-20180131-02.pdf")
        Path file8 = Path.of("NAMed1-20180131-03.pdf")
        Path file9 = Path.of("NAMed1-20180131-05.pdf")
        Path file10 = Path.of("NAMed1-20180131-101.pdf")
        Path file11 = Path.of("NAMFEE-20180131-B02.pdf")
        Path file12 = Path.of("NAMFEE-20180131-B04.pdf")
        Path file13 = Path.of("NAMFEE-20180131-C06.pdf")
        Path file14 = Path.of("NAMFEE-20180131-401.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile10 = new FairfaxFile(file10)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        FairfaxProcessingParameters processingParameters = new FairfaxProcessingParameters(
                sectionCodes: [ "ed1", "TWO", "FEE" ])
        processingParameters.rules = [ ProcessingRule.NumericStartsInHundredsNotConsideredSequenceSkips ]

        List<FairfaxFile> original = [ fairfaxFile14, fairfaxFile13, fairfaxFile12, fairfaxFile11,
                                       fairfaxFile9, fairfaxFile8, fairfaxFile7, fairfaxFile6, fairfaxFile5,
                                       fairfaxFile10, fairfaxFile4, fairfaxFile3, fairfaxFile2, fairfaxFile1 ]
        List<FairfaxFile> expected = [ fairfaxFile9, fairfaxFile10, fairfaxFile11, fairfaxFile12,
                                       fairfaxFile13 ]
        List<FairfaxFile> postMissing = FairfaxFile.postMissingSequenceFiles(original, processingParameters)
        assertThat("Post missing correctly found from=${FairfaxFile.asFilenames(original)} postMissing=${FairfaxFile.asFilenames(postMissing)}",
                postMissing, is(expected))
    }

    @Test
    void correctFindsPostMissingSequenceFilesWhenHundredsSkipNotOkay() {
        Path file1 = Path.of("NAMed1-20180131-A01.pdf")
        Path file2 = Path.of("NAMed1-20180131-A02.pdf")
        Path file3 = Path.of("NAMed1-20180131-A03.pdf")
        Path file4 = Path.of("NAMed1-20180131-A04.pdf")
        Path file5 = Path.of("NAMed1-20180131-400.pdf")
        Path file6 = Path.of("NAMed1-20180131-01.pdf")
        Path file7 = Path.of("NAMed1-20180131-02.pdf")
        Path file8 = Path.of("NAMed1-20180131-03.pdf")
        Path file9 = Path.of("NAMed1-20180131-05.pdf")
        Path file10 = Path.of("NAMed1-20180131-101.pdf")
        Path file11 = Path.of("NAMFEE-20180131-B02.pdf")
        Path file12 = Path.of("NAMFEE-20180131-B04.pdf")
        Path file13 = Path.of("NAMFEE-20180131-C06.pdf")
        Path file14 = Path.of("NAMFEE-20180131-401.pdf")

        FairfaxFile fairfaxFile1 = new FairfaxFile(file1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(file2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(file3)
        FairfaxFile fairfaxFile4 = new FairfaxFile(file4)
        FairfaxFile fairfaxFile5 = new FairfaxFile(file5)
        FairfaxFile fairfaxFile6 = new FairfaxFile(file6)
        FairfaxFile fairfaxFile7 = new FairfaxFile(file7)
        FairfaxFile fairfaxFile8 = new FairfaxFile(file8)
        FairfaxFile fairfaxFile9 = new FairfaxFile(file9)
        FairfaxFile fairfaxFile10 = new FairfaxFile(file10)
        FairfaxFile fairfaxFile11 = new FairfaxFile(file11)
        FairfaxFile fairfaxFile12 = new FairfaxFile(file12)
        FairfaxFile fairfaxFile13 = new FairfaxFile(file13)
        FairfaxFile fairfaxFile14 = new FairfaxFile(file14)

        FairfaxProcessingParameters processingParameters = new FairfaxProcessingParameters(
                sectionCodes: [ "ed1", "TWO", "FEE" ])
        processingParameters.rules = [ ProcessingRule.NumericStartsInHundredsConsideredSequenceSkips ]

        List<FairfaxFile> original = [ fairfaxFile14, fairfaxFile13, fairfaxFile12, fairfaxFile11,
                                       fairfaxFile9, fairfaxFile8, fairfaxFile7, fairfaxFile6, fairfaxFile5,
                                       fairfaxFile10, fairfaxFile4, fairfaxFile3, fairfaxFile2, fairfaxFile1 ]
        List<FairfaxFile> expected = [ fairfaxFile9, fairfaxFile10, fairfaxFile5, fairfaxFile14, fairfaxFile11,
                                       fairfaxFile12, fairfaxFile13 ]
        List<FairfaxFile> postMissing = FairfaxFile.postMissingSequenceFiles(original, processingParameters)
        assertThat("Post missing correctly found from=${FairfaxFile.asFilenames(original)} postMissing=${FairfaxFile.asFilenames(postMissing)}",
                postMissing, is(expected))
    }

}
