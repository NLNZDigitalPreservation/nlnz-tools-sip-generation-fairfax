package nz.govt.natlib.tools.sip.generation.fairfax.parameters

import nz.govt.natlib.tools.sip.state.SipProcessingException
import org.junit.Test

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat

class ProcessingOptionTest {

    @Test
    void correctlyMergesOverrides() {
        List<ProcessingOption> current = [ ProcessingOption.AlphaBeforeNumericSequencing, ProcessingOption.AnyFirstSectionCode ]
        List<ProcessingOption> overrides = [ ProcessingOption.NumericBeforeAlphaSequencing ]
        List<ProcessingOption> merged = ProcessingOption.mergeOverrides(current, overrides)

        List<ProcessingOption> expected = [ ProcessingOption.NumericBeforeAlphaSequencing, ProcessingOption.AnyFirstSectionCode ]
        assertThat("ProcessingOption merges correctly", merged, is(expected))
    }

    @Test(expected = SipProcessingException.class)
    void throwsExceptionWithUnrecognizedRule() {
        List<ProcessingOption> options = ProcessingOption.extract("alpha_before_numeric,unrecognized_rule", ",", true)
        assertThat("This point should not be reached", options, is([ ProcessingOption.AlphaBeforeNumericSequencing]))
    }

}
