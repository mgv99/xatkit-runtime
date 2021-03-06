package com.xatkit.dsl;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.dsl.intent.IntentVar;
import com.xatkit.dsl.library.LibraryProvider;
import com.xatkit.dsl.state.StateVar;
import com.xatkit.execution.ExecutionModel;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import static com.xatkit.dsl.DSL.intent;
import static com.xatkit.dsl.DSL.library;
import static com.xatkit.dsl.DSL.model;
import static com.xatkit.dsl.DSL.state;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ExecutionModelTest {

    private StateVar s1;

    private StateVar s2;

    private IntentVar i1;

    private IntentVar i2;

    private LibraryProvider library;

    private RuntimePlatform platform = mock(RuntimePlatform.class);

    private RuntimeEventProvider provider = mock(RuntimeEventProvider.class);

    @Before
    public void setUp() {
        i1 = intent("Intent1")
                .trainingSentence("Hello");

        i2 = intent("Intent2")
                .trainingSentence("Bye");

        s1 = state("S1");
        s2 = state("S2");

        s1
                .body(c -> System.out.println("S1 body"))
                .next()
                    .moveTo(s2);
        s2
                .body(c -> System.out.println("S2 body"))
                .next()
                    .moveTo(s1);

        library = library("Library")
                .intent(i1)
                .intent(i2);

    }

    @Test
    public void createModelWithIntents() {
        val model = model()
                .useEvent(i1)
                .useEvent(i2)
                .listenTo(provider)
                .initState(s1)
                .defaultFallbackState(s2);

        ExecutionModel base = model.getExecutionModel();
        assertThat(base.getUsedEvents()).hasSize(2);
        assertThat(base.getUsedEvents()).anyMatch(e -> e.getName().equals("Intent1"));
        assertThat(base.getUsedEvents()).anyMatch(e -> e.getName().equals("Intent2"));
    }

    @Test
    public void createModelWithLibrary() {
        val model = model()
                .useIntents(library)
                .listenTo(provider)
                .initState(s1)
                .defaultFallbackState(s2);

        ExecutionModel base = model.getExecutionModel();
        assertThat(base.getUsedEvents()).hasSize(2);
        assertThat(base.getUsedEvents()).anyMatch(e -> e.getName().equals("Intent1"));
        assertThat(base.getUsedEvents()).anyMatch(e -> e.getName().equals("Intent2"));
    }

    @Test
    public void createModelWithStates() {
        val model = model()
                .listenTo(provider)
                .useState(s1)
                .useState(s2)
                .initState(s1)
                .defaultFallbackState(s2);

        ExecutionModel base = model.getExecutionModel();
        assertThat(base.getStates()).hasSize(2);
        assertThat(base.getStates()).anyMatch(s -> s.getName().equals("S1"));
        assertThat(base.getStates()).anyMatch(s -> s.getName().equals("S2"));
    }

    @Test
    public void createModelWithPlatform() {
        val model = model()
                .usePlatform(platform)
                .listenTo(provider)
                .initState(s1)
                .defaultFallbackState(s2);

        ExecutionModel base = model.getExecutionModel();
        assertThat(base.getUsedPlatforms()).hasSize(1);
        assertThat(base.getUsedPlatforms()).contains(platform);
    }

    @Test
    public void createModelWithProvider() {
        val model = model()
                .listenTo(provider)
                .initState(s1)
                .defaultFallbackState(s2);

        ExecutionModel base = model.getExecutionModel();
        assertThat(base.getUsedProviders()).hasSize(1);
        assertThat(base.getUsedProviders()).contains(provider);
    }

}
