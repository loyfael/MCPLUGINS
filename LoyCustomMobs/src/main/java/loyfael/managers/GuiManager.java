package loyfael.managers;

import loyfael.LoyCustomMobs;
import java.util.UUID;

/**
 * Stub GUI manager – boss bar support disabled (handled by another plugin).
 */
public class GuiManager {
    private final LoyCustomMobs plugin;

    public GuiManager(LoyCustomMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the GUI manager
     */
    public void initialize() {
        plugin.getLogger().info("GuiManager initialized (boss bars disabled)");
    }

    /**
     * Reload configuration
     */
    public void reload() {
        plugin.getLogger().info("GuiManager reload requested (no GUI features active)");
    }

    /**
     * Clean up boss bar for a specific mob
     */
    public void removeBossBar(UUID mobId) {
        // No boss bars to remove.
    }

    /**
     * Clean up all GUI elements
     */
    public void cleanup() {
        plugin.getLogger().info("GuiManager cleanup complete (no GUI state)");
    }
}
