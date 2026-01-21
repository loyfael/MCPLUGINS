package loyfael.litefish.gui;

import loyfael.litefish.LiteFish;
import loyfael.litefish.models.FishDrop;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Fish catalog GUI showing all available fish
 */
public class FishCatalogGUI extends BaseGUI {
    
    private int currentPage = 0;
    private final List<FishDrop> allFish;
    private final int itemsPerPage = 28;
    
    public FishCatalogGUI(LiteFish plugin, Player player) {
        super(plugin, player, "&b&lFish Catalog", 54);
        this.allFish = new ArrayList<>(plugin.getDropManager().getAllDrops().values());
    }
    
    @Override
    protected void setupGUI() {
        // Navigation buttons
        if (currentPage > 0) {
            inventory.setItem(45, createPreviousButton());
        }
        
        if ((currentPage + 1) * itemsPerPage < allFish.size()) {
            inventory.setItem(53, createNextButton());
        }
        
        // Back button
        inventory.setItem(49, createBackButton());
        
        // Display fish for current page
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allFish.size());
        
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            // Skip navigation slots
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
            if (slot == 44) break;
            
            FishDrop fish = allFish.get(i);
            
            List<String> lore = new ArrayList<>();
            lore.add("&7Chance: &e" + fish.getChance() + "%");
            lore.add("&7Experience: &a" + fish.getExperience());
            
            if (plugin.getVaultHook().isEnabled()) {
                lore.add("&7Price: &2$" + String.format("%.2f", fish.getPrice()));
            }
            
            lore.add("");
            lore.add("&7Biomes:");
            
            if (fish.getBiomes().isEmpty()) {
                lore.add("&e  All biomes");
            } else {
                for (String biome : fish.getBiomes()) {
                    try {
                        Biome b = Biome.valueOf(biome);
                        String biomeName = plugin.getBiomeManager().getBiomeDisplayName(b);
                        lore.add("&e  " + biomeName);
                    } catch (IllegalArgumentException e) {
                        lore.add("&e  " + biome);
                    }
                }
            }
            
            lore.add("");
            
            // Check if player has caught this fish
            int caughtCount = plugin.getPlayerDataManager().getPlayerData(player).getFishCaughtCount(fish.getKey());
            if (caughtCount > 0) {
                lore.add("&a✓ Caught " + caughtCount + " times");
            } else {
                lore.add("&c✗ Not caught yet");
            }
            
            ItemStack fishItem = createGuiItem(fish.getMaterial(), 
                "&f" + fish.getDisplayName(), 
                lore.toArray(new String[0]));
            
            inventory.setItem(slot, fishItem);
            slot++;
        }
        
        // Page indicator
        inventory.setItem(4, createGuiItem(Material.BOOK, 
            "&e&lPage " + (currentPage + 1),
            "&7Showing " + Math.min(itemsPerPage, allFish.size() - (currentPage * itemsPerPage)) + " fish",
            "&7Total: " + allFish.size() + " fish"));
        
        // Fill empty slots
        fillEmpty();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick) {
        if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }
        
        playSound(Sound.UI_BUTTON_CLICK);
        
        if (slot == 45 && currentPage > 0) { // Previous
            currentPage--;
            refresh();
        } else if (slot == 53 && (currentPage + 1) * itemsPerPage < allFish.size()) { // Next
            currentPage++;
            refresh();
        } else if (slot == 49) { // Back
            new MainMenuGUI(plugin, player).open();
        }
    }
}
