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
class ForeverProjectPropertyOrLifeTest {
    // TODO Make this processing simpler
    // - given a starting folder
    // - and a set of selection criteria
    // - create SIPs for the given files
    static String ID_COLUMN_NAME = "MMSID"

    static final String RESOURCES_FOLDER = "ingestion-files-tests/scenario-forever-project-property-or-life"
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
        List<FairfaxProcessingParameters> parametersList = FairfaxProcessingParameters.build("PRS",
                [ ProcessingType.ParentGrouping ], sourceFolder, processingDate, testMethodState.fairfaxSpreadsheet,
                [ ProcessingRule.AllSectionsInSipRequired ], [ ])

        assertThat("Only a single FairfaxProcessingParameters is returned, size=${parametersList.size()}",
                parametersList.size(), is(1))

        FairfaxProcessingParameters processingParameters = parametersList.first()

        assertThat("Multiple section codes: 'PB1', 'BOO', 'AAT'", processingParameters.sectionCodes,
                is([ 'PB1', 'BOO', 'AAT' ]))

        processingParameters.sipProcessingState = testMethodState.sipProcessingState
        FairfaxFilesProcessor.processCollectedFiles(processingParameters, filesForProcessing)
        String sipAsXml = processingParameters.sipProcessingState.sipAsXml

        log.info("${System.lineSeparator()}FairfaxProcessingParameters and SipProcessingState:")
        log.info(processingParameters.detailedDisplay(0, true))
        log.info(System.lineSeparator())

        int expectedNumberOfFilesProcessed = 21
        int expectedNumberOfSipFiles = 21
        int expectedNumberOfThumbnailPageFiles = 21
        int expectedNumberOfValidFiles = 21
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

        TestHelper.assertExpectedSipMetadataValues(sipForValidation, "Test Publication One", "2018", "11", "23",
                IEEntityType.NewspaperIE, "ALMAMMS", "test-mms-id-one", "200",
                "PRESERVATION_MASTER", "VIEW", true, 1)

        TestHelper.assertExpectedSipFileValues(sipForValidation, 1, "PRSPB1-20181123-001.pdf", "PRSPB1-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0001", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 2, "PRSPB1-20181123-002.pdf", "PRSPB1-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0002", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 3, "PRSPB1-20181123-A01with-a-qualifier.pdf", "PRSPB1-20181123-A01with-a-qualifier.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0003", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 4, "PRSPB1-20181123-A02.pdf", "PRSPB1-20181123-A02.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0004", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 5, "PRSPB1-20181123-B01.pdf", "PRSPB1-20181123-B01.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0005", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 6, "PRSPB1-20181123-B02.pdf", "PRSPB1-20181123-B02.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0006", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 7, "PRSPB1-20181123-C01.pdf", "PRSPB1-20181123-C01.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0007", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 8, "PRSBOO-20181123-001.pdf", "PRSBOO-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0008", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 9, "PRSBOO-20181123-002.pdf", "PRSBOO-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0009", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 10, "PRSAAT-20181123-P01.pdf", "PRSAAT-20181123-P01.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0010", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 11, "PRSAAT-20181123-P02.pdf", "PRSAAT-20181123-P02.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0011", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 12, "LIPAAT-20181123-001.pdf", "LIPAAT-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0012", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 13, "LIPAAT-20181123-002.pdf", "LIPAAT-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0013", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 14, "LIPAAT-20181123-003.pdf", "LIPAAT-20181123-003.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0014", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 15, "HOCAAT-20181123-001.pdf", "HOCAAT-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0015", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 16, "HOCAAT-20181123-002.pdf", "HOCAAT-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0016", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 17, "HOCAAT-20181123-003.pdf", "HOCAAT-20181123-003.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0017", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 18, "FPPAAT-20181123-001.pdf", "FPPAAT-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0018", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 19, "FPDAAT-20181123-002.pdf", "FPDAAT-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0019", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 20, "FPDAAT-20181123-003.pdf", "FPDAAT-20181123-003.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0020", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 21, "FPPAAT-20181123-004.pdf", "FPPAAT-20181123-004.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0021", "application/pdf")

    }

}
