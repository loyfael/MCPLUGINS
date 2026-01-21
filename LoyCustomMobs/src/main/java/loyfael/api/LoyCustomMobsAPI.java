package loyfael.api;

import loyfael.LoyCustomMobs;
import loyfael.models.CustomMob;
import loyfael.models.MobRarity;
import loyfael.models.MobAbility;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Public API for LoyCustomMobs
 * Allows other plugins to interact with the custom mob system
 */
public class LoyCustomMobsAPI {

    private static LoyCustomMobs plugin;

    /**
     * Initialize the API (called internally)
     */
    public static void initialize(LoyCustomMobs pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Check if a mob is a custom mob
     */
    public static boolean isCustomMob(LivingEntity entity) {
        if (plugin == null) return false;
        return plugin.getMobManager().getCustomMob(entity.getUniqueId()) != null;
    }

    /**
     * Get custom mob data for an entity
     */
    public static CustomMob getCustomMob(LivingEntity entity) {
        if (plugin == null) return null;
        return plugin.getMobManager().getCustomMob(entity.getUniqueId());
    }

    /**
     * Get custom mob by UUID
     */
    public static CustomMob getCustomMob(UUID mobId) {
        if (plugin == null) return null;
        return plugin.getMobManager().getCustomMob(mobId);
    }

    /**
     * Convert a regular mob to a custom mob
     */
    public static CustomMob convertToCustomMob(LivingEntity entity, MobRarity rarity) {
        if (plugin == null) return null;
        return plugin.getMobManager().spawnCustomMob(entity.getLocation(), entity.getType(), rarity);
    }

    /**
     * Spawn a custom mob at a location
     */
    public static CustomMob spawnCustomMob(Location location, EntityType type, MobRarity rarity) {
        if (plugin == null) return null;
        return plugin.getMobManager().spawnCustomMob(location, type, rarity);
    }

    /**
     * Get all active custom mobs
     */
    public static Collection<CustomMob> getAllCustomMobs() {
        if (plugin == null) return java.util.Collections.emptyList();
        return plugin.getMobManager().getActiveMobs();
    }

    /**
     * Register a new mob ability (for addon plugins)
     */
    public static void registerAbility(String name, Class<? extends MobAbility> abilityClass) {
        if (plugin == null) return;
        plugin.getMobManager().registerAbility(name, abilityClass);
    }

    /**
     * Remove a custom mob from tracking
     */
    public static void removeCustomMob(UUID mobId) {
        if (plugin == null) return;
        plugin.getMobManager().removeCustomMob(mobId);
    }

    /**
     * Get nearby custom mobs to a player
     */
    public static Collection<CustomMob> getNearbyCustomMobs(Player player, double radius) {
        if (plugin == null) return java.util.Collections.emptyList();

        return getAllCustomMobs().stream()
                .filter(mob -> mob.getEntity().getWorld().equals(player.getWorld()))
                .filter(mob -> mob.getEntity().getLocation().distance(player.getLocation()) <= radius)
                .toList();
    }

    /**
     * Get plugin statistics
     */
    public static java.util.Map<String, Object> getStatistics() {
        if (plugin == null) return java.util.Collections.emptyMap();
        return plugin.getMobManager().getStatistics();
    }

    /**
     * Check if the plugin is loaded and ready
     */
    public static boolean isReady() {
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Get the plugin version
     */
    public static String getVersion() {
        if (plugin == null) return "Unknown";
        return plugin.getPluginMeta().getVersion();
    }
}
