package nz.govt.natlib.tools.sip.generation.fairfax.parameters

import nz.govt.natlib.tools.sip.state.SipProcessingException
import org.junit.Test

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat

class ProcessingOptionTest {

    @Test
    void correctlyMergesOverridesWhenOverrideExists() {
        List<ProcessingOption> current = [ ProcessingOption.AlphaBeforeNumericSequencing, ProcessingOption.GenerateProcessedPdfThumbnailsPage ]
        List<ProcessingOption> overrides = [ ProcessingOption.NumericBeforeAlphaSequencing ]
        List<ProcessingOption> merged = ProcessingOption.mergeOverrides(current, overrides)

        List<ProcessingOption> expected = [ ProcessingOption.NumericBeforeAlphaSequencing, ProcessingOption.GenerateProcessedPdfThumbnailsPage ]
        assertThat("ProcessingOption merges correctly", merged, is(expected))
    }

    @Test
    void correctlyMergesOverridesWhenOverrideDoesNotExist() {
        List<ProcessingOption> current = [ ProcessingOption.GenerateProcessedPdfThumbnailsPage ]
        List<ProcessingOption> overrides = [ ProcessingOption.NumericBeforeAlphaSequencing ]
        List<ProcessingOption> merged = ProcessingOption.mergeOverrides(current, overrides)

        List<ProcessingOption> expected = [ ProcessingOption.GenerateProcessedPdfThumbnailsPage, ProcessingOption.NumericBeforeAlphaSequencing ]
        assertThat("ProcessingOption merges correctly", merged, is(expected))
    }

    @Test
    void extractsCorrectlyWithoutDefaults() {
        List<ProcessingOption> options = ProcessingOption.extract("alpha_before_numeric,generate_processed_pdf_thumbnails_page",
                ",", [ ], true)
        List<ProcessingOption> expected = [ ProcessingOption.AlphaBeforeNumericSequencing, ProcessingOption.GenerateProcessedPdfThumbnailsPage ]
        assertThat("ProcessingOption extracted correctly", options, is(expected))
    }

    @Test
    void extractsCorrectlyWithDefaults() {
        List<ProcessingOption> options = ProcessingOption.extract("generate_processed_pdf_thumbnails_page",
                ",", [ ProcessingOption.NumericBeforeAlphaSequencing ], true)
        List<ProcessingOption> expected = [ ProcessingOption.NumericBeforeAlphaSequencing, ProcessingOption.GenerateProcessedPdfThumbnailsPage ]
        assertThat("ProcessingOption extracted correctly", options, is(expected))
    }

    @Test
    void extractsCorrectlyWithEmptyListAndDefaults() {
        List<ProcessingOption> options = ProcessingOption.extract("",
                ",", [ ProcessingOption.NumericBeforeAlphaSequencing ], true)
        List<ProcessingOption> expected = [ ProcessingOption.NumericBeforeAlphaSequencing ]
        assertThat("ProcessingOption extracted correctly", options, is(expected))
    }

    @Test(expected = SipProcessingException.class)
    void throwsExceptionWithUnrecognizedRule() {
        List<ProcessingOption> options = ProcessingOption.extract("alpha_before_numeric,unrecognized_rule", ",", [ ], true)
        assertThat("This point should not be reached", options, is([ ProcessingOption.AlphaBeforeNumericSequencing]))
    }

}
