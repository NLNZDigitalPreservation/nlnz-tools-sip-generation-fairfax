package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper
import nz.govt.natlib.tools.sip.files.FilesFinder
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import org.apache.commons.io.FilenameUtils

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate

@Slf4j
class ProcessorUtils {
    static final SimpleDateFormat FILE_TIMESTAMP_FORMATTER = new SimpleDateFormat('yyyy-MM-dd_HH-mm-ss')
    static final SimpleDateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss.SSS')
    static final DecimalFormat TOTAL_FORMAT = new DecimalFormat("###,###,###,###,###")
    static final String MD5_HASH_ZERO_LENGTH_FILE = "d41d8cd98f00b204e9800998ecf8427e"

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
        log.info("Found total files=${TOTAL_FORMAT.format(filesList.size())} for path=${filesPath.toFile().getCanonicalPath()}")
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
        log.info("Found total files=${TOTAL_FORMAT.format(filesList.size())} for path=${filesPath.toFile().getCanonicalPath()}")
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
    static boolean isSameFile(File file1, File file2, allowZeroLengthFiles = false) {
        String file1Md5Hash = generateMD5(file1)
        String file2Md5Hash = generateMD5(file2)

        if (!allowZeroLengthFiles) {
            boolean hasZeroLengthHashes = false
            String message = ""
            if (MD5_HASH_ZERO_LENGTH_FILE.equals(file1Md5Hash)) {
                hasZeroLengthHashes = true
                message += "file=${file1.getCanonicalPath()} has a prohibited zero-length file MD5 hash=${file1Md5Hash}"
            }
            if (MD5_HASH_ZERO_LENGTH_FILE.equals(file2Md5Hash)) {
                hasZeroLengthHashes = true
                if (message.length() > 0) {
                    message += ", "
                }
                message += "file=${file2.getCanonicalPath()} has a prohibited zero-length file MD5 hash=${file2Md5Hash}"
            }
            if (hasZeroLengthHashes) {
                throw new ProcessorException(message)
            }
        }
        return file1Md5Hash.equals(file2Md5Hash)
    }

    static String generateMD5(File file) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        file.eachByte(4096) { byte[] buffer, int length ->
            digest.update(buffer, 0, length)
        }
        return digest.digest().encodeHex() as String
    }

    static File nonDuplicateFile(File originalFile, boolean usePreIndex = true, String preIndexString = "-DUPLICATE-",
                                 boolean useAdditionalExtension = false, String additionalExtension = ".tmp") {
        String fileName = originalFile.name
        String baseName = FilenameUtils.getBaseName(fileName)
        String extension = FilenameUtils.getExtension(fileName)
        File parentFile = originalFile.parentFile
        File candidateFile = null
        boolean alreadyExists = true
        int duplicateIndexCount = 0
        while (alreadyExists) {
            String preDuplicateIndexString = usePreIndex ? preIndexString : ""
            String extraExtension = useAdditionalExtension ? additionalExtension : ""
            String candidateFileName = baseName + preDuplicateIndexString + duplicateIndexCount + "." +
                    extension + extraExtension
            candidateFile = new File(candidateFileName, parentFile)
            alreadyExists = candidateFile.exists()
            duplicateIndexCount += 1
        }
        return candidateFile
    }

    static boolean atomicMoveOrCopy(boolean moveFile, File sourceFile, File targetFile,
                                    boolean useAtomicOption = true) {
        // Handle the case of being interrupted by copying/moving to the destination file (which leads to a bunch
        // partial copies -- especially in a multithreaded case -- that need to be manually checked to verify that
        // they are incomplete versions.
        // Instead, copy/move the file to a temporary-named file and then rename the file when the copy is complete.
        boolean renameSuccessful = false
        File temporaryDestinationFile = nonDuplicateFile(targetFile, true, "-",
                true, ".tmpcopy")
        if (moveFile) {
            // The only valid move option is StandardCopyOption.REPLACE_EXISTING, which we don't want to do
            if (useAtomicOption) {
                try {
                    Path resultingPath = Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
                    renameSuccessful = true
                } catch (AtomicMoveNotSupportedException e) {
                    log.debug("Attempt at atomic file move file sourceFile=${sourceFile.getCanonicalPath()} to " +
                            "targetFile=${targetFile.getCanonicalPath()} failed, trying a non-atomic move approach.")
                    renameSuccessful = atomicMoveOrCopy(moveFile, sourceFile, targetFile, false)
                }
            } else {
                Files.move(sourceFile.toPath(), temporaryDestinationFile.toPath())
            }
        } else {
            Files.copy(sourceFile.toPath(), temporaryDestinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        }
        // Copy operations are by definition not atomic
        if (!useAtomicOption || !moveFile) {
            renameSuccessful = temporaryDestinationFile.renameTo(targetFile)
            if (!renameSuccessful) {
                printAndFlush("\n")
                log.error("Unable to rename temporaryDestinationFile=${temporaryDestinationFile.getCanonicalPath()} " +
                        "to destinationFile=${targetFile.getCanonicalPath()}")
            }
        }
        return renameSuccessful
    }

    static void printAndFlush(String message) {
        System.out.print(message)
        System.out.flush()
    }
}
