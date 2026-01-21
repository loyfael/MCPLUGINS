package loyfael.litefish.gui;

import loyfael.litefish.LiteFish;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Settings GUI (placeholder for now)
 */
public class SettingsGUI extends BaseGUI {
    
    public SettingsGUI(LiteFish plugin, Player player) {
        super(plugin, player, "&c&lSettings", 27);
    }
    
    @Override
    protected void setupGUI() {
        inventory.setItem(13, createGuiItem(Material.REDSTONE,
            "&c&lSettings",
            "&7Configure your preferences",
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
