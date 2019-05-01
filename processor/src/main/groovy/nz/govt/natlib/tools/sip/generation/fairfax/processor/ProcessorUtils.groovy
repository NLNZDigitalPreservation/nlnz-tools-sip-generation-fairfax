package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper
import nz.govt.natlib.tools.sip.files.FilesFinder
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import org.apache.commons.io.FilenameUtils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.LocalDate

@Slf4j
class ProcessorUtils {
    static SimpleDateFormat FILE_TIMESTAMP_FORMATTER = new SimpleDateFormat('yyyy-MM-dd_HH-mm-SS')

    static LocalDate parseDate(String dateString) {
        LocalDate parsedDate = null
        if (dateString != null) {
            parsedDate = LocalDate.parse(dateString, FairfaxFile.LOCAL_DATE_TIME_FORMATTER)
        }
        return parsedDate
    }

    static List<File> findFiles(String localPath, boolean isRegexNotGlob, boolean matchFilenameOnly,
                                boolean sortFiles, String pattern, Timekeeper timekeeper,
                                boolean includeSubdirectories = true) {
        List<File> filesList = [ ]
        Path filesPath = Paths.get(localPath)
        if (!Files.exists(filesPath) || !Files.isDirectory(filesPath)) {
            log.warn("Path '${filesPath}' does not exist is not a directory. Returning empty file list.")
            return filesList
        }

        log.info("Finding files for path=${filesPath.toFile().getCanonicalPath()} and pattern=${pattern}")
        timekeeper.logElapsed()
        boolean directoryOnly = false
        filesList = FilesFinder.getMatchingFilesFull(filesPath, isRegexNotGlob, matchFilenameOnly, sortFiles,
                includeSubdirectories, directoryOnly, pattern)
        log.info("Found total files=${filesList.size()} for path=${filesPath.toFile().getCanonicalPath()}")
        timekeeper.logElapsed()

        return filesList
    }

    static List<File> findNonMatchingFiles(String localPath, boolean isRegexNotGlob, boolean matchFilenameOnly,
                                boolean sortFiles, String pattern, Timekeeper timekeeper) {
        List<File> filesList = [ ]
        Path filesPath = Paths.get(localPath)
        if (!Files.exists(filesPath) || !Files.isDirectory(filesPath)) {
            log.warn("Path '${filesPath}' does not exist is not a directory. Returning empty file list.")
            return filesList
        }

        log.info("Finding files for path=${filesPath.toFile().getCanonicalPath()} and pattern=${pattern}")
        timekeeper.logElapsed()
        filesList = FilesFinder.getNonMatchingFiles(filesPath, isRegexNotGlob, matchFilenameOnly, sortFiles, pattern)
        log.info("Found total files=${filesList.size()} for path=${filesPath.toFile().getCanonicalPath()}")
        timekeeper.logElapsed()

        return filesList
    }

    static List<File> matchFiles(List<File> allFiles, String pattern) {
        List<File> matchedFiles = [ ]
        allFiles.each { File file ->
            if (file.getName() ==~ /${pattern}/) {
                matchedFiles.add(file)
            }
        }

        return matchedFiles
    }

    static void copyOrMoveFiles(List<File> files, File destination, boolean moveFiles) {
        if (moveFiles) {
            files.each { File file ->
                File destinationFile = new File(destination, file.getName())
                Files.move(file.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            }
        } else {
            files.each { File file ->
                File destinationFile = new File(destination, file.getName())
                Files.copy(file.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            }
        }
    }

    // Hash the files to determine if they are the same file.
    static boolean isSameFile(File file1, File file2) {
        String file1Md5Hash = generateMD5(file1)
        String file2Md5Hash = generateMD5(file2)

        return file1Md5Hash.equals(file2Md5Hash)
    }

    static String generateMD5(File file) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        file.eachByte(4096) { byte[] buffer, int length ->
            digest.update(buffer, 0, length)
        }
        return digest.digest().encodeHex() as String
    }

    static File nonDuplicateFile(File originalFile) {
        String fileName = originalFile.name
        String baseName = FilenameUtils.getBaseName(fileName)
        String extension = FilenameUtils.getExtension(fileName)
        File parentFile = originalFile.parentFile
        File candidateFile = null
        boolean alreadyExists = true
        int duplicateIndexCount = 0
        while (alreadyExists) {
            String candidateFileName = baseName + "-DUPLICATE-" + duplicateIndexCount + "." + extension
            candidateFile = new File(candidateFileName, parentFile)
            alreadyExists = candidateFile.exists()
            duplicateIndexCount += 1
        }
        return candidateFile
    }

    static void printAndFlush(String message) {
        System.out.print(message)
        System.out.flush()
    }
}
