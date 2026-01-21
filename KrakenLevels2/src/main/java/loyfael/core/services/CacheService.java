package loyfael.core.services;

import loyfael.api.interfaces.ICacheService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

/**
 * Service de cache thread-safe avec expiration automatique
 * Principe de responsabilité unique : gestion du cache uniquement
 */
public class CacheService implements ICacheService {

    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public CacheService() {
        // Nettoyage automatique toutes les 5 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis()));
    }

    @Override
    public Optional<Object> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return Optional.of(entry.getValue());
        }

        // Supprimer l'entrée expirée
        if (entry != null && entry.isExpired()) {
            cache.remove(key);
        }

        return Optional.empty();
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

    @Override
    public void invalidatePlayer(String playerUuid) {
        cache.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            return key.startsWith(playerUuid + "_") || key.equals("exists_" + playerUuid);
        });
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public boolean contains(String key) {
        CacheEntry entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    @Override
    public String getStats() {
        cleanupExpiredEntries(); // Nettoyer avant de compter
        return "&7Cache: " + cache.size() + " entrées actives";
    }

    @Override
    public void setTtl(String key, long ttlMs) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.setExpirationTime(System.currentTimeMillis() + ttlMs);
        }
    }

    /**
     * Ajoute une entrée avec un TTL court pour synchronisation inter-serveur
     * Utilisé pour les données critiques qui doivent être synchronisées rapidement
     */
    public void putWithShortTtl(String key, Object value) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis()));
        // TTL court pour synchronisation entre serveurs : 30 secondes au lieu de 30 minutes
        setTtl(key, TimeUnit.SECONDS.toMillis(30));
    }

    private void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Classe interne pour les entrées du cache avec expiration
     */
    private static class CacheEntry {
        private final Object value;
        private final long creationTime;
        private long expirationTime;
        private static final long DEFAULT_TTL = TimeUnit.MINUTES.toMillis(30); // 30 minutes par défaut

        public CacheEntry(Object value, long creationTime) {
            this.value = value;
            this.creationTime = creationTime;
            this.expirationTime = creationTime + DEFAULT_TTL;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        public boolean isExpired(long currentTime) {
            return currentTime > expirationTime;
        }

        public void setExpirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
        }
    }
}
