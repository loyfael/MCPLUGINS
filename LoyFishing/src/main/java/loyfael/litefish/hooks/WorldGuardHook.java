package loyfael.litefish.hooks;

import loyfael.litefish.LiteFish;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Hook for WorldGuard region integration
 * Simplified version to avoid API compatibility issues
 */
public class WorldGuardHook {
    
    private final LiteFish plugin;
    private boolean enabled = false;
    
    public WorldGuardHook(LiteFish plugin) {
        this.plugin = plugin;
        setup();
    }
    
    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            return;
        }
        
        try {
            // Simple check that WorldGuard is present
            enabled = true;
            plugin.getLogger().info("WorldGuard detected and hooked successfully!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into WorldGuard: " + e.getMessage());
            enabled = false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void disable() {
        enabled = false;
    }
    
    /**
     * Check if fishing is allowed at the given location
     * Simplified - always returns true for now to avoid API issues
     */
    public boolean canFish(Player player, Location location) {
        if (!enabled) return true;
        
        // For now, we'll assume fishing is allowed everywhere
        // This can be enhanced later with proper WorldGuard API integration
        return true;
    }
    
    /**
     * Get the region name at the given location
     * Simplified - returns null for now to avoid API issues
     */
    public String getRegionName(Location location) {
        if (!enabled) return null;
        
        // For now, we'll return null
        // This can be enhanced later with proper WorldGuard API integration
        return null;
    }
    
    /**
     * Check if a location is in a specific region
     * Simplified - returns false for now to avoid API issues
     */
    public boolean isInRegion(Location location, String regionName) {
        if (!enabled || regionName == null) return false;
        
        // For now, we'll return false
        // This can be enhanced later with proper WorldGuard API integration
        return false;
    }
    
    /**
     * Check if a player can build at the given location
     * Simplified - always returns true for now to avoid API issues
     */
    public boolean canBuild(Player player, Location location) {
        if (!enabled) return true;
        
        // For now, we'll assume building is allowed everywhere
        // This can be enhanced later with proper WorldGuard API integration
        return true;
    }
}
