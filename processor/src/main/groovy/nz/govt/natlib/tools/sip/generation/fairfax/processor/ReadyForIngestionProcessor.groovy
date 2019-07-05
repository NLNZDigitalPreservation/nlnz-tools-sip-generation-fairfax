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
import nz.govt.natlib.tools.sip.logging.JvmPerformanceLogger
import nz.govt.natlib.tools.sip.logging.Timekeeper
import nz.govt.natlib.tools.sip.pdf.thumbnail.ThreadedThumbnailGenerator
import nz.govt.natlib.tools.sip.processing.PerThreadLogFileAppender
import nz.govt.natlib.tools.sip.state.SipProcessingException
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReason
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType
import nz.govt.natlib.tools.sip.state.SipProcessingState
import nz.govt.natlib.tools.sip.utils.FileUtils
import nz.govt.natlib.tools.sip.utils.GeneralUtils
import org.apache.commons.io.FilenameUtils

import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

/**
 * For calling from gradle build scripts.
 */
@Log4j2
class ReadyForIngestionProcessor {
    static final String READY_FOR_INGESTION_COMPLETED_FILENAME = "ready-for-ingestion-FOLDER-COMPLETED"
    // TODO This might be better configurable or as a general option in ProcessorRunner.
    static final String KILL_FILE_NAME = "ready-for-ingestion-STOP"

    FairfaxSpreadsheet fairfaxSpreadsheet
    ProcessorConfiguration processorConfiguration
    List<ProcessingType> processingTypes
    List<ProcessingRule> overrideProcessingRules = [ ]
    List<ProcessingOption> overrideProcessingOptions = [ ]

    List<Tuple2<File, String>> failureFolderAndReasons = Collections.synchronizedList([ ])
    List<Tuple2<File, String>> skippedFolderAndReasons = Collections.synchronizedList([ ])

    AtomicBoolean killInitiationLogged = new AtomicBoolean(false)
    AtomicBoolean shutdownInitiated = new AtomicBoolean(false)

    AtomicInteger processingCount = new AtomicInteger(0)

    // Since the ReadyForIngestionProcessor creates this name, it can also parse it for other consumers.
    static Tuple2<String, LocalDate> parseFolderNameForTitleCodeAndDate(String folderName) {
        List<String> folderNameComponents = folderName.split(Pattern.quote("_"))
        LocalDate date = null
        String titleCode = null
        if (folderNameComponents.size() > 0) {
            String dateString = folderNameComponents.get(0)
            try {
                date = LocalDate.parse(dateString, FairfaxFile.LOCAL_DATE_TIME_FORMATTER)
            } catch (DateTimeParseException e) {
                log.warn("Unable to parse dateString=${dateString} to LocalDate. Using null.")
            }
        }
        if (folderNameComponents.size() > 1) {
            titleCode = folderNameComponents.get(1)
        }

        Tuple2<String, LocalDate> titleCodeAndDate = new Tuple2(titleCode, date)

        return titleCodeAndDate
    }

    static String assembleSipAndFilesFolderName(String dateString, String titleCode, String type, String identifier) {
        String folderName = "${dateString}_${titleCode}_${type}"
        if (identifier != null) {
            folderName = "${folderName}_${identifier}"
        }

        return folderName
    }

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
            log.info("START Processing sourceFolder=${processingParameters.sourceFolderPath()}")
            processorConfiguration.timekeeper.logElapsed()

            if (processingParameters.valid) {
                processValidTitleCodeFolder(processingParameters)
            }

            if (processingParameters.skip) {
                log.info("Skipping processing of sourceFolder=${processingParameters.sourceFolderPath()}")
            } else {
                sipAndFilesFolder = postProcess(processingParameters, destinationFolder, forReviewFolder, dateString)
            }

            log.info("END Processing sourceFolder=${processingParameters.sourceFolderPath()}")
            log.info("${System.lineSeparator()}FairfaxProcessingParameters and SipProcessingState:")
            log.info(processingParameters.detailedDisplay(0, true))
            log.info(System.lineSeparator())

            processorConfiguration.timekeeper.logElapsed()

            if (sipAndFilesFolder != null && sipAndFilesFolder.exists()) {
                File completedFile = new File(sipAndFilesFolder, READY_FOR_INGESTION_COMPLETED_FILENAME)
                completedFile.createNewFile()
            }
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

    File postProcess(FairfaxProcessingParameters processingParameters, File destinationFolder, File forReviewFolder,
                     String dateString) {
        File sourceFolder = processingParameters.sourceFolder
        File sipAndFilesFolder
        SipProcessingState sipProcessingState = processingParameters.sipProcessingState

        String folderName = assembleSipAndFilesFolderName(dateString, processingParameters.titleCode,
                processingParameters.type.getFieldValue(), sipProcessingState.identifier)
        String typeFolderNamePath = "${sipProcessingState.ieEntityType.getDisplayName()}${File.separator}${folderName}"
        if (sipProcessingState.complete && sipProcessingState.successful) {
            sipAndFilesFolder = new File(destinationFolder, typeFolderNamePath)
        } else {
            sipAndFilesFolder = new File(forReviewFolder,
                    "${sipProcessingState.failureReasonSummary}${File.separator}${typeFolderNamePath}")
        }
        File contentStreamsFolder = new File(sipAndFilesFolder, FilenameUtils.separatorsToSystem("content/streams"))
        // Note that unrecognized only gets moved/copied if ProcessingRule.HandleUnrecognised
        String invalidPath = FilenameUtils.separatorsToSystem("INVALID/${dateString}/${processingParameters.titleCode}")
        File invalidFilesFolder = new File(forReviewFolder, invalidPath)
        String ignoredPath = FilenameUtils.separatorsToSystem("IGNORED/${dateString}/${processingParameters.titleCode}")
        File ignoredFilesFolder = new File(forReviewFolder, ignoredPath)
        String unrecognizedPath = FilenameUtils.separatorsToSystem("UNRECOGNIZED/${dateString}/${processingParameters.titleCode}")
        File unrecognizedFilesFolder = new File(forReviewFolder, unrecognizedPath)

        boolean hasSipAndFilesFolder
        boolean hasInvalidFilesFolder
        boolean hasIgnoredFilesFolder
        boolean hasUnrecognizedFilesFolder
        // Move or copy the processed files to the destination folder
        if ((sipProcessingState.validFiles.size() > 0 || sipProcessingState.invalidFiles.size() > 0)) {
            hasSipAndFilesFolder = sipAndFilesFolder.exists()
            if (!hasSipAndFilesFolder && processorConfiguration.createDestination) {
                sipAndFilesFolder.mkdirs()
                hasSipAndFilesFolder = true
                contentStreamsFolder.mkdirs()
            }
        } else {
            hasSipAndFilesFolder = sipAndFilesFolder.exists()
            if (!hasSipAndFilesFolder && processorConfiguration.createDestination) {
                sipAndFilesFolder.mkdirs()
                hasSipAndFilesFolder = true
            }
        }
        FileUtils.copyOrMoveFiles(processorConfiguration.moveFiles, sipProcessingState.sipFiles, contentStreamsFolder)
        if (processingParameters.rules.contains(ProcessingRule.HandleInvalid)) {
            // If the files are invalid, then dump the files in an exception folder.
            if (sipProcessingState.invalidFiles.size() > 0) {
                hasInvalidFilesFolder = invalidFilesFolder.exists()
                if (!hasInvalidFilesFolder && processorConfiguration.createDestination) {
                    invalidFilesFolder.mkdirs()
                    hasInvalidFilesFolder = true
                }
            }
            if (hasInvalidFilesFolder) {
                FileUtils.copyOrMoveFiles(processorConfiguration.moveFiles, sipProcessingState.invalidFiles, invalidFilesFolder)
            }
        }

        if (processingParameters.rules.contains(ProcessingRule.HandleIgnored)) {
            // If the files are ignored, then dump the files in an exception folder.
            if (sipProcessingState.ignoredFiles.size() > 0) {
                hasIgnoredFilesFolder = ignoredFilesFolder.exists()
                if (!hasIgnoredFilesFolder && processorConfiguration.createDestination) {
                    ignoredFilesFolder.mkdirs()
                    hasIgnoredFilesFolder = true
                }
            }
            if (hasIgnoredFilesFolder) {
                FileUtils.copyOrMoveFiles(processorConfiguration.moveFiles, sipProcessingState.ignoredFiles, ignoredFilesFolder)
            }
        }

        if (processingParameters.rules.contains(ProcessingRule.HandleUnrecognised)) {
            // If the files aren't recognized, then dump the files in an exception folder.
            if (sipProcessingState.unrecognizedFiles.size() > 0) {
                hasUnrecognizedFilesFolder = unrecognizedFilesFolder.exists()
                if (!hasUnrecognizedFilesFolder && processorConfiguration.createDestination) {
                    unrecognizedFilesFolder.mkdirs()
                    hasUnrecognizedFilesFolder = true
                }
            }
            if (hasUnrecognizedFilesFolder) {
                FileUtils.copyOrMoveFiles(processorConfiguration.moveFiles, sipProcessingState.unrecognizedFiles, unrecognizedFilesFolder)
            }
        }

        // Write out the SIP file
        String sipAsXml = processingParameters.sipProcessingState.sipAsXml
        if (processingParameters.valid && !sipAsXml.isEmpty()) {
            File sipFile = new File(sipAndFilesFolder, FilenameUtils.separatorsToSystem("content/mets.xml"))
            sipFile.withWriter(StandardCharsets.UTF_8.name()) { Writer writer ->
                writer.write(sipAsXml)
            }
        }

        // Write out the FairfaxProcessingParameters and SipProcessingState
        Date now = new Date()
        // We will assume that millisecond timestamps ensures that the filename will be unique
        File processingStateFile = new File(sourceFolder,
                "${processingParameters.processingDifferentiator()}_parameters-and-state_${FileUtils.FILE_TIMESTAMP_FORMATTER.format(now)}.txt")
        processingStateFile.withWriter(StandardCharsets.UTF_8.name()) { Writer writer ->
            writer.write(processingParameters.detailedDisplay(0, true))
        }
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
        sipAndFilesFolder
    }

    void processValidTitleCodeFolder(FairfaxProcessingParameters processingParameters) {
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
        FairfaxFilesProcessor.processCollectedFiles(processingParameters, allFiles)
    }

    // See README.md for folder descriptions and structures.
    void process() {
        log.info("START ready-for-ingestion with parameters:")
        log.info("    startindDate=${processorConfiguration.startingDate}")
        log.info("    endingDate=${processorConfiguration.endingDate}")
        log.info("    sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}")
        log.info("    targetForIngestionFolder=${processorConfiguration.targetForIngestionFolder.getCanonicalPath()}")
        log.info("    forReviewFolder=${processorConfiguration.forReviewFolder.getCanonicalPath()}")
        log.info("${System.lineSeparator()}To initiate a graceful shutdown, use ^C or create a file=${getKillFile().canonicalPath}")

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

        JvmPerformanceLogger.logState("ReadyForIngestionProcessor Current thread state at start of ALL processing",
                true, true, true, false, true, true, true)

        List<File> invalidFolders = Collections.synchronizedList([ ])

        setupShutdownHook()
        // Process the collected directories across multiple threads
        // Note for debugging: GParsExecutorPool and GParsPool will collect any exceptions thrown in the block and then
        // list them after all the threads have finished processing everything in the block. This can make it difficult
        // to debug.
        GParsExecutorsPool.withPool(numberOfThreads) {
            titleCodeFoldersAndDates.eachParallel { Tuple2<File, String> titleCodeFolderAndDateString ->
                File titleCodeFolder = titleCodeFolderAndDateString.first
                String titleCode = titleCodeFolder.getName()
                String dateString = titleCodeFolderAndDateString.second
                String titleCodeFolderMessage = "titleCode=${titleCode}, date=${dateString}, folder=${titleCodeFolder.canonicalPath}"
                try {
                    if (continueProcessing) {
                        JvmPerformanceLogger.logState("ReadyForIngestionProcessor Current thread state at start of ${titleCodeFolderMessage}",
                                false, true, true, false, true, false, true)
                        // we want to process this directory, which should be a <titleCode>
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
                        JvmPerformanceLogger.logState("ReadyForIngestionProcessor Current thread state at end of ${titleCodeFolderMessage}",
                                false, true, true, false, true, false, true)
                    } else {
                        log.warn("Processing terminating, skipping processing of ${titleCodeFolderMessage}")
                        Tuple2<File, String> folderAndReason = new Tuple2(titleCodeFolder, "Processing terminating, skipping processing.")
                        skippedFolderAndReasons.add(folderAndReason)
                    }
                } catch (Exception e) {
                    log.error("Exception processing ${titleCodeFolderMessage}, note that Processing WILL continue", e)
                    Tuple2<File, String> fileAndReason = new Tuple2(titleCodeFolder, e.toString())
                    failureFolderAndReasons.add(fileAndReason)
                } catch (OutOfMemoryError e) {
                    log.error("Exception processing ${titleCodeFolderMessage}, note that Processing WILL continue", e)
                    log.error("Number of threads currently generating thumbnails queue length=${ThreadedThumbnailGenerator.numberThreadsGeneratingThumbnails()}")
                    JvmPerformanceLogger.logState("ReadyForIngestionProcessor Current thread state at end of ${titleCodeFolderMessage}",
                            false, true, true, false, true, true, true)
                    Tuple2<File, String> fileAndReason = new Tuple2(titleCodeFolder, e.toString())
                    failureFolderAndReasons.add(fileAndReason)
                }
                int currentProcessingCount = processingCount.addAndGet(1)
                if (currentProcessingCount % 100) {
                    log.info("Processing titleCode folders, processing count=${currentProcessingCount}/${titleCodeFoldersAndDates.size()} folders")
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
        JvmPerformanceLogger.logState("ReadyForIngestionProcessor Current thread state at end of ALL processing",
                true, true, true, false, true, true, true)
        log.info("${System.lineSeparator()}END ready-for-ingestion with parameters:")
        log.info("    startindDate=${processorConfiguration.startingDate}")
        log.info("    endingDate=${processorConfiguration.endingDate}")
        log.info("    sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}")
        log.info("    targetForIngestionFolder=${processorConfiguration.targetForIngestionFolder.getCanonicalPath()}")
        log.info("    forReviewFolder=${processorConfiguration.forReviewFolder.getCanonicalPath()}")
        log.info("${System.lineSeparator()}${System.lineSeparator()}Summary:")
        log.info("    Total folders processed=${GeneralUtils.TOTAL_FORMAT.format(titleCodeFoldersAndDates.size())}")
        log.info("    Processed with numberOfThreads=${GeneralUtils.TOTAL_FORMAT.format(numberOfThreads)}")
        log.info("    Maximum number of threads processing thumbnails=${GeneralUtils.TOTAL_FORMAT.format(processorConfiguration.maximumThumbnailPageThreads)}")
        processingTimekeeper.stop()
        processingTimekeeper.logElapsed(false, titleCodeFoldersAndDates.size(), true)
        log.info("${System.lineSeparator()}Total elapsed:")
        processorConfiguration.timekeeper.logElapsed()

        if (skippedFolderAndReasons.size() > 0) {
            log.info("${System.lineSeparator()}Folder processing skipped total=${skippedFolderAndReasons.size()}")
            skippedFolderAndReasons.each { Tuple2<File, String> folderAndReason ->
                log.info("    Skipped folder=${folderAndReason.first.canonicalPath}, reason=${folderAndReason.second}")
            }
        }

        if (failureFolderAndReasons.size() > 0) {
            log.info("${System.lineSeparator()}Folder processing failures total=${failureFolderAndReasons.size()}")
            failureFolderAndReasons.each { Tuple2<File, String> folderAndReason ->
                log.info("    Failure folder=${folderAndReason.first.canonicalPath}, reason=${folderAndReason.second}")
            }
        }
    }

    // TODO This does not seem to be working properly, even a kill <pid> still terminates immediately, same with ^C
    void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread("${this.class.name}-Shutdown-hook") {
            void run() {
                try {
                    log.warn("Shutdown hook called, attempting graceful shutdown by running existing processes until they finish.")
                    shutdownInitiated.set(true)
                } catch (InterruptedException e) {
                    log.error("Unexpected exception in shutdown hook, exception=", e)
                }
            }
        })
        log.info("Shutdown hook setup.")
    }

    boolean isContinueProcessing() {
        if (shutdownInitiated.get()) {
            return false
        }

        // Note that this value may switch back and forth while the processor is shutting down if the kill file is
        // repeatedly deleted/added. That's not necessarily a prohibited action.
        boolean killFileExists = getKillFile().exists()
        boolean doContinue = !killFileExists
        if (doContinue) {
            if (killInitiationLogged.get()) {
                // We had already initiated a shutdown, but now we are continuing...
                killInitiationLogged.compareAndExchange(true, false)
                log.warn("Processing will CONTINUE as killFile=${killFile.canonicalPath} has disappeared. Skipped processing up to this point will NOT be done.")
            }
        } else {
            if (!killInitiationLogged.get()) {
                killInitiationLogged.compareAndExchange(false, true)
                log.warn("Processing will STOP as killFile=${killFile.canonicalPath} exists. All threads must complete their current processing before termination.")
            }
        }

        return doContinue
    }

    File getKillFile() {
        return new File(processorConfiguration.targetForIngestionFolder, KILL_FILE_NAME)
    }
}
