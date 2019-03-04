package nz.govt.natlib.tools.sip.generation.fairfax

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.Sortable
import groovy.transform.ToString
import groovy.util.logging.Slf4j

@Canonical
@EqualsAndHashCode
@Sortable(includes = ['name', 'edition' ])
@ToString
@Slf4j
class FairfaxFileNameEditionKey {
    String name
    String edition
}
