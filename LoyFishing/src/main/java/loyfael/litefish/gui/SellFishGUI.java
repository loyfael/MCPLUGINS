package loyfael.litefish.gui;

import loyfael.litefish.LiteFish;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Sell Fish GUI (placeholder for now)
 */
public class SellFishGUI extends BaseGUI {
    
    public SellFishGUI(LiteFish plugin, Player player) {
        super(plugin, player, "&a&lSell Fish", 27);
    }
    
    @Override
    protected void setupGUI() {
        inventory.setItem(13, createGuiItem(Material.EMERALD,
            "&a&lSell All Fish",
            "&7Sell all fish in your inventory",
            "&7for money!"));
        
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
