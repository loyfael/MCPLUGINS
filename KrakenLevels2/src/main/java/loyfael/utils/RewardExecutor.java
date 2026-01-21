package loyfael.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import loyfael.Main;

/**
 * Centralise the execution of reward commands to guarantee thread-safety
 * and provide native integrations (Vault, LuckPerms) when available.
 */
public final class RewardExecutor {

    private static final Pattern ECONOMY_COMMAND_PATTERN = Pattern.compile(
        "(?i)^money\\s+(\\S+)\\s+vault\\s+give\\s+([0-9]+)$"
    );

    private static final Pattern LUCKPERMS_COMMAND_PATTERN = Pattern.compile(
        "(?i)^lp\\s+user\\s+(\\S+)\\s+parent\\s+(set|add|remove)\\s+(\\S+)$"
    );

    private RewardExecutor() {
        // Utility class
    }

    /**
     * Execute a list of reward commands for a player.
     */
    public static void executeCommands(Player player, List<String> commands) {
        if (player == null || commands == null || commands.isEmpty()) {
            return;
        }

        for (String rawCommand : commands) {
            executeCommand(player, rawCommand);
        }
    }

    /**
     * Execute a single reward command for the given player.
     * The %player% placeholder is handled automatically.
     */
    public static void executeCommand(Player player, String rawCommand) {
        if (player == null || rawCommand == null) {
            return;
        }

        String processed = rawCommand.replace("%player%", player.getName()).trim();
        if (processed.isEmpty()) {
            return;
        }

        logDebug("Executing reward command: " + processed);

        if (tryHandleEconomy(processed)) {
            return;
        }

        if (tryHandleLuckPerms(processed)) {
            return;
        }

        runConsoleCommand(processed);
    }

    /**
     * Broadcast reward messages (with color codes) to the server.
     */
    public static void broadcastMessages(Player player, List<String> messages) {
        if (player == null || messages == null || messages.isEmpty()) {
            return;
        }

        runSync(() -> {
            for (String rawMessage : messages) {
                if (rawMessage == null || rawMessage.trim().isEmpty()) {
                    continue;
                }
                String processed = rawMessage.replace("%player%", player.getName()).replace("&", "§");
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(processed));
                Bukkit.getConsoleSender().sendMessage(processed);
            }
        });
    }

    private static boolean tryHandleEconomy(String command) {
        Matcher matcher = ECONOMY_COMMAND_PATTERN.matcher(command);
        if (!matcher.matches()) {
            return false;
        }

        Main plugin = Main.getInstance();
        if (plugin == null) {
            return false;
        }

        Economy economy = plugin.getEconomy();
        if (economy == null) {
            logWarning("Economy reward command ignored because Vault economy is not configured: " + command);
            return false;
        }

        String targetName = matcher.group(1);
        double amount;
        try {
            amount = Double.parseDouble(matcher.group(2));
        } catch (NumberFormatException ex) {
            logWarning("Invalid amount in economy reward command: " + command);
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        AtomicReference<EconomyResponse> responseRef = new AtomicReference<>();
        runSync(() -> responseRef.set(economy.depositPlayer(target, amount)));

        EconomyResponse response = responseRef.get();
        if (response == null || !response.transactionSuccess()) {
            logWarning("Vault deposit failed for command '" + command + "': " +
                (response != null ? response.errorMessage : "no response"));
            return false; // Fall back to executing the original command
        }

        logDebug("Vault deposit success: " + amount + " -> " + targetName);
        return true;
    }

    private static boolean tryHandleLuckPerms(String command) {
        Matcher matcher = LUCKPERMS_COMMAND_PATTERN.matcher(command);
        if (!matcher.matches()) {
            return false;
        }

        LuckPerms luckPerms;
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException ex) {
            logWarning("LuckPerms reward command ignored because the API is not available: " + command);
            return false;
        }

        String targetName = matcher.group(1);
        String action = matcher.group(2).toLowerCase(Locale.ROOT);
        String group = matcher.group(3);

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = offlinePlayer.getUniqueId();
        if (uuid == null) {
            logWarning("Unable to resolve UUID for LuckPerms reward command: " + command);
            return false;
        }

        luckPerms.getUserManager().modifyUser(uuid, user -> handleLuckPermsAction(user, action, group))
            .exceptionally(throwable -> {
                logWarning("Failed to apply LuckPerms reward command '" + command + "': " + throwable.getMessage());
                return null;
            });
        // Return false to also execute the original command as a fallback
        return false;
    }

    private static void handleLuckPermsAction(User user, String action, String group) {
        switch (action) {
            case "set" -> {
                List<Node> nodesToRemove = new ArrayList<>();
                for (Node node : user.data().toCollection()) {
                    if (node instanceof InheritanceNode) {
                        nodesToRemove.add(node);
                    }
                }
                nodesToRemove.forEach(node -> user.data().remove(node));
                user.data().add(InheritanceNode.builder(group).value(true).build());
            }
            case "add" -> user.data().add(InheritanceNode.builder(group).value(true).build());
            case "remove" -> {
                List<Node> nodesToRemove = new ArrayList<>();
                for (Node node : user.data().toCollection()) {
                    if (node instanceof InheritanceNode inheritanceNode &&
                        inheritanceNode.getGroupName().equalsIgnoreCase(group)) {
                        nodesToRemove.add(node);
                    }
                }
                nodesToRemove.forEach(node -> user.data().remove(node));
            }
            default -> logWarning("Unsupported LuckPerms action: " + action);
        }
    }

    private static void runConsoleCommand(String command) {
        runSync(() -> {
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            if (!success) {
                logWarning("Reward command returned false: " + command);
            }
        });
    }

    private static void runSync(Runnable runnable) {
        if (runnable == null) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Main plugin = Main.getInstance();
            if (plugin != null) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        }
    }

    private static void logWarning(String message) {
        if (message == null) {
            return;
        }

        Main plugin = Main.getInstance();
        if (plugin != null) {
            plugin.getLogger().warning(message);
        } else {
            Bukkit.getLogger().warning("[KrakenLevels] " + message);
        }
    }

    private static void logDebug(String message) {
        if (message == null) {
            return;
        }

        Main plugin = Main.getInstance();
        if (plugin != null) {
            plugin.getLogger().info(message);
        }
    }
}
