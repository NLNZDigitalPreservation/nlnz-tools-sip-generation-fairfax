package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

@Slf4j
class GroupByDateAndNameProcessor {
    Timekeeper timekeeper
    FairfaxSpreadsheet fairfaxSpreadsheet
    Set<String> recognizedNames = [ ]
    Set<String> unrecognizedNames = [ ]

    GroupByDateAndNameProcessor(Timekeeper timekeeper) {
        this.timekeeper = timekeeper
    }

    void copyOrMoveFileToDateAndNameGroup(File destinationFolder, FairfaxFile targetFile, String dateFolderName,
                                          boolean moveFile) {
        String nameFolderName = targetFile.name
        String folderPath
        Set<String> allNameKeys = fairfaxSpreadsheet.allNameKeys

        if (allNameKeys.contains(targetFile.name)) {
            // There's an entry in the spreadsheet for this name
            // Goes to '<date>/<name>/<file>'
            if (!recognizedNames.contains(targetFile.name)) {
                recognizedNames.add(targetFile.name)
                log.info("copyOrMoveFileToDateAndNameGroup adding recognizedName=${targetFile.name}")
            }
            folderPath = "${destinationFolder}${File.separator}${dateFolderName}${File.separator}${nameFolderName}"
        } else {
            // There is no entry in the spreadsheet for this name
            // Goes to 'UNKNOWN/<date>/<file>'
            if (!unrecognizedNames.contains(targetFile.name)) {
                unrecognizedNames.add(targetFile.name)
                log.info("copyOrMoveFileToDateAndNameGroup adding unrecognizedName=${targetFile.name}")
            }
            folderPath = "${destinationFolder}${File.separator}UNKNOWN${File.separator}${dateFolderName}"
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

    // The groupByDateAndName structure is the following in the destinationFolder:
    // |- <yyyyMMdd>/<name>/{files}
    // |- UNKNOWN/<yyyyMMdd>/{files}
    void groupByDateAndName(File sourceFolder, File destinationFolder, boolean createDestination, boolean moveFiles,
                            LocalDate startingDate, LocalDate endingDate) {
        // Clear the set of recognized and unrecognized names before processing begins
        recognizedNames = [ ]
        unrecognizedNames = [ ]

        ProcessorLogger processorLogger = new ProcessorLogger()
        processorLogger.startSplit()

        log.info("START groupByDateAndName for sourceFolder=${sourceFolder.getCanonicalPath()}, " +
        "startindDate=${startingDate}, endingDate=${endingDate}")
        timekeeper.logElapsed()

        if (createDestination) {
            destinationFolder.mkdirs()
        }
        this.fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true

        String pattern = '\\w{6}-\\d{8}-\\w{3,4}\\.pdf'
        // Given that we could be dealing with 60,000+ files in the source directory, it's probably more efficient to
        // get them all at once
        List<File> allFiles = ProcessorUtils.findFiles(sourceFolder.getAbsolutePath(), isRegexNotGlob,
                matchFilenameOnly, sortFiles, pattern, timekeeper)

        if (startingDate != null && endingDate != null) {
            // Loop through the dates in sequence, finding and processing files
            LocalDate currentDate = startingDate
            while (currentDate.isBefore(endingDate) || currentDate.isEqual(endingDate)) {
                String dateString = FairfaxFile.LOCAL_DATE_TIME_FORMATTER.format(currentDate)
                pattern = '\\w{6}-' + dateString + '-\\w{3,4}\\.pdf'
                log.info("Searching for files matching pattern=${pattern}")
                List<File> foundFiles = ProcessorUtils.matchFiles(allFiles, pattern)
                log.info("Found total files=${foundFiles.size()} matching pattern=${pattern}")
                if (foundFiles.size() > 0) {
                    log.info("Moving=${moveFiles} total files=${foundFiles.size()} to destination=${destinationFolder.getCanonicalPath()}")
                    foundFiles.each { File foundFile ->
                        copyOrMoveFileToDateAndNameGroup(destinationFolder, new FairfaxFile(foundFile), dateString,
                                moveFiles)
                    }
                }
                currentDate = currentDate.plusDays(1L)
            }
        } else {
            log.info("startingDate=${startingDate} and endingDate=${endingDate} have not been both specified")
            //String pattern = ".*?\\.pdf"
            List<File> foundFiles = ProcessorUtils.findFiles(sourceFolder.getAbsolutePath(), isRegexNotGlob,
                    matchFilenameOnly, sortFiles, pattern, timekeeper)
            foundFiles.each { File foundFile ->
                // TODO This wouldn't work because there's no dateString
                String dateString = "TODO_NO_DATE_FOUND"
                copyOrMoveFileToDateAndNameGroup(destinationFolder, new FairfaxFile(foundFile), dateString,
                        moveFiles)
            }
        }

        log.info("START groupByDateAndName for sourceFolder=${sourceFolder.getCanonicalPath()}, " +
                "startindDate=${startingDate}, endingDate=${endingDate}")
        timekeeper.logElapsed()

        processorLogger.copySplit(destinationFolder, "Group-By-Date-And-Name", true)
    }
}
