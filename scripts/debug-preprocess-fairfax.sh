#!/bin/sh

export sourceFolder="$HOME/workspace/testdata/fairfax"
export targetBaseFolder="$HOME/workspace/testdata/fairfax-processing"
export targetPreProcessingFolder="${targetBaseFolder}/pre-processing_Aug_2021"
export forReviewFolder="${targetBaseFolder}/for-review_Aug_2021"

export startingDate="2021-08-26"
export endingDate="2021-08-26"

# Note that the number of threads increases processing speed due to ODS poor single-thread performance
export numberOfThreads=4

export minMemory="4G"
export maxMemory="8G"

java -Xms${minMemory} -Xmx${maxMemory} \
    -jar ../fat/build/libs/sip-generation-fairfax-fat-all-1.0.3-SNAPSHOT.jar \
    --preProcess \
    --startingDate="${startingDate}" \
    --endingDate="${endingDate}" \
    --sourceFolder="${sourceFolder}" \
    --targetPreProcessingFolder="${targetPreProcessingFolder}" \
    --forReviewFolder="${forReviewFolder}" \
    --createDestination \
    --parallelizeProcessing \
    --numberOfThreads ${numberOfThreads}
