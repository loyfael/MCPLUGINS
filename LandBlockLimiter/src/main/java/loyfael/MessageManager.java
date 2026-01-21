package loyfael;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class MessageManager {
    private final JavaPlugin plugin;
    private FileConfiguration messagesConfig;
    private final Map<String, String> messageCache;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messageCache = new HashMap<>();
        loadMessages();
    }

    public void loadMessages() {
        // Save default file if it doesn't exist
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        // Load configuration
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load default configuration for missing values
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messagesConfig.setDefaults(defConfig);
        }

        // Cache all messages
        messageCache.clear();
        for (String key : messagesConfig.getKeys(false)) {
            String message = messagesConfig.getString(key, "Missing message: " + key);
            messageCache.put(key, colorize(message));
        }

        plugin.getLogger().info("Messages loaded: " + messageCache.size() + " entries");
    }

    private String colorize(String message) {
        return message.replace('&', '§');
    }

    /**
     * Gets a message with placeholders
     */
    public String getMessage(String key, Object... placeholders) {
        String message = messageCache.getOrDefault(key, "§cMissing message: " + key);

        // Replace placeholders
        if (placeholders.length > 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    String placeholder = "{" + placeholders[i] + "}";
                    String value = String.valueOf(placeholders[i + 1]);
                    message = message.replace(placeholder, value);
                }
            }
        }

        return message;
    }

    /**
     * Gets a simple message without placeholders
     */
    public String getMessage(String key) {
        return messageCache.getOrDefault(key, "§cMissing message: " + key);
    }

    /**
     * Checks if a message exists
     */
    public boolean hasMessage(String key) {
        return messageCache.containsKey(key);
    }

    /**
     * Reloads messages from file
     */
    public void reloadMessages() {
        try {
            loadMessages();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error while reloading messages", e);
        }
    }
}
