package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import groovyx.gpars.GParsPool
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.processing.ProcessLogger

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Slf4j
class PreProcessProcessor {
    static final DateTimeFormatter LOCAL_DATE_FOLDER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

    ProcessorConfiguration processorConfiguration
    FairfaxSpreadsheet fairfaxSpreadsheet
    Set<String> recognizedTitleCodes = new ConcurrentHashMap<>().newKeySet()
    Set<String> unrecognizedTitleCodes = new ConcurrentHashMap<>().newKeySet()
    Set<File> inProcessDestinationFiles = new ConcurrentHashMap().newKeySet()

    // Locks
    ReentrantLock folderCreationLock = new ReentrantLock()
    ReentrantLock inProcessDestinationFilesLock = new ReentrantLock()

    PreProcessProcessor(ProcessorConfiguration processorConfiguration) {
        this.processorConfiguration = processorConfiguration
    }

    void makeDirs(File directory) {
        folderCreationLock.lock()
        try {
            directory.mkdirs()
        } finally {
            folderCreationLock.unlock()
        }
    }

    void waitForNoInProcessDestinationFile(File file) {
        int waitCount = 0
        while (inProcessDestinationFiles.contains(file)) {
            print("")
            log.warn("inProcessDestinationFiles (size=${inProcessDestinationFiles.size()}) (waitCount=${waitCount}) " +
                    "already contains file=${file.getCanonicalPath()}, waiting for it to clear.")
            sleep(2000)
            waitCount += 1
        }
    }

    void addInProcessDestinationFile(File file) {
        waitForNoInProcessDestinationFile(file)
        inProcessDestinationFilesLock.lock()
        try {
            if (inProcessDestinationFiles.contains(file)) {
                print("")
                log.warn("inProcessDestinationFiles already contains file=${file.getCanonicalPath()} (multiple threads checking the same file)")
            } else {
                inProcessDestinationFiles.add(file)
            }
        } finally {
            inProcessDestinationFilesLock.unlock()
        }
    }

    void removeInProcessDestinationFile(File file) {
        inProcessDestinationFilesLock.lock()
        try {
            if (inProcessDestinationFiles.contains(file)) {
                inProcessDestinationFiles.remove(file)
            } else {
                print("")
                log.warn("inProcessDestinationFiles DOES NOT contain file=${file.getCanonicalPath()} (multiple threads removing the same file)")
            }
        } finally {
            inProcessDestinationFilesLock.unlock()
        }
    }

    boolean copyOrMoveFileToPreProcessingDestination(File destinationFolder, File forReviewFolder, FairfaxFile targetFile,
                                                  String dateFolderName, boolean moveFile) {
        String titleCodeFolderName = targetFile.titleCode
        String folderPath
        Set<String> allNameKeys = fairfaxSpreadsheet.allTitleCodeKeys

        if (allNameKeys.contains(targetFile.titleCode)) {
            // There's an entry in the spreadsheet for this titleCode
            // Goes to '<date>/<titleCode>/<file>'
            if (!recognizedTitleCodes.contains(targetFile.titleCode)) {
                recognizedTitleCodes.add(targetFile.titleCode)
                print("")
                log.info("copyOrMoveFileToPreProcessingDestination adding titleCode=${targetFile.titleCode}")
            }
            folderPath = "${destinationFolder.getCanonicalPath()}${File.separator}${dateFolderName}${File.separator}${titleCodeFolderName}"
        } else {
            // There is no entry in the spreadsheet for this titleCode
            // Goes to 'UNKNOWN-TITLE-CODE/<date>/<file>'
            if (!unrecognizedTitleCodes.contains(targetFile.titleCode)) {
                unrecognizedTitleCodes.add(targetFile.titleCode)
                print("")
                log.info("copyOrMoveFileToPreProcessingDestination adding unrecognizedName=${targetFile.titleCode}")
            }
            folderPath = "${forReviewFolder.getCanonicalPath()}${File.separator}UNKNOWN-TITLE-CODE${File.separator}${dateFolderName}"
        }
        File destination = new File(folderPath)
        makeDirs(destination)

        File destinationFile = new File(destination, targetFile.file.getName())
        addInProcessDestinationFile(destinationFile)
        boolean moveToDestination = true
        if (destinationFile.exists()) {
            if (ProcessorUtils.isSameFile(targetFile.file, destinationFile)) {
                moveToDestination = false
                if (processorConfiguration.verbose) {
                    log.info("Skipping moveFile=${moveFile} destinationFile=${destinationFile.getCanonicalPath()} -- it already exists and is same file")
                } else {
                    ProcessorUtils.printAndFlush("+")
                }
                if (moveFile) {
                    Files.delete(targetFile.file.toPath())
                    if (processorConfiguration.verbose) {
                        log.info("Deleting moveFile=${moveFile} destinationFile=${destinationFile.getCanonicalPath()}")
                    } else {
                        ProcessorUtils.printAndFlush("-")
                    }
                }
            } else {
                boolean couldAlreadyExist = true
                while (couldAlreadyExist) {
                    File nonDuplicateFile = ProcessorUtils.nonDuplicateFile(destinationFile)
                    print("")
                    log.info("moveFile=${moveFile} destinationFile=${destinationFile.getCanonicalPath()} -- same name but different file")
                    log.info("             moving to destinationFile=${nonDuplicateFile.getCanonicalPath()}")
                    File oldDestinationFile = destinationFile
                    destinationFile = nonDuplicateFile
                    addInProcessDestinationFile(nonDuplicateFile)
                    removeInProcessDestinationFile(oldDestinationFile)
                    if (destinationFile.exists()) {
                        // another thread has already created this duplicate file
                        couldAlreadyExist = true
                    } else {
                        couldAlreadyExist = false
                    }
                }
            }
        }
        if (moveToDestination) {
            if (moveFile) {
                // The only valid move option is StandardCopyOption.REPLACE_EXISTING, which we don't want to do
                Files.move(targetFile.file.toPath(), destinationFile.toPath())
            } else {
                Files.copy(targetFile.file.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            }
            ProcessorUtils.printAndFlush(".")
        }
        removeInProcessDestinationFile(destinationFile)

        return moveToDestination
    }

    List<LocalDate> processingDates(LocalDate startingDate, LocalDate endingDate) {
        List<LocalDate> datesList = new ArrayList<>()
        LocalDate currentDate = startingDate
        while (currentDate <= endingDate) {
            datesList += currentDate
            currentDate = currentDate.plusDays(1L)
        }
        return datesList
    }

    List<FairfaxFile> filteredFiles(List<File> allFilesList, LocalDate startingDate, LocalDate endingDate,
                                    boolean sortByDate) {
        List<FairfaxFile> filteredList = new ArrayList<>()
        allFilesList.each { File theFile ->
            FairfaxFile fairfaxFile = new FairfaxFile(theFile)
            if (fairfaxFile.date >= startingDate && fairfaxFile.date <= endingDate) {
                filteredList.add(fairfaxFile)
            }
        }

        if (sortByDate) {
            filteredList.sort() { FairfaxFile file1, FairfaxFile file2 -> file1.date <=> file2.date }
        }
        return filteredList
    }

    // See the README.md for folder descriptions and structures.
    void process() {
        // Clear the set of recognized and unrecognized names before processing begins
        recognizedTitleCodes = new ConcurrentHashMap<>().newKeySet()
        unrecognizedTitleCodes = new ConcurrentHashMap<>().newKeySet()

        ProcessLogger processLogger = new ProcessLogger()
        processLogger.startSplit()

        log.info("START process for startindDate=${processorConfiguration.startingDate}, " +
                "endingDate=${processorConfiguration.endingDate}, " +
                "sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}, " +
                "forReviewFolder=${processorConfiguration.forReviewFolder.getCanonicalPath()}")
        processorConfiguration.timekeeper.logElapsed()

        if (processorConfiguration.createDestination) {
            processorConfiguration.targetPreProcessingFolder.mkdirs()
            processorConfiguration.forReviewFolder.mkdirs()
        }
        this.fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true

        String pattern = '\\w{5,7}-\\d{8}-\\w{3,4}.*?\\.[pP]{1}[dD]{1}[fF]{1}'
        // Given that we could be dealing with 60,000+ files in the source directory, it's probably more efficient to
        // get them all at once
        List<File> allFiles = ProcessorUtils.findFiles(processorConfiguration.sourceFolder.getAbsolutePath(),
                isRegexNotGlob, matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper)
        int allFilesFoundSize = allFiles.size()

        int numberOfThreads = processorConfiguration.parallelizeProcessing ? processorConfiguration.numberOfThreads : 1
        log.info("Spreading processing over numberOfThreads=${numberOfThreads}")

        ProcessingCounter filesProcessedCounter = new ProcessingCounter()
        ProcessingCounter filesMovedOrCopiedCounter = new ProcessingCounter()
        processorConfiguration.timekeeper.logElapsed(false, filesProcessedCounter.currentCount)
        if (processorConfiguration.startingDate != null && processorConfiguration.endingDate != null) {
            List<FairfaxFile> filteredFiles = filteredFiles(allFiles, processorConfiguration.startingDate,
                    processorConfiguration.endingDate, true)
            // Clear allFiles (especially if large) -- we want this collection garbage collected out
            allFiles = null
            log.info("Moving=${processorConfiguration.moveFiles} " +
                    "total files=${ProcessorUtils.TOTAL_FORMAT.format(filteredFiles.size())} " +
                    "to destination=${processorConfiguration.targetPreProcessingFolder.getCanonicalPath()}")
            GParsPool.withPool(numberOfThreads) {
                filteredFiles.eachParallel { FairfaxFile fairfaxFile ->
                    String dateString = LOCAL_DATE_FOLDER_FORMATTER.format(fairfaxFile.date)
                    boolean moved = copyOrMoveFileToPreProcessingDestination(
                            processorConfiguration.targetPreProcessingFolder,
                            processorConfiguration.forReviewFolder, fairfaxFile, dateString,
                            processorConfiguration.moveFiles)
                    if (moved) {
                        filesMovedOrCopiedCounter.incrementCounter()
                    }
                    filesProcessedCounter.incrementCounter()
                    if (filesProcessedCounter.currentCount % 5000 == 0) {
                        print("")
                        processorConfiguration.timekeeper.logElapsed(false, filesProcessedCounter.currentCount,
                                true)
                    }
                }
            }
        } else {
            log.info("startingDate=${processorConfiguration.startingDate} and " +
                    "endingDate=${processorConfiguration.endingDate} have not been both specified")

            List<File> foundFiles = ProcessorUtils.findNonMatchingFiles(
                    processorConfiguration.sourceFolder.getAbsolutePath(), isRegexNotGlob, matchFilenameOnly,
                    sortFiles, pattern, processorConfiguration.timekeeper)
            foundFiles.each { File foundFile ->
                String dateString = "UNKNOWN-DATE"
                copyOrMoveFileToPreProcessingDestination(processorConfiguration.targetPreProcessingFolder,
                        processorConfiguration.forReviewFolder, new FairfaxFile(foundFile), dateString,
                        processorConfiguration.moveFiles)
            }
        }
        processorConfiguration.timekeeper.logElapsed(false, filesProcessedCounter.total, true)

        log.info("END processing for parameters:")
        log.info("    startindDate=${processorConfiguration.startingDate}")
        log.info("    endingDate=${processorConfiguration.endingDate}")
        log.info("    sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}")
        log.info("    targetPreProcessingFolder=${processorConfiguration.targetPreProcessingFolder.getCanonicalPath()}")
        log.info("    forReviewFolder=${processorConfiguration.forReviewFolder.getCanonicalPath()}")
        processorConfiguration.timekeeper.logElapsed()
        log.info("Files totals:")
        log.info("    found=${ProcessorUtils.TOTAL_FORMAT.format(allFilesFoundSize)}")
        log.info("    reviewed=${ProcessorUtils.TOTAL_FORMAT.format(filesProcessedCounter.total)}")
        log.info("    files moved or copied=${ProcessorUtils.TOTAL_FORMAT.format(filesMovedOrCopiedCounter.total)}")
        int notMovedOrCopied = filesProcessedCounter.total - filesMovedOrCopiedCounter.total
        log.info("    NOT moved or copied=${ProcessorUtils.TOTAL_FORMAT.format(notMovedOrCopied)} (probably duplicates)")

        processLogger.copySplit(processorConfiguration.targetPreProcessingFolder, "Pre-Process-Processor", true)
    }
}
