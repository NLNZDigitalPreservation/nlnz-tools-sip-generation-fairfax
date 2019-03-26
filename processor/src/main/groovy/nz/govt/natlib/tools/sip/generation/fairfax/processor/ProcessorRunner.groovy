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
class ProcessorRunner implements Callable<Void>{
    final static LocalDate DEFAULT_STARTING_DATE = LocalDate.of(2015, 1, 1)
    final static LocalDate DEFAULT_ENDING_DATE = LocalDate.now()

    boolean commandExecuted = false

    @Option(names = ["--preProcess"], description = """Group source files by date and titleCode.
Output is used by readyForIngestion.
Requires sourceFolder, targetFolder, forReviewFolder.
Uses startingDate, endingDate.
Optional createDestination, moveFiles.""")
    boolean preProcess = false

    @Option(names = ["--readyForIngestion"], description = """Process the source files.
Output is ready for ingestion by Rosetta.
Requires sourceFolder, targetFolder, forReviewFolder.
Uses startingDate, endingDate.
Optional createDestination, moveFiles.""")
    boolean readyForIngestion = false

    @Option(names = ["-l", "--listFiles" ], description = """List the source files in an organized way.
Requires sourceFolder""")
    boolean listFiles = false

    @Option(names = ["--statisticalAudit" ], description = """Statistical audit.
Search through the source folder and provide a statistical audit of the files found.""")
    boolean statisticalAudit

    @Option(names = ["--extractMetadata"], description = """Extract and list the metadata from the source files.
Requires sourceFolder""")
    boolean extractMetadata = false

    @Option(names = ["--copyIngestedLoadsToIngestedFolder" ], description = """Copy the ingested loads to ingested folder.
Requires sourceFolder, targetFolder, forReviewFolder.
Uses startingDate, endingDate.
Optional createDestination, moveFiles, moveOrCopyEvenIfNoRosettaDoneFile""")
    boolean copyIngestedLoadsToIngestedFolder = false

    @Option(names = ["--copyProdLoadToTestStructures" ], description = """Copy the production load to test structures.
Requires sourceFolder, targetFolder.
Uses startingDate, endingDate""")
    boolean copyProdLoadToTestStructures = false

    @Option(names = ["--moveFiles" ], description = """Whether files will be moved or copied.
Default is copy (false).""")
    boolean moveFiles = false

    @Option(names = ["-c", "--createDestination" ], description = """Whether destination (or target) folders will be created.
Default is no creation (false).""")
    boolean createDestination = false

    @Option(names = ["--moveOrCopyEvenIfNoRosettaDoneFile" ], description = """Whether the move or copy takes place even if there is no Rosetta done file.
The Rosetta done files is a file with a titleCode of 'done'.
Default is no move or copy unless there IS a Rosetta done file (false).""")
    boolean moveOrCopyEvenIfNoRosettaDoneFile = false

    @Option(names = ["-h", "--help" ], usageHelp = true, description = 'Display a help message.')
    boolean helpRequested = false

    @Option(names = ["-b", "--startingDate"], paramLabel = "STARTING_DATE", description = """Starting date in the format yyyy-MM-dd.
Default is 2015-01-01.""")
    // TODO Need a custom converter
    LocalDate startingDate = DEFAULT_STARTING_DATE

    @Option(names = ["-e", "--endingDate"], paramLabel = "ENDING_DATE", description = """Ending date in the format yyyy-MM-dd.
Default is today.""")
    LocalDate endingDate = DEFAULT_ENDING_DATE

    @Option(names = ["-s", "--sourceFolder"], paramLabel = "SOURCE_FOLDER", description = 'source folder in the format /path/to/folder')
    File sourceFolder

    @Option(names = ["-t", "--targetFolder"], paramLabel = "TARGET_FOLDER", description = 'target folder in the format /path/to/folder')
    File targetFolder

    @Option(names = ["-r", "--forReviewFolder"], paramLabel = "FOR_REVIEW_FOLDER", description = 'for-review folder in the format /path/to/folder')
    File forReviewFolder

    Timekeeper timekeeper = new Timekeeper()

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
        process()
        return null
    }

    void process() {
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
            ReportsProcessor reportsProcessor = new ReportsProcessor(timekeeper)
            reportsProcessor.listFiles(sourceFolder)
            commandExecuted = true
        }
        if (statisticalAudit) {
            if (sourceFolder == null) {
                String message = "statisticalAudit requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            ReportsProcessor reportsProcessor = new ReportsProcessor(timekeeper)
            reportsProcessor.statisticalAudit(sourceFolder)
            commandExecuted = true
        }
        if (extractMetadata) {
            if (sourceFolder == null) {
                String message = "extractMetadata requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            ReportsProcessor reportsProcessor = new ReportsProcessor(timekeeper)
            reportsProcessor.extractMetadata(sourceFolder)
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
            MiscellaneousProcessor miscellaneousProcessor = new MiscellaneousProcessor(timekeeper)
            miscellaneousProcessor.copyProdLoadToTestStructures(sourceFolder, targetFolder, createDestination,
                                    startingDate, endingDate)
            commandExecuted = true
        }
        if (preProcess) {
            if (sourceFolder == null) {
                String message = "preProcess requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (targetFolder == null) {
                String message = "preProcess requires targetFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (forReviewFolder == null) {
                String message = "preProcess requires forReviewFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            PreProcessProcessor preProcessProcessor = new PreProcessProcessor(timekeeper)
            preProcessProcessor.process(sourceFolder, targetFolder, forReviewFolder, createDestination, moveFiles,
                    startingDate, endingDate)
            commandExecuted = true
        }
        if (readyForIngestion) {
            if (sourceFolder == null) {
                String message = "readyForIngestion requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (targetFolder == null) {
                String message = "readyForIngestion requires targetFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (forReviewFolder == null) {
                String message = "preProcess requires forReviewFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            ReadyForIngestionProcessor readyForIngestionProcessor = new ReadyForIngestionProcessor(timekeeper)
            readyForIngestionProcessor.process(sourceFolder, targetFolder, forReviewFolder, createDestination,
                    moveFiles, startingDate, endingDate)
            commandExecuted = true
        }
        if (copyIngestedLoadsToIngestedFolder) {
            if (sourceFolder == null) {
                String message = "copyIngestedLoadsToIngestedFolder requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (targetFolder == null) {
                String message = "copyIngestedLoadsToIngestedFolder requires targetFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (forReviewFolder == null) {
                String message = "copyIngestedLoadsToIngestedFolder requires forReviewFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            MiscellaneousProcessor miscellaneousProcessor = new MiscellaneousProcessor(timekeeper)
            miscellaneousProcessor.copyIngestedLoadsToIngestedFolder(sourceFolder, targetFolder, forReviewFolder,
                    createDestination, moveFiles, startingDate, endingDate, moveOrCopyEvenIfNoRosettaDoneFile)
            commandExecuted = true
        }
    }
}
