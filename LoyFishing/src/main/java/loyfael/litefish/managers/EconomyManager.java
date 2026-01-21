package loyfael.litefish.managers;

import loyfael.litefish.LiteFish;

/**
 * Manages economy-related operations
 */
public class EconomyManager {
    
    private final LiteFish plugin;
    
    public EconomyManager(LiteFish plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if economy is enabled and available
     */
    public boolean isEconomyEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("economy.enabled", true) &&
               plugin.getVaultHook().isEnabled();
    }
    
    /**
     * Get the sell multiplier from config
     */
    public double getSellMultiplier() {
        return plugin.getConfigManager().getConfig().getDouble("economy.sell-multiplier", 1.0);
    }
    
    /**
     * Check if auto-sell is enabled when inventory is full
     */
    public boolean isAutoSellOnFullInventory() {
        return plugin.getConfigManager().getConfig().getBoolean("economy.auto-sell-inventory-full", false);
    }
}
