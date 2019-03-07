package nz.govt.natlib.tools.sip.generation.fairfax.processor

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper

import java.time.LocalDate
import java.util.concurrent.Callable

@Slf4j
@Command(description = 'Runs different processors based on command-line options', name = 'processorRunner')
class ProcessorRunner implements Callable<Void>{
    final static LocalDate DEFAULT_STARTING_DATE = LocalDate.of(2015, 1, 1)
    final static LocalDate DEFAULT_ENDING_DATE = LocalDate.now()

    @Option(names = ["-g", "--groupByDateAndName"], description = """Group source files by date and name.
Output is used by processByDate.
Requires sourceFolder, destinationFolder.
Uses startingDate, endingDate""")
    boolean groupByDateAndName = false

    @Option(names = ["-d", "--processByDate"], description = """Process the source files.
Output is ready for ingestion by Rosetta.
Requires sourceFolder, destination Folder.
Uses startingDate, endingDate""")
    boolean processByDate = false

    @Option(names = ["-l", "--listFiles" ], description = """List the source files in an organized way.
Requires sourceFolder""")
    boolean listFiles = false

    @Option(names = [ "-x", "--extractMetadata"], description = """Extract and list the metadata from the source files.
Requires sourceFolder""")
    boolean extractMetadata = false

    @Option(names = [ "-p", "--copyProdLoadToTestStructures" ], description = """Copy the production load to test structures.
Requires sourceFolder, destination Folder.
Uses startingDate, endingDate""")
    boolean copyProdLoadToTestStructures = false

    @Option(names = ["-m", "--moveFiles" ], description = """Whether files will be moved or copied.
Default is copy (false).""")
    boolean moveFiles = false

    @Option(names = ["-c", "--createDestination" ], description = """Whether destination folders will be created.
Default is no creation (false).""")
    boolean createDestination = false

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

    Timekeeper timekeeper = new Timekeeper()

    static void main(String[] args) {
        ProcessorRunner processorRunner = new ProcessorRunner()
        CommandLine.call(processorRunner, args)
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
            MiscellaneousProcessor miscellaneousProcessor = new MiscellaneousProcessor(timekeeper)
            miscellaneousProcessor.listFiles(sourceFolder)
        }
        if (extractMetadata) {
            if (sourceFolder == null) {
                String message = "extractMetadata requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            MiscellaneousProcessor miscellaneousProcessor = new MiscellaneousProcessor(timekeeper)
            miscellaneousProcessor.extractMetadata(sourceFolder)
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
        }
        if (groupByDateAndName) {
            if (sourceFolder == null) {
                String message = "groupByDateAndName requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (targetFolder == null) {
                String message = "groupByDateAndName requires targetFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            GroupByDateAndNameProcessor groupByDateAndNameProcessor = new GroupByDateAndNameProcessor(timekeeper)
            groupByDateAndNameProcessor.groupByDateAndName(sourceFolder, targetFolder, createDestination, moveFiles,
                    startingDate, endingDate)
        }
        if (processByDate) {
            if (sourceFolder == null) {
                String message = "processByDate requires sourceFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            if (targetFolder == null) {
                String message = "processByDate requires targetFolder"
                log.error(message)
                throw new ProcessorException(message)
            }
            ProcessByDateProcessor processByDateProcessor = new ProcessByDateProcessor(timekeeper)
            processByDateProcessor.processByDate(sourceFolder, targetFolder, createDestination, moveFiles,
                    startingDate, endingDate)
        }
    }

}
