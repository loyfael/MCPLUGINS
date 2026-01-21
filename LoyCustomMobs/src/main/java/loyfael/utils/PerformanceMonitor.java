package loyfael.utils;

import loyfael.LoyCustomMobs;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced performance monitoring system for LoyCustomMobs
 * Tracks memory usage, TPS, and performance metrics
 */
public class PerformanceMonitor {
    private final LoyCustomMobs plugin;
    private final MemoryMXBean memoryBean;

    // Performance metrics
    private final AtomicLong totalMobsSpawned = new AtomicLong(0);
    private final AtomicLong totalAbilitiesExecuted = new AtomicLong(0);
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);

    // TPS tracking
    private double currentTPS = 20.0;
    private long lastTick = System.currentTimeMillis();

    // Monitoring task
    private BukkitRunnable monitorTask;

    public PerformanceMonitor(LoyCustomMobs plugin) {
        this.plugin = plugin;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
    }

    /**
     * Start performance monitoring
     */
    public void startMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
        }

        monitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateMetrics();
                checkPerformanceWarnings();
            }
        };

        // Run every 5 seconds
        monitorTask.runTaskTimerAsynchronously(plugin, 100L, 100L);

        plugin.getLogger().info("Performance monitoring started");
    }

    /**
     * Stop performance monitoring
     */
    public void stopMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
    }

    /**
     * Update performance metrics
     */
    private void updateMetrics() {
        // Update TPS
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastTick;
        if (timeDiff > 0) {
            currentTPS = Math.min(20.0, 1000.0 / timeDiff * 20.0);
        }
        lastTick = currentTime;
    }

    /**
     * Check for performance warnings
     */
    private void checkPerformanceWarnings() {
        // Check TPS
        if (currentTPS < 18.0) {
            plugin.getLogger().warning("Low TPS detected: " + String.format("%.2f", currentTPS));
        }

        // Check memory usage
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double memoryUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;

        if (memoryUsagePercent > 85.0) {
            plugin.getLogger().warning("High memory usage: " + String.format("%.1f%%", memoryUsagePercent));
        }

        // Check active mob count
        int activeMobs = plugin.getMobManager().getActiveMobs().size();
        if (activeMobs > 200) {
            plugin.getLogger().warning("High number of active custom mobs: " + activeMobs);
        }
    }

    /**
     * Record mob spawn
     */
    public void recordMobSpawn() {
        totalMobsSpawned.incrementAndGet();
    }

    /**
     * Record ability execution
     */
    public void recordAbilityExecution() {
        totalAbilitiesExecuted.incrementAndGet();
    }

    /**
     * Record cache hit
     */
    public void recordCacheHit() {
        totalCacheHits.incrementAndGet();
    }

    /**
     * Record cache miss
     */
    public void recordCacheMiss() {
        totalCacheMisses.incrementAndGet();
    }

    /**
     * Get performance report
     */
    public PerformanceReport getReport() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        return new PerformanceReport(
            currentTPS,
            heapUsage.getUsed(),
            heapUsage.getMax(),
            totalMobsSpawned.get(),
            totalAbilitiesExecuted.get(),
            totalCacheHits.get(),
            totalCacheMisses.get(),
            plugin.getMobManager().getActiveMobs().size()
        );
    }

    /**
     * Performance report data class
     */
    public static class PerformanceReport {
        public final double tps;
        public final long memoryUsed;
        public final long memoryMax;
        public final long totalMobsSpawned;
        public final long totalAbilitiesExecuted;
        public final long cacheHits;
        public final long cacheMisses;
        public final int activeMobs;

        public PerformanceReport(double tps, long memoryUsed, long memoryMax,
                               long totalMobsSpawned, long totalAbilitiesExecuted,
                               long cacheHits, long cacheMisses, int activeMobs) {
            this.tps = tps;
            this.memoryUsed = memoryUsed;
            this.memoryMax = memoryMax;
            this.totalMobsSpawned = totalMobsSpawned;
            this.totalAbilitiesExecuted = totalAbilitiesExecuted;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.activeMobs = activeMobs;
        }

        public double getMemoryUsagePercent() {
            return (double) memoryUsed / memoryMax * 100;
        }

        public double getCacheHitRate() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format(
                "Performance Report:\n" +
                "  TPS: %.2f\n" +
                "  Memory: %.1f%% (%.1f MB / %.1f MB)\n" +
                "  Active Mobs: %d\n" +
                "  Total Spawned: %d\n" +
                "  Abilities Executed: %d\n" +
                "  Cache Hit Rate: %.1f%%",
                tps,
                getMemoryUsagePercent(),
                memoryUsed / 1024.0 / 1024.0,
                memoryMax / 1024.0 / 1024.0,
                activeMobs,
                totalMobsSpawned,
                totalAbilitiesExecuted,
                getCacheHitRate()
            );
        }
    }
}
