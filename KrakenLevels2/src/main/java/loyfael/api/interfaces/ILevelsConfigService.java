package loyfael.api.interfaces;

import loyfael.core.services.LevelsConfigService.LevelConfig;
import java.util.List;
import java.util.Map;

/**
 * Interface pour le service de configuration des niveaux
 */
public interface ILevelsConfigService {

    /**
     * Obtient la configuration d'un niveau spécifique
     */
    LevelConfig getLevelConfig(int level);

    /**
     * Obtient toutes les configurations de niveau
     */
    Map<Integer, LevelConfig> getAllLevels();

    /**
     * Obtient le niveau maximum défini dans la configuration
     */
    int getMaxLevel();

    /**
     * Vérifie si un niveau existe dans la configuration
     */
    boolean levelExists(int level);

    /**
     * Obtient le nombre total de niveaux configurés
     */
    int getTotalLevels();

    /**
     * Obtient tous les niveaux triés par numéro
     */
    List<LevelConfig> getSortedLevels();

    /**
     * Recharge la configuration des niveaux
     */
    void reload();

    /**
     * Obtient les niveaux par type
     */
    List<LevelConfig> getLevelsByType(String typeName);

    /**
     * Obtient les niveaux de type currency
     */
    List<LevelConfig> getCurrencyLevels();

    /**
     * Obtient les niveaux de type missions
     */
    List<LevelConfig> getMissionLevels();
}
