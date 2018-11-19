package fr.zelus.jarvis.language.execution.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.osgi.framework.Bundle;

import fr.zelus.jarvis.core_platforms.utils.LibraryLoaderUtils;
import fr.zelus.jarvis.core_platforms.utils.PlatformLoaderUtils;
import fr.zelus.jarvis.execution.ExecutionModel;
import fr.zelus.jarvis.execution.ExecutionPackage;
import fr.zelus.jarvis.execution.ImportDeclaration;
import fr.zelus.jarvis.execution.LibraryImportDeclaration;
import fr.zelus.jarvis.execution.PlatformImportDeclaration;
import fr.zelus.jarvis.intent.IntentPackage;
import fr.zelus.jarvis.intent.Library;
import fr.zelus.jarvis.platform.Platform;
import fr.zelus.jarvis.platform.PlatformPackage;

public class ImportRegistry {

	private static ImportRegistry INSTANCE;

	public static ImportRegistry getInstance() {
		if (isNull(INSTANCE)) {
			INSTANCE = new ImportRegistry();
		}
		return INSTANCE;
	}

	private ResourceSet rSet;

	private Map<String, Platform> platforms;

	private Map<String, Library> libraries;

	private ImportRegistry() {
		this.rSet = new ResourceSetImpl();
		this.rSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		this.rSet.getPackageRegistry().put(ExecutionPackage.eINSTANCE.getNsURI(), ExecutionPackage.eINSTANCE);
		this.rSet.getPackageRegistry().put(PlatformPackage.eINSTANCE.getNsURI(), PlatformPackage.eINSTANCE);
		this.rSet.getPackageRegistry().put(IntentPackage.eINSTANCE.getNsURI(), IntentPackage.eINSTANCE);
		try {
			loadJarvisCorePlatforms();
		} catch (IOException | URISyntaxException e) {
			System.out.println("An error occurred when loading core platforms");
			e.printStackTrace();
		}
		ExecutionPackage.eINSTANCE.eClass();
		PlatformPackage.eINSTANCE.eClass();
		IntentPackage.eINSTANCE.eClass();
		this.platforms = new HashMap<>();
		this.libraries = new HashMap<>();
	}

	public Collection<Resource> loadExecutionModelImports(ExecutionModel model) {
		/*
		 * Clear the loaded platforms and libraries, we are reloading them all
		 */
		this.platforms.clear();
		this.libraries.clear();
		List<Resource> resources = new ArrayList<>();
		for (ImportDeclaration importDeclaration : model.getImports()) {
			resources.add(loadImport(importDeclaration));
		}
		return resources;
	}

	public Collection<Platform> getLoadedPlatforms(ExecutionModel model) {
		/*
		 * Reload all the platforms in case the imports have changed.
		 */
		this.loadExecutionModelImports(model);
		return this.platforms.values();
	}

	public Collection<Library> getLoadedLibraries(ExecutionModel model) {
		/*
		 * Reload all the libraries in case the imports have changed.
		 */
		this.loadExecutionModelImports(model);
		return this.libraries.values();
	}

	/**
	 * Loads the {@link Resource} described by the provided {@code importDeclaration}.
	 * <p>
	 * The loaded {@link Resource} can be either a {@link Platform} {@link Resource}, or a {@link Library}
	 * {@link Resource}.
	 * 
	 * @param importDeclaration the {@link ImportDeclaration} to load
	 * @return the loaded {@link Resource}, or {@code null} if the {@link ImportRegistry} was not able to load it
	 */
	public Resource loadImport(ImportDeclaration importDeclaration) {
		String path = importDeclaration.getPath();
		String alias = importDeclaration.getAlias();
		System.out.println(MessageFormat.format("Loading import from path {0}", path));
		/*
		 * Try to load it as a core platform
		 */
		Resource resource = null;
		try {
			resource = rSet.getResource(URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + path + ".xmi"),
					false);
			if (isNull(resource)) {
				/*
				 * In case .xmi has been specified within the import
				 */
				resource = rSet.getResource(URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + path), false);
			}
		} catch (Exception e) {
			System.out.println("Cannot load the import as a core platform");
		}
		/*
		 * The import is not a core platform, try to load it from its path, and register the resource using its alias.
		 * If the import declaration does not define an alias load the resource using its absolute path, meaning that
		 * the generated XMI will not be portable.
		 */
		if (isNull(resource)) {
			File importResourceFile = new File(path);
			URI importResourceFileURI = URI.createFileURI(importResourceFile.getAbsolutePath());
			URI importResourceURI = importResourceFileURI;
			if (nonNull(alias)) {
				URI importResourceAliasURI;
				if (importDeclaration instanceof PlatformImportDeclaration) {
					importResourceAliasURI = URI.createURI(PlatformLoaderUtils.CUSTOM_PLATFORM_PATHMAP + alias);
				} else if (importDeclaration instanceof LibraryImportDeclaration) {
					importResourceAliasURI = URI.createURI(LibraryLoaderUtils.CUSTOM_LIBRARY_PATHMAP + alias);
				} else {
					System.out.println(MessageFormat.format("Cannot load the provided import, unknown import type {0}",
							importDeclaration.eClass().getName()));
					return null;
				}
				rSet.getURIConverter().getURIMap().put(importResourceAliasURI, importResourceFileURI);
				Iterator<Resource> registeredResources = rSet.getResources().iterator();
				/*
				 * Removes the Resource from the resource set that matches either the alias URI or the base URI. The
				 * alias URI needs to be removed if the resource URI to load has changed, otherwise the old resource is
				 * returned. The base URI needs to be removed if the base URI was used to register the resource without
				 * an alias before.
				 */
				while (registeredResources.hasNext()) {
					Resource registeredResource = registeredResources.next();
					/*
					 * Check the last segment, the URI may contained either CUSTOM_PLATFORM_PATHMAP or
					 * CUSTOM_LIBRARY_PATHMAP if it was previously registered as a Platform/Library
					 */
					if (registeredResource.getURI().lastSegment().equals(alias)) {
						System.out.println(MessageFormat.format("Unregistering resource {0} from the ResourceSet",
								importResourceAliasURI));
						registeredResources.remove();
					}
					if (registeredResource.getURI().equals(importResourceURI)) {
						System.out.println(MessageFormat.format("Unregistering resource {0} from the ResourceSet",
								importResourceURI));
						registeredResources.remove();
					}
				}
				importResourceURI = importResourceAliasURI;
			} else {
				/*
				 * If there is an alias we need to remove the URIMap entry previously associated to the base URI. This
				 * allows to update aliases and remove them.
				 */
				Iterator<Map.Entry<URI, URI>> uriMapEntries = rSet.getURIConverter().getURIMap().entrySet().iterator();
				while (uriMapEntries.hasNext()) {
					Map.Entry<URI, URI> uriMapEntry = uriMapEntries.next();
					if (uriMapEntry.getValue().equals(importResourceURI)) {
						uriMapEntries.remove();
					}
				}
			}
			resource = rSet.getResource(importResourceURI, false);
			if (isNull(resource)) {
				resource = rSet.createResource(importResourceURI);
			}
		}
		if (nonNull(resource)) {
			try {
				resource.load(Collections.emptyMap());
			} catch (IOException e) {
				System.out.println("An error occurred when loading the resource");
				return null;
			}
		} else {
			System.out.println(
					MessageFormat.format("Cannot find the resource asssociated to the import {0}", importDeclaration));
			return null;
		}
		System.out.println(MessageFormat.format("Resource with URI {0} loaded", resource.getURI()));
		if (resource.getContents().isEmpty()) {
			System.out.println("The loaded resource is empty");
			return null;
		}
		for (EObject e : resource.getContents()) {
			if (e instanceof Platform) {
				Platform platform = (Platform) e;
				if (importDeclaration instanceof PlatformImportDeclaration) {
					if (this.platforms.containsKey((platform.getName()))) {
						System.out.println(MessageFormat.format("The platform {0} is already loaded, erasing it",
								platform.getName()));
					}
					System.out.println(MessageFormat.format("Registering platform {0}", platform.getName()));
					this.platforms.put(platform.getName(), platform);
				} else {
					System.out
							.println(MessageFormat.format("Trying to load a {0} using a {1}, please use a {2} instead",
									e.eClass().getName(), importDeclaration.getClass().getSimpleName(),
									PlatformImportDeclaration.class.getSimpleName()));
					return null;
				}
			} else if (e instanceof Library) {
				Library library = (Library) e;
				if (importDeclaration instanceof LibraryImportDeclaration) {
					if (this.libraries.containsKey(library.getName())) {
						System.out.println(MessageFormat.format("The library {0} is already loaded, erasing it",
								library.getName()));
					}
					System.out.println(MessageFormat.format("Registering library {0}", library.getName()));
					this.libraries.put(library.getName(), library);
				} else {
					System.out
							.println(MessageFormat.format("Trying to load a {0} using a {1}, please use a {2} instead",
									e.eClass().getName(), importDeclaration.getClass().getSimpleName(),
									LibraryImportDeclaration.class.getSimpleName()));
					return null;
				}
			} else {
				/*
				 * The top level element is not a platform, we are not loading a valid Platform resource
				 */
				System.out.println(MessageFormat.format(
						"The loaded resource contains the unknown top-level element {0}", e.eClass().getName()));
				return null;
			}
		}
		return resource;
	}

	public void loadJarvisCorePlatforms() throws IOException, URISyntaxException {
		Bundle bundle = org.eclipse.core.runtime.Platform.getBundle("fr.zelus.jarvis.core_platforms");
		if (isNull(bundle)) {
			throw new RuntimeException("Cannot find the bundle fr.zelus.jarvis.core_platforms");
		}
		URL platformFolderURL = bundle.getEntry("platforms/xmi/");
		if (isNull(platformFolderURL)) {
			System.out.println(MessageFormat.format(
					"Cannot load the platforms/xmi/ folder from the bundle {0}, trying to load it in development mode",
					bundle));
			/*
			 * If the plugin is not installed (i.e. if we are running an eclipse application from a workspace that
			 * contains the plugin sources) the folder is located in src/main/resources.
			 */
			platformFolderURL = bundle.getEntry("src/main/resources/platforms/xmi/");
			if (isNull(platformFolderURL)) {
				throw new RuntimeException(MessageFormat.format(
						"Cannot load the platforms/xmi/ folder from the bundle {0} (development mode failed)", bundle));
			} else {
				System.out.println("platforms/xmi/ folder loaded from the bundle in development mode");
			}
		}

		java.net.URI resolvedPlatformFolderURI = FileLocator.resolve(platformFolderURL).toURI();
		System.out.println(MessageFormat.format("Resolved platforms/xmi/ folder URI: {0}", resolvedPlatformFolderURI));

		if (resolvedPlatformFolderURI.getScheme().equals("jar")) {
			FileSystem fs = FileSystems.newFileSystem(resolvedPlatformFolderURI, Collections.emptyMap());

		}
		Path platformPath = Paths.get(resolvedPlatformFolderURI);
		System.out.println(MessageFormat.format("Crawling platforms in {0}", platformPath));
		Files.walk(platformPath, 1).filter(filePath -> !Files.isDirectory(filePath)).forEach(modelPath -> {
			try {
				InputStream is = Files.newInputStream(modelPath);
				rSet.getURIConverter().getURIMap().put(
						URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + modelPath.getFileName()),
						URI.createURI(modelPath.getFileName().toString()));
				Resource modelResource = this.rSet.createResource(
						URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + modelPath.getFileName().toString()));
				modelResource.load(is, Collections.emptyMap());
				System.out.println(MessageFormat.format("Platform resource {0} loaded (uri={1})",
						modelPath.getFileName(), modelResource.getURI()));
				is.close();
			} catch (IOException e) {
				System.out.println(MessageFormat.format("An error occurred when loading the platform resource {0}",
						modelPath.getFileName()));
			}
		});
	}

}