package loyfael.litefish.managers;

import loyfael.litefish.LiteFish;
import loyfael.litefish.models.BiomeData;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages biome-specific fishing configurations
 */
public class BiomeManager {
    
    private final LiteFish plugin;
    private final Map<Biome, BiomeData> biomeData;
    
    public BiomeManager(LiteFish plugin) {
        this.plugin = plugin;
        this.biomeData = new HashMap<>();
        loadBiomes();
    }
    
    public void loadBiomes() {
        biomeData.clear();
        
        FileConfiguration config = plugin.getConfigManager().getBiomesConfig();
        ConfigurationSection biomesSection = config.getConfigurationSection("biomes");
        
        if (biomesSection == null) {
            plugin.getLogger().warning("No biomes section found in biomes.yml");
            createDefaultBiomes();
            return;
        }
        
        for (String biomeKey : biomesSection.getKeys(false)) {
            try {
                Biome biome = Biome.valueOf(biomeKey.toUpperCase());
                ConfigurationSection biomeSection = biomesSection.getConfigurationSection(biomeKey);
                
                if (biomeSection != null) {
                    BiomeData data = new BiomeData(
                        biome,
                        biomeSection.getString("name", biome.name()),
                        biomeSection.getString("color", "#3F76E4"),
                        biomeSection.getInt("monster-chance", 15)
                    );
                    
                    biomeData.put(biome, data);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid biome name: " + biomeKey);
            }
        }
        
        plugin.getLogger().info("Loaded " + biomeData.size() + " biome configurations");
    }
    
    private void createDefaultBiomes() {
        // Ocean biomes
        biomeData.put(Biome.OCEAN, new BiomeData(Biome.OCEAN, "Ocean", "#3F76E4", 15));
        biomeData.put(Biome.DEEP_OCEAN, new BiomeData(Biome.DEEP_OCEAN, "Deep Ocean", "#1A4A8A", 25));
        biomeData.put(Biome.FROZEN_OCEAN, new BiomeData(Biome.FROZEN_OCEAN, "Frozen Ocean", "#7DC5FF", 20));
        biomeData.put(Biome.WARM_OCEAN, new BiomeData(Biome.WARM_OCEAN, "Warm Ocean", "#5FB3FF", 10));
        biomeData.put(Biome.LUKEWARM_OCEAN, new BiomeData(Biome.LUKEWARM_OCEAN, "Lukewarm Ocean", "#4A9FFF", 12));
        biomeData.put(Biome.COLD_OCEAN, new BiomeData(Biome.COLD_OCEAN, "Cold Ocean", "#2A6FFF", 18));
        
        // River biomes
        biomeData.put(Biome.RIVER, new BiomeData(Biome.RIVER, "River", "#0084FF", 5));
        biomeData.put(Biome.FROZEN_RIVER, new BiomeData(Biome.FROZEN_RIVER, "Frozen River", "#B8E6FF", 8));
        
        // Swamp
        biomeData.put(Biome.SWAMP, new BiomeData(Biome.SWAMP, "Swamp", "#4A7C59", 30));
        biomeData.put(Biome.MANGROVE_SWAMP, new BiomeData(Biome.MANGROVE_SWAMP, "Mangrove Swamp", "#6B8E5A", 25));
        
        plugin.getLogger().info("Created default biome configurations");
    }
    
    public Optional<BiomeData> getBiomeData(Biome biome) {
        return Optional.ofNullable(biomeData.get(biome));
    }
    
    public BiomeData getBiomeData(Location location) {
        Biome biome = location.getBlock().getBiome();
        return getBiomeDataOrDefault(biome);
    }
    
    public BiomeData getBiomeDataOrDefault(Biome biome) {
        return biomeData.getOrDefault(biome, getDefaultBiomeData(biome));
    }
    
    private BiomeData getDefaultBiomeData(Biome biome) {
        return new BiomeData(biome, biome.name(), "#3F76E4", 15);
    }
    
    public Map<Biome, BiomeData> getAllBiomeData() {
        return new HashMap<>(biomeData);
    }
    
    public void setBiomeData(Biome biome, BiomeData data) {
        biomeData.put(biome, data);
        saveBiomeData(biome, data);
    }
    
    private void saveBiomeData(Biome biome, BiomeData data) {
        FileConfiguration config = plugin.getConfigManager().getBiomesConfig();
        String path = "biomes." + biome.name();
        
        config.set(path + ".name", data.getName());
        config.set(path + ".color", data.getColor());
        config.set(path + ".monster-chance", data.getMonsterChance());
        
        plugin.getConfigManager().saveBiomesConfig();
    }
    
    public void reload() {
        loadBiomes();
    }
    
    public boolean hasBiomeData(Biome biome) {
        return biomeData.containsKey(biome);
    }
    
    public int getTotalConfiguredBiomes() {
        return biomeData.size();
    }
    
    public double getMonsterChanceMultiplier(Biome biome) {
        BiomeData data = getBiomeDataOrDefault(biome);
        return data.getMonsterChance() / 100.0; // Convert percentage to multiplier
    }
    
    public String getBiomeDisplayName(Biome biome) {
        BiomeData data = getBiomeDataOrDefault(biome);
        return data.getName();
    }
    
    public String getBiomeColor(Biome biome) {
        BiomeData data = getBiomeDataOrDefault(biome);
        return data.getColor();
    }
}
