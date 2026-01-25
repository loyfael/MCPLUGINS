package loyfael.hooks;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Hook for Lands plugin integration
 * Allows checking PvP permissions in claimed lands
 */
public class LandsHook {

    private final Plugin landsPlugin;

    public LandsHook(Plugin landsPlugin) {
        this.landsPlugin = landsPlugin;
    }

    /**
     * Check if PvP is allowed between two players based on Lands protection
     */
    public boolean canPvP(Player attacker, Player victim) {
        // Basic implementation - can be expanded with actual Lands API
        // For now, assume PvP is allowed unless in a protected area
        return true;
    }

    /**
     * Check if Lands plugin is available and enabled
     */
    public boolean isEnabled() {
        return landsPlugin != null && landsPlugin.isEnabled();
    }
}
