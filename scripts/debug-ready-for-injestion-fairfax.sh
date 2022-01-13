#!/bin/sh

export sourceFolder="$HOME/workspace/testdata/fairfax-processing/pre-processing_Dec_2021"
export targetBaseFolder="$HOME/workspace/testdata/fairfax-processing/latest-batch-ready-ingestion"
export targetForIngestionFolder="${targetBaseFolder}/for-ingestion"
export forReviewFolder="${targetBaseFolder}/for-review"

export startingDate="2021-12-01"
export endingDate="2021-12-19"

export forIngestionProcessingTypes="parent_grouping,parent_grouping_with_edition,supplement_grouping,create_sip_for_folder"
export forIngestionProcessingOptions="use_command_line_pdf_to_thumbnail_generation"


# Note that the number of threads increases processing speed due to ODS poor single-thread performance
export numberOfThreads=1
export maximumThumbnailPageThreads=1
export minMemory="4G"
export maxMemory="8G"

java -Xms${minMemory} -Xmx${maxMemory} \
    -jar ../fat/build/libs/sip-generation-fairfax-fat-all-1.0.3-SNAPSHOT.jar \
    --readyForIngestion \
    --startingDate="${startingDate}" \
    --endingDate="${endingDate}" \
    --sourceFolder="${sourceFolder}" \
    --targetForIngestionFolder="${targetForIngestionFolder}" \
    --forReviewFolder="${forReviewFolder}" \
    --createDestination \
    --parallelizeProcessing \
    --numberOfThreads=${numberOfThreads} \
    --maximumThumbnailPageThreads=${maximumThumbnailPageThreads} \
    --forIngestionProcessingTypes="${forIngestionProcessingTypes}" \
    --forIngestionProcessingRules="${forIngestionProcessingRules}" \
    --forIngestionProcessingOptions="${forIngestionProcessingOptions}"
