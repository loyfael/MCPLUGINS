package loyfael.api.interfaces;

import org.bukkit.entity.Player;
import java.util.concurrent.CompletableFuture;

/**
 * Interface pour la synchronisation inter-serveur
 * Principe de responsabilité unique : gestion de la synchronisation uniquement
 */
public interface ISynchronizationService {

    /**
     * Démarre le service de synchronisation
     */
    void start();

    /**
     * Arrête le service de synchronisation
     */
    void stop();

    /**
     * Synchronise les données d'un joueur avec les autres serveurs
     */
    CompletableFuture<Boolean> syncPlayerData(String playerUuid);

    /**
     * Force la synchronisation complète d'un joueur depuis la base
     */
    CompletableFuture<Boolean> forceSync(String playerUuid);

    /**
     * Notifie les autres serveurs d'un changement de données
     */
    void notifyDataChange(String playerUuid, String changeType, Object data);

    /**
     * Vérifie si les données d'un joueur sont à jour
     */
    boolean isDataUpToDate(String playerUuid);

    /**
     * Active/désactive la synchronisation pour un joueur
     */
    void setSyncEnabled(String playerUuid, boolean enabled);

    /**
     * Récupère le nom de ce serveur
     */
    String getServerName();

    /**
     * Obtient les statistiques de synchronisation
     */
    SyncStats getStats();

    /**
     * Classe pour les statistiques de synchronisation
     */
    class SyncStats {
        private final long syncOperations;
        private final long lastSyncTime;
        private final int conflictsResolved;
        private final String serverName;

        public SyncStats(long syncOperations, long lastSyncTime, int conflictsResolved, String serverName) {
            this.syncOperations = syncOperations;
            this.lastSyncTime = lastSyncTime;
            this.conflictsResolved = conflictsResolved;
            this.serverName = serverName;
        }

        public long getSyncOperations() { return syncOperations; }
        public long getLastSyncTime() { return lastSyncTime; }
        public int getConflictsResolved() { return conflictsResolved; }
        public String getServerName() { return serverName; }
    }

    /**
     * Types de changements pour la synchronisation
     */
    enum ChangeType {
        LEVEL_UPDATE,
        MISSION_PROGRESS,
        PLAYER_DATA,
        CACHE_INVALIDATION
    }
}
