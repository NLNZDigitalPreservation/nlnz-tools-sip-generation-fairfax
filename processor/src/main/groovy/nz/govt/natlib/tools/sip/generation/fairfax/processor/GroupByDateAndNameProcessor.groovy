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

    Set<String> recognizedNames = [ ]
    Set<String> unrecognizedNames = [ ]

    void copyOrMoveFileToDateAndNameGroup(File destinationFolder, FairfaxFile targetFile, String dateFolderName,
                                                 FairfaxSpreadsheet spreadsheet, boolean moveFile) {
        String nameFolderName = targetFile.name
        String folderPath
        Set<String> allNameKeys = spreadsheet.allNameKeys

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
        if (moveFile) {
            Files.move(targetFile.file.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        } else {
            Files.copy(targetFile.file.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        }
    }

// The groupByDateAndName structure is the following in the destinationFolder:
// |- <yyyyMMdd>/<name>/{files}
// |- UNKNOWN/<yyyyMMdd>/{files}
    void groupByDateAndName(File sourceFolder, File destinationFolder, boolean createDestination, boolean moveFiles,
                            LocalDate startingDate, LocalDate endingDate, Timekeeper timekeeper) {
        // Clear the set of recognized and unrecognized names before processing begins
        recognizedNames = [ ]
        unrecognizedNames = [ ]

        log.info("Starting groupByDateAndName")
        timekeeper.logElapsed()

        if (createDestination) {
            destinationFolder.mkdirs()
        }
        FairfaxSpreadsheet fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true

        String pattern = '\\w{6}-\\d{8}-\\w{3,4}\\.pdf'
        // Given that we could be dealing with 60,000+ files in the source directory, it's probably more efficient to
        // get them all at once
        List<File> allFiles = ProcessorUtils.findFiles(sourceFolder.getAbsolutePath(), isRegexNotGlob,
                matchFilenameOnly, sortFiles, pattern)

        if (startingDate != null || endingDate != null) {
            // if we are using a date range then we must find by date
            if (startingDate == null) {
                startingDate = ProcessorUtils.parseDate("20170101")
            }
            if (endingDate == null) {
                endingDate = LocalDate.now()
            }
            // Loop through the dates in sequence, finding and processing files
            LocalDate currentDate = startingDate
            while (currentDate.isBefore(endingDate) || currentDate.isEqual(endingDate)) {
                String dateString = FairfaxFile.LOCAL_DATE_TIME_FORMATTER.format(currentDate)
                pattern = '\\w{6}-' + dateString + '-\\w{3,4}\\.pdf'
                log.info("Searching for files matching pattern=${pattern}")
                List<File> foundFiles = ProcessorUtils.matchFiles(allFiles, pattern)
                log.info("Found total files=${foundFiles.size()} matching pattern=${pattern}")
                if (foundFiles.size() > 0) {
                    log.info("Moving=${moveFiles} files to destination=${destinationFolder.getCanonicalPath()}")
                    foundFiles.each { File foundFile ->
                        copyOrMoveFileToDateAndNameGroup(destinationFolder, new FairfaxFile(foundFile), dateString,
                                fairfaxSpreadsheet, moveFiles)
                    }
                }
                currentDate = currentDate.plusDays(1L)
            }
        } else {
            //String pattern = ".*?\\.pdf"
            List<File> foundFiles = ProcessorUtils.findFiles(sourceFolder.getAbsolutePath(), isRegexNotGlob,
                    matchFilenameOnly, sortFiles, pattern)
            foundFiles.each { File foundFile ->
                // TODO This wouldn't work because there's no dateString
                copyOrMoveFileToDateAndNameGroup(destinationFolder, new FairfaxFile(foundFile), dateString,
                        fairfaxSpreadsheet,moveFiles)
            }
        }

        log.info("Ending groupByDateAndName")
        timekeeper.logElapsed()

    }
}
