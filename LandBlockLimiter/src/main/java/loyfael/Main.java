package loyfael;

import me.angeschossen.lands.api.applicationframework.util.ULID;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private BlockCounter blockCounter;
    private BlockListener blockListener;
    private LandsIntegration landsIntegration;

    @Override
    public void onEnable() {
        // Check if Lands is present
        if (!Bukkit.getPluginManager().isPluginEnabled("Lands")) {
            getLogger().severe("Lands is not installed. Disabling LandsBlockLimiter.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize components
        messageManager = new MessageManager(this);
        configManager = new ConfigManager(this);
        blockCounter = new BlockCounter(this, configManager, messageManager);
        landsIntegration = LandsIntegration.of(this);
        blockListener = new BlockListener(this, configManager, blockCounter, landsIntegration, messageManager);

        // Register events
        getServer().getPluginManager().registerEvents(blockListener, this);

        // Start periodic anti-exploit scanner
        startAntiExploitScanner();

        getLogger().info(messageManager.getMessage("plugin_enabled", "version", getDescription().getVersion()));
        getLogger().info(messageManager.getMessage("block_limits_loaded", "count", configManager.getBlockLimits().size()));
    }

    @Override
    public void onDisable() {
        if (blockCounter != null) {
            blockCounter.shutdown(); // Proper shutdown of async workers
            blockCounter.clearAllCache();
        }
        getLogger().info(messageManager.getMessage("plugin_disabled"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("lbl")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(messageManager.getMessage("plugin_info_header", "version", getDescription().getVersion()));
            sender.sendMessage(messageManager.getMessage("plugin_info_author", "author", getDescription().getAuthors().get(0)));
            sender.sendMessage(messageManager.getMessage("plugin_info_reload"));
            sender.sendMessage(messageManager.getMessage("plugin_info_scan"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("landsblocklimiter.reload")) {
                sender.sendMessage(messageManager.getMessage("no_permission"));
                return true;
            }

            try {
                configManager.loadConfig();
                messageManager.reloadMessages();

                // FIXED: Clear all cache and force rescan on reload
                blockCounter.clearAllCache();

                // Update cached materials in listener for performance
                blockListener.updateLimitedMaterialsCache();

                sender.sendMessage(messageManager.getMessage("config_reloaded"));
                getLogger().info(messageManager.getMessage("debug_config_reloaded", "player", sender.getName()));

                // FIXED: Force scan of all lands after reload
                getLogger().info("Forcing rescan of all lands after reload...");

            } catch (Exception e) {
                sender.sendMessage(messageManager.getMessage("reload_error", "error", e.getMessage()));
                getLogger().severe("Error during reload: " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("scan")) {
            if (!sender.hasPermission("landsblocklimiter.admin")) {
                sender.sendMessage(messageManager.getMessage("no_permission"));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("&cUsage: /lbl scan <landId>");
                return true;
            }

            try {
                String landIdentifier = args[1];
                Land land = findLand(landIdentifier);

                if (land == null) {
                    sender.sendMessage(messageManager.getMessage("land_not_found", "landId", landIdentifier));
                    return true;
                }

                sender.sendMessage(messageManager.getMessage("scan_started", "landName", land.getName()));

                // Create final references for lambda
                final Land finalLand = land;
                final CommandSender finalSender = sender;

                // Force rescan in async to avoid blocking
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    try {
                        blockCounter.forceRescanLand(finalLand);

                        // Calculate total blocks found
                        int totalBlocks = 0;
                        for (Material material : configManager.getBlockLimits().keySet()) {
                            totalBlocks += blockCounter.getTotalBlockCount(finalLand, material);
                        }

                        // Send completion message on main thread
                        final int finalTotalBlocks = totalBlocks;
                        Bukkit.getScheduler().runTask(this, () -> {
                            finalSender.sendMessage(messageManager.getMessage("scan_completed",
                                "landName", finalLand.getName(),
                                "totalBlocks", finalTotalBlocks));
                        });

                    } catch (Exception e) {
                        Bukkit.getScheduler().runTask(this, () -> {
                            finalSender.sendMessage(messageManager.getMessage("scan_error", "error", e.getMessage()));
                        });
                        getLogger().severe("Error during manual scan: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

            } catch (IllegalArgumentException e) {
                sender.sendMessage("&cInvalid land identifier. Use a Lands ULID or exact land name.");
                return true;
            }
            return true;
        }

        sender.sendMessage(messageManager.getMessage("unknown_command"));
        return true;
    }

    /**
     * Starts a periodic scanner to detect and prevent land deletion/recreation exploits
     */
    private void startAntiExploitScanner() {
        // Check every 5 minutes (6000 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                if (configManager.isDebug()) {
                    getLogger().info("Starting periodic anti-exploit scan...");
                }

                // Scanner tous les lands existants pour détecter les exploits
                for (Land land : landsIntegration.getLands()) {
                    try {
                        // Force un rescan si le land n'a pas été scanné récemment
                        boolean needsRescan = blockCounter.needsAntiExploitScan(land);
                        if (needsRescan) {
                            if (configManager.isDebug()) {
                                getLogger().info("Anti-exploit scan needed for land " + land.getName() + " (ID: " + land.getULID() + ")");
                            }
                            blockCounter.forceRescanLand(land);
                        }
                    } catch (Exception e) {
                        if (configManager.isDebug()) {
                            getLogger().warning("Error during anti-exploit scan for land " + land.getName() + ": " + e.getMessage());
                        }
                    }
                }

                if (configManager.isDebug()) {
                    getLogger().info("Periodic anti-exploit scan completed");
                }

            } catch (Exception e) {
                getLogger().warning("Error during periodic anti-exploit scan: " + e.getMessage());
            }
        }, 6000L, 6000L); // 5 minutes d'intervalle
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public BlockCounter getBlockCounter() {
        return blockCounter;
    }

    public LandsIntegration getLandsIntegration() {
        return landsIntegration;
    }

    private Land findLand(String identifier) {
        try {
            return landsIntegration.getLandByULID(ULID.fromString(identifier));
        } catch (IllegalArgumentException ignored) {
            return landsIntegration.getLandByName(identifier);
        }
    }
}
