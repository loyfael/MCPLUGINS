package loyfael.litefish.gui;

import loyfael.litefish.LiteFish;
import loyfael.litefish.models.BiomeData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Biomes information GUI
 */
public class BiomesGUI extends BaseGUI {
    
    public BiomesGUI(LiteFish plugin, Player player) {
        super(plugin, player, "&2&lBiomes Information", 54);
    }
    
    @Override
    protected void setupGUI() {
        // Get all biomes from BiomeManager
        int slot = 10;
        
        // Ocean biomes
        addBiomeItem(slot++, Biome.OCEAN, Material.WATER_BUCKET);
        addBiomeItem(slot++, Biome.DEEP_OCEAN, Material.DARK_PRISMARINE);
        addBiomeItem(slot++, Biome.WARM_OCEAN, Material.TROPICAL_FISH);
        addBiomeItem(slot++, Biome.COLD_OCEAN, Material.COD);
        
        slot = 19; // Next row
        addBiomeItem(slot++, Biome.RIVER, Material.KELP);
        addBiomeItem(slot++, Biome.FROZEN_RIVER, Material.ICE);
        addBiomeItem(slot++, Biome.SWAMP, Material.LILY_PAD);
        
        // Special: Lava fishing
        slot = 28;
        inventory.setItem(slot, createGuiItem(Material.LAVA_BUCKET,
            "&c&lLava Fishing",
            "&7Fish in lava for rare items!",
            "&7Bonuses: &e+50% Rod Power, +20% Bait Luck",
            "",
            "&cDangerous but rewarding!"));
        
        // Back button
        inventory.setItem(49, createBackButton());
        
        // Fill empty slots
        fillEmpty();
    }
    
    private void addBiomeItem(int slot, Biome biome, Material icon) {
        Optional<BiomeData> biomeDataOpt = plugin.getBiomeManager().getBiomeData(biome);
        String biomeName = plugin.getBiomeManager().getBiomeDisplayName(biome);
        
        String monsterChance = biomeDataOpt.map(data -> String.valueOf(data.getMonsterChance())).orElse("Unknown");
        
        String[] lore = {
            "&7Monster Chance: &c" + monsterChance + "%",
            "&7Fish Available: &e" + plugin.getDropManager().getDropsForBiome(biome).size(),
            "",
            "&aClick to view fish!"
        };
        
        inventory.setItem(slot, createGuiItem(icon, "&f" + biomeName, lore));
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick) {
        if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }
        
        playSound(Sound.UI_BUTTON_CLICK);
        
        if (slot == 49) { // Back
            new MainMenuGUI(plugin, player).open();
        }
        // TODO: Open specific biome fish view
    }
}
