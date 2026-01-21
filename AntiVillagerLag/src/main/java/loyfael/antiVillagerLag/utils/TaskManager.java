package loyfael.antiVillagerLag.utils;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import loyfael.antiVillagerLag.AntiVillagerLag;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TaskManager optimisé pour 2000+ villagers
 * - Thread pool dédié pour éviter de bloquer le main thread
 * - Nettoyage automatique du cache
 * - Gestion de la mémoire optimisée
 */
public class TaskManager {

    private static ExecutorService asyncExecutor;
    private static BukkitTask cacheCleanupTask;
    private static BukkitTask memoryOptimizationTask;

    // Thread factory personnalisé pour nommer les threads
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "AVL-Async-" + counter.getAndIncrement());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1); // Légèrement moins prioritaire
            return thread;
        }
    };

    public static void initialize(AntiVillagerLag plugin) {
        // thread pool optimised for async tasks
        int processors = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(2, Math.min(processors / 2, 4)); // 2-4 threads max

        asyncExecutor = Executors.newFixedThreadPool(poolSize, THREAD_FACTORY);

        // Clean cache all 30 seconds
        cacheCleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                CompletableFuture.runAsync(() -> {
                    VillagerCache.cleanupOldEntries();

                    // Garbage collection suggéré si cache > 1000 entrées
                    if (VillagerCache.getCacheSize() > 1000) {
                        System.gc();
                    }
                }, asyncExecutor);
            }
        }.runTaskTimerAsynchronously(plugin, 600L, 600L); // 30s en ticks

        // optimisation all 5 minutes
        memoryOptimizationTask = new BukkitRunnable() {
            @Override
            public void run() {
                CompletableFuture.runAsync(() -> {
                    // statistics & memory usage
                    long totalMemory = Runtime.getRuntime().totalMemory();
                    long freeMemory = Runtime.getRuntime().freeMemory();
                    long usedMemory = totalMemory - freeMemory;

                    // Force cleanup if use > 80%
                    if ((float) usedMemory / totalMemory > 0.8f) {
                        VillagerCache.clearCache();
                        System.gc();
                        plugin.getLogger().warning("AVL: Emergency memory cleanup performed");
                    }
                }, asyncExecutor);
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L); // 5min
    }

    public static void shutdown() {
        if (cacheCleanupTask != null) {
            cacheCleanupTask.cancel();
        }

        if (memoryOptimizationTask != null) {
            memoryOptimizationTask.cancel();
        }

        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
        }
    }

    // Exécution asynchrone avec retour sur le main thread si nécessaire
    public static CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, asyncExecutor);
    }

    public static <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    // Synchronisation avec le main thread de Bukkit
    public static void runSync(AntiVillagerLag plugin, Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
