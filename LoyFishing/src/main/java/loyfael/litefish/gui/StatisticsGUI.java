package loyfael.litefish.gui;

import loyfael.litefish.LiteFish;
import loyfael.litefish.managers.PlayerDataManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Statistics GUI showing player fishing data
 */
public class StatisticsGUI extends BaseGUI {
    
    public StatisticsGUI(LiteFish plugin, Player player) {
        super(plugin, player, "&e&lYour Fishing Statistics", 54);
    }
    
    @Override
    protected void setupGUI() {
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        
        // Total fish caught
        int totalFish = dataManager.getTotalFishCaught(player);
        inventory.setItem(10, createGuiItem(Material.TROPICAL_FISH, 
            "&b&lTotal Fish Caught", 
            "&7You have caught &e" + totalFish + " &7fish",
            "",
            "&aKeep fishing!"));
        
        // Total experience gained
        int totalExp = dataManager.getTotalExperienceGained(player);
        inventory.setItem(12, createGuiItem(Material.EXPERIENCE_BOTTLE, 
            "&a&lTotal Experience", 
            "&7You have gained &e" + totalExp + " &7experience",
            "&7from fishing",
            "",
            "&aLevel up!"));
        
        // Money earned
        if (plugin.getVaultHook().isEnabled()) {
            double totalMoney = dataManager.getTotalMoneyEarned(player);
            inventory.setItem(14, createGuiItem(Material.EMERALD, 
                "&2&lMoney Earned", 
                "&7You have earned &e$" + String.format("%.2f", totalMoney),
                "&7from selling fish",
                "",
                "&aProfit!"));
        }
        
        // Favorite biome
        String favoriteBiome = dataManager.getFavoriteBiome(player);
        inventory.setItem(16, createGuiItem(Material.GRASS_BLOCK, 
            "&2&lFavorite Biome", 
            "&7Your favorite fishing spot:",
            "&e" + favoriteBiome,
            "",
            "&aExplore more!"));
        
        // Fish by type
        Map<String, Integer> fishCounts = dataManager.getFishCounts(player);
        int startSlot = 28;
        int count = 0;
        
        for (Map.Entry<String, Integer> entry : fishCounts.entrySet()) {
            if (count >= 7) break; // Limit to 7 items
            
            String fishKey = entry.getKey();
            int fishCount = entry.getValue();
            final int slotIndex = count; // Make it final for lambda
            
            // Get the fish drop to get its material
            plugin.getDropManager().getDrop(fishKey).ifPresent(drop -> {
                inventory.setItem(startSlot + slotIndex, createGuiItem(drop.getMaterial(), 
                    "&f" + drop.getDisplayName(), 
                    "&7Caught: &e" + fishCount + " &7times",
                    "&7Experience: &a" + (drop.getExperience() * fishCount),
                    "",
                    "&7Keep catching them!"));
            });
            
            count++;
        }
        
        // Back button
        inventory.setItem(49, createBackButton());
        
        // Fill empty slots
        fillEmpty();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick) {
        if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }
        
        playSound(Sound.UI_BUTTON_CLICK);
        
        if (slot == 49) { // Back button
            new MainMenuGUI(plugin, player).open();
        }
    }
}
