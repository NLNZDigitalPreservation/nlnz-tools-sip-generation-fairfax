=============
Release Notes
=============


Introduction
============

This guide, designed for a NLNZ Tools SIP Generation Fairfax developer, covers release notes from `1.0.1-SNAPSHOT`.
Versions are in reverse chronological order, with the most recent version first. While the
*NLNZ Tools SIP Generation Fairfax Developer Guide* and *NLNZ Tools SIP Generation Fairfax User Guide* are accurate for
the current release, the *Release Notes* can give some idea of how things have changed since the last major release.

Contents of this document
-------------------------

Following this introduction, the Release Notes includes the following sections:

-   **1.1.0-SNAPSHOT** - 1.1.0 development.

-   **1.0.3-SNAPSHOT** - 1.0.3 development.

-   **1.0.2-SNAPSHOT** - 1.0.2 development.

-   **1.0.1-SNAPSHOT** - 1.0.1 development.

-   **1.0.0-SNAPSHOT** - 1.0.0 development.

1.1.0-SNAPSHOT
==============

Adds in a new Clean Up FTP Processor. This allows users to delete a batch of files from the ftp folder that fall within
a range of dates.
Removes default dates being set for all processors - this is an additional safety check to ensure files aren't deleted
accidentally.

1.0.3-SNAPSHOT
==============

Life supplements for the Dominion Post and the Press now have a unique title code which no longer matches with their
parent publication. 1.0.3-SNAPSHOT matches up Life supplements with their corresponding publications and adds them to
the end of their respective titles to be processed. In addition it processes them as a separate supplement title using
the new title code.

1.0.2-SNAPSHOT
==============

Property title codes do not match up with their corresponding publications.  1.0.2-SNAPSHOT matches up property titles
with their corresponding publications and adds them to the end of their respective titles to be processed.

1.0.1-SNAPSHOT
==============

The Forever Project includes files that are to be appended to the end of multiple titles.  As such the changes in
1.0.1-SNAPSHOT searches for Forever Project Files and appends them to the end of their respective titles to be
processed.  In addition to this, sequence letter column was added to the spreadsheet and spreadsheet processor to handle
supplement grouping files based on the use of sequence letters.

1.0.0-SNAPSHOT
==============

TODO Keep track of ongoing development changes here.






