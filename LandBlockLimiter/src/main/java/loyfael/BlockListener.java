package loyfael;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlockListener implements Listener {
    private final Main plugin;
    private final ConfigManager configManager;
    private final BlockCounter blockCounter;
    private final LandsIntegration landsIntegration;
    private final MessageManager messageManager;

    // Performance optimization: cache limited materials
    private Set<Material> limitedMaterials;
    private long lastConfigReload = 0;

    public BlockListener(Main plugin, ConfigManager configManager, BlockCounter blockCounter, LandsIntegration landsIntegration, MessageManager messageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.blockCounter = blockCounter;
        this.landsIntegration = landsIntegration;
        this.messageManager = messageManager;

        // Initialize limited materials cache
        updateLimitedMaterialsCache();
    }

    /**
     * Update the limited materials cache for performance
     */
    public void updateLimitedMaterialsCache() {
        this.limitedMaterials = ConcurrentHashMap.newKeySet();
        this.limitedMaterials.addAll(configManager.getBlockLimits().keySet());
        this.lastConfigReload = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Material material = event.getBlock().getType();

        // Fast exit: check if material has limit using cached set
        if (!limitedMaterials.contains(material)) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Get the land at this location with minimal API calls
        Land land = getLandFromCache(location);
        if (land == null) {
            // Not in a land, no restriction
            return;
        }

        // Check build permissions if enabled (fast exit if no permission)
        if (configManager.shouldCheckBuildPermissions()) {
            if (!land.isTrusted(player.getUniqueId())) {
                // Player doesn't have build permission, let Lands handle it
                return;
            }
        }

        int realPhysicalCount = blockCounter.countPhysicalBlocksInLandImmediate(land, material);
        int limit = configManager.getBlockLimit(material);

        if (configManager.isDebug()) {
            plugin.getLogger().info("=== ANTI-EXPLOIT PLACEMENT CHECK ===");
            plugin.getLogger().info("Player: " + player.getName());
            plugin.getLogger().info("Material: " + material);
            plugin.getLogger().info("Land: " + land.getName() + " (ID: " + land.getULID() + ")");
            plugin.getLogger().info("REAL physical count found: " + realPhysicalCount);
            plugin.getLogger().info("Limit configured: " + limit);
            plugin.getLogger().info("Will block placement: " + (realPhysicalCount >= limit));
        }

        if (realPhysicalCount >= limit) {
            event.setCancelled(true);
            String blockName = configManager.hasTranslation(material)
                ? configManager.getBlockTranslation(material)
                : material.name().toLowerCase();

            String message = messageManager.getMessage("limit_reached_physical",
                "block", blockName,
                "limit", limit);
            player.sendMessage(message);

            if (configManager.isDebug()) {
                plugin.getLogger().info("RESULT: BLOCKED - Limit reached! Physical blocks found: " + realPhysicalCount + "/" + limit);
            }

            blockCounter.forceRescanLand(land);
            return;
        }

        if (configManager.isDebug()) {
            plugin.getLogger().info("RESULT: ALLOWED - No exploit detected (" + realPhysicalCount + "<" + limit + ")");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaceMonitor(BlockPlaceEvent event) {
        Material material = event.getBlock().getType();

        // Fast exit: check if material has limit using cached set
        if (!limitedMaterials.contains(material)) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Get the land at this location
        Land land = getLandFromCache(location);
        if (land == null) {
            return;
        }

        blockCounter.addBlock(land, player.getUniqueId(), material);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();

        // Fast exit: check if material has limit
        if (!limitedMaterials.contains(material)) {
            return;
        }

        Location location = event.getBlock().getLocation();
        Player player = event.getPlayer();

        // Get the land at this location
        Land land = getLandFromCache(location);
        if (land == null) {
            // Not in a land
            return;
        }

        // Remove the block from the counter (async to avoid blocking)
        blockCounter.removeBlock(land, player.getUniqueId(), material);
    }

    /**
     * Event fired when a player joins the server
     * Automatically checks all their lands to prevent exploits
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (configManager.isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + " joined - checking all lands for block limits");
        }

        // Delay of 3 seconds to allow the player to fully load
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                // Scan all existing lands for this player
                for (Land land : landsIntegration.getLands()) {
                    try {
                        // Check if the player is the owner or a member of this land
                        if (land.getOwnerUID().equals(player.getUniqueId()) || land.isTrusted(player.getUniqueId())) {
                            if (configManager.isDebug()) {
                                plugin.getLogger().info("Forcing rescan of land " + land.getName() + " (ID: " + land.getULID() + ") for player " + player.getName());
                            }

                            // Force a full rescan of the land
                            blockCounter.forceRescanLand(land);
                        }
                    } catch (Exception e) {
                        if (configManager.isDebug()) {
                            plugin.getLogger().warning("Error checking land " + land.getName() + " for player " + player.getName() + ": " + e.getMessage());
                        }
                    }
                }

                if (configManager.isDebug()) {
                    plugin.getLogger().info("Completed block limit verification for player " + player.getName());
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Error checking block limits for player " + player.getName() + ": " + e.getMessage());
                if (configManager.isDebug()) {
                    e.printStackTrace();
                }
            }
        }, 60L); // 3 secondes de délai
    }

    /**
     * Fast land lookup with caching
     */
    private Land getLandFromCache(Location location) {
        try {
            return landsIntegration.getLandByChunk(
                location.getWorld(),
                location.getChunk().getX(),
                location.getChunk().getZ()
            );
        } catch (Exception e) {
            // If Lands API fails, return null to avoid blocking placement
            return null;
        }
    }
}
