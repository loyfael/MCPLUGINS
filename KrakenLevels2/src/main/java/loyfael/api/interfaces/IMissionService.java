package loyfael.api.interfaces;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.List;
import java.util.Map;

/**
 * Interface pour les services de missions
 * Principe de responsabilité unique - gestion centralisée des missions
 */
public interface IMissionService {

    /**
     * Traite un événement pour les missions
     */
    void processEvent(Player player, Event event);

    /**
     * Récupère les missions actives d'un joueur
     */
    List<Mission> getPlayerMissions(String playerUuid);

    /**
     * Ajoute une mission à un joueur
     */
    void assignMission(String playerUuid, Mission mission);

    /**
     * Met à jour le progrès d'une mission
     */
    void updateMissionProgress(String playerUuid, String missionId, int progress);

    /**
     * Récupère le progrès d'une mission
     */
    int getMissionProgress(String playerUuid, String missionId);

    /**
     * Vérifie si une mission est complétée
     */
    boolean isMissionCompleted(String playerUuid, String missionId);

    /**
     * Récompense le joueur pour une mission complétée
     */
    void rewardPlayer(Player player, Mission mission);

    /**
     * Récupère toutes les missions disponibles
     */
    List<Mission> getAvailableMissions();

    /**
     * Force la complétion d'un niveau et déclenche les récompenses associées
     */
    boolean forceCompleteLevel(Player player, int levelNumber);

    /**
     * Classe interne pour encapsuler les données de mission
     */
    class Mission {
        private final String id;
        private final String name;
        private final String type;
        private final int targetAmount;
        private final Map<String, Object> rewards;
        private final String description; // Ajout de description
        private int currentProgress;

        public Mission(String id, String name, String type, int targetAmount, Map<String, Object> rewards) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.targetAmount = targetAmount;
            this.rewards = rewards;
            this.description = "Mission: " + name; // Description par défaut
            this.currentProgress = 0;
        }

        // Constructeur avec description
        public Mission(String id, String name, String type, int targetAmount, Map<String, Object> rewards, String description) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.targetAmount = targetAmount;
            this.rewards = rewards;
            this.description = description != null ? description : "Mission: " + name;
            this.currentProgress = 0;
        }

        // Getters et setters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public int getTargetAmount() { return targetAmount; }
        public Map<String, Object> getRewards() { return rewards; }
        public String getDescription() { return description; } // Ajout du getter description
        public int getCurrentProgress() { return currentProgress; }
        public void setCurrentProgress(int currentProgress) { this.currentProgress = currentProgress; }

        public boolean isCompleted() { return currentProgress >= targetAmount; }
        public double getProgressPercentage() { return (double) currentProgress / targetAmount * 100; }
    }

    /**
     * Sauvegarde les données modifiées d'un joueur spécifique
     */
    default void savePlayerDataIfModified(String playerUuid) {
        // Implémentation par défaut vide pour la compatibilité
    }

    /**
     * Sauvegarde toutes les données modifiées
     */
    default void saveAllModifiedData() {
        // Implémentation par défaut vide pour la compatibilité
    }

    /**
     * Nettoie les données d'un joueur déconnecté
     */
    default void cleanupPlayerData(String playerUuid) {
        // Implémentation par défaut vide pour la compatibilité
    }
}
