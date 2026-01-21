package loyfael.api.interfaces;

import java.util.Optional;

/**
 * Interface pour les services de cache
 * Principe de responsabilité unique - séparation du cache et de la base de données
 */
public interface ICacheService {

    /**
     * Met en cache une valeur
     */
    void put(String key, Object value);

    /**
     * Récupère une valeur du cache
     */
    Optional<Object> get(String key);

    /**
     * Supprime une valeur du cache
     */
    void remove(String key);

    /**
     * Invalide le cache d'un joueur
     */
    void invalidatePlayer(String playerUuid);

    /**
     * Vide complètement le cache
     */
    void clear();

    /**
     * Vérifie si une clé existe dans le cache
     */
    boolean contains(String key);

    /**
     * Récupère les statistiques du cache
     */
    String getStats();

    /**
     * Définit la durée de vie des entrées (en millisecondes)
     */
    void setTtl(String key, long ttlMs);

    /**
     * Ajoute une entrée avec un TTL court pour synchronisation inter-serveur
     * Utilisé pour les données critiques qui doivent être synchronisées rapidement
     */
    default void putWithShortTtl(String key, Object value) {
        put(key, value);
        setTtl(key, 30000); // 30 secondes par défaut
    }
}
