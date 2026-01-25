package loyfael.managers;

import loyfael.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages plugin configuration and messages
 * Handles color codes and message formatting
 */
public class ConfigManager {

    private final Main plugin;
    private FileConfiguration config;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Load or reload configuration
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    /**
     * Get a formatted message from config
     * @param key Message key
     * @return Formatted message with colors
     */
    public String getMessage(String key) {
        String message = config.getString("messages." + key, "&cMessage manquant: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Get a formatted message with placeholder replacement
     * @param key Message key
     * @param placeholder Placeholder to replace
     * @param value Value to replace placeholder with
     * @return Formatted message
     */
    public String getMessage(String key, String placeholder, String value) {
        String message = getMessage(key);
        return message.replace("%" + placeholder + "%", value);
    }

    /**
     * Get GUI item name
     * @param itemKey Item key
     * @return Formatted item name
     */
    public String getGuiItemName(String itemKey) {
        String name = config.getString("gui.items." + itemKey + ".name", "&cNom manquant: " + itemKey);
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    /**
     * Get GUI item lore
     * @param itemKey Item key
     * @return Formatted lore list
     */
    public List<String> getGuiItemLore(String itemKey) {
        List<String> lore = config.getStringList("gui.items." + itemKey + ".lore");
        return lore.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }

    /**
     * Get GUI item material
     * @param itemKey Item key
     * @return Material name
     */
    public String getGuiItemMaterial(String itemKey) {
        return config.getString("gui.items." + itemKey + ".material", "STONE");
    }

    /**
     * Get GUI title
     * @return Formatted GUI title
     */
    public String getGuiTitle() {
        String title = config.getString("gui.title", "&8Mode de Jeu");
        return ChatColor.translateAlternateColorCodes('&', title);
    }

    /**
     * Get database type
     * @return Database type (sqlite or mysql)
     */
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    /**
     * Get mode change delay in milliseconds
     * @return Delay in milliseconds
     */
    public long getModeChangeDelay() {
        return config.getLong("mode-change-delay", 3000);
    }

    /**
     * Get cooldown time for mode changes (configurable)
     * @return Cooldown time in milliseconds
     */
    public long getCooldownTime() {
        return config.getLong("mode-change-cooldown", 24 * 60 * 60 * 1000); // 24h par défaut
    }

    /**
     * Get MySQL configuration
     */
    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "aethersafemode");
    }

    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "username");
    }

    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "password");
    }

    /**
     * Get SQLite file path
     * @return SQLite file path
     */
    public String getSQLiteFile() {
        return config.getString("database.sqlite.file", "data.db");
    }

    /**
     * Get formatted prefix for all messages
     * @return Formatted prefix
     */
    public String getPrefix() {
        return getMessage("prefix");
    }

    /**
     * Get detailed messages for mode activation
     * @param key Message key (safe-mode-activated or unsafe-mode-activated)
     * @return List of formatted messages
     */
    public List<String> getDetailedMessages(String key) {
        List<String> messages = config.getStringList("detailed-messages." + key);
        return messages.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }
}
