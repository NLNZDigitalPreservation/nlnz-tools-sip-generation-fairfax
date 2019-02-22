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
        assertThat("Name parsed correctly", testFairfaxFile.name, is("TST"))
        assertThat("Edition parsed correctly", testFairfaxFile.edition, is("ED1"))
        assertNotNull("Year extracted", testFairfaxFile.dateYear)
        assertThat("dateYear parsed correctly", testFairfaxFile.dateYear, is(new Integer(2018)))
        assertThat("dateMonthOfYear parsed correctly", testFairfaxFile.dateMonthOfYear, is(new Integer(10)))
        assertThat("dateDayOfMonth parsed correctly", testFairfaxFile.dateDayOfMonth, is(new Integer(22)))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceLetter, is("B"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumberString, is("024"))
        assertThat("Prefix parsed correctly", testFairfaxFile.sequenceNumber, is(24))
        assertTrue("FairfaxFile is valid", testFairfaxFile.isValid())
    }

    @Test
    void createsCorrectlyWithNumberOnlySequence() {
        String originalFilename = "t20ABC-20181022-024.pdf"
        when(mockFile.getName()).thenReturn(originalFilename)

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertThat("Name parsed correctly", testFairfaxFile.name, is("t20"))
        assertThat("Edition parsed correctly", testFairfaxFile.edition, is("ABC"))
        assertThat("dateYear parsed correctly", testFairfaxFile.dateYear, is(new Integer(2018)))
        assertThat("dateMonthOfYear parsed correctly", testFairfaxFile.dateMonthOfYear, is(new Integer(10)))
        assertThat("dateDayOfMonth parsed correctly", testFairfaxFile.dateDayOfMonth, is(new Integer(22)))
        assertThat("sequenceLetter parsed correctly", testFairfaxFile.sequenceLetter, is(""))
        assertThat("sequenceNumber parsed correctly", testFairfaxFile.sequenceNumber, is(24))
        assertTrue("FairfaxFile is valid", testFairfaxFile.isValid())
    }

    @Test
    void createsCorrectlyWithInvalidFilename() {
        String originalFilename = "abc20181022-024.pdf"
        when(mockFile.getName()).thenReturn(originalFilename)

        FairfaxFile testFairfaxFile = new FairfaxFile(mockFile)

        assertThat("filename extracted correctly", testFairfaxFile.filename, is(originalFilename))
        assertFalse("FairfaxFile is invalid", testFairfaxFile.isValid())
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
}
