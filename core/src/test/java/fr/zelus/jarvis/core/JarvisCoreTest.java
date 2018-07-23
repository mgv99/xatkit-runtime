package fr.zelus.jarvis.core;

import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.dialogflow.DialogFlowApi;
import fr.zelus.jarvis.intent.EventDefinition;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.intent.IntentDefinition;
import fr.zelus.jarvis.intent.IntentFactory;
import fr.zelus.jarvis.module.*;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationFactory;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import fr.zelus.jarvis.stubs.StubJarvisModule;
import fr.zelus.jarvis.stubs.io.StubJsonWebhookEventProvider;
import fr.zelus.jarvis.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class JarvisCoreTest {

    protected static String VALID_PROJECT_ID = VariableLoaderHelper.getJarvisDialogFlowProject();

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static OrchestrationModel VALID_ORCHESTRATION_MODEL;

    protected static EventInstance VALID_EVENT_INSTANCE;

    protected JarvisCore jarvisCore;

    public static Configuration buildConfiguration(String projectId, String languageCode, Object orchestrationModel) {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowApi.PROJECT_ID_KEY, projectId);
        configuration.addProperty(DialogFlowApi.LANGUAGE_CODE_KEY, languageCode);
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, orchestrationModel);
        return configuration;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        Module stubModule = ModuleFactory.eINSTANCE.createModule();
        stubModule.setName("StubJarvisModule");
        stubModule.setJarvisModulePath("fr.zelus.jarvis.stubs.StubJarvisModule");
        Action stubAction = ModuleFactory.eINSTANCE.createAction();
        stubAction.setName("StubJarvisAction");
        // No parameters, keep it simple
        stubModule.getActions().add(stubAction);
        InputProviderDefinition stubInputProvider = ModuleFactory.eINSTANCE.createInputProviderDefinition();
        stubInputProvider.setName("StubInputProvider");
        stubModule.getEventProviderDefinitions().add(stubInputProvider);
        IntentDefinition stubIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        stubIntentDefinition.setName("Default Welcome Intent");
        /*
         * Create the valid EventInstance used in handleEvent tests
         */
        VALID_EVENT_INSTANCE = IntentFactory.eINSTANCE.createEventInstance();
        VALID_EVENT_INSTANCE.setDefinition(stubIntentDefinition);
        // No parameters, keep it simple
        stubModule.getIntentDefinitions().add(stubIntentDefinition);
        VALID_ORCHESTRATION_MODEL = OrchestrationFactory.eINSTANCE.createOrchestrationModel();
        OrchestrationLink link = OrchestrationFactory.eINSTANCE.createOrchestrationLink();
        link.setEvent(stubIntentDefinition);
        ActionInstance actionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(stubAction);
        link.getActions().add(actionInstance);
        VALID_ORCHESTRATION_MODEL.getOrchestrationLinks().add(link);
        /*
         * Create the Resource used to store the valid orchestration model.
         */
        ResourceSet testResourceSet = new ResourceSetImpl();
        testResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl
                ());

        Resource testIntentResource = testResourceSet.createResource(URI.createURI("/tmp/jarvisTestIntentResource" +
                ".xmi"));
        testIntentResource.getContents().clear();
        testIntentResource.getContents().add(stubModule);
        testIntentResource.save(Collections.emptyMap());

        Resource testOrchestrationResource = testResourceSet.createResource(URI.createURI
                ("/tmp/jarvisTestOrchestrationResource.xmi"));
        testOrchestrationResource.getContents().clear();
        testOrchestrationResource.getContents().add(VALID_ORCHESTRATION_MODEL);
        testOrchestrationResource.save(Collections.emptyMap());
    }

    @After
    public void tearDown() {
        if (nonNull(jarvisCore) && !jarvisCore.isShutdown()) {
            jarvisCore.shutdown();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    /**
     * Returns a valid {@link JarvisCore} instance.
     *
     * @return a valid {@link JarvisCore} instance
     */
    private JarvisCore getValidJarvisCore() {
        Configuration configuration = buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE,
                VALID_ORCHESTRATION_MODEL);
        jarvisCore = new JarvisCore(configuration);
        return jarvisCore;
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        jarvisCore = new JarvisCore(null);
    }

    @Test(expected = NullPointerException.class)
    public void constructMissingOrchestrationPathInConfiguration() {
        Configuration configuration = new BaseConfiguration();
        jarvisCore = new JarvisCore(configuration);
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowApi.PROJECT_ID_KEY, VALID_PROJECT_ID);
        configuration.addProperty(DialogFlowApi.LANGUAGE_CODE_KEY, VALID_LANGUAGE_CODE);
        configuration.addProperty(JarvisCore.ORCHESTRATION_MODEL_KEY, VALID_ORCHESTRATION_MODEL);
        jarvisCore = new JarvisCore(configuration);
        checkJarvisCore(jarvisCore);
    }

    @Test
    public void constructValidDefaultModuleConstructor() {
        /*
         * Use another OrchestrationModel linking to the StubJarvisModuleDefaultConstructor stub class, that only
         * defines a default constructor.
         */
        Module stubModule = ModuleFactory.eINSTANCE.createModule();
        stubModule.setName("StubJarvisModuleDefaultConstructor");
        stubModule.setJarvisModulePath("fr.zelus.jarvis.stubs.StubJarvisModuleDefaultConstructor");
        Action stubAction = ModuleFactory.eINSTANCE.createAction();
        stubAction.setName("StubJarvisAction");
        // No parameters, keep it simple
        stubModule.getActions().add(stubAction);
        InputProviderDefinition stubInputProvider = ModuleFactory.eINSTANCE.createInputProviderDefinition();
        stubInputProvider.setName("StubInputProvider");
        stubModule.getEventProviderDefinitions().add(stubInputProvider);
        IntentDefinition stubIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        stubIntentDefinition.setName("Default Welcome Intent");
        // No parameters, keep it simple
        stubModule.getIntentDefinitions().add(stubIntentDefinition);
        OrchestrationModel orchestrationModel = OrchestrationFactory.eINSTANCE.createOrchestrationModel();
        OrchestrationLink link = OrchestrationFactory.eINSTANCE.createOrchestrationLink();
        link.setEvent(stubIntentDefinition);
        ActionInstance actionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(stubAction);
        link.getActions().add(actionInstance);
        orchestrationModel.getOrchestrationLinks().add(link);
        jarvisCore = new JarvisCore(buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, orchestrationModel));
        checkJarvisCore(jarvisCore, orchestrationModel);
    }

    @Test
    public void constructValidWebhookEventProvider() {
        Module stubModule = ModuleFactory.eINSTANCE.createModule();
        stubModule.setName("StubJarvisModule");
        stubModule.setJarvisModulePath("fr.zelus.jarvis.stubs.StubJarvisModule");
        EventProviderDefinition stubWebhookEventProviderDefinition = ModuleFactory.eINSTANCE
                .createEventProviderDefinition();
        stubWebhookEventProviderDefinition.setName("StubJsonWebhookEventProvider");
        stubModule.getEventProviderDefinitions().add(stubWebhookEventProviderDefinition);
        OrchestrationModel orchestrationModel = OrchestrationFactory.eINSTANCE.createOrchestrationModel();
        orchestrationModel.getEventProviderDefinitions().add(stubWebhookEventProviderDefinition);
        jarvisCore = new JarvisCore(buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, orchestrationModel));
        checkJarvisCore(jarvisCore, orchestrationModel);
        assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders()).as("Server WebhookEventProvider" +
                " collection is not empty").isNotEmpty();
        assertThat(jarvisCore.getJarvisServer().getRegisteredWebhookEventProviders().iterator().next()).as("Valid " +
                "registered WebhookEventProvider").isInstanceOf(StubJsonWebhookEventProvider.class);
    }

    @Test(expected = JarvisException.class)
    public void getOrchestrationModelInvalidType() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel(new Integer(2));
    }

    @Test(expected = JarvisException.class)
    public void getOrchestrationModelFromInvalidString() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel("/tmp/test.xmi");
    }

    @Test(expected = JarvisException.class)
    public void getOrchestrationModelFromInvalidURI() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel(URI.createURI("/tmp/test.xmi"));
    }

    @Test
    public void getOrchestrationModelFromValidInMemory() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel(VALID_ORCHESTRATION_MODEL);
        assertThat(orchestrationModel).as("Valid OrchestrationModel").isEqualTo(VALID_ORCHESTRATION_MODEL);
    }

    @Test
    public void getOrchestrationModelFromValidString() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel(VALID_ORCHESTRATION_MODEL.eResource
                ().getURI().toString());
        assertThat(orchestrationModel).as("Not null OrchestrationModel").isNotNull();
        /*
         * Not enough, but comparing the entire content of the model is more complicated than it looks like.
         */
        assertThat(orchestrationModel.getOrchestrationLinks()).as("Valid OrchestrationLink size").hasSize
                (VALID_ORCHESTRATION_MODEL.getOrchestrationLinks().size());
    }

    @Test
    public void getOrchestrationModelFromValidURI() {
        jarvisCore = getValidJarvisCore();
        OrchestrationModel orchestrationModel = jarvisCore.getOrchestrationModel(VALID_ORCHESTRATION_MODEL.eResource
                ().getURI());
        assertThat(orchestrationModel).as("Not null OrchestrationModel").isNotNull();
        /*
         * Not enough, but comparing the entire content of the model is more complicated than it looks like.
         */
        assertThat(orchestrationModel.getOrchestrationLinks()).as("Valid OrchestrationLink size").hasSize
                (VALID_ORCHESTRATION_MODEL.getOrchestrationLinks().size());
    }

    @Test(expected = JarvisException.class)
    public void shutdownAlreadyShutdown() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.shutdown();
        jarvisCore.shutdown();
    }

    @Test(expected = NullPointerException.class)
    public void getOrCreateJarvisSessionNullSessionId() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.getOrCreateJarvisSession(null);
    }

    @Test
    public void getOrCreateJarvisSessionValidSessionId() {
        jarvisCore = getValidJarvisCore();
        JarvisSession session = jarvisCore.getOrCreateJarvisSession("sessionID");
        assertThat(session).as("Not null JarvisSession").isNotNull();
        /*
         * Use contains because the underlying DialogFlow API add additional identification information in the
         * returned JarvisSession.
         */
        assertThat(session.getSessionId()).as("Valid session ID").contains("sessionID");
    }

    @Test
    public void shutdown() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.shutdown();
        softly.assertThat(jarvisCore.getExecutorService().isShutdown()).as("ExecutorService is shutdown");
        softly.assertThat(jarvisCore.getDialogFlowApi().isShutdown()).as("DialogFlow API is shutdown");
        softly.assertThat(jarvisCore.getJarvisModuleRegistry().getModules()).as("Empty module registry").isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void handleEventNullEvent() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.handleEvent(null, new JarvisSession("sessionID"));
    }

    @Test(expected = NullPointerException.class)
    public void handleEventNullSession() {
        jarvisCore = getValidJarvisCore();
        jarvisCore.handleEvent(VALID_EVENT_INSTANCE, null);
    }

    @Test
    public void handleEventValidEvent() throws InterruptedException {
        jarvisCore = getValidJarvisCore();
        /*
         * It is not necessary to check the the module list is not null and contains at least one element, this is
         * done in loadModule test.
         */
        StubJarvisModule stubJarvisModule = (StubJarvisModule) jarvisCore.getJarvisModuleRegistry().getJarvisModule
                ("StubJarvisModule");
        jarvisCore.handleEvent(VALID_EVENT_INSTANCE, jarvisCore.getOrCreateJarvisSession("sessionID"));
        /*
         * Sleep to ensure that the Action has been processed.
         */
        Thread.sleep(1000);
        softly.assertThat(stubJarvisModule.getAction().isActionProcessed()).as("Action processed").isTrue();
    }

    @Test
    public void handleEventNotHandledEvent() {
        jarvisCore = getValidJarvisCore();
        StubJarvisModule stubJarvisModule = (StubJarvisModule) jarvisCore.getJarvisModuleRegistry().getJarvisModule
                ("StubJarvisModule");
        EventDefinition notHandledDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        notHandledDefinition.setName("NotHandled");
        EventInstance notHandledEventInstance = IntentFactory.eINSTANCE.createEventInstance();
        notHandledEventInstance.setDefinition(notHandledDefinition);
        jarvisCore.handleEvent(notHandledEventInstance, jarvisCore.getOrCreateJarvisSession("sessionID"));
        assertThat(stubJarvisModule.getAction().isActionProcessed()).as("Action not processed").isFalse();
    }

    /**
     * Computes a set of basic assertions on the provided {@code jarvisCore} using the
     * {@link #VALID_ORCHESTRATION_MODEL}.
     *
     * @param jarvisCore the {@link JarvisCore} instance to check
     */
    private void checkJarvisCore(JarvisCore jarvisCore) {
        checkJarvisCore(jarvisCore, VALID_ORCHESTRATION_MODEL);
    }

    /**
     * Computes a set of basic assertions on the provided {@code jarvisCore} using the provided {@code
     * orchestrationModel}.
     *
     * @param jarvisCore         the {@link JarvisCore} instance to check
     * @param orchestrationModel the {@link OrchestrationModel} to check
     */
    private void checkJarvisCore(JarvisCore jarvisCore, OrchestrationModel orchestrationModel) {
        /*
         * isNotNull() assertions are not soft, otherwise the runner does not print the assertion error and fails on
         * a NullPointerException in the following assertions.
         */
        assertThat(jarvisCore.getDialogFlowApi()).as("Not null DialogFlow API").isNotNull();
        softly.assertThat(jarvisCore.getDialogFlowApi().getProjectId()).as("Valid DialogFlowAPI project ID").isEqualTo
                (VALID_PROJECT_ID);
        softly.assertThat(jarvisCore.getDialogFlowApi().getLanguageCode()).as("Valid DialogFlowAPI language code")
                .isEqualTo(VALID_LANGUAGE_CODE);
        assertThat(jarvisCore.getOrchestrationModel()).as("Not null OrchestrationModel").isNotNull();
        softly.assertThat(jarvisCore.getOrchestrationModel()).as("Valid OrchestrationModel").isEqualTo
                (orchestrationModel);
        softly.assertThat(jarvisCore.isShutdown()).as("Not shutdown").isFalse();
        assertThat(jarvisCore.getJarvisServer()).as("Not null JarvisServer").isNotNull();
    }

}
