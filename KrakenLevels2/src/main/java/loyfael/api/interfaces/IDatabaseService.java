package loyfael.api.interfaces;

import java.util.Map;
import java.util.Optional;

/**
 * Interface pour les services de base de données
 * Principe d'inversion de dépendance et de responsabilité unique
 */
public interface IDatabaseService {

    /**
     * Initialise la connexion à la base de données
     */
    boolean initialize();

    /**
     * Sauvegarde une donnée
     */
    void saveData(String key, Object value);

    /**
     * Récupère une donnée
     */
    Optional<Object> getData(String key);

    /**
     * Supprime une donnée
     */
    boolean deleteData(String key);

    /**
     * Vérifie si une donnée existe
     */
    boolean exists(String key);

    /**
     * Récupère toutes les données d'un préfixe
     */
    Map<String, Object> getDataByPrefix(String prefix);

    /**
     * Ferme la connexion
     */
    void disconnect();

    /**
     * Vérifie si la connexion est active
     */
    boolean isConnected();

    /**
     * Effectue une sauvegarde complète
     */
    void backup();
}
