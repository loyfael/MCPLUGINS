package loyfael.core.services;

import loyfael.api.interfaces.ISynchronizationService;
import loyfael.api.interfaces.IDatabaseService;
import loyfael.api.interfaces.ICacheService;
import loyfael.api.interfaces.IConfigurationService;
import loyfael.api.interfaces.IMongoConnectionManager;
import loyfael.api.interfaces.IPlayerService;
import loyfael.Main;
import loyfael.core.mongodb.MongoLogger;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;

/**
 * Service de synchronisation inter-serveur utilisant MongoDB polling (compatible standalone)
 * Principe de responsabilité unique : synchronisation uniquement
 * Uses shared MongoDB connection manager
 */
public class SynchronizationService implements ISynchronizationService {

    private final IDatabaseService databaseService;
    private final ICacheService cacheService;
    private final IConfigurationService configService;
    private final IMongoConnectionManager connectionManager;
    
    // MongoDB references using shared connection
    private MongoDatabase syncDatabase;
    private MongoCollection<Document> syncCollection;
    
    // Configuration du serveur
    private String serverName; // Non-final pour permettre l'initialisation différée
    private final ScheduledExecutorService syncExecutor;
    
    // Données de synchronisation
    private final ConcurrentMap<String, Long> lastSyncTimes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> syncEnabled = new ConcurrentHashMap<>();
    private final AtomicLong syncOperations = new AtomicLong(0);
    private final AtomicInteger conflictsResolved = new AtomicInteger(0);
    
    private volatile boolean running = false;

    public SynchronizationService(IDatabaseService databaseService, 
                                ICacheService cacheService, 
                                IConfigurationService configService,
                                IMongoConnectionManager connectionManager) {
        this.databaseService = databaseService;
        this.cacheService = cacheService;
        this.configService = configService;
        this.connectionManager = connectionManager;
        // Initialize server name as null, will be set in start() method
        this.serverName = null;
        this.syncExecutor = Executors.newScheduledThreadPool(2);
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        try {
            this.serverName = configService.getConfig().getString("server.name", "server-" + System.currentTimeMillis());
            initializeMongoDB();
            startChangeStreamListener();
            startPeriodicSync();
            running = true;
            MongoLogger.debug("Service de synchronisation démarré pour le serveur : " + serverName);
        } catch (Exception e) {
            MongoLogger.error("Erreur lors du démarrage de la synchronisation : " + e.getMessage());
            MongoLogger.debug("Détail démarrage synchronisation", e);
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        
        try {
            // Connection manager handles cleanup of shared connection
            syncDatabase = null;
            syncCollection = null;
            
            syncExecutor.shutdown();
            if (!syncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
            MongoLogger.debug("Service de synchronisation arrêté.");
        } catch (Exception e) {
            MongoLogger.debug("Erreur lors de l'arrêt de la synchronisation", e);
        }
    }

    private void initializeMongoDB() throws Exception {
        // Use shared connection manager instead of creating a new connection
        if (connectionManager == null) {
            throw new RuntimeException("MongoDB connection manager not available (MongoDB might be disabled)");
        }
        
        if (!connectionManager.isConnected()) {
            throw new RuntimeException("MongoDB connection not initialized");
        }
        
        String databaseName = connectionManager.getDatabaseName();
        syncDatabase = connectionManager.getDatabase(databaseName);
        syncCollection = syncDatabase.getCollection("playerdata");
    }

    private void startChangeStreamListener() {
        // Utiliser un système de polling au lieu des Change Streams pour compatibilité standalone MongoDB
        syncExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (!running) return;
                
                // Polling périodique pour vérifier les changements
                checkForDataChanges();
                
            } catch (Exception e) {
                if (running) {
                    MongoLogger.debug("Erreur lors de la vérification des changements", e);
                }
            }
        }, 5, 10, java.util.concurrent.TimeUnit.SECONDS); // Vérifier toutes les 10 secondes
    }

    /**
     * Vérifie les changements de données en utilisant polling (compatible MongoDB standalone)
     */
    private void checkForDataChanges() {
        try {
            // Récupérer tous les joueurs connectés
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerUuid = player.getUniqueId().toString();
                checkPlayerDataChange(playerUuid);
            }
        } catch (Exception e) {
            MongoLogger.debug("Erreur lors de la vérification des changements", e);
        }
    }

    /**
     * Vérifie si les données d'un joueur ont changé depuis la dernière synchronisation
     */
    private void checkPlayerDataChange(String playerUuid) {
        try {
            // Récupérer les données depuis MongoDB
            Document filter = new Document("_id", "player_" + playerUuid);
            Document playerDoc = syncCollection.find(filter).first();
            
            if (playerDoc == null) return;

            // Vérifier les métadonnées pour éviter les boucles
            Document metadata = playerDoc.get("metadata", Document.class);
            if (metadata != null && serverName.equals(metadata.getString("lastModifiedBy"))) {
                return; // Ignore nos propres changements
            }

            // Vérifier le timestamp de dernière modification
            Long lastModified = metadata != null ? metadata.getLong("lastModified") : null;
            Long lastSyncTime = lastSyncTimes.get(playerUuid);
            
            if (lastModified != null && (lastSyncTime == null || lastModified > lastSyncTime)) {
                // Les données ont changé, synchroniser
                handlePlayerDataSync(playerUuid, playerDoc);
            }

        } catch (Exception e) {
            MongoLogger.debug("Erreur lors de la vérification des données du joueur " + playerUuid, e);
        }
    }

    /**
     * Traite la synchronisation des données d'un joueur
     */
    private void handlePlayerDataSync(String playerUuid, Document playerDoc) {
        try {
            // Invalider le cache local pour ce joueur
            cacheService.invalidatePlayer(playerUuid);
            lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            syncOperations.incrementAndGet();

            // Notifier le joueur si il est connecté
            Player player = Bukkit.getPlayer(java.util.UUID.fromString(playerUuid));
            if (player != null && player.isOnline()) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    // Recharger les données du joueur
                    Main.getInstance().getPlayerService().getPlayerData(playerUuid);
                    // Optionnel: notifier le joueur
                    // player.sendMessage("§aDonnées synchronisées avec les autres serveurs");
                });
            }

        } catch (Exception e) {
            MongoLogger.debug("Erreur lors de la synchronisation des données du joueur " + playerUuid, e);
        }
    }

    private void startPeriodicSync() {
        // Synchronisation périodique toutes les 30 secondes
        syncExecutor.scheduleAtFixedRate(() -> {
            try {
                // Synchroniser tous les joueurs connectés
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String uuid = player.getUniqueId().toString();
                    if (syncEnabled.getOrDefault(uuid, true)) {
                        syncPlayerData(uuid);
                    }
                }
            } catch (Exception e) {
                MongoLogger.debug("Erreur lors de la synchronisation périodique", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Boolean> syncPlayerData(String playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!syncEnabled.getOrDefault(playerUuid, true)) {
                    return false;
                }

                // Marquer les données avec notre serveur pour éviter les boucles
                var playerDataOpt = Main.getInstance().getPlayerService().getPlayerData(playerUuid);
                if (playerDataOpt.isPresent()) {
                    var playerData = playerDataOpt.get();
                    
                    // Ajouter des métadonnées de synchronisation
                    Document metadata = new Document()
                        .append("lastModifiedBy", serverName)
                        .append("lastModified", System.currentTimeMillis())
                        .append("version", System.currentTimeMillis());

                    // Sauvegarder avec métadonnées (sera intercepté par le change stream des autres serveurs)
                    Main.getInstance().getPlayerService().savePlayerData(playerUuid, playerData);
                    
                    lastSyncTimes.put(playerUuid, System.currentTimeMillis());
                    syncOperations.incrementAndGet();
                    return true;
                }
                return false;
            } catch (Exception e) {
                MongoLogger.debug("Erreur lors de la synchronisation des données de " + playerUuid, e);
                return false;
            }
        }, syncExecutor);
    }

    @Override
    public CompletableFuture<Boolean> forceSync(String playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                
                // Récupérer les données depuis MongoDB
                Document filter = new Document("_id", "player_" + playerUuid);
                Document mongoDoc = syncCollection.find(filter).first();
                
                // Récupérer les données locales actuelles
                var localPlayerDataOpt = Main.getInstance().getPlayerService().getPlayerData(playerUuid);
                
                if (mongoDoc == null && !localPlayerDataOpt.isPresent()) {
                    MongoLogger.debug("Aucune donnée trouvée pour " + playerUuid);
                    return false;
                }
                
                // Si seulement des données locales existent, les pousser vers MongoDB
                if (mongoDoc == null && localPlayerDataOpt.isPresent()) {
                    // Utils.sendConsoleLog("&e[SYNC] ⬆️ Poussée des données locales vers MongoDB pour " + playerUuid);
                    Main.getInstance().getPlayerService().savePlayerData(playerUuid, localPlayerDataOpt.get());
                    // Utils.sendConsoleLog("&a[SYNC] ✅ Données locales poussées vers MongoDB pour " + playerUuid);
                    return true;
                }
                
                // Si seulement des données MongoDB existent, les charger localement
                if (mongoDoc != null && !localPlayerDataOpt.isPresent()) {
                    // Utils.sendConsoleLog("&e[SYNC] ⬇️ Chargement des données MongoDB localement pour " + playerUuid);
                    cacheService.invalidatePlayer(playerUuid);
                    Main.getInstance().getPlayerService().getPlayerData(playerUuid);
                    // Utils.sendConsoleLog("&a[SYNC] ✅ Données MongoDB chargées localement pour " + playerUuid);
                    return true;
                }
                
                // Les deux existent : fusion intelligente
                // Utils.sendConsoleLog("&e[SYNC] 🔄 Fusion intelligente des données pour " + playerUuid);
                IPlayerService.PlayerData localData = localPlayerDataOpt.get();
                IPlayerService.PlayerData mongoData = deserializeMongoData(mongoDoc);
                
                if (mongoData == null) {
                    MongoLogger.debug("Erreur de désérialisation MongoDB pour " + playerUuid);
                    return false;
                }
                
                // Utils.sendConsoleLog("&9[SYNC] 📊 Local - Niveau: " + localData.getLevel() + ", Missions: " + localData.getMissionProgress().size());
                // Utils.sendConsoleLog("&9[SYNC] 📊 MongoDB - Niveau: " + mongoData.getLevel() + ", Missions: " + mongoData.getMissionProgress().size());
                
                // Créer les données fusionnées
                IPlayerService.PlayerData mergedData = mergePlayers(localData, mongoData, playerUuid);
                
                // Utils.sendConsoleLog("&9[SYNC] 📊 Fusionné - Niveau: " + mergedData.getLevel() + ", Missions: " + mergedData.getMissionProgress().size());
                
                // Sauvegarder les données fusionnées localement et dans MongoDB
                Main.getInstance().getPlayerService().savePlayerData(playerUuid, mergedData);
                // Utils.sendConsoleLog("&a[SYNC] ✅ Données fusionnées et synchronisées pour " + playerUuid);
                
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
                syncOperations.incrementAndGet();
                return true;
                
            } catch (Exception e) {
                MongoLogger.debug("Erreur lors de la synchronisation forcée de " + playerUuid, e);
                return false;
            }
        }, syncExecutor);
    }

    @Override
    public void notifyDataChange(String playerUuid, String changeType, Object data) {
        syncExecutor.submit(() -> {
            try {
                // Les changements seront automatiquement détectés par le change stream
                syncPlayerData(playerUuid);
            } catch (Exception e) {
                MongoLogger.debug("Erreur lors de la notification de changement", e);
            }
        });
    }

    @Override
    public boolean isDataUpToDate(String playerUuid) {
        Long lastSync = lastSyncTimes.get(playerUuid);
        if (lastSync == null) return false;
        
        // Considérer les données comme à jour si synchronisées dans les 60 dernières secondes
        return (System.currentTimeMillis() - lastSync) < 60000;
    }

    @Override
    public void setSyncEnabled(String playerUuid, boolean enabled) {
        syncEnabled.put(playerUuid, enabled);
        if (enabled) {
            // Utils.sendConsoleLog("&aSynchronisation activée pour " + playerUuid);
            syncPlayerData(playerUuid);
        } else {
            MongoLogger.debug("Synchronisation désactivée pour " + playerUuid);
        }
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    /**
     * Récupère le timestamp de dernière modification locale pour un joueur
     */
    private Long getLocalLastModified(String playerUuid) {
        // Pour simplifier, on utilise la dernière fois que ce serveur a synchronisé le joueur
        return lastSyncTimes.getOrDefault(playerUuid, 0L);
    }

    /**
     * Désérialise un document MongoDB en PlayerData
     */
    private IPlayerService.PlayerData deserializeMongoData(Document mongoDoc) {
        try {
            // Extraire le sous-document "data" qui contient les vraies données du joueur
            Document dataDoc = mongoDoc.get("data", Document.class);
            if (dataDoc == null) {
                throw new IllegalArgumentException("Aucun sous-document 'data' trouvé dans le document MongoDB");
            }
            
            // Gestion sécurisée de tous les champs avec valeurs par défaut
            String uuid = dataDoc.getString("uuid");
            String name = dataDoc.getString("name");
            
            if (uuid == null || name == null) {
                throw new IllegalArgumentException("UUID ou nom manquant dans le sous-document 'data'");
            }
            
            int level = dataDoc.getInteger("level", 0);
            
            // Gestion sécurisée de lastSeen avec valeur par défaut
            Long lastSeenLong = dataDoc.getLong("lastSeen");
            long lastSeen = lastSeenLong != null ? lastSeenLong : System.currentTimeMillis();
            
            int buttonAmount = dataDoc.getInteger("buttonAmount", 0);
            
            IPlayerService.PlayerData playerData = new IPlayerService.PlayerData(uuid, name, level, lastSeen, buttonAmount);
            
            // Restaurer la progression des missions (lire depuis le sous-document data)
            Document missionProgressDoc = dataDoc.get("missionProgress", Document.class);
            if (missionProgressDoc != null) {
                Map<String, Integer> missionProgress = new HashMap<>();
                for (String key : missionProgressDoc.keySet()) {
                    missionProgress.put(key, missionProgressDoc.getInteger(key, 0));
                }
                playerData.setMissionProgress(missionProgress);
            }
            
            // Restaurer les données personnalisées (lire depuis le sous-document data)
            Document customDataDoc = dataDoc.get("customData", Document.class);
            if (customDataDoc != null) {
                Map<String, Object> customData = new HashMap<>();
                for (String key : customDataDoc.keySet()) {
                    customData.put(key, customDataDoc.get(key));
                }
                playerData.setCustomData(customData);
            }
            
            return playerData;
        } catch (Exception e) {
            MongoLogger.debug("Erreur lors de la désérialisation MongoDB", e);
            if (mongoDoc != null) {
                MongoLogger.debug("Document MongoDB : " + mongoDoc.toJson());
            }
            return null;
        }
    }

    /**
     * Fusionne intelligemment les données de deux sources
     * Prend le niveau le plus élevé et la progression maximale pour chaque mission
     */
    private IPlayerService.PlayerData mergePlayers(IPlayerService.PlayerData local, IPlayerService.PlayerData mongo, String playerUuid) {
        // Utils.sendConsoleLog("&9[SYNC] 🔄 Début fusion pour " + playerUuid);
        
        // Prendre le niveau le plus élevé
        int finalLevel = Math.max(local.getLevel(), mongo.getLevel());
        // Utils.sendConsoleLog("&9[SYNC] 📈 Niveaux - Local: " + local.getLevel() + ", MongoDB: " + mongo.getLevel() + " → Final: " + finalLevel);
        
        // Prendre le lastSeen le plus récent
        long finalLastSeen = Math.max(local.getLastSeen(), mongo.getLastSeen());
        
        // Créer les données fusionnées avec les infos de base
        IPlayerService.PlayerData merged = new IPlayerService.PlayerData(
            local.getUuid(), 
            local.getName(), 
            finalLevel, 
            finalLastSeen, 
            Math.max(local.getButtonAmount(), mongo.getButtonAmount())
        );
        
        // Fusionner la progression des missions (prendre le max pour chaque mission)
        Map<String, Integer> mergedMissionProgress = new HashMap<>();
        
        // Ajouter toutes les missions locales
        if (local.getMissionProgress() != null) {
            mergedMissionProgress.putAll(local.getMissionProgress());
            // Utils.sendConsoleLog("&9[SYNC] 📝 Missions locales copiées: " + local.getMissionProgress().size());
        }
        
        // Fusionner avec les missions MongoDB (prendre le max)
        if (mongo.getMissionProgress() != null) {
            // Utils.sendConsoleLog("&9[SYNC] 📝 Missions MongoDB à fusionner: " + mongo.getMissionProgress().size());
            for (Map.Entry<String, Integer> entry : mongo.getMissionProgress().entrySet()) {
                String missionKey = entry.getKey();
                int mongoProgress = entry.getValue();
                int localProgress = mergedMissionProgress.getOrDefault(missionKey, 0);
                
                // Prendre le progrès maximum
                int finalProgress = Math.max(localProgress, mongoProgress);
                mergedMissionProgress.put(missionKey, finalProgress);
                
                if (finalProgress != localProgress) {
                    MongoLogger.debug("Mission " + missionKey + " : " + localProgress + " → " + finalProgress);
                }
            }
        }
        
        merged.setMissionProgress(mergedMissionProgress);
        // Utils.sendConsoleLog("&9[SYNC] 📝 Total missions après fusion: " + mergedMissionProgress.size());
        
        // Fusionner les données personnalisées (MongoDB gagne en cas de conflit)
        Map<String, Object> mergedCustomData = new HashMap<>();
        if (local.getCustomData() != null) {
            mergedCustomData.putAll(local.getCustomData());
        }
        if (mongo.getCustomData() != null) {
            mergedCustomData.putAll(mongo.getCustomData()); // MongoDB overwrite local
        }
        merged.setCustomData(mergedCustomData);
        
        // Logger les changements
        // if (finalLevel != local.getLevel()) {
        //     Utils.sendConsoleLog("&a[SYNC] ✅ Niveau synchronisé pour " + playerUuid + ": " + local.getLevel() + " → " + finalLevel);
        // }
        
        int missionChanges = 0;
        if (local.getMissionProgress() != null && mongo.getMissionProgress() != null) {
            for (String missionKey : mergedMissionProgress.keySet()) {
                int localProg = local.getMissionProgress().getOrDefault(missionKey, 0);
                int mongoProg = mongo.getMissionProgress().getOrDefault(missionKey, 0);
                int finalProg = mergedMissionProgress.get(missionKey);
                
                if (finalProg != localProg) {
                    missionChanges++;
                }
            }
        }
        
        // if (missionChanges > 0) {
        //     Utils.sendConsoleLog("&a[SYNC] ✅ Progression de " + missionChanges + " mission(s) synchronisée pour " + playerUuid);
        // }
        
        // Utils.sendConsoleLog("&9[SYNC] 🏁 Fin fusion pour " + playerUuid);
        return merged;
    }

    @Override
    public SyncStats getStats() {
        return new SyncStats(
            syncOperations.get(),
            lastSyncTimes.values().stream().mapToLong(Long::longValue).max().orElse(0),
            conflictsResolved.get(),
            serverName
        );
    }
}
