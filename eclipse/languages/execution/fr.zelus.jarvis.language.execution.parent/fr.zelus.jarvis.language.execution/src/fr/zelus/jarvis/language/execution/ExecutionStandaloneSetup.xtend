/*
 * generated by Xtext 2.12.0
 */
package fr.zelus.jarvis.language.execution


/**
 * Initialization support for running Xtext languages without Equinox extension registry.
 */
class ExecutionStandaloneSetup extends ExecutionStandaloneSetupGenerated {

	def static void doSetup() {
		new ExecutionStandaloneSetup().createInjectorAndDoEMFRegistration()
	}
}