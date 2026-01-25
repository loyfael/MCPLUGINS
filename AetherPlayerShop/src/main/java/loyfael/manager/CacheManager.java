package loyfael.manager;

import loyfael.model.Shop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Gestionnaire de cache haute performance pour les shops les plus consultés
 * Garantit des temps de réponse < 50ms selon le cahier des charges
 */
public class CacheManager {

    private final Map<String, CachedShop> shopCache;
    private final Map<String, Integer> accessCount;
    private final Queue<String> recentAccesses;
    private final ScheduledExecutorService scheduler;
    private final int maxCacheSize;
    private final long expireTimeMinutes;

    public CacheManager() {
        this.shopCache = new ConcurrentHashMap<>();
        this.accessCount = new ConcurrentHashMap<>();
        this.recentAccesses = new ConcurrentLinkedQueue<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.maxCacheSize = 1000; // Configurable via config
        this.expireTimeMinutes = 30; // Configurable via config

        startCacheCleanup();
    }

    /**
     * Ajoute un shop au cache
     */
    public void cacheShop(@NotNull Shop shop) {
        if (shopCache.size() >= maxCacheSize) {
            evictLeastUsed();
        }

        shopCache.put(shop.getId(), new CachedShop(shop, Instant.now()));
        recordAccess(shop.getId());
    }

    /**
     * Récupère un shop depuis le cache
     */
    @Nullable
    public Shop getCachedShop(@NotNull String shopId) {
        CachedShop cached = shopCache.get(shopId);
        if (cached == null) {
            return null;
        }

        // Vérification de l'expiration
        if (isExpired(cached)) {
            shopCache.remove(shopId);
            return null;
        }

        recordAccess(shopId);
        return cached.shop;
    }

    /**
     * Met à jour un shop dans le cache
     */
    public void updateCachedShop(@NotNull Shop shop) {
        shopCache.put(shop.getId(), new CachedShop(shop, Instant.now()));
    }

    /**
     * Supprime un shop du cache
     */
    public void removeCachedShop(@NotNull String shopId) {
        shopCache.remove(shopId);
        accessCount.remove(shopId);
    }

    /**
     * Invalide tout le cache - utilisé lors des modifications importantes
     */
    public void invalidateCache() {
        shopCache.clear();
        accessCount.clear();
        recentAccesses.clear();
    }

    /**
     * Alias pour invalidateCache() - pour compatibilité
     */
    public void clearCache() {
        invalidateCache();
    }

    /**
     * Obtient les shops les plus populaires du cache
     */
    @NotNull
    public List<Shop> getMostPopularShops(int limit) {
        return accessCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> getCachedShop(entry.getKey()))
                .filter(shop -> shop != null)
                .collect(Collectors.toList());
    }

    /**
     * Obtient les statistiques du cache
     */
    public CacheStats getCacheStats() {
        long expiredCount = shopCache.values().stream()
                .mapToLong(cached -> isExpired(cached) ? 1 : 0)
                .sum();

        return new CacheStats(
            shopCache.size(),
            (int) expiredCount,
            accessCount.values().stream().mapToInt(Integer::intValue).sum()
        );
    }

    /**
     * Nettoyage automatique du cache
     */
    private void startCacheCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            // Suppression des entrées expirées
            shopCache.entrySet().removeIf(entry -> isExpired(entry.getValue()));

            // Si le cache est encore trop plein, éviction des moins utilisés
            while (shopCache.size() > maxCacheSize * 0.8) {
                evictLeastUsed();
            }

            // Nettoyage des accès anciens
            cleanupOldAccesses();

        }, 5, 5, TimeUnit.MINUTES);
    }

    private void recordAccess(@NotNull String shopId) {
        accessCount.merge(shopId, 1, Integer::sum);
        recentAccesses.offer(shopId + ":" + System.currentTimeMillis());

        // Limite de la queue des accès récents
        while (recentAccesses.size() > 10000) {
            recentAccesses.poll();
        }
    }

    private boolean isExpired(@NotNull CachedShop cached) {
        return cached.cachedAt.plus(expireTimeMinutes, ChronoUnit.MINUTES)
                .isBefore(Instant.now());
    }

    private void evictLeastUsed() {
        if (accessCount.isEmpty()) return;

        String leastUsedId = accessCount.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (leastUsedId != null) {
            shopCache.remove(leastUsedId);
            accessCount.remove(leastUsedId);
        }
    }

    private void cleanupOldAccesses() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        recentAccesses.removeIf(access -> {
            String[] parts = access.split(":");
            return parts.length == 2 && Long.parseLong(parts[1]) < cutoffTime;
        });
    }


    /**
     * Classe interne pour stocker les shops en cache avec metadata
     */
    private static class CachedShop {
        final Shop shop;
        final Instant cachedAt;

        CachedShop(@NotNull Shop shop, @NotNull Instant cachedAt) {
            this.shop = shop;
            this.cachedAt = cachedAt;
        }
    }

    /**
     * Statistiques du cache
     */
    public static class CacheStats {
        public final int totalCached;
        public final int expiredEntries;
        public final int totalAccesses;

        CacheStats(int totalCached, int expiredEntries, int totalAccesses) {
            this.totalCached = totalCached;
            this.expiredEntries = expiredEntries;
            this.totalAccesses = totalAccesses;
        }
    }
}
