package loyfael.litefish.config;

import loyfael.litefish.LiteFish;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Manages all configuration files for LiteFish
 */
public class ConfigManager {
    
    private final LiteFish plugin;
    private final File dataFolder;
    
    // Configuration files
    private FileConfiguration config;
    private FileConfiguration biomesConfig;
    private FileConfiguration dropsConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration tournamentsConfig;
    
    // Configuration file objects
    private File configFile;
    private File biomesFile;
    private File dropsFile;
    private File messagesFile;
    private File tournamentsFile;
    
    public ConfigManager(LiteFish plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
    }
    
    public void loadConfigs() {
        // Create data folder if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Load main config
        loadMainConfig();
        
        // Load other configs
        loadBiomesConfig();
        loadDropsConfig();
        loadMessagesConfig();
        loadTournamentsConfig();
    }
    
    private void loadMainConfig() {
        configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Add defaults
        addConfigDefaults();
        saveConfig();
    }
    
    private void loadBiomesConfig() {
        biomesFile = new File(dataFolder, "biomes.yml");
        if (!biomesFile.exists()) {
            saveResource("biomes.yml");
        }
        biomesConfig = YamlConfiguration.loadConfiguration(biomesFile);
    }
    
    private void loadDropsConfig() {
        dropsFile = new File(dataFolder, "drops.yml");
        if (!dropsFile.exists()) {
            saveResource("drops.yml");
        }
        dropsConfig = YamlConfiguration.loadConfiguration(dropsFile);
    }
    
    private void loadMessagesConfig() {
        messagesFile = new File(dataFolder, "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml");
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    private void loadTournamentsConfig() {
        tournamentsFile = new File(dataFolder, "tournaments.yml");
        if (!tournamentsFile.exists()) {
            saveResource("tournaments.yml");
        }
        tournamentsConfig = YamlConfiguration.loadConfiguration(tournamentsFile);
    }
    
    private void addConfigDefaults() {
        // Database settings
        config.addDefault("database.enabled", false);
        config.addDefault("database.type", "SQLITE");
        config.addDefault("database.host", "localhost");
        config.addDefault("database.port", 3306);
        config.addDefault("database.database", "litefish");
        config.addDefault("database.username", "root");
        config.addDefault("database.password", "");
        
        // General settings
        config.addDefault("general.check-updates", true);
        config.addDefault("general.auto-sell", false);
        config.addDefault("general.sound-effects", true);
        config.addDefault("general.particle-effects", true);
        
        // Fishing mechanics
        config.addDefault("fishing.custom-drops-only", false);
        config.addDefault("fishing.vanilla-exp", true);
        config.addDefault("fishing.min-fishing-time", 5);
        config.addDefault("fishing.max-fishing-time", 30);
        config.addDefault("fishing.difficulty-enabled", true);
        
        // Economy
        config.addDefault("economy.enabled", true);
        config.addDefault("economy.sell-multiplier", 1.0);
        config.addDefault("economy.auto-sell-inventory-full", false);
        
        // Tournaments
        config.addDefault("tournaments.enabled", true);
        config.addDefault("tournaments.auto-start", false);
        config.addDefault("tournaments.broadcast-interval", 300);
        
        // WorldGuard integration
        config.addDefault("worldguard.enabled", true);
        config.addDefault("worldguard.respect-fishing-flag", true);
        
        // Nexo integration
        config.addDefault("nexo.enabled", true);
        config.addDefault("nexo.custom-baits", true);
        config.addDefault("nexo.custom-rods", true);
        
        config.options().copyDefaults(true);
    }
    
    private void saveResource(String resourcePath) {
        try {
            InputStream inputStream = plugin.getResource(resourcePath);
            if (inputStream != null) {
                File outFile = new File(dataFolder, resourcePath);
                Files.copy(inputStream, outFile.toPath());
                inputStream.close();
            } else {
                // Create default file if resource doesn't exist
                createDefaultFile(resourcePath);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save resource " + resourcePath, e);
            createDefaultFile(resourcePath);
        }
    }
    
    private void createDefaultFile(String fileName) {
        File file = new File(dataFolder, fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                // Add some default content based on file name
                switch (fileName) {
                    case "biomes.yml":
                        createDefaultBiomesConfig(config);
                        break;
                    case "drops.yml":
                        createDefaultDropsConfig(config);
                        break;
                    case "messages.yml":
                        createDefaultMessagesConfig(config);
                        break;
                    case "tournaments.yml":
                        createDefaultTournamentsConfig(config);
                        break;
                }
                
                config.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create default file " + fileName, e);
        }
    }
    
    private void createDefaultBiomesConfig(YamlConfiguration config) {
        config.set("biomes.OCEAN.name", "Ocean");
        config.set("biomes.OCEAN.color", "#3F76E4");
        config.set("biomes.OCEAN.monster-chance", 15);
        
        config.set("biomes.RIVER.name", "River");
        config.set("biomes.RIVER.color", "#0084FF");
        config.set("biomes.RIVER.monster-chance", 5);
        
        config.set("biomes.FROZEN_OCEAN.name", "Frozen Ocean");
        config.set("biomes.FROZEN_OCEAN.color", "#7DC5FF");
        config.set("biomes.FROZEN_OCEAN.monster-chance", 25);
    }
    
    private void createDefaultDropsConfig(YamlConfiguration config) {
        // Default fish drops
        config.set("drops.cod.material", "COD");
        config.set("drops.cod.chance", 40.0);
        config.set("drops.cod.experience", 5);
        config.set("drops.cod.price", 10.0);
        
        config.set("drops.salmon.material", "SALMON");
        config.set("drops.salmon.chance", 25.0);
        config.set("drops.salmon.experience", 7);
        config.set("drops.salmon.price", 15.0);
        
        config.set("drops.tropical_fish.material", "TROPICAL_FISH");
        config.set("drops.tropical_fish.chance", 15.0);
        config.set("drops.tropical_fish.experience", 10);
        config.set("drops.tropical_fish.price", 25.0);
        
        config.set("drops.pufferfish.material", "PUFFERFISH");
        config.set("drops.pufferfish.chance", 10.0);
        config.set("drops.pufferfish.experience", 15);
        config.set("drops.pufferfish.price", 40.0);
    }
    
    private void createDefaultMessagesConfig(YamlConfiguration config) {
        config.set("prefix", "&b[LiteFish] &f");
        config.set("no-permission", "&cYou don't have permission to use this command!");
        config.set("reload-success", "&aConfiguration reloaded successfully!");
        config.set("player-not-found", "&cPlayer not found!");
        config.set("fishing.caught", "&aYou caught a {fish}!");
        config.set("fishing.sold", "&aYou sold {amount} fish for &e${money}!");
        config.set("tournament.started", "&eFishing tournament has started!");
        config.set("tournament.ended", "&eFishing tournament has ended!");
    }
    
    private void createDefaultTournamentsConfig(YamlConfiguration config) {
        config.set("tournaments.daily.enabled", true);
        config.set("tournaments.daily.duration", 3600); // 1 hour in seconds
        config.set("tournaments.daily.start-time", "20:00");
        config.set("tournaments.daily.rewards.1", "100");
        config.set("tournaments.daily.rewards.2", "50");
        config.set("tournaments.daily.rewards.3", "25");
    }
    
    public void reloadConfigs() {
        loadConfigs();
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
        }
    }
    
    public void saveBiomesConfig() {
        try {
            biomesConfig.save(biomesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save biomes.yml", e);
        }
    }
    
    public void saveDropsConfig() {
        try {
            dropsConfig.save(dropsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save drops.yml", e);
        }
    }
    
    // Getters
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getBiomesConfig() {
        return biomesConfig;
    }
    
    public FileConfiguration getDropsConfig() {
        return dropsConfig;
    }
    
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    public FileConfiguration getTournamentsConfig() {
        return tournamentsConfig;
    }
    
    // Additional helper methods
    public int getFishingCooldownMs() {
        return config.getInt("fishing.cooldown-seconds", 5) * 1000;
    }
    
    public boolean isAnimationEnabled() {
        return config.getBoolean("fishing.animations.enabled", true);
    }
    
    public boolean isMiniGameEnabled() {
        return config.getBoolean("fishing.mini-game-enabled", true);
    }
    
    public double getMiniGameChance() {
        return config.getDouble("fishing.mini-game-chance", 0.30);
    }
    
    public boolean isVoidFishingEnabled() {
        return config.getBoolean("fishing.void-fishing-enabled", true);
    }
}
