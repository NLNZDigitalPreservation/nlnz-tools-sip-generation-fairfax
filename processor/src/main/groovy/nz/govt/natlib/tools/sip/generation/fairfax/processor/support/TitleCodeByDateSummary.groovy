package nz.govt.natlib.tools.sip.generation.fairfax.processor.support

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.Sortable
import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFile

import java.time.LocalDate

@Canonical
@EqualsAndHashCode(includes = [ 'localDate', 'titleCode' ])
@Sortable(includes = [ 'localDate', 'titleCode' ])
@Log4j2
class TitleCodeByDateSummary {
    LocalDate localDate
    String titleCode
    Set<FairfaxFile> files = [ ]
    List<FairfaxFile> duplicateFiles = [ ]

    void addFile(FairfaxFile fairfaxFile) {
        if (files.contains(fairfaxFile)) {
            duplicateFiles.add(fairfaxFile)
        } else {
            files.add(fairfaxFile)
        }
    }

    List<FairfaxFile> outOfSequenceFiles() {
        List<FairfaxFile> outOfSequenceFiles = [ ]
        List<FairfaxFile> sortedFiles = files.sort()
        FairfaxFile lastFairfaxFile
        sortedFiles.each { FairfaxFile fairfaxFile ->
            if (lastFairfaxFile == null) {
                if (fairfaxFile.sequenceNumber != 1) {
                    outOfSequenceFiles.add(fairfaxFile)
                }
            } else {
                if (!fairfaxFile.comesDirectlyAfter(lastFairfaxFile)) {
                    outOfSequenceFiles.addAll([ lastFairfaxFile, fairfaxFile ])
                }
            }
            lastFairfaxFile = fairfaxFile
        }

        return outOfSequenceFiles

    }

    String forSpreadsheet(String separator = '|') {
        StringBuilder stringBuilder = new StringBuilder()
        stringBuilder.append(files.size())
        stringBuilder.append(separator)
        stringBuilder.append(titleCode)
        stringBuilder.append(separator)

        boolean firstFile = true
        outOfSequenceFiles().each { FairfaxFile fairfaxFile ->
            if (!firstFile) {
                stringBuilder.append(',')
            }
            stringBuilder.append(fairfaxFile.filename)
            firstFile = false
        }
        stringBuilder.append(separator)

        firstFile = true
        duplicateFiles.each { FairfaxFile fairfaxFile ->
            if (!firstFile) {
                stringBuilder.append(',')
            }
            stringBuilder.append(fairfaxFile.filename)
            firstFile = false
        }

        return stringBuilder.toString()
    }

}
