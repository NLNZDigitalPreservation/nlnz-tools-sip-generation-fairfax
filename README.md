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

## Processing usage

### ProcessorRunner used for all processing

A build will produce a sip-generation-fairfax-fat-all jar. This jar has the main class
`nz.govt.natlib.tools.sip.generation.fairfax.processor.ProcessorRunner`. Run this jar using:
```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar
```

The basic parameters used by this runner are as follows (these parameters are discussed more in detail):
```
Usage: processorRunner [-cdghlmpx] [-b=STARTING_DATE] [-e=ENDING_DATE]
                       [-s=SOURCE_FOLDER] [-t=TARGET_FOLDER]
Runs different processors based on command-line options

Processing stages:
  -d, --processByDate        Process the source files.
                             Output is ready for ingestion by Rosetta.
                             Requires sourceFolder, destination Folder.
                             Uses startingDate, endingDate
  -g, --groupByDateAndName   Group source files by date and name.
                             Output is used by processByDate.
                             Requires sourceFolder, destinationFolder.
                             Uses startingDate, endingDate
  -l, --listFiles            List the source files in an organized way.
                             Requires sourceFolder
  -p, --copyProdLoadToTestStructures
                             Copy the production load to test structures.
                             Requires sourceFolder, destination Folder.
                             Uses startingDate, endingDate
  -x, --extractMetadata      Extract and list the metadata from the source files.
                             Requires sourceFolder

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

Options:
  -c, --createDestination    Whether destination folders will be created. Default is
                               creation.
  -m, --moveFiles            Whether files will be moved or copied. Default is copy.
  -h, --help                 Display a help message.
```

While it is possible to multiple processing stages in the same run, note that they use the same `sourceFolder` and
`targetFolder` parameters. The source and target folders have different structures and different purposes, so we
recommend running a single stages at a time.

### Processing stages

There are several stages to processing, each of which has its own section:

1. *groupByDateAndName: Grouping the files by date and name*.
2. *processByDate: Process the files by date*.

See the *Parameters* section for a discussion of the different parameters used.

#### groupByDateAndName: Grouping the files by date and name
The first stage of processing where files are separated out by date and name. Note that the `--moveFiles` option is
not included in the example.
```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
    --groupByDateAndName \
    --startingDate="yyyy-MM-dd" \
    --endingDate="yyyy-MM-dd" \
    --sourceFolder="/path/to/source/folder" \
    --targetFolder="/path/to/target/folder" \
    --createDestination
```

The target folder is populated with the following structure:
````
<targetFolder>/
  |- <yyyyMMdd>/<name>/{files-for-the-given-name>
  |- UNKNOWN/<yyyyMMdd>/{files-that-have-no-name-mapping-for-that-date}
````

#### processByDate: Process the files by date
The second state of processing where files are aggregated into specific SIPs ready for ingestion into Rosetta. Note that
the `--moveFiles` option is not included in the example.
```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
    --processByDate \
    --startingDate="yyyy-MM-dd" \
    --endingDate="yyyy-MM-dd" \
    --sourceFolder="/path/to/source/folder" \
    --targetFolder="/path/to/target/folder" \
    --createDestination
```

The target folder is populated with the following structure:
````
<targetFolder>/
  |- <yyyyMMdd>/<ingest-key>/{files-for-the-given-ingest-key>
  |- UNKNOWN/<yyyyMMdd>/<name>/{files-that-have-no-ingest-key-for-that-date}
````

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
1. groupByDateAndName structure. This is to mimic the input to processByName.
   Directory structure: `groupByDateAndName/<yyyyMMdd>/<name>/{files}`
2. post-processByDate structure. This is the structure that gets ingested into Rosetta.
   Directory structure: `rosettaIngest/<date-in-yyyMMdd>/<name>_<yyyyMMdd>/{files}`

These structures provide for testing the Fairfax processor, to see if its outputs match the work done previously.

```
java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
    --copyProdLoadToTestStructures \
    --startingDate="yyyy-MM-dd" \
    --endingDate="yyyy-MM-dd" \
    --sourceFolder="/path/to/source/folder" \
    --targetFolder="/path/to/target/folder"
```

### Parameters

Parameters and their usage.

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

#### moveFiles
Whether files are moved or copied to the target folder when processed. The default is `false` if this option is not
specified.

#### createDestination
Whether the target folder is created if it does not exist. The default is `false` if this option is not specified.

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
