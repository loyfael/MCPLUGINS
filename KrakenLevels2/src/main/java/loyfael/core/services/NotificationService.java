package loyfael.core.services;

import loyfael.api.interfaces.INotificationService;
import loyfael.api.interfaces.IConfigurationService;
import loyfael.utils.Utils;
import loyfael.Main;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service de notifications centralisé
 * Principe de responsabilité unique : gestion des messages uniquement
 */
public class NotificationService implements INotificationService {

    private final IConfigurationService configService;
    private final Map<String, String> messages = new ConcurrentHashMap<>();
    private FileConfiguration messageConfig;
    private File messagesFile;

    public NotificationService(IConfigurationService configService) {
        this.configService = configService;
        initialize();
    }

    @Override
    public void sendMessage(Player player, String messageKey, Object... placeholders) {
        if (player == null || messageKey == null) return;

        String message = getMessage(messageKey, placeholders);
        if (!message.isEmpty()) {
            player.sendMessage(Utils.colorize(message));
        }
    }

    @Override
    public void sendActionBar(Player player, String messageKey, Object... placeholders) {
        if (player == null || messageKey == null) return;

        String message = getMessage(messageKey, placeholders);
        if (!message.isEmpty()) {
            Utils.sendActionBar(player, message);
        }
    }

    @Override
    public void sendTitle(Player player, String titleKey, String subtitleKey, Object... placeholders) {
        if (player == null) return;

        String title = titleKey != null ? getMessage(titleKey, placeholders) : "";
        String subtitle = subtitleKey != null ? getMessage(subtitleKey, placeholders) : "";

        if (!title.isEmpty() || !subtitle.isEmpty()) {
            Utils.sendTitle(player, title, subtitle);
        }
    }

    @Override
    public void broadcast(String messageKey, Object... placeholders) {
        if (messageKey == null) return;

        String message = getMessage(messageKey, placeholders);
        if (!message.isEmpty()) {
            Main.getInstance().getServer().broadcastMessage(Utils.colorize(message));
        }
    }

    @Override
    public void logConsole(String messageKey, Object... placeholders) {
        if (messageKey == null) return;

        String message = getMessage(messageKey, placeholders);
        if (!message.isEmpty()) {
            Utils.sendConsoleLog(message);
        }
    }

    @Override
    public String getMessage(String messageKey, Object... placeholders) {
        if (messageKey == null) return "";

        String message = messages.getOrDefault(messageKey, "§c[Message manquant: " + messageKey + "]");

        // Remplacer les placeholders
        if (placeholders != null && placeholders.length > 0) {
            for (int i = 0; i < placeholders.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(placeholders[i]));
            }
        }

        return message;
    }

    public void initialize() {
        try {
            File dataFolder = Main.getInstance().getDataFolder();
            messagesFile = new File(dataFolder, "messages.yml");

            if (!messagesFile.exists()) {
                Main.getInstance().saveResource("messages.yml", false);
            }

            messageConfig = YamlConfiguration.loadConfiguration(messagesFile);
            loadMessages();

        } catch (Exception e) {
            Utils.sendConsoleLog("&cErreur lors de l'initialisation des messages: " + e.getMessage());
        }
    }

    @Override
    public void reloadMessages() {
        try {
            messageConfig = YamlConfiguration.loadConfiguration(messagesFile);
            loadMessages();
        } catch (Exception e) {
            Utils.sendConsoleLog("&cErreur lors du rechargement des messages: " + e.getMessage());
        }
    }

    private void loadMessages() {
        messages.clear();
        loadMessagesFromConfig("", messageConfig.getConfigurationSection("messages"));
    }

    /**
     * Charge récursivement les messages depuis la configuration
     */
    private void loadMessagesFromConfig(String prefix, org.bukkit.configuration.ConfigurationSection section) {
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (section.isConfigurationSection(key)) {
                loadMessagesFromConfig(fullKey, section.getConfigurationSection(key));
            } else {
                String value = section.getString(key);
                if (value != null) {
                    messages.put(fullKey, value);
                }
            }
        }
    }
}
