package loyfael.api.interfaces;

/**
 * Interface pour le conteneur de services principal
 * Principe d'inversion de dépendance - centralisation des dépendances
 */
public interface IServiceContainer {

    /**
     * Enregistre un service dans le conteneur
     */
    <T> void registerService(Class<T> serviceInterface, T implementation);

    /**
     * Récupère un service du conteneur
     */
    <T> T getService(Class<T> serviceInterface);

    /**
     * Vérifie si un service est enregistré
     */
    boolean hasService(Class<?> serviceInterface);

    /**
     * Initialise tous les services enregistrés
     */
    void initializeServices();

    /**
     * Ferme tous les services proprement
     */
    void shutdownServices();

    /**
     * Récupère les statistiques des services
     */
    String getServicesStatus();
}
