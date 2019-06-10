package nz.govt.natlib.tools.sip.generation.fairfax.parameters

import nz.govt.natlib.tools.sip.state.SipProcessingException
import org.junit.Test

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat

class ProcessingTypeTest {

    @Test
    void extractsCorrectly() {
        List<ProcessingType> types = ProcessingType.extract("parent_grouping,parent_grouping_with_edition",
                ",", true)
        List<ProcessingType> expected = [ ProcessingType.ParentGrouping, ProcessingType.ParentGroupingWithEdition ]
        assertThat("ProcessingRule extracted correctly", types, is(expected))
    }

    @Test
    void sortsCorrectly() {
        List<ProcessingType> unsorted = [ ProcessingType.CreateSipForFolder, ProcessingType.ParentGrouping,
                                          ProcessingType.SupplementGrouping, ProcessingType.CreateSipForFolder,
                                          ProcessingType.ParentGroupingWithEdition ]
        List<ProcessingType> expected = [ ProcessingType.ParentGroupingWithEdition, ProcessingType.ParentGrouping,
                                          ProcessingType.SupplementGrouping, ProcessingType.CreateSipForFolder,
                                          ProcessingType.CreateSipForFolder ]
        assertThat("ProcessingRule extracted with defaults correctly", unsorted.sort(), is(expected))
    }

    @Test(expected = SipProcessingException.class)
    void throwsExceptionWithUnrecognizedType() {
        List<ProcessingType> types = ProcessingType.extract("supplement_grouping,unrecognized_type", ",",true)
        assertThat("This point should not be reached", types, is([ ProcessingType.ParentGrouping ]))
    }
}
