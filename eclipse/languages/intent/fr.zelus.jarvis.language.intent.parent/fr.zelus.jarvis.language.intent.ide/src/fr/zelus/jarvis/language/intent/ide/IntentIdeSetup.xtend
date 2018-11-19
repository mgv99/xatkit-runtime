/*
 * generated by Xtext 2.12.0
 */
package fr.zelus.jarvis.language.intent.ide

import com.google.inject.Guice
import fr.zelus.jarvis.language.intent.IntentRuntimeModule
import fr.zelus.jarvis.language.intent.IntentStandaloneSetup
import org.eclipse.xtext.util.Modules2

/**
 * Initialization support for running Xtext languages as language servers.
 */
class IntentIdeSetup extends IntentStandaloneSetup {

	override createInjector() {
		Guice.createInjector(Modules2.mixin(new IntentRuntimeModule, new IntentIdeModule))
	}
	
}