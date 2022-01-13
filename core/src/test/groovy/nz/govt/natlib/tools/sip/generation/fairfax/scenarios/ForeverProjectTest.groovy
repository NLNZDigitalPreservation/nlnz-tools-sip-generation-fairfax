package nz.govt.natlib.tools.sip.generation.fairfax.scenarios

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.IEEntityType
import nz.govt.natlib.tools.sip.extraction.SipXmlExtractor
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.TestHelper
import nz.govt.natlib.tools.sip.generation.fairfax.TestHelper.TestMethodState
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType
import nz.govt.natlib.tools.sip.generation.fairfax.processor.FairfaxFilesProcessor
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
import static org.junit.Assert.*

/**
 * Tests the {@code forever-project} scenario.
 *
 * Note that this test is complicated by the files either being part of a directory structure or in a resource file (jar),
 * so the {@link TestHelper} class is used to handle both scenarios. In real-life processing the files would be on the
 * filesystem and not in a resource. We explicitly use only filesystem files in
 * {@link #correctlyAssembleSipFromFilesOnFilesystem} (as an example to script writers), but this unit test is
 * ignored for builds.
 */
@RunWith(MockitoJUnitRunner.class)
@Log4j2
class ForeverProjectTest {
    // TODO Make this processing simpler
    // - given a starting folder
    // - and a set of selection criteria
    // - create SIPs for the given files
    static String ID_COLUMN_NAME = "MMSID"

    static final String RESOURCES_FOLDER = "ingestion-files-tests/scenario-forever-project"
    static final String IMPORT_PARAMETERS_FILENAME = "test-fairfax-import-parameters.json"

    TestMethodState testMethodState

    @Before
    void setup() {
        testMethodState = new TestMethodState(ID_COLUMN_NAME, RESOURCES_FOLDER, IMPORT_PARAMETERS_FILENAME)
    }

    /**
     * Note to developers: Ensure that this is exactly the same test as {@link #correctlyAssembleSipFromFiles()}, except
     * that this test only reads from the file system, not a resource file.
     *
     * This test should use the local filesystem when running from within an IDE.
     *
     * This test then becomes a starting point for scripts that create and process SIPs.
     */
    @Test
    // TODO Ignore this test before making a code commit
    @Ignore
    void correctlyAssembleSipFromFilesOnFilesystem() {
        boolean forLocalFilesystem = true
        TestHelper.initializeTestMethod(testMethodState, "ForeverProjectTest-", forLocalFilesystem)

        // TODO A more complicated pattern -- date and other masks?
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        List<Path> filesForProcessing = TestHelper.getFilesForProcessingFromFileSystem(isRegexNotGlob, matchFilenameOnly,
                sortFiles, testMethodState.localPath, ".*?\\.[pP]{1}[dD]{1}[fF]{1}")

        processFiles(filesForProcessing)
    }

    @Test
    void correctlyAssembleSipFromFiles() {
        boolean forLocalFilesystem = false
        TestHelper.initializeTestMethod(testMethodState, "ForeverProjectTest-", forLocalFilesystem)

        // TODO A more complicated pattern -- date and other masks?
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        List<Path> filesForProcessing = TestHelper.getFilesForProcessingFromResource(isRegexNotGlob, matchFilenameOnly,
                sortFiles, testMethodState.resourcePath, testMethodState.localPath, ".*?\\.[pP]{1}[dD]{1}[fF]{1}")

        processFiles(filesForProcessing)
    }

    void processFiles(List<Path> filesForProcessing) {
        String dateString = "20181123"
        LocalDate processingDate = LocalDate.parse(dateString, FairfaxFile.LOCAL_DATE_TIME_FORMATTER)

        Path sourceFolder = Path.of(testMethodState.localPath)
        List<FairfaxProcessingParameters> parametersList = FairfaxProcessingParameters.build("SUS",
                [ ProcessingType.ParentGrouping ], sourceFolder, processingDate, testMethodState.fairfaxSpreadsheet,
                [ ProcessingRule.AllSectionsInSipRequired ], [ ])

        assertThat("Only a single FairfaxProcessingParameters is returned, size=${parametersList.size()}",
                parametersList.size(), is(1))

        FairfaxProcessingParameters processingParameters = parametersList.first()

        assertThat("Multiple section codes: 'ED1', 'TAB', 'QFS'", processingParameters.sectionCodes,
                is([ 'ED1', 'TAB', 'QFS' ]))

        processingParameters.sipProcessingState = testMethodState.sipProcessingState
        FairfaxFilesProcessor.processCollectedFiles(processingParameters, filesForProcessing)
        String sipAsXml = processingParameters.sipProcessingState.sipAsXml

        log.info("${System.lineSeparator()}FairfaxProcessingParameters and SipProcessingState:")
        log.info(processingParameters.detailedDisplay(0, true))
        log.info(System.lineSeparator())

        int expectedNumberOfFilesProcessed = 8
        int expectedNumberOfSipFiles = 8
        int expectedNumberOfThumbnailPageFiles = 8
        int expectedNumberOfValidFiles = 8
        int expectedNumberOfInvalidFiles = 0
        int expectedNumberOfIgnoredFiles = 0
        int expectedNumberOfUnrecognizedFiles = 0
        TestHelper.assertSipProcessingStateFileNumbers(expectedNumberOfFilesProcessed, expectedNumberOfSipFiles,
                expectedNumberOfThumbnailPageFiles, expectedNumberOfValidFiles, expectedNumberOfInvalidFiles,
                expectedNumberOfIgnoredFiles, expectedNumberOfUnrecognizedFiles, testMethodState.sipProcessingState)

        log.info("STARTING SIP validation")
        sipConstructedCorrectly(sipAsXml)
        log.info("ENDING SIP validation")
        log.info("Process output path=${testMethodState.processOutputInterceptor.path}")
        Path processingStateFilePath = testMethodState.sipProcessingState.toTempFile()
        log.info("sipProcessingState file path=${processingStateFilePath}")
        testMethodState.processOutputInterceptor.stopAndClose()
        // In a normal processing script, the processed files, the processing output and the sipProcessingState file
        // would be moved/copied to a processing completed directory based on the processing state.
    }

    void sipConstructedCorrectly(String sipXml) {
        SipXmlExtractor sipForValidation = new SipXmlExtractor(sipXml)

        assertTrue("SipXmlExtractor has content", sipForValidation.xml.length() > 0)

        assertTrue("SipProcessingState is complete", testMethodState.sipProcessingState.isComplete())

        // Test that no Missing Sequence exceptions are thrown due to the title_codes being different
        assertTrue("SipProcessingState is successful", testMethodState.sipProcessingState.isSuccessful())
        assertTrue("SipProcessingState has no exceptions", testMethodState.sipProcessingState.exceptions.size() == 0)

        TestHelper.assertExpectedSipMetadataValues(sipForValidation, "Test Publication One", "2018", "11", "23 [ED1]",
                IEEntityType.NewspaperIE, "ALMAMMS", "test-mms-id-one", "200",
                "PRESERVATION_MASTER", "VIEW", true, 1)

        TestHelper.assertExpectedSipFileValues(sipForValidation, 1, "SUSED1-20181123-001.pdf", "SUSED1-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0001", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 2, "SUSED1-20181123-002.pdf", "SUSED1-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0002", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 3, "SUSED1-20181123-003.pdf", "SUSED1-20181123-003.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0003", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 4, "SUSED1-20181123-004.pdf", "SUSED1-20181123-004.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0004", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 5, "SUSED1-20181123-005.pdf", "SUSED1-20181123-005.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0005", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 6, "FPSED1-20181123-001.pdf", "FPSED1-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0006", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 7, "FPSED1-20181123-002.pdf", "FPSED1-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0007", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 8, "FPSED1-20181123-003.pdf", "FPSED1-20181123-003.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0008", "application/pdf")

    }

}
