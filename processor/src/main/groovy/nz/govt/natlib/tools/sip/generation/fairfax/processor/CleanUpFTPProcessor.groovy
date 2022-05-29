package nz.govt.natlib.tools.sip.generation.fairfax.processor;

import groovy.util.logging.Log4j2
import groovyx.gpars.GParsExecutorsPool
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.processing.ProcessLogger
import nz.govt.natlib.tools.sip.utils.GeneralUtils
import nz.govt.natlib.tools.sip.utils.PathUtils

import java.nio.file.Files;
import java.nio.file.Path
import java.time.LocalDate

@Log4j2
class CleanUpFTPProcessor {
    ProcessorConfiguration processorConfiguration
    FairfaxSpreadsheet fairfaxSpreadsheet

    CleanUpFTPProcessor(ProcessorConfiguration processorConfiguration) {
        this.processorConfiguration = processorConfiguration
    }

    List<FairfaxFile> filteredFiles(List<Path> allFilesList, LocalDate startingDate, LocalDate endingDate) {
        List<FairfaxFile> filteredList = new ArrayList<>()
        allFilesList.each { Path theFile ->
            FairfaxFile fairfaxFile = new FairfaxFile(theFile)
            if (fairfaxFile.date >= startingDate && fairfaxFile.date <= endingDate) {
                filteredList.add(fairfaxFile)
            }
        }

        return filteredList
    }

    void process() {
        ProcessLogger processLogger = new ProcessLogger()
        processLogger.startSplit()

        if (processorConfiguration.startingDate != null && processorConfiguration.endingDate != null) {
            log.info("Action required. Waiting for user to review details before continuing")
            print("\n\n\n\n\n\n\n\n")
            print("##################################################################################\n")
            print("##                                                                              ##\n")
            print("##                             ACTION REQUIRED                                  ##\n")
            print("##                                                                              ##\n")
            print("##  This process will permanently delete all matching files                     ##\n")
            print("##                                                                              ##\n")
            print("##  Please carefully review the date range and source folder before continuing  ##\n")
            print("##                                                                              ##\n")
            print("##################################################################################\n")
            print("\n\n")
            print("Are you sure you wish to permanently delete these files?\n")
            print("\n\n")
            print("Starting date: ${processorConfiguration.startingDate}\n")
            print("Ending date: ${processorConfiguration.endingDate}\n")
            print("From the folder: ${processorConfiguration.sourceFolder.normalize().toString()}\n")
            print("\n\n")
            print("Type 'confirm' to confirm and begin deleting\n")
            print("Type 'exit' to cancel:\n")

            def confirm = System.in.newReader().readLine()

            LocalDate currentDate = LocalDate.now();
            LocalDate currentDateMinus2Months = currentDate.minusMonths(2);
            boolean safeDateRange = processorConfiguration.endingDate.isBefore(currentDateMinus2Months);

            if (!safeDateRange) {
                print("\n\n")
                print("##################################################################################\n")
                print("##                                                                              ##\n")
                print("## Ending date ${processorConfiguration.endingDate} is less than 2 months ago                             ##\n")
                print("## Proceeding is not advised                                                    ##\n")
                print("## Are you sure you wish to continue?                                           ##\n")
                print("##                                                                              ##\n")
                print("##################################################################################\n")
                print("\n\n")
                print("Type 'confirm' to confirm and begin deleting\n")
                print("Type 'exit' to cancel:\n")

                def confirmEndingDate = System.in.newReader().readLine()

                if (confirmEndingDate == "confirm") {
                    safeDateRange = true;
                }
            }

            if (confirm == "confirm" && safeDateRange) {
                print("\n\n")
                log.info("User has confirmed dates and source folder")
                log.info("START process for startingDate=${processorConfiguration.startingDate}, " +
                        "endingDate=${processorConfiguration.endingDate}, " +
                        "sourceFolder=${processorConfiguration.sourceFolder.normalize().toString()}, " +
                        processorConfiguration.timekeeper.logElapsed())

                this.fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

                boolean isRegexNotGlob = true
                boolean matchFilenameOnly = true
                boolean sortFiles = false

                String pattern = FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_SEQUENCE_PATTERN
                // Given that we could be dealing with 60,000+ files in the source directory, it's probably more efficient to
                // get them all at once
                List<Path> allFiles = PathUtils.findFiles(processorConfiguration.sourceFolder.normalize().toString(),
                        isRegexNotGlob, matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper)
                int allFilesFoundSize = allFiles.size()

                int numberOfThreads = processorConfiguration.parallelizeProcessing ? processorConfiguration.numberOfThreads : 1
                log.info("Spreading processing over numberOfThreads=${numberOfThreads}")

                ProcessingCounter filesProcessedCounter = new ProcessingCounter()
                ProcessingCounter filesDeleted = new ProcessingCounter()
                processorConfiguration.timekeeper.logElapsed(false, filesProcessedCounter.currentCount)

                List<FairfaxFile> filteredFiles = filteredFiles(allFiles, processorConfiguration.startingDate,
                        processorConfiguration.endingDate)
                // Clear allFiles (especially if large) -- we want this collection garbage collected out
                allFiles = null
                log.info("Deleting ${GeneralUtils.TOTAL_FORMAT.format(filteredFiles.size())} files")
                GParsExecutorsPool.withPool(numberOfThreads) {
                    filteredFiles.eachParallel { FairfaxFile fairfaxFile ->
                        try {
                            if (Files.exists(fairfaxFile.file)) {
                                Files.delete(fairfaxFile.file)
                                GeneralUtils.printAndFlush(".")
                                filesDeleted.incrementCounter()
                            }
                            filesProcessedCounter.incrementCounter()
                            if (filesProcessedCounter.currentCount % 5000 == 0) {
                                GeneralUtils.printAndFlush("\n")
                                processorConfiguration.timekeeper.logElapsed(false, filesProcessedCounter.currentCount,
                                        true)
                            }
                        } catch (Exception e) {
                            log.error("Exception deleting newspaperFile=${fairfaxFile}", e)
                        }
                    }
                }

                processorConfiguration.timekeeper.logElapsed(false, filesProcessedCounter.total, true)

                log.info("END processing for parameters:")
                log.info("    startindDate=${processorConfiguration.startingDate}")
                log.info("    endingDate=${processorConfiguration.endingDate}")
                log.info("    sourceFolder=${processorConfiguration.sourceFolder.normalize().toString()}")
                processorConfiguration.timekeeper.logElapsed()
                log.info("Files totals:")
                log.info("    found=${GeneralUtils.TOTAL_FORMAT.format(allFilesFoundSize)}")
                log.info("    reviewed=${GeneralUtils.TOTAL_FORMAT.format(filesProcessedCounter.total)}")
                log.info("    files deleted=${GeneralUtils.TOTAL_FORMAT.format(filesDeleted.total)}")
                int notDeleted = filesProcessedCounter.total - filesDeleted.total
                log.info("    NOT delted=${GeneralUtils.TOTAL_FORMAT.format(notDeleted)}")

            } else {
                log.info("Process cancelled by user")
            }
        } else {
            log.info("startingDate=${processorConfiguration.startingDate} and " +
                    "endingDate=${processorConfiguration.endingDate} have not been both specified")
        }
    }
}
