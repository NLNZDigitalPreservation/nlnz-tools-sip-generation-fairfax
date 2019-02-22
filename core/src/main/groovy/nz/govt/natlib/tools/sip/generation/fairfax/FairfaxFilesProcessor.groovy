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

@Slf4j
class FairfaxFilesProcessor {

    static String processCollectedFiles(SipProcessingState sipProcessingState, FairfaxSpreadsheet fairfaxSpreadsheet,
                                        List<File> filesForProcessing) {
        String sipAsXml
        log.info("STARTING processFiles")

        Map<FairfaxFileGroupKey, FairfaxFileGroup> fairfaxFileGroupMap = [ : ]
        filesForProcessing.each { File rawFile ->
            FairfaxFile fairfaxFile = new FairfaxFile(rawFile)
            log.info("Processing fairfaxFile=${fairfaxFile}")
            if (fairfaxFile.isValid()) {
                FairfaxFileGroupKey fairfaxFileKey = FairfaxFileGroupKey.fromFairfaxFile(fairfaxFile)
                log.info("fairfaxFileKey=${fairfaxFileKey}")
                FairfaxFileGroup fairfaxFileGroup = fairfaxFileGroupMap.get(fairfaxFileKey)
                if (fairfaxFileGroup == null) {
                    fairfaxFileGroup = new FairfaxFileGroup(fairfaxFileKey)
                    fairfaxFileGroupMap.put(fairfaxFileKey, fairfaxFileGroup)
                }
                if (fairfaxFile.file.length() == 0) {
                    SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                            SipProcessingExceptionReasonType.FILE_OF_LENGTH_ZERO, null,
                            fairfaxFile.file.getCanonicalPath())
                    sipProcessingState.addException(SipProcessingException.createWithReason(exceptionReason))
                    fairfaxFile.validPdf = false
                } else {
                    // We use the Jhove validator as it is the same type used by Rosetta.
                    // There is also a PDF/A validator using the PdfValidatorType.PDF_BOX_VALIDATOR
                    PdfValidator pdfValidator = PdfValidatorFactory.getValidator(PdfValidatorType.JHOVE_VALIDATOR)
                    SipProcessingException sipProcessingException = pdfValidator.validatePdf(rawFile.toPath())
                    if (sipProcessingException != null) {
                        sipProcessingState.addException(sipProcessingException)
                        fairfaxFile.validPdf = false
                    }
                }
                fairfaxFileGroup.addFile(fairfaxFile)
            } else {
                SipProcessingExceptionReason exceptionReason = new SipProcessingExceptionReason(
                        SipProcessingExceptionReasonType.INVALID_PAGE_FILENAME, null,
                        fairfaxFile.file.getCanonicalPath())
                SipProcessingException sipProcessingException = SipProcessingException.createWithReason(exceptionReason)
                sipProcessingState.addException(sipProcessingException)
                log.warn(sipProcessingException.toString())
            }
        }
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
                List<FairfaxFile> fairfaxFiles = fairfaxFileGroup.files.sort()
                List<Sip.FileWrapper> fileWrappers = fairfaxFiles.collect() { FairfaxFile fairfaxFile ->
                    SipFileWrapperFactory.generate(fairfaxFile.file, true, true)
                }
                int sequenceNumber = 1
                fileWrappers.each { Sip.FileWrapper fileWrapper ->
                    String label = String.format("%03d", sequenceNumber)
                    fileWrapper.label = label
                    sequenceNumber += 1
                    filesProcessed += 1
                }
                Sip testSip = fairfaxFileGroupMatch.sip.clone()
                testSip.fileWrappers = fileWrappers
                sipProcessingState.setComplete(true)
                log.info("\n* * * SipProcessingState:")
                log.info(sipProcessingState.toString())
                SipXmlGenerator sipXmlGenerator = new SipXmlGenerator(testSip)
                sipAsXml = sipXmlGenerator.getSipAsXml()
                log.info("\n* * *   S I P   * * *")
                log.info(sipAsXml)
                log.info("\n* * *   E N D   O F   S I P   * * *")
            } else {
                sipProcessingState.setComplete(true)
                // We can't process the files
                throw new RuntimeException("Unable to process fairfaxFileGroup=${fairfaxFileGroup}: No matching sip")
            }
            log.info("ENDING processing")
        }
        sipProcessingState.totalFilesProcessed = filesProcessed

        return sipAsXml
    }

}
