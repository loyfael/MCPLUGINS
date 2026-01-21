package loyfael.utils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import loyfael.Main;
import loyfael.api.interfaces.IPlayerService;

/**
 * Utility class for various helper functions
 * Provides methods for colorizing messages, logging, and player data management
 */
public final class Utils {

    // Cache to avoid repeated calls
    private static final ConcurrentHashMap<String, String> configCache = new ConcurrentHashMap<>();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    // Private constructor to prevent instantiation
    private Utils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Colorize a message with Bukkit color codes
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Alias for the colorize method for compatibility
     */
    public static String color(String message) {
        return colorize(message);
    }

    /**
     * Colorize a list of strings
     */
    public static List<String> colorize(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return List.of();
        }
        return strings.stream()
                .map(Utils::colorize)
                .collect(Collectors.toList());
    }

    /**
     * Send a message to the console with colorization
     */
    public static void sendConsoleLog(String message) {
        try {
            if (message == null) {
                System.err.println("[KrakenLevels] Attempted to log null message");
                return;
            }

            String formattedMessage = translateColorCodes(message);
            System.out.println("[KrakenLevels] " + formattedMessage);

        } catch (Exception e) {
            System.err.println("[KrakenLevels] Error logging message: " + e.getMessage());
            // Fallback to basic logging
            try {
                System.out.println("[KrakenLevels] " + (message != null ? message : "null"));
            } catch (Exception fallbackError) {
                System.err.println("[KrakenLevels] Critical logging error: " + fallbackError.getMessage());
            }
        }
    }

    /**
     * Translates color codes in a message
     */
    public static String translateColorCodes(String message) {
        try {
            if (message == null || message.isEmpty()) {
                return "";
            }

            // Simple color code translation for console
            return message.replace("&a", "")
                         .replace("&b", "")
                         .replace("&c", "")
                         .replace("&d", "")
                         .replace("&e", "")
                         .replace("&f", "")
                         .replace("&0", "")
                         .replace("&1", "")
                         .replace("&2", "")
                         .replace("&3", "")
                         .replace("&4", "")
                         .replace("&5", "")
                         .replace("&6", "")
                         .replace("&7", "")
                         .replace("&8", "")
                         .replace("&9", "")
                         .replace("&l", "")
                         .replace("&m", "")
                         .replace("&n", "")
                         .replace("&o", "")
                         .replace("&r", "");

        } catch (Exception e) {
            System.err.println("Error translating color codes: " + e.getMessage());
            return message != null ? message : "";
        }
    }

    /**
     * Safely formats a string with parameters
     */
    public static String formatMessage(String template, Object... args) {
        try {
            if (template == null) {
                return "null";
            }

            if (args == null || args.length == 0) {
                return template;
            }

            return String.format(template, args);

        } catch (Exception e) {
            System.err.println("Error formatting message: " + e.getMessage());
            return template != null ? template : "null";
        }
    }

    /**
     * Safely converts an object to string
     */
    public static String safeToString(Object obj) {
        try {
            if (obj == null) {
                return "null";
            }

            return obj.toString();

        } catch (Exception e) {
            return "toString_error";
        }
    }

    /**
     * Safely parses an integer with default fallback
     */
    public static int safeParseInt(String str, int defaultValue) {
        try {
            if (str == null || str.trim().isEmpty()) {
                return defaultValue;
            }

            return Integer.parseInt(str.trim());

        } catch (NumberFormatException e) {
            System.err.println("Error parsing integer '" + str + "': " + e.getMessage());
            return defaultValue;
        } catch (Exception e) {
            System.err.println("Unexpected error parsing integer: " + e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Safely parses a double with default fallback
     */
    public static double safeParseDouble(String str, double defaultValue) {
        try {
            if (str == null || str.trim().isEmpty()) {
                return defaultValue;
            }

            return Double.parseDouble(str.trim());

        } catch (NumberFormatException e) {
            System.err.println("Error parsing double '" + str + "': " + e.getMessage());
            return defaultValue;
        } catch (Exception e) {
            System.err.println("Unexpected error parsing double: " + e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Safely checks if a string is not null or empty
     */
    public static boolean isValidString(String str) {
        try {
            return str != null && !str.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Logs an error with full stack trace
     */
    public static void logError(String context, Exception e) {
        try {
            Main plugin = Main.getInstance();
            if (plugin != null) {
                plugin.getLogger().severe(context + ": " + e.getMessage());
                e.printStackTrace();
            } else {
                System.err.println("[KrakenLevels] " + context + ": " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception loggingError) {
            System.err.println("[KrakenLevels] Critical logging error: " + loggingError.getMessage());
        }
    }

    /**
     * Logs a warning message
     */
    public static void logWarning(String message) {
        try {
            Main plugin = Main.getInstance();
            if (plugin != null) {
                plugin.getLogger().warning(message);
            } else {
                System.err.println("[KrakenLevels] WARNING: " + message);
            }
        } catch (Exception e) {
            System.err.println("[KrakenLevels] Error logging warning: " + e.getMessage());
        }
    }

    /**
     * Envoie une action bar à un joueur (SOLID - utilise l'API moderne)
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;

        try {
            Component component = LEGACY_SERIALIZER.deserialize(colorize(message));
            player.sendActionBar(component);
        } catch (Exception e) {
            // Fallback pour les versions plus anciennes
            player.sendMessage(colorize("&7[ActionBar] " + message));
        }
    }

    /**
     * Envoie un titre à un joueur
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        if (player == null) return;

        try {
            Component titleComponent = title != null ? LEGACY_SERIALIZER.deserialize(colorize(title)) : Component.empty();
            Component subtitleComponent = subtitle != null ? LEGACY_SERIALIZER.deserialize(colorize(subtitle)) : Component.empty();

            player.sendTitlePart(net.kyori.adventure.title.TitlePart.TITLE, titleComponent);
            player.sendTitlePart(net.kyori.adventure.title.TitlePart.SUBTITLE, subtitleComponent);
        } catch (Exception e) {
            // Fallback
            if (title != null && !title.isEmpty()) {
                player.sendMessage(colorize("&6&l" + title));
            }
            if (subtitle != null && !subtitle.isEmpty()) {
                player.sendMessage(colorize("&e" + subtitle));
            }
        }
    }

    /**
     * Récupère le montant de boutons d'un joueur (SOLID)
     */
    public static int getPlayerButtonAmount(Player player) {
        if (player == null) return 0;

        Main plugin = Main.getInstance();
        if (plugin == null) return 0;

        try {
            IPlayerService playerService = plugin.getPlayerService();
            if (playerService == null) return 0;

            String playerUuid = player.getUniqueId().toString();
            Optional<IPlayerService.PlayerData> playerData = playerService.getPlayerData(playerUuid);

            if (playerData.isPresent()) {
                return playerData.get().getButtonAmount();
            }

            // Si aucune donnée n'existe, créer un nouveau profil
            playerService.createPlayer(playerUuid, player.getName());
            return 0;

        } catch (Exception e) {
            plugin.getLogger().warning("Error while getting button amount for " + player.getName() + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Sets the player's button amount (SOLID)
     */
    public static void setPlayerButtonAmount(Player player, int amount) {
        if (player == null) return;

        Main plugin = Main.getInstance();
        if (plugin == null) return;

        try {
            IPlayerService playerService = plugin.getPlayerService();
            if (playerService == null) return;

            String playerUuid = player.getUniqueId().toString();
            Optional<IPlayerService.PlayerData> playerDataOpt = playerService.getPlayerData(playerUuid);

            if (playerDataOpt.isPresent()) {
                IPlayerService.PlayerData playerData = playerDataOpt.get();
                playerData.setButtonAmount(amount);
                playerService.savePlayerData(playerUuid, playerData);
            } else {
                // Créer un nouveau profil avec le montant spécifié
                IPlayerService.PlayerData newData = playerService.createPlayer(playerUuid, player.getName());
                newData.setButtonAmount(amount);
                playerService.savePlayerData(playerUuid, newData);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error while setting button amount for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Ajoute des boutons à un joueur (SOLID)
     */
    public static void addPlayerButtonAmount(Player player, int amount) {
        if (player == null || amount == 0) return;

        int currentAmount = getPlayerButtonAmount(player);
        setPlayerButtonAmount(player, Math.max(0, currentAmount + amount));
    }

    /**
     * Removes buttons from a player (SOLID)
     */
    public static void removePlayerButtonAmount(Player player, int amount) {
        if (player == null || amount <= 0) return;

        int currentAmount = getPlayerButtonAmount(player);
        setPlayerButtonAmount(player, Math.max(0, currentAmount - amount));
    }

    /**
     * Gets the player's level (SOLID)
     */
    public static int getPlayerLevel(Player player) {
        if (player == null) return 0;

        Main plugin = Main.getInstance();
        if (plugin == null) return 0;

        try {
            IPlayerService playerService = plugin.getPlayerService();
            if (playerService == null) return 0;

            return playerService.getPlayerLevel(player.getUniqueId().toString());

        } catch (Exception e) {
            plugin.getLogger().warning("Error while retrieving level for " + player.getName() + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Checks if a player exists in the database (SOLID)
     */
    public static boolean playerExists(Player player) {
        if (player == null) return false;

        Main plugin = Main.getInstance();
        if (plugin == null) return false;

        try {
            IPlayerService playerService = plugin.getPlayerService();
            if (playerService == null) return false;

            return playerService.playerExists(player.getUniqueId().toString());

        } catch (Exception e) {
            plugin.getLogger().warning("Error while checking existence for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Clears the configuration cache
     */
    public static void clearConfigCache() {
        configCache.clear();
        sendConsoleLog("&7Configuration cache cleared.");
    }

    /**
     * Conversion sécurisée vers entier
     */
    public static int safeIntegerConversion(Object value, int defaultValue) {
        if (value == null) return defaultValue;

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }
}
