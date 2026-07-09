package loyfael.core;

import loyfael.api.interfaces.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Conteneur de services principal implémentant l'inversion de dépendance.
 * Responsabilité unique : gérer le cycle de vie des services.
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

    private void initializeInOrder() throws Exception {
        if (hasService(IConfigurationService.class)) {
            getService(IConfigurationService.class).initialize();
        }

        if (hasService(IMongoConnectionManager.class)) {
            getService(IMongoConnectionManager.class).initialize();
        }

        if (hasService(IDatabaseService.class)) {
            boolean dbReady = getService(IDatabaseService.class).initialize();
            if (!dbReady && hasService(IMongoConnectionManager.class)) {
                throw new RuntimeException("Impossible d'initialiser le service de base de données MongoDB");
            }
        }

        if (hasService(ISynchronizationService.class)) {
            IConfigurationService configService = getService(IConfigurationService.class);
            if (configService.getConfig().getBoolean("synchronization.enabled", false)) {
                getService(ISynchronizationService.class).start();
            }
        }
    }

    @Override
    public void shutdownServices() {
        shutdownIfPresent(ISynchronizationService.class, service -> ((ISynchronizationService) service).stop());
        shutdownIfPresent(IDatabaseService.class, service -> ((IDatabaseService) service).disconnect());
        shutdownIfPresent(IMongoConnectionManager.class, service -> ((IMongoConnectionManager) service).disconnect());
        shutdownIfPresent(IConfigurationService.class, service -> ((IConfigurationService) service).shutdown());
    }

    private void shutdownIfPresent(Class<?> serviceClass, ServiceShutdown action) {
        Object service = services.get(serviceClass);
        if (service == null) {
            return;
        }
        try {
            action.run(service);
        } catch (Exception e) {
            logger.warning("Erreur lors de l'arrêt du service "
                + service.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Reconnects MongoDB using the current configuration (used by /levels reload).
     */
    public void reconnectMongoDB() throws Exception {
        if (!hasService(IMongoConnectionManager.class)) {
            return;
        }

        IMongoConnectionManager connectionManager = getService(IMongoConnectionManager.class);
        connectionManager.reconnect();

        if (hasService(IDatabaseService.class)) {
            IDatabaseService databaseService = getService(IDatabaseService.class);
            databaseService.disconnect();
            if (!databaseService.initialize()) {
                throw new RuntimeException("Impossible de réinitialiser le service de base de données MongoDB");
            }
        }
    }

    @Override
    public String getServicesStatus() {
        StringBuilder status = new StringBuilder("État des services:\n");
        services.forEach((interfaceClass, implementation) ->
            status.append("- ")
                .append(interfaceClass.getSimpleName())
                .append(" -> ")
                .append(implementation.getClass().getSimpleName())
                .append("\n"));
        return status.toString();
    }

    @FunctionalInterface
    private interface ServiceShutdown {
        void run(Object service) throws Exception;
    }
}
