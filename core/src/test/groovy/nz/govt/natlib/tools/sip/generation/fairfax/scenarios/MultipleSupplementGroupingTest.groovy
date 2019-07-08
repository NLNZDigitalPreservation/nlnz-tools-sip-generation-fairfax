package nz.govt.natlib.tools.sip.generation.fairfax.scenarios

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.IEEntityType
import nz.govt.natlib.tools.sip.extraction.SipXmlExtractor
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.SipFactory
import nz.govt.natlib.tools.sip.generation.fairfax.TestHelper
import nz.govt.natlib.tools.sip.generation.fairfax.TestHelper.TestMethodState
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType
import nz.govt.natlib.tools.sip.generation.fairfax.processor.FairfaxFilesProcessor
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType
import nz.govt.natlib.tools.sip.state.SipProcessingState
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
 * Tests the {@code multiple-supplement-grouping} scenario.
 *
 * Note that this test is complicated by the files either being part of a directory structure or in a resource file (jar),
 * so the {@link TestHelper} class is used to handle both scenarios. In real-life processing the files would be on the
 * filesystem and not in a resource. We explicitly use only filesystem files in
 * {@link #correctlyAssembleSipFromFilesOnFilesystem} (as an example to script writers), but this unit test is
 * ignored for builds.
 */
@RunWith(MockitoJUnitRunner.class)
@Log4j2
class MultipleSupplementGroupingTest {
    // TODO Make this processing simpler
    // - given a starting folder
    // - and a set of selection criteria
    // - create SIPs for the given files
    static String ID_COLUMN_NAME = "MMSID"

    static final String RESOURCES_FOLDER = "ingestion-files-tests/scenario-multiple-supplement-grouping"
    static final String IMPORT_PARAMETERS_FILENAME = "test-fairfax-import-parameters.json"

    static final String MMSID_SUP_AND_BEE = "test-mms-id-one-SUP+BEE-CHILD"
    static final String MMSID_SUP = "test-mms-id-one-SUP-CHILD"
    static final String MMSID_BEE_AND_ZOP = "test-mms-id-one-BEE+ZOP-CHILD"

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
        TestHelper.initializeTestMethod(testMethodState, "MultipleSupplementGroupingTest-", forLocalFilesystem)

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
        TestHelper.initializeTestMethod(testMethodState, "MultipleSupplementGroupingTest-", forLocalFilesystem)

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
        List<FairfaxProcessingParameters> parametersList = FairfaxProcessingParameters.build("TST",
                [ ProcessingType.SupplementGrouping ], sourceFolder, processingDate, testMethodState.fairfaxSpreadsheet)

        assertThat("FairfaxProcessingParameters matching each edition is returned, size=${parametersList.size()}",
                parametersList.size(), is(3))

        SipProcessingState originalSipProcessingState = testMethodState.sipProcessingState

        parametersList.each { FairfaxProcessingParameters processingParameters ->
            String mmsId = processingParameters.mmsid
            switch (mmsId) {
                case MMSID_SUP_AND_BEE:
                    assertThat("Matches section codes: 'SUP', 'BEE'", processingParameters.sectionCodes,
                            is([ 'SUP', 'BEE' ]))
                    break
                case MMSID_SUP:
                    assertThat("Matches section codes: 'SUP'", processingParameters.sectionCodes,
                            is([ 'SUP' ]))
                    break
                case MMSID_BEE_AND_ZOP:
                    assertThat("Matches section codes: 'BEE', 'ZOP'", processingParameters.sectionCodes,
                            is([ 'BEE', 'ZOP' ]))
                    break
                default:
                    assertTrue("Unmatched parameters for mmsId=${mmsId}, processingParameters=${processingParameters}", false)
                    break
            }

            testMethodState.sipProcessingState = originalSipProcessingState.clone()
            processingParameters.sipProcessingState = testMethodState.sipProcessingState
            FairfaxFilesProcessor.processCollectedFiles(processingParameters, filesForProcessing)
            String sipAsXml = processingParameters.sipProcessingState.sipAsXml

            log.info("${System.lineSeparator()}FairfaxProcessingParameters and SipProcessingState:")
            log.info(processingParameters.detailedDisplay(0, true))
            log.info(System.lineSeparator())

            boolean expectedThumbnailFile = false
            switch (mmsId) {
                case MMSID_SUP_AND_BEE:
                    expectedThumbnailFile = processingParameters.options.contains(ProcessingOption.AlwaysGenerateThumbnailPage)
                    assertFalse("For mmsId=${mmsId} skip=${processingParameters.skip} (expected false)", processingParameters.skip)
                    expectedSizingSUPandBEE()
                    break
                case MMSID_SUP:
                    expectedThumbnailFile = processingParameters.options.contains(ProcessingOption.AlwaysGenerateThumbnailPage)
                    assertFalse("For mmsId=${mmsId} skip=${processingParameters.skip} (expected false)", processingParameters.skip)
                    expectedSizingSUP()
                    break
                case MMSID_BEE_AND_ZOP:
                    expectedThumbnailFile = false
                    assertTrue("For mmsId=${mmsId} skip=${processingParameters.skip} (expected true)", processingParameters.skip)
                    break
                default:
                    assertFalse("Unrecognized mmsId=${mmsId}", true)
                    break
            }

            if (processingParameters.options.contains(ProcessingOption.GenerateProcessedPdfThumbnailsPage)) {
                if (expectedThumbnailFile) {
                    assertTrue("Thumbnail page exists, file=${processingParameters.thumbnailPageFile.normalize()}",
                            Files.exists(processingParameters.thumbnailPageFile))
                    // We delete the file because we don't want it sticking around after the test
                    // Comment out the following line if you want to view the file
                    Files.deleteIfExists(processingParameters.thumbnailPageFile)
                } else {
                    assertNull("Thumbnail page DOES NOT exist, file=${processingParameters.thumbnailPageFile}",
                            processingParameters.thumbnailPageFile)
                }
            } else {
                assertNull("Thumbnail page DOES NOT exist, file=${processingParameters.thumbnailPageFile}",
                        processingParameters.thumbnailPageFile)
            }

            log.info("STARTING SIP validation")
            switch (mmsId) {
                case MMSID_SUP_AND_BEE:
                    sipConstructedCorrectlySUPandBEE(sipAsXml)
                    break
                case MMSID_SUP:
                    sipConstructedCorrectlySUP(sipAsXml)
                    break
                case MMSID_BEE_AND_ZOP:
                    assertThat("No sipAsXML for skipped processing", sipAsXml, is(SipProcessingState.EMPTY_SIP_AS_XML))
                    break
                default:
                    assertFalse("Unrecognized mmsid=${mmsId}", true)
                    break
            }

            log.info("ENDING SIP validation")
            log.info("Process output path=${testMethodState.processOutputInterceptor.path}")
            Path processingStateFilePath = testMethodState.sipProcessingState.toTempFile()
            log.info("sipProcessingState file path=${processingStateFilePath}")
            testMethodState.processOutputInterceptor.stopAndClose()
            // In a normal processing script, the processed files, the processing output and the sipProcessingState file
            // would be moved/copied to a processing completed directory based on the processing state.
        }
    }

    void expectedSizingSUPandBEE() {
        int expectedNumberOfFilesProcessed = 5
        int expectedNumberOfSipFiles = 5
        int expectedNumberOfThumbnailPageFiles = 5
        int expectedNumberOfValidFiles = 5
        int expectedNumberOfInvalidFiles = 0
        int expectedNumberOfIgnoredFiles = 2
        int expectedNumberOfUnrecognizedFiles = 0
        TestHelper.assertSipProcessingStateFileNumbers(expectedNumberOfFilesProcessed, expectedNumberOfSipFiles,
                expectedNumberOfThumbnailPageFiles, expectedNumberOfValidFiles, expectedNumberOfInvalidFiles,
                expectedNumberOfIgnoredFiles, expectedNumberOfUnrecognizedFiles, testMethodState.sipProcessingState)

    }

    void expectedSizingSUP() {
        int expectedNumberOfFilesProcessed = 3
        int expectedNumberOfSipFiles = 3
        int expectedNumberOfThumbnailPageFiles = 3
        int expectedNumberOfValidFiles = 3
        int expectedNumberOfInvalidFiles = 0
        int expectedNumberOfIgnoredFiles = 4
        int expectedNumberOfUnrecognizedFiles = 0
        TestHelper.assertSipProcessingStateFileNumbers(expectedNumberOfFilesProcessed, expectedNumberOfSipFiles,
                expectedNumberOfThumbnailPageFiles, expectedNumberOfValidFiles, expectedNumberOfInvalidFiles,
                expectedNumberOfIgnoredFiles, expectedNumberOfUnrecognizedFiles, testMethodState.sipProcessingState)
    }

    void sipConstructedCorrectlySUPandBEE(String sipXml) {
        SipXmlExtractor sipForValidation = new SipXmlExtractor(sipXml)

        assertTrue("SipXmlExtractor has content", sipForValidation.xml.length() > 0)

        assertTrue("SipProcessingState is complete", testMethodState.sipProcessingState.isComplete())
        assertTrue("SipProcessingState is successful", testMethodState.sipProcessingState.isSuccessful())

        TestHelper.assertExpectedSipMetadataValues(sipForValidation, "Test Publication One SUP+BEE CHILD", "2018", "11", "23",
                IEEntityType.NewspaperIE, "ALMAMMS", MMSID_SUP_AND_BEE, "200",
                "PRESERVATION_MASTER", "VIEW", true, 1)

        TestHelper.assertExpectedSipFileValues(sipForValidation, 1, "TSTSUP-20181123-001.pdf", "TSTSUP-20181123-001.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0001", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 2, "TSTSUP-20181123-002.pdf", "TSTSUP-20181123-002.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0002", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 3, "TSTSUP-20181123-003.pdf", "TSTSUP-20181123-003.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0003", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 4, "TSTBEE-20181123-001.pdf", "TSTBEE-20181123-001.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0004", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 5, "TSTBEE-20181123-002.pdf", "TSTBEE-20181123-002.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0005", "application/pdf")
    }

    void sipConstructedCorrectlySUP(String sipXml) {
        SipXmlExtractor sipForValidation = new SipXmlExtractor(sipXml)

        assertTrue("SipXmlExtractor has content", sipForValidation.xml.length() > 0)

        assertTrue("SipProcessingState is complete", testMethodState.sipProcessingState.isComplete())
        assertTrue("SipProcessingState is successful", testMethodState.sipProcessingState.isSuccessful())

        TestHelper.assertExpectedSipMetadataValues(sipForValidation, "Test Publication One SUP CHILD", "2018", "11", "23",
                IEEntityType.NewspaperIE, "ALMAMMS", MMSID_SUP, "200",
                "PRESERVATION_MASTER", "VIEW", true, 1)

        TestHelper.assertExpectedSipFileValues(sipForValidation, 1, "TSTSUP-20181123-001.pdf", "TSTSUP-20181123-001.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0001", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 2, "TSTSUP-20181123-002.pdf", "TSTSUP-20181123-002.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0002", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 3, "TSTSUP-20181123-003.pdf", "TSTSUP-20181123-003.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0003", "application/pdf")
    }

}
