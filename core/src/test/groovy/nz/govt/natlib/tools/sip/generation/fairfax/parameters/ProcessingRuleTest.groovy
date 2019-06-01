package nz.govt.natlib.tools.sip.generation.fairfax.parameters

import nz.govt.natlib.tools.sip.state.SipProcessingException
import org.junit.Test

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat

class ProcessingRuleTest {

    @Test
    void correctlyMergesOverrides() {
        List<ProcessingRule> current = [ ProcessingRule.MultipleEditions, ProcessingRule.HandleUnrecognised ]
        List<ProcessingRule> overrides = [ ProcessingRule.SingleEdition ]
        List<ProcessingRule> merged = ProcessingRule.mergeOverrides(current, overrides)

        List<ProcessingRule> expected = [ ProcessingRule.SingleEdition, ProcessingRule.HandleUnrecognised ]
        assertThat("ProcessingRule merges correctly", merged, is(expected))
    }

    @Test(expected = SipProcessingException.class)
    void throwsExceptionWithUnrecognizedRule() {
        List<ProcessingRule> rules = ProcessingRule.extract("multiple_editions,unrecognized_rule", ",", true)
        assertThat("This point should not be reached", rules, is([ ProcessingRule.MultipleEditions]))
    }
}
