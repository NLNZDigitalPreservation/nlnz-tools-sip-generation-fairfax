package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.util.logging.Log4j2
import groovy.util.slurpersupport.GPathResult
import nz.govt.natlib.tools.sip.IEEntityType
import nz.govt.natlib.tools.sip.extraction.SipXmlExtractor
import nz.govt.natlib.tools.sip.files.FilesFinder
import nz.govt.natlib.tools.sip.generation.parameters.Spreadsheet
import nz.govt.natlib.tools.sip.processing.ProcessOutputInterceptor
import nz.govt.natlib.tools.sip.state.SipProcessingException
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReason
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType
import nz.govt.natlib.tools.sip.state.SipProcessingState

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

/**
 * Useful methods for use across different unit tests.
 *
 */
@Log4j2
class TestHelper {
    static final String RESOURCES_FOLDER = "nz/govt/natlib/tools/sip/generation/fairfax"


    static class TestMethodState {
        String idColumnName
        String resourcesFolder
        String importParametersFilename
        SipProcessingState sipProcessingState
        ProcessOutputInterceptor processOutputInterceptor
        String localPath
        String resourcePath
        FairfaxSpreadsheet fairfaxSpreadsheet

        TestMethodState(String idColumnName, String resourcesFolder, String importParametersFilename) {
            this.idColumnName = idColumnName
            this.resourcesFolder = resourcesFolder
            this.importParametersFilename = importParametersFilename
        }
    }

    static void initializeTestMethod(TestMethodState testMethodState, String filePrefix, boolean forLocalFilesystem) {
        testMethodState.sipProcessingState = new SipProcessingState()
        testMethodState.processOutputInterceptor = ProcessOutputInterceptor.forTempFile(filePrefix,
                ".txt", false)
        testMethodState.sipProcessingState.processingOutputPath = testMethodState.processOutputInterceptor.path
        testMethodState.processOutputInterceptor.start()

        if (forLocalFilesystem) {
            testMethodState.localPath = "src/test/resources/${testMethodState.resourcesFolder}"

            File spreadsheetFile = new File("${testMethodState.localPath}/${testMethodState.importParametersFilename}")
            Spreadsheet spreadsheet = Spreadsheet.fromJson(testMethodState.idColumnName, spreadsheetFile.text, true, true)
            testMethodState.fairfaxSpreadsheet = new FairfaxSpreadsheet(spreadsheet)
        } else {
            testMethodState.resourcePath = "${testMethodState.resourcesFolder}"
            testMethodState.localPath = "src/test/resources/${testMethodState.resourcesFolder}"

            testMethodState.fairfaxSpreadsheet = loadSpreadsheet(testMethodState.resourcePath, testMethodState.localPath,
                    testMethodState.importParametersFilename, testMethodState.idColumnName)
        }
    }

    /**
     * Returns the contents of the file from the given filename and resources folder.
     * Make an attempt to open the file as a resource.
     * If that fails, try to open the file with the path resourcesFolder/filename. This should be relative
     * to the current working directory if the the resourcesFolder is a relative path.
     *
     * @param filename
     * @param resourcesFolder
     * @return
     */
    static String getTextFromResourceOrFile(String filename, String resourcesFolder = RESOURCES_FOLDER) {
        String resourcePath = "${resourcesFolder}/${filename}"
        String localPath = "src/test/resources/${resourcePath}"

        String text
        InputStream inputStream = TestHelper.class.getResourceAsStream(filename)
        if (inputStream == null) {
            File inputFile = new File(localPath)
            if (!inputFile.exists()) {
                inputFile = new File(new File(""), localPath)
            }
            text = inputFile.text
        } else {
            text = inputStream.text
        }
        return text
    }

    /**
     * Returns the file from the given filename and resources folder.
     * Make an attempt to open the file as a resource.
     * If that fails, try to open the file with the path resourcesFolder/filename. This should be relative
     * to the current working directory if the resourcesFolder is a relative path.
     *
     * @param filename
     * @param resourcesFolder
     * @return
     */
    static File getFileFromResourceOrFile(String filename, String resourcesFolder = RESOURCES_FOLDER) {
        String resourcePath = "${resourcesFolder}/${filename}"
        String localPath = "src/test/resources/${resourcePath}"

        URL resourceURL = TestHelper.class.getResource(filename)
        File resourceFile
        if (resourceURL != null) {
            resourceFile = new File(resourceURL.getFile())
        }
        if (resourceFile != null && (resourceFile.isFile() || resourceFile.isDirectory())) {
            return resourceFile
        } else {
            File returnFile = new File(localPath)
            return returnFile
        }
    }

    /**
     * When loading files from a resource path, we assume that there aren't that many files (tens rather than thousands)
     * so we use use the traditional java.io approach to listing files.
     *
     * @param folderResourcePath
     * @return
     */
    static List<File> getResourceFiles(String folderResourcePath, boolean isRegexNotGlob, boolean matchFilenameOnly,
                                        String pattern) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader()
        URL url = loader.getResource(folderResourcePath)
        String path = url.getPath()

        List<File> files = Arrays.asList(new File(path).listFiles())
        log.info("All files:")
        files.each { file ->
            log.info("folderResourcePath=${folderResourcePath} found file=${file.getCanonicalPath()}")
        }
        if (!isRegexNotGlob) {
            throw new RuntimeException("Globbing not supported for finding resource files, use a regex pattern instead")
        }
        List<File> filteredFiles
        if (matchFilenameOnly) {
            filteredFiles = files.findAll { File  file ->
                file.getName() ==~ /${pattern}/
            }
        } else {
            filteredFiles = files.findAll { File file ->
                file.getCanonicalPath() ==~ /${pattern}/
            }
        }
        return filteredFiles
    }

    static List<File> getFilesForProcessingFromFileSystem(boolean isRegexNotGlob, boolean matchFilenameOnly, boolean sortFiles,
                                                   String localPath, String pattern) {
        List<File> filesForProcessing = [ ]
        Path filesPath = Paths.get(localPath)
        if (!Files.exists(filesPath) || !Files.isDirectory(filesPath)) {
            log.warn("Path '${filesPath}' does not exist is not a directory. Returning empty file list.")
        } else {
            filesForProcessing = FilesFinder.getMatchingFiles(filesPath, isRegexNotGlob, matchFilenameOnly,
                    sortFiles, pattern)
        }

        log.info("Collected ${filesForProcessing.size()} files for processing")
        filesForProcessing.each { File file ->
            log.info("File for processing=${file.getCanonicalPath()}")
        }

        return filesForProcessing
    }

    static List<File> getFilesForProcessingFromResource(boolean isRegexNotGlob, boolean matchFilenameOnly, boolean sortFiles,
                                                        String resourcePath, String localPath, String pattern) {
        List<File> filesForProcessing = findFiles(resourcePath, localPath, isRegexNotGlob, matchFilenameOnly,
                sortFiles, pattern)

        log.info("Collected ${filesForProcessing.size()} files for processing")
        filesForProcessing.each { File file ->
            log.info("File for processing=${file.getCanonicalPath()}")
        }

        return filesForProcessing
    }

    static List<File> getMatchingFiles(Collection<File> files, String pattern) {
        return files.findAll { file ->
            file.getCanonicalPath() ==~ /${pattern}/
        }
    }

    static FairfaxSpreadsheet loadSpreadsheet(String resourcePath, String localPath, String importParametersFilename, String idColumnName) {
        Spreadsheet spreadsheet
        InputStream defaultSpreadsheetInputStream = FairfaxSpreadsheet.getResourceAsStream(resourcePath)
        if (defaultSpreadsheetInputStream == null) {
            File spreadsheetFile = new File("${localPath}/${importParametersFilename}")
            spreadsheet = Spreadsheet.fromJson(idColumnName, spreadsheetFile.text, true, true)
        } else {
            spreadsheet = Spreadsheet.fromJson(idColumnName, defaultSpreadsheetInputStream.text, true, true)
        }
        FairfaxSpreadsheet fairfaxSpreadsheet = new FairfaxSpreadsheet(spreadsheet)

        return fairfaxSpreadsheet
    }

    // TODO Could handle more than one pattern (see https://www.javacodegeeks.com/2012/11/java-7-file-filtering-using-nio-2-part-2.html)
    static List<File> findFiles(String resourcePath, String localPath, boolean isRegexNotGlob, boolean matchFilenameOnly,
                                boolean sortFiles, String pattern) {
        List<File> filesList = [ ]
        // We check if we're using a resource stream to load the files, otherwise we are loading from the file system
        InputStream doWeChooseAResourceStream = TestHelper.getResourceAsStream(resourcePath)
        if (doWeChooseAResourceStream == null) {
            Path filesPath = Paths.get(localPath)
            if (!Files.exists(filesPath) || !Files.isDirectory(filesPath)) {
                log.warn("Path '${filesPath}' does not exist is not a directory. Returning empty file list.")
                return filesList
            }

            filesList = FilesFinder.getMatchingFiles(filesPath, isRegexNotGlob, matchFilenameOnly, sortFiles, pattern)
            return filesList
        } else {
            List<File> files = getResourceFiles(resourcePath, isRegexNotGlob, matchFilenameOnly, pattern)
            filesList = getMatchingFiles(files, pattern)

            return filesList
        }
    }

    static void assertExpectedSipMetadataValues(SipXmlExtractor sipForValidation, String title, int year, int month,
                                                int dayOfMonth, IEEntityType ieEntityType, String objectIdentifierType,
                                                String objectcIdentifierValue, String policyId, String preservationType,
                                                String usageType, boolean isDigitalOriginal, int revisionNumber) {
        assertThat("title", sipForValidation.extractTitle(), is(title))
        assertThat("year", sipForValidation.extractYear(), is(year))
        assertThat("month", sipForValidation.extractMonth(), is(month))
        assertThat("dayOfMonth", sipForValidation.extractDayOfMonth(), is(dayOfMonth))
        assertThat("ieEntityType", sipForValidation.extractIEEntityType(), is(ieEntityType))
        assertThat("objectIdentifierType", sipForValidation.extractObjectIdentifierType(), is(objectIdentifierType))
        assertThat("objectIdentifierValue", sipForValidation.extractObjectIdentifierValue(), is(objectcIdentifierValue))
        assertThat("policyId", sipForValidation.extractPolicyId(), is(policyId))
        assertThat("preservationType", sipForValidation.extractPreservationType(), is(preservationType))
        assertThat("usageType", sipForValidation.extractUsageType(), is(usageType))
        assertThat("digitalOriginal", sipForValidation.extractDigitalOriginal(), is(isDigitalOriginal))
        assertThat("revisionNumber", sipForValidation.extractRevisionNumber(), is(revisionNumber))
    }

    static void assertExpectedSipFileValues(SipXmlExtractor sipForValidation, int idIndex, String originalName,
                                            String originalPath, long sizeBytes, String fixityType, String fixityValue,
                                            String fileLabel, String mimeType) {
        GPathResult fileGPath = sipForValidation.extractFileIdRecord(idIndex)
        // NOTE Any unit test errors in this section (such as:
        // java.lang.NoSuchMethodError: org.hamcrest.Matcher.describeMismatch(Ljava/lang/Object;Lorg/hamcrest/Description;)V
        // could indicate that a null value is coming into the test, which could mean that the value is not in the SIP's
        // XML.
        assertThat("fileWrapper${idIndex}.fileOriginalName", sipForValidation.extractFileOriginalName(fileGPath), is(originalName))
        assertThat("fileWrapper${idIndex}.fileOriginalPath", sipForValidation.extractFileOriginalPath(fileGPath), is(originalPath))
        assertThat("fileWrapper${idIndex}.fileSizeBytes", sipForValidation.extractFileSizeBytes(fileGPath), is(sizeBytes))
        assertThat("fileWrapper${idIndex}.fixityType", sipForValidation.extractFileFixityType(fileGPath), is(fixityType))
        assertThat("fileWrapper${idIndex}.fixityValue", sipForValidation.extractFileFixityValue(fileGPath), is(fixityValue))
        assertThat("fileWrapper${idIndex}.label", sipForValidation.extractFileLabel(fileGPath), is(fileLabel))
        assertThat("fileWrapper${idIndex}.mimeType", sipForValidation.extractFileMimeType(fileGPath), is(mimeType))
        // This is dependent on the filesystem, so we can't really test this
        //assertThat("fileWrapper${idIndex}.modificationDate", sipForValidation.extractFileModificationDate(fileGPath), is(LocalDateTime.of(
        //        LocalDate.of(2015, 7, 29),
        //        LocalTime.of(0, 0, 0, 0))))
        //assertThat("fileWrapper${idIndex}.creationDate", sipForValidation.extractFileCreationDate(fileGPath), is(LocalDateTime.of(
        //        LocalDate.of(2015, 7, 29),
        //        LocalTime.of(0, 0, 0, 0))))

    }

    static void assertExpectedExceptionReason(SipProcessingState sipProcessingState, SipProcessingExceptionReasonType type) {
        assertFalse("SipProcessingState is NOT successful", sipProcessingState.isSuccessful())
        assertTrue("SipProcessingState has exceptions", sipProcessingState.exceptions.size() > 0)
        SipProcessingException firstException = sipProcessingState.exceptions.first()
        assertTrue("SipProcessingException has reasons", firstException.reasons.size() > 0)
        SipProcessingExceptionReason firstExceptionReason = firstException.reasons.first()
        assertThat("SipProcessingState firstExceptionReason type is ${type}", firstExceptionReason.reasonType, is(type))
    }
}
