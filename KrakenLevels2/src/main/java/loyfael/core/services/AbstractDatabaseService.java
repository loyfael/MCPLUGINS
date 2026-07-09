package loyfael.core.services;

import loyfael.api.interfaces.IDatabaseService;
import loyfael.api.interfaces.IConfigurationService;

import java.util.Map;
import java.util.Optional;

/**
 * Service de base de données abstrait appliquant le principe d'ouverture/fermeture
 * Principe de substitution de Liskov : les implémentations peuvent être échangées
 */
public abstract class AbstractDatabaseService implements IDatabaseService {

    protected final IConfigurationService configService;
    protected boolean connected = false;

    protected AbstractDatabaseService(IConfigurationService configService) {
        this.configService = configService;
    }

    @Override
    public final boolean initialize() {
        try {
            if (connected) {
                return true;
            }

            connected = doInitialize();
            return connected;

        } catch (Exception e) {
            connected = false;
            return false;
        }
    }

    @Override
    public final void disconnect() {
        if (connected) {
            try {
                doDisconnect();
            } catch (Exception ignored) {
            } finally {
                connected = false;
            }
        }
    }

    @Override
    public final boolean isConnected() {
        return connected;
    }

    // Méthodes abstraites à implémenter par les classes concrètes
    protected abstract boolean doInitialize();
    protected abstract void doDisconnect();

    // Default implementation for parameter validation
    protected final void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
    }

    protected final void ensureConnected() {
        if (!connected) {
            throw new IllegalStateException("Database service not connected");
        }
    }
}
