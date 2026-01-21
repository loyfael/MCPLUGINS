package loyfael.litefish.gui;

import loyfael.litefish.LiteFish;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Main menu GUI for LiteFish
 */
public class MainMenuGUI extends BaseGUI {
    
    public MainMenuGUI(LiteFish plugin, Player player) {
        super(plugin, player, "&9&lLiteFish Menu", 45);
    }
    
    @Override
    protected void setupGUI() {
        // Statistics
        inventory.setItem(10, createGuiItem(Material.BOOK, 
            "&e&lYour Statistics", 
            "&7View your fishing statistics",
            "&7and achievements",
            "",
            "&aClick to view!"));
        
        // Fish Catalog
        inventory.setItem(12, createGuiItem(Material.TROPICAL_FISH, 
            "&b&lFish Catalog", 
            "&7Browse all available fish",
            "&7and their information",
            "",
            "&aClick to browse!"));
        
        // Biomes Info
        inventory.setItem(14, createGuiItem(Material.GRASS_BLOCK, 
            "&2&lBiomes", 
            "&7View biome information",
            "&7and their fish",
            "",
            "&aClick to view!"));
        
        // Tournament
        inventory.setItem(16, createGuiItem(Material.GOLDEN_SWORD, 
            "&6&lTournament", 
            "&7Join fishing tournaments",
            "&7and compete with others",
            "",
            "&aClick to participate!"));
        
        // Economy (Sell Fish)
        if (plugin.getVaultHook().isEnabled()) {
            inventory.setItem(28, createGuiItem(Material.EMERALD, 
                "&a&lSell Fish", 
                "&7Sell your caught fish",
                "&7for money",
                "",
                "&aClick to sell!"));
        }
        
        // Settings
        inventory.setItem(30, createGuiItem(Material.REDSTONE, 
            "&c&lSettings", 
            "&7Configure your fishing",
            "&7preferences",
            "",
            "&aClick to configure!"));
        
        // Baits and Rods
        inventory.setItem(32, createGuiItem(Material.FISHING_ROD, 
            "&d&lBaits & Rods", 
            "&7Manage your fishing gear",
            "&7and baits",
            "",
            "&aClick to manage!"));
        
        // Admin Panel
        if (hasPermission("litefish.admin")) {
            inventory.setItem(34, createGuiItem(Material.COMMAND_BLOCK, 
                "&4&lAdmin Panel", 
                "&7Administrative tools",
                "&7and configuration",
                "",
                "&cAdmin only!"));
        }
        
        // Close button
        inventory.setItem(40, createCloseButton());
        
        // Fill empty slots
        fillEmpty();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick) {
        if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }
        
        playSound(Sound.UI_BUTTON_CLICK);
        
        switch (slot) {
            case 10: // Statistics
                new StatisticsGUI(plugin, player).open();
                break;
                
            case 12: // Fish Catalog
                new FishCatalogGUI(plugin, player).open();
                break;
                
            case 14: // Biomes
                new BiomesGUI(plugin, player).open();
                break;
                
            case 16: // Tournament
                new TournamentGUI(plugin, player).open();
                break;
                
            case 28: // Sell Fish
                if (plugin.getVaultHook().isEnabled()) {
                    new SellFishGUI(plugin, player).open();
                }
                break;
                
            case 30: // Settings
                new SettingsGUI(plugin, player).open();
                break;
                
            case 32: // Baits & Rods
                new BaitsRodsGUI(plugin, player).open();
                break;
                
            case 34: // Admin Panel
                if (hasPermission("litefish.admin")) {
                    new AdminGUI(plugin, player).open();
                }
                break;
                
            case 40: // Close
                close();
                playSound(Sound.BLOCK_CHEST_CLOSE);
                break;
        }
    }
}
