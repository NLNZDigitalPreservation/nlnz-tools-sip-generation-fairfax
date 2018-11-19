package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(includeNames=true, includePackage=false, excludes=[ 'parameterMap' ])
class FairfaxFileMatch {
    FairfaxFile fairfaxFile
    List<String> stringMatches = [ ]

    int totalMatches() {
        return stringMatches.size()
    }
}
