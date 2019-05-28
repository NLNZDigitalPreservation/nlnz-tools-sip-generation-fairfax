# National Library of New Zealand Tools SIP Generation for Fairfax files

Tools for building Fairfax-based SIPs for ingestion into the Rosetta archive system as well as command-line tools for
performing that processing.

## Synopsis

This repository contains classes used to build Fairfax-based SIPs for ingestion into the Rosetta archive system.

## Motivation

We want to automate the generation of Fairfax SIPs from Fairfax files.

## Important

### Relationships with other scripting code
Some of this scripting code is related to the codebase *nlnz-tools-scripts-ingestion* found in the github
repository: https://github.com/NLNZDigitalPreservation/nlnz-tools-scripts-ingestion and there is an expectation
that the two codebases will work together.

### File copying
File copies are done in 2 steps:
- The file is copied to its new target with a file extension of `.tmpcopy`.
- The file is renamed to the target name.

This means that the target does not have its correct name until the copy is complete. Subsequent runs on the same source
do checks to see if the target's MD5 hash is the same. If the hash is the same, the copy is not done.

### Atomic file moves
Some processing has a `--moveFiles` option. Note that when moving files *across* file systems (in other words, from
one file system to another), it's not possible to have truly atomic operations. If the move operation is interrupted
before it completes, what can happen is that a file of the same name will exist on both filesystems, with the target
file system having an incomplete file.

With that in mind, file moves have the following characteristics:
- If a file move can be done atomicly (as determined by the Java runtime), it is done atomicly.
- If the file move cannot be done atomicly (as determined by the Java runtime), the file moves take the following steps:
    1. The file is copied across to the target file system with a `.tmpcopy` extension.
    2. The file is renamed to the target file name.
    3. The source file is deleted.

This means that if at any point the operation is interrupted, a recovery can take place. A move when the file already
exists in the target folder will trigger a MD5 hash comparison. If the source file and the target file are identical,
the source file is deleted. Otherwise, the target file is moved across (using the steps above) with a `-DUPLICATE-#`
in the filename. These `-DUPLICATE-#` files need to be checked manually to determine which file is correct.

We hope these mitigations will prevent any data loss.

## Requirements

- A set of Fairfax files for ingestion
- nz.govt.natlib.m11n.tools:automation-core
- The nz.govt.natlib.tools:sip-generation-core/nz.govt.natlib.tools:sip-generation-gradle-plugin

## Processing stages

Files moves through the different processing stages. Each processing stage has its own folder structure.

The stages are as follows:

1. *FTP stage* - this is where files are placed from the file producer.
2. *Pre-processing stage* - Files are moved from the FTP stage in preparation for processing.
3. *Ready-for-ingestion stage* - Files are processed and sips are created. This stage is where files are ingested into
   Rosetta.
4. *Ingested stage* - Files have been ingested into Rosetta.
5. *For-review stage* - Exceptions or issues with the files means that a review is necessary before the files can be
   moved to the correct stage.

### ProcessorRunner processing usage

ProcessorRunner used for all processing. Building this project:
```
gradle clean build
```
will produce a `sip-generation-fairfax-fat-all` jar. This jar has the main class
`nz.govt.natlib.tools.sip.generation.fairfax.processor.ProcessorRunner`. Run this jar using:
```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar
```

The basic parameters used by this runner are as follows (these parameters are discussed more in detail):
```
Runs different processors based on command-line options.

Processing stages:
      --preProcess          Group source files by date and titleCode.
                            Output is used by readyForIngestion.
                            Requires sourceFolder, targetPreProcessingFolder, forReviewFolder.
                            Uses startingDate, endingDate.
                            Optional createDestination, moveFiles.
                            This is a processing operation and must run exclusively of other processing operations.
      --readyForIngestion   Process the source files.
                            Output is ready for ingestion by Rosetta.
                            Requires sourceFolder, targetForIngestionFolder, forReviewFolder.
                            Uses startingDate, endingDate.
                            Optional createDestination, moveFiles.
                            This is a processing operation and must run exclusively of other processing operations.
      --copyIngestedLoadsToIngestedFolder
                            Copy the ingested loads to ingested folder.
                            Requires sourceFolder, targetPostProcessedFolder, forReviewFolder.
                            Uses startingDate, endingDate.
                            Optional createDestination, moveFiles, moveOrCopyEvenIfNoRosettaDoneFile.
                            This is a processing operation and must run exclusively of other processing operations.
      --copyProdLoadToTestStructures
                            Copy the production load to test structures.
                            Requires sourceFolder, targetFolder.
                            Uses startingDate, endingDate.
                            This is a processing operation and must run exclusively of other processing operations.

Reports:
  -l, --listFiles           List the source files in an organized way.
                            Requires sourceFolder.
                            This is a reporting operation and cannot be run with any processing operations.
      --extractMetadata     Extract and list the metadata from the source files.
                            Requires sourceFolder.
                            This is a reporting operation and cannot be run with any processing operations.
      --statisticalAudit    Statistical audit.
                            Search through the source folder and provide a
                              statistical audit of the files found.
                            This is a reporting operation and cannot be run with any processing operations.

Parameters:
  -b, --startingDate=STARTING_DATE
                            Starting date in the format yyyy-MM-dd.
                            Default is 2015-01-01.
  -e, --endingDate=ENDING_DATE
                            Ending date in the format yyyy-MM-dd.
                            Default is today.
  -s, --sourceFolder=SOURCE_FOLDER
                            source folder in the format /path/to/folder
      --targetFolder=TARGET_FOLDER
                            target folder in the format /path/to/folder.
                            This is the destination folder used when no other destination folders are specified.
      --targetForIngestionFolder=TARGET_FOR_INGESTION_FOLDER
                            target for-ingestion folder in the format /path/to/folder
      --targetPostProcessedFolder=TARGET_POST_PROCESSED_FOLDER
                            target post-processed folder in the format /path/to/folder
      --targetPreProcessingFolder=TARGET_PRE_PROCESS_FOLDER
                            target pre-processing folder in the format /path/to/folder
      --numberOfThreads=<numberOfThreads>
                            Number of threads when running operations in parallel.
                            The default is 1.
  -r, --forReviewFolder=FOR_REVIEW_FOLDER
                            for-review folder in the format /path/to/folder

Options:
  -c, --createDestination   Whether destination (or target) folders will be created.
                            Default is no creation (false).
      --moveFiles           Whether files will be moved or copied.
                            Default is copy (false).
      --moveOrCopyEvenIfNoRosettaDoneFile
                            Whether the move or copy takes place even if there is no Rosetta done file.
                            The Rosetta done files is a file with a titleCode of 'done'.
                            Default is no move or copy unless there IS a Rosetta done file (false).
      --parallelizeProcessing
                            Run operations in parallel (if possible).
                            Operations that have components that can run in parallel currently are:
                                --preProcess
      --detailedTimings     Include detailed timings (for specific operations).
  -h, --help                Display a help message.
      --verbose               Include verbose output
```

See the *Parameters* section for a discussion of the different parameters used.

While it is possible to multiple processing stages in the same run, note that they use the same `sourceFolder` and
`targetFolder` parameters. The source and target folders have different structures and different purposes, so we
recommend running a single stages at a time.

### PDF filenames

PDF filenames have the following structure:
```
<TitleCode><EditionCode>-yyyyMMdd-<optional-sequence-letter><optional-sequence-number><optional-qualifier>.pdf
```

For example, `SHMED1-20181108-011.pdf` and `WHMED1-20181108-G012new-page.pdf` are valid filenames.

### FTP stage

All PDF files are placed in a single FTP folder by the file producer. There are no subfolders.

### Pre-processing stage

The first stage of processing where files are taken from the FTP folder and are separated out by date and titleCode.
Note that the `--moveFiles` option is not included in the example.
```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
    --preProcess \
    --startingDate="yyyy-MM-dd" \
    --endingDate="yyyy-MM-dd" \
    --sourceFolder="/path/to/source/folder" \
    --targetFolder="/path/to/target/folder" \
    --forReviewFolder="/path/to/for-review/folder" \
    --createDestination
```

Files are moved from the FTP stage folder into a folder structure that prepares them for processing. The folder
structure for *Pre-processing* output is as follows:
```
<targetPreProcessingFolder>/<date-in-yyyyMMdd>/<TitleCode>/{files for that titleCode and date}
```

Note that the script `fairfax-pre-and-post-process-grouper.py` is generally used for taking the files from the ftp
folder to the pre-processing stage. This script is found in the github repository
https://github.com/NLNZDigitalPreservation/nlnz-tools-scripts-ingestion and the expectation that the two mechanisms
(this processor and the Python script) will operate in the same way. Currently the Python script is the one being used
to pre-process Fairfax files.

### Ready-for-ingestion stage

The second state of processing where files are aggregated into specific SIPs ready for ingestion into Rosetta. Note that
the `--moveFiles` option is not included in the example.
```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
    --readyForIngestion \
    --startingDate="yyyy-MM-dd" \
    --endingDate="yyyy-MM-dd" \
    --sourceFolder="/path/to/source/folder" \
    --targetFolder="/path/to/target/folder" \
    --forReviewFolder="/path/to/for-review/folder" \
    --createDestination
```

Files are processed and prepared for ingestion. The *Ready-for-ingestion* folder structure is how Rosetta ingests the
files. Magazines and newspapers have different Material Flows, so ingestion of those different IEEntity types must
be in different folders.

Note that Rosetta ingestion requires that the `content` folder's parent parent be the folder used in Rosetta's
Submission Format. In this case that folder is either `magazines` or `newspapers`, with the folder for an individual
publication's ingestion directly underneath:
```
<targetFolder>/<magazines|newspapers>/<date-in-yyyyMMdd>_<TitleCode><EditionCode>_<full-name-of-publication>/content/streams/{files for that titleCode/editionCode}
```

Notes:
- That the `mets.xml` file is placed in the `content` folder.
Note that magazines and newspapers have different material flows, so they are processed from different root folders
under the target folder.

### Ingested stage

Once files have been ingested into Rosetta, a file with the name of `done` is placed in the root folder (in this case,
that folder is `<TitleCode><EditionCode>_<full-name-of-publication>`). This means that folder can be moved to the
*Ingested* folder. Note that the `--moveFiles` option is not included in the example. Note also that the `done` file
must exist, otherwise `--moveOrCopyEvenIfNoRosettaDoneFile` needs to be specified.
```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
    --copyIngestedLoadsToIngestedFolder \
    --startingDate="yyyy-MM-dd" \
    --endingDate="yyyy-MM-dd" \
    --sourceFolder="/path/to/source/folder" \
    --targetFolder="/path/to/target/folder" \
    --forReviewFolder="/path/to/for-review/folder" \
    --createDestination
```

The folder structure for the ingested (post-processed) stage is as follows:
```
<targetFolder>/<magazines|newspapers>/<TitleCode>/<yyyy>/<date-in-yyyyMMdd-format>
```
In this dated folder, the file structure matches the same structure that was ingested into Rosetta, namely:
```
<date-in-yyyyMMdd-format>
   |- done
   |- content/
           |- mets.xml
           |- streams/
                   |- <pdf-files>
```
Note that the `mets.xml` file is placed in the `content` folder. The `done` files is in the root `yyyyMMdd` folder.

### For-review stage

If a file or set of files is unable to be processed for some reason, it will be placed in the *For-review* folder. There
is no processor that operates on the *For-review* stage. Processors that output to the *For-review* folder use the
parameter `forReviewFolder` to set the location of the *For-review* folder.

If the files come from the FTP folder and the *TitleCode* and date are identifiable from the filename, the files are in
the following structure:
```
<forReviewFolder>/<date-in-yyyyMMMdd>/<TitleCode>/{files}
```
If the files come from the FTP folder and the *TitleCode* is not identifiable from the filename (but the date is), the
files are in the following structure:
```
<forReviewFolder>/UNKNOWN-TITLE-CODE/<date-in-yyyyMMdd>/{files-that-have-no-title-code-mapping-for-that-date}
```

If the files come from the FTP folder and the *TitleCode* and date are not identifiable from the filename, the files are
in the following structure:
```
<forReviewFolder>/UNKNOWN-TITLE-CODE/UNKNOWN-DATE/{files-that-have-no-title-code-mapping-for-that-date}
```

If the files come from the *Pre-processing* stage but cannot be processed into the *Ready-for-ingestion* stage because
the files are not recognized, then they're placed in the following structure:
```
<forReviewFolder>/UNRECOGNIZED/<date-in-yyyyMMdd>/<TitleCode>/{files for that titleCode}
```

If the files come from the *Pre-processing* stage but cannot be processed into the *Ready-for-ingestion* stage because
of some error in processing, then they're placed in the following structure:
```
<forReviewFolder>/<unknown|newspaper|magazine><date-in-yyyyMMdd>/<TitleCode><EditionCode>_<full-name-of-publication>/content/streams/{files for that titleCode/editionCode}
```

If the files come from the *Ready-for-ingestion* stage but are not ingested into Rosetta properly, then they're placed in the
following structure:
```
<forReviewFolder>/<date-in-yyyyMMdd>/<TitleCode><EditionCode>_<full-name-of-publication>/content/streams/{files for that titleCode/editionCode}
```

### Other stages (reporting, diagnosis and testing)

#### listFiles: list files based on source folder
`listFiles` simply lists files by titleCode, editionCode and date:
```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
    --listFiles \
    --startingDate="yyyy-MM-dd" \
    --endingDate="yyyy-MM-dd" \
    --sourceFolder="/path/to/source/folder"
```

#### extractMetadata: extract metadata from the pdf files based on source folder
```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
    --extractMetadata \
    --startingDate="yyyy-MM-dd" \
    --endingDate="yyyy-MM-dd" \
    --sourceFolder="/path/to/source/folder"
```

#### copyProdLoadToTestStructures: Copy production load files
Copies files from previous production loads into Rosetta into Pre-processing *and* Ready-for-ingestion structures
for testing. The structures are as follows:
1. preProcess structure. This is to mimic the input to readyForIngestion processing. The folder structures are the
   same as the output to `preProcess`, with the folder structure starting with `<targetFolder>/preProcess`.
2. readyForIngestion structure. This is the structure that gets ingested into Rosetta. The folder structures are the
   same as the output to `readyForIngestion`, with the folder structure starting with `<targetFolder>/readyForIngestion`.

These structures provide for testing the Fairfax processor, to see if its outputs match the work done previously.

```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
    --copyProdLoadToTestStructures \
    --startingDate="yyyy-MM-dd" \
    --endingDate="yyyy-MM-dd" \
    --sourceFolder="/path/to/source/folder" \
    --targetFolder="/path/to/target/folder" \
    --createDestination
```

### Parameters

Parameters and their usage. See the `--help` output shown previously if the parameter does not appear here.

#### startingDate
The starting date for file processing (inclusive). Files before this date are ignored. Note that the `startingDate` is
based on the file name, and not the time stamp of the file. Files usually have the format:
```
<TitleCode><EditionCode>-yyyyMMdd-<optional-sequence-letter><optional-sequence-number>
```

The format of the starting date `yyyy-MM-dd`.

#### endingDate
The ending date for file processing (inclusive). Files after this date are ignored. Note that the `endingDate` is based
on the file name, and not the time stamp of the file. The format of the ending date is `yyyy-MM-dd`.

#### sourceFolder
The source folder for various types of processing. This folder must exist and must be a directory.

#### targetFolder
The target, or destination, folder for various types of processing. If this folder does not exist then
`--createDestination` must be set to force its creation. The way this folder gets populated depends on the type of
 processing.

#### forReviewFolder
The folder where files that did not process correctly (had errors or exceptions). If this folder does not exist then
`--createDestination` must be set to force its creation. The way this folder gets populated depends on the type of
processing.

#### moveFiles
Whether files are moved or copied to the target folder when processed. The default is `false` if this option is not
specified.

#### createDestination
Whether the target folder is created if it does not exist. The default is `false` if this option is not specified.

#### moveOrCopyEvenIfNoRosettaDoneFile
Whether the move or copy takes place even if there is no Rosetta done file. The Rosetta done files is a file with a name
of 'done'. Default is no move or copy unless there IS a Rosetta done file (false).


## Updating the default fairfax import parameters JSON file
From time to time the spreadsheet that defines how the Fairfax files are ingested will changed based on new information.
When this happens, the json file found at `core/src/main/resources/default-fairfax-import-parameters.json` needs
updating to reflect the changes in the source spreadsheet.

### Export original spreadsheet in csv format
First, export the original spreadsheet in `.csv` format with the file separator as `|` and save it.

### Copy exported spreadsheet to default-fairfax-import-spreadsheet.csv
Copy the exported csv spreadsheet to:
`core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-spreadsheet.csv`.

### Execute the build task updateDefaultFairfaxImportParameters
Execute the gradle task `updateDefaultFairfaxImportParameters`, which takes the csv spreadsheet and converts it to a
JSON file, which is then used for the actual processing:
```
gradle updateDefaultFairfaxImportParameters \
  -PfairfaxSpreadsheetImportFilename="core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-spreadsheet.csv" \
  -PfairfaxSpreadsheetExportFilename="core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-parameters.json"
```

### Execute the build task exportDefaultFairfaxImportParameters
Execute the gradle task `exportDefaultFairfaxImportParameters`, which takes a JSON file and converts it to a
csv spreadsheet file, which makes it easier for user editing:
```
gradle exportDefaultFairfaxImportParameters \
  -PfairfaxSpreadsheetImportFilename="core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-parameters.json" \
  -PfairfaxSpreadsheetExportFilename="core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-spreadsheet.csv"
```

The edited spreadsheet can be loaded back into json using the build task `updateDefaultFairfaxImportParameters`.

### Check in the changes and build a new version of the jar
Changes should then be checked in and a new version of this jar built, which will have the new JSON processing resource
file.

## Contributors

See git commits to see who contributors are. Issues are tracked through the git repository issue tracker.

## License

&copy; 2018 &mdash; 2019 National Library of New Zealand. All rights reserved. MIT license.
