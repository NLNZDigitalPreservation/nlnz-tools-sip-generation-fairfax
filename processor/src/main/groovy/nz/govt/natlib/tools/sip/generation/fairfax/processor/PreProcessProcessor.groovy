package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.processing.ProcessLogger

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

@Slf4j
class PreProcessProcessor {
    Timekeeper timekeeper
    FairfaxSpreadsheet fairfaxSpreadsheet
    Set<String> recognizedTitleCodes = [ ]
    Set<String> unrecognizedTitleCodes = [ ]

    PreProcessProcessor(Timekeeper timekeeper) {
        this.timekeeper = timekeeper
    }

    void copyOrMoveFileToPreProcessingDestination(File destinationFolder, File forReviewFolder, FairfaxFile targetFile,
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
        if (destinationFile.exists()) {
            log.info("Skipping moveFile=${moveFile} destinationFile=${destinationFile.getCanonicalPath()} -- it already exists")
        } else {
            if (moveFile) {
                Files.move(targetFile.file.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            } else {
                Files.copy(targetFile.file.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            }
        }
    }

    // See the README.md for folder descriptions and structures.
    void process(File sourceFolder, File destinationFolder, File forReviewFolder, boolean createDestination,
                 boolean moveFiles, LocalDate startingDate, LocalDate endingDate) {
        // Clear the set of recognized and unrecognized names before processing begins
        recognizedTitleCodes = [ ]
        unrecognizedTitleCodes = [ ]

        ProcessLogger processLogger = new ProcessLogger()
        processLogger.startSplit()

        log.info("START process for startindDate=${startingDate}, endingDate=${endingDate}, " +
                "sourceFolder=${sourceFolder.getCanonicalPath()}, forReviewFolder=${forReviewFolder.getCanonicalPath()}")
        timekeeper.logElapsed()

        if (createDestination) {
            destinationFolder.mkdirs()
            forReviewFolder.mkdirs()
        }
        this.fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true

        String pattern = '\\w{6}-\\d{8}-\\w{3,4}.*?\\.pdf'
        // Given that we could be dealing with 60,000+ files in the source directory, it's probably more efficient to
        // get them all at once
        List<File> allFiles = ProcessorUtils.findFiles(sourceFolder.getAbsolutePath(), isRegexNotGlob,
                matchFilenameOnly, sortFiles, pattern, timekeeper)

        if (startingDate != null && endingDate != null) {
            // Loop through the dates in sequence, finding and processing files
            LocalDate currentDate = startingDate
            while (currentDate.isBefore(endingDate) || currentDate.isEqual(endingDate)) {
                String dateString = FairfaxFile.LOCAL_DATE_TIME_FORMATTER.format(currentDate)
                pattern = '\\w{6}-' + dateString + '-\\w{3,4}.*?\\.pdf'
                log.info("Searching for files matching pattern=${pattern}")
                List<File> foundFiles = ProcessorUtils.matchFiles(allFiles, pattern)
                log.info("Found total files=${foundFiles.size()} matching pattern=${pattern}")
                if (foundFiles.size() > 0) {
                    log.info("Moving=${moveFiles} total files=${foundFiles.size()} to destination=${destinationFolder.getCanonicalPath()}")
                    foundFiles.each { File foundFile ->
                        copyOrMoveFileToPreProcessingDestination(destinationFolder, forReviewFolder,
                                new FairfaxFile(foundFile), dateString, moveFiles)
                    }
                }
                currentDate = currentDate.plusDays(1L)
            }
        } else {
            log.info("startingDate=${startingDate} and endingDate=${endingDate} have not been both specified")

            List<File> foundFiles = ProcessorUtils.findNonMatchingFiles(sourceFolder.getAbsolutePath(), isRegexNotGlob,
                    matchFilenameOnly, sortFiles, pattern, timekeeper)
            foundFiles.each { File foundFile ->
                String dateString = "UNKNOWN-DATE"
                copyOrMoveFileToPreProcessingDestination(destinationFolder, forReviewFolder, new FairfaxFile(foundFile),
                        dateString, moveFiles)
            }
        }

        log.info("END process for startindDate=${startingDate}, endingDate=${endingDate}, " +
                "sourceFolder=${sourceFolder.getCanonicalPath()}, forReviewFolder=${forReviewFolder.getCanonicalPath()}")
        timekeeper.logElapsed()

        processLogger.copySplit(destinationFolder, "Pre-Process-Processor", true)
    }
}
