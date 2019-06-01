package nz.govt.natlib.tools.sip.generation.fairfax.parameters

import groovy.util.logging.Slf4j

@Slf4j
enum ProcessingOption {
    AlphaBeforeNumericSequencing("alpha_before_numeric"),
    AnyFirstEditionCode("any_first_edition_code")

    private static final Map<String, ProcessingOption> LOOKUP_BY_FIELD_VALUE = [ : ]
    private final String fieldValue

    static {
        ProcessingOption.values().each { ProcessingOption processingOption ->
            LOOKUP_BY_FIELD_VALUE.put(processingOption.fieldValue, processingOption)
        }
    }

    static List<ProcessingOption> extract(String list, String separator = ",") {
        List<ProcessingOption> processingOptions = [ ]
        List<String> separatedList = list.split(separator)
        separatedList.each { String value ->
            String strippedValue = value.strip()
            ProcessingOption processingOption = forFieldValue(strippedValue)
            if (processingOption == null) {
                if (!strippedValue.isEmpty()) {
                    log.warn("Unable to match processing option=${strippedValue} to a ProcessingOption enum value.")
                }
            } else {
                processingOptions.add(processingOption)
            }
        }

        return processingOptions
    }

    static forFieldValue(String fieldValue) {
        return LOOKUP_BY_FIELD_VALUE.get(fieldValue.strip())
    }

    ProcessingOption(String fieldValue) {
        this.fieldValue = fieldValue
    }

    String getFieldValue() {
        return this.fieldValue
    }

    String toString() {
        return this.fieldValue
    }
}
