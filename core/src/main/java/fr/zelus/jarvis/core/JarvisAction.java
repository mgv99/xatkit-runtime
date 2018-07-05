package fr.zelus.jarvis.core;

import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;

import java.text.MessageFormat;
import java.util.concurrent.Callable;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * The concrete implementation of an {@link fr.zelus.jarvis.module.Action} definition.
 * <p>
 * A {@link JarvisAction} represents an atomic action that are automatically executed by the {@link JarvisCore}
 * component. Instances of this class are created by the associated {@link JarvisModule} from an input
 * {@link fr.zelus.jarvis.intent.RecognizedIntent}.
 * <p>
 * Note that {@link JarvisAction}s implementations must be stored in the <i>action</i> package of their associated
 * concrete {@link JarvisModule} implementation to enable their automated loading. For example, the action
 * <i>MyAction</i> defined in the module <i>myModulePackage.MyModule</i> should be stored in the package
 * <i>myModulePackage.action</i>
 *
 * @param <T> the concrete {@link JarvisModule} subclass type containing the action
 * @see fr.zelus.jarvis.module.Action
 * @see JarvisCore
 * @see JarvisModule
 */
public abstract class JarvisAction<T extends JarvisModule> implements Callable<Object> {

    /**
     * The {@link JarvisModule} subclass containing this action.
     */
    protected T module;

    /**
     * The {@link JarvisContext} associated to this action.
     */
    protected JarvisContext context;

    /**
     * The name of the variable to use to store the result of the {@link #call()} method.
     * <p>
     * The value of this attribute is used by {@link JarvisCore#handleMessage(String, JarvisSession)} to store the
     * result of each {@link JarvisAction} in the variable defined in the provided orchestration model.
     *
     * @see JarvisCore#handleMessage(String, JarvisSession)
     * @see #getReturnVariable()
     */
    protected String returnVariable;

    /**
     * Constructs a new {@link JarvisModule} with the provided {@code containingModule} and {@code context}.
     *
     * @param containingModule the {@link JarvisModule} containing this action
     * @param context          the {@link JarvisContext} associated to this action
     * @throws NullPointerException if the provided {@code containingModule} or {@code context} is {@code null}
     */
    public JarvisAction(T containingModule, JarvisContext context) {
        checkNotNull(containingModule, "Cannot construct a {0} with a null containing module", this.getClass()
                .getSimpleName());
        checkNotNull(context, "Cannot construct a %s with a null %s", this.getClass().getSimpleName(), JarvisContext
                .class.getSimpleName());
        this.module = containingModule;
        this.context = context;
    }

    public final void setReturnVariable(String variableName) {
        this.returnVariable = variableName;
    }

    /**
     * Return the name of the variable to use to store the result of the {@link #call()} method.
     * <p>
     * This method is used by {@link JarvisCore#handleMessage(String, JarvisSession)} to store the result of each
     * {@link JarvisAction} in the variable defined in the provided orchestration model.
     *
     * @return the name of the variable to use to store the result of the {@link #call()} method
     * @see JarvisCore#handleMessage(String, JarvisSession)
     */
    public final String getReturnVariable() {
        return returnVariable;
    }

    /**
     * Disable the default constructor, JarvisActions must be constructed with their containing module.
     */
    private JarvisAction() {
        /*
         * Disable the default constructor, JarvisActions must be constructed with their containing module.
         */
    }

    /**
     * Returns the name of the action.
     * <p>
     * This method returns the value of {@link Class#getName()}, and can not be overridden by concrete subclasses.
     * {@link JarvisAction}'s names are part of jarvis' naming convention, and are used to dynamically load modules
     * and actions.
     *
     * @return the name of the action.
     */
    public final String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Runs the action and returns its result.
     * <p>
     * This method should not be called manually, and is handled by the {@link JarvisCore} component, that
     * orchestrates the {@link JarvisAction}s returned by the registered {@link JarvisModule}s.
     *
     * @return
     * @see JarvisCore
     */
    @Override
    public abstract Object call();

    @Override
    public String toString() {
        return MessageFormat.format("{0} ({1})", getName(), super.toString());
    }
}
