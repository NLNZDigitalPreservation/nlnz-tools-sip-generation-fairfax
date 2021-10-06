#!/bin/sh

export sourceFolder="/media/sf_Y_DRIVE/ndha/pre-deposit_prod/NDHA_submission_Rosetta/fairfax-processing/pre-processing_Sep_2021"
export targetBaseFolder="/media/sf_Y_DRIVE/ndha/pre-deposit_prod/NDHA_submission_Rosetta/fairfax-processing/latest-batch-ready-ingestion"
export targetForIngestionFolder="${targetBaseFolder}/for-ingestion"
export forReviewFolder="${targetBaseFolder}/for-review"

export startingDate="2021-09-01"
export endingDate="2021-09-31"

export forIngestionProcessingTypes="parent_grouping,parent_grouping_with_edition,supplement_grouping,create_sip_for_folder"
export forIngestionProcessingOptions="use_command_line_pdf_to_thumbnail_generation"


# Note that the number of threads increases processing speed due to ODS poor single-thread performance
export numberOfThreads=4
export maximumThumbnailPageThreads=2
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
