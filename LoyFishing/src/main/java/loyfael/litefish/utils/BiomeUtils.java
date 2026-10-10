package loyfael.litefish.utils;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;

import java.util.List;
import java.util.Locale;

/**
 * Registry-based replacements for the deprecated Biome.valueOf / values / name
 */
public final class BiomeUtils {

    private BiomeUtils() {
    }

    private static Registry<Biome> registry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
    }

    /**
     * Resolve a biome from a name such as "DEEP_OCEAN", "deep_ocean" or "minecraft:deep_ocean"
     *
     * @throws IllegalArgumentException if no biome matches
     */
    public static Biome parse(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        NamespacedKey key = lower.contains(":") ? NamespacedKey.fromString(lower) : NamespacedKey.minecraft(lower);
        Biome biome = key == null ? null : registry().get(key);
        if (biome == null) {
            throw new IllegalArgumentException("Unknown biome: " + name);
        }
        return biome;
    }

    /**
     * Upper-case name of a biome, e.g. "DEEP_OCEAN" (same format as the old enum name)
     */
    public static String name(Biome biome) {
        return biome.key().value().toUpperCase(Locale.ROOT);
    }

    public static List<Biome> all() {
        return registry().stream().toList();
    }
}
