package loyfael.litefish.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Handles GUI click events
 */
public class GUIListener implements Listener {
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof BaseGUI) {
            event.setCancelled(true); // Prevent item movement
            
            if (event.getWhoClicked() instanceof Player) {
                BaseGUI gui = (BaseGUI) holder;
                
                // Handle the click
                gui.handleClick(event.getSlot(), event.getCurrentItem(), event.isShiftClick());
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Handle any cleanup if needed
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof BaseGUI) {
            // GUI was closed, could save data or cleanup here
        }
    }
}
