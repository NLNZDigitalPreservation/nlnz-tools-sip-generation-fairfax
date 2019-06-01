package nz.govt.natlib.tools.sip.generation.fairfax.parameters

import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.state.SipProcessingException

@Slf4j
enum ProcessingRule {
    HandleUnprocessed("handle_unprocessed"),
    HandleUnrecognised("handle_unrecognised"),
    HandleInvalid("handle_invalid"),
    Manual("manual"),
    MultipleEditions("multiple_editions"),
    SingleEdition("single_edition")

    private static final Map<String, ProcessingRule> LOOKUP_BY_FIELD_VALUE = [ : ]
    private static final Map<ProcessingRule, List<ProcessingRule>> OVERRIDES_MAP = [ : ]
    private final String fieldValue

    static {
        values().each { ProcessingRule processingRule ->
            LOOKUP_BY_FIELD_VALUE.put(processingRule.fieldValue, processingRule)
            OVERRIDES_MAP.put(MultipleEditions, [ SingleEdition ])
            OVERRIDES_MAP.put(SingleEdition, [ MultipleEditions ])
        }
    }

    static List<ProcessingRule> extract(String list, String separator = ",", boolean exceptionIfUnrecognized = false) {
        List<ProcessingRule> processingRules = [ ]
        if (list == null || list.strip().isEmpty()) {
            return processingRules
        }
        List<String> separatedList = list.split(separator)
        separatedList.each { String value ->
            String strippedValue = value.strip()
            ProcessingRule processingRule = forFieldValue(strippedValue)
            if (processingRule == null) {
                if (!strippedValue.isEmpty()) {
                    String message = "Unable to match processing rule=${strippedValue} to a ProcessingRule enum value."
                    log.warn(message)
                    if (exceptionIfUnrecognized) {
                        throw new SipProcessingException(message)
                    }
                }
            } else {
                processingRules.add(processingRule)
            }
        }

        return processingRules
    }

    static List<ProcessingRule> mergeOverrides(List<ProcessingRule> current, List<ProcessingRule> overrides) {
        List<ProcessingRule> merged = [ ]
        current.each { ProcessingRule option ->
            ProcessingRule override = overrides.find { ProcessingRule possibleOverride ->
                option.isOverride(possibleOverride)
            }
            if (override == null) {
                merged.add(option)
            } else {
                merged.add(override)
            }
        }
        return merged.unique()
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

    boolean isOverride(ProcessingRule otherOption) {
        return overrides().contains(otherOption)
    }

    List<ProcessingRule> overrides() {
        List<ProcessingRule> overrides = OVERRIDES_MAP.get(this)
        return overrides == null ? [ ] : overrides
    }

    String toString() {
        return this.fieldValue
    }
}
