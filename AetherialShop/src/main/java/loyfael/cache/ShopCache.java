package loyfael.cache;

import loyfael.model.ShopItem;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

public class ShopCache {

    private final ConcurrentHashMap<String, CachedShopData> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CachedPlayerData> playerCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final Logger logger;

    public ShopCache(Logger logger) {
        this.logger = logger;
        startCleanupTasks();
    }

    public static class CachedShopData {
        public final List<ShopItem> items;
        public final long timestamp;
        public final String dateKey;
        private volatile boolean dirty = false;

        public CachedShopData(List<ShopItem> items, String dateKey) {
            this.items = new ArrayList<>(items);
            this.timestamp = System.currentTimeMillis();
            this.dateKey = dateKey;
        }

        public boolean isExpired(long maxAge) {
            return System.currentTimeMillis() - timestamp > maxAge;
        }

        public void markDirty() { this.dirty = true; }
        public boolean isDirty() { return dirty; }
        public void markClean() { this.dirty = false; }
    }

    public static class CachedPlayerData {
        public volatile double lastKnownBalance;
        public volatile int inventorySlots;
        public final long lastUpdate;

        public CachedPlayerData(double balance, int slots) {
            this.lastKnownBalance = balance;
            this.inventorySlots = slots;
            this.lastUpdate = System.currentTimeMillis();
        }

        public boolean isStale() {
            return System.currentTimeMillis() - lastUpdate > 5000; // 5 secondes
        }
    }

    public CachedShopData getCachedData(String dateKey) {
        cacheLock.readLock().lock();
        try {
            return cache.get(dateKey);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public void cacheData(String dateKey, List<ShopItem> items) {
        cacheLock.writeLock().lock();
        try {
            CachedShopData data = new CachedShopData(items, dateKey);
            cache.put(dateKey, data);
            logger.fine("Données du shop mises en cache pour " + dateKey + " - " + items.size() + " items");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public void updatePlayerCache(String playerName, double balance, int inventorySlots) {
        playerCache.put(playerName, new CachedPlayerData(balance, inventorySlots));
    }

    public CachedPlayerData getPlayerCache(String playerName) {
        CachedPlayerData data = playerCache.get(playerName);
        return (data != null && !data.isStale()) ? data : null;
    }

    public void invalidatePlayerCache(String playerName) {
        playerCache.remove(playerName);
    }

    public void markShopDataDirty(String dateKey) {
        CachedShopData data = cache.get(dateKey);
        if (data != null) {
            data.markDirty();
        }
    }

    private void startCleanupTasks() {
        // Nettoyage des données expirées toutes les heures
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long maxAge = TimeUnit.HOURS.toMillis(25);
                int removed = 0;

                cacheLock.writeLock().lock();
                try {
                    removed = cache.entrySet().removeIf(entry -> entry.getValue().isExpired(maxAge)) ? 1 : 0;
                } finally {
                    cacheLock.writeLock().unlock();
                }

                // Nettoyage cache joueurs
                int playerRemoved = playerCache.entrySet().removeIf(entry -> entry.getValue().isStale()) ? 1 : 0;

                if (removed > 0 || playerRemoved > 0) {
                    logger.info("Cache nettoyé - Shop: " + removed + ", Joueurs: " + playerRemoved);
                }
            } catch (Exception e) {
                logger.warning("Erreur lors du nettoyage du cache: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.HOURS);

        // Statistiques du cache toutes les 10 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                logger.fine("Statistiques cache - Shop: " + cache.size() + " entrées, Joueurs: " + playerCache.size() + " entrées");
            } catch (Exception e) {
                logger.warning("Erreur lors des statistiques du cache: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    public void shutdown() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            logger.info("Cache fermé proprement");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public int getShopCacheSize() { return cache.size(); }
    public int getPlayerCacheSize() { return playerCache.size(); }
}
