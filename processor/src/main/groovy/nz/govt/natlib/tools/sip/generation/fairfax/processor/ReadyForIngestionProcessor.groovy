package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Log4j2
import groovyx.gpars.GParsPool
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType
import nz.govt.natlib.tools.sip.processing.ProcessLogger
import nz.govt.natlib.tools.sip.state.SipProcessingException
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReason
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType
import nz.govt.natlib.tools.sip.state.SipProcessingState

import java.time.LocalDate

/**
 * For calling from gradle build scripts.
 */
@Log4j2
class ReadyForIngestionProcessor {
    FairfaxSpreadsheet fairfaxSpreadsheet
    ProcessorConfiguration processorConfiguration
    ProcessingType processingType
    List<ProcessingRule> processingRules = [ ]
    List<ProcessingOption> processingOptions = [ ]

    ReadyForIngestionProcessor(ProcessorConfiguration processorConfiguration) {
        this.processorConfiguration = processorConfiguration
        this.processingType = ProcessingType.forFieldValue(this.processorConfiguration.forIngestionProcessingType)
        if (processingType == null) {
            String message = "No acceptable value for processingType=${this.processorConfiguration.forIngestionProcessingType}".toString()
            SipProcessingException exception = new SipProcessingExceptionReason(
                    SipProcessingExceptionReasonType.INVALID_PARAMETERS, null, message)
            throw exception
        }
        this.processingRules = ProcessingRule.extract(this.processorConfiguration.forIngestionProcessingRules,
                ",", processingType.defaultRules, true)
        this.processingOptions = ProcessingOption.extract(this.processorConfiguration.forIngestionProcessingOptions,
                ",", processingType.defaultOptions, true)
    }

    SipProcessingState processTitleCodeFolder(File titleCodeFolder, File destinationFolder, File forReviewFolder,
                                              String dateString, FairfaxProcessingParameters processingParameters) {
        // Process the files in the titleCode folder
        // TODO ProcessLogger won't be able to work in a multithreaded environment! -- if we could split the logging
        // then we could probably do multithreading.
        ProcessLogger processLogger = new ProcessLogger()
        processLogger.startSplit()

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        // Only process PDF files
        String pattern = '\\w{5,7}-\\d{8}-\\w{3,4}.*?\\.[pP]{1}[dD]{1}[fF]{1}'

        log.info("START processTitleCodeFolder for pattern=${pattern}, titleCodeFolder=${titleCodeFolder.getCanonicalPath()}")
        processorConfiguration.timekeeper.logElapsed()

        List<File> allFiles = ProcessorUtils.findFiles(titleCodeFolder.getAbsolutePath(), isRegexNotGlob,
                matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper)

        SipProcessingState sipProcessingState = processingParameters.sipProcessingState
        // Process the folder as a single collection of files
        // Note that the folder is processed for a single processingType (so there could be multiple passes, one for
        // each processingType).
        String sipAsXml = FairfaxFilesProcessor.processCollectedFiles(processingParameters, allFiles)

        File sipAndFilesFolder
        String folderName = "${dateString}_${processingParameters.titleCode}_${processingParameters.processingType.getFieldValue()}_${sipProcessingState.identifier}"
        if (sipProcessingState.complete && sipProcessingState.successful) {
            sipAndFilesFolder = new File(destinationFolder,
                    "${sipProcessingState.ieEntityType.getDisplayName()}/${folderName}")
        } else {
            sipAndFilesFolder = new File(forReviewFolder,
                    "${sipProcessingState.ieEntityType.getDisplayName()}/${folderName}")
        }
        File contentStreamsFolder = new File(sipAndFilesFolder, "content/streams")
        // Note that unrecognized only gets moved/copied if ProcessingRule.HandleUnrecognised
        File unrecognizedFilesFolder = new File(forReviewFolder, "UNRECOGNIZED/${dateString}/${processingParameters.titleCode}")

        boolean hasSipAndFilesFolder
        boolean hasUnrecognizedFilesFolder
        // Move or copy the processed files to the destination folder
        if ((sipProcessingState.validFiles.size() > 0 || sipProcessingState.invalidFiles.size() > 0)) {
            hasSipAndFilesFolder = true
            if (!sipAndFilesFolder.exists() && processorConfiguration.createDestination) {
                sipAndFilesFolder.mkdirs()
                contentStreamsFolder.mkdirs()
            }
        }
        ProcessorUtils.copyOrMoveFiles(processorConfiguration.moveFiles, sipProcessingState.validFiles, contentStreamsFolder)
        if (processingParameters.processingRules.contains(ProcessingRule.HandleInvalid)) {
            ProcessorUtils.copyOrMoveFiles(processorConfiguration.moveFiles, sipProcessingState.invalidFiles, contentStreamsFolder)
        }

        if (processingParameters.processingRules.contains(ProcessingRule.HandleUnrecognised)) {
            // If the files aren't recognized, then dump the files in an exception folder
            if (sipProcessingState.unrecognizedFiles.size() > 0) {
                hasUnrecognizedFilesFolder = true
                if (!unrecognizedFilesFolder.exists() && processorConfiguration.createDestination) {
                    unrecognizedFilesFolder.mkdirs()
                }
            }
            ProcessorUtils.copyOrMoveFiles(processorConfiguration.moveFiles, sipProcessingState.unrecognizedFiles, unrecognizedFilesFolder)
        }

        // Write out the FairfaxProcessingParameters and SipProcessingState
        Date now = new Date()
        // We will assume that millisecond timestamps ensures that the filename will be unique
        File processingStateFile = new File(titleCodeFolder,
                "${processingParameters.processingDifferentiator()}_parameters-and-state_${ProcessorUtils.FILE_TIMESTAMP_FORMATTER.format(now)}.txt")
        processingStateFile.write(processingParameters.detailedDisplay(0, true))
        ProcessorUtils.copyOrMoveFiles(false, [ processingStateFile ], sipAndFilesFolder)

        // Write out the SIP file
        File sipFile = new File(sipAndFilesFolder, "content/mets.xml")
        sipFile.write(sipAsXml)

        log.info("END processTitleCodeFolder for pattern=${pattern}, titleCodeFolder=${titleCodeFolder.getCanonicalPath()}")
        processorConfiguration.timekeeper.logElapsed()

        if (hasSipAndFilesFolder) {
            processLogger.copySplit(sipAndFilesFolder, "Process-Name-Folder", !hasUnrecognizedFilesFolder)
        }
        if (hasUnrecognizedFilesFolder) {
            processLogger.copySplit(unrecognizedFilesFolder, "Process-Name-Folder", true)
        }
        return sipProcessingState
    }

    // See README.md for folder descriptions and structures.
    void process() {
        log.info("START ready-for-ingestion with parameters:")
        log.info("    startindDate=${processorConfiguration.startingDate}")
        log.info("    endingDate=${processorConfiguration.endingDate}")
        log.info("    sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}")
        log.info("    targetForIngestionFolder=${processorConfiguration.targetForIngestionFolder.getCanonicalPath()}")
        log.info("    forReviewFolder=${processorConfiguration.forReviewFolder.getCanonicalPath()}")
        processorConfiguration.timekeeper.logElapsed()

        if (processorConfiguration.createDestination) {
            processorConfiguration.targetForIngestionFolder.mkdirs()
            processorConfiguration.forReviewFolder.mkdirs()
        }

        this.fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

        // First, collect all the directories to process
        List<Tuple2<File, String>> titleCodeFoldersAndDates = [ ]

        // Loop through the dates in sequence, finding and processing files
        LocalDate currentDate = processorConfiguration.startingDate
        while (currentDate <= processorConfiguration.endingDate) {
            String currentDateString = FairfaxFile.LOCAL_DATE_TIME_FORMATTER.format(currentDate)
            File dateFolder = new File(processorConfiguration.sourceFolder, currentDateString)
            if (dateFolder.exists() && dateFolder.isDirectory()) {
                dateFolder.listFiles().each { File subFile ->
                    if (subFile.isDirectory()) {
                        Tuple2<File, String> titleCodeFolderAndDate = new Tuple2<>(subFile, currentDateString)
                        titleCodeFoldersAndDates.add(titleCodeFolderAndDate)
                    } else {
                        log.info("Skipping ${subFile.getCanonicalPath()} as not directory=${subFile.isDirectory()}")
                    }
                }
            } else {
                log.info("Skipping ${dateFolder.getCanonicalPath()} as exists=${dateFolder.exists()}, directory=${dateFolder.isDirectory()}")
            }
            currentDate = currentDate.plusDays(1L)
        }

        log.info("Collected total titleCode directories to " +
                "process=${ProcessorUtils.TOTAL_FORMAT.format(titleCodeFoldersAndDates.size())}")
        int numberOfThreads = processorConfiguration.parallelizeProcessing ? processorConfiguration.numberOfThreads : 1
        if (numberOfThreads > 1) {
            numberOfThreads = 1
            log.info("Currently multi-threading is not supported for ReadyForIngestionProcessor as ready-for-ingestion")
            log.info("requires that each directory processed has an individual log of that processing stored with the files")
            log.info("and there currently doesn't exist a way to differentiate console output by the thread that produced the message.")
        }
        // log.info("Processing over numberOfThreads=${numberOfThreads}")

        // Process the collected directories across multiple threads
        GParsPool.withPool(numberOfThreads) {
            titleCodeFoldersAndDates.eachParallel { Tuple2<File, String> titleCodeFolderAndDateString ->
                // we want to process this directory, which should be a <titleCode>
                File titleCodeFolder = titleCodeFolderAndDateString.first
                String titleCode = titleCodeFolder.getName()
                String dateString = titleCodeFolderAndDateString.second
                LocalDate processingDate = LocalDate.parse(dateString, FairfaxFile.LOCAL_DATE_TIME_FORMATTER)
                FairfaxProcessingParameters processingParameters = FairfaxProcessingParameters.build(titleCode,
                        this.processingType, processingDate, fairfaxSpreadsheet)
                processingParameters.overrideProcessingRules(this.processingRules)
                processingParameters.overrideProcessingOptions(this.processingOptions)
                if (processingParameters.processingRules.contains(ProcessingRule.MultipleEditions)) {
                    processingParameters.editionDiscriminators.each { String editionDiscriminator ->
                        FairfaxProcessingParameters editionParameters = processingParameters.clone()
                        editionParameters.currentEdition = editionDiscriminator
                        processTitleCodeFolder(titleCodeFolder, processorConfiguration.targetForIngestionFolder,
                                processorConfiguration.forReviewFolder, dateString, editionParameters)
                    }
                } else {
                    processTitleCodeFolder(titleCodeFolder, processorConfiguration.targetForIngestionFolder,
                            processorConfiguration.forReviewFolder, dateString, processingParameters)
                }
            }
        }
    }

}
