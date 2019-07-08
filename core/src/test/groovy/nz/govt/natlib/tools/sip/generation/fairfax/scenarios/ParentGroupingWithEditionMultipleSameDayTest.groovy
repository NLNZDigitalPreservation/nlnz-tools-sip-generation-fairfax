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
 * Tests the {@code parent-grouping-with-edition-multiple-same-day} scenario.
 *
 * Note that this test is complicated by the files either being part of a directory structure or in a resource file (jar),
 * so the {@link TestHelper} class is used to handle both scenarios. In real-life processing the files would be on the
 * filesystem and not in a resource. We explicitly use only filesystem files in
 * {@link #correctlyAssembleSipFromFilesOnFilesystem} (as an example to script writers), but this unit test is
 * ignored for builds.
 */
@RunWith(MockitoJUnitRunner.class)
@Log4j2
class ParentGroupingWithEditionMultipleSameDayTest {
    // TODO Make this processing simpler
    // - given a starting folder
    // - and a set of selection criteria
    // - create SIPs for the given files
    static String ID_COLUMN_NAME = "MMSID"

    static final String RESOURCES_FOLDER = "ingestion-files-tests/scenario-parent-grouping-with-edition-multiple-same-day"
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
        TestHelper.initializeTestMethod(testMethodState, "ParentGroupingWithEditionMultipleSameDayTest-", forLocalFilesystem)

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
        TestHelper.initializeTestMethod(testMethodState, "ParentGroupingWithEditionMultipleSameDayTest-", forLocalFilesystem)

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
                [ ProcessingType.ParentGroupingWithEdition, ProcessingType.ParentGrouping, ProcessingType.CreateSipForFolder ],
                sourceFolder, processingDate, testMethodState.fairfaxSpreadsheet)

        assertThat("2 FairfaxProcessingParameters are returned, size=${parametersList.size()}, list=${parametersList}",
                parametersList.size(), is(2))

        SipProcessingState originalSipProcessingState = testMethodState.sipProcessingState

        parametersList.each { FairfaxProcessingParameters currentProcessingParameters ->

            String discriminatorCode = currentProcessingParameters.editionDiscriminators.first()
            switch (discriminatorCode) {
                case "PB1":
                    assertThat("Processing type is ParentGroupingWithEdition", currentProcessingParameters.type,
                            is(ProcessingType.ParentGroupingWithEdition))
                    assertThat("editionDiscriminator matches=PB1", discriminatorCode, is("PB1"))
                    assertThat ( "Multiple section codes: 'PB1', 'BOO', 'ZOO', 'AAT'", currentProcessingParameters.sectionCodes,
                            is ( [ 'PB1', 'BOO', 'ZOO', 'AAT' ] ) )
                    break
                case "PB2":
                    assertThat("Processing type is ParentGroupingWithEdition", currentProcessingParameters.type,
                            is(ProcessingType.ParentGroupingWithEdition))
                    assertThat("editionDiscriminator matches=PB2", discriminatorCode, is("PB2"))
                    assertThat ( "Multiple section codes: 'PB1', 'BOO', 'ZOO'", currentProcessingParameters.sectionCodes,
                            is ( [ 'PB1', 'BOO', 'ZOO' ] ) )
                    break
                default:
                    assertFalse("Unrecognized discriminatorCode=${discriminatorCode}", true)
                    break
            }
            assertTrue ( "Numeric before alpha sequencing",
                    currentProcessingParameters.options.contains (ProcessingOption.NumericBeforeAlphaSequencing))

            testMethodState.sipProcessingState = originalSipProcessingState.clone()
            currentProcessingParameters.sipProcessingState = testMethodState.sipProcessingState
            FairfaxFilesProcessor.processCollectedFiles(currentProcessingParameters, filesForProcessing)
            String sipAsXml = currentProcessingParameters.sipProcessingState.sipAsXml

            assertTrue("Processing rules includes EditionDiscriminatorsUsingSmartSubstitute",
                    currentProcessingParameters.rules.contains(ProcessingRule.EditionDiscriminatorsUsingSmartSubstitute))

            // With rule EditionDiscriminatorsUsingSmartSubstitute, the processing parameters will change depending on
            // the discriminatorCode. This takes place in the ParentGroupingWithEditionProcessor
            switch (discriminatorCode) {
                case "PB1":
                    assertThat("currentEdition matches=PB1", currentProcessingParameters.currentEdition, is("PB1"))
                    assertThat("editionDiscriminators matches=[ PB1 ]", currentProcessingParameters.editionDiscriminators,
                            is([ "PB1" ]))
                    assertThat ( "Multiple section codes: 'PB1', 'BOO', 'ZOO', 'AAT'", currentProcessingParameters.sectionCodes,
                            is ( [ 'PB1', 'BOO', 'ZOO', 'AAT' ] ) )
                    break
                case "PB2":
                    assertThat("currentEdition matches=PB2", currentProcessingParameters.currentEdition, is("PB2"))
                    assertThat("editionDiscriminators matches=[ PB1 PB2 ]", currentProcessingParameters.editionDiscriminators,
                            is([ "PB1", "PB2" ]))
                    assertThat ( "Multiple section codes: 'PB1', 'BOO', 'ZOO'", currentProcessingParameters.sectionCodes,
                            is ( [ 'PB1', 'BOO', 'ZOO' ] ) )
                    break
                default:
                    assertFalse("Unrecognized discriminatorCode=${discriminatorCode}", true)
                    break
            }

            log.info("${System.lineSeparator()}FairfaxProcessingParameters and SipProcessingState:")
            log.info(currentProcessingParameters.detailedDisplay(0, true))
            log.info(System.lineSeparator())

            boolean expectedThumbnailFile = false
            switch (discriminatorCode) {
                case "PB1" :
                    expectedThumbnailFile = currentProcessingParameters.options.contains(ProcessingOption.AlwaysGenerateThumbnailPage)
                    expectedSizingPB1()
                    break
                case "PB2" :
                    expectedThumbnailFile = currentProcessingParameters.options.contains(ProcessingOption.AlwaysGenerateThumbnailPage)
                    expectedSizingPB2()
                    break
                default:
                    assertFalse("Unrecognized discriminatorCode=${discriminatorCode}", true)
                    break
            }

            if (currentProcessingParameters.options.contains(ProcessingOption.GenerateProcessedPdfThumbnailsPage)) {
                if (expectedThumbnailFile) {
                    assertTrue("Thumbnail page exists, file=${currentProcessingParameters.thumbnailPageFile.normalize()}",
                            Files.exists(currentProcessingParameters.thumbnailPageFile))
                    // We delete the file because we don't want it sticking around after the test
                    // Comment out the following line if you want to view the file
                    Files.deleteIfExists(currentProcessingParameters.thumbnailPageFile)
                } else {
                    assertNull("Thumbnail page DOES NOT exist, file=${currentProcessingParameters.thumbnailPageFile}",
                            currentProcessingParameters.thumbnailPageFile)
                }
            } else {
                assertNull("Thumbnail page DOES NOT exist, file=${currentProcessingParameters.thumbnailPageFile}",
                        currentProcessingParameters.thumbnailPageFile)
            }

            log.info("STARTING SIP validation")
            switch (discriminatorCode) {
                case "PB1" :
                    sipConstructedCorrectlyPB1Edition(sipAsXml)
                    break
                case "PB2" :
                    sipConstructedCorrectlyPB2Edition(sipAsXml)
                    break
                case "PB3" :
                    assertThat("Empty string for SIP=${sipAsXml}", sipAsXml, is(""))
                    break
                default:
                    assertFalse("Unrecognized discriminatorCode=${discriminatorCode}", true)
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

    void expectedSizingPB1() {
        int expectedNumberOfFilesProcessed = 13
        int expectedNumberOfSipFiles = 13
        int expectedNumberOfThumbnailPageFiles = 13
        int expectedNumberOfValidFiles = 13
        int expectedNumberOfInvalidFiles = 0
        int expectedNumberOfIgnoredFiles = 6
        int expectedNumberOfUnrecognizedFiles = 0
        TestHelper.assertSipProcessingStateFileNumbers(expectedNumberOfFilesProcessed, expectedNumberOfSipFiles,
                expectedNumberOfThumbnailPageFiles, expectedNumberOfValidFiles, expectedNumberOfInvalidFiles,
                expectedNumberOfIgnoredFiles, expectedNumberOfUnrecognizedFiles, testMethodState.sipProcessingState)
    }

    void expectedSizingPB2() {
        int expectedNumberOfFilesProcessed = 12
        int expectedNumberOfSipFiles = 12
        int expectedNumberOfThumbnailPageFiles = 12
        int expectedNumberOfValidFiles = 12
        int expectedNumberOfInvalidFiles = 0
        int expectedNumberOfIgnoredFiles = 7
        int expectedNumberOfUnrecognizedFiles = 0
        TestHelper.assertSipProcessingStateFileNumbers(expectedNumberOfFilesProcessed, expectedNumberOfSipFiles,
                expectedNumberOfThumbnailPageFiles, expectedNumberOfValidFiles, expectedNumberOfInvalidFiles,
                expectedNumberOfIgnoredFiles, expectedNumberOfUnrecognizedFiles, testMethodState.sipProcessingState)
    }

    void sipConstructedCorrectlyPB1Edition(String sipXml) {
        SipXmlExtractor sipForValidation = new SipXmlExtractor(sipXml)

        assertTrue("SipXmlExtractor has content", sipForValidation.xml.length() > 0)

        assertTrue("SipProcessingState is complete", testMethodState.sipProcessingState.isComplete())
        assertTrue("SipProcessingState is successful", testMethodState.sipProcessingState.isSuccessful())

        TestHelper.assertExpectedSipMetadataValues(sipForValidation, "Test Publication One", "2018", "11", "23 [PB1]",
                IEEntityType.NewspaperIE, "ALMAMMS", "test-mms-id-one", "200",
                "PRESERVATION_MASTER", "VIEW", true, 1)

        TestHelper.assertExpectedSipFileValues(sipForValidation, 1, "TSTPB1-20181123-001.pdf", "TSTPB1-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0001", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 2, "TSTPB1-20181123-002.pdf", "TSTPB1-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0002", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 3, "TSTPB1-20181123-A01with-a-qualifier.pdf", "TSTPB1-20181123-A01with-a-qualifier.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0003", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 4, "TSTPB1-20181123-A02.pdf", "TSTPB1-20181123-A02.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0004", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 5, "TSTPB1-20181123-B01.pdf", "TSTPB1-20181123-B01.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0005", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 6, "TSTPB1-20181123-B02.pdf", "TSTPB1-20181123-B02.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0006", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 7, "TSTPB1-20181123-B03.pdf", "TSTPB1-20181123-B03.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0007", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 8, "TSTPB1-20181123-C01.pdf", "TSTPB1-20181123-C01.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0008", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 9, "TSTPB1-20181123-C02.pdf", "TSTPB1-20181123-C02.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0009", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 10, "TSTBOO-20181123-001.pdf", "TSTBOO-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0010", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 11, "TSTBOO-20181123-002.pdf", "TSTBOO-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0011", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 12, "TSTAAT-20181123-P01.pdf", "TSTAAT-20181123-P01.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0012", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 13, "TSTAAT-20181123-P02.pdf", "TSTAAT-20181123-P02.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0013", "application/pdf")
    }

    void sipConstructedCorrectlyPB2Edition(String sipXml) {
        SipXmlExtractor sipForValidation = new SipXmlExtractor(sipXml)

        assertTrue("SipXmlExtractor has content", sipForValidation.xml.length() > 0)

        assertTrue("SipProcessingState is complete", testMethodState.sipProcessingState.isComplete())
        assertTrue("SipProcessingState is successful", testMethodState.sipProcessingState.isSuccessful())

        TestHelper.assertExpectedSipMetadataValues(sipForValidation, "Test Publication Two", "2018", "11", "23 [PB2]",
                IEEntityType.NewspaperIE, "ALMAMMS", "test-mms-id-two", "200",
                "PRESERVATION_MASTER", "VIEW", true, 1)

        TestHelper.assertExpectedSipFileValues(sipForValidation, 1, "TSTPB2-20181123-001.pdf", "TSTPB2-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0001", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 2, "TSTPB1-20181123-002.pdf", "TSTPB1-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0002", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 3, "TSTPB2-20181123-A01.pdf", "TSTPB2-20181123-A01.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0003", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 4, "TSTPB1-20181123-A02.pdf", "TSTPB1-20181123-A02.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0004", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 5, "TSTPB1-20181123-B01.pdf", "TSTPB1-20181123-B01.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0005", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 6, "TSTPB2-20181123-B02-with-some-other-qualifier.pdf", "TSTPB2-20181123-B02-with-some-other-qualifier.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0006", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 7, "TSTPB1-20181123-B03.pdf", "TSTPB1-20181123-B03.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0007", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 8, "TSTPB2-20181123-B04.pdf", "TSTPB2-20181123-B04.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0008", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 9, "TSTPB1-20181123-C01.pdf", "TSTPB1-20181123-C01.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0009", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 10, "TSTPB1-20181123-C02.pdf", "TSTPB1-20181123-C02.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0010", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 11, "TSTBOO-20181123-001.pdf", "TSTBOO-20181123-001.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0011", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 12, "TSTBOO-20181123-002.pdf", "TSTBOO-20181123-002.pdf",
                636L, "MD5", "7273a4d61a8dab92be4393e2923ad2d2", "0012", "application/pdf")
    }

}
