package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.IEEntityType
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.SipFileWrapperFactory
import nz.govt.natlib.tools.sip.generation.SipXmlGenerator
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxSpreadsheet
import nz.govt.natlib.tools.sip.generation.fairfax.SipFactory
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType
import nz.govt.natlib.tools.sip.generation.fairfax.processor.type.ParentGroupingProcessor
import nz.govt.natlib.tools.sip.generation.fairfax.processor.type.ParentGroupingWithEditionProcessor
import nz.govt.natlib.tools.sip.generation.fairfax.processor.type.SipForFolderProcessor
import nz.govt.natlib.tools.sip.generation.fairfax.processor.type.SupplementGroupingProcessor
import nz.govt.natlib.tools.sip.generation.fairfax.special.PageUnavailableWriter
import nz.govt.natlib.tools.sip.logging.JvmPerformanceLogger
import nz.govt.natlib.tools.sip.pdf.PdfValidator
import nz.govt.natlib.tools.sip.pdf.PdfValidatorFactory
import nz.govt.natlib.tools.sip.pdf.PdfValidatorType
import nz.govt.natlib.tools.sip.pdf.thumbnail.ThreadedThumbnailGenerator
import nz.govt.natlib.tools.sip.pdf.thumbnail.ThumbnailParameters
import nz.govt.natlib.tools.sip.state.SipProcessingException
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReason
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType
import nz.govt.natlib.tools.sip.state.SipProcessingState

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

/**
 * The main processing class. Takes a given set of processing parameters and
 * a set of files and processes them according to the parameters.
 */
@Log4j2
class FairfaxFilesProcessor {
    static final String FOREVER_PROJECT_PREFIX = "FP"
    static final String[] PROPERTY_TITLES = ["HON", "SOP", "HOC", "HOW", "HWE", "PRB", "CHM"]
    static final String[] LIFE_SUPPLEMENTS = ["LID", "LIP"]

    FairfaxProcessingParameters processingParameters
    List<Path> filesForProcessing

    Map<FairfaxFile, FairfaxFile> processedFairfaxFiles

    static Sip getBlankSip() {
        Sip sip = new Sip(title: 'UNKNOWN_TITLE', ieEntityType: IEEntityType.UNKNOWN,
                objectIdentifierType: 'UNKNOWN_OBJECT_IDENTIFIER_TYPE',
                objectIdentifierValue: 'UNKNOWN_OBJECT_IDENTIFIER_VALUE', policyId: 'UNKNOWN_POLICY_ID',
                preservationType: 'UNKNOWN_PRESERVATION_TYPE', usageType: 'UNKNOWN_USAGE_TYPE',
                digitalOriginal: true, revisionNumber: 1,
                year: 2038, month: 12, dayOfMonth: 31)
        sip.updateFromDateFields()
        return sip
    }

    static void processCollectedFiles(FairfaxProcessingParameters processingParameters,
                                      List<Path> filesForProcessing) {
        FairfaxFilesProcessor fairfaxFilesProcessor = new FairfaxFilesProcessor(processingParameters,
                filesForProcessing)
        if (processingParameters.rules.contains(ProcessingRule.ForceSkip)) {
            log.info("Skipping processing sourceFolder=${processingParameters.sourceFolder.normalize()} as processing rules include=${ProcessingRule.ForceSkip.fieldValue}")
            processingParameters.skip = true
            processingParameters.sipProcessingState.sipAsXml = SipProcessingState.EMPTY_SIP_AS_XML
            return
        }
        fairfaxFilesProcessor.process()
    }

    FairfaxFilesProcessor(FairfaxProcessingParameters processingParameters, List<Path> filesForProcessing) {
        this.processingParameters = processingParameters
        this.filesForProcessing = filesForProcessing
    }

    void process() {
        log.info("STARTING process for processingParameters=${processingParameters}")
        JvmPerformanceLogger.logState("FairfaxFilesProcessor Current thread state at start",
                false, true, true, false, true, false, true)
        processedFairfaxFiles = [ : ]

        if (this.processingParameters.valid) {
            List<FairfaxFile> fairfaxFilesForProcessing = filesForProcessing.collect { Path rawFile ->
                new FairfaxFile(rawFile)
            }
            List<FairfaxFile> validNamedFiles = extractValidNamedFiles(fairfaxFilesForProcessing)

            List<FairfaxFile> sortedFilesForProcessing
            switch (processingParameters.type) {
                case ProcessingType.ParentGrouping:
                    sortedFilesForProcessing = ParentGroupingProcessor.selectAndSort(processingParameters, validNamedFiles)
                    break
                case ProcessingType.ParentGroupingWithEdition:
                    sortedFilesForProcessing = ParentGroupingWithEditionProcessor.selectAndSort(processingParameters, validNamedFiles)
                    break
                case ProcessingType.SupplementGrouping:
                    sortedFilesForProcessing = SupplementGroupingProcessor.selectAndSort(processingParameters, validNamedFiles)
                    break
                case ProcessingType.CreateSipForFolder:
                    sortedFilesForProcessing = SipForFolderProcessor.selectAndSort(processingParameters, validNamedFiles)
                    if (processingParameters.spreadsheetRow == FairfaxSpreadsheet.BLANK_ROW) {
                        String detailedReason = "No matching spreadsheet row for titleCode=${processingParameters.titleCode}, " +
                                "date=${processingParameters.date}, folder=${processingParameters.sourceFolder.normalize()}."
                        SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                                SipProcessingExceptionReasonType.NO_MATCHING_SIP_DEFINITION, null,
                                detailedReason)
                        SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
                        processingParameters.sipProcessingState.addException(sipProcessingException)
                        log.warn(detailedReason)
                    } else {
                        // TODO Should this be an error
                        log.warn("ProcessingType.CreateSipForFolder should have SipProcessingException reason.")
                    }
                    break
                default:
                    sortedFilesForProcessing = []
                    break
            }

            // Determine if fairfaxFiles contains any files that starts with FP or is a Property or Life title code
            // If so, process the files as a single collection with FP/Property/Life files at the end
            boolean toAddAtEnd = false
            for (FairfaxFile file : sortedFilesForProcessing) {
                if (file.getTitleCode().startsWith(FOREVER_PROJECT_PREFIX) ||
                        PROPERTY_TITLES.contains(file.getTitleCode()) ||
                        (LIFE_SUPPLEMENTS.contains(file.getTitleCode()) &&
                                processingParameters.titleCode != file.getTitleCode()) ) {
                    toAddAtEnd = true
                    break
                }
            }
            // Property, Forever Project and Life are to be added at the end of the publication
            // Life goes before Homes which goes before Forever Project
            if (toAddAtEnd) {
                List<FairfaxFile> foreverProjectFiles = []
                List<FairfaxFile> propertyFiles = []
                List<FairfaxFile> lifeFiles = []
                List<FairfaxFile> sortedFiles = []
                for (FairfaxFile fairfaxFile : sortedFilesForProcessing) {
                     if (fairfaxFile.getTitleCode().startsWith(FOREVER_PROJECT_PREFIX)) {
                        foreverProjectFiles.add(fairfaxFile)
                    } else if (LIFE_SUPPLEMENTS.contains(fairfaxFile.getTitleCode())) {
                         lifeFiles.add(fairfaxFile)
                     } else if (PROPERTY_TITLES.contains(fairfaxFile.getTitleCode())) {
                        propertyFiles.add(fairfaxFile)
                    } else {
                        sortedFiles.add(fairfaxFile)
                    }
                }
                for (FairfaxFile pf : lifeFiles) {
                    sortedFiles.add(pf)
                }
                for (FairfaxFile pf : propertyFiles) {
                    sortedFiles.add(pf)
                }
                for (FairfaxFile ff : foreverProjectFiles) {
                    sortedFiles.add(ff)
                }
                sortedFilesForProcessing = sortedFiles
            }

            if (processingParameters.skip) {
                log.info("Skipping processing for processingParameters=${processingParameters}")
            } else {
                List<FairfaxFile> sipFiles = differentiateFiles(validNamedFiles, sortedFilesForProcessing)

                String sipAsXml = generateSipAsXml(sipFiles, processingParameters.date)
                processingParameters.sipProcessingState.sipAsXml = sipAsXml
            }
        }

        JvmPerformanceLogger.logState("FairfaxFilesProcessor Current thread state at end",
                false, true, true, false, true, false, true)
        log.info("ENDING process for processingParameters=${processingParameters}")
    }

    List<FairfaxFile> differentiateFiles(List<FairfaxFile> validNamedFiles, List<FairfaxFile> sortedFilesForProcessing) {
        processingParameters.sipProcessingState.ignoredFiles =
                FairfaxFile.differences(validNamedFiles, sortedFilesForProcessing).collect { FairfaxFile ffxFile ->
                    ffxFile.file
                }

        if (processingParameters.sipProcessingState.ignoredFiles.size() > 0 &&
                processingParameters.rules.contains(ProcessingRule.AllSectionsInSipRequired)) {
            // Strip the ignored of any editionDiscriminator files
            List<FairfaxFile> withoutEditionFiles = processingParameters.sipProcessingState.ignoredFiles.findAll {
                Path file ->
                    FairfaxFile fairfaxFile = new FairfaxFile(file)
                    !processingParameters.editionDiscriminators.contains(fairfaxFile.sectionCode)
            }
            if (!withoutEditionFiles.isEmpty()) {
                String detailedReason = "${ProcessingRule.AllSectionsInSipRequired.fieldValue} but these files are not processed=${withoutEditionFiles}".toString()
                if (processingParameters.rules.contains(ProcessingRule.AllSectionsInSipRequired)) {
                    SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                            SipProcessingExceptionReasonType.ALL_FILES_CANNOT_BE_PROCESSED, null,
                            detailedReason)
                    SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
                    processingParameters.sipProcessingState.addException(sipProcessingException)
                    log.warn(detailedReason)
                } else {
                    log.info(detailedReason)
                }
            }
        }

        sortedFilesForProcessing.each { FairfaxFile fileForProcessing ->
            processFile(fileForProcessing)
        }
        // TODO We are converting back and forth between FairfaxFile and File for different processing stages to
        // ensure that the sip-generation-core classes don't get polluted with Fairfax-specific functionality.
        // At some point we need to look at finding a better way. Perhaps there's an interface that might expose
        // a wrapper so that it can be processed through implementation-specific processing.
        // For the moment we do the conversion. This is something to consider when refactoring/redesigning this
        // application.
        List<FairfaxFile> sipFiles = processingParameters.sipProcessingState.sipFiles.collect { Path file ->
            new FairfaxFile(file)
        }
        checkForMissingSequenceFiles(sipFiles)

        checkForManualProcessing()

        // See the note above about converting back and forth.
        List<FairfaxFile> thumbnailPageFiles = processingParameters.sipProcessingState.thumbnailPageFiles.collect { Path file ->
            new FairfaxFile(file)
        }
        generateThumbnailPage(thumbnailPageFiles)
        // TODO If we are generating a thumbnail page when there are errors we may want to consider generating a
        // TODO thumbnail page for ALL the files (this could help in understanding the problem).
        return sipFiles
    }

    List<FairfaxFile> extractValidNamedFiles(List<FairfaxFile> originalList) {
        List<FairfaxFile> cleanedList = [ ]
        originalList.each { FairfaxFile fairfaxFile ->
            if (fairfaxFile.isValidName()) {
                cleanedList.add(fairfaxFile)
            } else {
                SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                        SipProcessingExceptionReasonType.INVALID_PAGE_FILENAME, null,
                        fairfaxFile.file.normalize().toString())
                SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
                processingParameters.sipProcessingState.addException(sipProcessingException)
                log.warn(sipProcessingException.toString())
                processingParameters.sipProcessingState.unrecognizedFiles.add(fairfaxFile.file)
            }
        }
        return cleanedList
    }

    void processFile(FairfaxFile fairfaxFile) {
        log.info("Processing fairfaxFile=${fairfaxFile}")
        // We generally include all files whether they are valid or invalid. We don't include duplicate files.
        SipProcessingState sipProcessingState = processingParameters.sipProcessingState

        boolean includeFileInSip = true
        boolean includeInThumbnailPage = true
        if (processedFairfaxFiles.containsKey(fairfaxFile)) {
            // We have a duplicate file -- possibly a different qualifier
            // We use the fairfax file as a key, but we'll get the duplicate back
            FairfaxFile firstVersion = processedFairfaxFiles.get(fairfaxFile)
            SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                    SipProcessingExceptionReasonType.DUPLICATE_FILE, null,
                    firstVersion.file.normalize().toString(), fairfaxFile.file.normalize().toString())
            sipProcessingState.addException(SipProcessingException.createWithReason(exceptionReason))
            includeFileInSip = false
        } else {
            processedFairfaxFiles.put(fairfaxFile, fairfaxFile)
            if (Files.size(fairfaxFile.file) == 0) {
                SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                        SipProcessingExceptionReasonType.FILE_OF_LENGTH_ZERO, null,
                        fairfaxFile.file.normalize().toString())
                fairfaxFile.zeroLengthFile = true
                if (processingParameters.rules.contains(ProcessingRule.ZeroLengthPdfReplacedWithPageUnavailablePdf)) {
                    Path replacementFile = PageUnavailableWriter.writeToToTemporaryDirectory(fairfaxFile.file.fileName.toString())
                    fairfaxFile.originalFile = fairfaxFile.file
                    fairfaxFile.file = replacementFile
                }
                processingParameters.sipProcessingState.addException(SipProcessingException.createWithReason(exceptionReason))
            } else {
                // We use the Jhove validator as it is the same type used by Rosetta.
                // There is also a PDF/A validator using the PdfValidatorType.PDF_BOX_VALIDATOR
                PdfValidator pdfValidator = PdfValidatorFactory.getValidator(PdfValidatorType.JHOVE_VALIDATOR)
                SipProcessingException sipProcessingException = pdfValidator.validatePdf(fairfaxFile.file)
                if (sipProcessingException != null) {
                    processingParameters.sipProcessingState.addException(sipProcessingException)
                } else {
                    fairfaxFile.validPdf = true
                    fairfaxFile.validForProcessing = true
                }
            }
        }
        if (fairfaxFile.validPdf && fairfaxFile.validForProcessing) {
            processingParameters.sipProcessingState.validFiles.add(fairfaxFile.file)
        } else {
            processingParameters.sipProcessingState.invalidFiles.add(fairfaxFile.originalFileOrFile)
        }
        if (includeFileInSip) {
            sipProcessingState.sipFiles.add(fairfaxFile.file)
        }
        if (includeInThumbnailPage) {
            sipProcessingState.thumbnailPageFiles.add(fairfaxFile.file)
        }
    }

    String generateSipAsXml(List<FairfaxFile> sortedFairfaxFiles, LocalDate sipDate) {
        String sipAsXml = ""
        if (sortedFairfaxFiles.isEmpty()) {
            String detailedReason = "Unable to process processingParameters=${processingParameters}: No matching files."
            SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                    SipProcessingExceptionReasonType.NO_MATCHING_SIP_DEFINITION, null,
                    detailedReason)
            SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
            processingParameters.sipProcessingState.addException(sipProcessingException)
            log.warn(detailedReason)
        } else {
            String titleKey = processingParameters.type == ProcessingType.SupplementGrouping ?
                    SipFactory.TITLE_METS_KEY :
                    SipFactory.TITLE_PARENT_KEY
            Sip sip = SipFactory.fromMap(processingParameters.spreadsheetRow, [ ], false, false, titleKey)

            sip.year = sipDate.year
            sip.month = sipDate.monthValue
            sip.dayOfMonth = sipDate.dayOfMonth
            sip.updateFromDateFields()

            if (processingParameters.includeCurrentEditionForDcCoverage) {
                sip.dcCoverage = "${sipDate.dayOfMonth} [${processingParameters.currentEdition}]"
            }
            processingParameters.sipProcessingState.ieEntityType = sip.ieEntityType
            processingParameters.sipProcessingState.identifier = formatSipProcessingStateIdentifier()

            List<Path> filesForSip = sortedFairfaxFiles.collect() { FairfaxFile fairfaxFile ->
                fairfaxFile.file
            }
            Sip testSip = sip.clone()
            sipAsXml = generateSipAsXml(testSip, filesForSip)
            processingParameters.sipProcessingState.totalFilesProcessed = filesForSip.size()
            processingParameters.sipProcessingState.setComplete(true)
            log.debug("\n* * *   S I P   * * *")
            log.debug(sipAsXml)
            log.debug("\n* * *   E N D   O F   S I P   * * *")
        }

        return sipAsXml
    }

    String generateSipAsXml(Sip sip, List<Path> files) {
        List<Sip.FileWrapper> fileWrappers = files.collect() { Path file ->
            SipFileWrapperFactory.generate(file, true, true)
        }
        int sequenceNumber = 1
        fileWrappers.each { Sip.FileWrapper fileWrapper ->
            String label = String.format("%04d", sequenceNumber)
            fileWrapper.label = label
            sequenceNumber += 1
        }
        sip.fileWrappers = fileWrappers
        SipXmlGenerator sipXmlGenerator = new SipXmlGenerator(sip)

        return sipXmlGenerator.getSipAsXml()
    }

    String formatSipProcessingStateIdentifier() {

        String titleWithUnderscores
        if (processingParameters.type == ProcessingType.SupplementGrouping) {
            // supplement_grouping needs to add a few more things for uniqueness
            // as it's possible for multiple supplements to have the same title code
            String sectionCodesString = processingParameters.sectionCodesString.
                    replace("+", "-").replace(",", "-")
            String titleAndId = processingParameters.titleMets.strip().replace(' ', '_') +
                    "_" + processingParameters.mmsid
            titleWithUnderscores = sectionCodesString.isEmpty() ? titleAndId : "${sectionCodesString}_${titleAndId}"
        } else {
            String title = processingParameters.getTitleParent()
            titleWithUnderscores = title.trim().replace(' ', '_')
        }
        if (processingParameters.currentEdition == null) {
            return "_${titleWithUnderscores}"
        } else {
            return "${processingParameters.currentEdition}__${titleWithUnderscores}"
        }
    }

    void checkForMissingSequenceFiles(List<FairfaxFile> checkList) {
        if (processingParameters.rules.contains(ProcessingRule.MissingSequenceError)) {
            List<FairfaxFile> postMissingSequenceFiles = FairfaxFile.postMissingSequenceFiles(checkList,
                    processingParameters)
            if (postMissingSequenceFiles.size() > 0) {
                boolean hasMissingFiles = true
                if (processingParameters.rules.contains(ProcessingRule.MissingSequenceDoubleWideIgnored)) {
                    hasMissingFiles = notAllMissingFilesAreDoubleWides(checkList, postMissingSequenceFiles)
                }
                if (hasMissingFiles) {
                    List<String> filenamesOnly = FairfaxFile.asFilenames(postMissingSequenceFiles)
                    String listOfFiles = "${filenamesOnly}".toString()
                    SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                            SipProcessingExceptionReasonType.MISSING_SEQUENCE_FILES, null,
                            listOfFiles)
                    SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
                    processingParameters.sipProcessingState.addException(sipProcessingException)
                    log.warn(exceptionReason.toString())
                }
            }
        }
    }

    boolean notAllMissingFilesAreDoubleWides(List<FairfaxFile> checkList, List<FairfaxFile> postMissingSequenceFiles) {
        FairfaxFile genuineMissingFile = postMissingSequenceFiles.find { FairfaxFile missingFile ->
            !isDoubleWideInSequence(checkList, missingFile)
        }
        return genuineMissingFile != null
    }

    boolean isDoubleWideInSequence(List<FairfaxFile> checkList, FairfaxFile postMissingFile) {
        int postMissingFileIndex = checkList.indexOf(postMissingFile)
        int previousIndex = postMissingFileIndex - 1
        if (previousIndex < 0) {
            // There is no previous file
            return false
        }
        FairfaxFile previousFile = checkList.get(previousIndex)

        // Either the previous file was a double wide or this file is a double wide.
        return previousFile.isSameHeightDoubleWidth(postMissingFile) ||
                previousFile.isSameHeightHalfWidth(postMissingFile)
    }

    void checkForManualProcessing() {
        if (processingParameters.rules.contains(ProcessingRule.Manual)) {
            String reason = "Processing will be redirected to for-review."
            SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                    SipProcessingExceptionReasonType.MANUAL_PROCESSING_REQUIRED, null,
                    reason)
            SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
            processingParameters.sipProcessingState.addException(sipProcessingException)
            log.warn(exceptionReason.toString())
        }
    }

    boolean doGenerateThumbnailPage() {
        boolean doGenerate = false
        if (processingParameters.options.contains(ProcessingOption.GenerateProcessedPdfThumbnailsPage)) {
            boolean alwaysGenerate = processingParameters.options.contains(ProcessingOption.AlwaysGenerateThumbnailPage)
            boolean skipWhenErrorFree = processingParameters.options.contains(ProcessingOption.SkipThumbnailPageGenerationWhenNoErrors)
            doGenerate = alwaysGenerate ||
                    (skipWhenErrorFree && processingParameters.sipProcessingState.hasExceptions())
        }
        return doGenerate
    }

    void generateThumbnailPage(List<FairfaxFile> fairfaxPdfFiles) {
        if (doGenerateThumbnailPage()) {
            if (!fairfaxPdfFiles.isEmpty()) {
                String processingDifferentiator = processingParameters.processingDifferentiator()
                String thumbnailPagePrefix = "${processingDifferentiator}_thumbnail_page"
                String thumbnailPageTitle = "${processingDifferentiator}_thumbnail_page.jpeg"
                processingParameters.thumbnailPageFileFinalName = thumbnailPageTitle
                File thumbnailPageFile = File.createTempFile("${thumbnailPagePrefix}_", ".jpeg")
                // We want this temporary file to be deleted on exit. It will be copied to its final destination.
                thumbnailPageFile.deleteOnExit()
                ThumbnailParameters thumbnailParameters = new ThumbnailParameters(thumbnailHeight: 240,
                        useAffineTransformation: false, textJustification: ThumbnailParameters.TextJustification.RIGHT,
                        maximumPageWidth: 1200, pageTitleText: thumbnailPageTitle,
                        pageTitleFontJustification: ThumbnailParameters.TextJustification.RIGHT)
                boolean useCommandLine = processingParameters.options.contains(ProcessingOption.UseCommandLinePdfToThumbnailGeneration)
                thumbnailParameters.generateWithPdftoppm = useCommandLine

                List<Path> pdfFiles = fairfaxPdfFiles.collect { FairfaxFile sortedFile ->
                    sortedFile.file
                }
                processingParameters.thumbnailPageFile = thumbnailPageFile.toPath()
                log.info("Generating thumbnail page file=${thumbnailPageTitle} (created as temp file=${thumbnailPageFile.getCanonicalPath()})   ")
                ThreadedThumbnailGenerator.writeThumbnailPage(pdfFiles, thumbnailParameters, thumbnailPageFile.toPath())
            }
        }
    }
}
