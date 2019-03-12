# National Library of New Zealand Tools SIP Generation for Fairfax files

Tools for building Fairfax-based SIPs for ingestion into the Rosetta archive system as well as command-line tools for
performing that processing.

## Synopsis

This repository contains classes used to build Fairfax-based SIPs for ingestion into the Rosetta archive system.

## Motivation

We want to automate the generation of Fairfax SIPs from Fairfax files.

## Important

At this time there is no important information to impart.

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
Usage: processorRunner [-cdghilmnpx] [-b=STARTING_DATE] [-e=ENDING_DATE]
                     [-r=FOR_REVIEW_FOLDER] [-s=SOURCE_FOLDER]
                     [-t=TARGET_FOLDER]
Runs different processors based on command-line options.

Processing stages:
    -g, --preProcess          Group source files by date and name.
                              Output is used by readyForIngestion.
                              Requires sourceFolder, targetFolder, forReviewFolder.
                              Uses startingDate, endingDate.
                              Optional createDestination, moveFiles.
    -d, --readyForIngestion   Process the source files.
                              Output is ready for ingestion by Rosetta.
                              Requires sourceFolder, targetFolder, forReviewFolder.
                              Uses startingDate, endingDate.
                              Optional createDestination, moveFiles.
    -i, --copyIngestedLoadsToIngestedFolder
                              Copy the ingested loads to ingested folder.
                              Requires sourceFolder, targetFolder, forReviewFolder.
                              Uses startingDate, endingDate.
                              Optional createDestination, moveFiles,
                                moveOrCopyEvenIfNoRosettaDoneFile
    -l, --listFiles           List the source files in an organized way.
                              Requires sourceFolder
    -x, --extractMetadata     Extract and list the metadata from the source files.
                              Requires sourceFolder
    -p, --copyProdLoadToTestStructures
                              Copy the production load to test structures.
                              Requires sourceFolder, targetFolder.
                              Uses startingDate, endingDate

Parameters:
    -b, --startingDate=STARTING_DATE
                              Starting date in the format yyyy-MM-dd.
                              Default is 2015-01-01.
    -e, --endingDate=ENDING_DATE
                              Ending date in the format yyyy-MM-dd.
                              Default is today.
    -s, --sourceFolder=SOURCE_FOLDER
                              source folder in the format /path/to/folder
    -t, --targetFolder=TARGET_FOLDER
                              target folder in the format /path/to/folder
    -r, --forReviewFolder=FOR_REVIEW_FOLDER
                              for-review folder in the format /path/to/folder

Options:
    -c, --createDestination   Whether destination (or target) folders will be created.
                              Default is no creation (false).
    -m, --moveFiles           Whether files will be moved or copied.
                              Default is copy (false).
    -n, --moveOrCopyEvenIfNoRosettaDoneFile
                              Whether the move or copy takes place even if there is no
                                Rosetta done file.
                              The Rosetta done files is a file with a name of 'done'.
                              Default is no move or copy unless there IS a Rosetta
                                done file (false).
    -h, --help                Display a help message.
```

See the *Parameters* section for a discussion of the different parameters used.

While it is possible to multiple processing stages in the same run, note that they use the same `sourceFolder` and
`targetFolder` parameters. The source and target folders have different structures and different purposes, so we
recommend running a single stages at a time.

### PDF filenames

PDF filenames have the following structure:
```
<Name><Edition>-yyyyMMdd-<optional-sequence-letter><optional-sequence-number><optional-qualifier>.pdf
```

For example, `SHMED1-20181108-011.pdf` and `WHMED1-20181108-G012new-page.pdf` are valid filenames.

### FTP stage

All PDF files are placed in a single FTP folder by the file producer. There are no subfolders.

### Pre-processing stage

The first stage of processing where files are taken from the FTP folder and are separated out by date and name. Note
that the `--moveFiles` option is not included in the example.
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
<targetFolder>/<date-in-yyyyMMMdd>/<Name>/{files for that name and date}
```

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
files:
```
<targetFolder>/<date-in-yyyyMMdd>/<Name><Edition>_<full-name-of-publication>/content/streams/{files for that name/edition}
```

Note that the `mets.xml` file is placed in the `content` folder.

### Ingested stage

Once files have been ingested into Rosetta, a file with the name of `done` is placed in the root folder (in this case,
that folder is `<Name><Edition>_<full-name-of-publication>`). This means that folder can be moved to the *Ingested*
folder. Note that the `--moveFiles` option is not included in the example. Note also that the `done` file must exist,
otherwise `--moveOrCopyEvenIfNoRosettaDoneFile` needs to be specified.
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

The folder structure for the ingested stage is as follows:
```
<targetFolder>/<full-name-of-publication>/<date-in-yyyyMMdd>/<Name><Edition>/content/streams/{files for that name/edition}
```
Note that the `mets.xml` file is placed in the `content` folder. The `done` files is in the `<Name><Edition>` folder.

### For-review stage

If a file or set of files is unable to be processed for some reason, it will be placed in the *For-review* folder. There
is no processor that operates on the *For-review* stage. Processors that output to the *For-review* folder use the
parameter `forReviewFolder` to set the location of the *For-review* folder.

If the files come from the FTP folder and the *Name* and date are identifiable from the filename, the files are in the
following structure:
```
<forReviewFolder>/<date-in-yyyyMMMdd>/<Name>/{files}
```
If the files come from the FTP folder and the *Name* is not identifiable from the filename (but the date is), the files
are in the following structure:
```
<forReviewFolder>/UNKNOWN-NAME/<date-in-yyyyMMdd>/{files-that-have-no-name-mapping-for-that-date}
```

If the files come from the FTP folder and the *Name* and date are not identifiable from the filename, the files are in
the following structure:
```
<forReviewFolder>/UNKNOWN-NAME/UNKNOWN-DATE/{files-that-have-no-name-mapping-for-that-date}
```

If the files come from the *Pre-processing* stage but cannot be processed into the *Ready-for-ingestion* stage because
the files are not recognized, then they're placed in the following structure:
```
<forReviewFolder>/UNRECOGNIZED/<date-in-yyyyMMdd>/<Name>/{files for that name}
```

If the files come from the *Pre-processing* stage but cannot be processed into the *Ready-for-ingestion* stage because
of some error in processing, then they're placed in the following structure:
```
<forReviewFolder>/<date-in-yyyyMMdd>/<Name><Edition>_<full-name-of-publication>/content/streams/{files for that name/edition}
```

If the files come from the *Ready-for-ingestion* stage but are not ingested into Rosetta properly, then they're placed in the
following structure:
```
<forReviewFolder>/<date-in-yyyyMMdd>/<Name><Edition>_<full-name-of-publication>/content/streams/{files for that name/edition}
```

### Other stages (reporting, diagnosis and testing)

#### listFiles: list files based on source folder
`listFiles` simply lists files by name, edition and date:
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
Copies files from previous production loads into Rosetta into groupByDateAndName *and* pre-Rosetta ingest structures
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
<Name><Edition>-yyyyMMdd-<optional-sequence-letter><optional-sequence-number>
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
updating to reflect the changes in the source spreadsheet. First, export the original spreadsheet in `.csv` format
with the file separator as `|` and save it, replacing `core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-spreadsheet.csv`.

The task `updateDefaultFairfaxImportParameters` then updates that JSON file.
```
gradle updateDefaultFairfaxImportParameters -PfairfaxSpreadsheetImportFilename="core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-spreadsheet.csv"
```

Changes should then be checked in and a new version of this jar built.

## Contributors

See git commits to see who contributors are. Issues are tracked through the git repository issue tracker.

## License

&copy; 2018 &mdash; 2019 National Library of New Zealand. All rights reserved. MIT license.
