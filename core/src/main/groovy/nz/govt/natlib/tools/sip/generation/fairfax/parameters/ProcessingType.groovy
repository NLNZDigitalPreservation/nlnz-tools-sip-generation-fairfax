package nz.govt.natlib.tools.sip.generation.fairfax.parameters

import groovy.util.logging.Log4j2
import nz.govt.natlib.tools.sip.state.SipProcessingException

@Log4j2
enum ProcessingType {
    // Note that declaration order is the sorting order
    ParentGroupingWithEdition("parent_grouping_with_edition",
            [ ProcessingRule.SkipIgnored, ProcessingRule.SkipUnrecognised, ProcessingRule.SkipInvalid,
              ProcessingRule.Automatic,
              ProcessingRule.AllSectionsInSipRequired, ProcessingRule.MissingSequenceError,
              ProcessingRule.MissingSequenceDoubleWideIgnored, ProcessingRule.IgnoreEditionsWithoutMatchingFiles,
              ProcessingRule.ZeroLengthPdfReplacedWithPageUnavailablePdf, ProcessingRule.DoNotForceSkip,
              ProcessingRule.NumericStartsInHundredsNotConsideredSequenceSkips ],
            [ ProcessingOption.NumericBeforeAlphaSequencing, ProcessingOption.GenerateProcessedPdfThumbnailsPage,
              ProcessingOption.SkipThumbnailPageGenerationWhenNoErrors,
              ProcessingOption.UseInMemoryPdfToThumbnailGeneration ]),
    ParentGrouping("parent_grouping",
            [ ProcessingRule.SkipIgnored, ProcessingRule.SkipUnrecognised, ProcessingRule.SkipInvalid,
              ProcessingRule.Automatic,
              ProcessingRule.AllSectionsInSipRequired, ProcessingRule.MissingSequenceError,
              ProcessingRule.MissingSequenceDoubleWideIgnored, ProcessingRule.IgnoreEditionsWithoutMatchingFiles,
              ProcessingRule.ZeroLengthPdfReplacedWithPageUnavailablePdf, ProcessingRule.DoNotForceSkip,
              ProcessingRule.NumericStartsInHundredsNotConsideredSequenceSkips ],
            [ ProcessingOption.NumericBeforeAlphaSequencing, ProcessingOption.GenerateProcessedPdfThumbnailsPage,
              ProcessingOption.SkipThumbnailPageGenerationWhenNoErrors,
              ProcessingOption.UseInMemoryPdfToThumbnailGeneration ]),
    SupplementGrouping("supplement_grouping",
            [ ProcessingRule.SkipIgnored, ProcessingRule.SkipUnrecognised, ProcessingRule.SkipInvalid,
              ProcessingRule.Automatic,
              ProcessingRule.AllSectionsInSipOptional, ProcessingRule.MissingSequenceError,
              ProcessingRule.MissingSequenceDoubleWideIgnored, ProcessingRule.IgnoreEditionsWithoutMatchingFiles,
              ProcessingRule.ZeroLengthPdfReplacedWithPageUnavailablePdf, ProcessingRule.DoNotForceSkip,
              ProcessingRule.NumericStartsInHundredsNotConsideredSequenceSkips ],
            [ ProcessingOption.NumericBeforeAlphaSequencing, ProcessingOption.GenerateProcessedPdfThumbnailsPage,
              ProcessingOption.SkipThumbnailPageGenerationWhenNoErrors,
              ProcessingOption.UseInMemoryPdfToThumbnailGeneration ]),
    CreateSipForFolder("create_sip_for_folder",
            [ ProcessingRule.SkipIgnored, ProcessingRule.SkipUnrecognised, ProcessingRule.SkipInvalid,
              ProcessingRule.Automatic,
              ProcessingRule.AllSectionsInSipRequired, ProcessingRule.MissingSequenceError,
              ProcessingRule.MissingSequenceDoubleWideIgnored, ProcessingRule.IgnoreEditionsWithoutMatchingFiles,
              ProcessingRule.ZeroLengthPdfReplacedWithPageUnavailablePdf, ProcessingRule.DoNotForceSkip,
              ProcessingRule.NumericStartsInHundredsNotConsideredSequenceSkips ],
            [ ProcessingOption.NumericBeforeAlphaSequencing, ProcessingOption.GenerateProcessedPdfThumbnailsPage,
              ProcessingOption.AlwaysGenerateThumbnailPage, ProcessingOption.UseInMemoryPdfToThumbnailGeneration ])

    private static final Map<String, ProcessingType> LOOKUP_BY_FIELD_VALUE = [ : ]
    private final String fieldValue
    private final List<ProcessingRule> defaultRules
    private final List<ProcessingOption> defaultOptions

    static {
        values().each { ProcessingType processingType ->
            LOOKUP_BY_FIELD_VALUE.put(processingType.fieldValue, processingType)
        }
    }

    static List<ProcessingType> extract(String list, String separator = ",",
                                        boolean exceptionIfUnrecognized = false) {
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
                    String message = "Unable to match processing type=${strippedValue} to a ProcessingType enum value."
                    log.warn(message)
                    if (exceptionIfUnrecognized) {
                        throw new SipProcessingException(message)
                    }
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

    ProcessingType(String fieldValue, List<ProcessingRule> defaultRules, List<ProcessingOption> defaultOptions) {
        this.fieldValue = fieldValue
        this.defaultRules = defaultRules
        this.defaultOptions = defaultOptions
    }

    String getFieldValue() {
        return this.fieldValue
    }

    List<ProcessingRule> getDefaultRules() {
        return this.defaultRules
    }

    List<ProcessingOption> getDefaultOptions() {
        return this.defaultOptions
    }

    String toString() {
        return this.fieldValue
    }
}
