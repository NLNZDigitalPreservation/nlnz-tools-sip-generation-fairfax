package nz.govt.natlib.tools.sip.generation.fairfax.scenarios

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.IEEntityType
import nz.govt.natlib.tools.sip.extraction.SipXmlExtractor
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule
import nz.govt.natlib.tools.sip.generation.fairfax.processor.FairfaxFilesProcessor
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.TestHelper
import nz.govt.natlib.tools.sip.generation.fairfax.TestHelper.TestMethodState
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

/**
 * Tests the {@code empty-file} scenario.
 *
 * Note that this test is complicated by the files either being part of a directory structure or in a resource file (jar),
 * so the {@link TestHelper} class is used to handle both scenarios. In real-life processing the files would be on the
 * filesystem and not in a resource. We explicitly use only filesystem files in
 * {@link #correctlyAssembleSipFromFilesOnFilesystem} (as an example to script writers), but this unit test is
 * ignored for builds.
 */
@RunWith(MockitoJUnitRunner.class)
@Log4j2
class EmptyFileTest {
    // TODO Make this processing simpler
    // - given a starting folder
    // - and a set of selection criteria
    // - create SIPs for the given files
    static String ID_COLUMN_NAME = "MMSID"

    static final String RESOURCES_FOLDER = "ingestion-files-tests/scenario-empty-file"
    static final String IMPORT_PARAMETERS_FILENAME = "test-fairfax-import-parameters.json"

    TestMethodState testMethodState

    @Before
    void setup() {
        testMethodState = new TestMethodState(ID_COLUMN_NAME, RESOURCES_FOLDER, IMPORT_PARAMETERS_FILENAME)
    }

    /**
     * Note to developers: Ensure that this is exactly the same test as {@link #correctlyAssemblesSipsFromFilesNoReplacementPdf()}, except
     * that this test only reads from the file system, not a resource file.
     *
     * This test should use the local filesystem when running from within an IDE.
     *
     * This test then becomes a starting point for scripts that create and process SIPs.
     */
    @Test
    // TODO Ignore this test before making a code commit
    @Ignore
    void correctlyAssemblesSipsFromFilesOnFilesystemNoReplacementPdf() {
        correctlyAssemblesSipFromFilesOnFilesystem([ ProcessingRule.ZeroLengthPdfSkipped ])
    }

    /**
     * Note to developers: Ensure that this is exactly the same test as {@link #correctAssemblesSipsFromFilesWithReplacementPdf()}, except
     * that this test only reads from the file system, not a resource file.
     *
     * This test should use the local filesystem when running from within an IDE.
     *
     * This test then becomes a starting point for scripts that create and process SIPs.
     */
    @Test
    // TODO Ignore this test before making a code commit
    @Ignore
    void correctlyAssemblesSipsFromFilesOnFilesystemWithReplacementPdf() {
        correctlyAssemblesSipFromFilesOnFilesystem([ ProcessingRule.ZeroLengthPdfReplacedWithPageUnavailablePdf ])
    }


    void correctlyAssemblesSipFromFilesOnFilesystem(List<ProcessingRule> overrideRules) {
        boolean forLocalFilesystem = true
        TestHelper.initializeTestMethod(testMethodState, "EmptyFileTest-", forLocalFilesystem)

        // TODO A more complicated pattern -- date and other masks?
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        List<Path> filesForProcessing = TestHelper.getFilesForProcessingFromFileSystem(isRegexNotGlob, matchFilenameOnly,
                sortFiles, testMethodState.localPath, ".*?\\.[pP]{1}[dD]{1}[fF]{1}")

        processFiles(filesForProcessing, overrideRules)
    }

    @Test
    void correctlyAssemblesSipsFromFilesNoReplacementPdf() {
        correctlyAssembleSipFromFiles([ ProcessingRule.ZeroLengthPdfSkipped ])
    }

    @Test
    void correctAssemblesSipsFromFilesWithReplacementPdf() {
        correctlyAssembleSipFromFiles([ ProcessingRule.ZeroLengthPdfReplacedWithPageUnavailablePdf ])
    }

    void correctlyAssembleSipFromFiles(List<ProcessingRule> overrideRules) {
        boolean forLocalFilesystem = false
        TestHelper.initializeTestMethod(testMethodState, "EmptyFileTest-", forLocalFilesystem)

        // TODO A more complicated pattern -- date and other masks?
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        List<Path> filesForProcessing = TestHelper.getFilesForProcessingFromResource(isRegexNotGlob, matchFilenameOnly,
                sortFiles, testMethodState.resourcePath, testMethodState.localPath, ".*?\\.[pP]{1}[dD]{1}[fF]{1}")

        processFiles(filesForProcessing, overrideRules)
    }

    void processFiles(List<Path> filesForProcessing, List<ProcessingRule> overrideRules) {
        String dateString = "20181123"
        LocalDate processingDate = LocalDate.parse(dateString, FairfaxFile.LOCAL_DATE_TIME_FORMATTER)

        Path sourceFolder = Path.of(testMethodState.localPath)
        List<FairfaxProcessingParameters> parametersList = FairfaxProcessingParameters.build("TST",
                [ ProcessingType.ParentGrouping ], sourceFolder, processingDate, testMethodState.fairfaxSpreadsheet,
                overrideRules)

        assertThat("Only a single FairfaxProcessingParameters is returned, size=${parametersList.size()}",
                parametersList.size(), is(1))

        FairfaxProcessingParameters processingParameters = parametersList.first()

        overrideRules.each { ProcessingRule rule ->
            assertTrue("processingParameters rules contains override=${rule}",
                    processingParameters.rules.contains(rule))
        }

        processingParameters.sipProcessingState = testMethodState.sipProcessingState
        FairfaxFilesProcessor.processCollectedFiles(processingParameters, filesForProcessing)
        String sipAsXml = processingParameters.sipProcessingState.sipAsXml

        log.info("${System.lineSeparator()}FairfaxProcessingParameters and SipProcessingState:")
        log.info(processingParameters.detailedDisplay(0, true))
        log.info(System.lineSeparator())

        boolean replaceZeroLengthPdfWithPageUnavailable = processingParameters.rules.contains(ProcessingRule.ZeroLengthPdfReplacedWithPageUnavailablePdf)
        int expectedNumberOfFilesProcessed = 10
        int expectedNumberOfSipFiles = 10
        int expectedNumberOfThumbnailPageFiles = 10
        int expectedNumberOfValidFiles = 9
        int expectedNumberOfInvalidFiles = 1
        int expectedNumberOfIgnoredFiles = 0
        int expectedNumberOfUnrecognizedFiles = 0
        TestHelper.assertSipProcessingStateFileNumbers(expectedNumberOfFilesProcessed, expectedNumberOfSipFiles,
                expectedNumberOfThumbnailPageFiles, expectedNumberOfValidFiles, expectedNumberOfInvalidFiles,
                expectedNumberOfIgnoredFiles, expectedNumberOfUnrecognizedFiles, testMethodState.sipProcessingState)

        // If a thumbnail page will be generated, then it will always generate because empty file is a processing exception.
        if (processingParameters.options.contains(ProcessingOption.GenerateProcessedPdfThumbnailsPage)) {
            assertTrue("Thumbnail page exists, file=${processingParameters.thumbnailPageFile.normalize()}",
                    Files.exists(processingParameters.thumbnailPageFile))
            // We delete the file because we don't want it sticking around after the test
            // Comment out the following line if you want to view the file
            Files.deleteIfExists(processingParameters.thumbnailPageFile)
        } else {
            assertNull("Thumbnail page DOES NOT exist, file=${processingParameters.thumbnailPageFile}",
                    processingParameters.thumbnailPageFile)
        }

        log.info("SIP validation")
        sipConstructedCorrectly(sipAsXml, replaceZeroLengthPdfWithPageUnavailable)
        log.info("ENDING SIP validation")
        log.info("Process output path=${testMethodState.processOutputInterceptor.path}")
        Path processingStateFilePath = testMethodState.sipProcessingState.toTempFile()
        log.info("sipProcessingState file path=${processingStateFilePath}")
        testMethodState.processOutputInterceptor.stopAndClose()
        // In a normal processing script, the processed files, the processing output and the sipProcessingState file
        // would be moved/copied to a processing completed directory based on the processing state.
    }

    void sipConstructedCorrectly(String sipXml, boolean replaceZeroLengthPdfWithPageUnavailable) {
        SipXmlExtractor sipForValidation = new SipXmlExtractor(sipXml)

        assertTrue("SipXmlExtractor has content", sipForValidation.xml.length() > 0)

        assertTrue("SipProcessingState is complete", testMethodState.sipProcessingState.isComplete())
        TestHelper.assertExpectedExceptionReason(testMethodState.sipProcessingState, SipProcessingExceptionReasonType.FILE_OF_LENGTH_ZERO)

        TestHelper.assertExpectedSipMetadataValues(sipForValidation, "Test Publication One", "2018", "11", "23",
                IEEntityType.NewspaperIE, "ALMAMMS", "test-mms-id-one", "200",
                "PRESERVATION_MASTER", "VIEW", true, 1)

        TestHelper.assertExpectedSipFileValues(sipForValidation, 1, "TSTPB1-20181123-001.pdf", "TSTPB1-20181123-001.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0001", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 2, "TSTPB1-20181123-002.pdf", "TSTPB1-20181123-002.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0002", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 3, "TSTPB1-20181123-003.pdf", "TSTPB1-20181123-003.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0003", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 4, "TSTPB1-20181123-004.pdf", "TSTPB1-20181123-004.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0004", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 5, "TSTPB1-20181123-005.pdf", "TSTPB1-20181123-005.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0005", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 6, "TSTPB1-20181123-006.pdf", "TSTPB1-20181123-006.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0006", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 7, "TSTPB1-20181123-007.pdf", "TSTPB1-20181123-007.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0007", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 8, "TSTPB1-20181123-008.pdf", "TSTPB1-20181123-008.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0008", "application/pdf")

        if (replaceZeroLengthPdfWithPageUnavailable) {
            TestHelper.assertExpectedSipFileValues(sipForValidation, 9, "TSTPB1-20181123-009.pdf", "TSTPB1-20181123-009.pdf",
                    15694L, "MD5", "c7670674e8304d565e40f3da43ad65c5", "0009", "application/pdf")
        } else {
            TestHelper.assertExpectedSipFileValues(sipForValidation, 9, "TSTPB1-20181123-009.pdf", "TSTPB1-20181123-009.pdf",
                    0L, "MD5", "d41d8cd98f00b204e9800998ecf8427e", "0009", "application/pdf")
        }

        TestHelper.assertExpectedSipFileValues(sipForValidation, 10, "TSTPB1-20181123-010.pdf", "TSTPB1-20181123-010.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0010", "application/pdf")
    }

}
