package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFileTitleEditionKey
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.generation.fairfax.processor.support.TitleCodeByDateSummary
import nz.govt.natlib.tools.sip.pdf.PdfInformationExtractor

import java.time.LocalDate
import java.time.Period

@Slf4j
class ReportsProcessor {
    ProcessorConfiguration processorConfiguration
    Set<String> recognizedTitleCodes = []
    Set<String> unrecognizedTitleCodes = []

    ReportsProcessor(ProcessorConfiguration processorConfiguration) {
        this.processorConfiguration = processorConfiguration
    }

    void listFiles() {
        log.info("STARTING listFiles")

        // Clear the set of recognized and unrecognized names before processing begins
        recognizedTitleCodes = []
        unrecognizedTitleCodes = []
        Set<FairfaxFileTitleEditionKey> recognizedTitleCodeEditionCodes = []
        Set<FairfaxFileTitleEditionKey> unrecognizedTitleCodeEditionCodes = []
        Set<File> invalidFiles = []

        log.info("sourceFolder=${processorConfiguration.sourceFolder}")

        FairfaxSpreadsheet fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()
        Set<String> allNameKeys = fairfaxSpreadsheet.allTitleCodeKeys
        Set<FairfaxFileTitleEditionKey> allNameEditionKeys = fairfaxSpreadsheet.allTitleCodeEditionCodeKeys

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        String pattern = ".*?\\.[pP]{1}[dD]{1}[fF]{1}"
        List<File> foundFiles = ProcessorUtils.findFiles(processorConfiguration.sourceFolder.getAbsolutePath(),
                isRegexNotGlob, matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper)
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

        log.info("ENDING listFiles")
        processorConfiguration.timekeeper.logElapsed()
    }

    void statisticalAudit() {
        log.info("STARTING statisticalAudit")
        StringBuilder summaryTextBuilder = new StringBuilder()

        // Clear the set of recognized and unrecognized names before processing begins
        recognizedTitleCodes = []
        unrecognizedTitleCodes = []
        Set<FairfaxFileTitleEditionKey> recognizedTitleCodeEditionCodes = []
        Set<FairfaxFileTitleEditionKey> unrecognizedTitleCodeEditionCodes = []
        Set<File> invalidFiles = []
        List<Tuple2<LocalDate, Integer>> totalsByDateList = [ ]

        log.info("sourceFolder=${processorConfiguration.sourceFolder}")

        FairfaxSpreadsheet fairfaxSpreadsheet = FairfaxSpreadsheet.defaultInstance()
        Set<String> allNameKeys = fairfaxSpreadsheet.allTitleCodeKeys
        Set<FairfaxFileTitleEditionKey> allNameEditionKeys = fairfaxSpreadsheet.allTitleCodeEditionCodeKeys

        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        String pattern = ".*?\\.[pP]{1}[dD]{1}[fF]{1}"
        List<File> foundFiles = ProcessorUtils.findFiles(processorConfiguration.sourceFolder.getAbsolutePath(),
                isRegexNotGlob, matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper)
        Map<LocalDate, Map<String, TitleCodeByDateSummary>> dateToTitleCodeMap = [ : ]
        foundFiles.each { File file ->
            FairfaxFile fairfaxFile = new FairfaxFile(file)
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
                        //log.info("listFiles adding recognizedTitleCodeEditionCodes=${fairfaxFileTitleEditionKey}")
                    }
                } else {
                    if (!unrecognizedTitleCodeEditionCodes.contains(fairfaxFileTitleEditionKey)) {
                        unrecognizedTitleCodeEditionCodes.add(fairfaxFileTitleEditionKey)
                        //log.info("listFiles adding unrecognizedTitleCodeEditionCodes=${fairfaxFileTitleEditionKey}")
                    }
                }

                LocalDate localDate = fairfaxFile.dateAsLocalDate()
                Map<String, TitleCodeByDateSummary> titleCodeToSummaryMap
                if (dateToTitleCodeMap.containsKey(localDate)) {
                    titleCodeToSummaryMap = dateToTitleCodeMap.get(localDate)
                } else {
                    titleCodeToSummaryMap = [:]
                    dateToTitleCodeMap.put(localDate, titleCodeToSummaryMap)
                }
                TitleCodeByDateSummary titleCodeByDateSummary
                if (titleCodeToSummaryMap.containsKey(fairfaxFile.titleCode)) {
                    titleCodeByDateSummary = titleCodeToSummaryMap.get(fairfaxFile.titleCode)
                } else {
                    titleCodeByDateSummary = new TitleCodeByDateSummary(localDate: localDate,
                            titleCode: fairfaxFile.titleCode)
                    titleCodeToSummaryMap.put(fairfaxFile.titleCode, titleCodeByDateSummary)
                }
                titleCodeByDateSummary.addFile(fairfaxFile)
            } else {
                invalidFiles.add(file)
            }
        }

        log.info("* * * *")
        logAndAppend(summaryTextBuilder, "Recognized tileCodes:")
        recognizedTitleCodes.each { String recognizedName ->
            logAndAppend(summaryTextBuilder, "${recognizedName}")
        }
        log.info("* * * *")
        logAndAppend(summaryTextBuilder, "Recognized titleCode/editionCode:")
        recognizedTitleCodeEditionCodes.each { FairfaxFileTitleEditionKey fairfaxFileNameEditionKey ->
            logAndAppend(summaryTextBuilder, "${fairfaxFileNameEditionKey.titleCode}/" +
                    "${fairfaxFileNameEditionKey.editionCode}")
        }
        log.info("* * * *")
        logAndAppend(summaryTextBuilder, "UNRECOGNIZED titleCodes:")
        unrecognizedTitleCodes.each { String recognizedName ->
            logAndAppend(summaryTextBuilder, "${recognizedName}")
        }
        log.info("* * * *")
        logAndAppend(summaryTextBuilder, "UNRECOGNIZED titleCode/editionCode:")
        unrecognizedTitleCodeEditionCodes.each { FairfaxFileTitleEditionKey fairfaxFileNameEditionKey ->
            logAndAppend(summaryTextBuilder, "${fairfaxFileNameEditionKey.titleCode}/" +
                    "${fairfaxFileNameEditionKey.editionCode}")
        }
        log.info("* * * *")
        logAndAppend(summaryTextBuilder, "INVALID files:")
        invalidFiles.each { File file ->
            logAndAppend(summaryTextBuilder, "${file.getCanonicalPath()}")
        }
        log.info("* * * *")

        println("Processing detail for sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}:")
        println()
        println("date|total_files|title_code|out-of-sequence-files|duplicate-files")
        String spreadsheetSeparator = "|"
        List<LocalDate> sortedDates = dateToTitleCodeMap.keySet().sort()
        LocalDate lastDate
        List<LocalDate> dateGaps = [ ]
        sortedDates.each { LocalDate dateKey ->
            if (lastDate != null) {
                Period sinceLastDate = Period.between(lastDate, dateKey)
                if (sinceLastDate.getDays() > 1) {
                    lastDate = lastDate.plusDays(1)
                    dateGaps.add(lastDate)
                    totalsByDateList.add(new Tuple2<LocalDate, Integer>(lastDate, 0))
                    while ((lastDate = lastDate.plusDays(1)) != dateKey) {
                        dateGaps.add(lastDate)
                        totalsByDateList.add(new Tuple2<LocalDate, Integer>(lastDate, 0))
                    }
                }
            }
            Map<String, TitleCodeByDateSummary> titleCodeToSummaryMap = dateToTitleCodeMap.get(dateKey)
            List<String> sortedTitleCodes = titleCodeToSummaryMap.keySet().sort()
            boolean firstForDate = true
            int totalFilesForDate = 0
            sortedTitleCodes.each { String titleCode ->
                TitleCodeByDateSummary titleCodeByDateSummary = titleCodeToSummaryMap.get(titleCode)
                if (firstForDate) {
                    print("${dateKey}")
                    firstForDate = false
                }
                println("${spreadsheetSeparator}${titleCodeByDateSummary.forSpreadsheet(spreadsheetSeparator)}")
                totalFilesForDate += titleCodeByDateSummary.files.size()
            }
            println("${spreadsheetSeparator}${totalFilesForDate}")
            totalsByDateList.add(new Tuple2<LocalDate, Integer>(dateKey, totalFilesForDate))
            lastDate = dateKey
        }

        if (dateGaps.size() > 0) {
            log.info("* * * *")
            logAndAppend(summaryTextBuilder, "DATE gaps (missing dates):")
            dateGaps.each { LocalDate localDate ->
                logAndAppend(summaryTextBuilder, "${localDate}")
            }
            log.info("* * * *")
        }

        println()
        println("Processing exceptions summary for sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}:")
        println()
        println(summaryTextBuilder.toString())
        println()

        println("Date totals summary for sourceFolder=${processorConfiguration.sourceFolder.getCanonicalPath()}:")
        println("Date|Total for date")
        totalsByDateList.each { Tuple2<LocalDate, Integer> dateTotalTuple ->
            println("${dateTotalTuple.first}|${dateTotalTuple.second}")
        }
        println()

        log.info("ENDING statisticalAudit")
        processorConfiguration.timekeeper.logElapsed()
    }

    void logAndAppend(StringBuilder stringBuilder, String message) {
        stringBuilder.append(message)
        stringBuilder.append(System.lineSeparator())
        log.info(message)
    }

    void extractMetadata() {
        log.info("STARTING extractMetadata doLast")
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        boolean includeSubdirectories = true
        String pattern = ".*?\\.[pP]{1}[dD]{1}[fF]{1}"
        List<File> pdfFiles = ProcessorUtils.findFiles(processorConfiguration.sourceFolder.getAbsolutePath(),
                isRegexNotGlob, matchFilenameOnly, sortFiles, pattern, processorConfiguration.timekeeper,
                includeSubdirectories)

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

        processorConfiguration.timekeeper.logElapsed()
    }

}
