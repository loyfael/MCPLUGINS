package loyfael.api.interfaces;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Interface pour les services de configuration
 * Principe d'inversion de dépendance - les classes dépendent d'abstractions
 */
public interface IConfigurationService {

    /**
     * Initialise le service de configuration
     */
    void initialize();

    /**
     * Recharge la configuration
     */
    void reload();

    /**
     * Sauvegarde la configuration
     */
    void save();

    /**
     * Obtient la configuration
     */
    FileConfiguration getConfig();

    /**
     * Obtient un message depuis messages.yml avec support des placeholders
     */
    String getMessage(String path, Object... placeholders);

    /**
     * Obtient un message depuis messages.yml sans placeholders
     */
    String getMessage(String path);

    /**
     * Vérifie si le service est initialisé
     */
    boolean isInitialized();

    /**
     * Ferme proprement le service
     */
    void shutdown();
}
