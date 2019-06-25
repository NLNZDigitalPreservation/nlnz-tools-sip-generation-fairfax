===================
Script Runner Guide
===================

Additional TODO
===============

-   Placeholder for additional TODO items.


Introduction
============

About NLNZ Tools SIP Generation Fairfax
---------------------------------------

NLNZ Tools SIP Generation Fairfax is specific set of tools for processing Fairfax-specific content. The ultimate output
of these tools are SIPs for ingestion into the Rosetta archiving system.

Most of the operations are run on the command line using a set of parameters and a spreadsheet of values that when
combined together with the operational code produce an output that is ready for ingestion into Rosetta.

The purpose of these tools is to process the Fairfax files. The long-term goal would be to wrap these tools into a
user interface. See the **Future milestones** section of the :doc:`developer-guide` for more details.

About this document
-------------------

This document is the NLNZ Tools SIP Generation Fairfax Script Runner Guide. It describes how to use the command-line
tools provided by the project to perform various workflow operations.

The manual is divided into chapters, each of which deals with a particular scripting operation.

See also:

-   :doc:`Workflow Guide<workflow-guide>`
-   :doc:`Developer Guide<developer-guide>`
-   :doc:`Librarian Guide<librarian-guide>`
-   :doc:`FAQ<faq>`


Contents of this document
-------------------------

Following this introduction, this User Guide includes the following sections:

-   **ProcessorRunner general usage** - Covers general processing parameters.

-   **FTP stage**  - Covers the FTP stage.

-   **Pre-processing stage**  - Covers the pre-processing stage.

-   **Ready-for-ingestion stage** - Covers ready-for-ingestion stage.

-   **Copying ingested loads to ingested folder** - Covers copying ingested loads to their final ingested folder.

-   **Additional tools** - Covers additional scripting tools.

-   **Converting the spreadsheet to JSON and vice-versa** - Covers converting the parameters spreadsheet between formats.

-   **Copying and moves** - Covers how copying files and moving files ensure data integrity.


Relationships with other scripting code
=======================================

Some of this scripting code is related to the codebase *nlnz-tools-scripts-ingestion* found in the github
repository https://github.com/NLNZDigitalPreservation/nlnz-tools-scripts-ingestion . See the documentation for that
codebase at https://nlnz-tools-sip-generation.readthedocs.io . There is an expectation that the two codebases will work
together.

There is also some additional scripts in the github repository:
https://github.com/NLNZDigitalPreservation/nlnz-tools-scripts-ingestion . See the documentation for those scripts
found at https://nlnz-tools-scripts-ingestion.readthedocs.io .


ProcessorRunner general usage
=============================

ProcessorRunner runs different processors based on command-line options.

Processing for different processing stages
------------------------------------------
Processing stages are discussed in more detail in :doc:`workflow-guide`.

+-------------------------------------+--------------------------------------------------------------------------------+
| Processing stage                    | Description                                                                    |
+=====================================+================================================================================+
| --preProcess                        | Group source files by date and titleCode. Output is used by readyForIngestion. |
|                                     | Requires sourceFolder, targetPreProcessingFolder, forReviewFolder.             |
|                                     | Uses startingDate, endingDate.                                                 |
|                                     | Optional createDestination, moveFiles, parallelizeProcessing, numberOfThreads. |
|                                     | This is a processing operation and must run exclusively of other processing    |
|                                     | operations.                                                                    |
+-------------------------------------+--------------------------------------------------------------------------------+
| --readyForIngestion                 | Process the source files. Output is ready for ingestion by Rosetta.            |
|                                     | Requires sourceFolder, targetForIngestionFolder, forReviewFolder,              |
|                                     | processingType.                                                                |
|                                     | Uses startingDate, endingDate.                                                 |
|                                     | Optional createDestination. Note that moveFiles is not supported at this time. |
|                                     | Optional parallelizeProcessing, numberOfThreads, maximumThumbnailPageThreads.  |
|                                     | This is a processing operation and must run exclusively of other processing    |
|                                     | operations.                                                                    |
+-------------------------------------+--------------------------------------------------------------------------------+
| --copyIngestedLoadsToIngestedFolder | Copy the ingested loads to ingested folder.                                    |
|                                     | Requires sourceFolder, targetPostProcessedFolder, forReviewFolder.             |
|                                     | Uses startingDate, endingDate.                                                 |
|                                     | Optional createDestination, moveFiles, moveOrCopyEvenIfNoRosettaDoneFile.      |
|                                     | Optional parallelizeProcessing, numberOfThreads, maximumThumbnailPageThreads.  |
|                                     | This is a processing operation and must run exclusively of other processing    |
|                                     | operations.                                                                    |
+-------------------------------------+--------------------------------------------------------------------------------+

Other types of processing
-------------------------
+---------------------------------+------------------------------------------------------------------------------------+
| Other processing                | Description                                                                        |
+=================================+====================================================================================+
| --copyProdLoadToTestStructures  | Copy the production load to test structures.                                       |
|                                 | Uses startingDate, endingDate.                                                     |
|                                 | This is a processing operation and must run exclusively of other processing        |
|                                 | operations.                                                                        |
+---------------------------------+------------------------------------------------------------------------------------+
| --generateThumbnailPageFromPdfs | Generate a thumbnail page from the PDFs in the given folder.                       |
|                                 | Requires sourceFolder, targetFolder.                                               |
|                                 | Optional startingDate and endingDate will select directories that match dates in   |
|                                 | yyyyMMdd format.                                                                   |
|                                 | Generates a thumbnail page using the PDFs in the source folder. The name of the    |
|                                 | jpeg is based on the source folder.                                                |
|                                 | This is a processing operation and must run exclusively of other processing        |
|                                 | operations.                                                                        |
+---------------------------------+------------------------------------------------------------------------------------+

Reports
-------
+--------------------+-------------------------------------------------------------------------------------------------+
| Reports            | Description                                                                                     |
+====================+=================================================================================================+
| -l, --listFiles    | List the source files in an organized way.                                                      |
|                    | Requires sourceFolder.                                                                          |
|                    | This is a reporting operation and cannot be run with any other processing operations.           |
+--------------------+-------------------------------------------------------------------------------------------------+
| --extractMetadata  | Extract and list the metadata from the source files.                                            |
|                    | Requires sourceFolder.                                                                          |
|                    | This is a reporting operation and cannot be run with any other processing operations.           |
+--------------------+-------------------------------------------------------------------------------------------------+
| --statisticalAudit | Statistical audit.                                                                              |
|                    | Search through the source folder and provide a statistical audit of the files found.            |
|                    | This is a reporting operation and cannot be run with any processing operations.                 |
+--------------------+-------------------------------------------------------------------------------------------------+

General parameters
------------------
+--------------------------------------------------------------+--------------------------------------------------------+
| Parameters - General                                         | Description                                            |
+==============================================================+========================================================+
| -b, --startingDate=STARTING_DATE                             | Starting date in the format yyyy-MM-dd (inclusive).    |
|                                                              | Dates are usually based on file name (not timestamp).  |
|                                                              | Default is 2015-01-01.                                 |
+--------------------------------------------------------------+--------------------------------------------------------+
| -e, --endingDate=ENDING_DATE                                 | Ending date in the format yyyy-MM-dd (inclusive).      |
|                                                              | Default is today. Files after this date are ignored.   |
+--------------------------------------------------------------+--------------------------------------------------------+
| -s, --sourceFolder=SOURCE_FOLDER                             | Source folder in the format /path/to/folder            |
|                                                              | This folder must exist and must be a directory.        |
+--------------------------------------------------------------+--------------------------------------------------------+
| --targetFolder=TARGET_FOLDER                                 | Target folder in the format /path/to/folder.           |
|                                                              | This is the destination folder used when no other      |
|                                                              | destination folders are specified.                     |
|                                                              | Use --createDestination to force its creation.         |
+--------------------------------------------------------------+--------------------------------------------------------+
| --targetPreProcessingFolder=TARGET_PRE_PROCESS_FOLDER        | Target pre-processing folder in the format             |
|                                                              | /path/to/folder                                        |
|                                                              | Use --createDestination to force its creation.         |
+--------------------------------------------------------------+--------------------------------------------------------+
| --targetPostProcessedFolder=TARGET_POST_PROCESSED_FOLDER     | Target post-processed folder in the format             |
|                                                              | /path/to/folder                                        |
|                                                              | Use --createDestination to force its creation.         |
+--------------------------------------------------------------+--------------------------------------------------------+
| -r, --forReviewFolder=FOR_REVIEW_FOLDER                      | For-review folder in the format /path/to/folder.       |
|                                                              | For processing exceptions, depending on processor.     |
|                                                              | Use --createDestination to force its creation.         |
+--------------------------------------------------------------+--------------------------------------------------------+
| --numberOfThreads=NUMBER_OF_THREADS                          | Number of threads when running operations in parallel. |
|                                                              | The default is 1.                                      |
+--------------------------------------------------------------+--------------------------------------------------------+
| --maximumThumbnailPageThreads=MAXIMUM_THUMBNAIL_PAGE_THREADS | Maximum of threads that can be used to generate        |
|                                                              | thumbnail pages when running operations in parallel    |
|                                                              | The default is 1.                                      |
|                                                              | This limit is in place because in-memory thumbnail     |
|                                                              | pagegeneration can be quite resource intensive and can |
|                                                              | overload the JVM.                                      |
+--------------------------------------------------------------+--------------------------------------------------------+
| --generalProcessingOptions=GENERAL_PROCESSING_OPTIONS        | General processing options.                            |
|                                                              | A comma-separated list of options. These options will  |
|                                                              | override any contradictory options.                    |
|                                                              | These processing options may or may not be applied     |
|                                                              | depending on the processing that takes place.          |
|                                                              | See the class ProcessorOption for a list of what those |
|                                                              | options are.                                           |
+--------------------------------------------------------------+--------------------------------------------------------+

Ready-for-ingestion parameters
------------------------------
+--------------------------------------------------------+-------------------------------------------------------------+
| Parameters - Ready-for-ingestion                       | Description                                                 |
+========================================================+=============================================================+
| --targetForIngestionFolder=TARGET_FOR_INGESTION_FOLDER | Target for-ingestion folder in the format /path/to/folder   |
|                                                        | Use --createDestination to force its creation.              |
+--------------------------------------------------------+-------------------------------------------------------------+
| --forIngestionProcessingTypes=PROCESSING_TYPES         | Comma-separated list of for-ingestion processing types.     |
|                                                        | A pre-processing titleCode folder should only be processed  |
|                                                        | once for a single processing type. It may be possible for   |
|                                                        | multiple processing types to apply to the same folder,      |
|                                                        | producing different SIPs.                                   |
+--------------------------------------------------------+-------------------------------------------------------------+
| --forIngestionProcessingRules=PROCESSING_RULES         | For-ingestion processing rules.                             |
|                                                        | A comma-separated list of rules. These rules will override  |
|                                                        | any contradictory rules.                                    |
+--------------------------------------------------------+-------------------------------------------------------------+
| --forIngestionProcessingOptions=PROCESSING_OPTIONS     | For-ingestion processing options.                           |
|                                                        | A comma-separated list of options. These options will       |
|                                                        | override any contradictory options.                         |
+--------------------------------------------------------+-------------------------------------------------------------+

Options
-------
+-------------------------------------+--------------------------------------------------------------------------------+
| Options                             | Description                                                                    |
+=====================================+================================================================================+
| -c, --createDestination             | Whether destination (or target) folders will be created.                       |
|                                     | Default is no creation (false).                                                |
+-------------------------------------+--------------------------------------------------------------------------------+
| --moveFiles                         | Whether files will be moved or copied. Default is copy (false).                |
+-------------------------------------+--------------------------------------------------------------------------------+
| --parallelizeProcessing             | Run operations in parallel (if possible).                                      |
|                                     | Operations that have components that can run in parallel currently are:        |
|                                     | --preProcess, --readyForIngestion, --generateThumbnailPageFromPdfs             |
+-------------------------------------+--------------------------------------------------------------------------------+
| --detailedTimings                   | Include detailed timings (for specific operations).                            |
+-------------------------------------+--------------------------------------------------------------------------------+
| --moveOrCopyEvenIfNoRosettaDoneFile | Whether the move or copy takes place even if there is no Rosetta done file.    |
|                                     | The Rosetta done files is a file with a titleCode of 'done'.                   |
|                                     | Default is no move or copy unless there IS a Rosetta done file (false).        |
+-------------------------------------+--------------------------------------------------------------------------------+
| --verbose                           | Include verbose output.                                                        |
+-------------------------------------+--------------------------------------------------------------------------------+
| -h, --help                          | Display a help message.                                                        |
+-------------------------------------+--------------------------------------------------------------------------------+

General processing options
--------------------------
General processing options are those options specified by the parameter
``--generalProcessingOptions=GENERAL_PROCESSING_OPTIONS``. In the codebase they are represented by the ``enum``
``ProcessorOption``.

The options are as follows:

``search_subdirectories``
    When finding files, also include subdirectories. Overridden by ``root_folder_only``.

``root_folder_only``
    When finding files, only use the specified folder (not subdirectories). Overridden by ``search_subdirectories``.

``use_source_subdirectory_as_target``
    Use the source folder as the target folder. This only works for certain kinds of processing.

``show_directory_only``
    Used when converting a directory path to a file or folder name. In this case only the directory name (without any
    parent directories) is used. Overridden by ``show_directory_and_one_parent``, ``show_directory_and_two_parents``,
    ``show_directory_and_three_parents``, ``show_full_path``.

``show_directory_and_one_parent``
    Used when converting a directory path to a file or folder name. In this case only the directory name and one
    parent directory is used. Overridden by ``show_directory_only``, ``show_directory_and_two_parents``,
    ``show_directory_and_three_parents``, ``show_full_path``.

``show_directory_and_two_parents``
    Used when converting a directory path to a file or folder name. In this case only the directory name and two
    parent directories are used. Overridden by ``show_directory_only``, ``show_directory_and_one_parent``,
    ``show_directory_and_three_parents``, ``show_full_path``.

``show_directory_and_three_parents``
    Used when converting a directory path to a file or folder name. In this case only the directory name and three
    parent directories are used. Overridden by ``show_directory_only``, ``show_directory_and_one_parent``,
    ``show_directory_and_two_parents``, ``show_full_path``.

``show_full_path``
    Used when converting a directory path to a file or folder name. In this case the full path is used. Overridden by
    ``show_directory_only``, ``show_directory_and_one_parent``, ``show_directory_and_two_parents``,
    ``show_directory_and_three_parents``.


FTP stage
=========

All PDF files are placed in a single FTP folder by the file producer. There are no subfolders.

Pre-processing stage
====================

The pre-processing stage moves the files found in the ``ftp`` directory to the ``pre-processing`` folder. In the
ftp folder all the files sit in the same directory. In the ``pre-processing`` directory, the files are separated out by
date and ``title_code``, as in the following structure::

    <targetPreProcessingFolder>/<date-in-yyyyMMdd>/<TitleCode>/{files for that titleCode and date}

This file structure prepares the files for ready-for-ingestion processing.

Example processing command
--------------------------
The ``sip-generation-fairfax-fat-all`` jar as shown in the following example::

    sourceFolder="/path/to/ftp/folder"
    targetBaseFolder="/path/to/LD_Sched/fairfax-processing"
    targetPreProcessingFolder="${targetBaseFolder}/pre-processing"
    forReviewFolder="${targetBaseFolder}/for-review"

    startingDate="2019-06-01"
    endingDate="2019-06-15"

    # Note that the number of threads increases processing speed due to ODS poor single-thread performance
    numberOfThreads=800

    maxMemory="2048m"
    minMemory="2048m"

    java -Xms${minMemory} -Xmx${maxMemory} \
        -jar fat/build/libs/sip-generation-fairfax-fat-all-<VERSION>.jar \
        --preProcess \
        --startingDate="${startingDate}" \
        --endingDate="${endingDate}" \
        --sourceFolder="${sourceFolder}" \
        --targetPreProcessingFolder="${targetPreProcessingFolder}" \
        --forReviewFolder="${forReviewFolder}" \
        --createDestination \
        --moveFiles \
        --parallelizeProcessing \
        --numberOfThreads ${numberOfThreads}


For-review
----------
If a file or set of files is unable to be processed for some reason, it will be placed in the *For-review* folder. There
is no processor that operates on the *For-review* stage. Processors that output to the *For-review* folder use the
parameter ``forReviewFolder`` to set the location of the *For-review* folder.

FTP files with identifiable title_code
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
If the files come from the FTP folder and the *TitleCode* and date are identifiable from the filename, the files are in
the following structure::

    <forReviewFolder>/<date-in-yyyyMMMdd>/<TitleCode>/{files}

FTP files without identifiable title_code and identifiable date
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
If the files come from the FTP folder and the *TitleCode* is not identifiable from the filename (but the date is), the
files are in the following structure::

    <forReviewFolder>/UNKNOWN-TITLE-CODE/<date-in-yyyyMMdd>/{files-that-have-no-title-code-mapping-for-that-date}

FTP files without identifiable title_code and without identifiable date
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
If the files come from the FTP folder and the *TitleCode* and date are not identifiable from the filename, the files are
in the following structure::

    <forReviewFolder>/UNKNOWN-TITLE-CODE/UNKNOWN-DATE/{files-that-have-no-title-code-mapping-for-that-date}


Ready-for-ingestion stage
=========================

The second state of processing where files are aggregated into specific SIPs ready for ingestion into Rosetta.

Note that the ``--moveFiles`` option is currently not supported, as multiple processing types operate on the same set
of files.

The *Ready-for-ingestion* folder structure is how Rosetta ingests the files. Magazines and newspapers have different
*Material Flows*, so ingestion of those different IEEntity types must be in different folders.

Processing spreadsheet
----------------------
The processing spreadsheet is used in the ready-for-ingestion stage to determine how a particular set of files
associated with a title code are processed.

Default spreadsheet
~~~~~~~~~~~~~~~~~~~
A spreadsheet exists that determines how a given title code is processed for a given processing type. A default
spreadsheet exists in the codebase under
``src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-spreadsheet.csv``. This
spreadsheet uses a column delimiter of ``|``.

Spreadsheet conversion to JSON
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Build script tasks exist to conver a ``.csv`` spreadsheet to a ``.json`` file. See the section
`Converting the spreadsheet to JSON and vice-versa`_ for an explanation on how that conversion is done.

Spreadsheet structure
~~~~~~~~~~~~~~~~~~~~~
The structure of the spreadsheet is discussed in the :doc:`librarian-guide`.

JSON file structure
~~~~~~~~~~~~~~~~~~~
The JSON-file structure lays out the same parameters in a JSON format. The actual processing uses the JSON file as its
processing input. For example, the Taupo Times has the following entry::

    {
        "row-0246": {
            "MMSID": "9917962373502836",
            "title_parent": "Taupo Times",
            "processing_type": "parent_grouping",
            "processing_rules": "",
            "processing_options": "numeric_before_alpha",
            "publication_key": "title_code",
            "title_code": "TAT",
            "edition_discriminators": "",
            "section_codes": "ED1+TAB+QFS",
            "Access": "200",
            "Magazine": "0",
            "ingest_status": "STA",
            "Frequency": "",
            "entity_type": "PER",
            "title_mets": "Taupo Times",
            "ISSN online": "",
            "Bib ID": "",
            "Access condition": "",
            "Date catalogued": "",
            "Collector_folder": "Taupo_Times",
            "Cataloguer": "",
            "Notes": "Fairfax updated title code",
            "first_issue_starting_page": "",
            "last_issue_starting_page": "",
            "has_volume_md": "0",
            "has_issue_md": "0",
            "has_number_md": "0",
            "previous_volume": "",
            "previous_volume_date": "",
            "previous_volume_frequency": "",
            "previous_issue": "",
            "previous_issue_date": "",
            "previous_issue_frequency": "",
            "previous_number": "",
            "previous_number_date": "",
            "previous_number_frequency": ""
        }
    }

Folder structure
----------------
The structure of the ready-for-ingestion output is discussed in the :doc:`librarian-guide`.

Deciding how to process: Processing types, spreadsheets and folders
-------------------------------------------------------------------

When the ready-for-ingestion processing takes place, each folder that gets processed has a ``title_code`` (which is the
name of the folder itself. The ready-for-ingestion processing takes that ``title_code`` and matches it with a
spreadsheet for the given ``processing_type``. If there is no spreadsheet row that matches the ``title_code`` and
``processing_type``, then no processing for that type takes place. There may be other processing types that match a
specific spreadsheet row.

Processing types
----------------

There are different processing types that have slightly different ways of dealing with the files in a ``title_code``
folder. When multiple processing types are specified, the processing types checked in order until a spreadsheet row
is found that matches. Processing types themselves correspond to the class ``ProcessingType``.

The processing types are checked in the following order:

``parent_grouping_with_edition``
    The ``title_code`` is combined with the first ``edition_discriminators`` to produce a spreadsheet row match.

``parent_grouping_with_edition`` default rules:
    ``skip_ignored``, ``skip_unrecognised``, ``skip_invalid``, ``automatic``, ``required_all_sections_in_sip``,
    ``missing_sequence_is_error``, ``missing_sequence_double_wide_is_ignored``, ``ignore_editions_without_files``,
    ``zero_length_pdf_replaced_with_page_unavailable``, ``do_not_force_skip``,
    ``numeric_starts_in_hundreds_not_considered_sequence_skips``.

``parent_grouping_with_edition`` default options:
    ``numeric_before_alpha``, ``generate_processed_pdf_thumbnails_page``,
    ``skip_generation_thumbnail_page_when_error_free``, ``use_in_memory_pdf_to_thumbnail_generation``.

``parent_grouping``
    The ``title_code`` is used to produce a spreadsheet row match.

``parent_grouping`` default rules:
    ``skip_ignored``, ``skip_unrecognised``, ``skip_invalid``, ``automatic``, ``required_all_sections_in_sip``,
    ``missing_sequence_is_error``, ``missing_sequence_double_wide_is_ignored``, ``ignore_editions_without_files``,
    ``zero_length_pdf_replaced_with_page_unavailable``, ``do_not_force_skip``,
    ``numeric_starts_in_hundreds_not_considered_sequence_skips``.

``parent_grouping`` default options:
    ``numeric_before_alpha``, ``generate_processed_pdf_thumbnails_page``,
    ``skip_generation_thumbnail_page_when_error_free``, ``use_in_memory_pdf_to_thumbnail_generation``.

``supplement_grouping``
    The ``title_code`` and ``section_code`` is used to produce a spreadsheet row match. This is generally used for
    publications that are part of a parent publication (for example, a parent publication might have a special section
    that can be extracted with its own MMSID).

``supplement_grouping`` default rules:
    ``skip_ignored``, ``skip_unrecognised``, ``skip_invalid``, ``automatic``, ``optional_all_sections_in_sip``,
    ``missing_sequence_is_error``, ``missing_sequence_double_wide_is_ignored``, ``ignore_editions_without_files``,
    ``zero_length_pdf_replaced_with_page_unavailable``, ``do_not_force_skip``,
    ``numeric_starts_in_hundreds_not_considered_sequence_skips``.

``supplement_grouping`` default options:
    ``numeric_before_alpha``, ``generate_processed_pdf_thumbnails_page``,
    ``skip_generation_thumbnail_page_when_error_free``, ``use_in_memory_pdf_to_thumbnail_generation``.

``create_sip_for_folder``
    This a catch all for when there is no spreadsheet row match. The ``title_code`` is still used to produce an output
    folder structure with the given files. However, the ``mets.xml`` does not have MMSID, publication name, access
    value. All those values would need editing before the folder could be ingested into Rosetta.

``create_sip_for_folder`` default rules:
    ``skip_ignored``, ``skip_unrecognised``, ``skip_invalid``, ``automatic``, ``required_all_sections_in_sip``,
    ``missing_sequence_is_error``, ``missing_sequence_double_wide_is_ignored``, ``ignore_editions_without_files``,
    ``zero_length_pdf_replaced_with_page_unavailable``, ``do_not_force_skip``,
    ``numeric_starts_in_hundreds_not_considered_sequence_skips``.

``create_sip_for_folder`` default options:
    ``numeric_before_alpha``, ``generate_processed_pdf_thumbnails_page``,
    ``skip_generation_thumbnail_page_when_error_free``, ``use_in_memory_pdf_to_thumbnail_generation``.

Processing rules
----------------
Processing rules determine how certain aspects of the workflow take place. Each processing rule has an opposite rule
that can be used to override its value.

``handle_ignored``
    Ignored files are placed in a separate for-review folder called ``IGNORED/date/title_code``. Override is
    ``skip_ignored``.

``skip_ignored``
    Ignored files are not placed in any separate folders. Override is ``handle_ignored``.

``handle_unrecognised``
    Unrecognised files are placed in a separate for-review folder called ``UNRECOGNIZED/date/title_code``. Override is
    ``skip_unrecognised``.

``skip_unrecognised``
    Unrecognised files are not placed in any separate folders. Override is ``handle_unrecognised``.

``handle_invalid``
    Invalid files are placed in a separate for-review folder called ``INVALID/date/title_code``. Override is
    ``skip_invalid``.

``skip_invalid``
    Invalid files are not placed in any separate folders. Override is ``handle_invalid``.

``manual``
    The generated file structure is always sent to for-review if there are no errors. Override is ``automatic``.

``automatic``
    The generated file structure is set to ready-for-ingestion if there are no errors. Override is ``manual``.

``force_skip``
    Skips the processing of the given type/date/title_code combination. Useful for spreadsheet rows that are not being
    processed correctly. Override is ``do_not_force_skip``.

``do_not_force_skip``
    Processes the given type/date/title_code combination. Override is ``force_skip``.

``process_all_editions``
    Process all the editions for a given title_code, even if there are no specific edition files. Override is
    ``ignore_editions_without_files``.

``ignore_editions_without_files``
    Only processes edition for a given title_code that has actual edition-specific files. For example, there might be
    ``edition_discriminators`` ``ED1+ED2+ED3``, but only ``ED1`` and ``ED2`` files exist. In that case, only ``ED1`` and
    ``ED2`` output would be created. Override is ``process_all_editions``.

``edition_discriminators_using_smart_substitute``
    For processing type ``parent_grouping_with_edition``, the ``title_code`` and a specific ``section_code`` form the
    spreadsheet row key. ``edition_discriminators_using_smart_substitute`` is for something like the following
    situation: For the ``title_code`` ``QCM`` we want to make edition substitutions, but eachedition discriminator has
    its own section code. We have titleCode: QCM, with 3 separate editions: edition discriminator: ED1,
    section_codes: ED1; edition discriminator: ED2, section_codes: ED2; and editionDiscriminator: ED3,
    section_codes: ED3. We still want to substitute the pages in ED2 and ED3 over the ED1 pages. In order to do that, we
    find the FIRST edition discriminator and set the edition discriminators to the FIRST edition discriminator and the
    current edition (section code). That means for ED2, we would use the ED1 pages and substitute in the ED2 pages.
    Override is ``edition_discriminators_not_using_smart_substitute``.

``required_all_sections_in_sip``
    All sections are required to appear in the SIP. If they are not included based on the spreadsheet row, then an
    exception is generated. Override is ``optional_all_sections_in_sip``.

``optional_all_sections_in_sip``
    Not all sections are required to appear in the SIP. Override is ``required_all_sections_in_sip``.

``missing_sequence_is_ignored``
    Missing sequences in page numbering (such as skipping from page 1 to 3) are ignored. Override is
    ``missing_sequence_is_error``.

``missing_sequence_is_error``
    Missing sequences are not treated as an error. Override is ``missing_sequence_is_ignored``.

``missing_sequence_double_wide_is_ignored``
    A missing sequence whose previous page is either double the width or half the width or the current page is treated
    as if there is no missing sequence. This is to handle the common situation of double-wide pages. Override is
    ``missing_sequence_double_wide_is_error``.

``missing_sequence_double_wide_is_error``
    Even if the previous page is double the width or half the width of the current page, the missing sequence is still
    treated as an error (if ``missing_sequence_is_error`` is a rule). Override is
    ``missing_sequence_double_wide_is_ignored``.

``zero_length_pdf_replaced_with_page_unavailable``
    A zero-length PDF file (a file with a size of ``0``) is replaced with the standard *page unavailable* PDF file.
    This file is found in the codebase under ``core/src/main/resources/page-unavailable.pdf``. Override is
    ``zero_length_pdf_skipped``.

``zero_length_pdf_skipped``
    A zero-length PDF file (a file with a size of ``0``) is skipped (not replaced by any other file). Override is
    ``zero_length_pdf_replaced_with_page_unavailable``.

``numeric_starts_in_hundreds_not_considered_sequence_skips``
    There are some cases where a wrap starts in the 400's. Normally this would be considered a skipped sequence, but
    with this option sequence numbering starting in the 400's or more (so starting with 400 or 401, or 500 or 501, and
    so on) is not considered a sequence numbering skip. Override is
    ``numeric_starts_in_hundreds_considered_sequence_skips``.

``numeric_starts_in_hundreds_considered_sequence_skips``
    Sequence numbering skips that start with 400 or 401 or 500 or 501 and so on are still treated as a sequence
    numbering skip. Override is ``numeric_starts_in_hundreds_not_considered_sequence_skips``.

Processing options
------------------
Processing options determine how certain aspects of the workflow take place. Each processing option has an opposite
option that can be used to override its value. In general options don't have side effects, but rules do.

``alpha_before_numeric``
    Sequences are sorted with sequence letters sorted before sequence numbers only. So, we would have ordering
    ``A01, A02, B01, B02, 01, 02``. Override is ``numeric_before_alpha``.

``numeric_before_alpha``
    Sequences are sorted with sequence numbers only sorted before sequence letters only. So, we would have ordering
    ``01, 02, A01, A02, B01, B02``. Override is ``alpha_before_numeric``.

``generate_processed_pdf_thumbnails_page``
    Generates a thumbnail page of each PDF that is included in the SIP. This can be a resource (memory and CPU)
    intensive operation. Override is ``do_not_generate_processed_pdf_thumbnails_page``.

``do_not_generate_processed_pdf_thumbnails_page``
    Does not generate a thumbnail page of each PDF that is included in the SIP. Override is
    ``generate_processed_pdf_thumbnails_page``.

``skip_generation_thumbnail_page_when_error_free``
    Skip thumbnail page generation when there are no processing errors. Override is ``always_generate_thumbnail_page``.

``always_generate_thumbnail_page``
    Always generate thumbnail page. Override is ``skip_generation_thumbnail_page_when_error_free``.

``use_in_memory_pdf_to_thumbnail_generation``
    Use the in-memory pdf to thumbnail page generation. This can be a resource (memory and CPU) intensive operation.
    Override is ``use_command_line_pdf_to_thumbnail_generation``.

``use_command_line_pdf_to_thumbnail_generation``
    On linux-based systems, this option will use the command-line tool ``pdftoppm`` to generate the pdf thumbnails.
    This is a much faster (and much higher quality) operation. Override is
    ``use_in_memory_pdf_to_thumbnail_generation``.

Overrides for rules and options
-------------------------------
Processing rules and options can be overridden on several different levels.

Each processing type has a set of default processing rules and processing options.

The processing type rules and options are overridden by the rules and options in the given spreadsheet row that is
matched for processing a given ``title_code`` folder.

Finally, the command-line processing rules and processing options are applied and will override all previous options.

For example, the ``parent_grouping`` processing type has default processing option, ``numeric_before_alpha``. When
processing the title code ``DPT``, this default option is overridden by ``alpha_before_numeric`` for the DPT row
for ``parent_grouping``. Finally, it is possible to specify a processing option ``numeric_before_alpha`` on the
command line, which would mean that all processing sorts the ordering of PDFs as ``numeric_before_alpha``.

Example processing command
--------------------------
The following snippet illustrates a ready-for-ingestion processing command::

    sourceFolder="path/to/LD_Sched/fairfax-processing/pre-processing"
    targetBaseFolder="/path/to/LD_Sched/fairfax-processing"
    targetForIngestionFolder="${targetBaseFolder}/for-ingestion"
    forReviewFolder="${targetBaseFolder}/for-review"

    startingDate="2019-06-03"
    endingDate="2019-06-09"

    forIngestionProcessingTypes="parent_grouping,parent_grouping_with_edition,create_sip_for_folder"
    forIngestionProcessingOptions="use_command_line_pdf_to_thumbnail_generation"

    numberOfThreads=60
    # Note we ware using command-line pdf-to-thumbnail generation, which can handle higher throughput
    maximumThumbnailPageThreads=60

    maxMemory="3048m"
    minMemory="3048m"

    java -Xms${minMemory} -Xmx${maxMemory} \
        -jar fat/build/libs/sip-generation-fairfax-fat-all-<VERSION>.jar \
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

For-review
----------
See the :doc:`librarian-guide` for a discussion of the for-review output and how a librarian handles the different
exceptions to processing.


Copying ingested loads to ingested folder
=========================================

Once files have been ingested into Rosetta, a file with the name of ``done`` is placed in the root folder (in this case,
that folder is ``<magazine|newspaper>/<date-in-yyyyMMdd>_<tile_code>_<processing_type>_<optional-edition>__<full-name-of-publication>``.
This means that folder can be moved to the ``post-processed`` folder.

post-processed folder structure
-------------------------------
The folder structure for the ingested (post-processed) stage is as follows::

    <targetFolder>/<magazines|newspapers>/<TitleCode>/<yyyy>/<date-in-yyyyMMdd-format>

In this dated folder, the file structure matches the same structure that was ingested into Rosetta, namely::

    <date-in-yyyyMMdd-format>
       |- done
       |- content/
               |- mets.xml
               |- streams/
                       |- <pdf-files>

Note that the ``mets.xml`` file is placed in the `content` folder. The ``done`` files is in the root ``yyyyMMdd``
folder.

Example processing command
--------------------------
The following snippet illustrates a ``--copyIngestedLoadsToIngestedFolder`` processing command::

    baseFolder="/path/to/LD_Sched/fairfax-processing"
    sourceFolder="${baseFolder}/for-ingestion"
    targetPostProcessedFolder="${baseFolder}/post-processed"
    forReviewFolder="${baseFolder}/for-review"

    startingDate="2019-06-03"
    endingDate="2019-06-09"

    # Currently the processing is not multithreaded, but eventually it would be
    numberOfThreads=60

    maxMemory="2048m"
    minMemory="2048m"

    java -Xms${minMemory} -Xmx${maxMemory} \
        -jar fat/build/libs/sip-generation-fairfax-fat-all-<VERSION>.jar \
        --copyIngestedLoadsToIngestedFolder \
        --startingDate="${startingDate}" \
        --endingDate="${endingDate}" \
        --sourceFolder="${sourceFolder}" \
        --targetPostProcessedFolder="${targetForIngestionFolder}" \
        --forReviewFolder="${forReviewFolder}" \
        --createDestination \
        --parallelizeProcessing \
        --numberOfThreads=${numberOfThreads}

Important notes
---------------
The ``--moveFiles`` option is not included in the example.
The the ``done`` file must exist or the files will not be copied/moved. If files must be copied regardless of the
existence of the ``done`` file, use the option ``--moveOrCopyEvenIfNoRosettaDoneFile``.

For-review
----------
If a file or set of files is unable to be processed for some reason, it will be placed in the *For-review* folder. There
is no processor that operates on the *For-review* stage. Processors that output to the *For-review* folder use the
parameter ``forReviewFolder`` to set the location of the *For-review* folder.

If the files come from the *Ready-for-ingestion* stage but are not ingested into Rosetta properly, then they're placed in the
following structure (TODO verify this output for copyIngestedLoadsToIngestedFolder)::

    <forReviewFolder>/<date-in-yyyyMMdd>/<TitleCode><SectionCode>_<full-name-of-publication>/content/streams/{files for that titleCode/sectionCode}


Additional tools
================

listFiles: list files based on source folder
--------------------------------------------
``listFiles`` simply lists files by title code, section code and date::

    java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
        --listFiles \
        --startingDate="yyyy-MM-dd" \
        --endingDate="yyyy-MM-dd" \
        --sourceFolder="/path/to/source/folder"

extractMetadata: extract metadata from the pdf files based on source folder
---------------------------------------------------------------------------
Extracts metadata from the pdf files::

    java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
        --extractMetadata \
        --startingDate="yyyy-MM-dd" \
        --endingDate="yyyy-MM-dd" \
        --sourceFolder="/path/to/source/folder"


copyProdLoadToTestStructures: Copy production load files
--------------------------------------------------------
Copies files from previous production loads into Rosetta into Pre-processing *and* Ready-for-ingestion structures
for testing. The structures are as follows:

    1. preProcess structure. This is to mimic the input to readyForIngestion processing. The folder structures are the
    same as the output to `preProcess`, with the folder structure starting with ``<targetFolder>/preProcess``.
    2. readyForIngestion structure. This is the structure that gets ingested into Rosetta. The folder structures are the
    same as the output to `readyForIngestion`, with the folder structure starting with
    ``<targetFolder>/readyForIngestion``.

These structures provide for testing the Fairfax processor, to see if its outputs match the work done previously::

    java -jar sip-generation-fairfax-fat-all-<VERSION>.jar \
        --copyProdLoadToTestStructures \
        --startingDate="yyyy-MM-dd" \
        --endingDate="yyyy-MM-dd" \
        --sourceFolder="/path/to/source/folder" \
        --targetFolder="/path/to/target/folder" \
        --createDestination

Converting the spreadsheet to JSON and vice-versa
=================================================

From time to time the spreadsheet that defines how the Fairfax files are ingested will changed based on new information.
When this happens, the json file found at ``core/src/main/resources/default-fairfax-import-parameters.json`` needs
updating to reflect the changes in the source spreadsheet.

Converting the csv spreadsheet to JSON
--------------------------------------
    1. First, export the original spreadsheet in ``.csv`` format with the file separator as ``|`` and save it.
    2. Copy the exported csv spreadsheet to:
       ``core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-spreadsheet.csv``.
    3. Execute the gradle task ``updateDefaultFairfaxImportParameters``, which takes the csv spreadsheet and converts it
       to a JSON file, which is then used for the actual processing::

            gradle updateDefaultFairfaxImportParameters \
              -PfairfaxSpreadsheetImportFilename="core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-spreadsheet.csv" \
              -PfairfaxSpreadsheetExportFilename="core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-parameters.json"

Note that there is no requirement to use the filenames given in the example. The given filenames are the ones the code
uses.

Converting the JSON parameters to csv spreadsheet
-------------------------------------------------
The JSON file can be converted to a csv spreadsheet using the build task ``exportDefaultFairfaxImportParameters``::

    gradle exportDefaultFairfaxImportParameters \
      -PfairfaxSpreadsheetImportFilename="core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-parameters.json" \
      -PfairfaxSpreadsheetExportFilename="core/src/main/resources/nz/govt/natlib/tools/sip/generation/fairfax/default-fairfax-import-spreadsheet.csv"

Note that there is no requirement to use the filenames given in the example. The given filenames are the ones the code
uses.

Check in the changes and build a new version of the jar
-------------------------------------------------------
Once both the ``.csv`` and ``.json`` files have been updated, changes should then be checked in and a new version of this
the processor jar built, which will have the new JSON processing resource file.


Copying and moves
=================

File copying
------------
File copies are done in 2 steps:
- The file is copied to its new target with a file extension of ``.tmpcopy``.
- The file is renamed to the target name.

This means that the target does not have its correct name until the copy is complete. Subsequent runs on the same source
do checks to see if the target's MD5 hash is the same. If the hash is the same, the copy is not done.

Atomic file moves
-----------------
Some processing has a ``--moveFiles`` option. Note that when moving files *across* file systems (in other words, from
one file system to another), it's not possible to have truly atomic operations. If the move operation is interrupted
before it completes, what can happen is that a file of the same name will exist on both filesystems, with the target
file system having an incomplete file.

With that in mind, file moves have the following characteristics:

- If a file move can be done atomicly (as determined by the Java runtime), it is done atomicly.
- If the file move cannot be done atomicly (as determined by the Java runtime), the file moves take the following steps:
    1. The file is copied across to the target file system with a ``.tmpcopy`` extension.
    2. The file is renamed to the target file name.
    3. The source file is deleted.

This means that if at any point the operation is interrupted, a recovery can take place. A move when the file already
exists in the target folder will trigger a MD5 hash comparison. If the source file and the target file are identical,
the source file is deleted. Otherwise, the target file is moved across (using the steps above) with a ``-DUPLICATE-#``
in the filename. These ``-DUPLICATE-#`` files need to be checked manually to determine which file is correct.

We hope these mitigations will prevent any data loss.
