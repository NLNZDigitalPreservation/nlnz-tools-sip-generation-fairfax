package nz.govt.natlib.tools.sip.generation.fairfax

import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption

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
    File mockFile, mockFile1, mockFile2, mockFile3

    @Test
    void createsCorrectlyWithLetterSequence() {
        String originalFilename = "TSTED1-20181022-B024.pdf"
        when(mockFile.getName()).thenReturn(originalFilename)

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
        when(mockFile.getName()).thenReturn(originalFilename)

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
        when(mockFile.getName()).thenReturn(originalFilename)

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
        when(mockFile.getName()).thenReturn(originalFilename)

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
        when(mockFile.getName()).thenReturn(originalFilename)

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
        when(mockFile.getName()).thenReturn(originalFilename)

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
        when(mockFile.getName()).thenReturn(originalFilename)

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
        when(mockFile.getName()).thenReturn(originalFilename)

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
        when(mockFile.getName()).thenReturn(originalFilename)

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertFalse("FairfaxFile is invalid", testFairfaxFile.isValidName())
    }

    @Test
    void matchesWhenSamePrefixAndDate() {
        String filename1 = "Mixy2k-20181022-023.pdf"
        String filename2 = "Mixy2k-20181022-001.pdf"
        when(mockFile1.getName()).thenReturn(filename1)
        when(mockFile2.getName()).thenReturn(filename2)

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
        when(mockFile1.getName()).thenReturn(filename1)
        when(mockFile2.getName()).thenReturn(filename2)

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)

        assertTrue("Same prefix and date in filename matches", fairfaxFile1.matches(fairfaxFile2))
        assertTrue("Matches with sequence", fairfaxFile1.matchesWithSequence(fairfaxFile2))
    }

    @Test
    void doesNotMatchWhenSamePrefixButDifferentDate() {
        String filename1 = "123456-20181022-023.pdf"
        String filename2 = "123456-20181021-023.pdf"
        when(mockFile1.getName()).thenReturn(filename1)
        when(mockFile2.getName()).thenReturn(filename2)

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)

        assertFalse("Same prefix but different dates does not match", fairfaxFile1.matches(fairfaxFile2))
    }

    @Test
    void doesNotMatchWhenDifferentPrefix() {
        String filename1 = "NAMed1-20181022-023.pdf"
        String filename2 = "NAMed2-20181022-023.pdf"
        when(mockFile1.getName()).thenReturn(filename1)
        when(mockFile2.getName()).thenReturn(filename2)

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)

        assertFalse("Different prefixes does not match", fairfaxFile1.matches(fairfaxFile2))
    }

    @Test
    void sortsCorrectlyWithSameDateButDifferentSequenceNumbers() {
        String filename1 = "NAMed1-20181022-023.pdf"
        String filename2 = "NAMed1-20181022-022.pdf"
        String filename3 = "NAMed1-20181022-021.pdf"
        when(mockFile1.getName()).thenReturn(filename1)
        when(mockFile2.getName()).thenReturn(filename2)
        when(mockFile3.getName()).thenReturn(filename3)

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
        when(mockFile1.getName()).thenReturn(filename1)
        when(mockFile2.getName()).thenReturn(filename2)
        when(mockFile3.getName()).thenReturn(filename3)

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
        when(mockFile1.getName()).thenReturn(filename1)
        when(mockFile2.getName()).thenReturn(filename2)
        when(mockFile3.getName()).thenReturn(filename3)

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
        when(mockFile1.getName()).thenReturn(filename1)
        when(mockFile2.getName()).thenReturn(filename2)
        when(mockFile3.getName()).thenReturn(filename3)

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
        when(mockFile1.getName()).thenReturn(filename1)
        when(mockFile2.getName()).thenReturn(filename2)
        when(mockFile3.getName()).thenReturn(filename3)

        FairfaxFile fairfaxFile1 = new FairfaxFile(mockFile1)
        FairfaxFile fairfaxFile2 = new FairfaxFile(mockFile2)
        FairfaxFile fairfaxFile3 = new FairfaxFile(mockFile3)

        LocalDate january12018 = new LocalDate(2018, 1, 1)
        LocalDate june302018 = new LocalDate(2018, 6, 30)
        LocalDate december312018 = new LocalDate(2018, 12, 31)
        assertThat("Creates date correctly for ${january12018}", fairfaxFile1.date, is(january12018))
        assertThat("Creates date correctly for ${june302018}", fairfaxFile2.date, is(june302018))
        assertThat("Creates date correctly for ${december312018}", fairfaxFile3.date, is(december312018))
    }

    @Test
    void sortsCorrectlyUsingNumericBeforeAlpha() {
        File file1 = new File("NAMed1-20180131-M023.pdf")
        File file2 = new File("NAMed1-20180131-A01.pdf")
        File file3 = new File("NAMed1-20180131-A02.pdf")
        File file4 = new File("NAMed1-20180131-02.pdf")
        File file5 = new File("NAMed1-20180131-01.pdf")
        File file6 = new File("NAMed1-20180131-C1.pdf")
        File file7 = new File("NAMed1-20180131-C2.pdf")

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
        File file1 = new File("NAMed1-20180131-M023.pdf")
        File file2 = new File("NAMed1-20180131-A01.pdf")
        File file3 = new File("NAMed1-20180131-A02.pdf")
        File file4 = new File("NAMed1-20180131-02.pdf")
        File file5 = new File("NAMed1-20180131-01.pdf")
        File file6 = new File("NAMed1-20180131-C1.pdf")
        File file7 = new File("NAMed1-20180131-C2.pdf")

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
        File file1 = new File("NAMed1-20180131-A01.pdf")
        File file2 = new File("NAMed1-20180131-A02.pdf")
        File file3 = new File("NAMed1-20180131-A03.pdf")
        File file4 = new File("NAMed2-20180131-A02.pdf")
        File file5 = new File("NAMed1-20180131-A04.pdf")
        File file6 = new File("NAMed1-20180131-01.pdf")
        File file7 = new File("NAMed2-20180131-01.pdf")
        File file8 = new File("NAMed2-20180131-02.pdf")
        File file9 = new File("NAMed2-20180131-03.pdf")
        File file10 = new File("NAMed3-20180131-A01.pdf")
        File file11 = new File("NAMFEE-20180131-A01.pdf")
        File file12 = new File("NAMFEE-20180131-A02.pdf")
        File file13 = new File("NAMNOT-20180131-A01.pdf")
        File file14 = new File("NAMNOT-20180131-A02.pdf")

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
        List<FairfaxFile> substituted = FairfaxFile.filterSubstituteAndSort(processingParameters, original)
        assertTrue("There are substitutions possible", FairfaxFile.hasSubstitutions("ed1", original))
        assertThat("Filter substitute and sort done correctly from=${original} to=${substituted}", substituted,
                is(expected))
    }

    @Test
    void correctlyFiltersSubstitutesAndSortsWithSecondSectionCode() {
        File file1 = new File("NAMed1-20180131-A01.pdf")
        File file2 = new File("NAMed1-20180131-A02.pdf")
        File file3 = new File("NAMed1-20180131-A03.pdf")
        File file4 = new File("NAMed2-20180131-A02.pdf")
        File file5 = new File("NAMed1-20180131-A04.pdf")
        File file6 = new File("NAMed1-20180131-01.pdf")
        File file7 = new File("NAMed2-20180131-01.pdf")
        File file8 = new File("NAMed2-20180131-02.pdf")
        File file9 = new File("NAMed2-20180131-03.pdf")
        File file10 = new File("NAMed3-20180131-A01.pdf")
        File file11 = new File("NAMFEE-20180131-A01.pdf")
        File file12 = new File("NAMFEE-20180131-A02.pdf")
        File file13 = new File("NAMNOT-20180131-A01.pdf")
        File file14 = new File("NAMNOT-20180131-A02.pdf")

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
        List<FairfaxFile> substituted = FairfaxFile.filterSubstituteAndSort(processingParameters, original)
        assertTrue("There are substitutions possible", FairfaxFile.hasSubstitutions("ed1", original))
        assertThat("Filter substitute and sort done correctly from=${original} to=${substituted}", substituted,
                is(expected))
    }

    @Test
    void correctlyFiltersSubstitutesAndSortsWithSecondSectionCodeAndAlphaBeforeNumeric() {
        File file1 = new File("NAMed1-20180131-A01.pdf")
        File file2 = new File("NAMed1-20180131-A02.pdf")
        File file3 = new File("NAMed1-20180131-A03.pdf")
        File file4 = new File("NAMed2-20180131-A02.pdf")
        File file5 = new File("NAMed1-20180131-A04.pdf")
        File file6 = new File("NAMed1-20180131-01.pdf")
        File file7 = new File("NAMed2-20180131-01.pdf")
        File file8 = new File("NAMed2-20180131-02.pdf")
        File file9 = new File("NAMed2-20180131-03.pdf")
        File file10 = new File("NAMed3-20180131-A01.pdf")
        File file11 = new File("NAMFEE-20180131-A01.pdf")
        File file12 = new File("NAMFEE-20180131-A02.pdf")
        File file13 = new File("NAMNOT-20180131-A01.pdf")
        File file14 = new File("NAMNOT-20180131-A02.pdf")

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
                processingOptions: [ ProcessingOption.AlphaBeforeNumericSequencing ] )

        List<FairfaxFile> original = [ fairfaxFile14, fairfaxFile13, fairfaxFile12, fairfaxFile11, fairfaxFile10,
                                       fairfaxFile9, fairfaxFile8, fairfaxFile7, fairfaxFile6, fairfaxFile5,
                                       fairfaxFile4, fairfaxFile3, fairfaxFile2, fairfaxFile1 ]
        List<FairfaxFile> expected = [ fairfaxFile1, fairfaxFile4, fairfaxFile3, fairfaxFile5, fairfaxFile7,
                                       fairfaxFile8, fairfaxFile9, fairfaxFile11, fairfaxFile12 ]
        List<FairfaxFile> substituted = FairfaxFile.filterSubstituteAndSort(processingParameters, original)
        assertTrue("There are substitutions possible", FairfaxFile.hasSubstitutions("ed1", original))
        assertThat("Filter substitute and sort done correctly from=${original} to=${substituted}", substituted,
                is(expected))
    }

    @Test
    void correctlyFiltersSubstitutesAndSortsWhenThereAreNoSubstitutes() {
        File file1 = new File("NAMed1-20180131-A01.pdf")
        File file2 = new File("NAMed1-20180131-A02.pdf")
        File file3 = new File("NAMed1-20180131-A03.pdf")
        File file4 = new File("NAMed2-20180131-A02.pdf")
        File file5 = new File("NAMed1-20180131-A04.pdf")
        File file6 = new File("NAMed1-20180131-01.pdf")
        File file7 = new File("NAMed2-20180131-01.pdf")
        File file8 = new File("NAMed2-20180131-02.pdf")
        File file9 = new File("NAMed2-20180131-03.pdf")
        File file11 = new File("NAMFEE-20180131-A01.pdf")
        File file12 = new File("NAMFEE-20180131-A02.pdf")
        File file13 = new File("NAMNOT-20180131-A01.pdf")
        File file14 = new File("NAMNOT-20180131-A02.pdf")

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
        List<FairfaxFile> substituted = FairfaxFile.filterSubstituteAndSort(processingParameters, original)
        assertFalse("There are substitutions possible", FairfaxFile.hasSubstitutions("ed3", original))
        assertThat("Filter substitute and sort done correctly from=${original} to=${substituted}", substituted,
                is(expected))
    }
}
