# National Library of New Zealand Tools SIP Generation for Fairfax files

Tools for building Fairfax-based SIPs for ingestion into the Rosetta archive system.

## Synopsis

This repository contains classes used to build Fairfax-based SIPs for ingestion into the Rosetta archive system.

## Motivation

We want to automate the generation of Fairfax SIPs from Fairfax files.

## Important

At this time there is no important information to impart.

## Requirements

- A set of Fairfax files for ingestion
- The nlnz-tools-sip-generation-gradle-plugin

## Usage

### Parameters

Parameters and their usage.

### Running

Running the tool.

#### Updating the default fairfax import parameters JSON file
From time to time the spreadsheet that defines how the Fairfax files are ingested will changed based on new information.
When this happens, the json file found at `core/src/main/resources/default-fairfax-import-parameters.json` needs
updating to reflect the changes in the source spreadsheet. First, export the original spreadsheet in `.csv` format
with the file separator as `|` and save it, replacing `core/src/main/resources/default-fairfax-import-spreadsheet.csv`.

The task `updateDefaultFairfaxImportParameters` then updates that JSON file.
```
gradle updateDefaultFairfaxImportParameters -PfairfaxSpreadsheetImportFilename="core/src/main/resources/default-fairfax-import-spreadsheet.csv"
```

Changes should then be checked in and a new version of this jar built.

## Contributors

See git commits to see who contributors are. Issues are tracked through the git repository issue tracker.

## License

&copy; 2018 National Library of New Zealand. All rights reserved. MIT license.
