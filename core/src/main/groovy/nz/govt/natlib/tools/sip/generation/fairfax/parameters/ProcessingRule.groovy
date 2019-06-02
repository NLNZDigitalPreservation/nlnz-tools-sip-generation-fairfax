package nz.govt.natlib.tools.sip.generation.fairfax.parameters

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.state.SipProcessingException

@Log4j2
enum ProcessingRule {
    HandleUnprocessed("handle_unprocessed"),
    HandleUnrecognised("handle_unrecognised"),
    HandleInvalid("handle_invalid"),
    Manual("manual"),
    MultipleEditions("multiple_editions"),
    SingleEdition("single_edition"),
    AllSectionsInSipRequired("required_all_sections_in_sip"),
    AllSectionsInSipOptional("optional_all_sections_in_sip")

    private static final Map<String, ProcessingRule> LOOKUP_BY_FIELD_VALUE = [ : ]
    private static final Map<ProcessingRule, List<ProcessingRule>> OVERRIDES_MAP = [ : ]
    private final String fieldValue

    static {
        values().each { ProcessingRule processingRule ->
            LOOKUP_BY_FIELD_VALUE.put(processingRule.fieldValue, processingRule)
            OVERRIDES_MAP.put(MultipleEditions, [ SingleEdition ])
            OVERRIDES_MAP.put(SingleEdition, [ MultipleEditions ])
            OVERRIDES_MAP.put(AllSectionsInSipRequired, [ AllSectionsInSipOptional ])
            OVERRIDES_MAP.put(AllSectionsInSipOptional, [ AllSectionsInSipRequired ])
        }
    }

    static List<ProcessingRule> extract(String list, String separator = ",", List<ProcessingRule> defaults = [ ],
                                        boolean exceptionIfUnrecognized = false) {
        List<ProcessingRule> processingRules = [ ]
        if (list == null || list.strip().isEmpty()) {
            return mergeOverrides(defaults, processingRules)
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

        return mergeOverrides(defaults, processingRules)
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
        merged = merged.unique()
        overrides.each { ProcessingRule override ->
            if (!merged.contains(override)) {
                merged.add(override)
            }
        }
        return merged
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
