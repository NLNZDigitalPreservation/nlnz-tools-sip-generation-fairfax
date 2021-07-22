@if "%DEBUG%" == "" @echo off

set sourceFolder="C:/Users/leefr/VM_SHARED/testdata/fairfax"
set targetBaseFolder="C:/Users/leefr/VM_SHARED/testdata/NDHA_submission_Rosetta/fairfax-processing"
set targetPreProcessingFolder="%targetBaseFolder%/pre-processing_Jun_2021"
set forReviewFolder="%targetBaseFolder%/for-review_Jun_2021"

set startingDate="2021-06-01"
set endingDate="2021-06-30"

# Note that the number of threads increases processing speed due to ODS poor single-thread performance
set numberOfThreads=4

set minMemory="4G"
set maxMemory="8G"

java -Xms%minMemory% -Xmx%maxMemory% \
    -jar ../fat/build/libs/sip-generation-fairfax-fat-all-1.0.2-SNAPSHOT.jar \
    --preProcess \
    --startingDate="%startingDate%" \
    --endingDate="%endingDate%" \
    --sourceFolder="%sourceFolder%" \
    --targetPreProcessingFolder="%targetPreProcessingFolder%" \
    --forReviewFolder="%forReviewFolder%" \
    --createDestination \
    --parallelizeProcessing \
    --numberOfThreads %numberOfThreads%



