package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFilesProcessor
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.processing.ProcessLogger
import nz.govt.natlib.tools.sip.state.SipProcessingState

import java.time.LocalDate

/**
 * For calling from gradle build scripts.
 */
@Slf4j
class ProcessByDateProcessor {
    FairfaxSpreadsheet fairfaxSpreadsheet
    Timekeeper timekeeper

    ProcessByDateProcessor(Timekeeper timekeeper) {
        this.timekeeper = timekeeper
    }

    SipProcessingState processNameFolder(File nameFolder, File destinationFolder, String name, String dateString,
                                                      boolean createDestination, boolean moveFilesToDestination) {
        // Process the files in the name folder
        ProcessLogger processLogger = new ProcessLogger()
        processLogger.startSplit()

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        // Only process PDF files
        String pattern = '\\w{6}-\\d{8}-\\w{3,4}\\.pdf'

        log.info("START processNameFolder for pattern=${pattern}, nameFolder=${nameFolder.getCanonicalPath()}")
        timekeeper.logElapsed()

        List<File> allFiles = ProcessorUtils.findFiles(nameFolder.getAbsolutePath(), isRegexNotGlob, matchFilenameOnly,
                sortFiles, pattern, timekeeper)

        SipProcessingState sipProcessingState = new SipProcessingState()
        // Process the folder as a single collection of files
        // TODO Note that there may be multiple destinations (as there could be different editions of the same name for a given day).
        String sipAsXml = FairfaxFilesProcessor.processCollectedFiles(sipProcessingState, fairfaxSpreadsheet, allFiles)

        File sipAndFilesFolder
        if (sipProcessingState.complete && sipProcessingState.successful) {
            sipAndFilesFolder = new File(destinationFolder, "${dateString}/${name}/${sipProcessingState.identifier}")
        } else {
            sipAndFilesFolder = new File(destinationFolder, "FAILURE/${dateString}/${name}")
        }
        File unrecognizedFilesFolder = new File(destinationFolder, "UNRECOGNIZED/${dateString}/${name}")

        boolean hasSipAndFilesFolder
        boolean hasUnrecognizedFilesFolder
        // Move or copy the processed files to the destination folder
        if ((sipProcessingState.validFiles.size() > 0 || sipProcessingState.invalidFiles.size() > 0)) {
            hasSipAndFilesFolder = true
            if (!sipAndFilesFolder.exists() && createDestination) {
                sipAndFilesFolder.mkdirs()
            }
        }
        ProcessorUtils.copyOrMoveFiles(sipProcessingState.validFiles, sipAndFilesFolder, moveFilesToDestination)
        ProcessorUtils.copyOrMoveFiles(sipProcessingState.invalidFiles, sipAndFilesFolder, moveFilesToDestination)

        // If the files aren't recognized, then dump the files in an exception folder
        if (sipProcessingState.unrecognizedFiles.size() > 0) {
            hasUnrecognizedFilesFolder = true
            if (!unrecognizedFilesFolder.exists() && createDestination) {
                unrecognizedFilesFolder.mkdirs()
            }
        }
        ProcessorUtils.copyOrMoveFiles(sipProcessingState.unrecognizedFiles, unrecognizedFilesFolder, moveFilesToDestination)

        // Write out the SipProcessingState
        File sipProcessingStateFile = new File(sipAndFilesFolder, "sipProcessingState.txt")
        sipProcessingStateFile.write(sipProcessingState.toString())

        // Write out the SIP file
        File sipFile = new File(sipAndFilesFolder, "mets.xml")
        sipFile.write(sipAsXml)

        log.info("END processNameFolder for pattern=${pattern}, nameFolder=${nameFolder.getCanonicalPath()}")
        timekeeper.logElapsed()

        if (hasSipAndFilesFolder) {
            processLogger.copySplit(sipAndFilesFolder, "Process-By-Date", false)
        }
        if (hasUnrecognizedFilesFolder) {
            processLogger.copySplit(unrecognizedFilesFolder, "Process-By-Date", true)
        }
        return sipProcessingState
    }

    // The processByDate destinationFolder structure is the following:
    // |- <date-in-yyyMMdd>/<name>_<yyyyMMdd>-<identifier>/{files}
    void processByDate(File sourceFolder, File destinationFolder, boolean createDestination, boolean moveFiles,
                              LocalDate startingDate, LocalDate endingDate) {
        if (createDestination) {
            destinationFolder.mkdirs()
        }

        this.fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

        // Loop through the dates in sequence, finding and processing files
        LocalDate currentDate = startingDate
        while (currentDate.isBefore(endingDate) || currentDate.equals(endingDate)) {
            // The folder structure is <sourceFolder>/<date>/<name>/{files}
            String currentDateString = FairfaxFile.LOCAL_DATE_TIME_FORMATTER.format(currentDate)
            File dateFolder = new File(sourceFolder, currentDateString)
            if (dateFolder.exists() && dateFolder.isDirectory()) {
                dateFolder.listFiles().each { File subFile ->
                    if (subFile.isDirectory()) {
                        // we want to process this directory, which should be a <name>
                        processNameFolder(subFile, destinationFolder, currentDateString, subFile.getName(),
                                createDestination, moveFiles)
                    } else {
                        log.info("Skipping ${subFile.getCanonicalPath()} as not directory=${subFile.isDirectory()}")
                    }
                }
            } else {
                log.info("Skipping ${dateFolder.getCanonicalPath()} as exists=${dateFolder.exists()}, directory=${dateFolder.isDirectory()}")
            }
            currentDate = currentDate.plusDays(1L)
        }
    }

}
