package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper
import nz.govt.natlib.tools.sip.files.FilesFinder
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFileNameEditionKey
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.pdf.PdfInformationExtractor

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.regex.Matcher

@Slf4j
class MiscellaneousProcessor {
    Set<String> recognizedNames = []
    Set<String> unrecognizedNames = []

    void listFiles(File sourceFolder, Timekeeper timekeeper) {
        log.info("STARTING listFiles doLast")

        // Clear the set of recognized and unrecognized names before processing begins
        recognizedNames = []
        unrecognizedNames = []
        Set<FairfaxFileNameEditionKey> recognizedNameEditions = []
        Set<FairfaxFileNameEditionKey> unrecognizedNameEditions = []
        Set<File> invalidFiles = []

        log.info("sourceFolder=${sourceFolder}")

        FairfaxSpreadsheet fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()
        Set<String> allNameKeys = fairfaxSpreadsheet.allNameKeys
        Set<FairfaxFileNameEditionKey> allNameEditionKeys = fairfaxSpreadsheet.allNameEditionKeys

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        String pattern = ".*?\\.pdf"
        List<File> foundFiles = ProcessorUtils.findFiles(sourceFolder.getAbsolutePath(), isRegexNotGlob, matchFilenameOnly, sortFiles, pattern)
        List<FairfaxFile> fairfaxFiles = foundFiles.collect { File file ->
            new FairfaxFile(file)
        }

        FairfaxFile previousFile
        fairfaxFiles.each { FairfaxFile fairfaxFile ->
            if (fairfaxFile.isValid()) {
                if (allNameKeys.contains(fairfaxFile.name)) {
                    if (!recognizedNames.contains(fairfaxFile.name)) {
                        recognizedNames.add(fairfaxFile.name)
                        log.info("listFiles adding recognizedName=${fairfaxFile.name}")
                    }
                } else {
                    if (!unrecognizedNames.contains(fairfaxFile.name)) {
                        unrecognizedNames.add(fairfaxFile.name)
                        log.info("listFiles adding unrecognizedName=${fairfaxFile.name}")
                    }
                }
                FairfaxFileNameEditionKey fairfaxFileNameEditionKey = new FairfaxFileNameEditionKey(
                        name: fairfaxFile.name, edition: fairfaxFile.edition)
                if (allNameEditionKeys.contains(fairfaxFileNameEditionKey)) {
                    if (!recognizedNameEditions.contains(fairfaxFileNameEditionKey)) {
                        recognizedNameEditions.add(fairfaxFileNameEditionKey)
                        log.info("listFiles adding recognizedNameEditions=${fairfaxFileNameEditionKey}")
                    }
                } else {
                    if (!unrecognizedNameEditions.contains(fairfaxFileNameEditionKey)) {
                        unrecognizedNameEditions.add(fairfaxFileNameEditionKey)
                        log.info("listFiles adding unrecognizedNameEditions=${fairfaxFileNameEditionKey}")
                    }
                }
            } else {
                invalidFiles.add(fairfaxFile.file)
            }

            if (previousFile != null) {
                if (previousFile.name != fairfaxFile.name) {
                    println("* * * CHANGE OF PREFIX * * *")
                } else if (previousFile.edition != fairfaxFile.edition) {
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
        log.info("Recognized names:")
        recognizedNames.each { String recognizedName ->
            log.info("    ${recognizedName}")
        }
        log.info("* * * *")
        log.info("Recognized names and editions:")
        recognizedNameEditions.each { FairfaxFileNameEditionKey fairfaxFileNameEditionKey ->
            log.info("    ${fairfaxFileNameEditionKey}")
        }
        log.info("* * * *")
        log.info("UNRECOGNIZED names:")
        unrecognizedNames.each { String recognizedName ->
            log.info("    ${recognizedName}")
        }
        log.info("* * * *")
        log.info("UNRECOGNIZED names and editions:")
        unrecognizedNameEditions.each { FairfaxFileNameEditionKey fairfaxFileNameEditionKey ->
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

    void extractMetadata(File sourceFolder, Timekeeper timekeeper) {
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

    List<File> findProdLoadDirectoriesBetweenDates(String localPath, LocalDate startingDate, LocalDate endingDate,
                                                   Timekeeper timekeeper) {
        List<File> directoriesList = []
        java.nio.file.Path filesPath = Paths.get(localPath)
        if (!Files.exists(filesPath) || !Files.isDirectory(filesPath)) {
            log.warn("Path '${filesPath}' does not exist is not a directory. Returning empty file list.")
            return directoriesList
        }

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        boolean includeSubdirectories = true
        boolean directoryOnly = true

        // Load directories have the structure <name>_<yyyyMMdd> (and possibly <name><edition>_<yyyyMMdd>
        String pattern = '\\w{3,6}_\\d{8}'
        log.info("Finding directories for path=${filesPath.toFile().getCanonicalPath()} and pattern=${pattern}")
        timekeeper.logElapsed()
        directoriesList = FilesFinder.getMatchingFilesFull(filesPath, isRegexNotGlob, matchFilenameOnly, sortFiles,
                includeSubdirectories, directoryOnly, pattern)
        log.info("Found total directories=${directoriesList.size()} for path=${filesPath.toFile().getCanonicalPath()}")
        timekeeper.logElapsed()

        List<File> filteredDirectoriesList = []
        String regexPattern = '(?<name>\\w{3,6})_(?<date>\\d{8})'
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

// Copies the prod load structure to two structures:
// 1. groupByDateAndName structure. This is to mimic the input to processByName.
//    Directory structure: groupByDateAndName/<yyyyMMdd>/<name>/{files}
// 2. post-processByDate structure. This is the structure that gets ingested into Rosetta.
//    Directory structure: rosettaIngest/<date-in-yyyMMdd>/<name>_<yyyyMMdd>-<identifier>/{files}
// These structures provide for testing the Fairfax processor, to see if its outputs match the work done previously.
    void copyProdLoadToTestStructures(File sourceFolder, File destinationFolder, boolean createDestination,
                                      LocalDate startingDate, LocalDate endingDate, Timekeeper timekeeper) {
        // The source files are going to be in a subdirectory with the directory structure being:
        // <name>_yyyyMMdd/content/streams/{files} with the mets.xml in the content directory.
        // Find the source directories that are between the starting date and the ending date
        List<File> filteredDirectoriesList = findProdLoadDirectoriesBetweenDates(sourceFolder.getCanonicalPath(),
                startingDate, endingDate)

        // We need to copy the files to the groupByDateAndName structure AND the post-processByDate structure.
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        String pattern = '\\w{6}-\\d{8}-.*?\\.pdf'
        String directoryPattern = '(?<name>\\w{3,6})_(?<date>\\d{8})'

        log.info("Processing filteredDirectories total=${filteredDirectoriesList.size()}")
        int filteredDirectoriesCount = 1
        filteredDirectoriesList.each { File sourceDirectory ->
            log.info("Processing ${filteredDirectoriesCount}/${filteredDirectoriesList.size()}, current=${sourceDirectory.getCanonicalPath()}")
            Matcher matcher = sourceDirectory.getName() =~ /${directoryPattern}/
            String dateString
            String nameString
            if (matcher.matches()) {
                dateString = matcher.group('date')
                nameString = matcher.group('name')
            } else {
                dateString = "UNKNOWN-DATE"
                nameString = "UNKNOWN-NAME"
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
                    List<File> pdfFiles = ProcessorUtils.findFiles(streamsFolder.getAbsolutePath(), isRegexNotGlob, matchFilenameOnly, sortFiles, pattern)
                    sourceFiles.addAll(pdfFiles)
                } else {
                    log.info("streamsFolder=${streamsFolder.getCanonicalPath()} does not exist -- SKIPPING")
                }
            } else {
                log.info("contentFolder=${contentFolder.getCanonicalPath()} does not exist -- SKIPPING")
            }

            // Copy to the groupByDateAndName structure
            File groupByDateAndNameDestinationFolder = new File(destinationFolder, "groupByDateAndName/${dateString}/${nameString}")
            groupByDateAndNameDestinationFolder.mkdirs()
            sourceFiles.each { File sourceFile ->
                File destinationFile = new File(groupByDateAndNameDestinationFolder, sourceFile.getName())
                if (!destinationFile.exists()) {
                    Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
                }
            }

            /// Copy to the post-processByDate structure
            File rosettaIngestFolder = new File(destinationFolder, "rosettaIngest/${dateString}/${nameString}_${dateString}")
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