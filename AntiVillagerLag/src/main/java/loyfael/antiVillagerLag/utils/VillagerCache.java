package loyfael.antiVillagerLag.utils;

import org.bukkit.entity.Villager;
import loyfael.antiVillagerLag.AntiVillagerLag;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/*
* Classe of cache for Villager data
* - Thread-safe cache using ConcurrentHashMap
* - Stores villager data like AI state, cooldowns, and last restock time
* - Provides methods to get, update, and remove villager data
* - Automatic cleanup of old entries to prevent memory leaks
*/
public class VillagerCache {

    // Cache thread-safe pour les données des villagers
    private static final ConcurrentHashMap<UUID, VillagerData> cache = new ConcurrentHashMap<>();

    // Classe interne pour stocker les données en mémoire
    public static class VillagerData {
        public boolean isMarked;
        public boolean aiState;
        public long aiCooldown;
        public long levelCooldown;
        public long lastRestock;
        public long lastUpdate;

        public VillagerData(boolean isMarked, boolean aiState, long aiCooldown, long levelCooldown, long lastRestock) {
            this.isMarked = isMarked;
            this.aiState = aiState;
            this.aiCooldown = aiCooldown;
            this.levelCooldown = levelCooldown;
            this.lastRestock = lastRestock;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    public static VillagerData getVillagerData(Villager villager, AntiVillagerLag plugin) {
        UUID uuid = villager.getUniqueId();

        // Verify cache first
        VillagerData cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }

        // if not in cache, check if villager is marked
        boolean isMarked = VillagerUtilities.hasMarker(villager, plugin);
        if (!isMarked) {
            return null; // villager is not marked, no data to return
        }

        boolean aiState = VillagerUtilities.getMarker(villager, plugin);
        long aiCooldown = VillagerUtilities.getAiCooldown(villager, plugin);
        long levelCooldown = VillagerUtilities.getLevelCooldown(villager, plugin);
        long lastRestock = VillagerUtilities.getLastRestock(villager, plugin);

        VillagerData data = new VillagerData(isMarked, aiState, aiCooldown, levelCooldown, lastRestock);
        cache.put(uuid, data);

        return data;
    }

    public static void updateVillagerData(Villager villager, VillagerData data) {
        cache.put(villager.getUniqueId(), data);
        data.lastUpdate = System.currentTimeMillis();
    }

    public static void removeVillager(UUID uuid) {
        cache.remove(uuid);
    }

    public static void clearCache() {
        cache.clear();
    }

    // Automatic cleanup of old entries
    // Removes entries older than 5 minutes (300000 milliseconds)
    public static void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        long maxAge = 300000; // 5 minutes

        cache.entrySet().removeIf(entry ->
            (now - entry.getValue().lastUpdate) > maxAge
        );
    }

    public static int getCacheSize() {
        return cache.size();
    }
}
