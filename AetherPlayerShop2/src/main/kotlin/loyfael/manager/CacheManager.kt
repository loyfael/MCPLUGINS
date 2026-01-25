package loyfael.manager

import loyfael.model.Shop
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.*

/**
 * Gestionnaire de cache haute performance pour les shops
 */
class CacheManager {

    private val shopCache: ConcurrentHashMap<String, CachedShop> = ConcurrentHashMap()
    private val accessCount: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
    private val recentAccesses: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val maxCacheSize: Int = 1000 // Configurable via config
    private val expireTimeMinutes: Long = 30 // Configurable via config

    init {
        startCacheCleanup()
    }

    /**
     * Ajoute un shop au cache
     */
    fun cacheShop(shop: Shop) {
        if (shopCache.size >= maxCacheSize) {
            evictLeastUsed()
        }

        shopCache[shop.id] = CachedShop(shop, Instant.now())
        recordAccess(shop.id)
    }

    /**
     * Récupère un shop depuis le cache
     */
    fun getCachedShop(shopId: String): Shop? {
        val cached = shopCache[shopId] ?: return null

        // Vérification de l'expiration
        if (isExpired(cached)) {
            shopCache.remove(shopId)
            return null
        }

        recordAccess(shopId)
        return cached.shop
    }

    /**
     * Met à jour un shop dans le cache
     */
    fun updateCachedShop(shop: Shop) {
        shopCache[shop.id] = CachedShop(shop, Instant.now())
    }

    /**
     * Supprime un shop du cache
     */
    fun removeCachedShop(shopId: String) {
        shopCache.remove(shopId)
        accessCount.remove(shopId)
    }

    /**
     * Invalide tout le cache
     */
    fun invalidateCache() {
        shopCache.clear()
        accessCount.clear()
        recentAccesses.clear()
    }

    /**
     * Alias pour invalidateCache()
     */
    fun clearCache() {
        invalidateCache()
    }

    /**
     * Obtient les shops les plus populaires du cache
     */
    fun getMostPopularShops(limit: Int): List<Shop> {
        return accessCount.entries
            .sortedByDescending { it.value }
            .take(limit)
            .mapNotNull { getCachedShop(it.key) }
    }

    /**
     * Obtient les statistiques du cache
     */
    fun getCacheStats(): CacheStats {
        val expiredCount = shopCache.values.count { isExpired(it) }
        val totalAccesses = accessCount.values.sum()

        return CacheStats(
            totalCached = shopCache.size,
            expiredEntries = expiredCount,
            totalAccesses = totalAccesses
        )
    }

    /**
     * Nettoyage automatique du cache
     */
    private fun startCacheCleanup() {
        scheduler.scheduleAtFixedRate({
            // Suppression des entrées expirées
            shopCache.entries.removeIf { isExpired(it.value) }

            // Si le cache est encore trop plein, éviction des moins utilisés
            while (shopCache.size > maxCacheSize * 0.8) {
                evictLeastUsed()
            }

            // Nettoyage des accès anciens
            cleanupOldAccesses()
        }, 5, 5, TimeUnit.MINUTES)
    }

    private fun recordAccess(shopId: String) {
        accessCount.merge(shopId, 1, Int::plus)
        recentAccesses.offer("$shopId:${System.currentTimeMillis()}")

        // Limite de la queue des accès récents
        while (recentAccesses.size > 10000) {
            recentAccesses.poll()
        }
    }

    private fun isExpired(cached: CachedShop): Boolean {
        return cached.cachedAt.plus(expireTimeMinutes, ChronoUnit.MINUTES)
            .isBefore(Instant.now())
    }

    private fun evictLeastUsed() {
        if (accessCount.isEmpty()) return

        val leastUsedId = accessCount.minByOrNull { it.value }?.key ?: return

        shopCache.remove(leastUsedId)
        accessCount.remove(leastUsedId)
    }

    private fun cleanupOldAccesses() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        recentAccesses.removeIf { access ->
            val parts = access.split(":")
            parts.size == 2 && parts[1].toLong() < cutoffTime
        }
    }

    /**
     * Classe interne pour stocker les shops en cache avec metadata
     */
    private data class CachedShop(
        val shop: Shop,
        val cachedAt: Instant
    )

    /**
     * Statistiques du cache
     */
    data class CacheStats(
        val totalCached: Int,
        val expiredEntries: Int,
        val totalAccesses: Int
    )
}
