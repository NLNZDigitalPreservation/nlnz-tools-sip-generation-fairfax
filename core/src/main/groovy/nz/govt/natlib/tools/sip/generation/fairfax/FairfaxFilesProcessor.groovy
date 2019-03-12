package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.SipFileWrapperFactory
import nz.govt.natlib.tools.sip.generation.SipXmlGenerator
import nz.govt.natlib.tools.sip.pdf.PdfValidator
import nz.govt.natlib.tools.sip.pdf.PdfValidatorFactory
import nz.govt.natlib.tools.sip.pdf.PdfValidatorType
import nz.govt.natlib.tools.sip.state.SipProcessingException
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReason
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType
import nz.govt.natlib.tools.sip.state.SipProcessingState
import nz.govt.natlib.tools.sip.state.SipProcessingType

@Slf4j
class FairfaxFilesProcessor {
    SipProcessingState sipProcessingState
    FairfaxSpreadsheet fairfaxSpreadsheet
    List<File> filesForProcessing

    Map<FairfaxFileGroupKey, FairfaxFileGroup> fairfaxFileGroupMap
    Map<FairfaxFile, FairfaxFile> processedFairfaxFiles

    static String processCollectedFiles(SipProcessingState sipProcessingState, FairfaxSpreadsheet fairfaxSpreadsheet,
                                        List<File> filesForProcessing) {
        FairfaxFilesProcessor fairfaxFilesProcessor = new FairfaxFilesProcessor(sipProcessingState, fairfaxSpreadsheet,
                filesForProcessing)
        return fairfaxFilesProcessor.process()

    }

    FairfaxFilesProcessor(SipProcessingState sipProcessingState, FairfaxSpreadsheet fairfaxSpreadsheet,
                          List<File> filesForProcessing) {
        this.sipProcessingState = sipProcessingState
        this.fairfaxSpreadsheet = fairfaxSpreadsheet
        this.filesForProcessing = filesForProcessing
    }

    String process() {
        log.info("STARTING process")
        fairfaxFileGroupMap = [ : ]
        processedFairfaxFiles = [ : ]

        filesForProcessing.each { File rawFile ->
            FairfaxFile fairfaxFile = new FairfaxFile(rawFile)
            processFile(fairfaxFile)
        }
        String sipAsXml = generateSipAsXml()
        log.info("ENDING process")

        return sipAsXml
    }

    void processFile(FairfaxFile fairfaxFile) {
        log.info("Processing fairfaxFile=${fairfaxFile}")
        if (fairfaxFile.isValidName()) {
            FairfaxFileGroupKey fairfaxFileKey = FairfaxFileGroupKey.fromFairfaxFile(fairfaxFile)
            log.info("fairfaxFileKey=${fairfaxFileKey}")
            FairfaxFileGroup fairfaxFileGroup = fairfaxFileGroupMap.get(fairfaxFileKey)
            if (fairfaxFileGroup == null) {
                fairfaxFileGroup = new FairfaxFileGroup(fairfaxFileKey)
                fairfaxFileGroupMap.put(fairfaxFileKey, fairfaxFileGroup)
            }
            if (processedFairfaxFiles.containsKey(fairfaxFile)) {
                // We have a duplicate file -- possibly a different qualifier
                // We use the fairfax file as a key, but we'll get the duplicate back
                FairfaxFile firstVersion = processedFairfaxFiles.get(fairfaxFile)
                SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                        SipProcessingExceptionReasonType.DUPLICATE_FILE, null,
                        firstVersion.file.getCanonicalPath(), fairfaxFile.file.getCanonicalPath())
                sipProcessingState.addException(SipProcessingException.createWithReason(exceptionReason))
            } else {
                processedFairfaxFiles.put(fairfaxFile, fairfaxFile)
                if (fairfaxFile.file.length() == 0) {
                    SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                            SipProcessingExceptionReasonType.FILE_OF_LENGTH_ZERO, null,
                            fairfaxFile.file.getCanonicalPath())
                    sipProcessingState.addException(SipProcessingException.createWithReason(exceptionReason))
                } else {
                    // We use the Jhove validator as it is the same type used by Rosetta.
                    // There is also a PDF/A validator using the PdfValidatorType.PDF_BOX_VALIDATOR
                    PdfValidator pdfValidator = PdfValidatorFactory.getValidator(PdfValidatorType.JHOVE_VALIDATOR)
                    SipProcessingException sipProcessingException = pdfValidator.validatePdf(fairfaxFile.file.toPath())
                    if (sipProcessingException != null) {
                        sipProcessingState.addException(sipProcessingException)
                    } else {
                        fairfaxFile.validPdf = true
                        fairfaxFile.validForProcessing = true
                    }
                }
            }
            if (fairfaxFile.validPdf && fairfaxFile.validForProcessing) {
                sipProcessingState.validFiles.add(fairfaxFile.file)
            } else {
                sipProcessingState.invalidFiles.add(fairfaxFile.file)
            }
            fairfaxFileGroup.addFile(fairfaxFile)
        } else {
            SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                    SipProcessingExceptionReasonType.INVALID_PAGE_FILENAME, null,
                    fairfaxFile.file.getCanonicalPath())
            SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
            sipProcessingState.addException(sipProcessingException)
            log.warn(sipProcessingException.toString())
            sipProcessingState.unrecognizedFiles.add(fairfaxFile.file)
        }
    }

    String generateSipAsXml() {
        String sipAsXml
        // Find the publication (ultimately the MMSID) associated with this set of files.
        log.info("FINDING publication associated with the files")
        Integer filesProcessed = 0
        boolean allowZeroRatio = true
        fairfaxFileGroupMap.each { FairfaxFileGroupKey fairfaxFileGroupKey, FairfaxFileGroup fairfaxFileGroup ->
            log.info("Checking fairfaxFileGroupKey=${fairfaxFileGroupKey}, fairfaxFileGroup=${fairfaxFileGroup}")
            FairfaxFileGroupMatcher fairfaxFileGroupMatcher = new FairfaxFileGroupMatcher(sipProcessingState)
            FairfaxFileGroupMatch fairfaxFileGroupMatch = fairfaxFileGroupMatcher.mostLikelyMatch(fairfaxFileGroup,
                    fairfaxSpreadsheet, allowZeroRatio)
            if (fairfaxFileGroupMatch != null) {
                log.info("Will process fairfaxFileGroup=${fairfaxFileGroup} according to sip=${fairfaxFileGroupMatch.sip}")
                sipProcessingState.processingType = getProcessingType(fairfaxFileGroupKey)
                sipProcessingState.identifier = formatSipProcessingStateIdentifier(fairfaxFileGroupKey)
                List<FairfaxFile> fairfaxFiles = fairfaxFileGroup.files.sort()
                List<File> filesForSip = fairfaxFiles.collect() { FairfaxFile fairfaxFile ->
                    fairfaxFile.file
                }
                Sip testSip = fairfaxFileGroupMatch.sip.clone()
                sipAsXml = generateSipAsXml(testSip, filesForSip)
                filesProcessed = filesForSip.size()
            } else {
                String detailedReason = "Unable to process fairfaxFileGroup=${fairfaxFileGroup}: No matching sip definition in Fairfax spreadsheet."
                SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                        SipProcessingExceptionReasonType.NO_MATCHING_SIP_DEFINITION, null,
                        detailedReason)
                SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
                sipProcessingState.addException(sipProcessingException)
                log.warn(detailedReason)
                // Move the files from validFiles to invalidFiles
                List<File> sortedFiles = this.filesForProcessing.sort(false) { File a, b ->
                    a.name <=> b.name
                }
                sortedFiles.each { File file ->
                    filesProcessed += 1
                    sipProcessingState.validFiles.remove(file)
                    sipProcessingState.invalidFiles.add(file)
                }
                sipAsXml = generateSipAsXml(getBlankSip(), sortedFiles)
            }
            sipProcessingState.setComplete(true)
            log.info("\n* * * SipProcessingState:")
            log.info(sipProcessingState.toString())
            log.info("\n* * *   S I P   * * *")
            log.info(sipAsXml)
            log.info("\n* * *   E N D   O F   S I P   * * *")
        }
        sipProcessingState.totalFilesProcessed = filesProcessed

        return sipAsXml
    }

    String generateSipAsXml(Sip sip, List<File> files) {
        List<Sip.FileWrapper> fileWrappers = files.collect() { File file ->
            SipFileWrapperFactory.generate(file, true, true)
        }
        int sequenceNumber = 1
        fileWrappers.each { Sip.FileWrapper fileWrapper ->
            String label = String.format("%03d", sequenceNumber)
            fileWrapper.label = label
            sequenceNumber += 1
        }
        sip.fileWrappers = fileWrappers
        SipXmlGenerator sipXmlGenerator = new SipXmlGenerator(sip)

        return sipXmlGenerator.getSipAsXml()
    }

    String formatSipProcessingStateIdentifier(FairfaxFileGroupKey fairfaxFileGroupKey) {
        String title = fairfaxSpreadsheet.getTitleForNameEdition(fairfaxFileGroupKey.name, fairfaxFileGroupKey.edition)
        String titleWithUnderscores = title.trim().replace(' ', '_')

        return "${fairfaxFileGroupKey.edition}_${titleWithUnderscores}"
    }

    SipProcessingType getProcessingType(FairfaxFileGroupKey fairfaxFileGroupKey) {
        boolean isMagazine = fairfaxSpreadsheet.isMagazineForNameEdition(fairfaxFileGroupKey.name, fairfaxFileGroupKey.edition)

        return isMagazine ? SipProcessingType.MAGAZINE : SipProcessingType.NEWSPAPER
    }

    Sip getBlankSip() {
        Sip sip = new Sip(title: 'UNKNOWN_TITLE', ieEntityType: 'UNKNOWN_ENTITY_TYPE',
                objectIdentifierType: 'UNKNOWN_OBJECT_IDENTIFIER_TYPE',
                objectIdentifierValue: 'UNKNOWN_OBJECT_IDENTIFIER_VALUE', policyId: 'UNKNOWN_POLICY_ID',
                preservationType: 'UNKNOWN_PRESERVATION_TYPE', usageType: 'UNKNOWN_USAGE_TYPE',
                digitalOriginal: true, revisionNumber: 1,
                year: 2038, month: 12, dayOfMonth: 31)

    }
}
