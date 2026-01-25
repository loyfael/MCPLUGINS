package loyfael.config;

import loyfael.Main;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final Main plugin;
    private FileConfiguration config;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Valeurs par défaut si absentes
        setDefaults();
    }

    private void setDefaults() {
        config.addDefault("mongodb.connection-string", "mongodb://localhost:27017");
        config.addDefault("mongodb.database", "aetherplayershop");

        config.addDefault("shop.max-shops-per-player", 6);
        config.addDefault("shop.teleport-enabled", true);
        config.addDefault("shop.teleport-delay", 3);

        config.addDefault("gui.menu-title", "§6§lAether Player Shop");
        config.addDefault("gui.items-per-page", 45);

        config.addDefault("cache.max-size", 1000);
        config.addDefault("cache.expire-minutes", 30);

        config.addDefault("effects.sound-enabled", true);
        config.addDefault("effects.particle-enabled", true);

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    // MongoDB Configuration
    public String getMongoConnectionString() {
        return config.getString("mongodb.connection-string", "mongodb://localhost:27017");
    }

    public String getMongoDatabaseName() {
        return config.getString("mongodb.database", "aetherplayershop");
    }

    // Shop Configuration
    public int getMaxShopsPerPlayer() {
        return config.getInt("shop.max-shops-per-player", 6);
    }

    public boolean isTeleportEnabled() {
        return config.getBoolean("shop.teleport-enabled", true);
    }

    public int getTeleportDelay() {
        return config.getInt("shop.teleport-delay", 3);
    }

    // GUI Configuration
    public String getMenuTitle() {
        return config.getString("gui.menu-title", "§6§lAether Player Shop");
    }

    public int getItemsPerPage() {
        return config.getInt("gui.items-per-page", 45);
    }

    // Cache Configuration
    public int getCacheMaxSize() {
        return config.getInt("cache.max-size", 1000);
    }

    public int getCacheExpireMinutes() {
        return config.getInt("cache.expire-minutes", 30);
    }

    // Effects Configuration
    public boolean isSoundEnabled() {
        return config.getBoolean("effects.sound-enabled", true);
    }

    public boolean isParticleEnabled() {
        return config.getBoolean("effects.particle-enabled", true);
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
}
