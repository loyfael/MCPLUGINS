package loyfael.litefish.gui;

import loyfael.litefish.LiteFish;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Tournament GUI (placeholder for now)
 */
public class TournamentGUI extends BaseGUI {
    
    public TournamentGUI(LiteFish plugin, Player player) {
        super(plugin, player, "&6&lFishing Tournament", 27);
    }
    
    @Override
    protected void setupGUI() {
        inventory.setItem(13, createGuiItem(Material.GOLDEN_SWORD,
            "&6&lTournament System",
            "&7Coming soon!",
            "&7Compete with other players",
            "&7in fishing competitions!"));
        
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
