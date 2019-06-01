package nz.govt.natlib.tools.sip.generation.fairfax.parameters

import groovy.util.logging.Slf4j

@Slf4j
enum ProcessingType {
    ParentGrouping("parent_grouping"),
    SupplementGrouping("supplement_grouping"),
    CreateSipForFolder("create_sip_for_folder")

    private static final Map<String, ProcessingType> LOOKUP_BY_FIELD_VALUE = [ : ]
    private final String fieldValue

    static {
        values().each { ProcessingType processingType ->
            LOOKUP_BY_FIELD_VALUE.put(processingType.fieldValue, processingType)
        }
    }

    static List<ProcessingType> extract(String list, String separator = ",") {
        List<ProcessingType> processingTypes = [ ]
        if (list == null || list.strip().isEmpty()) {
            return processingTypes
        }
        List<String> separatedList = list.split(separator)
        separatedList.each { String value ->
            String strippedValue = value.strip()
            ProcessingType processingType = forFieldValue(strippedValue)
            if (processingType == null) {
                if (!strippedValue.isEmpty()) {
                    log.warn("Unable to match processing type=${strippedValue} to a ProcessingType enum value.")
                }
            } else {
                processingTypes.add(processingType)
            }
        }

        return processingTypes
    }

    static forFieldValue(String fieldValue) {
        if (fieldValue != null && !fieldValue.strip().isEmpty()) {
            return LOOKUP_BY_FIELD_VALUE.get(fieldValue.strip())
        } else {
            return null
        }
    }

    ProcessingType(String fieldValue) {
        this.fieldValue = fieldValue
    }

    String getFieldValue() {
        return this.fieldValue
    }

    String toString() {
        return this.fieldValue
    }
}
