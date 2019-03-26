package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.Sortable
import groovy.transform.ToString
import groovy.util.logging.Slf4j

@Canonical
@EqualsAndHashCode
@Sortable(includes = ['titleCode', 'editionCode' ])
@ToString
@Slf4j
class FairfaxFileTitleEditionKey {
    String titleCode
    String editionCode
}
