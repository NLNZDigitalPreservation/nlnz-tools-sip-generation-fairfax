package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Log4j2
import groovyx.gpars.GParsExecutorsPool
import nz.govt.natlib.tools.sip.logging.DefaultTimekeeper
import nz.govt.natlib.tools.sip.logging.JvmPerformanceLogger
import nz.govt.natlib.tools.sip.logging.Timekeeper
import nz.govt.natlib.tools.sip.processing.PerThreadLogFileAppender
import nz.govt.natlib.tools.sip.utils.GeneralUtils
import nz.govt.natlib.tools.sip.utils.PathUtils

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

/**
 * For calling from gradle build scripts.
 */
@Log4j2
class PostIngestionProcessor {
    static final String POST_INGESTION_COMPLETED_FILENAME = "post-ingestion-FOLDER-COMPLETED"
    // TODO This might be better configurable or as a general option in ProcessorRunner.
    static final String KILL_FILE_NAME = "post-ingestion-STOP"
    static final String DONE_FILE_PATTERN = Pattern.quote("done")

    ProcessorConfiguration processorConfiguration

    List<Tuple2<Path, String>> failureFolderAndReasons = Collections.synchronizedList([ ])
    List<Tuple2<Path, String>> skippedFolderAndReasons = Collections.synchronizedList([ ])

    AtomicBoolean killInitiationLogged = new AtomicBoolean(false)
    AtomicBoolean shutdownInitiated = new AtomicBoolean(false)

    AtomicInteger processingCount = new AtomicInteger(0)

    PostIngestionProcessor(ProcessorConfiguration processorConfiguration) {
        this.processorConfiguration = processorConfiguration
    }

    // file structure for post-processing is the following:
    // <newspaper|magazine>/<title_code>/<year>/<date_titleCode_other-information>/
    //                                                       |- done
    //                                                       |- content/
    //                                                       |       |- mets.xml
    //                                                       |       |- streams/
    //                                                       |               |- <file_name>
    //                                                       |- processing, log, other files
    void processDoneFolder(String titleCode, LocalDate folderDate, Path doneFolder) {
        String magazineOrNewspaper = magazineOrNewspaperFromPath(doneFolder)
        String appendPath = magazineOrNewspaper + File.separator + titleCode + File.separator + folderDate.year +
                File.separator + doneFolder.fileName
        Path targetFolder = processorConfiguration.targetPostProcessedFolder.resolve(appendPath)

        UUID appenderId = UUID.randomUUID()
        Path processLoggingFile = PerThreadLogFileAppender.startWithGeneratedFilename(processorConfiguration.sourceFolder,
                "${doneFolder.fileName}_post-ingestion-processing-log", appenderId)

        try {
            log.info("START Processing doneFolder=${doneFolder}")
            processorConfiguration.timekeeper.logElapsed()

            PathUtils.atomicMoveOrCopyDirectory(processorConfiguration.moveFiles, doneFolder, targetFolder,
                    true, false, null)

            log.info("END Processing doneFolder=${doneFolder}")
            processorConfiguration.timekeeper.logElapsed()

            if (targetFolder != null && Files.exists(targetFolder)) {
                Path completedFile = targetFolder.resolve(POST_INGESTION_COMPLETED_FILENAME)
                Files.createFile(completedFile)
            }
        } finally {
            PerThreadLogFileAppender.stopAndRemove(appenderId)
            if (targetFolder != null) {
                if (processLoggingFile != null && Files.exists(processLoggingFile)) {
                    PathUtils.copyOrMoveFiles(true, [ processLoggingFile ], targetFolder)
                }
            }
        }
    }

    String magazineOrNewspaperFromPath(Path path) {
        List<String> segments = PathUtils.asSegments(path)
        return segments.contains("magazine") ? "magazine" : "newspaper"
    }

    List<Path> collectDoneFolders() {
        List<Path> allFolders
        if (processorConfiguration.moveOrCopyEvenIfNoRosettaDoneFile) {
            log.info("moveOrCopyEvenIfNoRosettaDoneFile has been set, collecting all folders in ${processorConfiguration.sourceFolder.normalize().toString()}")
            allFolders = processorConfiguration.sourceFolder.toFile().listFiles().collect { File file ->
                file.toPath()
            }
        } else {
            boolean isRegexNotGlob = true
            boolean matchFilenameOnly = true
            boolean sortFiles = true

            log.info("Processing for pattern=${DONE_FILE_PATTERN}, sourceFolder=${processorConfiguration.sourceFolder.normalize().toString()}")

            List<Path> doneFiles = PathUtils.findFiles(processorConfiguration.sourceFolder.normalize().toString(), isRegexNotGlob,
                    matchFilenameOnly, sortFiles, DONE_FILE_PATTERN, processorConfiguration.timekeeper)
            allFolders = doneFiles.collect { Path doneFile ->
                doneFile.parent
            }
        }
        return allFolders
    }

    // See documentation for folder descriptions and structures.
    void process() {
        log.info("START post-ingestion processor with parameters:")
        log.info("    startindDate=${processorConfiguration.startingDate}")
        log.info("    endingDate=${processorConfiguration.endingDate}")
        log.info("    sourceFolder=${processorConfiguration.sourceFolder.normalize().toString()}")
        log.info("    targetPostProcessedFolder=${processorConfiguration.targetPostProcessedFolder.normalize().toString()}")
        log.info("    forReviewFolder=${processorConfiguration.forReviewFolder.normalize().toString()}")
        log.info("${System.lineSeparator()}To initiate a graceful shutdown, use ^C or create a file=${getKillFile().normalize().toString()}")

        processorConfiguration.timekeeper.logElapsed()
        Timekeeper processingTimekeeper = new DefaultTimekeeper()

        if (processorConfiguration.createDestination) {
            Files.createDirectories(processorConfiguration.targetPostProcessedFolder)
            Files.createDirectories(processorConfiguration.forReviewFolder)
        }
        List<Path> doneFolders = collectDoneFolders()

        log.info("Collected total 'done' folders to process=${GeneralUtils.TOTAL_FORMAT.format(doneFolders.size())}")
        int numberOfThreads = processorConfiguration.parallelizeProcessing ? processorConfiguration.numberOfThreads : 1
        log.info("Processing over numberOfThreads=${numberOfThreads}")

        JvmPerformanceLogger.logState("PostIngestionProcessor Current thread state at start of ALL processing",
                true, true, true, false, true, true, true)

        setupShutdownHook()
        // Process the collected directories across multiple threads
        // Note for debugging: GParsExecutorPool and GParsPool will collect any exceptions thrown in the block and then
        // list them after all the threads have finished processing everything in the block. This can make it difficult
        // to debug.
        GParsExecutorsPool.withPool(numberOfThreads) {
            doneFolders.eachParallel { Path doneFolder ->
                try {
                    if (continueProcessing) {
                        // We use the ReadyForIngestionProcessor to get the titleCode and date of the parent folder
                        Tuple2<String, LocalDate> parentFolderTitleCode = ReadyForIngestionProcessor.parseFolderNameForTitleCodeAndDate(doneFolder.fileName.toString())
                        String titleCode = parentFolderTitleCode.first
                        LocalDate folderDate = parentFolderTitleCode.second
                        if (processorConfiguration.startingDate <= folderDate && folderDate <= processorConfiguration.endingDate) {
                            processDoneFolder(titleCode, folderDate, doneFolder)
                        } else {
                            log.warn("Skipping processing of doneFolder=${doneFolder}, folderDate=${folderDate} outside of date range.")
                            Tuple2<Path, String> folderAndReason = new Tuple2(doneFolder, "Skipping processing, folderDate=${folderDate} outside of date range.")
                            skippedFolderAndReasons.add(folderAndReason)
                        }
                    } else {
                        log.warn("Processing terminating, skipping processing of doneFolder=${doneFolder}")
                        Tuple2<Path, String> folderAndReason = new Tuple2(doneFolder, "Processing terminating, skipping processing.")
                        skippedFolderAndReasons.add(folderAndReason)
                    }
                } catch (Exception e) {
                    log.error("Exception processing doneFolder=${doneFolder}, note that Processing WILL continue", e)
                    Tuple2<Path, String> fileAndReason = new Tuple2(doneFolder, e.toString())
                    failureFolderAndReasons.add(fileAndReason)
                } catch (OutOfMemoryError e) {
                    log.error("Exception processing doneFolder=${doneFolder}, note that Processing WILL continue", e)
                    JvmPerformanceLogger.logState("PostIngestionProcessor Current thread state at end of doneFolder=${doneFolder}",
                            false, true, true, false, true, true, true)
                    Tuple2<Path, String> fileAndReason = new Tuple2(doneFolder, e.toString())
                    failureFolderAndReasons.add(fileAndReason)
                }
            }
            int currentProcessingCount = processingCount.addAndGet(1)
            if (currentProcessingCount % 100) {
                log.info("Processing done files, processing count=${currentProcessingCount}/${doneFolders.size()}")
            }
        }
        JvmPerformanceLogger.logState("PostIngestionProcessor Current thread state at end of ALL processing",
                true, true, true, false, true, true, true)
        log.info("${System.lineSeparator()}END ready-for-ingestion with parameters:")
        log.info("    startindDate=${processorConfiguration.startingDate}")
        log.info("    endingDate=${processorConfiguration.endingDate}")
        log.info("    sourceFolder=${processorConfiguration.sourceFolder.normalize().toString()}")
        log.info("    targetPostProcessedFolder=${processorConfiguration.targetPostProcessedFolder.normalize().toString()}")
        log.info("    forReviewFolder=${processorConfiguration.forReviewFolder.normalize().toString()}")
        log.info("${System.lineSeparator()}${System.lineSeparator()}Summary:")
        log.info("    Total done files processed=${GeneralUtils.TOTAL_FORMAT.format(doneFolders.size())}")
        log.info("    Processed with numberOfThreads=${GeneralUtils.TOTAL_FORMAT.format(numberOfThreads)}")
        processingTimekeeper.stop()
        processingTimekeeper.logElapsed(false, doneFolders.size(), true)
        log.info("${System.lineSeparator()}Total elapsed:")
        processorConfiguration.timekeeper.logElapsed()

        if (skippedFolderAndReasons.size() > 0) {
            log.info("${System.lineSeparator()}Folder processing skipped total=${skippedFolderAndReasons.size()}")
            skippedFolderAndReasons.each { Tuple2<Path, String> folderAndReason ->
                log.info("    Skipped 'done' folder=${folderAndReason.first}, reason=${folderAndReason.second}")
            }
        }

        if (failureFolderAndReasons.size() > 0) {
            log.info("${System.lineSeparator()}Folder processing failures total=${failureFolderAndReasons.size()}")
            failureFolderAndReasons.each { Tuple2<Path, String> folderAndReason ->
                log.info("    Failure 'done' folder=${folderAndReason.first}, reason=${folderAndReason.second}")
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
        boolean killFileExists = Files.exists(getKillFile())
        boolean doContinue = !killFileExists
        if (doContinue) {
            if (killInitiationLogged.get()) {
                // We had already initiated a shutdown, but now we are continuing...
                killInitiationLogged.compareAndExchange(true, false)
                log.warn("Processing will CONTINUE as killFile=${killFile.normalize().toString()} has disappeared. Skipped processing up to this point will NOT be done.")
            }
        } else {
            if (!killInitiationLogged.get()) {
                killInitiationLogged.compareAndExchange(false, true)
                log.warn("Processing will STOP as killFile=${killFile.normalize().toString()} exists. All threads must complete their current processing before termination.")
            }
        }

        return doContinue
    }

    Path getKillFile() {
        return processorConfiguration.targetPostProcessedFolder.resolve(KILL_FILE_NAME)
    }
}
