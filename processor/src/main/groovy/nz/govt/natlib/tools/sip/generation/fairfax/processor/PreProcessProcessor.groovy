package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.processing.ProcessLogger

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

@Slf4j
class PreProcessProcessor {
    ProcessorConfiguration processorConfiguration
    FairfaxSpreadsheet fairfaxSpreadsheet
    Set<String> recognizedTitleCodes = [ ]
    Set<String> unrecognizedTitleCodes = [ ]

    PreProcessProcessor(ProcessorConfiguration processorConfiguration) {
        this.processorConfiguration = processorConfiguration
    }

    boolean copyOrMoveFileToPreProcessingDestination(File destinationFolder, File forReviewFolder, FairfaxFile targetFile,
                                                  String dateFolderName, boolean moveFile) {
        String titleCodeFolderName = targetFile.titleCode
        String folderPath
        Set<String> allNameKeys = fairfaxSpreadsheet.allTitleCodeKeys

        if (allNameKeys.contains(targetFile.titleCode)) {
            // There's an entry in the spreadsheet for this titleCode
            // Goes to '<date>/<titleCode>/<file>'
            if (!recognizedTitleCodes.contains(targetFile.titleCode)) {
                recognizedTitleCodes.add(targetFile.titleCode)
                log.info("copyOrMoveFileToPreProcessingDestination adding titleCode=${targetFile.titleCode}")
            }
            folderPath = "${destinationFolder.getCanonicalPath()}${File.separator}${dateFolderName}${File.separator}${titleCodeFolderName}"
        } else {
            // There is no entry in the spreadsheet for this titleCode
            // Goes to 'UNKNOWN-TITLE-CODE/<date>/<file>'
            if (!unrecognizedTitleCodes.contains(targetFile.titleCode)) {
                unrecognizedTitleCodes.add(targetFile.titleCode)
                log.info("copyOrMoveFileToPreProcessingDestination adding unrecognizedName=${targetFile.titleCode}")
            }
            folderPath = "${forReviewFolder.getCanonicalPath()}${File.separator}UNKNOWN-TITLE-CODE${File.separator}${dateFolderName}"
        }
        File destination = new File(folderPath)
        destination.mkdirs()

        File destinationFile = new File(destination, targetFile.file.getName())
        boolean moveToDestination = true
        if (destinationFile.exists()) {
            if (ProcessorUtils.isSameFile(targetFile.file, destinationFile)) {
                moveToDestination = false
                if (processorConfiguration.verbose) {
                    log.info("Skipping moveFile=${moveFile} destinationFile=${destinationFile.getCanonicalPath()} -- it already exists and is same file")
                } else {
                    ProcessorUtils.printAndFlush("+")
                }
                if (moveFile) {
                    Files.delete(targetFile.file.toPath())
                    if (processorConfiguration.verbose) {
                        log.info("Deleting moveFile=${moveFile} destinationFile=${destinationFile.getCanonicalPath()}")
                    } else {
                        ProcessorUtils.printAndFlush("-")
                    }
                }
            } else {
                File nonDuplicateFile = ProcessorUtils.nonDuplicateFile(destinationFile)
                log.info("moveFile=${moveFile} destinationFile=${destinationFile.getCanonicalPath()} -- same name but different file")
                log.info("moveFile=${moveFile} moving to destinationFile=${nonDuplicateFile.getCanonicalPath()}")
                destinationFile = nonDuplicateFile
            }
        }
        if (moveToDestination) {
            if (moveFile) {
                // The only valid move option is StandardCopyOption.REPLACE_EXISTING, which we don't want to do
                Files.move(targetFile.file.toPath(), destinationFile.toPath())
            } else {
                Files.copy(targetFile.file.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            }
        }

        return moveToDestination
    }

    // See the README.md for folder descriptions and structures.
    void process() {
        // Clear the set of recognized and unrecognized names before processing begins
        recognizedTitleCodes = [ ]
        unrecognizedTitleCodes = [ ]

        ProcessLogger processLogger = new ProcessLogger()
        processLogger.startSplit()

        log.info("START process for startindDate=${processorConfiguration.startingDate}, " +
                "endingDate=${processorConfiguration.endingDate}, " +
                "sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}, " +
                "forReviewFolder=${processorConfiguration.forReviewFolder.getCanonicalPath()}")
        processorConfiguration.timekeeper.logElapsed()

        if (processorConfiguration.createDestination) {
            processorConfiguration.targetPreProcessingFolder.mkdirs()
            processorConfiguration.forReviewFolder.mkdirs()
        }
        this.fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true

        String pattern = '\\w{5,7}-\\d{8}-\\w{3,4}.*?\\.[pP]{1}[dD]{1}[fF]{1}'
        // Given that we could be dealing with 60,000+ files in the source directory, it's probably more efficient to
        // get them all at once
        List<File> allFiles = ProcessorUtils.findFiles(processorConfiguration.sourceFolder.getAbsolutePath(),
                isRegexNotGlob, matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper)

        int filesProcessed = 0
        int filesMovedOrCopied = 0
        processorConfiguration.timekeeper.logElapsed(false, filesProcessed)
        if (processorConfiguration.startingDate != null && processorConfiguration.endingDate != null) {
            // Loop through the dates in sequence, finding and processing files
            LocalDate currentDate = processorConfiguration.startingDate
            while (currentDate <= processorConfiguration.endingDate) {
                String dateString = FairfaxFile.LOCAL_DATE_TIME_FORMATTER.format(currentDate)
                pattern = '\\w{5,7}-' + dateString + '-\\w{3,4}.*?\\.[pP]{1}[dD]{1}[fF]{1}'
                log.info("Searching for files matching pattern=${pattern}")
                List<File> foundFiles = ProcessorUtils.matchFiles(allFiles, pattern)
                log.info("Found total files=${foundFiles.size()} matching pattern=${pattern}")
                if (foundFiles.size() > 0) {
                    log.info("Moving=${processorConfiguration.moveFiles} total files=${foundFiles.size()} to " +
                            "destination=${processorConfiguration.targetPreProcessingFolder.getCanonicalPath()}")
                    foundFiles.each { File foundFile ->
                        boolean moved = copyOrMoveFileToPreProcessingDestination(
                                processorConfiguration.targetPreProcessingFolder,
                                processorConfiguration.forReviewFolder, new FairfaxFile(foundFile), dateString,
                                processorConfiguration.moveFiles)
                        if (moved) {
                            filesMovedOrCopied += 1
                        }
                        filesProcessed += 1
                        if (filesProcessed % 5000 == 0) {
                            processorConfiguration.timekeeper.logElapsed(false, filesProcessed, true)
                        }
                    }
                }
                currentDate = currentDate.plusDays(1L)
            }
        } else {
            log.info("startingDate=${processorConfiguration.startingDate} and " +
                    "endingDate=${processorConfiguration.endingDate} have not been both specified")

            List<File> foundFiles = ProcessorUtils.findNonMatchingFiles(
                    processorConfiguration.sourceFolder.getAbsolutePath(), isRegexNotGlob, matchFilenameOnly,
                    sortFiles, pattern, processorConfiguration.timekeeper)
            foundFiles.each { File foundFile ->
                String dateString = "UNKNOWN-DATE"
                copyOrMoveFileToPreProcessingDestination(processorConfiguration.targetPreProcessingFolder,
                        processorConfiguration.forReviewFolder, new FairfaxFile(foundFile), dateString,
                        processorConfiguration.moveFiles)
            }
        }
        processorConfiguration.timekeeper.logElapsed(false, filesProcessed, true)

        log.info("END process for startindDate=${processorConfiguration.startingDate}, " +
                "endingDate=${processorConfiguration.endingDate}, " +
                "sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}, " +
                "forReviewFolder=${processorConfiguration.forReviewFolder.getCanonicalPath()}")
        processorConfiguration.timekeeper.logElapsed()

        processLogger.copySplit(processorConfiguration.targetPreProcessingFolder, "Pre-Process-Processor", true)
    }
}
