package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper
import nz.govt.natlib.tools.sip.files.FilesFinder
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFileTitleEditionKey
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.pdf.PdfInformationExtractor

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.regex.Matcher

@Slf4j
class MiscellaneousProcessor {
    Timekeeper timekeeper
    Set<String> recognizedTitleCodes = []
    Set<String> unrecognizedTitleCodes = []

    MiscellaneousProcessor(Timekeeper timekeeper) {
        this.timekeeper = timekeeper
    }

    void listFiles(File sourceFolder) {
        log.info("STARTING listFiles doLast")

        // Clear the set of recognized and unrecognized names before processing begins
        recognizedTitleCodes = []
        unrecognizedTitleCodes = []
        Set<FairfaxFileTitleEditionKey> recognizedTitleCodeEditionCodes = []
        Set<FairfaxFileTitleEditionKey> unrecognizedTitleCodeEditionCodes = []
        Set<File> invalidFiles = []

        log.info("sourceFolder=${sourceFolder}")

        FairfaxSpreadsheet fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()
        Set<String> allNameKeys = fairfaxSpreadsheet.allTitleCodeKeys
        Set<FairfaxFileTitleEditionKey> allNameEditionKeys = fairfaxSpreadsheet.allTitleCodeEditionCodeKeys

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        String pattern = ".*?\\.pdf"
        List<File> foundFiles = ProcessorUtils.findFiles(sourceFolder.getAbsolutePath(), isRegexNotGlob,
                matchFilenameOnly, sortFiles, pattern, timekeeper)
        List<FairfaxFile> fairfaxFiles = foundFiles.collect { File file ->
            new FairfaxFile(file)
        }

        FairfaxFile previousFile
        fairfaxFiles.each { FairfaxFile fairfaxFile ->
            if (fairfaxFile.isValidName()) {
                if (allNameKeys.contains(fairfaxFile.titleCode)) {
                    if (!recognizedTitleCodes.contains(fairfaxFile.titleCode)) {
                        recognizedTitleCodes.add(fairfaxFile.titleCode)
                        log.info("listFiles adding recognizedTitleCode=${fairfaxFile.titleCode}")
                    }
                } else {
                    if (!unrecognizedTitleCodes.contains(fairfaxFile.titleCode)) {
                        unrecognizedTitleCodes.add(fairfaxFile.titleCode)
                        log.info("listFiles adding unrecognizedTitleCode=${fairfaxFile.titleCode}")
                    }
                }
                FairfaxFileTitleEditionKey fairfaxFileTitleEditionKey = new FairfaxFileTitleEditionKey(
                        titleCode: fairfaxFile.titleCode, editionCode: fairfaxFile.editionCode)
                if (allNameEditionKeys.contains(fairfaxFileTitleEditionKey)) {
                    if (!recognizedTitleCodeEditionCodes.contains(fairfaxFileTitleEditionKey)) {
                        recognizedTitleCodeEditionCodes.add(fairfaxFileTitleEditionKey)
                        log.info("listFiles adding recognizedTitleCodeEditionCodes=${fairfaxFileTitleEditionKey}")
                    }
                } else {
                    if (!unrecognizedTitleCodeEditionCodes.contains(fairfaxFileTitleEditionKey)) {
                        unrecognizedTitleCodeEditionCodes.add(fairfaxFileTitleEditionKey)
                        log.info("listFiles adding unrecognizedTitleCodeEditionCodes=${fairfaxFileTitleEditionKey}")
                    }
                }
            } else {
                invalidFiles.add(fairfaxFile.file)
            }

            if (previousFile != null) {
                if (previousFile.titleCode != fairfaxFile.titleCode) {
                    println("* * * CHANGE OF PREFIX * * *")
                } else if (previousFile.editionCode != fairfaxFile.editionCode) {
                    println("* * * CHANGE OF EDITION * * *")
                } else if (previousFile.dateYear != fairfaxFile.dateYear &&
                        previousFile.dateMonthOfYear != fairfaxFile.dateMonthOfYear &&
                        previousFile.dateDayOfMonth != fairfaxFile.dateDayOfMonth) {
                    println("* * * CHANGE OF DATE * * *")
                }
            }
            println(fairfaxFile)

            previousFile = fairfaxFile
        }

        log.info("* * * *")
        log.info("Recognized tileCodes:")
        recognizedTitleCodes.each { String recognizedName ->
            log.info("    ${recognizedName}")
        }
        log.info("* * * *")
        log.info("Recognized titleCodes and editionCodes:")
        recognizedTitleCodeEditionCodes.each { FairfaxFileTitleEditionKey fairfaxFileNameEditionKey ->
            log.info("    ${fairfaxFileNameEditionKey}")
        }
        log.info("* * * *")
        log.info("UNRECOGNIZED titleCodes:")
        unrecognizedTitleCodes.each { String recognizedName ->
            log.info("    ${recognizedName}")
        }
        log.info("* * * *")
        log.info("UNRECOGNIZED titleCodes and editionCodes:")
        unrecognizedTitleCodeEditionCodes.each { FairfaxFileTitleEditionKey fairfaxFileNameEditionKey ->
            log.info("    ${fairfaxFileNameEditionKey}")
        }
        log.info("* * * *")
        log.info("INVALID files:")
        invalidFiles.each { File file ->
            log.info("    ${file.getCanonicalPath()}")
        }
        log.info("* * * *")

        log.info("ENDING listFiles doLast")
        timekeeper.logElapsed()
    }

    void extractMetadata(File sourceFolder) {
        log.info("STARTING extractMetadata doLast")
        FileNameFinder fileNameFinder = new FileNameFinder()
        List<String> filenames = fileNameFinder.getFileNames(sourceFolder.getAbsolutePath(), "**/*.pdf")
        List<File> pdfFiles = filenames.collect { String filename ->
            new File(filename)
        }

        pdfFiles.each { File pdfFile ->
            log.info("* * * * *")
            log.info("${pdfFile.getCanonicalPath()} METADATA:")
            Map<String, String> pdfMetadata = PdfInformationExtractor.extractMetadata(pdfFile)
            pdfMetadata.each { String key, String value ->
                log.info("    key=${key}, value=${value}")
            }
            log.info("* * * * *")
            log.info("* * * * *")
            log.info("${pdfFile.getCanonicalPath()} TEXT:")
            String text = PdfInformationExtractor.extractText(pdfFile)
            log.info("${text}")
            log.info("* * * * *")
            log.info("* * * * *")
            log.info("")
        }

        timekeeper.logElapsed()
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

        // Load directories have the structure <titleCode>_<yyyyMMdd> (and possibly <titleCode><editionCode>_<yyyyMMdd>
        String pattern = '\\w{3,6}_\\d{8}'
        log.info("Finding directories for path=${filesPath.toFile().getCanonicalPath()} and pattern=${pattern}")
        timekeeper.logElapsed()
        directoriesList = FilesFinder.getMatchingFilesFull(filesPath, isRegexNotGlob, matchFilenameOnly, sortFiles,
                includeSubdirectories, directoryOnly, pattern)
        log.info("Found total directories=${directoriesList.size()} for path=${filesPath.toFile().getCanonicalPath()}")
        timekeeper.logElapsed()

        List<File> filteredDirectoriesList = []
        String regexPattern = '(?<titleCode>\\w{3,6})_(?<date>\\d{8})'
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

    // See the README.md for a description of the file structures.
    void copyIngestedLoadsToIngestedFolder(File sourceFolder, File destinationFolder, File forReviewFolder,
                                           boolean createDestination, boolean moveFiles, LocalDate startingDate,
                                           LocalDate endingDate, boolean moveOrCopyEvenIfNoRosettaDoneFile) {
        // Look for folders called 'content'. Does it have a 'mets.xml'?
        // Does the parent have a 'done' file (and done is needed)
        // Load the mets.xml to get the publication titleCode and date
        // If moving and parent of content folder has no other subfolders after moving, then delete it, and so on


    }

    // Split
    // See the README.md for a description of the file structures.
    void copyAndSplitBetweenNonIngestedAndIngested(File sourceFolder, File destinationFolder, File forReviewFolder,
                                           boolean createDestination, boolean moveFiles, LocalDate startingDate,
                                           LocalDate endingDate, boolean moveOrCopyEvenIfNoRosettaDoneFile) {
        // Look for folders called 'content'. Does it have a 'mets.xml'?
        // Does the parent have a 'done' file.
        // If it has a 'done' file it gets moved to the ingested folder.
        // If it doesn't have a 'done' file it gets moved to the pre-process folder (or ready-for-ingestion??).
        // Load the mets.xml to get the publication titleCode and date.
        // If moving and parent of content folder has no other subfolders after moving, then delete it, and so on


    }

    // Copies the prod load structure to two structures:
    // 1. preProcess structure. This is to mimic the input to preProcess.
    // 2. readyForIngestion structure. This is the structure that gets ingested into Rosetta.
    //
    // See the README.md for a description of the folder structures
    //
    // These structures provide for testing the Fairfax processor, to see if its outputs match the work done previously.
    void copyProdLoadToTestStructures(File sourceFolder, File destinationFolder, boolean createDestination,
                                      LocalDate startingDate, LocalDate endingDate) {
        // The source files are going to be in a subdirectory with the directory structure being:
        // <titleCode>_yyyyMMdd/content/streams/{files} with the mets.xml in the content directory.
        // Find the source directories that are between the starting date and the ending date
        List<File> filteredDirectoriesList = findProdLoadDirectoriesBetweenDates(sourceFolder.getCanonicalPath(),
                startingDate, endingDate)

        // We need to copy the files to the preProcess structure AND the readyForIngestion structure.
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        String pattern = '\\w{6}-\\d{8}-.*?\\.pdf'
        String directoryPattern = '(?<titleCode>\\w{3,6})_(?<date>\\d{8})'

        log.info("Processing filteredDirectories total=${filteredDirectoriesList.size()}")
        int filteredDirectoriesCount = 1
        filteredDirectoriesList.each { File sourceDirectory ->
            log.info("Processing ${filteredDirectoriesCount}/${filteredDirectoriesList.size()}, current=${sourceDirectory.getCanonicalPath()}")
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
                            matchFilenameOnly, sortFiles, pattern, timekeeper)
                    sourceFiles.addAll(pdfFiles)
                } else {
                    log.info("streamsFolder=${streamsFolder.getCanonicalPath()} does not exist -- SKIPPING")
                }
            } else {
                log.info("contentFolder=${contentFolder.getCanonicalPath()} does not exist -- SKIPPING")
            }

            // Copy to the preProcess structure
            File groupByDateAndNameDestinationFolder = new File(destinationFolder, "groupByDateAndName/${dateString}/${titleCodeString}")
            if (createDestination) {
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
            File rosettaIngestFolder = new File(destinationFolder, "rosettaIngest/${dateString}/${titleCodeString}_${dateString}")
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
}