package com.xatkit.util;

import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.State;
import com.xatkit.execution.Transition;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.xbase.XFeatureCall;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * An utility class the eases the access to {@link ExecutionModel}'s features.
 * <p>
 * The singleton instance of this class is initialized from an {@link ExecutionModel} instance, and provide
 * high-level method to retrieve <i>Init</i> and <i>Default_Fallback</i> states, compute top-level
 * {@link IntentDefinition}, or retrieve accessed {@link EventDefinition}.
 */
public class ExecutionModelHelper {

    /**
     * The singleton instance of this class.
     */
    private static ExecutionModelHelper INSTANCE = null;

    /**
     * Initializes the {@link ExecutionModelHelper} singleton instance with the provided {@code executionModel}.
     * <p>
     * This method must be called before any access to the singleton instance.
     *
     * @param executionModel the {@link ExecutionModel} used to initialize the singleton instance
     * @return the created {@link ExecutionModelHelper}
     */
    public static ExecutionModelHelper create(ExecutionModel executionModel) {
        if (nonNull(INSTANCE)) {
            throw new IllegalStateException("Cannot create an ExecutionModelHelper, it already exists");
        } else {
            INSTANCE = new ExecutionModelHelper(executionModel);
            return INSTANCE;
        }
    }

    /**
     * Returns the singleton instance of {@link ExecutionModelHelper}.
     * <p>
     * The singleton instance must have been initialized with {@link ExecutionModelHelper#create(ExecutionModel)}
     * before accessing it.
     *
     * @return the singleton instance of {@link ExecutionModelHelper}
     * @throws IllegalStateException if the singleton instance has not been initialized
     * @see #create(ExecutionModel)
     */
    public static ExecutionModelHelper getInstance() {
        if (isNull(INSTANCE)) {
            throw new IllegalStateException(MessageFormat.format("Cannot access the {0} instance, the {0} has not " +
                    "been initialized. Use {0}.create to initialize it.", ExecutionModelHelper.class.getSimpleName()));
        }
        return INSTANCE;
    }

    /**
     * The underlying {@link ExecutionModel}.
     */
    private ExecutionModel executionModel;

    /**
     * The <i>Init</i> {@link State} of the {@link ExecutionModel}.
     */
    private State initState;

    /**
     * The <i>Default_Fallback</i> {@link State} of the {@link ExecutionModel}.
     */
    private State fallbackState;

    /**
     * The cached collection of top-level {@link IntentDefinition} associated to the {@link ExecutionModel}.
     * <p>
     * Top-level intents are intents that do not require any context to get matched. They are defined as part of the
     * <i>Init</i> state's transitions, or in states that can be reached from the <i>Init</i> through wildcard
     * transitions.
     */
    private Collection<IntentDefinition> topLevelIntents;

    /**
     * The cached collection of all accessed {@link EventDefinition}.
     */
    private Collection<EventDefinition> allAccessedEvents;

    /**
     * Initializes the {@link ExecutionModelHelper} with the provided {@link ExecutionModel}.
     * <p>
     * This constructor retrieves the <i>Init</i> and <i>Default_Fallback</i> {@link State}s from the provided
     * {@link ExecutionModel}, and compute high-level information such as the top-level {@link IntentDefinition}s or
     * the accessed {@link EventDefinition}.
     *
     * @param executionModel the {@link ExecutionModel} used to initialize the helper
     */
    private ExecutionModelHelper(ExecutionModel executionModel) {
        this.executionModel = executionModel;
        for (State s : executionModel.getStates()) {
            if (s.getName().equals("Init")) {
                this.initState = s;
            } else if (s.getName().equals("Default_Fallback")) {
                this.fallbackState = s;
            }
        }
        this.topLevelIntents = computeTopLevelIntents();
        this.allAccessedEvents = getAccessedEvents(executionModel::eAllContents);
    }

    /**
     * Returns the underlying {@link ExecutionModel}.
     *
     * @return the underlying {@link ExecutionModel}
     */
    public ExecutionModel getExecutionModel() {
        return this.executionModel;
    }

    /**
     * Returns the <i>Init</i> {@link State} of the {@link ExecutionModel}.
     *
     * @return the <i>Init</i> {@link State} of the {@link ExecutionModel}
     */
    public State getInitState() {
        return this.initState;
    }

    /**
     * Returns the <i>Default_Fallback</i> {@link State} of the {@link ExecutionModel}.
     *
     * @return the <i>Default_Fallback</i> {@link State} of the {@link ExecutionModel}.
     */
    public State getFallbackState() {
        return this.fallbackState;
    }

    /**
     * Returns the top-level {@link IntentDefinition}s of the {@link ExecutionModel}.
     * <p>
     * Top-level intents are intents that do not require any context to get matched. They are defined as part of the
     * <i>Init</i> state's transitions, or in states that can be reached from the <i>Init</i> through wildcard
     * transitions.
     *
     * @return the top-level {@link IntentDefinition}s of the {@link ExecutionModel}.
     */
    public Collection<IntentDefinition> getTopLevelIntents() {
        return this.topLevelIntents;
    }

    /**
     * Returns all the {@link EventDefinition}s accessed in the {@link ExecutionModel}.
     * <p>
     * This method does not filter where the {@link EventDefinition}s are accessed. If you need precise filtering you
     * can check {@link #getAccessedEvents(Transition)} to retrieve the {@link EventDefinition}s accessed in a
     * {@link Transition}.
     *
     * @return the {@link EventDefinition}s accessed in the {@link ExecutionModel}
     * @see #getAccessedEvents(Transition)
     */
    public Collection<EventDefinition> getAllAccessedEvents() {
        return this.allAccessedEvents;
    }

    /**
     * Retrieves the top-level {@link IntentDefinition}s of the {@link ExecutionModel}.
     * <p>
     * Top-level intents are intents that do not require any context to get matched. They are defined as part of the
     * <i>Init</i> state's transitions, or in states that can be reached from the <i>Init</i> through wildcard
     * transitions.
     *
     * @return the top-level {@link IntentDefinition}s of the {@link ExecutionModel}
     */
    private Collection<IntentDefinition> computeTopLevelIntents() {
        Set<IntentDefinition> result = new HashSet<>();
        Collection<State> topLevelStates = getStatesReachableWithWildcard(initState);
        topLevelStates.stream().flatMap(state -> state.getTransitions().stream()).forEach(t -> {
            getAccessedEvents(t).forEach(e -> {
                if (e instanceof IntentDefinition) {
                    result.add((IntentDefinition) e);
                }
            });
        });
        return topLevelIntents;
    }

    /**
     * Returns the {@link State}s that can be reached from the provided {@code state} with wildcard {@link Transition}s.
     *
     * @param state the {@link State} to use to start the search
     * @return the {@link State}s that can be reached from the provided {@code state} with wildcard {@link Transition}s
     * @see Transition#isIsWildcard()
     * @see #getStatesReachableWithWildcard(State, Set)
     */
    private Collection<State> getStatesReachableWithWildcard(State state) {
        return getStatesReachableWithWildcard(state, new HashSet<>());
    }

    /**
     * Returns the {@link State}s that can be reached from the provided {@code state} with wildcard {@link Transition}s.
     * <p>
     * This method is a recursive implementation of {@link #getStatesReachableWithWildcard(State)} that uses the
     * {@code result} {@link Set} as an accumulator.
     *
     * @param state  the {@link State} to use to start the search
     * @param result the {@link Set} used to gather the retrieved {@link State}s
     * @return the {@link State}s that can be reached from the provided {@code state} with wildcard {@link Transition}s
     */
    private Collection<State> getStatesReachableWithWildcard(State state, Set<State> result) {
        boolean added = result.add(state);
        if (added) {
            if (state.getTransitions().stream().anyMatch(Transition::isIsWildcard)) {
                /*
                 * We assume here there is only one state
                 */
                State otherState = state.getTransitions().get(0).getState();
                return getStatesReachableWithWildcard(otherState, result);
            } else {
                return result;
            }
        } else {
            /*
             * Break infinite loops (we have already added the state, no need to check its transitions)
             */
            return result;
        }
    }

    /**
     * Returns the {@link EventDefinition}s accessed from the provided {@link Transition}.
     * <p>
     * This method looks in the content tree of the provided {@link Transition} to retrieve the
     * {@link EventDefinition} accesses. This means that {@link EventDefinition} accesses defined as part of complex
     * conditions will be retrieved by this method.
     *
     * @param transition the {@link Transition} to retrieve the {@link EventDefinition} accesses from
     * @return the {@link EventDefinition}s accessed in the provided {@link Transition}
     */
    public Collection<EventDefinition> getAccessedEvents(Transition transition) {
        Iterable<EObject> transitionContents = transition::eAllContents;
        return getAccessedEvents(transitionContents);
    }

    /**
     * Returns all the {@link EventDefinition} accesses from the provided {@link EObject}s.
     * <p>
     * This method does not iterate the content tree of the provided {@link EObject}.
     *
     * @param eObjects the {@link EObject}s to retrieve the {@link EventDefinition} accesses from
     * @return the accessed {@link EventDefinition}s
     */
    private Collection<EventDefinition> getAccessedEvents(Iterable<EObject> eObjects) {
        Set<EventDefinition> result = new HashSet<>();
        for (EObject e : eObjects) {
            if (e instanceof XFeatureCall) {
                XFeatureCall featureCall = (XFeatureCall) e;
                if (isEventDefinitionAccess(featureCall.getFeature())) {
                    EventDefinition eventDefinition = getAccessedEventDefinition(featureCall.getFeature());
                    if (nonNull(eventDefinition)) {
                        result.add(eventDefinition);
                    } else {
                        throw new RuntimeException(MessageFormat.format("Cannot retrieve the {0} from the provided " +
                                        "{1} {2}", EventDefinition.class.getSimpleName(),
                                featureCall.getFeature().getClass().getSimpleName(), featureCall.getFeature()));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if the provided {@link JvmIdentifiableElement} is an {@link EventDefinition} access.
     * <p>
     * This method checks if the provided {@code element} corresponds to an access (in the execution language) to a
     * class derived from the imported events. Such accesses are typically performed in transition guards:
     * {@code
     * <pre>
     * MyState {
     *     Next {
     *         intent == MyIntent --> OtherState
     *     }
     * }
     * </pre>
     * }
     * The inferrer allows such accesses by deriving a class {@code MyIntent} from the imported
     * {@link com.xatkit.intent.IntentDefinition}s.
     * <p>
     * See {@link #getAccessedEventDefinition(JvmIdentifiableElement)} to retrieve the accessed {@link EventDefinition}.
     *
     * @param element the {@link JvmIdentifiableElement} to check
     * @return {@code true} if the provided {@code element} is an {@link EventDefinition} access, {@code false}
     * otherwise
     * @see #getAccessedEventDefinition(JvmIdentifiableElement)
     */
    private boolean isEventDefinitionAccess(JvmIdentifiableElement element) {
        if (element instanceof JvmGenericType) {
            JvmGenericType typeFeature = (JvmGenericType) element;
            if (typeFeature.getSuperTypes().stream().anyMatch(t -> t.getIdentifier().equals(EventDefinition.class.getName()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the {@link EventDefinition} associated to the provided {@link JvmIdentifiableElement}.
     * <p>
     * This method checks if the provided {@code element} corresponds to an access (in the execution language) to a
     * class derived from the imported events, and returns the events. Such accesses are typically performed in
     * transition guards:
     * {@code
     * <pre>
     * MyState {
     *     Next {
     *         intent == MyIntent --> OtherState
     *     }
     * }
     * </pre>
     * }
     * The inferrer allows such accesses by deriving a class {@code MyIntent} from the imported
     * {@link com.xatkit.intent.IntentDefinition}s.
     * <p>
     * See {@link #isEventDefinitionAccess(JvmIdentifiableElement)} to check whether {@code element} is an
     * {@link EventDefinition} access.
     *
     * @param element the {@link JvmIdentifiableElement} to check
     * @return the associated {@link EventDefinition} if it exists, {@code null} otherwise
     * @see #isEventDefinitionAccess(JvmIdentifiableElement)
     */
    private @Nullable
    EventDefinition getAccessedEventDefinition(JvmIdentifiableElement element) {
        if (element instanceof JvmGenericType) {
            JvmGenericType typeFeature = (JvmGenericType) element;
            if (typeFeature.getSuperTypes().stream().anyMatch(t -> t.getIdentifier().equals(EventDefinition.class.getName()))) {
                Optional<JvmMember> field =
                        typeFeature.getMembers().stream().filter(m -> m instanceof JvmField && m.getSimpleName().equals("base")).findAny();
                if (field.isPresent()) {
                    return (EventDefinition) ((JvmField) field.get()).getConstantValue();
                } else {
                    throw new RuntimeException(MessageFormat.format("Cannot find the static field {0}.{1}, this field" +
                            " should have been set during Xtext parsing", element.getSimpleName(), "base"));
                }
            }
        }
        return null;
    }
}
