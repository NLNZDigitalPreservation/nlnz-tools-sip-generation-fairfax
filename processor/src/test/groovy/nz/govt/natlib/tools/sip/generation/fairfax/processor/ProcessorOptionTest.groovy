package nz.govt.natlib.tools.sip.generation.fairfax.processor

import org.junit.Test

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat

class ProcessorOptionTest {
    @Test
    void correctlyMergesOverridesWhenOverrideExists() {
        List<ProcessorOption> current = [ProcessorOption.SearchSubdirectories ]
        List<ProcessorOption> overrides = [ ProcessorOption.RootFolderOnly ]
        List<ProcessorOption> merged = ProcessorOption.mergeOverrides(current, overrides)

        List<ProcessorOption> expected = [ ProcessorOption.RootFolderOnly ]
        assertThat("ProcessorOption merges correctly", merged, is(expected))
    }

    @Test
    void correctlyMergesOverridesWhenOverrideDoesNotExist() {
        List<ProcessorOption> current = [ ProcessorOption.UseSourceSubdirectoryAsTarget ]
        List<ProcessorOption> overrides = [ ProcessorOption.RootFolderOnly ]
        List<ProcessorOption> merged = ProcessorOption.mergeOverrides(current, overrides)

        List<ProcessorOption> expected = [ ProcessorOption.UseSourceSubdirectoryAsTarget, ProcessorOption.RootFolderOnly ]
        assertThat("ProcessorOption merges correctly", merged, is(expected))
    }

    @Test
    void extractsCorrectlyWithoutDefaults() {
        List<ProcessorOption> options = ProcessorOption.extract("search_subdirectories,use_source_subdirectory_as_target",
                ",", [ ], true)
        List<ProcessorOption> expected = [ProcessorOption.SearchSubdirectories, ProcessorOption.UseSourceSubdirectoryAsTarget ]
        assertThat("ProcessorOption extracted correctly", options, is(expected))
    }

    @Test
    void extractsCorrectlyWithDefaults() {
        List<ProcessorOption> options = ProcessorOption.extract("root_folder_only",
                ",", [ ProcessorOption.UseSourceSubdirectoryAsTarget ], true)
        List<ProcessorOption> expected = [ ProcessorOption.UseSourceSubdirectoryAsTarget, ProcessorOption.RootFolderOnly ]
        assertThat("ProcessorOption extracted correctly", options, is(expected))
    }

    @Test
    void extractsCorrectlyWithEmptyListAndDefaults() {
        List<ProcessorOption> options = ProcessorOption.extract("",
                ",", [ ProcessorOption.SearchSubdirectories ], true)
        List<ProcessorOption> expected = [ ProcessorOption.SearchSubdirectories ]
        assertThat("ProcessorOption extracted correctly", options, is(expected))
    }

    @Test(expected = ProcessorException.class)
    void throwsExceptionWithUnrecognizedRule() {
        List<ProcessorOption> options = ProcessorOption.extract("root_folder_only,unrecognized_rule", ",", [ ], true)
        assertThat("This point should not be reached", options, is([ ProcessorOption.RootFolderOnly]))
    }

}
