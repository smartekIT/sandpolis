//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.instance.plugin;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.foundation.util.CertUtil;
import com.sandpolis.core.foundation.util.JarUtil;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.plugin.PluginEvents.PluginLoadedEvent;
import com.sandpolis.core.instance.plugin.PluginStore.PluginStoreConfig;
import com.sandpolis.core.instance.state.VirtPlugin;
import com.sandpolis.core.instance.state.st.STCollection;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.STCollectionStore;

/**
 * The {@link PluginStore} manages plugins.<br>
 * <br>
 * Plugins can be in one of 4 states:
 * <ul>
 * <li>DOWNLOADED: The plugin exists on the filesystem, but the
 * {@link PluginStore} does not know about it. The plugin does not have a
 * {@link Plugin} instance.</li>
 * <li>INSTALLED: The {@link PluginStore} has a {@link Plugin} instance for the
 * plugin, but no extensions points have been loaded.</li>
 * <li>DISABLED: The plugin is INSTALLED, but cannot be loaded until it's
 * enabled.</li>
 * <li>LOADED: The plugin is INSTALLED and all extension points have been
 * loaded.</li>
 * </ul>
 *
 * @since 5.0.0
 */
public final class PluginStore extends STCollectionStore<Plugin> implements ConfigurableStore<PluginStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(PluginStore.class);

	public PluginStore() {
		super(log);
	}

	/**
	 * The default certificate verifier which allows all plugins.
	 */
	private static Function<X509Certificate, Boolean> verifier = c -> true;

	public Optional<Plugin> getByPackageId(String packageId) {
		return stream().filter(plugin -> plugin.getPackageId().equals(packageId)).findAny();
	}

	/**
	 * Get a component of a plugin archive.
	 *
	 * @param plugin   The plugin
	 * @param instance The instance type of the component
	 * @param sub      The subtype of the component
	 * @return The component as a {@link ByteSource} or {@code null} if the
	 *         component does not exist
	 */
	public ByteSource getPluginComponent(Plugin plugin, InstanceType instance, InstanceFlavor sub) {
		URL url = getPluginComponentUrl(plugin, instance, sub);

		return url != null ? Resources.asByteSource(url) : null;
	}

	/**
	 * Find all components that the given plugin contains.
	 *
	 * @param plugin The plugin to search
	 * @return A list of components that were found in the plugin
	 */
	public List<InstanceFlavor> findComponentTypes(Plugin plugin) {
		checkNotNull(plugin);

		List<InstanceFlavor> types = new LinkedList<>();

		// TODO don't check invalid combinations
		for (InstanceType instance : InstanceType.values())
			for (InstanceFlavor sub : InstanceFlavor.values())
				if (getPluginComponentUrl(plugin, instance, sub) != null)
					types.add(sub);

		return types;
	}

	/**
	 * Get a {@link URL} representing a component of the given plugin.
	 *
	 * @param plugin   The target plugin
	 * @param instance The instance type
	 * @param sub      The instance subtype
	 * @return A url for the component or {@code null} if not found
	 */
	public URL getPluginComponentUrl(Plugin plugin, InstanceType instance, InstanceFlavor sub) {
		checkNotNull(plugin);
		checkNotNull(instance);
		checkNotNull(sub);

		return null;
	}

	/**
	 * Scan the plugin directory for uninstalled core plugins and install them.
	 *
	 * @throws IOException If a filesystem error occurs
	 */
	public void scanPluginDirectory() throws IOException {
		// TODO will install an arbitrary version if there's more than one
		Files.list(Environment.LIB.path())
				// Core plugins only
				.filter(path -> path.getFileName().toString().startsWith("sandpolis-plugin-"))
				// Skip installed plugins
				.filter(path -> {
					try (Stream<Plugin> stream = stream()) {
						// Read plugin id
						String id = JarUtil.getManifestValue(path.toFile(), "Plugin-Id").orElse(null);

						return stream.noneMatch(plugin -> plugin.getId().equals(id));
					} catch (IOException e) {
						return false;
					}
				}).forEach(PluginStore::installPlugin);

	}

	/**
	 * Load all installed plugins that are enabled and currently unloaded.
	 */
	public void loadPlugins() {
		stream()
				// Enabled plugins only
				.filter(Plugin::isEnabled)
				// Skip loaded plugins
				.filter(plugin -> !plugin.isLoaded())
				// Load each plugin
				.forEach(PluginStore::loadPlugin);
	}

	/**
	 * Install a plugin.
	 *
	 * @param path The plugin's filesystem artifact
	 */
	public synchronized void installPlugin(Path path) {
		log.debug("Installing plugin: {}", path.toString());

		try {
			Plugin plugin = create(p -> {
			});
			plugin.install(path, true);
		} catch (IOException e) {
			log.error("Failed to install plugin", e);
		}
	}

	/**
	 * Load a plugin. This method verifies the plugin artifact's hash.
	 *
	 * @param plugin The plugin to load
	 */
	private void loadPlugin(Plugin plugin) {
		if (plugin.isLoaded())
			throw new IllegalStateException();

		// Verify hash
		try {
			if (!plugin.checkHash()) {
				log.error("The stored plugin hash does not match the artifact's hash");
				return;
			}
		} catch (IOException e) {
			log.error("Failed to hash plugin", e);
			return;
		}

		// Verify certificate
		try {
			if (!verifier.apply(CertUtil.parseCert(plugin.getCertificate()))) {
				log.error("Failed to verify plugin certificate");
				return;
			}
		} catch (CertificateException e) {
			log.error("Failed to load plugin certificate", e);
			return;
		}

		log.debug("Loading plugin: {} ({})", plugin.getName(), plugin.getId());

		try {
			plugin.load();
		} catch (IOException e) {
			log.error("Failed to load plugin", e);
			return;
		}
		post(PluginLoadedEvent::new, plugin);
	}

	public Stream<Plugin> getLoadedPlugins() {
		return stream().filter(Plugin::isEnabled).filter(Plugin::isLoaded);
	}

	@Override
	public void init(Consumer<PluginStoreConfig> configurator) {
		var config = new PluginStoreConfig();
		configurator.accept(config);

		collection = config.collection;
	}

	public Plugin create(Consumer<VirtPlugin> configurator) {
		var plugin = new Plugin(collection.newDocument());
		configurator.accept(plugin);
		add(plugin);
		return plugin;
	}

	@Override
	protected Plugin constructor(STDocument document) {
		return new Plugin(document);
	}

	@ConfigStruct
	public static final class PluginStoreConfig {

		public Function<X509Certificate, Boolean> verifier;

		public STCollection collection;
	}

	public static final PluginStore PluginStore = new PluginStore();
}
