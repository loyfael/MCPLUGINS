package loyfael.managers;

import loyfael.LoyCustomMobs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Manages all configuration files for the plugin
 */
public class ConfigManager {

    private final LoyCustomMobs plugin;
    private FileConfiguration config;
    private FileConfiguration lootConfig;
    private FileConfiguration mobsConfig;

    private File configFile;
    private File lootFile;
    private File mobsFile;

    public ConfigManager(LoyCustomMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all configuration files
     */
    public void loadConfigs() {
        createDataFolder();
        loadMainConfig();
        loadLootConfig();
        loadMobsConfig();
    }

    /**
     * Create plugin data folder if it doesn't exist
     */
    private void createDataFolder() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }

    /**
     * Load main configuration file
     */
    private void loadMainConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            saveDefaultConfig("config.yml");
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Set default values if missing
        setDefaultConfigValues();
        saveMainConfig();
    }

    /**
     * Load loot configuration file
     */
    private void loadLootConfig() {
        lootFile = new File(plugin.getDataFolder(), "loot.yml");

        if (!lootFile.exists()) {
            saveDefaultConfig("loot.yml");
        }

        lootConfig = YamlConfiguration.loadConfiguration(lootFile);
    }

    /**
     * Load mobs configuration file
     */
    private void loadMobsConfig() {
        mobsFile = new File(plugin.getDataFolder(), "mobs.yml");

        if (!mobsFile.exists()) {
            saveDefaultConfig("mobs.yml");
        }

        mobsConfig = YamlConfiguration.loadConfiguration(mobsFile);
    }

    /**
     * Save default configuration from resources
     */
    private void saveDefaultConfig(String fileName) {
        try {
            InputStream inputStream = plugin.getResource(fileName);
            if (inputStream != null) {
                File targetFile = new File(plugin.getDataFolder(), fileName);
                Files.copy(inputStream, targetFile.toPath());
                inputStream.close();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save default " + fileName, e);
        }
    }

    /**
     * Set default configuration values
     */
    private void setDefaultConfigValues() {
        // Plugin settings
        config.addDefault("plugin.version", "2.0.0");
        config.addDefault("plugin.debug", false);
        config.addDefault("plugin.metrics", true);
        config.addDefault("plugin.auto-update", true);

        // Mob spawn settings
        config.addDefault("mobs.spawn-chance", 0.15);
        config.addDefault("mobs.max-level", 10);
        config.addDefault("mobs.level-scaling", 1.5);
        config.addDefault("mobs.health-multiplier", 2.0);
        config.addDefault("mobs.damage-multiplier", 1.5);

        // World settings
        config.addDefault("worlds.enabled-worlds", java.util.Arrays.asList("world", "world_nether", "world_the_end"));
        config.addDefault("worlds.disable-in-claimed-land", true);

        // Loot settings
        config.addDefault("loot.drop-chance", 0.75);
        config.addDefault("loot.rare-drop-chance", 0.25);
        config.addDefault("loot.epic-drop-chance", 0.05);

        // Experience settings
        config.addDefault("experience.bonus-multiplier", 2.0);
        config.addDefault("experience.max-bonus", 500);

        config.options().copyDefaults(true);
    }

    /**
     * Save main configuration
     */
    public void saveMainConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
        }
    }

    /**
     * Save loot configuration
     */
    public void saveLootConfig() {
        try {
            lootConfig.save(lootFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save loot.yml", e);
        }
    }

    /**
     * Save mobs configuration
     */
    public void saveMobsConfig() {
        try {
            mobsConfig.save(mobsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save mobs.yml", e);
        }
    }

    /**
     * Reload all configurations
     */
    public void reloadConfigs() {
        config = YamlConfiguration.loadConfiguration(configFile);
        lootConfig = YamlConfiguration.loadConfiguration(lootFile);
        mobsConfig = YamlConfiguration.loadConfiguration(mobsFile);
    }

    // Getters for configuration values
    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getLootConfig() { return lootConfig; }
    public FileConfiguration getMobsConfig() { return mobsConfig; }

    public boolean isDebugEnabled() { return config.getBoolean("plugin.debug", false); }
    public boolean isMetricsEnabled() { return config.getBoolean("plugin.metrics", true); }
    public boolean isAutoUpdateEnabled() { return config.getBoolean("plugin.auto-update", true); }

    public double getSpawnChance() { return config.getDouble("mobs.spawn-chance", 0.15); }
    public int getMaxLevel() { return config.getInt("mobs.max-level", 10); }
    public double getLevelScaling() { return config.getDouble("mobs.level-scaling", 1.5); }
    public double getHealthMultiplier() { return config.getDouble("mobs.health-multiplier", 2.0); }
    public double getDamageMultiplier() { return config.getDouble("mobs.damage-multiplier", 1.5); }

    public java.util.List<String> getEnabledWorlds() {
        return config.getStringList("worlds.enabled-worlds");
    }
    public boolean isDisableInClaimedLand() {
        return config.getBoolean("worlds.disable-in-claimed-land", true);
    }

    public double getLootDropChance() { return config.getDouble("loot.drop-chance", 0.75); }
    public double getRareDropChance() { return config.getDouble("loot.rare-drop-chance", 0.25); }
    public double getEpicDropChance() { return config.getDouble("loot.epic-drop-chance", 0.05); }

    public double getExperienceMultiplier() { return config.getDouble("experience.bonus-multiplier", 2.0); }
    public int getMaxExperienceBonus() { return config.getInt("experience.max-bonus", 500); }
}
