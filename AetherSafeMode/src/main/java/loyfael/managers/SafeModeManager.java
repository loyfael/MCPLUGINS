package loyfael.managers;

import loyfael.Main;
import loyfael.gui.ConfirmationGUI;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player SafeMode states and transitions
 * Handles GUI interactions and database persistence
 */
public class SafeModeManager {

    private final Main plugin;
    private final Map<UUID, Boolean> playerModes = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> pendingChanges = new HashMap<>();

    public SafeModeManager(Main plugin) {
        this.plugin = plugin;
        loadAllOnlinePlayers();
    }

    /**
     * Load modes for all online players
     */
    private void loadAllOnlinePlayers() {
        plugin.getServer().getOnlinePlayers().forEach(this::loadPlayerMode);
    }

    /**
     * Load a player's mode from database
     */
    public void loadPlayerMode(Player player) {
        UUID uuid = player.getUniqueId();

        // Load asynchronously to avoid blocking main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean safeMode = plugin.getDatabaseManager().getPlayerMode(uuid);

                // Update cache on main thread
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        playerModes.put(uuid, safeMode);
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Save a player's mode to database
     */
    public void savePlayerMode(Player player) {
        UUID uuid = player.getUniqueId();
        Boolean mode = playerModes.get(uuid);

        if (mode != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getDatabaseManager().setPlayerMode(uuid, mode);
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    /**
     * Save a player's mode to database synchronously (for shutdown)
     */
    public void savePlayerModeSync(Player player) {
        UUID uuid = player.getUniqueId();
        Boolean mode = playerModes.get(uuid);

        if (mode != null) {
            plugin.getDatabaseManager().setPlayerMode(uuid, mode);
        }
    }

    /**
     * Save all online players' modes (synchronously for shutdown)
     */
    public void saveAllPlayers() {
        plugin.getServer().getOnlinePlayers().forEach(this::savePlayerModeSync);
    }

    /**
     * Check if a player is in safe mode
     */
    public boolean isSafeMode(Player player) {
        return playerModes.getOrDefault(player.getUniqueId(), true); // Default to safe
    }

    /**
     * Initiate mode toggle with GUI confirmation
     */
    public void initiateToggle(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancel any pending change
        cancelPendingChange(uuid);

        // Show mode selection GUI
        ConfirmationGUI gui = new ConfirmationGUI(plugin, player);
        plugin.getServer().getPluginManager().registerEvents(gui, plugin);
        gui.open();
    }

    /**
     * Switch mode immediately (called after confirmation)
     */
    public void switchMode(Player player, boolean targetMode) {
        UUID uuid = player.getUniqueId();
        boolean currentMode = isSafeMode(player);

        // If no change needed, just inform player
        if (currentMode == targetMode) {
            if (targetMode) {
                player.sendMessage(plugin.getConfigManager().getMessage("current-mode-safe"));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("current-mode-unsafe"));
            }
            return;
        }

        // Show switching message
        if (targetMode) {
            player.sendMessage(plugin.getConfigManager().getMessage("switching-to-safe"));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("switching-to-unsafe"));
        }

        // Apply mode change with delay
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Update cache
                playerModes.put(uuid, targetMode);

                // Save to database
                savePlayerMode(player);

                // Send confirmation message
                if (targetMode) {
                    player.sendMessage(plugin.getConfigManager().getMessage("mode-changed-safe"));
                    sendModeExplanation(player, true);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("mode-changed-unsafe"));
                    sendModeExplanation(player, false);
                }

                // Remove from pending changes
                pendingChanges.remove(uuid);
            }
        }.runTaskLater(plugin, plugin.getConfigManager().getModeChangeDelay() / 50); // Convert ms to ticks

        pendingChanges.put(uuid, task);
    }

    /**
     * Send detailed explanation of the new mode
     */
    private void sendModeExplanation(Player player, boolean safeMode) {
        List<String> messages;
        if (safeMode) {
            messages = plugin.getConfigManager().getDetailedMessages("safe-mode-activated");
        } else {
            messages = plugin.getConfigManager().getDetailedMessages("unsafe-mode-activated");
        }

        // Si les messages sont configurés, les utiliser, sinon utiliser les messages par défaut
        if (messages != null && !messages.isEmpty()) {
            for (String message : messages) {
                player.sendMessage(message);
            }
        } else {
            // Messages par défaut en cas de problème de config
            if (safeMode) {
                player.sendMessage("§a✅ Mode sécurisé activé !");
            } else {
                player.sendMessage("§c⚠️ Mode combat activé !");
            }
        }
    }

    /**
     * Cancel pending mode change
     */
    public void cancelPendingChange(UUID uuid) {
        BukkitTask task = pendingChanges.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * Check if player has pending mode change
     */
    public boolean hasPendingChange(UUID uuid) {
        return pendingChanges.containsKey(uuid);
    }

    /**
     * Remove player from cache (called on quit)
     */
    public void removePlayer(UUID uuid) {
        playerModes.remove(uuid);
        cancelPendingChange(uuid);
    }

    /**
     * Get player mode from cache or default
     */
    public Boolean getCachedMode(UUID uuid) {
        return playerModes.get(uuid);
    }

    /**
     * Force set player mode (admin command usage)
     */
    public void forceSetMode(Player player, boolean safeMode) {
        UUID uuid = player.getUniqueId();

        // Cancel any pending changes
        cancelPendingChange(uuid);

        // Update cache and database
        playerModes.put(uuid, safeMode);
        savePlayerMode(player);

        // Notify player
        if (safeMode) {
            player.sendMessage(plugin.getConfigManager().getMessage("mode-changed-safe"));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("mode-changed-unsafe"));
        }
    }
}
