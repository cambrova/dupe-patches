package io.github.traelmyx.dupepatches;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge port of Panda Anti-Dupe by TreeOfSelf (CC0-1.0, https://github.com/TreeOfSelf/PandaAntiDupe), which is
 * Fabric only. Every patch has its own switch. Only dupes that still work in the vanilla game are patched, and each
 * one has a game test that reproduces it with the patch switched off.
 */
public final class DupePatches {
	public static final String MOD_ID = "dupepatches";
	public static final Logger LOGGER = LoggerFactory.getLogger("Dupe Patches");

	/** Patch switches, in config file order. Item and inventory dupes default on; farm dupes default off. */
	public static final Map<String, Patch> PATCHES = new LinkedHashMap<>();

	// The trident and book dupes only work on 1.21.1; vanilla 1.21.11 and 26.x already prevent both, so they are not patched there.
	//? if <1.21.11 {
	public static final Patch TRIDENT = patch("trident", true,
		"Moving a trident into the crafting grid or a container while charging it, then throwing it, kept a copy.");
	public static final Patch BOOK = patch("book", true,
		"Signing a book with a title over 32 characters made the player impossible to save, so they rolled back on relog.");
	//?} else {
	/*public static final Patch TRIDENT = new Patch("trident", false, "");
	public static final Patch BOOK = new Patch("book", false, "");
	*///?}
	public static final Patch GRAVITY_BLOCK = patch("gravityBlock", false,
		"Sand, gravel, concrete powder and other falling blocks that go through an end portal also land where they left. Vanilla keeps this on purpose and sand dupers rely on it, so it's off by default.");

	/** Players, entities and block positions the game tests keep on vanilla behaviour, to show each patch matters. */
	public static final Set<Object> VANILLA = ConcurrentHashMap.newKeySet();

	private DupePatches() {
	}

	public static final class Patch {
		public final String key;
		public final String description;
		public volatile boolean enabled;

		Patch(String key, boolean enabled, String description) {
			this.key = key;
			this.enabled = enabled;
			this.description = description;
		}

		public boolean on(Entity entity) {
			return enabled && (entity == null || !VANILLA.contains(entity.getUUID()));
		}

		public boolean on(BlockPos pos) {
			return enabled && (pos == null || !VANILLA.contains(pos.immutable()));
		}
	}

	private static Patch patch(String key, boolean enabled, String description) {
		Patch patch = new Patch(key, enabled, description);
		PATCHES.put(key, patch);
		return patch;
	}

	public static void loadConfig(Path configDir) {
		Path file = configDir.resolve(MOD_ID + ".properties");
		Properties props = new Properties();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				props.load(reader);
			} catch (IOException e) {
				LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}
		for (Patch patch : PATCHES.values()) {
			String value = props.getProperty(patch.key);
			if (value != null) patch.enabled = Boolean.parseBoolean(value.trim());
		}
		StringBuilder out = new StringBuilder("# Dupe Patches. true = the dupe is patched, false = vanilla behaviour.\n");
		for (Patch patch : PATCHES.values()) {
			out.append("\n# ").append(patch.description).append('\n').append(patch.key).append('=').append(patch.enabled).append('\n');
		}
		try {
			Files.createDirectories(configDir);
			try (Writer writer = Files.newBufferedWriter(file)) {
				writer.write(out.toString());
			}
		} catch (IOException e) {
			LOGGER.warn("Could not write {}", file, e);
		}
	}
}
