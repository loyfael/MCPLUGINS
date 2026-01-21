package loyfael.litefish.hooks;

import loyfael.litefish.LiteFish;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Hook for Nexo custom items integration
 */
public class NexoHook {
    
    private final LiteFish plugin;
    private boolean enabled = false;
    private Object nexoItems;
    
    public NexoHook(LiteFish plugin) {
        this.plugin = plugin;
        setup();
    }
    
    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("Nexo") == null) {
            return;
        }
        
        try {
            // Try to access Nexo API
            Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            nexoItems = nexoItemsClass;
            enabled = true;
            
            plugin.getLogger().info("Nexo detected and hooked successfully!");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("Nexo plugin found but API not accessible");
            enabled = false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into Nexo: " + e.getMessage());
            enabled = false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void disable() {
        nexoItems = null;
        enabled = false;
    }
    
    /**
     * Check if an ItemStack is a Nexo item
     */
    public boolean isNexoItem(ItemStack item) {
        if (!enabled || item == null) return false;
        
        try {
            Class<?> nexoItemsClass = (Class<?>) nexoItems;
            return (Boolean) nexoItemsClass.getMethod("isCustomItem", ItemStack.class).invoke(null, item);
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking if item is Nexo item: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the Nexo item ID from an ItemStack
     */
    public String getNexoItemId(ItemStack item) {
        if (!enabled || item == null) return null;
        
        try {
            Class<?> nexoItemsClass = (Class<?>) nexoItems;
            Object nexoItem = nexoItemsClass.getMethod("itemFromItemStack", ItemStack.class).invoke(null, item);
            
            if (nexoItem != null) {
                return (String) nexoItem.getClass().getMethod("getItemID").invoke(nexoItem);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting Nexo item ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Create a Nexo item by ID
     */
    public ItemStack createNexoItem(String itemId) {
        return createNexoItem(itemId, 1);
    }
    
    /**
     * Create a Nexo item by ID with specific amount
     */
    public ItemStack createNexoItem(String itemId, int amount) {
        if (!enabled || itemId == null) return null;
        
        try {
            Class<?> nexoItemsClass = (Class<?>) nexoItems;
            Object nexoItem = nexoItemsClass.getMethod("itemFromId", String.class).invoke(null, itemId);
            
            if (nexoItem != null) {
                ItemStack item = (ItemStack) nexoItem.getClass().getMethod("build").invoke(nexoItem);
                if (item != null) {
                    item.setAmount(amount);
                    return item;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating Nexo item '" + itemId + "': " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Check if a Nexo item exists
     */
    public boolean nexoItemExists(String itemId) {
        if (!enabled || itemId == null) return false;
        
        try {
            Class<?> nexoItemsClass = (Class<?>) nexoItems;
            Object nexoItem = nexoItemsClass.getMethod("itemFromId", String.class).invoke(null, itemId);
            return nexoItem != null;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking if Nexo item exists: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if an item is a custom fishing rod
     */
    public boolean isCustomFishingRod(ItemStack item) {
        if (!isNexoItem(item)) return false;
        
        String itemId = getNexoItemId(item);
        if (itemId == null) return false;
        
        // Check if the item ID contains fishing rod indicators
        return itemId.toLowerCase().contains("fishing_rod") || 
               itemId.toLowerCase().contains("rod") ||
               itemId.toLowerCase().contains("fishing");
    }
    
    /**
     * Check if an item is a custom bait
     */
    public boolean isCustomBait(ItemStack item) {
        if (!isNexoItem(item)) return false;
        
        String itemId = getNexoItemId(item);
        if (itemId == null) return false;
        
        // Check if the item ID contains bait indicators
        return itemId.toLowerCase().contains("bait") || 
               itemId.toLowerCase().contains("lure") ||
               itemId.toLowerCase().contains("worm");
    }
    
    /**
     * Get the fishing power bonus from a custom rod
     */
    public double getFishingRodPower(ItemStack rod) {
        if (!isCustomFishingRod(rod)) return 1.0;
        
        String itemId = getNexoItemId(rod);
        if (itemId == null) return 1.0;
        
        // Return power based on rod type (can be configured later)
        switch (itemId.toLowerCase()) {
            case "diamond_fishing_rod":
                return 1.5;
            case "netherite_fishing_rod":
                return 2.0;
            case "magic_fishing_rod":
                return 2.5;
            default:
                return 1.2; // Default bonus for custom rods
        }
    }
    
    /**
     * Get the luck bonus from a custom bait
     */
    public double getBaitLuckBonus(ItemStack bait) {
        if (!isCustomBait(bait)) return 1.0;
        
        String itemId = getNexoItemId(bait);
        if (itemId == null) return 1.0;
        
        // Return luck bonus based on bait type (can be configured later)
        switch (itemId.toLowerCase()) {
            case "golden_bait":
                return 1.3;
            case "magical_bait":
                return 1.5;
            case "legendary_bait":
                return 2.0;
            default:
                return 1.1; // Default bonus for custom baits
        }
    }
}
