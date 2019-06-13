package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Log4j2
import groovyx.gpars.GParsExecutorsPool
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType
import nz.govt.natlib.tools.sip.logging.DefaultTimekeeper
import nz.govt.natlib.tools.sip.logging.Timekeeper
import nz.govt.natlib.tools.sip.pdf.thumbnail.ThreadedThumbnailGenerator
import nz.govt.natlib.tools.sip.processing.PerThreadLogFileAppender
import nz.govt.natlib.tools.sip.state.SipProcessingException
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReason
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType
import nz.govt.natlib.tools.sip.state.SipProcessingState
import nz.govt.natlib.tools.sip.utils.FileUtils
import nz.govt.natlib.tools.sip.utils.GeneralUtils

import java.time.LocalDate

/**
 * For calling from gradle build scripts.
 */
@Log4j2
class ReadyForIngestionProcessor {
    FairfaxSpreadsheet fairfaxSpreadsheet
    ProcessorConfiguration processorConfiguration
    List<ProcessingType> processingTypes
    List<ProcessingRule> overrideProcessingRules = [ ]
    List<ProcessingOption> overrideProcessingOptions = [ ]

    ReadyForIngestionProcessor(ProcessorConfiguration processorConfiguration) {
        this.processorConfiguration = processorConfiguration
        this.processingTypes = ProcessingType.extract(this.processorConfiguration.forIngestionProcessingTypes, ",")
        if (processingTypes.isEmpty()) {
            String message = "No acceptable value for processingTypes=${this.processorConfiguration.forIngestionProcessingTypes}".toString()
            SipProcessingException exception = new SipProcessingExceptionReason(
                    SipProcessingExceptionReasonType.INVALID_PARAMETERS, null, message)
            throw exception
        }
        this.overrideProcessingRules = ProcessingRule.extract(this.processorConfiguration.forIngestionProcessingRules,
                ",", [ ], true)
        this.overrideProcessingOptions = ProcessingOption.extract(this.processorConfiguration.forIngestionProcessingOptions,
                ",", [ ], true)
    }

    SipProcessingState processTitleCodeFolder(FairfaxProcessingParameters processingParameters, File destinationFolder,
                                              File forReviewFolder, String dateString) {
        // Process the files in the titleCode folder

        UUID appenderId = UUID.randomUUID()
        File processLoggingFile = PerThreadLogFileAppender.startWithGeneratedFilename(processingParameters.sourceFolder,
                "${processingParameters.processingDifferentiator()}_processing-log", appenderId)

        File sipAndFilesFolder

        try {
            File sourceFolder = processingParameters.sourceFolder

            log.info("START Processing sourceFolder=${processingParameters.sourceFolderPath()}")
            processorConfiguration.timekeeper.logElapsed()

            String sipAsXml = ""
            if (processingParameters.valid) {
                sipAsXml = processValidTitleCodeFolder(processingParameters)
            }

            SipProcessingState sipProcessingState = processingParameters.sipProcessingState

            String folderName = "${dateString}_${processingParameters.titleCode}_${processingParameters.type.getFieldValue()}"
            if (sipProcessingState.identifier != null) {
                folderName = "${folderName}_${sipProcessingState.identifier}"
            }
            String typeFolderNamePath = "${sipProcessingState.ieEntityType.getDisplayName()}${File.separator}${folderName}"
            if (sipProcessingState.complete && sipProcessingState.successful) {
                sipAndFilesFolder = new File(destinationFolder, typeFolderNamePath)
            } else {
                sipAndFilesFolder = new File(forReviewFolder,
                        "${sipProcessingState.failureReasonSummary}${File.separator}${typeFolderNamePath}")
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
            } else {
                hasSipAndFilesFolder = true
                if (!sipAndFilesFolder.exists() && processorConfiguration.createDestination) {
                    sipAndFilesFolder.mkdirs()
                }
            }
            FileUtils.copyOrMoveFiles(processorConfiguration.moveFiles, sipProcessingState.validFiles, contentStreamsFolder)
            if (processingParameters.rules.contains(ProcessingRule.HandleInvalid)) {
                FileUtils.copyOrMoveFiles(processorConfiguration.moveFiles, sipProcessingState.invalidFiles, contentStreamsFolder)
            }

            if (processingParameters.rules.contains(ProcessingRule.HandleUnrecognised)) {
                // If the files aren't recognized, then dump the files in an exception folder
                if (sipProcessingState.unrecognizedFiles.size() > 0) {
                    hasUnrecognizedFilesFolder = true
                    if (!unrecognizedFilesFolder.exists() && processorConfiguration.createDestination) {
                        unrecognizedFilesFolder.mkdirs()
                    }
                }
                FileUtils.copyOrMoveFiles(processorConfiguration.moveFiles, sipProcessingState.unrecognizedFiles, unrecognizedFilesFolder)
            }

            // Write out the SIP file
            if (processingParameters.valid && !sipAsXml.isEmpty()) {
                File sipFile = new File(sipAndFilesFolder, "content/mets.xml")
                sipFile.write(sipAsXml)
            }

            // Write out the FairfaxProcessingParameters and SipProcessingState
            Date now = new Date()
            // We will assume that millisecond timestamps ensures that the filename will be unique
            File processingStateFile = new File(sourceFolder,
                    "${processingParameters.processingDifferentiator()}_parameters-and-state_${FileUtils.FILE_TIMESTAMP_FORMATTER.format(now)}.txt")
            processingStateFile.write(processingParameters.detailedDisplay(0, true))
            if (sipAndFilesFolder.exists()) {
                FileUtils.copyOrMoveFiles(false, [processingStateFile], sipAndFilesFolder)
            }

            // Move the thumbnail page file to the sipAndFilesFolder
            if (processingParameters.thumbnailPageFile != null && processingParameters.thumbnailPageFile.exists()) {
                File thumbnailPageFileNewFile = new File(processingParameters.thumbnailPageFile.parentFile,
                        processingParameters.thumbnailPageFileFinalName)
                processingParameters.thumbnailPageFile.renameTo(thumbnailPageFileNewFile)
                FileUtils.copyOrMoveFiles(true, [thumbnailPageFileNewFile], sipAndFilesFolder)
            }

            log.info("END Processing sourceFolder=${processingParameters.sourceFolderPath()}")
            log.info("${System.lineSeparator()}FairfaxProcessingParameters and SipProcessingState:")
            log.info(processingParameters.detailedDisplay(0, true))
            log.info(System.lineSeparator())

            processorConfiguration.timekeeper.logElapsed()

        } finally {
            PerThreadLogFileAppender.stopAndRemove(appenderId)
            if (sipAndFilesFolder != null) {
                if (processLoggingFile != null && processLoggingFile.exists()) {
                    FileUtils.copyOrMoveFiles(false, [processLoggingFile], sipAndFilesFolder)
                }
            }
        }

        return processingParameters.sipProcessingState
    }

    String processValidTitleCodeFolder(FairfaxProcessingParameters processingParameters) {
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        // Only process PDF files
        String pattern = FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_PATTERN

        log.info("Processing for pattern=${pattern}, titleCodeFolder=${processingParameters.sourceFolderPath()}")

        List<File> allFiles = FileUtils.findFiles(processingParameters.sourceFolder.getAbsolutePath(),
                isRegexNotGlob, matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper)

        // Process the folder as a single collection of files
        // Note that the folder is processed for a single processingType (so there could be multiple passes, one for
        // each processingType).
        String sipAsXml = FairfaxFilesProcessor.processCollectedFiles(processingParameters, allFiles)

        return sipAsXml
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
        Timekeeper processingTimekeeper = new DefaultTimekeeper()

        if (processorConfiguration.createDestination) {
            processorConfiguration.targetForIngestionFolder.mkdirs()
            processorConfiguration.forReviewFolder.mkdirs()
        }

        this.fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

        // First, collect all the directories to process
        List<Tuple2<File, String>> titleCodeFoldersAndDates = [ ]

        // Loop through the dates in sequence, finding all the folders to process
        List<LocalDate> datesInRange = GeneralUtils.datesInRange(processorConfiguration.startingDate,
                processorConfiguration.endingDate)
        datesInRange.each { LocalDate currentDate ->
            String currentDateString = FairfaxFile.LOCAL_DATE_TIME_FORMATTER.format(currentDate)
            File dateFolder = new File(processorConfiguration.sourceFolder, currentDateString)
            if (dateFolder.exists() && dateFolder.isDirectory()) {
                dateFolder.listFiles().each { File subFile ->
                    if (subFile.isDirectory()) {
                        Tuple2<File, String> titleCodeFolderAndDate = new Tuple2<>(subFile, currentDateString)
                        titleCodeFoldersAndDates.add(titleCodeFolderAndDate)
                        log.info("Adding ${subFile.getCanonicalPath()} to list of titleCodeFolderAndDate")
                    } else {
                        log.info("Skipping ${subFile.getCanonicalPath()} as not directory=${subFile.isDirectory()}")
                    }
                }
            } else {
                log.info("Skipping ${dateFolder.getCanonicalPath()} as exists=${dateFolder.exists()}, directory=${dateFolder.isDirectory()}")
            }
        }

        log.info("Collected total titleCode directories to " +
                "process=${GeneralUtils.TOTAL_FORMAT.format(titleCodeFoldersAndDates.size())}")
        int numberOfThreads = processorConfiguration.parallelizeProcessing ? processorConfiguration.numberOfThreads : 1
        log.info("Processing over numberOfThreads=${numberOfThreads}")
        ThreadedThumbnailGenerator.changeMaximumConcurrentThreads(processorConfiguration.maximumThumbnailPageThreads)
        log.info("Maximum number of threads processing thumbnails=${processorConfiguration.maximumThumbnailPageThreads}")

        List<File> invalidFolders = Collections.synchronizedList([ ])
        // Process the collected directories across multiple threads
        // Note for debugging: GParsExecutorPool and GParsPool will collect any exceptions thrown in the block and then
        // list them after all the threads have finished processing everything in the block. This can make it difficult
        // to debug.
        GParsExecutorsPool.withPool(numberOfThreads) {
            titleCodeFoldersAndDates.eachParallel { Tuple2<File, String> titleCodeFolderAndDateString ->
                // we want to process this directory, which should be a <titleCode>
                File titleCodeFolder = titleCodeFolderAndDateString.first
                String titleCode = titleCodeFolder.getName()
                String dateString = titleCodeFolderAndDateString.second
                LocalDate processingDate = LocalDate.parse(dateString, FairfaxFile.LOCAL_DATE_TIME_FORMATTER)

                // Avoid issue when multiple threads iterating through this list.
                List<ProcessingType> perThreadProcessingTypes = (List<ProcessingType>) this.processingTypes.clone()
                List<ProcessingRule> perThreadOverrideRules = (List<ProcessingRule>) this.overrideProcessingRules.clone()
                List<ProcessingOption> perThreadOverrideOptions = (List<ProcessingOption>) this.overrideProcessingOptions.clone()

                List<FairfaxProcessingParameters> parametersList = FairfaxProcessingParameters.build(titleCode,
                        perThreadProcessingTypes, titleCodeFolder, processingDate, fairfaxSpreadsheet,
                        perThreadOverrideRules, perThreadOverrideOptions)

                parametersList.each { FairfaxProcessingParameters processingParameters ->
                    if (!processingParameters.valid) {
                        invalidFolders.add(titleCodeFolder)
                    }
                    processTitleCodeFolder(processingParameters, processorConfiguration.targetForIngestionFolder,
                            processorConfiguration.forReviewFolder, dateString)
                }
            }
        }
        if (invalidFolders.size() > 0) {
            log.warn("Summary of invalid folders:")
            invalidFolders.each { File folder ->
                String titleCode = folder.getName()
                log.warn("    titleCode=${titleCode}, invalid folder=${folder.getCanonicalPath()}")
            }
        }
        log.info("END ready-for-ingestion with parameters:")
        log.info("    startindDate=${processorConfiguration.startingDate}")
        log.info("    endingDate=${processorConfiguration.endingDate}")
        log.info("    sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}")
        log.info("    targetForIngestionFolder=${processorConfiguration.targetForIngestionFolder.getCanonicalPath()}")
        log.info("    forReviewFolder=${processorConfiguration.forReviewFolder.getCanonicalPath()}")
        log.info("${System.lineSeparator()}${System.lineSeparator()}Summary:")
        log.info("    Total folders processed=${GeneralUtils.TOTAL_FORMAT.format(titleCodeFoldersAndDates.size())}")
        log.info("    Processed with over numberOfThreads=${GeneralUtils.TOTAL_FORMAT.format(numberOfThreads)}")
        log.info("    Maximum number of threads processing thumbnails=${GeneralUtils.TOTAL_FORMAT.format(processorConfiguration.maximumThumbnailPageThreads)}")
        processingTimekeeper.stop()
        processingTimekeeper.logElapsed(false, titleCodeFoldersAndDates.size(), true)
        log.info("${System.lineSeparator()}Total elapsed:")
        processorConfiguration.timekeeper.logElapsed()
    }
}
