package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Log4j2
import groovyx.gpars.GParsExecutorsPool
import nz.govt.natlib.tools.sip.utils.FilesFinder
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.logging.ThreadedTimekeeper
import nz.govt.natlib.tools.sip.logging.Timekeeper
import nz.govt.natlib.tools.sip.pdf.thumbnail.ThreadedThumbnailGenerator
import nz.govt.natlib.tools.sip.pdf.thumbnail.ThumbnailParameters
import nz.govt.natlib.tools.sip.utils.GeneralUtils
import nz.govt.natlib.tools.sip.utils.PathUtils
import org.apache.commons.io.FilenameUtils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.regex.Matcher

@Log4j2
class MiscellaneousProcessor {
    ProcessorConfiguration processorConfiguration

    MiscellaneousProcessor(ProcessorConfiguration processorConfiguration) {
        this.processorConfiguration = processorConfiguration
    }

    List<Path> findProdLoadDirectoriesBetweenDates(String localPath, LocalDate startingDate, LocalDate endingDate) {
        List<Path> directoriesList = []
        Path filesPath = Paths.get(localPath)
        if (!Files.exists(filesPath) || !Files.isDirectory(filesPath)) {
            log.warn("Path '${filesPath}' does not exist is not a directory. Returning empty file list.")
            return directoriesList
        }

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        boolean includeSubdirectories = true
        boolean directoryOnly = true

        // See the README.md for this project for a description of the load directories file structure.
        // Generally it's: TODO
        // TODO: Incorrect: Load directories have the structure <titleCode>_<yyyyMMdd> (and possibly <titleCode><sectionCode>_<yyyyMMdd>
        String pattern = '\\w{3,6}_\\d{8}'
        log.info("Finding directories for path=${filesPath.normalize()} and pattern=${pattern}")
        processorConfiguration.timekeeper.logElapsed()
        directoriesList = FilesFinder.getMatchingFilesFull(filesPath, isRegexNotGlob, matchFilenameOnly, sortFiles,
                includeSubdirectories, directoryOnly, pattern)
        log.info("Found total directories=${directoriesList.size()} for path=${filesPath.normalize()}")
        processorConfiguration.timekeeper.logElapsed()

        List<Path> filteredDirectoriesList = []
        String regexPattern = '(?<titleCode>\\w{3,7})_(?<date>\\d{8})'
        directoriesList.each { Path directory ->
            Matcher matcher = directory.fileName.toString() =~ /${regexPattern}/
            if (matcher.matches()) {
                String dateString = matcher.group('date')
                LocalDate directoryDate = GeneralUtils.parseDate(dateString)
                if ((directoryDate.isEqual(startingDate) || directoryDate.isAfter(startingDate)) &&
                        (directoryDate.isBefore(endingDate) || directoryDate.isEqual(endingDate))) {
                    filteredDirectoriesList.add(directory)
                }
            }
        }
        return filteredDirectoriesList
    }

    // Split
    // See the README.md (Ingested stage) for a description of the file structures.
    void copyAndSplitBetweenNonIngestedAndIngested() {
        // Look for folders called 'content'. Does it have a 'mets.xml'?
        // Does the parent have a 'done' file.
        // If it has a 'done' file it gets moved to the ingested folder.
        // If it doesn't have a 'done' file it gets moved to the pre-process folder (or ready-for-ingestion??).
        // Load the mets.xml to get the publication titleCode and date.
        // If moving and parent of content folder has no other subfolders after moving, then delete it, and so on
        log.info("copyAndSplitBetweenNonIngestedAndIngested: Currently this work is being done by the python script:")
        log.info("    fairfax-pre-and-post-process-grouper.py")
        log.info("    See the github repository: https://github.com/NLNZDigitalPreservation/nlnz-tools-scripts-ingestion")
    }

    // Copies the prod load structure to two structures:
    // 1. preProcess structure. This is to mimic the input to preProcess.
    // 2. readyForIngestion structure. This is the structure that gets ingested into Rosetta.
    //
    // See the README.md for a description of the folder structures
    //
    // These structures provide for testing the Fairfax processor, to see if its outputs match the work done previously.
    void copyProdLoadToTestStructures() {
        // The source files are going to be in a subdirectory with the directory structure being:
        // <titleCode>_yyyyMMdd/content/streams/{files} with the mets.xml in the content directory.
        // Find the source directories that are between the starting date and the ending date
        List<Path> filteredDirectoriesList = findProdLoadDirectoriesBetweenDates(
                processorConfiguration.sourceFolder.normalize().toString(),
                processorConfiguration.startingDate, processorConfiguration.endingDate)

        // We need to copy the files to the preProcess structure AND the readyForIngestion structure.
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        String pattern = FairfaxFile.PDF_FILE_WITH_TITLE_SECTION_DATE_PATTERN
        String directoryPattern = '(?<titleCode>\\w{3,7})_(?<date>\\d{8})'

        log.info("Processing filteredDirectories total=${filteredDirectoriesList.size()}")
        int filteredDirectoriesCount = 1
        filteredDirectoriesList.each { Path sourceDirectory ->
            log.info("Processing ${filteredDirectoriesCount}/${filteredDirectoriesList.size()}, " +
                    "current=${processorConfiguration.sourceFolder.normalize().toString()}")
            Matcher matcher = sourceDirectory.fileName.toString() =~ /${directoryPattern}/
            String dateString
            String titleCodeString
            if (matcher.matches()) {
                dateString = matcher.group('date')
                titleCodeString = matcher.group('titleCode')
            } else {
                dateString = "UNKNOWN-DATE"
                titleCodeString = "UNKNOWN-TITLE-CODE"
            }
            List<Path> sourceFiles = []
            Path contentFolder = sourceDirectory.resolve("content")
            if (Files.exists(contentFolder)) {
                Path metsFile = contentFolder.resolve("mets.xml")
                if (Files.exists(metsFile)) {
                    sourceFiles.add(metsFile)
                } else {
                    log.info("metsFile=${metsFile.normalize()} does not exist -- SKIPPING")
                }
                Path streamsFolder = contentFolder.resolve("streams")
                if (Files.exists(streamsFolder)) {
                    List<Path> pdfFiles = PathUtils.findFiles(streamsFolder.normalize().toString(), isRegexNotGlob,
                            matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper)
                    sourceFiles.addAll(pdfFiles)
                } else {
                    log.info("streamsFolder=${streamsFolder.normalize()} does not exist -- SKIPPING")
                }
            } else {
                log.info("contentFolder=${contentFolder.normalize()} does not exist -- SKIPPING")
            }

            // Copy to the preProcess structure
            Path groupByDateAndNameDestinationFolder = processorConfiguration.targetFolder.resolve(
                    FilenameUtils.separatorsToSystem("groupByDateAndName/${dateString}/${titleCodeString}"))
            if (processorConfiguration.createDestination) {
                Files.createDirectories(groupByDateAndNameDestinationFolder)
            }
            sourceFiles.each { Path sourceFile ->
                Path destinationFile = groupByDateAndNameDestinationFolder.resolve(sourceFile.fileName.toString())
                if (!Files.exists(destinationFile)) {
                    Files.copy(sourceFile, destinationFile, StandardCopyOption.COPY_ATTRIBUTES)
                }
            }

            /// Copy to the readyForIngestion structure
            Path rosettaIngestFolder = processorConfiguration.targetFolder.resolve(
                    FilenameUtils.separatorsToSystem("rosettaIngest/${dateString}/${titleCodeString}_${dateString}"))
            Files.createDirectories(rosettaIngestFolder)
            sourceFiles.each { Path sourceFile ->
                Path destinationFile = rosettaIngestFolder.resolve(sourceFile.fileName.toString())
                if (!Files.exists(destinationFile)) {
                    Files.copy(sourceFile, destinationFile, StandardCopyOption.COPY_ATTRIBUTES)
                }
            }
            filteredDirectoriesCount += 1
        }
    }

    List<Path> findPdfFiles(Path sourceFolder, boolean includeSubdirectories = false) {
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        // Any pdf will do
        String pattern = '.*?\\.[pP]{1}[dD]{1}[fF]{1}'
        List<Path> pdfFiles = PathUtils.findFiles(sourceFolder.normalize().toString(),
                isRegexNotGlob, matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper,
                includeSubdirectories)

        return pdfFiles
    }

    void generateThumbnailPageFromPdfs(Path sourceFolder,
                                       ProcessorOption showDirectoryOption = ProcessorOption.ShowDirectoryAndTwoParents) {
        // TODO This could be a processor option (but make it clear it's different from 'SearchSubdirectories')
        boolean includeSubdirectories = false
        List<Path> pdfFiles = findPdfFiles(sourceFolder, includeSubdirectories)
        if (pdfFiles.isEmpty()) {
            log.info("No PDF files found in folder=${sourceFolder.normalize()}")
        } else {
            String convertedFilepath = ProcessorUtils.filePathAsSafeString(sourceFolder, [ showDirectoryOption ])
            Path thumbnailPageFile
            if (processorConfiguration.processorOptions.contains(ProcessorOption.UseSourceSubdirectoryAsTarget)) {
                thumbnailPageFile = sourceFolder.resolve("${convertedFilepath}_thumbnail_page.jpeg")
            } else {
                thumbnailPageFile = processorConfiguration.targetFolder.resolve("${convertedFilepath}_thumbnail_page.jpeg")
            }

            String thumbnailPageTitle = "PDF files in ${sourceFolder.normalize().toString()}"
            ThumbnailParameters thumbnailParameters = new ThumbnailParameters(thumbnailHeight: 240,
                    useAffineTransformation: false, textJustification: ThumbnailParameters.TextJustification.RIGHT,
                    maximumPageWidth: 1200, pageTitleText: thumbnailPageTitle,
                    pageTitleFontJustification: ThumbnailParameters.TextJustification.RIGHT)

            log.info("START Generating thumbnail page from pdfs in sourceFolder=${sourceFolder.normalize()}, thumbnailPage=${thumbnailPageFile.normalize()}")
            Timekeeper singlePageTimekeeper = ThreadedTimekeeper.forCurrentThread()
            singlePageTimekeeper.start()
            ThreadedThumbnailGenerator.writeThumbnailPage(pdfFiles, thumbnailParameters, thumbnailPageFile)
            log.info("END Generated thumbnail page from pdfs in sourceFolder=${sourceFolder.normalize()}, thumbnailPage=${thumbnailPageFile.normalize()}")
            singlePageTimekeeper.logElapsed()
        }
    }

    void generateThumbnailPageFromPdfs() {
        ProcessorOption showDirectoryOption = ProcessorOption.showDirectoryOption(processorConfiguration.processorOptions,
                ProcessorOption.ShowDirectoryAndTwoParents)
        boolean generateForSubfolders = processorConfiguration.processorOptions.contains(ProcessorOption.SearchSubdirectories)
        if (generateForSubfolders) {
            generateThumbnailPageFromPdfs(processorConfiguration.sourceFolder)
            List<Path> allSubdirectories = [ ]
            if (processorConfiguration.startingDate != null && processorConfiguration.endingDate != null) {
                allSubdirectories = PathUtils.allSubdirectoriesInDateRange(processorConfiguration.sourceFolder,
                        processorConfiguration.startingDate, processorConfiguration.endingDate,
                        GeneralUtils.DATE_YYYYMMDD_FORMATTER, true)
            } else {
                allSubdirectories = PathUtils.allSubdirectories(processorConfiguration.sourceFolder, true)
            }
            int numberOfThreads = processorConfiguration.parallelizeProcessing ? processorConfiguration.numberOfThreads : 1
            log.info("Processing over numberOfThreads=${numberOfThreads}")

            ThreadedThumbnailGenerator.changeMaximumConcurrentThreads(processorConfiguration.maximumThumbnailPageThreads)
            log.info("Maximum number of threads processing thumbnails=${processorConfiguration.maximumThumbnailPageThreads}")

            log.info("Starting processing total subdirectories=${allSubdirectories.size()}")
            GParsExecutorsPool.withPool(numberOfThreads) {
                allSubdirectories.eachParallel { Path subdirectory ->
                    try {
                        generateThumbnailPageFromPdfs(subdirectory, showDirectoryOption)
                    } catch (Exception e) {
                        log.error("Exception processing subdirectory=${subdirectory.normalize().toString()}", e)
                    }
                }
            }
            log.info("Finished processing total subdirectories=${allSubdirectories.size()}")
            processorConfiguration.timekeeper.logElapsed(false, allSubdirectories.size(), true)
        } else {
            generateThumbnailPageFromPdfs(processorConfiguration.sourceFolder, showDirectoryOption)
        }
    }
}