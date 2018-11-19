/*
 * ge
 * nerated by Xtext 2.12.0
 */
package fr.zelus.jarvis.language.execution.ui.contentassist

import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.RuleCall
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor
import org.eclipse.xtext.Assignment
import static java.util.Objects.isNull
import fr.zelus.jarvis.platform.Platform
import fr.zelus.jarvis.language.execution.util.PlatformRegistry
import fr.zelus.jarvis.execution.ExecutionModel

/**
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#content-assist
 * on how to customize the content assistant.
 */
class ExecutionProposalProvider extends AbstractExecutionProposalProvider {

	override completeExecutionModel_EventProviderDefinitions(EObject model, Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		var platforms = PlatformRegistry.getInstance.loadExecutionModelPlatforms(model as ExecutionModel)
		platforms.map[m|m.eventProviderDefinitions.map[i|i.name]].flatten.forEach [ iName |
			acceptor.accept(createCompletionProposal(iName, context))
		]
		super.completeExecutionModel_EventProviderDefinitions(model, assignment, context, acceptor)
	}

	override completeExecutionRule_Event(EObject model, Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		var platforms = PlatformRegistry.getInstance.loadExecutionModelPlatforms(model.eContainer as ExecutionModel)
		platforms.map[m|m.intentDefinitions.map[i|i.name]].flatten.forEach [ iName |
			acceptor.accept(createCompletionProposal(iName, context))
		]
		platforms.map[m|m.eventProviderDefinitions.map[e|e.eventDefinitions.map[ed|ed.name]].flatten].flatten.forEach [ edName |
			acceptor.accept(createCompletionProposal(edName, context))
		]
		super.completeExecutionRule_Event(model, assignment, context, acceptor)
	}

	override completeActionInstance_Action(EObject model, Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		println("completion")
		println(model)
		/*
		 * Retrieve the OrchestrationModel, it can be different than the direct parent in case of nested on error ActionInstances.
		 */
		var ExecutionModel executionModel = null
		var currentObject = model
		while(isNull(executionModel)) {
			currentObject = currentObject.eContainer
			if(currentObject instanceof ExecutionModel) {
				executionModel = currentObject
			}
		}
		val platforms = PlatformRegistry.getInstance.loadExecutionModelPlatforms(executionModel)
		platforms.map[m|m.actions].flatten.forEach [ a |
			var String prefix = (a.eContainer as Platform).name + ".";
			var parameterString = ""
			if(!a.parameters.empty) {
				parameterString += '('
				parameterString += a.parameters.map[p|p.key + " : \"\""].join(", ")
				parameterString += ')'
			}
			acceptor.accept(createCompletionProposal(prefix + a.name + parameterString, context))
		]
		super.completeActionInstance_Action(model, assignment, context, acceptor)
	}

}