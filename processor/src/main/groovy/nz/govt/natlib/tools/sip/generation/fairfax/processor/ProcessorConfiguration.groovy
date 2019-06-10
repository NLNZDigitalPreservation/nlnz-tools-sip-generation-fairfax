package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.logging.Timekeeper

import java.time.LocalDate

@Log4j2
trait ProcessorConfiguration {
    final static LocalDate DEFAULT_STARTING_DATE = LocalDate.of(2015, 1, 1)
    final static LocalDate DEFAULT_ENDING_DATE = LocalDate.now()

    boolean preProcess
    boolean readyForIngestion
    boolean listFiles

    boolean statisticalAudit
    boolean extractMetadata
    boolean copyIngestedLoadsToIngestedFolder

    boolean copyProdLoadToTestStructures

    boolean moveFiles
    boolean createDestination

    boolean parallelizeProcessing
    int numberOfThreads

    boolean moveOrCopyEvenIfNoRosettaDoneFile

    boolean includeDetailedTimings

    boolean verbose

    boolean helpRequested

    Timekeeper timekeeper

    LocalDate startingDate = DEFAULT_STARTING_DATE
    LocalDate endingDate = DEFAULT_ENDING_DATE

    File sourceFolder
    File targetFolder
    File targetPreProcessingFolder
    File targetForIngestionFolder
    File targetPostProcessedFolder
    File forReviewFolder

    String forIngestionProcessingTypes
    String forIngestionProcessingRules
    String forIngestionProcessingOptions

    List<ProcessorOption> processorOptions
}
