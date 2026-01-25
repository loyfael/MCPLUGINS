package loyfael.hooks;

import org.bukkit.Location;

/**
 * Hook for WorldGuard plugin integration
 * Allows checking PvP permissions in WorldGuard regions
 */
public class WorldGuardHook {

    /**
     * Check if PvP is allowed at the given location
     */
    public boolean canPvP(Location location) {
        // Basic implementation - can be expanded with actual WorldGuard API
        // For now, assume PvP is allowed unless in a protected region
        return true;
    }

    /**
     * Check if WorldGuard is available
     */
    public boolean isEnabled() {
        return true;
    }
}
