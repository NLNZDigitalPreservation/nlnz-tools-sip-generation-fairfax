package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.util.logging.Slf4j
import nz.govt.natlib.m11n.tools.automation.logging.Timekeeper
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option

import java.time.LocalDate
import java.util.concurrent.Callable

@Slf4j
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

    String forIngestionProcessingType
}
