package loyfael.core.services;

import loyfael.api.interfaces.IDatabaseService;
import loyfael.api.interfaces.IConfigurationService;
import loyfael.utils.Utils;

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

            Utils.sendConsoleLog("&6Initializing database service...");
            connected = doInitialize();

            if (connected) {
                Utils.sendConsoleLog("&aDatabase service initialized successfully.");
            } else {
                Utils.sendConsoleLog("&cFailed to initialize the database service.");
            }

            return connected;

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError during database initialization: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    @Override
    public final void disconnect() {
        if (connected) {
            try {
                doDisconnect();
                connected = false;
                Utils.sendConsoleLog("&eDatabase connection closed.");
            } catch (Exception e) {
                Utils.sendConsoleLog("&cError while closing the database: " + e.getMessage());
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
