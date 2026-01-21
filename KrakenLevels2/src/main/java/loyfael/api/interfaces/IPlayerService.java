package loyfael.api.interfaces;

import org.bukkit.entity.Player;
import java.util.List;
import java.util.Optional;

/**
 * Interface pour les services de gestion des joueurs
 * Principe de responsabilité unique - gestion centralisée des données joueurs
 */
public interface IPlayerService {

    /**
     * Récupère les données d'un joueur
     */
    Optional<PlayerData> getPlayerData(String playerUuid);

    /**
     * Sauvegarde les données d'un joueur
     */
    void savePlayerData(String playerUuid, PlayerData data);

    /**
     * Vérifie si un joueur existe dans la base de données
     */
    boolean playerExists(String playerUuid);

    /**
     * Crée un nouveau profil joueur
     */
    PlayerData createPlayer(String playerUuid, String playerName);

    /**
     * Met à jour la dernière connexion du joueur
     */
    void updateLastSeen(String playerUuid);

    /**
     * Récup��re le niveau actuel d'un joueur
     */
    int getPlayerLevel(String playerUuid);

    /**
     * Met à jour le niveau d'un joueur
     */
    void updatePlayerLevel(String playerUuid, int newLevel);

    /**
     * Met à jour le niveau d'un joueur (alias pour compatibilité)
     */
    void setPlayerLevel(String playerUuid, int newLevel);

    /**
     * Récupère tous les joueurs par ordre de niveau
     */
    List<PlayerData> getTopPlayers(int limit);

    /**
     * Récupère les statistiques d'un joueur
     */
    PlayerStats getPlayerStats(String playerUuid);

    /**
     * Classe représentant les données d'un joueur
     */
    class PlayerData {
        private String uuid;
        private String name;
        private int level;
        private long lastSeen;
        private int buttonAmount; // Ajout de buttonAmount
        private java.util.Map<String, Integer> missionProgress;
        private java.util.Map<String, Object> customData;

        // Constructeur principal (2 paramètres)
        public PlayerData(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
            this.level = 0;
            this.lastSeen = System.currentTimeMillis();
            this.buttonAmount = 0;
            this.missionProgress = new java.util.HashMap<>();
            this.customData = new java.util.HashMap<>();
        }

        // Constructeur complet (5 paramètres pour compatibilité)
        public PlayerData(String uuid, String name, int level, long lastSeen, int buttonAmount) {
            this.uuid = uuid;
            this.name = name;
            this.level = level;
            this.lastSeen = lastSeen;
            this.buttonAmount = buttonAmount;
            this.missionProgress = new java.util.HashMap<>();
            this.customData = new java.util.HashMap<>();
        }

        // Getters et setters
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }

        public long getLastSeen() { return lastSeen; }
        public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

        public int getButtonAmount() { return buttonAmount; }
        public void setButtonAmount(int buttonAmount) { this.buttonAmount = buttonAmount; }

        public java.util.Map<String, Integer> getMissionProgress() { return missionProgress; }
        public void setMissionProgress(java.util.Map<String, Integer> missionProgress) { this.missionProgress = missionProgress; }

        public java.util.Map<String, Object> getCustomData() { return customData; }
        public void setCustomData(java.util.Map<String, Object> customData) { this.customData = customData; }
    }

    /**
     * Classe représentant les statistiques d'un joueur
     */
    class PlayerStats {
        private final int level;
        private final int totalMissionsCompleted;
        private final long playtime;

        public PlayerStats(int level, int totalMissionsCompleted, long playtime) {
            this.level = level;
            this.totalMissionsCompleted = totalMissionsCompleted;
            this.playtime = playtime;
        }

        public int getLevel() { return level; }
        public int getTotalMissionsCompleted() { return totalMissionsCompleted; }
        public long getPlaytime() { return playtime; }
    }
}
