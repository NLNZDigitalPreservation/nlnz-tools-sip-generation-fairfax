#!/bin/sh

export sourceFolder="C:/Users/leefr/VM_SHARED/testdata/fairfax"
export targetBaseFolder="C:/Users/leefr/VM_SHARED/testdata/NDHA_submission_Rosetta/fairfax-processing"
export targetPreProcessingFolder="${targetBaseFolder}/pre-processing_Jun_2021"
export forReviewFolder="${targetBaseFolder}/for-review_Jun_2021"

export startingDate="2021-06-01"
export endingDate="2021-06-30"

# Note that the number of threads increases processing speed due to ODS poor single-thread performance
export numberOfThreads=4

export minMemory="4G"
export maxMemory="8G"

java -Xms${minMemory} -Xmx${maxMemory} \
    -jar ../fat/build/libs/sip-generation-fairfax-fat-all-1.0.2-SNAPSHOT.jar \
    --preProcess \
    --startingDate="${startingDate}" \
    --endingDate="${endingDate}" \
    --sourceFolder="${sourceFolder}" \
    --targetPreProcessingFolder="${targetPreProcessingFolder}" \
    --forReviewFolder="${forReviewFolder}" \
    --createDestination \
    --parallelizeProcessing \
    --numberOfThreads ${numberOfThreads}
