package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.SipFileWrapperFactory
import nz.govt.natlib.tools.sip.extraction.SipXmlExtractor
import nz.govt.natlib.tools.sip.files.FilesFinder
import nz.govt.natlib.tools.sip.generation.SipXmlGenerator
import nz.govt.natlib.tools.sip.generation.parameters.Spreadsheet
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

/**
 * Tests the {@code series-sequential} scenario.
 *
 * Note that this test is complicated by the files either being part of a directory structure or in a resource file (jar),
 * so the {@link TestHelper} class is used to handle both scenarios. In real-life processing the files would be on the
 * filesystem and not in a resource. We explicitly use only filesystem files in
 * {@link #correctlyAssembleSipFromFilesOnFilesystem} (as an example to script writers), but this unit test is
 * ignored for builds.
 */
@RunWith(MockitoJUnitRunner.class)
@Slf4j
class SeriesSequentialTest {
    // TODO Make this processing simpler
    // - given a starting folder
    // - and a set of selection criteria
    // - create SIPs for the given files
    static String ID_COLUMN_NAME = "MMSID"

    static final String RESOURCES_FOLDER = "ingestion-files-tests/scenario-series-sequential"
    static final String IMPORT_PARAMETERS_FILENAME = "test-fairfax-import-parameters.json"

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
        String localPath = "src/test/resources/${RESOURCES_FOLDER}"

        File spreadsheetFile = new File("${localPath}/${IMPORT_PARAMETERS_FILENAME}")
        Spreadsheet spreadsheet = Spreadsheet.fromJson(ID_COLUMN_NAME, spreadsheetFile.text, true, true)
        FairfaxSpreadsheet fairfaxSpreadsheet = new FairfaxSpreadsheet(spreadsheet)

        // TODO A more complicated pattern -- date and other masks?
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        List<File> filesForProcessing
        Path filesPath = Paths.get(localPath)
        if (!Files.exists(filesPath) || !Files.isDirectory(filesPath)) {
            log.warn("Path '${filesPath}' does not exist is not a directory. Returning empty file list.")
        } else {
            filesForProcessing = FilesFinder.getMatchingFiles(filesPath, isRegexNotGlob, matchFilenameOnly, ".*?\\.pdf")
        }

        println("Collected ${filesForProcessing.size()} files for processing")
        filesForProcessing.each { File file ->
            println("File for processing=${file.getCanonicalPath()}")
        }

        processCollectedFiles(fairfaxSpreadsheet, filesForProcessing)
    }

    @Test
    void correctlyAssembleSipFromFiles() {
        String resourcePath = "${RESOURCES_FOLDER}"
        String localPath = "src/test/resources/${RESOURCES_FOLDER}"

        FairfaxSpreadsheet fairfaxSpreadsheet = TestHelper.loadSpreadsheet(resourcePath, localPath, IMPORT_PARAMETERS_FILENAME, ID_COLUMN_NAME)

        // TODO A more complicated pattern -- date and other masks?
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        List<File> filesForProcessing = TestHelper.findFiles(resourcePath, localPath, isRegexNotGlob, matchFilenameOnly, ".*?\\.pdf")

        println("Collected ${filesForProcessing.size()} files for processing")
        filesForProcessing.each { File file ->
            println("File for processing=${file.getCanonicalPath()}")
        }

        processCollectedFiles(fairfaxSpreadsheet, filesForProcessing)
    }

    void processCollectedFiles(FairfaxSpreadsheet fairfaxSpreadsheet, List<File> filesForProcessing) {
        println("STARTING processFiles")

        Map<FairfaxFileGroupKey, FairfaxFileGroup> fairfaxFileGroupMap = [:]
        filesForProcessing.each { File rawFile ->
            FairfaxFile fairfaxFile = new FairfaxFile(rawFile)
            println("Processing fairfaxFile=${fairfaxFile}")
            if (fairfaxFile.isValid()) {
                FairfaxFileGroupKey fairfaxFileKey = FairfaxFileGroupKey.fromFairfaxFile(fairfaxFile)
                println("fairfaxFileKey=${fairfaxFileKey}")
                FairfaxFileGroup fairfaxFileGroup = fairfaxFileGroupMap.get(fairfaxFileKey)
                if (fairfaxFileGroup == null) {
                    fairfaxFileGroup = new FairfaxFileGroup(fairfaxFileKey)
                    fairfaxFileGroupMap.put(fairfaxFileKey, fairfaxFileGroup)
                }
                fairfaxFileGroup.addFile(fairfaxFile)
            } else {
                println("FairfaxFile=${fairfaxFile} is NOT valid.")
            }
        }
        // Find the publication (ultimately the MMSID) associated with this set of files.
        println("FINDING publication associated with the files")
        Integer filesProcessed = 0
        boolean allowZeroRatio = true
        fairfaxFileGroupMap.each { FairfaxFileGroupKey fairfaxFileGroupKey, FairfaxFileGroup fairfaxFileGroup ->
            println("Checking fairfaxFileGroupKey=${fairfaxFileGroupKey}, fairfaxFileGroup=${fairfaxFileGroup}")
            FairfaxFileGroupMatch fairfaxFileGroupMatch = FairfaxFileGroupMatcher.mostLikelyMatch(fairfaxFileGroup,
                    fairfaxSpreadsheet, allowZeroRatio)
            if (fairfaxFileGroupMatch != null) {
                println("Will process fairfaxFileGroup=${fairfaxFileGroup} according to sip=${fairfaxFileGroupMatch.sip}")
                List<FairfaxFile> fairfaxFiles = fairfaxFileGroup.files.sort()
                List<Sip.FileWrapper> fileWrappers = fairfaxFiles.collect() { FairfaxFile fairfaxFile ->
                    SipFileWrapperFactory.generate(fairfaxFile.file, true, true)
                }
                int sequenceNumber = 1
                fileWrappers.each { Sip.FileWrapper fileWrapper ->
                    String label = String.format("%03d", sequenceNumber)
                    fileWrapper.label = label
                    sequenceNumber += 1
                    filesProcessed += 1
                }
                Sip testSip = fairfaxFileGroupMatch.sip.clone()
                testSip.fileWrappers = fileWrappers
                SipXmlGenerator sipXmlGenerator = new SipXmlGenerator(testSip)
                String sipAsXml = sipXmlGenerator.getSipAsXml()
                println("\n* * *   S I P   * * *")
                println(sipAsXml)
                println("\n* * *   E N D   O F   S I P   * * *")
                println("STARTING SIP validation")
                seriesSequentialConstructedCorrectly(sipAsXml)
                println("ENDING SIP validation")
            } else {
                // We can't process the files
                throw new RuntimeException("Unable to process fairfaxFileGroup=${fairfaxFileGroup}: No matching sip")
            }
            println("ENDING processing")
        }
        assertThat("9 files should have been processed", filesProcessed, is(10))
    }

    void seriesSequentialConstructedCorrectly(String sipXml) {
        SipXmlExtractor sipForValidation = new SipXmlExtractor(sipXml)

        assertTrue("SipXmlExtractor has content", sipForValidation.xml.length() > 0)

        TestHelper.assertExpectedSipMetadataValues(sipForValidation, "Test Publication One", 2018, 11, 23,
                "NewspaperIE", "ALMAMMS", "test-mms-id-one", "200", "PRESERVATION_MASTER", "VIEW", true, 1)

        TestHelper.assertExpectedSipFileValues(sipForValidation, 1, "TSTPB1-20181123-001.pdf", "TSTPB1-20181123-001.pdf",
                11438L, "MD5", "b8b673eeaa076ff19501318a27f85e9c", "001", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 2, "TSTPB1-20181123-002.pdf", "TSTPB1-20181123-002.pdf",
                11437L, "MD5", "df39cff17991188d9994ff94bddf3985", "002", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 3, "TSTPB1-20181123-003.pdf", "TSTPB1-20181123-003.pdf",
                11657L, "MD5", "1533ab07ff8620fffaec83a2afd92170", "003", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 4, "TSTPB1-20181123-004.pdf", "TSTPB1-20181123-004.pdf",
                11554L, "MD5", "857326c06870577255acd4b21e1a64d7", "004", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 5, "TSTPB1-20181123-005.pdf", "TSTPB1-20181123-005.pdf",
                11605L, "MD5", "02e254147945f60a6a2be1c35ae0689e", "005", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 6, "TSTPB1-20181123-006.pdf", "TSTPB1-20181123-006.pdf",
                11430L, "MD5", "6b932154c4b004a2507d73dc3aaf0736", "006", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 7, "TSTPB1-20181123-007.pdf", "TSTPB1-20181123-007.pdf",
                11543L, "MD5", "a7ceb9001aab17e78cfaf1559f130071", "007", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 8, "TSTPB1-20181123-008.pdf", "TSTPB1-20181123-008.pdf",
                11436L, "MD5", "449dc86bd38979d10c8fb6c3b375a467", "008", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 9, "TSTPB1-20181123-009.pdf", "TSTPB1-20181123-009.pdf",
                11612L, "MD5", "fee5322aa8d3c7a4fe7adeba7953e071", "009", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 10, "TSTPB1-20181123-010.pdf", "TSTPB1-20181123-010.pdf",
                11440L, "MD5", "f621c3081711e895d8fa3d2dd5e49ffa", "010", "application/pdf")
    }

}