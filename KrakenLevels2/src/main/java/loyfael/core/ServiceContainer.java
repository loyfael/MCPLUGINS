package loyfael.core;

import loyfael.api.interfaces.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Conteneur de services principal implémentant l'inversion de dépendance
 * Responsabilité unique : gérer le cycle de vie des services
 */
public class ServiceContainer implements IServiceContainer {

    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Logger logger;
    private boolean initialized = false;

    public ServiceContainer(Logger logger) {
        this.logger = logger;
    }

    @Override
    public <T> void registerService(Class<T> serviceInterface, T implementation) {
        if (serviceInterface == null || implementation == null) {
            throw new IllegalArgumentException("Service interface et implementation ne peuvent pas être null");
        }

        services.put(serviceInterface, implementation);
        // Suppression du log d'enregistrement de service
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceInterface) {
        Object service = services.get(serviceInterface);
        if (service == null) {
            throw new IllegalStateException("Service non trouvé: " + serviceInterface.getSimpleName());
        }
        return (T) service;
    }

    @Override
    public boolean hasService(Class<?> serviceInterface) {
        return services.containsKey(serviceInterface);
    }

    @Override
    public void initializeServices() {
        try {
            initializeInOrder();
            initialized = true;
        } catch (Exception e) {
            logger.severe("Échec de l'initialisation des services: " + e.getMessage());
            throw new RuntimeException("Échec de l'initialisation des services", e);
        }
    }

    private void initializeInOrder() {
        // 1. Configuration en premier
        if (hasService(IConfigurationService.class)) {
            logger.info("[KrakenLevels] Initialisation du service de configuration...");
            getService(IConfigurationService.class).initialize();
        }

        // 2. Cache
        if (hasService(ICacheService.class)) {
            logger.info("[KrakenLevels] Initialisation du service de cache...");
            // Le cache n'a pas besoin d'initialisation particulière
        }

        // 3. Base de données (dépend de la configuration)
        if (hasService(IDatabaseService.class)) {
            logger.info("[KrakenLevels] Initialisation du service de base de données...");
            getService(IDatabaseService.class).initialize();
        }

        // 4. Services métier qui dépendent des services de base
        if (hasService(ILevelsConfigService.class)) {
            logger.info("[KrakenLevels] Service de configuration des niveaux déjà initialisé");
            // Le LevelsConfigService s'initialise dans son constructeur, pas besoin d'appeler initialize()
        }

        // 5. Service de synchronisation (dépend de la base de données et de la configuration)
        if (hasService(ISynchronizationService.class)) {
            logger.info("[KrakenLevels] Initialisation du service de synchronisation...");
            IConfigurationService configService = getService(IConfigurationService.class);
            boolean syncEnabled = configService.getConfig().getBoolean("synchronization.enabled", false);
            
            if (syncEnabled) {
                ISynchronizationService syncService = getService(ISynchronizationService.class);
                syncService.start();
                logger.info("[KrakenLevels] Service de synchronisation démarré");
            } else {
                logger.info("[KrakenLevels] Service de synchronisation désactivé dans la configuration");
            }
        }

        // Les autres services (PlayerService, NotificationService, etc.) n'ont pas besoin d'initialisation explicite
        logger.info("[KrakenLevels] Tous les services ont été initialisés avec succès");
    }

    @Override
    public void shutdownServices() {
        services.values().forEach(service -> {
            try {
                if (service instanceof ISynchronizationService) {
                    ((ISynchronizationService) service).stop();
                } else if (service instanceof IConfigurationService) {
                    ((IConfigurationService) service).shutdown();
                } else if (service instanceof IDatabaseService) {
                    ((IDatabaseService) service).disconnect();
                }
            } catch (Exception e) {
                logger.warning("Erreur lors de l'arrêt du service " + service.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });
    }

    @Override
    public String getServicesStatus() {
        StringBuilder status = new StringBuilder("État des services:\n");

        services.forEach((interfaceClass, implementation) -> {
            String serviceName = interfaceClass.getSimpleName();
            String implName = implementation.getClass().getSimpleName();
            status.append("- ").append(serviceName).append(" -> ").append(implName).append("\n");
        });

        return status.toString();
    }
}
