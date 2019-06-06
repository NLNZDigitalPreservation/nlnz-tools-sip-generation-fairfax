package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.IEEntityType
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.SipFileWrapperFactory
import nz.govt.natlib.tools.sip.generation.SipXmlGenerator
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxProcessingParameters
import nz.govt.natlib.tools.sip.generation.fairfax.SipFactory
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingOption
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingRule
import nz.govt.natlib.tools.sip.generation.fairfax.parameters.ProcessingType
import nz.govt.natlib.tools.sip.generation.fairfax.processor.type.ParentGroupingProcessor
import nz.govt.natlib.tools.sip.generation.fairfax.processor.type.SipForFolderProcessor
import nz.govt.natlib.tools.sip.pdf.PdfValidator
import nz.govt.natlib.tools.sip.pdf.PdfValidatorFactory
import nz.govt.natlib.tools.sip.pdf.PdfValidatorType
import nz.govt.natlib.tools.sip.pdf.thumbnail.ThumbnailGenerator
import nz.govt.natlib.tools.sip.pdf.thumbnail.ThumbnailParameters
import nz.govt.natlib.tools.sip.state.SipProcessingException
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReason
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType

import java.time.LocalDate

@Log4j2
class FairfaxFilesProcessor {
    FairfaxProcessingParameters processingParameters
    List<File> filesForProcessing

    Map<FairfaxFile, FairfaxFile> processedFairfaxFiles

    static Sip getBlankSip() {
        Sip sip = new Sip(title: 'UNKNOWN_TITLE', ieEntityType: IEEntityType.UNKNOWN,
                objectIdentifierType: 'UNKNOWN_OBJECT_IDENTIFIER_TYPE',
                objectIdentifierValue: 'UNKNOWN_OBJECT_IDENTIFIER_VALUE', policyId: 'UNKNOWN_POLICY_ID',
                preservationType: 'UNKNOWN_PRESERVATION_TYPE', usageType: 'UNKNOWN_USAGE_TYPE',
                digitalOriginal: true, revisionNumber: 1,
                year: 2038, month: 12, dayOfMonth: 31)

    }

    static String processCollectedFiles(FairfaxProcessingParameters processingParameters,
                                        List<File> filesForProcessing) {
        FairfaxFilesProcessor fairfaxFilesProcessor = new FairfaxFilesProcessor(processingParameters,
                filesForProcessing)
        return fairfaxFilesProcessor.process()
    }

    FairfaxFilesProcessor(FairfaxProcessingParameters processingParameters, List<File> filesForProcessing) {
        this.processingParameters = processingParameters
        this.filesForProcessing = filesForProcessing
    }

    String process() {
        log.info("STARTING process for processingParameters=${processingParameters}")
        processedFairfaxFiles = [ : ]

        String sipAsXml = ""
        if (this.processingParameters.valid) {
            List<FairfaxFile> fairfaxFilesForProcessing = filesForProcessing.collect { File rawFile ->
                new FairfaxFile(rawFile)
            }
            List<FairfaxFile> validNamedFiles = extractValidNamedFiles(fairfaxFilesForProcessing)

            List<FairfaxFile> sortedFilesForProcessing
            switch (processingParameters.type) {
                case ProcessingType.ParentGrouping:
                    sortedFilesForProcessing = ParentGroupingProcessor.selectAndSort(processingParameters, validNamedFiles)
                    break
                case ProcessingType.CreateSipForFolder:
                    sortedFilesForProcessing = SipForFolderProcessor.selectAndSort(processingParameters, validNamedFiles)
                    break
                default:
                    sortedFilesForProcessing = []
                    break
            }

            if (processingParameters.options.contains(ProcessingOption.GenerateProcessedPdfThumbnailsPage)) {
                if (!sortedFilesForProcessing.isEmpty()) {
                    String readableDate = processingParameters.date.format(FairfaxProcessingParameters.READABLE_DATE_FORMAT)
                    // TODO When we do more sophisticated processing the date-titleCode-type combination may not be enough to easily differentiate
                    String thumbnailPagePrefix = "${readableDate}_${processingParameters.titleCode}_${processingParameters.type.fieldValue}_thumbnail_page"
                    String thumbnailPageTitle = "${readableDate}_${processingParameters.titleCode}_${processingParameters.type.fieldValue}_thumbnail_page.jpeg"
                    processingParameters.thumbnailPageFileFinalName = thumbnailPageTitle
                    File thumbnailPageFile = File.createTempFile("${thumbnailPagePrefix}_", ".jpeg")
                    ThumbnailParameters thumbnailParameters = new ThumbnailParameters(thumbnailHeight: 180,
                            useAffineTransformation: false, textJustification: ThumbnailParameters.TextJustification.RIGHT,
                            maximumPageWidth: 1200, pageTitleText: thumbnailPageTitle)
                    List<File> pdfFiles = sortedFilesForProcessing.collect { FairfaxFile sortedFile ->
                        sortedFile.file
                    }
                    processingParameters.thumbnailPageFile = thumbnailPageFile
                    ThumbnailGenerator.writeThumbnailPage(pdfFiles, thumbnailParameters, thumbnailPageFile)
                }
            }

            processingParameters.sipProcessingState.ignoredFiles =
                    FairfaxFile.differences(validNamedFiles, sortedFilesForProcessing).
                            collect { FairfaxFile fairfaxFile ->
                        fairfaxFile.file
                    }

            if (processingParameters.sipProcessingState.ignoredFiles.size() > 0 &&
                    processingParameters.rules.contains(ProcessingRule.AllSectionsInSipRequired)) {
                // Strip the ignored of any editionDiscriminator files
                List<FairfaxFile> withoutEditionFiles = processingParameters.sipProcessingState.ignoredFiles.findAll {
                    File file ->
                        FairfaxFile fairfaxFile = new FairfaxFile(file)
                        !processingParameters.editionDiscriminators.contains(fairfaxFile.sectionCode)
                }
                if (!withoutEditionFiles.isEmpty()) {
                    String detailedReason = "${ProcessingRule.AllSectionsInSipRequired.fieldValue} but these files are not processed=${withoutEditionFiles}".toString()
                    SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                            SipProcessingExceptionReasonType.ALL_FILES_CANNOT_BE_PROCESSED, null,
                            detailedReason)
                    SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
                    processingParameters.sipProcessingState.addException(sipProcessingException)
                    log.warn(detailedReason)
                }
            }

            List<FairfaxFile> successfulFiles = [ ]
            sortedFilesForProcessing.each { FairfaxFile fileForProcessing ->
                if (processFile(fileForProcessing)) {
                    successfulFiles.add(fileForProcessing)
                }
            }
            checkForMissingSequenceFiles(successfulFiles)

            // The valid files should be filtered and sorted already
            sipAsXml = generateSipAsXml(successfulFiles, processingParameters.date)
        }

        log.info("ENDING process for processingParameters=${processingParameters}")

        return sipAsXml
    }

    List<FairfaxFile> extractValidNamedFiles(List<FairfaxFile> originalList) {
        List<FairfaxFile> cleanedList = [ ]
        originalList.each { FairfaxFile fairfaxFile ->
            if (fairfaxFile.isValidName()) {
                cleanedList.add(fairfaxFile)
            } else {
                SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                        SipProcessingExceptionReasonType.INVALID_PAGE_FILENAME, null,
                        fairfaxFile.file.getCanonicalPath())
                SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
                processingParameters.sipProcessingState.addException(sipProcessingException)
                log.warn(sipProcessingException.toString())
                processingParameters.sipProcessingState.unrecognizedFiles.add(fairfaxFile.file)
            }
        }
        return cleanedList
    }

    boolean processFile(FairfaxFile fairfaxFile) {
        log.info("Processing fairfaxFile=${fairfaxFile}")
        // We generally include all files whether they are valid or invalid. We don't include duplicate files.
        boolean includeFileInSip = true
        if (processedFairfaxFiles.containsKey(fairfaxFile)) {
            // We have a duplicate file -- possibly a different qualifier
            // We use the fairfax file as a key, but we'll get the duplicate back
            FairfaxFile firstVersion = processedFairfaxFiles.get(fairfaxFile)
            SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                    SipProcessingExceptionReasonType.DUPLICATE_FILE, null,
                    firstVersion.file.getCanonicalPath(), fairfaxFile.file.getCanonicalPath())
            processingParameters.sipProcessingState.addException(SipProcessingException.createWithReason(exceptionReason))
            includeFileInSip = false
        } else {
            processedFairfaxFiles.put(fairfaxFile, fairfaxFile)
            if (fairfaxFile.file.length() == 0) {
                SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                        SipProcessingExceptionReasonType.FILE_OF_LENGTH_ZERO, null,
                        fairfaxFile.file.getCanonicalPath())
                processingParameters.sipProcessingState.addException(SipProcessingException.createWithReason(exceptionReason))
            } else {
                // We use the Jhove validator as it is the same type used by Rosetta.
                // There is also a PDF/A validator using the PdfValidatorType.PDF_BOX_VALIDATOR
                PdfValidator pdfValidator = PdfValidatorFactory.getValidator(PdfValidatorType.JHOVE_VALIDATOR)
                SipProcessingException sipProcessingException = pdfValidator.validatePdf(fairfaxFile.file.toPath())
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
            processingParameters.sipProcessingState.invalidFiles.add(fairfaxFile.file)
        }
        return includeFileInSip
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
            Sip sip = SipFactory.fromMap(processingParameters.spreadsheetRow)
            sip.year = sipDate.year
            sip.month = sipDate.monthValue
            sip.dayOfMonth = sipDate.dayOfMonth
            processingParameters.sipProcessingState.ieEntityType = sip.ieEntityType
            processingParameters.sipProcessingState.identifier = formatSipProcessingStateIdentifier()

            List<File> filesForSip = sortedFairfaxFiles.collect() { FairfaxFile fairfaxFile ->
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

    String generateSipAsXml(Sip sip, List<File> files) {
        List<Sip.FileWrapper> fileWrappers = files.collect() { File file ->
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
        String title = processingParameters.getTitleParent()
        String titleWithUnderscores = title.trim().replace(' ', '_')

        if (processingParameters.currentEdition == null) {
            return "_${titleWithUnderscores}"
        } else {
            return "_${processingParameters.currentEdition}_${titleWithUnderscores}"
        }
    }

    void checkForMissingSequenceFiles(List<FairfaxFile> checkList) {
        if (processingParameters.rules.contains(ProcessingRule.MissingSequenceError)) {
            List<FairfaxFile> postMissingSequenceFiles = FairfaxFile.postMissingSequenceFiles(checkList,
                    processingParameters)
            if (postMissingSequenceFiles.size() > 0) {
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
