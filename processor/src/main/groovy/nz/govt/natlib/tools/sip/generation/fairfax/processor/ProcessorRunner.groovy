package nz.govt.natlib.tools.sip.generation.fairfax.processor

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper

import java.time.LocalDate
import java.util.concurrent.Callable

@Slf4j
@Command(description = 'Runs different processors based on command-line options.', name = 'processorRunner')
class ProcessorRunner implements ProcessorConfiguration, Callable<Void> {
    final static LocalDate DEFAULT_STARTING_DATE = LocalDate.of(2015, 1, 1)
    final static LocalDate DEFAULT_ENDING_DATE = LocalDate.now()

    boolean commandExecuted = false

    @Option(names = ["--preProcess"], description = """Group source files by date and titleCode.
Output is used by readyForIngestion.
Requires sourceFolder, targetPreProcessingFolder, forReviewFolder.
Uses startingDate, endingDate.
Optional createDestination, moveFiles.
This is a processing operation and must run exclusively of other processing operations.""")
    boolean preProcess = false

    @Option(names = ["--readyForIngestion"], description = """Process the source files.
Output is ready for ingestion by Rosetta.
Requires sourceFolder, targetForIngestionFolder, forReviewFolder.
Uses startingDate, endingDate.
Optional createDestination, moveFiles.
This is a processing operation and must run exclusively of other processing operations.""")
    boolean readyForIngestion = false

    @Option(names = ["-l", "--listFiles" ], description = """List the source files in an organized way.
Requires sourceFolder.
This is a reporting operation and cannot be run with any processing operations.""")
    boolean listFiles = false

    @Option(names = ["--statisticalAudit" ], description = """Statistical audit.
Search through the source folder and provide a statistical audit of the files found.
This is a reporting operation and cannot be run with any processing operations.""")
    boolean statisticalAudit

    @Option(names = ["--extractMetadata"], description = """Extract and list the metadata from the source files.
Requires sourceFolder.
This is a reporting operation and cannot be run with any processing operations.""")
    boolean extractMetadata = false

    @Option(names = ["--copyIngestedLoadsToIngestedFolder" ], description = """Copy the ingested loads to ingested folder.
Requires sourceFolder, targetPostProcessedFolder, forReviewFolder.
Uses startingDate, endingDate.
Optional createDestination, moveFiles, moveOrCopyEvenIfNoRosettaDoneFile.
This is a processing operation and must run exclusively of other processing operations.""")
    boolean copyIngestedLoadsToIngestedFolder = false

    @Option(names = ["--copyProdLoadToTestStructures" ], description = """Copy the production load to test structures.
Requires sourceFolder, targetFolder.
Uses startingDate, endingDate.
This is a processing operation and must run exclusively of other processing operations.""")
    boolean copyProdLoadToTestStructures = false

    @Option(names = ["--moveFiles" ], description = """Whether files will be moved or copied.
Default is copy (false).""")
    boolean moveFiles = false

    @Option(names = ["-c", "--createDestination" ], description = """Whether destination (or target) folders will be created.
Default is no creation (false).""")
    boolean createDestination = false

    @Option(names = ["--parallelizeProcessing" ], description = """Run operations in parallel (if possible).
Operations that have components that can run in parallel currently are:
    --preProcess""")
    boolean parallelizeProcessing = false

    @Option(names = ["--numberOfThreads"], description = """Number of threads when running operations in parallel.
The default is 1.""")
    int numberOfThreads = 1

    @Option(names = ["--moveOrCopyEvenIfNoRosettaDoneFile" ],
            description = """Whether the move or copy takes place even if there is no Rosetta done file.
The Rosetta done files is a file with a titleCode of 'done'.
Default is no move or copy unless there IS a Rosetta done file (false).""")
    boolean moveOrCopyEvenIfNoRosettaDoneFile = false

    @Option(names = ["--detailedTimings"], description = """Include detailed timings (for specific operations).""")
    boolean includeDetailedTimings = false

    @Option(names = ["--verbose"], description = """Include verbose output""")
    boolean verbose = false

    @Option(names = ["-h", "--help" ], usageHelp = true, description = 'Display a help message.')
    boolean helpRequested = false

    @Option(names = ["-b", "--startingDate"], paramLabel = "STARTING_DATE",
            description = """Starting date in the format yyyy-MM-dd.
Default is 2015-01-01.""")
    // TODO Need a custom converter
    LocalDate startingDate = DEFAULT_STARTING_DATE

    @Option(names = ["-e", "--endingDate"], paramLabel = "ENDING_DATE",
            description = """Ending date in the format yyyy-MM-dd.
Default is today.""")
    LocalDate endingDate = DEFAULT_ENDING_DATE

    @Option(names = ["-s", "--sourceFolder"], paramLabel = "SOURCE_FOLDER",
            description = 'source folder in the format /path/to/folder')
    File sourceFolder

    @Option(names = ["--targetFolder"], paramLabel = "TARGET_FOLDER",
            description = """target folder in the format /path/to/folder.
This is the destination folder used when no other destination folders are specified.""")
    File targetFolder

    @Option(names = ["--targetPreProcessingFolder"], paramLabel = "TARGET_PRE_PROCESS_FOLDER",
            description = """target pre-processing folder in the format /path/to/folder""")
    File targetPreProcessingFolder

    @Option(names = ["--targetForIngestionFolder"], paramLabel = "TARGET_FOR_INGESTION_FOLDER",
            description = """target for-ingestion folder in the format /path/to/folder""")
    File targetForIngestionFolder

    @Option(names = ["--targetPostProcessedFolder"], paramLabel = "TARGET_POST_PROCESSED_FOLDER",
            description = """target post-processed folder in the format /path/to/folder""")
    File targetPostProcessedFolder

    @Option(names = ["-r", "--forReviewFolder"], paramLabel = "FOR_REVIEW_FOLDER", description = 'for-review folder in the format /path/to/folder')
    File forReviewFolder

    static void main(String[] args) {
        ProcessorRunner processorRunner = new ProcessorRunner()
        CommandLine.call(processorRunner, args)
        if (!processorRunner.commandExecuted) {
            String[] helpArgs = [ '-h' ]
            CommandLine.call(processorRunner, helpArgs)
        }
    }

    @Override
    Void call() throws Exception {
        timekeeper = new Timekeeper()

        showParameters()

        process()
        return null
    }

    void showParameters() {
        log.info("")
        log.info("Parameters as set:")
        log.info("    Processing stages:")
        log.info("        preProcess=${preProcess}")
        log.info("        readyForIngestion=${readyForIngestion}")
        log.info("        copyIngestedLoadsToIngestedFolder=${copyIngestedLoadsToIngestedFolder}")
        log.info("    Other types of processing:")
        log.info("        copyProdLoadToTestStructures=${copyProdLoadToTestStructures}")
        log.info("    Reporting:")
        log.info("        listFiles=${listFiles}")
        log.info("        statisticalAudit=${statisticalAudit}")
        log.info("        extractMetadata=${extractMetadata}")
        log.info("    Source and target folders:")
        log.info("        sourceFolder=${sourceFolder}")
        log.info("        targetFolder=${targetFolder}")
        log.info("        targetPreProcessingFolder=${targetPreProcessingFolder}")
        log.info("        targetForIngestionFolder=${targetForIngestionFolder}")
        log.info("        targetPostProcessedFolder=${targetPostProcessedFolder}")
        log.info("        forReviewFolder=${forReviewFolder}")
        log.info("    Date scoping:")
        log.info("        startingDate=${startingDate}")
        log.info("        endingDate=${endingDate}")
        log.info("    Options:")
        log.info("        moveFiles=${moveFiles}")
        log.info("        createDestination=${createDestination}")
        log.info("        parallelizeProcessing=${parallelizeProcessing}")
        log.info("        numberOfThreads=${numberOfThreads}")
        log.info("        moveOrCopyEvenIfNoRosettaDoneFile=${moveOrCopyEvenIfNoRosettaDoneFile}")
        log.info("        includeDetailedTimings=${includeDetailedTimings}")
        log.info("        verbose=${verbose}")
        log.info("")
    }

    void displayProcessingLegend() {
        log.info("")
        log.info("Processing legend:")
        log.info("    .  -- indicates a file has been processed (either moved or copied)")
        log.info("    !  -- indicates a move or copy operation was not successful")
        log.info("    :  -- indicates a folder has been processed (either moved or copied)")
        log.info("    +  -- indicates a duplicate pre-process file has been detected and is exactly the same as")
        log.info("          the target file. If --moveFiles has been specified the source file is deleted.")
        log.info("    #  -- indicates a duplicate folder has been detected and will be copied or moved with the name of the")
        log.info("          folder with a '-<number>' appended to it.")
        log.info("    *  -- indicates that a pre-process file already exists (and is the same) in the post-processing")
        log.info("          target directory. In this case, the file is either not processed (if a copy) or deleted in the")
        log.info("          source folder (if --moveFiles).")
        log.info("    ?  -- indicates that a pre-process file already exists (and is NOT the same) in the post-processing")
        log.info("          target directory. In this case, the file is either copied or moved to the for_review_folder")
        log.info("    -  -- indicates that a source file has been deleted. This can happen when:")
        log.info("              - When pre-processing and the file already exists and --moveFiles is specified.")
        log.info("    =  -- indicates that a source folder has been deleted. This can happen when:")
        log.info("              - When post-processing and --moveFiles, the parent folder of the 'done' file deleted.")
        log.info("")
    }

    void process() {
        int totalProcessingOperations = (copyProdLoadToTestStructures ? 1 : 0) + (preProcess ? 1 : 0) +
                (readyForIngestion ? 1 : 0) + (copyIngestedLoadsToIngestedFolder ? 1 : 0)
        if (totalProcessingOperations > 1) {
            String message = "Only 1 processing operation (copyProdLoadToTestStructures, preProcess, " +
                    "readyForIngestion or copyIngestedLoadsToIngestedFolder) can run at a time. " +
                    "Your command requests total processing operations=${totalProcessingOperations}. Please change your command."
            log.error(message)
            throw new ProcessorException(message)
        }
        int totalReportingOperations = (listFiles ? 1 : 0) + (statisticalAudit ? 1 : 0) + (extractMetadata ? 1 : 0)
        if (totalReportingOperations > 0 && totalProcessingOperations > 0) {
            String message = "Reporting operations (listFiles, statisticalAudit, extractMetadata) cannot be run with any processing operations."
            log.error(message)
            throw new ProcessorException(message)
        }
        if (sourceFolder != null && (!sourceFolder.exists() || !sourceFolder.isDirectory())) {
            String message = "sourceFolder=${sourceFolder.getCanonicalPath()} must exist=${sourceFolder.exists()} and must be directory=${sourceFolder.isDirectory()}"
            log.error(message)
            throw new ProcessorException(message)
        }
        timekeeper.start()
        // Do the non-destructive options first
        if (listFiles) {
            if (sourceFolder == null) {
                String message = "listFiles requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            ReportsProcessor reportsProcessor = new ReportsProcessor(this)
            reportsProcessor.listFiles()
            commandExecuted = true
        }
        if (statisticalAudit) {
            if (sourceFolder == null) {
                String message = "statisticalAudit requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            ReportsProcessor reportsProcessor = new ReportsProcessor(this)
            reportsProcessor.statisticalAudit()
            commandExecuted = true
        }
        if (extractMetadata) {
            if (sourceFolder == null) {
                String message = "extractMetadata requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            ReportsProcessor reportsProcessor = new ReportsProcessor(this)
            reportsProcessor.extractMetadata()
            commandExecuted = true
        }
        if (copyProdLoadToTestStructures) {
            if (sourceFolder == null) {
                String message = "copyProdLoadToTestStructures requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (targetFolder == null) {
                String message = "copyProdLoadToTestStructures requires targetFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            displayProcessingLegend()
            MiscellaneousProcessor miscellaneousProcessor = new MiscellaneousProcessor(this)
            miscellaneousProcessor.copyProdLoadToTestStructures()
            commandExecuted = true
        }
        if (preProcess) {
            if (sourceFolder == null) {
                String message = "preProcess requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (targetPreProcessingFolder == null) {
                String message = "preProcess requires targetPreProcessingFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (forReviewFolder == null) {
                String message = "preProcess requires forReviewFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            displayProcessingLegend()
            PreProcessProcessor preProcessProcessor = new PreProcessProcessor(this)
            preProcessProcessor.process()
            commandExecuted = true
        }
        if (readyForIngestion) {
            if (sourceFolder == null) {
                String message = "readyForIngestion requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (targetForIngestionFolder == null) {
                String message = "readyForIngestion requires targetForIngestionFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (forReviewFolder == null) {
                String message = "preProcess requires forReviewFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            displayProcessingLegend()
            ReadyForIngestionProcessor readyForIngestionProcessor = new ReadyForIngestionProcessor(this)
            readyForIngestionProcessor.process()
            commandExecuted = true
        }
        if (copyIngestedLoadsToIngestedFolder) {
            if (sourceFolder == null) {
                String message = "copyIngestedLoadsToIngestedFolder requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (targetPostProcessedFolder == null) {
                String message = "copyIngestedLoadsToIngestedFolder requires targetPostProcessedFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (forReviewFolder == null) {
                String message = "copyIngestedLoadsToIngestedFolder requires forReviewFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            displayProcessingLegend()
            MiscellaneousProcessor miscellaneousProcessor = new MiscellaneousProcessor(this)
            miscellaneousProcessor.copyIngestedLoadsToIngestedFolder()
            commandExecuted = true
        }
    }
}
