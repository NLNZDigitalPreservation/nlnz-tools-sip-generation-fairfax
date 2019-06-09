package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Log4j2
import groovyx.gpars.GParsExecutorsPool
import nz.govt.natlib.tools.sip.files.FilesFinder
import nz.govt.natlib.tools.sip.logging.ThreadedTimekeeper
import nz.govt.natlib.tools.sip.logging.Timekeeper
import nz.govt.natlib.tools.sip.pdf.thumbnail.ThumbnailGenerator
import nz.govt.natlib.tools.sip.pdf.thumbnail.ThumbnailParameters

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

    List<File> findProdLoadDirectoriesBetweenDates(String localPath, LocalDate startingDate, LocalDate endingDate) {
        List<File> directoriesList = []
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
        log.info("Finding directories for path=${filesPath.toFile().getCanonicalPath()} and pattern=${pattern}")
        processorConfiguration.timekeeper.logElapsed()
        directoriesList = FilesFinder.getMatchingFilesFull(filesPath, isRegexNotGlob, matchFilenameOnly, sortFiles,
                includeSubdirectories, directoryOnly, pattern)
        log.info("Found total directories=${directoriesList.size()} for path=${filesPath.toFile().getCanonicalPath()}")
        processorConfiguration.timekeeper.logElapsed()

        List<File> filteredDirectoriesList = []
        String regexPattern = '(?<titleCode>\\w{3,7})_(?<date>\\d{8})'
        directoriesList.each { File directory ->
            Matcher matcher = directory.getName() =~ /${regexPattern}/
            if (matcher.matches()) {
                String dateString = matcher.group('date')
                LocalDate directoryDate = ProcessorUtils.parseDate(dateString)
                if ((directoryDate.isEqual(startingDate) || directoryDate.isAfter(startingDate)) &&
                        (directoryDate.isBefore(endingDate) || directoryDate.isEqual(endingDate))) {
                    filteredDirectoriesList.add(directory)
                }
            }
        }
        return filteredDirectoriesList
    }

    // See the README.md (Ingested stage) for a description of the file structures.
    void copyIngestedLoadsToIngestedFolder() {
        // Look for folders called 'content'. Does it have a 'mets.xml'?
        // Does the parent have a 'done' file (and done is needed)
        // Load the mets.xml to get the publication titleCode and date
        // If moving and parent of content folder has no other subfolders after moving, then delete it, and so on
        log.info("copyIngestedLoadsToIngestedFolder: Currently this work is being done by the python script:")
        log.info("    fairfax-pre-and-post-process-grouper.py")
        log.info("    See the github repository: https://github.com/NLNZDigitalPreservation/nlnz-tools-scripts-ingestion")
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
        List<File> filteredDirectoriesList = findProdLoadDirectoriesBetweenDates(
                processorConfiguration.sourceFolder.getCanonicalPath(),
                processorConfiguration.startingDate, processorConfiguration.endingDate)

        // We need to copy the files to the preProcess structure AND the readyForIngestion structure.
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        String pattern = '\\w{5,7}-\\d{8}-.*?\\.[pP]{1}[dD]{1}[fF]{1}'
        String directoryPattern = '(?<titleCode>\\w{3,7})_(?<date>\\d{8})'

        log.info("Processing filteredDirectories total=${filteredDirectoriesList.size()}")
        int filteredDirectoriesCount = 1
        filteredDirectoriesList.each { File sourceDirectory ->
            log.info("Processing ${filteredDirectoriesCount}/${filteredDirectoriesList.size()}, " +
                    "current=${processorConfiguration.sourceFolder.getCanonicalPath()}")
            Matcher matcher = sourceDirectory.getName() =~ /${directoryPattern}/
            String dateString
            String titleCodeString
            if (matcher.matches()) {
                dateString = matcher.group('date')
                titleCodeString = matcher.group('titleCode')
            } else {
                dateString = "UNKNOWN-DATE"
                titleCodeString = "UNKNOWN-TITLE-CODE"
            }
            List<File> sourceFiles = []
            File contentFolder = new File(sourceDirectory, "content")
            if (contentFolder.exists()) {
                File metsFile = new File(contentFolder, "mets.xml")
                if (metsFile.exists()) {
                    sourceFiles.add(metsFile)
                } else {
                    log.info("metsFile=${metsFile.getCanonicalPath()} does not exist -- SKIPPING")
                }
                File streamsFolder = new File(contentFolder, "streams")
                if (streamsFolder.exists()) {
                    List<File> pdfFiles = ProcessorUtils.findFiles(streamsFolder.getAbsolutePath(), isRegexNotGlob,
                            matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper)
                    sourceFiles.addAll(pdfFiles)
                } else {
                    log.info("streamsFolder=${streamsFolder.getCanonicalPath()} does not exist -- SKIPPING")
                }
            } else {
                log.info("contentFolder=${contentFolder.getCanonicalPath()} does not exist -- SKIPPING")
            }

            // Copy to the preProcess structure
            File groupByDateAndNameDestinationFolder = new File(processorConfiguration.targetFolder,
                    "groupByDateAndName/${dateString}/${titleCodeString}")
            if (processorConfiguration.createDestination) {
                groupByDateAndNameDestinationFolder.mkdirs()
            }
            groupByDateAndNameDestinationFolder.mkdirs()
            sourceFiles.each { File sourceFile ->
                File destinationFile = new File(groupByDateAndNameDestinationFolder, sourceFile.getName())
                if (!destinationFile.exists()) {
                    Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
                }
            }

            /// Copy to the readyForIngestion structure
            File rosettaIngestFolder = new File(processorConfiguration.targetFolder,
                    "rosettaIngest/${dateString}/${titleCodeString}_${dateString}")
            rosettaIngestFolder.mkdirs()
            sourceFiles.each { File sourceFile ->
                File destinationFile = new File(rosettaIngestFolder, sourceFile.getName())
                if (!destinationFile.exists()) {
                    Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
                }
            }
            filteredDirectoriesCount += 1
        }
    }

    List<File> findPdfFiles(File sourceFolder, boolean includeSubdirectories = false) {
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        // Any pdf will do
        String pattern = '.*?\\.[pP]{1}[dD]{1}[fF]{1}'
        List<File> pdfFiles = ProcessorUtils.findFiles(sourceFolder.getAbsolutePath(),
                isRegexNotGlob, matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper,
                includeSubdirectories)

        return pdfFiles
    }

    void generateThumbnailPageFromPdfs(File sourceFolder,
                                       ProcessorOption showDirectoryOption = ProcessorOption.ShowDirectoryAndTwoParents) {
        // TODO This could be a processor option (but make it clear it's different from 'SearchSubdirectories')
        boolean includeSubdirectories = false
        List<File> pdfFiles = findPdfFiles(sourceFolder, includeSubdirectories)
        if (pdfFiles.isEmpty()) {
            log.info("No PDF files found in folder=${sourceFolder.getCanonicalPath()}")
        } else {
            String convertedFilepath = ProcessorUtils.filePathAsSafeString(sourceFolder, [ showDirectoryOption ])
            File thumbnailPageFile
            if (processorConfiguration.processorOptions.contains(ProcessorOption.UseSourceSubdirectoryAsTarget)) {
                thumbnailPageFile = new File(sourceFolder, "${convertedFilepath}_thumbnail_page.jpeg")
            } else {
                thumbnailPageFile = new File(processorConfiguration.targetFolder, "${convertedFilepath}_thumbnail_page.jpeg")
            }

            String thumbnailPageTitle = "PDF files in ${sourceFolder.getCanonicalPath()}"
            ThumbnailParameters thumbnailParameters = new ThumbnailParameters(thumbnailHeight: 240,
                    useAffineTransformation: false, textJustification: ThumbnailParameters.TextJustification.RIGHT,
                    maximumPageWidth: 1200, pageTitleText: thumbnailPageTitle,
                    pageTitleFontJustification: ThumbnailParameters.TextJustification.RIGHT)

            log.info("START Generating thumbnail page from pdfs in sourceFolder=${sourceFolder.getCanonicalPath()}, thumbnailPage=${thumbnailPageFile.getCanonicalPath()}")
            Timekeeper singlePageTimekeeper = ThreadedTimekeeper.forCurrentThread()
            singlePageTimekeeper.start()
            ThumbnailGenerator.writeThumbnailPage(pdfFiles, thumbnailParameters, thumbnailPageFile)
            log.info("END Generated thumbnail page from pdfs in sourceFolder=${sourceFolder.getCanonicalPath()}, thumbnailPage=${thumbnailPageFile.getCanonicalPath()}")
            singlePageTimekeeper.logElapsed()
        }
    }

    void generateThumbnailPageFromPdfs() {
        ProcessorOption showDirectoryOption = ProcessorOption.showDirectoryOption(processorConfiguration.processorOptions,
                ProcessorOption.ShowDirectoryAndTwoParents)
        boolean generateForSubfolders = processorConfiguration.processorOptions.contains(ProcessorOption.SearchSubdirectories)
        if (generateForSubfolders) {
            generateThumbnailPageFromPdfs(processorConfiguration.sourceFolder)
            List<File> allSubdirectories = ProcessorUtils.allSubdirectories(processorConfiguration.sourceFolder)
            int numberOfThreads = processorConfiguration.parallelizeProcessing ? processorConfiguration.numberOfThreads : 1
            log.info("Processing over numberOfThreads=${numberOfThreads}")
            log.info("Starting processing total subdirectories=${allSubdirectories.size()}")
            GParsExecutorsPool.withPool(numberOfThreads) {
                allSubdirectories.eachParallel { File subdirectory ->
                    generateThumbnailPageFromPdfs(subdirectory, showDirectoryOption)
                }
            }
            log.info("Finished processing total subdirectories=${allSubdirectories.size()}")
            processorConfiguration.timekeeper.logElapsed(false, allSubdirectories.size(), true)
        } else {
            generateThumbnailPageFromPdfs(processorConfiguration.sourceFolder, showDirectoryOption)
        }
    }
}