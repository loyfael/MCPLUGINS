package loyfael;

import me.angeschossen.lands.api.land.Land;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashMap;

public class BlockCounter {
    private final Main plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;

    // Cache: Land ULID -> PlayerUUID -> Material -> Count
    private final Map<String, Map<UUID, Map<Material, Integer>>> blockCounts;

    // Performance optimizations
    private final Set<String> scanningLands = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastScanTime = new ConcurrentHashMap<>();
    private final Queue<ScanTask> scanQueue = new LinkedList<>();
    private BukkitTask scanWorker;

    // Cache expiry time (configurable)
    private final long CACHE_EXPIRY_MS;
    // Max chunks to scan per tick (configurable)
    private final int MAX_CHUNKS_PER_TICK;

    public BlockCounter(Main plugin, ConfigManager configManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.blockCounts = new ConcurrentHashMap<>();

        // Load performance settings from config
        this.CACHE_EXPIRY_MS = configManager.getCacheExpiryMs();
        this.MAX_CHUNKS_PER_TICK = configManager.getMaxChunksPerTick();

        // Start async scan worker only if enabled
        if (configManager.isAsyncScanningEnabled()) {
            startScanWorker();
        }
    }

    private void startScanWorker() {
        long interval = configManager.getScanIntervalTicks();
        scanWorker = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            processScanQueue();
        }, 20L, interval);
    }

    public void shutdown() {
        if (scanWorker != null) {
            scanWorker.cancel();
        }
        scanQueue.clear();
        scanningLands.clear();
    }

    /**
     * Counts blocks for a player in a specific land
     */
    public int getBlockCount(Land land, UUID playerUUID, Material material) {
        if (land == null) return 0;

        // Check if we need to scan this land
        ensureLandScanned(land);

        Map<UUID, Map<Material, Integer>> landCounts = blockCounts.get(getLandKey(land));
        if (landCounts == null) return 0;

        Map<Material, Integer> playerCounts = landCounts.get(playerUUID);
        if (playerCounts == null) return 0;

        return playerCounts.getOrDefault(material, 0);
    }

    /**
     * Gets the total count with performance optimizations
     */
    public int getTotalBlockCount(Land land, Material material, Location excludeLocation) {
        if (land == null) return 0;

        // For placement checks, use fast cache-based counting
        if (excludeLocation != null) {
            return getTotalBlockCountFast(land, material);
        }

        return getTotalBlockCount(land, material);
    }

    /**
     * Fast total count using cache only (for placement checks)
     * FIXED: Better synchronization between incremental counter and real-world scan
     */
    public int getTotalBlockCountFast(Land land, Material material) {
        if (land == null) return 0;

        String landId = getLandKey(land);

        // Check if we have valid cache
        Map<UUID, Map<Material, Integer>> landCounts = blockCounts.get(landId);
        Long lastScan = lastScanTime.get(landId);

        // If we have recent cache (less than 30 seconds), use incremental counting
        if (landCounts != null && lastScan != null &&
            (System.currentTimeMillis() - lastScan) < 30000) {

            int total = 0;
            for (Map<Material, Integer> playerCounts : landCounts.values()) {
                total += playerCounts.getOrDefault(material, 0);
            }

            if (configManager.isDebug()) {
                plugin.getLogger().info("getTotalBlockCountFast for land " + landId + " material " + material + ": " + total + " (from cache)");
            }

            return total;
        }

        // If no cache or cache is old, force immediate synchronous scan
        if (configManager.isDebug()) {
            plugin.getLogger().info("FORCING immediate scan for land " + landId + " - cache missing or stale");
        }

        // Perform immediate synchronous scan for placement verification
        landCounts = performImmediateScan(land);
        if (landCounts != null) {
            blockCounts.put(landId, landCounts);
            lastScanTime.put(landId, System.currentTimeMillis());

            int total = 0;
            for (Map<Material, Integer> playerCounts : landCounts.values()) {
                total += playerCounts.getOrDefault(material, 0);
            }

            if (configManager.isDebug()) {
                plugin.getLogger().info("getTotalBlockCountFast for land " + landId + " material " + material + ": " + total + " (from immediate scan)");
            }

            return total;
        }

        return 0;
    }

    /**
     * Performs immediate synchronous scan for critical placement checks
     * FIXED: Scan ALL chunks that are part of the land area, regardless of ownership status
     */
    private Map<UUID, Map<Material, Integer>> performImmediateScan(Land land) {
        if (land == null) return null;

        Map<UUID, Map<Material, Integer>> landCounts = new ConcurrentHashMap<>();
        UUID ownerUUID = land.getOwnerUID();

        if (ownerUUID != null) {
            landCounts.put(ownerUUID, new ConcurrentHashMap<>());
        }

        try {
            // Get limited materials set for faster lookup
            Set<Material> limitedMaterials = configManager.getBlockLimits().keySet();

            if (configManager.isDebug()) {
                plugin.getLogger().info("IMMEDIATE scan starting for land " + getLandKey(land) + " (" + land.getName() + ") - scanning ALL land chunks");
            }

            // FIXED: Scan ALL chunks that are within the land boundaries
            // This prevents any bypass attempts by land deletion/recreation
            for (World world : Bukkit.getWorlds()) {
                Chunk[] loadedChunks = world.getLoadedChunks();

                for (Chunk chunk : loadedChunks) {
                    try {
                        // Get the land that SHOULD own this chunk (current or previous)
                        Land chunkLand = plugin.getLandsIntegration().getLandByChunk(world, chunk.getX(), chunk.getZ());

                        // Scan if:
                        // 1. Chunk currently belongs to our land
                        // 2. OR chunk has no current owner (might be from deleted land)
                        // This ensures we always count all physical blocks in the area
                        if (chunkLand == null || sameLand(chunkLand, land)) {
                            scanChunkForAllPhysicalBlocks(chunk, landCounts, ownerUUID, limitedMaterials);
                        }
                    } catch (Exception e) {
                        // Continue scanning other chunks
                    }
                }
            }

            if (configManager.isDebug()) {
                int totalBlocks = landCounts.values().stream()
                    .mapToInt(playerMap -> playerMap.values().stream().mapToInt(Integer::intValue).sum())
                    .sum();
                plugin.getLogger().info("IMMEDIATE scan completed for land " + getLandKey(land) +
                    " - Found " + totalBlocks + " limited blocks in ALL land chunks");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error during immediate land scan: " + e.getMessage());
            return null;
        }

        return landCounts;
    }

    /**
     * Gets the total count of a material in the land (all players combined)
     */
    public int getTotalBlockCount(Land land, Material material) {
        if (land == null) return 0;

        ensureLandScanned(land);

        Map<UUID, Map<Material, Integer>> landCounts = blockCounts.get(getLandKey(land));
        if (landCounts == null) return 0;

        int total = 0;
        for (Map<Material, Integer> playerCounts : landCounts.values()) {
            total += playerCounts.getOrDefault(material, 0);
        }
        return total;
    }

    /**
     * Ensures a land is scanned (with caching and async processing)
     */
    private void ensureLandScanned(Land land) {
        if (land == null) return;

        String landId = getLandKey(land);

        // Check if already scanning
        if (scanningLands.contains(landId)) {
            return;
        }

        // Check cache validity
        Long lastScan = lastScanTime.get(landId);
        if (lastScan != null && (System.currentTimeMillis() - lastScan) < CACHE_EXPIRY_MS) {
            return; // Cache still valid
        }

        // Check if we have any cache
        if (blockCounts.containsKey(landId) && lastScan != null) {
            return; // Use existing cache for now
        }

        // Queue for async scanning
        queueLandScan(land);
    }

    /**
     * Queue a land for async scanning
     */
    private void queueLandScan(Land land) {
        if (scanningLands.add(getLandKey(land))) {
            scanQueue.offer(new ScanTask(land));
        }
    }

    /**
     * Process scan queue asynchronously
     */
    private void processScanQueue() {
        int processed = 0;

        while (!scanQueue.isEmpty() && processed < MAX_CHUNKS_PER_TICK) {
            ScanTask task = scanQueue.poll();
            if (task != null) {
                try {
                    scanLandOptimized(task.land);
                    processed++;
                } catch (Exception e) {
                    plugin.getLogger().warning("Error during async land scan: " + e.getMessage());
                } finally {
                    scanningLands.remove(getLandKey(task.land));
                }
            }
        }
    }

    /**
     * Optimized land scanning - only scan specific chunks
     */
    private void scanLandOptimized(Land land) {
        if (land == null) return;

        String landId = getLandKey(land);
        Map<UUID, Map<Material, Integer>> landCounts = new ConcurrentHashMap<>();

        UUID ownerUUID = land.getOwnerUID();
        if (ownerUUID != null) {
            landCounts.put(ownerUUID, new ConcurrentHashMap<>());
        }

        try {
            // Get land chunks more efficiently using Lands API
            scanLandChunksOptimized(land, landCounts, ownerUUID);

            // Update cache on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                blockCounts.put(landId, landCounts);
                lastScanTime.put(landId, System.currentTimeMillis());

                if (configManager.isDebug()) {
                    int totalBlocks = landCounts.values().stream()
                        .mapToInt(playerMap -> playerMap.values().stream().mapToInt(Integer::intValue).sum())
                        .sum();
                    plugin.getLogger().info("Optimized scan completed for land " + landId +
                        " (" + land.getName() + ") - Found " + totalBlocks + " limited blocks");
                }
            });

        } catch (Exception e) {
            plugin.getLogger().warning("Error during optimized land scan: " + e.getMessage());
        }
    }

    /**
     * Scan only land chunks (much more efficient)
     * Fixed to handle land recreation by scanning actual chunk positions
     */
    private void scanLandChunksOptimized(Land land, Map<UUID, Map<Material, Integer>> landCounts, UUID ownerUUID) {
        // Get limited materials set for faster lookup
        Set<Material> limitedMaterials = configManager.getBlockLimits().keySet();

        if (configManager.isDebug()) {
            plugin.getLogger().info("Scanning land " + getLandKey(land) + " (" + land.getName() + ") with " + limitedMaterials.size() + " limited materials");
        }

        // Scan all loaded chunks and check if they belong to this land
        for (World world : Bukkit.getWorlds()) {
            Chunk[] loadedChunks = world.getLoadedChunks();

            int scannedChunks = 0;
            int foundChunks = 0;

            for (Chunk chunk : loadedChunks) {
                try {
                    // Check if chunk belongs to current land by coordinates (not cached ID)
                    Land chunkLand = plugin.getLandsIntegration().getLandByChunk(world, chunk.getX(), chunk.getZ());
                    if (sameLand(chunkLand, land)) {
                        // FIXED: Use the anti-exploit scan method here too
                        scanChunkForAllPhysicalBlocks(chunk, landCounts, ownerUUID, limitedMaterials);
                        foundChunks++;
                    }
                    scannedChunks++;
                } catch (Exception e) {
                    if (configManager.isDebug()) {
                        plugin.getLogger().warning("Error scanning chunk " + chunk.getX() + "," + chunk.getZ() + ": " + e.getMessage());
                    }
                }
            }

            if (configManager.isDebug() && foundChunks > 0) {
                plugin.getLogger().info("World " + world.getName() + ": scanned " + scannedChunks + " chunks, found " + foundChunks + " belonging to land " + getLandKey(land));
            }
        }
    }

    /**
     * Enhanced chunk scanning with better block counting and debug info
     */
    private void scanChunkOptimized(Chunk chunk, Map<UUID, Map<Material, Integer>> landCounts,
                                  UUID defaultOwner, Set<Material> limitedMaterials) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        Map<Material, Integer> foundInChunk = new HashMap<>();

        // Optimized block iteration
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX + x;
                int worldZ = chunkZ + z;

                // Limit Y scanning to reasonable range (surface + underground)
                int minY = Math.max(world.getMinHeight(), -64);
                int maxY = Math.min(world.getMaxHeight(), 128); // Limiter à y=128 au lieu de 320

                for (int y = minY; y < maxY; y++) {
                    Block block = world.getBlockAt(worldX, y, worldZ);
                    Material material = block.getType();

                    // Fast set lookup instead of map lookup
                    if (limitedMaterials.contains(material)) {
                        // Vérifier que c'est vraiment un bloc placé et pas de l'air ou autre
                        if (material != Material.AIR && !block.isEmpty()) {
                            UUID playerUUID = (defaultOwner != null) ? defaultOwner : new UUID(0, 0);

                            landCounts.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                                     .merge(material, 1, Integer::sum);

                            foundInChunk.merge(material, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        // Debug logging for chunk scan results
        if (configManager.isDebug() && !foundInChunk.isEmpty()) {
            StringBuilder sb = new StringBuilder("Chunk " + chunk.getX() + "," + chunk.getZ() + " found: ");
            foundInChunk.forEach((material, count) -> sb.append(material).append("=").append(count).append(" "));
            plugin.getLogger().info(sb.toString());
        }
    }

    /**
     * Scan chunk for ALL physical blocks present (prevents land deletion/recreation exploit)
     * This method counts all limited blocks physically present in the chunk,
     * regardless of when they were placed or when the land was created
     */
    private void scanChunkForAllPhysicalBlocks(Chunk chunk, Map<UUID, Map<Material, Integer>> landCounts,
                                             UUID defaultOwner, Set<Material> limitedMaterials) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        Map<Material, Integer> foundInChunk = new HashMap<>();

        // Scan every block in the chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX + x;
                int worldZ = chunkZ + z;

                // Limit Y scanning to reasonable range (surface + underground)
                int minY = Math.max(world.getMinHeight(), -64);
                int maxY = Math.min(world.getMaxHeight(), 128);

                for (int y = minY; y < maxY; y++) {
                    Block block = world.getBlockAt(worldX, y, worldZ);
                    Material material = block.getType();

                    // Count ALL limited materials that are physically present
                    if (limitedMaterials.contains(material)) {
                        if (material != Material.AIR && !block.isEmpty()) {
                            // Attribute all blocks to the current land owner to prevent exploit
                            UUID playerUUID = (defaultOwner != null) ? defaultOwner : new UUID(0, 0);

                            landCounts.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                                     .merge(material, 1, Integer::sum);

                            foundInChunk.merge(material, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        // Debug logging for chunk scan results
        if (configManager.isDebug() && !foundInChunk.isEmpty()) {
            StringBuilder sb = new StringBuilder("Chunk " + chunk.getX() + "," + chunk.getZ() + " PHYSICAL scan found: ");
            foundInChunk.forEach((material, count) -> sb.append(material).append("=").append(count).append(" "));
            plugin.getLogger().info(sb.toString());
        }
    }

    /**
     * Adds a block to the counter
     * FIXED: Update cache immediately to prevent rapid placement exploits
     */
    public void addBlock(Land land, UUID playerUUID, Material material) {
        if (land == null) return;

        String landId = getLandKey(land);

        // Ensure cache exists for this land
        Map<UUID, Map<Material, Integer>> landCounts = blockCounts.computeIfAbsent(landId, k -> new ConcurrentHashMap<>());
        Map<Material, Integer> playerCounts = landCounts.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());

        // Update the cache immediately
        int currentCount = playerCounts.getOrDefault(material, 0);
        playerCounts.put(material, currentCount + 1);

        // Update last scan time to keep cache valid
        lastScanTime.put(landId, System.currentTimeMillis());

        if (configManager.isDebug()) {
            // Show the new total after increment
            int newTotal = getTotalBlockCountFast(land, material);
            plugin.getLogger().info("Bloc ajouté : " + material +
                " pour " + playerUUID + " dans la parcelle " + landId +
                ". Total : " + newTotal);
        }
    }

    /**
     * Removes a block from the counter
     * FIXED: Force cache refresh after block removal to ensure accuracy
     */
    public void removeBlock(Land land, UUID playerUUID, Material material) {
        if (land == null) return;

        Map<UUID, Map<Material, Integer>> landCounts = blockCounts.get(getLandKey(land));
        if (landCounts == null) return;

        Map<Material, Integer> playerCounts = landCounts.get(playerUUID);
        if (playerCounts == null) return;

        int currentCount = playerCounts.getOrDefault(material, 0);
        if (currentCount > 0) {
            if (currentCount == 1) {
                playerCounts.remove(material);
            } else {
                playerCounts.put(material, currentCount - 1);
            }

            // Update the last scan time to indicate fresh cache
            lastScanTime.put(getLandKey(land), System.currentTimeMillis());

            if (configManager.isDebug()) {
                int newTotal = 0;
                for (Map<Material, Integer> pCounts : landCounts.values()) {
                    newTotal += pCounts.getOrDefault(material, 0);
                }
                plugin.getLogger().info(messageManager.getMessage("debug_block_removed",
                    "material", material,
                    "player", playerUUID,
                    "landId", getLandKey(land),
                    "count", getBlockCount(land, playerUUID, material)) +
                    " | New total in land: " + newTotal);
            }
        }
    }

    /**
     * Checks if a player can place a block (optimized)
     */
    public boolean canPlaceBlock(Land land, UUID playerUUID, Material material) {
        if (!configManager.hasLimit(material)) return true;

        int totalCount = getTotalBlockCountFast(land, material);
        int limit = configManager.getBlockLimit(material);

        return totalCount < limit;
    }

    /**
     * Clears cache for a specific land
     */
    public void clearLandCache(String landId) {
        blockCounts.remove(landId);
        lastScanTime.remove(landId);
        scanningLands.remove(landId);
    }

    /**
     * Clears cache for a specific land (overload for Land object)
     */
    public void clearLandCache(Land land) {
        clearLandCache(getLandKey(land));
    }

    /**
     * Clears all cache
     */
    public void clearAllCache() {
        blockCounts.clear();
        lastScanTime.clear();
        scanningLands.clear();
        scanQueue.clear();
    }

    /**
     * Forces a rescan of all blocks in a land
     */
    public void forceRescanLand(Land land) {
        if (land != null) {
            clearLandCache(getLandKey(land));
            queueLandScan(land);
        }
    }

    /**
     * Vérifie si un land a besoin d'un scan anti-exploit
     * Retourne true si le land n'a jamais été scanné ou si le dernier scan date de plus de 10 minutes
     */
    public boolean needsAntiExploitScan(Land land) {
        if (land == null) return false;

        String landId = getLandKey(land);
        Long lastScan = lastScanTime.get(landId);

        // Si jamais scanné, scan nécessaire
        if (lastScan == null) {
            return true;
        }

        // Si le dernier scan date de plus de 10 minutes (600000 ms), scan nécessaire
        long timeSinceLastScan = System.currentTimeMillis() - lastScan;
        return timeSinceLastScan > 600000; // 10 minutes
    }

    /**
     * MÉTHODE ANTI-EXPLOIT CRITIQUE
     * Compte immédiatement les blocs physiques dans un land sans se fier au cache
     * Cette méthode est appelée à chaque placement pour détecter l'exploit de suppression/recréation
     */
    public int countPhysicalBlocksInLandImmediate(Land land, Material material) {
        if (land == null) return 0;

        int totalFound = 0;

        try {
            if (configManager.isDebug()) {
                plugin.getLogger().info("ANTI-EXPLOIT: Scanning physical blocks for material " + material + " in land " + land.getName());
            }

            // Scanner tous les chunks chargés pour trouver ceux qui appartiennent à ce land
            for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                org.bukkit.Chunk[] loadedChunks = world.getLoadedChunks();

                for (org.bukkit.Chunk chunk : loadedChunks) {
                    try {
                        // Vérifier si ce chunk appartient à notre land
                        Land chunkLand = plugin.getLandsIntegration().getLandByChunk(world, chunk.getX(), chunk.getZ());

                        if (sameLand(chunkLand, land)) {
                            // Scanner physiquement ce chunk pour le material spécifique
                            int blocksInChunk = countMaterialInChunkPhysical(chunk, material);
                            totalFound += blocksInChunk;

                            if (configManager.isDebug() && blocksInChunk > 0) {
                                plugin.getLogger().info("ANTI-EXPLOIT: Found " + blocksInChunk + " blocks of " + material + " in chunk " + chunk.getX() + "," + chunk.getZ());
                            }
                        }
                    } catch (Exception e) {
                        // Continue avec les autres chunks si l'un pose problème
                        if (configManager.isDebug()) {
                            plugin.getLogger().warning("Error scanning chunk " + chunk.getX() + "," + chunk.getZ() + ": " + e.getMessage());
                        }
                    }
                }
            }

            if (configManager.isDebug()) {
                plugin.getLogger().info("ANTI-EXPLOIT: Total physical blocks of " + material + " found in land " + land.getName() + ": " + totalFound);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("ANTI-EXPLOIT: Error during immediate physical scan: " + e.getMessage());
            return 0;
        }

        return totalFound;
    }

    /**
     * Count specific material in a chunk by scanning all blocks physically
     */
    private int countMaterialInChunkPhysical(org.bukkit.Chunk chunk, Material material) {
        int count = 0;
        org.bukkit.World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        // Scanner chaque bloc du chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX + x;
                int worldZ = chunkZ + z;

                // Limiter le scan Y pour les performances
                int minY = Math.max(world.getMinHeight(), -64);
                int maxY = Math.min(world.getMaxHeight(), 320);

                for (int y = minY; y < maxY; y++) {
                    try {
                        org.bukkit.block.Block block = world.getBlockAt(worldX, y, worldZ);
                        if (block.getType() == material && !block.isEmpty()) {
                            count++;
                        }
                    } catch (Exception e) {
                        // Continue si un bloc pose problème
                    }
                }
            }
        }

        return count;
    }

    /**
     * Quick check if a chunk contains any limited blocks (for orphan detection)
     */
    private boolean hasLimitedBlocksInChunk(Chunk chunk, Set<Material> limitedMaterials) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        // Quick scan - stop at first limited block found
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX + x;
                int worldZ = chunkZ + z;

                // Limit Y scanning to reasonable range
                int minY = Math.max(world.getMinHeight(), -64);
                int maxY = Math.min(world.getMaxHeight(), 128);

                for (int y = minY; y < maxY; y++) {
                    Block block = world.getBlockAt(worldX, y, worldZ);
                    Material material = block.getType();

                    if (limitedMaterials.contains(material) && material != Material.AIR && !block.isEmpty()) {
                        if (configManager.isDebug()) {
                            plugin.getLogger().info("Found orphaned limited block " + material + " at " + worldX + "," + y + "," + worldZ + " in chunk " + chunk.getX() + "," + chunk.getZ());
                        }
                        return true; // Found at least one limited block
                    }
                }
            }
        }
        
        return false; // No limited blocks found
    }

    private String getLandKey(Land land) {
        return land.getULID().toString();
    }

    private boolean sameLand(Land first, Land second) {
        return first != null && second != null && getLandKey(first).equals(getLandKey(second));
    }

    // Task class for scan queue
    private static class ScanTask {
        final Land land;

        ScanTask(Land land) {
            this.land = land;
        }
    }
}
