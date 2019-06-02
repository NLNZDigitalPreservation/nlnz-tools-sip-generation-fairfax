package nz.govt.natlib.tools.sip.generation.fairfax.scenarios

import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.IEEntityType
import nz.govt.natlib.tools.sip.extraction.SipXmlExtractor
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFilesProcessor
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.TestHelper
import nz.govt.natlib.tools.sip.generation.fairfax.TestHelper.TestMethodState
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

import java.nio.file.Path
import java.time.LocalDate

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

/**
 * Tests the {@code all-sections-required-but-some-ignored} scenario.
 *
 * Note that this test is complicated by the files either being part of a directory structure or in a resource file (jar),
 * so the {@link TestHelper} class is used to handle both scenarios. In real-life processing the files would be on the
 * filesystem and not in a resource. We explicitly use only filesystem files in
 * {@link #correctlyAssembleSipFromFilesOnFilesystem} (as an example to script writers), but this unit test is
 * ignored for builds.
 */
@RunWith(MockitoJUnitRunner.class)
@Slf4j
class AllSectionCodesRequiredTest {
    // TODO Make this processing simpler
    // - given a starting folder
    // - and a set of selection criteria
    // - create SIPs for the given files
    static String ID_COLUMN_NAME = "MMSID"

    static final String RESOURCES_FOLDER = "ingestion-files-tests/scenario-multiple-section-codes"
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
        TestHelper.initializeTestMethod(testMethodState, "AllSectionCodesRequiredTest-", forLocalFilesystem)

        // TODO A more complicated pattern -- date and other masks?
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        List<File> filesForProcessing = TestHelper.getFilesForProcessingFromFileSystem(isRegexNotGlob, matchFilenameOnly,
                sortFiles, testMethodState.localPath, ".*?\\.[pP]{1}[dD]{1}[fF]{1}")

        processFiles(filesForProcessing)
    }

    @Test
    void correctlyAssembleSipFromFiles() {
        boolean forLocalFilesystem = false
        TestHelper.initializeTestMethod(testMethodState, "AllSectionCodesRequiredTest-", forLocalFilesystem)

        // TODO A more complicated pattern -- date and other masks?
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        List<File> filesForProcessing = TestHelper.getFilesForProcessingFromResource(isRegexNotGlob, matchFilenameOnly,
                sortFiles, testMethodState.resourcePath, testMethodState.localPath, ".*?\\.[pP]{1}[dD]{1}[fF]{1}")

        processFiles(filesForProcessing)
    }

    void processFiles(List<File> filesForProcessing) {
        String dateString = "20181123"
        LocalDate processingDate = LocalDate.parse(dateString, FairfaxFile.LOCAL_DATE_TIME_FORMATTER)

        FairfaxProcessingParameters processingParameters = FairfaxProcessingParameters.build("TST",
                ProcessingType.ParentGrouping, processingDate, testMethodState.fairfaxSpreadsheet)
        processingParameters.processingRules = ProcessingRule.mergeOverrides(processingParameters.processingRules,
                [ ProcessingRule.AllSectionsInSipRequired ])

        assertThat("Multiple section codes: 'PB1', 'BOO', 'ZOO', 'AAT'", processingParameters.sectionCodes,
                is([ 'PB1', 'BOO', 'ZOO', 'AAT' ]))

        processingParameters.sipProcessingState = testMethodState.sipProcessingState
        String sipAsXml = FairfaxFilesProcessor.processCollectedFiles(processingParameters, filesForProcessing)

        log.info("START FairfaxProcessingParameters and SipProcessingState:")
        log.info(processingParameters.detailedDisplay(0, true))
        log.info("END FairfaxProcessingParameters and SipProcessingState")

        int expectedNumberOfFilesProcessed = 11
        assertThat("${expectedNumberOfFilesProcessed} files should have been processed",
                testMethodState.sipProcessingState.totalFilesProcessed, is(expectedNumberOfFilesProcessed))
        int expectedNumberOfValidFiles = 11
        assertThat("${expectedNumberOfValidFiles} valid files should have been processed",
                testMethodState.sipProcessingState.validFiles.size(), is(expectedNumberOfValidFiles))
        int expectedNumberOfInvalidFiles = 0
        assertThat("${expectedNumberOfInvalidFiles} invalid files should have been processed",
                testMethodState.sipProcessingState.invalidFiles.size(), is(expectedNumberOfInvalidFiles))
        int expectedNumberOfIgnoredFiles = 2
        assertThat("${expectedNumberOfIgnoredFiles} ignored files should have been processed",
                testMethodState.sipProcessingState.ignoredFiles.size(), is(expectedNumberOfIgnoredFiles))
        assertThat("First ignored file is 'TSTQEE-20181123-001.pdf'",
                testMethodState.sipProcessingState.ignoredFiles.first().getName(), is("TSTQEE-20181123-001.pdf"))
        int expectedNumberOfUnrecognizedFiles = 0
        assertThat("${expectedNumberOfUnrecognizedFiles} unrecognized files should have been processed",
                testMethodState.sipProcessingState.unrecognizedFiles.size(), is(expectedNumberOfUnrecognizedFiles))

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
        TestHelper.assertExpectedExceptionReason(testMethodState.sipProcessingState, SipProcessingExceptionReasonType.ALL_FILES_CANNOT_BE_PROCESSED)

        TestHelper.assertExpectedSipMetadataValues(sipForValidation, "Test Publication One", 2018, 11, 23,
                IEEntityType.NewspaperIE, "ALMAMMS", "test-mms-id-one", "200",
                "PRESERVATION_MASTER", "VIEW", true, 1)

        TestHelper.assertExpectedSipFileValues(sipForValidation, 1, "TSTPB1-20181123-001.pdf", "TSTPB1-20181123-001.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0001", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 2, "TSTPB1-20181123-002.pdf", "TSTPB1-20181123-002.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0002", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 3, "TSTPB1-20181123-A01with-a-qualifier.pdf", "TSTPB1-20181123-A01with-a-qualifier.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0003", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 4, "TSTPB1-20181123-A02.pdf", "TSTPB1-20181123-A02.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0004", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 5, "TSTPB1-20181123-B01.pdf", "TSTPB1-20181123-B01.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0005", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 6, "TSTPB1-20181123-B02.pdf", "TSTPB1-20181123-B02.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0006", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 7, "TSTPB1-20181123-C01.pdf", "TSTPB1-20181123-C01.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0007", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 8, "TSTBOO-20181123-001.pdf", "TSTBOO-20181123-001.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0008", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 9, "TSTBOO-20181123-002.pdf", "TSTBOO-20181123-002.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0009", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 10, "TSTAAT-20181123-P01.pdf", "TSTAAT-20181123-P01.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0010", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 11, "TSTAAT-20181123-P02.pdf", "TSTAAT-20181123-P02.pdf",
                739L, "MD5", "b5808604069f9f61d94e0660409616ba", "0011", "application/pdf")
    }

}