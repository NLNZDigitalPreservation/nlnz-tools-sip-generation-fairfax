package nz.govt.natlib.tools.sip.generation.fairfax

import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.SipFileWrapperFactory
import nz.govt.natlib.tools.sip.generation.SipXmlGenerator
import nz.govt.natlib.tools.sip.generation.parameters.Spreadsheet
import org.apache.commons.io.FileUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner

import static org.hamcrest.core.Is.is
import static org.junit.Assert.*
import static org.mockito.Mockito.when

/**
 * Tests the {@code series-sequential} scenario.
 */
@RunWith(MockitoJUnitRunner.class)
class SeriesSequentialTest {
    static String ID_COLUMN_NAME = "MMSID"

    static final String RESOURCES_FOLDER = "ingestion-files-tests/scenario-series-sequential"
    static final String IMPORT_PARAMETERS_FILENAME = "test-fairfax-import-parameters.json"

    @Test
    void correctlyAssembleSipFromFiles() {
        Spreadsheet spreadsheet
        List<String> filenames
        String resourcePath = "${RESOURCES_FOLDER}/${IMPORT_PARAMETERS_FILENAME}"
        String localPath = "src/test/resources/${RESOURCES_FOLDER}"
        InputStream defaultSpreadsheetInputStream = FairfaxSpreadsheet.getResourceAsStream(resourcePath)
        if (defaultSpreadsheetInputStream == null) {
            File spreadsheetFile = new File("${localPath}/${IMPORT_PARAMETERS_FILENAME}")
            spreadsheet = Spreadsheet.fromJson(ID_COLUMN_NAME, spreadsheetFile.text, true, true)
        } else {
            spreadsheet = Spreadsheet.fromJson(ID_COLUMN_NAME, defaultSpreadsheetInputStream.text, true, true)
        }
        FairfaxSpreadsheet fairfaxSpreadsheet = new FairfaxSpreadsheet(spreadsheet)

        if (defaultSpreadsheetInputStream == null) {
            FileNameFinder fileNameFinder = new FileNameFinder()
            // TODO Perhaps a my 'streaming-like' approach, otherwise we have to find all the files (which could be thousands)
            // before processing them.
            // One approach would be to only find files for a certain date mask and step through the dates one by one
            //filenames = fileNameFinder.getFileNames(localPath, "**/*.pdf")
            File parentFolder = new File(localPath)
            String[] extensions = [ "pdf" ].toArray()
            Collection<File> files = FileUtils.listFiles(parentFolder, extensions, true)
            List<File> matchingFiles = getMatchingFiles(files, ".*?/.*?\\.pdf")
            filenames = matchingFiles.collect { File file ->
                file.getCanonicalPath()
            }
        } else {
            List<File> files = getResourceFiles(resourcePath)
            List<File> matchingFiles = getMatchingFiles(files, ".*?/.*?\\.pdf")
            filenames = matchingFiles.collect { File file ->
                file.getCanonicalPath()
            }
        }
        filenames.each { String filename ->
            println("File for processing=${filename}")
        }
        println("STARTING processFiles")

        Map<FairfaxFileGroupKey, FairfaxFileGroup> fairfaxFileGroupMap = [ : ]
        filenames.each { String filename ->
            FairfaxFile fairfaxFile = new FairfaxFile(new File(filename))
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
        boolean allowZeroRatio = true
        fairfaxFileGroupMap.each { FairfaxFileGroupKey fairfaxFileGroupKey, FairfaxFileGroup fairfaxFileGroup ->
            FairfaxFileGroupMatch fairfaxFileGroupMatch = FairfaxFileGroupMatcher.mostLikelyMatch(fairfaxFileGroup,
                    fairfaxSpreadsheet, allowZeroRatio)
            if (fairfaxFileGroupMatch != null) {
                println("Will process fairfaxFileGroup=${fairfaxFileGroup} according to sip=${fairfaxFileGroupMatch.sip}")
                List<FairfaxFile> fairfaxFiles = fairfaxFileGroup.files.sort()
                List<Sip.FileWrapper> fileWrappers = fairfaxFiles.collect() { FairfaxFile fairfaxFile ->
                    SipFileWrapperFactory.generate(fairfaxFile.file, true, false)
                }
                Sip testSip = fairfaxFileGroupMatch.sip.clone()
                testSip.fileWrappers = fileWrappers
                SipXmlGenerator sipXmlGenerator = new SipXmlGenerator(testSip)
                String sipAsXml = sipXmlGenerator.getSipAsXml()
                println("\n* * *   S I P   * * *")
                println(sipAsXml)
                println("\n* * *   E N D   O F   S I P   * * *")
                println("\n* * *   E X T R A C T E D   X M L   * * *")
                println("title=${testSip.title}")
                println("\n* * *   E X T R A C T E D   X M L   * * *")
            } else {
                println("Unable to process fairfaxFileGroup=${fairfaxFileGroup}: No matching sip")
                // This is the exception: we can't process the file
            }
            println("* * *\n\n")
        }

    }

    List<File> getMatchingFiles(Collection<File> files, String pattern) {
        return files.findAll { file ->
            file.getCanonicalPath() ==~ /${pattern}/
        }
    }

    List<File> getResourceFiles(String folderResourcePath) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader()
        URL url = loader.getResource(folderResourcePath)
        String path = url.getPath()

        List<File> files = Arrays.asList(new File(path).listFiles())
        files.each { file ->
            println("folderResourcePath=${folderResourcePath} found file=${file.getCanonicalPath()}")
        }

        return files
    }
}
