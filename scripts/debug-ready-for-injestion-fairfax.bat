java -Xms4G -Xmx8G  -jar ../fat/build/libs/sip-generation-fairfax-fat-all-1.0.3-SNAPSHOT.jar  --readyForIngestion  --startingDate="2021-06-01" --endingDate="2021-06-30" --sourceFolder="C:/Users/leefr/VM_SHARED/testdata/NDHA_submission_Rosetta/fairfax-processing/pre-processing_Jun_2021" --targetForIngestionFolder="C:/Users/leefr/VM_SHARED/testdata/NDHA_submission_Rosetta/fairfax-processing/latest-batch-ready-ingestion/for-ingestion" --forReviewFolder="C:/Users/leefr/VM_SHARED/testdata/NDHA_submission_Rosetta/fairfax-processing/latest-batch-ready-ingestion/for-review" --createDestination --parallelizeProcessing --numberOfThreads=1 --maximumThumbnailPageThreads=1 --forIngestionProcessingTypes="parent_grouping,parent_grouping_with_edition,supplement_grouping,create_sip_for_folder" --forIngestionProcessingRules="" --forIngestionProcessingOptions="use_command_line_pdf_to_thumbnail_generation"