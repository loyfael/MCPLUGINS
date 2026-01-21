package loyfael.core.services;

import loyfael.api.interfaces.IPlayerService;
import loyfael.api.interfaces.IDatabaseService;
import loyfael.api.interfaces.ICacheService;
import loyfael.utils.Utils;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;

/**
 * Service de gestion des joueurs avec cache et base de données
 * Principe de responsabilité unique : gestion des données joueurs uniquement
 * Principe d'inversion de dépendance : dépend d'abstractions, pas d'implémentations
 */
public class PlayerService implements IPlayerService {

    private final IDatabaseService databaseService;
    private final ICacheService cacheService;

    public PlayerService(IDatabaseService databaseService, ICacheService cacheService) {
        this.databaseService = databaseService;
        this.cacheService = cacheService;
    }

    @Override
    public Optional<PlayerData> getPlayerData(String playerUuid) {
        if (playerUuid == null || playerUuid.trim().isEmpty()) {
            return Optional.empty();
        }

        // Vérifier d'abord le cache
        String cacheKey = "player_" + playerUuid;
        Optional<Object> cached = cacheService.get(cacheKey);

        if (cached.isPresent() && cached.get() instanceof PlayerData) {
            return Optional.of((PlayerData) cached.get());
        }

        // Si pas en cache, récupérer de la base de données
        Optional<Object> dbData = databaseService.getData(cacheKey);
        if (dbData.isPresent()) {
            try {
                PlayerData playerData = deserializePlayerData(dbData.get());
                // Mettre en cache pour les prochaines requêtes
                cacheService.put(cacheKey, playerData);
                return Optional.of(playerData);
            } catch (Exception e) {
                Utils.sendConsoleLog("&cErreur lors de la désérialisation des données joueur: " + e.getMessage());
            }
        }

        return Optional.empty();
    }

    @Override
    public void savePlayerData(String playerUuid, PlayerData data) {
        if (playerUuid == null || data == null) {
            throw new IllegalArgumentException("UUID et données joueur ne peuvent pas être null");
        }

        String cacheKey = "player_" + playerUuid;

        // Assurer que les Maps ne sont jamais null (sécurité supplémentaire)
        if (data.getMissionProgress() == null) {
            data.setMissionProgress(new java.util.HashMap<>());
        }
        if (data.getCustomData() == null) {
            data.setCustomData(new java.util.HashMap<>());
        }

        // Mettre en cache avec TTL court pour synchronisation rapide entre serveurs
        cacheService.putWithShortTtl(cacheKey, data);

        // Sauvegarder en base de données de manière asynchrone
        try {
            Map<String, Object> serializedData = serializePlayerData(data);
            databaseService.saveData(cacheKey, serializedData);
        } catch (Exception e) {
            Utils.sendConsoleLog("&cErreur lors de la sauvegarde des données joueur: " + e.getMessage());
        }
    }

    @Override
    public boolean playerExists(String playerUuid) {
        if (playerUuid == null || playerUuid.trim().isEmpty()) {
            return false;
        }

        String cacheKey = "exists_" + playerUuid;

        // Vérifier le cache d'abord
        Optional<Object> cached = cacheService.get(cacheKey);
        if (cached.isPresent()) {
            return (Boolean) cached.get();
        }

        // Vérifier la base de données
        boolean exists = databaseService.exists("player_" + playerUuid);

        // Mettre en cache le résultat
        cacheService.put(cacheKey, exists);
        cacheService.setTtl(cacheKey, 300000); // 5 minutes de cache

        return exists;
    }

    @Override
    public PlayerData createPlayer(String playerUuid, String playerName) {
        if (playerUuid == null || playerName == null) {
            throw new IllegalArgumentException("UUID et nom du joueur ne peuvent pas être null");
        }

        PlayerData newPlayer = new PlayerData(
            playerUuid,
            playerName,
            0, // niveau initial 0 - les joueurs doivent travailler pour atteindre le niveau 1
            System.currentTimeMillis(),
            0 // montant de boutons initial (à supprimer plus tard)
        );

        savePlayerData(playerUuid, newPlayer);
        return newPlayer;
    }

    @Override
    public void updateLastSeen(String playerUuid) {
        Optional<PlayerData> playerData = getPlayerData(playerUuid);
        if (playerData.isPresent()) {
            PlayerData data = playerData.get();
            data.setLastSeen(System.currentTimeMillis());
            savePlayerData(playerUuid, data);
        }
    }

    @Override
    public int getPlayerLevel(String playerUuid) {
        return getPlayerData(playerUuid)
            .map(PlayerData::getLevel)
            .orElse(0); // Niveau 0 par défaut au lieu de 1
    }

    @Override
    public void updatePlayerLevel(String playerUuid, int newLevel) {
        Optional<PlayerData> playerDataOpt = getPlayerData(playerUuid);
        if (playerDataOpt.isPresent()) {
            PlayerData data = playerDataOpt.get();
            data.setLevel(newLevel);
            
            // Utiliser un TTL court pour les changements de niveau critiques
            String key = "player_" + playerUuid;
            Map<String, Object> serializedData = serializePlayerData(data);
            cacheService.putWithShortTtl(key, serializedData);
            
            // Sauvegarder immédiatement en base
            databaseService.saveData(key, serializedData);
        }
    }

    @Override
    public void setPlayerLevel(String playerUuid, int newLevel) {
        // Alias pour updatePlayerLevel pour compatibilité
        updatePlayerLevel(playerUuid, newLevel);
    }

    @Override
    public List<PlayerData> getTopPlayers(int limit) {
        List<PlayerData> topPlayers = new ArrayList<>();

        try {
            Map<String, Object> allPlayerData = databaseService.getDataByPrefix("player_");

            // Convertir et trier par niveau
            allPlayerData.values().stream()
                .map(this::deserializePlayerData)
                .sorted((p1, p2) -> Integer.compare(p2.getLevel(), p1.getLevel()))
                .limit(Math.max(1, limit))
                .forEach(topPlayers::add);

        } catch (Exception e) {
            Utils.sendConsoleLog("&cErreur lors de la récupération du classement: " + e.getMessage());
        }

        return topPlayers;
    }

    @Override
    public PlayerStats getPlayerStats(String playerUuid) {
        Optional<PlayerData> playerDataOpt = getPlayerData(playerUuid);
        if (playerDataOpt.isEmpty()) {
            return new PlayerStats(0, 0, 0L);
        }

        PlayerData playerData = playerDataOpt.get();
        int totalMissionsCompleted = playerData.getMissionProgress().size();
        long playtime = System.currentTimeMillis() - playerData.getLastSeen(); // Estimation simple

        return new PlayerStats(playerData.getLevel(), totalMissionsCompleted, playtime);
    }

    /**
     * Sérialise les données joueur pour le stockage
     */
    private Map<String, Object> serializePlayerData(PlayerData data) {
        Map<String, Object> serialized = new java.util.HashMap<>();
        serialized.put("uuid", data.getUuid());
        serialized.put("name", data.getName());
        serialized.put("level", data.getLevel());
        serialized.put("lastSeen", data.getLastSeen());
        serialized.put("buttonAmount", data.getButtonAmount());
        serialized.put("missionProgress", data.getMissionProgress());
        serialized.put("customData", data.getCustomData());
        return serialized;
    }

    /**
     * Désérialise les données joueur depuis le stockage
     */
    @SuppressWarnings("unchecked")
    private PlayerData deserializePlayerData(Object data) {
        if (data instanceof PlayerData) {
            return (PlayerData) data;
        }

        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            PlayerData playerData = new PlayerData(
                (String) map.get("uuid"),
                (String) map.get("name"),
                ((Number) map.getOrDefault("level", 0)).intValue(), // Niveau 0 par défaut
                ((Number) map.getOrDefault("lastSeen", System.currentTimeMillis())).longValue(),
                ((Number) map.getOrDefault("buttonAmount", 0)).intValue()
            );
            
            // Restaurer la progression des missions
            Object missionProgressObj = map.get("missionProgress");
            if (missionProgressObj instanceof Map) {
                Map<String, Object> missionProgressMap = (Map<String, Object>) missionProgressObj;
                for (Map.Entry<String, Object> entry : missionProgressMap.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        playerData.getMissionProgress().put(entry.getKey(), ((Number) entry.getValue()).intValue());
                    }
                }
            }
            
            // Restaurer les données personnalisées
            Object customDataObj = map.get("customData");
            if (customDataObj instanceof Map) {
                playerData.setCustomData(new java.util.HashMap<>((Map<String, Object>) customDataObj));
            }
            
            return playerData;
        }

        // Gestion des MemorySection (format de configuration Bukkit)
        if (data instanceof org.bukkit.configuration.MemorySection) {
            org.bukkit.configuration.MemorySection section = (org.bukkit.configuration.MemorySection) data;
            PlayerData playerData = new PlayerData(
                section.getString("uuid"),
                section.getString("name"),
                section.getInt("level", 0), // Niveau 0 par défaut
                section.getLong("lastSeen", System.currentTimeMillis()),
                section.getInt("buttonAmount", 0)
            );
            
            // Restaurer la progression des missions depuis MemorySection
            Object missionProgressObj = section.get("missionProgress");
            if (missionProgressObj instanceof Map) {
                Map<String, Object> missionProgressMap = (Map<String, Object>) missionProgressObj;
                for (Map.Entry<String, Object> entry : missionProgressMap.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        playerData.getMissionProgress().put(entry.getKey(), ((Number) entry.getValue()).intValue());
                    }
                }
            }
            
            // Restaurer les données personnalisées depuis MemorySection
            Object customDataObj = section.get("customData");
            if (customDataObj instanceof Map) {
                playerData.setCustomData(new java.util.HashMap<>((Map<String, Object>) customDataObj));
            }
            
            return playerData;
        }

        throw new IllegalArgumentException("Format de données joueur invalide: " + data.getClass());
    }
}
