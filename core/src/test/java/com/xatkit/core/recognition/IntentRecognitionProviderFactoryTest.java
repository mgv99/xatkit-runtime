package com.xatkit.core.recognition;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.recognition.dialogflow.DialogFlowApi;
import com.xatkit.core.recognition.dialogflow.DialogFlowApiTest;
import com.xatkit.core.recognition.processor.IntentPostProcessor;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.core.server.XatkitServer;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IntentRecognitionProviderFactoryTest extends AbstractXatkitTest {

    private IntentRecognitionProvider provider;

    private XatkitCore xatkitCore;

    @Before
    public void setUp() {
        xatkitCore = mock(XatkitCore.class);
        when(xatkitCore.getEventDefinitionRegistry()).thenReturn(new EventDefinitionRegistry());
        when(xatkitCore.getXatkitServer()).thenReturn(mock(XatkitServer.class));
    }

    @After
    public void tearDown() {
        if (nonNull(provider) && !provider.isShutdown()) {
            provider.shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void getIntentRecognitionProviderNullXatkitCore() {
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void getIntentRecognitionProviderNullConfiguration() {
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitCore, null);
    }

    @Test
    public void getIntentRecognitionProviderDialogFlowProperties() {
        /*
         * Use DialogFlowApiTest.buildConfiguration to get a valid configuration (with a valid path to a credentials
         * file)
         */
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitCore,
                DialogFlowApiTest.buildConfiguration());
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DialogFlowApi").isInstanceOf(DialogFlowApi.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is not null").isNotNull();
        assertThat(provider.getPreProcessors()).as("PreProcessor list is empty").isEmpty();
        assertThat(provider.getPostProcessors()).as("PostProcessor list is empty").isEmpty();
    }

    @Test
    public void getIntentRecognitionProviderDialogFlowPropertiesDisabledAnalytics() {
        Configuration configuration = DialogFlowApiTest.buildConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactoryConfiguration.ENABLE_RECOGNITION_ANALYTICS, false);
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitCore, configuration);
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a DialogFlowApi").isInstanceOf(DialogFlowApi.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is null").isNull();
        assertThat(provider.getPreProcessors()).as("PreProcessor list is empty").isEmpty();
        assertThat(provider.getPostProcessors()).as("PostProcessor list is empty").isEmpty();
    }

    @Test
    public void getIntentRecognitionProviderEmptyConfiguration() {
        /*
         * The factory should return a RegExIntentRecognitionProvider if the provided configuration does not
         * contain any IntentRecognitionProvider property.
         */
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider
                (xatkitCore, new BaseConfiguration());
        assertThat(provider).as("Not null IntentRecognitionProvider").isNotNull();
        assertThat(provider).as("IntentRecognitionProvider is a RegExIntentRecognitionProvider").isInstanceOf
                (RegExIntentRecognitionProvider.class);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is not null").isNotNull();
        assertThat(provider.getPreProcessors()).as("PreProcessor list is empty").isEmpty();
        assertThat(provider.getPostProcessors()).as("PostProcessor list is empty").isEmpty();
    }

    @Test
    public void getIntentRecognitionProviderEmptyConfigurationDisableAnalytics() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactoryConfiguration.ENABLE_RECOGNITION_ANALYTICS, false);
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitCore, configuration);
        assertThat(provider.getRecognitionMonitor()).as("Recognition monitor is null").isNull();
        assertThat(provider.getPreProcessors()).as("PreProcessor list is empty").isEmpty();
        assertThat(provider.getPostProcessors()).as("PostProcessor list is empty").isEmpty();
    }

    @Ignore
    @Test
    public void getIntentRecognitionProviderEmptyConfigurationPreProcessor() {
        // TODO when at least one pre-processor is implemented in xatkit-runtime
    }

    @Test
    public void getIntentRecognitionProviderEmptyConfigurationPostProcessor() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(IntentRecognitionProviderFactoryConfiguration.RECOGNITION_POSTPROCESSORS_KEY,
                "RemoveEnglishStopWords");
        provider = IntentRecognitionProviderFactory.getIntentRecognitionProvider(xatkitCore, configuration);
        assertThat(provider.getPostProcessors()).as("PostProcessor list contains 1 element").hasSize(1);
        IntentPostProcessor postProcessor = provider.getPostProcessors().get(0);
        assertThat(postProcessor.getClass().getSimpleName()).as("Valid PostProcessor").isEqualTo(
                "RemoveEnglishStopWordsPostProcessor");
    }
}
