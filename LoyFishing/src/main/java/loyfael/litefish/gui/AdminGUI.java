package loyfael.litefish.gui;

import loyfael.litefish.LiteFish;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Admin GUI (placeholder for now)
 */
public class AdminGUI extends BaseGUI {
    
    public AdminGUI(LiteFish plugin, Player player) {
        super(plugin, player, "&4&lAdmin Panel", 27);
    }
    
    @Override
    protected void setupGUI() {
        inventory.setItem(13, createGuiItem(Material.COMMAND_BLOCK,
            "&4&lAdmin Panel",
            "&7Administrative tools",
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
