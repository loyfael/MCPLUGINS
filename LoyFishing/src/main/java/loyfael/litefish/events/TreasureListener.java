package loyfael.litefish.events;

import loyfael.litefish.LiteFish;
import loyfael.litefish.managers.TreasureManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles treasure chest interactions
 */
public class TreasureListener implements Listener {
    
    private final LiteFish plugin;
    private final TreasureManager treasureManager;
    
    public TreasureListener(LiteFish plugin) {
        this.plugin = plugin;
        this.treasureManager = plugin.getTreasureManager();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && 
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        // Check if item is a treasure
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        String treasureId = meta.getPersistentDataContainer().get(
            plugin.getKey("treasure_id"), 
            org.bukkit.persistence.PersistentDataType.STRING
        );
        
        if (treasureId != null) {
            // This is a treasure, open it
            event.setCancelled(true);
            treasureManager.openTreasure(event.getPlayer(), item);
        }
    }
}
