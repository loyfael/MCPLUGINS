package loyfael.litefish.gui;

import loyfael.litefish.LiteFish;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Baits and Rods GUI (placeholder for now)
 */
public class BaitsRodsGUI extends BaseGUI {
    
    public BaitsRodsGUI(LiteFish plugin, Player player) {
        super(plugin, player, "&d&lBaits & Rods", 27);
    }
    
    @Override
    protected void setupGUI() {
        inventory.setItem(13, createGuiItem(Material.FISHING_ROD,
            "&d&lBaits & Rods",
            "&7Manage your fishing gear",
            "&7Coming soon!"));
        
        inventory.setItem(22, createBackButton());
        fillEmpty();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick) {
        if (slot == 22) {
            playSound(Sound.UI_BUTTON_CLICK);
            new MainMenuGUI(plugin, player).open();
        }
    }
}
