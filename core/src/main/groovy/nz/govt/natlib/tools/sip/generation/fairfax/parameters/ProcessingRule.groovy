package nz.govt.natlib.tools.sip.generation.fairfax.parameters

import groovy.util.logging.Slf4j

@Slf4j
enum ProcessingRule {
    HandleUnprocessed("handle_unprocessed"),
    HandleUnrecognised("handle_unrecognised"),
    HandleInvalid("handle_invalid"),
    Manual("manual"),
    MultipleEditions("multiple_editions")

    private static final Map<String, ProcessingRule> LOOKUP_BY_FIELD_VALUE = [ : ]
    private final String fieldValue

    static {
        ProcessingRule.values().each { ProcessingRule processingRule ->
            LOOKUP_BY_FIELD_VALUE.put(processingRule.fieldValue, processingRule)
        }
    }

    static List<ProcessingRule> extract(String list, String separator = ",") {
        List<ProcessingRule> processingRules = [ ]
        List<String> separatedList = list.split(separator)
        separatedList.each { String value ->
            String strippedValue = value.strip()
            ProcessingRule processingRule = forFieldValue(strippedValue)
            if (processingRule == null) {
                if (!strippedValue.isEmpty()) {
                    log.warn("Unable to match processing rule=${strippedValue} to a ProcessingRule enum value.")
                }
            } else {
                processingRules.add(processingRule)
            }
        }

        return processingRules
    }

    static forFieldValue(String fieldValue) {
        return LOOKUP_BY_FIELD_VALUE.get(fieldValue.strip())
    }

    ProcessingRule(String fieldValue) {
        this.fieldValue = fieldValue
    }

    String getFieldValue() {
        return this.fieldValue
    }

    String toString() {
        return this.fieldValue
    }
}
