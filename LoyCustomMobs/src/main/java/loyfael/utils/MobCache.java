package loyfael.utils;

import loyfael.models.CustomMob;
import org.bukkit.entity.LivingEntity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * High-performance cache system for custom mobs
 * Reduces database/file lookups and improves performance
 */
public class MobCache {
    private static final int CACHE_SIZE = 1000;
    private static final long CACHE_EXPIRE_TIME = 300000; // 5 minutes

    private final ConcurrentHashMap<UUID, CacheEntry> mobCache;
    private final ScheduledExecutorService cleanupExecutor;

    public MobCache() {
        this.mobCache = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MobCache-Cleanup");
            t.setDaemon(true);
            return t;
        });

        // Schedule cache cleanup every minute
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries,
                                          60, 60, TimeUnit.SECONDS);
    }

    /**
     * Cache a custom mob
     */
    public void cacheMob(UUID mobId, CustomMob mob) {
        if (mobCache.size() >= CACHE_SIZE) {
            evictOldestEntry();
        }

        mobCache.put(mobId, new CacheEntry(mob, System.currentTimeMillis()));
    }

    /**
     * Get cached mob
     */
    public CustomMob getCachedMob(UUID mobId) {
        CacheEntry entry = mobCache.get(mobId);
        if (entry == null) return null;

        // Check if expired
        if (System.currentTimeMillis() - entry.timestamp > CACHE_EXPIRE_TIME) {
            mobCache.remove(mobId);
            return null;
        }

        // Update access time
        entry.timestamp = System.currentTimeMillis();
        return entry.mob;
    }

    /**
     * Remove mob from cache
     */
    public void removeMob(UUID mobId) {
        mobCache.remove(mobId);
    }

    /**
     * Check if mob is cached
     */
    public boolean isCached(UUID mobId) {
        CacheEntry entry = mobCache.get(mobId);
        return entry != null &&
               (System.currentTimeMillis() - entry.timestamp <= CACHE_EXPIRE_TIME);
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        return new CacheStats(mobCache.size(), CACHE_SIZE);
    }

    /**
     * Cleanup expired entries
     */
    private void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        mobCache.entrySet().removeIf(entry ->
            currentTime - entry.getValue().timestamp > CACHE_EXPIRE_TIME);
    }

    /**
     * Evict oldest entry when cache is full
     */
    private void evictOldestEntry() {
        UUID oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (var entry : mobCache.entrySet()) {
            if (entry.getValue().timestamp < oldestTime) {
                oldestTime = entry.getValue().timestamp;
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            mobCache.remove(oldestKey);
        }
    }

    /**
     * Shutdown cache
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        mobCache.clear();
    }

    /**
     * Cache entry wrapper
     */
    private static class CacheEntry {
        final CustomMob mob;
        volatile long timestamp;

        CacheEntry(CustomMob mob, long timestamp) {
            this.mob = mob;
            this.timestamp = timestamp;
        }
    }

    /**
     * Cache statistics
     */
    public static class CacheStats {
        public final int currentSize;
        public final int maxSize;
        public final double fillPercentage;

        CacheStats(int currentSize, int maxSize) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.fillPercentage = (double) currentSize / maxSize * 100;
        }
    }
}
