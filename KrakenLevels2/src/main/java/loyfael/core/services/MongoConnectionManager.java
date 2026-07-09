package loyfael.core.services;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import loyfael.api.interfaces.IConfigurationService;
import loyfael.api.interfaces.IMongoConnectionManager;
import loyfael.core.mongodb.MongoConfig;
import loyfael.core.mongodb.MongoDriverLogging;
import loyfael.core.mongodb.MongoExceptionHandler;
import loyfael.core.mongodb.MongoLogger;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

/**
 * Manages the single shared {@link MongoClient} instance for the entire plugin JVM.
 * Responsible for configuration loading, connection testing, authentication validation,
 * and clean shutdown.
 */
public class MongoConnectionManager implements IMongoConnectionManager {

    private final IConfigurationService configService;

    private MongoClient mongoClient;
    private MongoConfig mongoConfig;
    private volatile boolean connected;

    public MongoConnectionManager(IConfigurationService configService) {
        this.configService = configService;
    }

    @Override
    public synchronized void initialize() throws Exception {
        if (connected && mongoClient != null) {
            MongoLogger.debug("MongoDB déjà connecté, initialisation ignorée.");
            return;
        }

        closeClientSilently();

        MongoLogger.info("Initialisation de MongoDB...");
        mongoConfig = MongoConfig.from(configService);

        MongoLogger.setDebug(mongoConfig.isDebug());
        MongoDriverLogging.configure(mongoConfig.isDebug());

        try {
            MongoClientSettings settings = buildClientSettings(mongoConfig);
            mongoClient = MongoClients.create(settings);

            MongoLogger.debug("MongoClient créé, test de connexion en cours...");
            verifyConnection(mongoConfig.getDatabaseName());

            connected = true;
            MongoLogger.info("MongoDB connecté (" + mongoConfig.getConnectionLabel() + ").");

        } catch (Exception exception) {
            connected = false;
            closeClientSilently();
            MongoExceptionHandler.logFailure(exception);
            throw exception;
        }
    }

    @Override
    public synchronized void reconnect() throws Exception {
        disconnect();
        initialize();
    }

    private MongoClientSettings buildClientSettings(MongoConfig config) {
        ConnectionString connectionString = config.toConnectionString();
        MongoCredential credential = config.getCredential();

        MongoLogger.debug("Mode de configuration : " + config.getMode());
        MongoLogger.debug("Base de données : " + config.getDatabaseName());
        MongoLogger.debug("Authentification : " + (credential != null ? "configurée" : "aucune"));

        MongoClientSettings.Builder builder = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .applyToClusterSettings(cluster -> cluster
                .serverSelectionTimeout(config.getServerSelectionTimeoutMs(), TimeUnit.MILLISECONDS))
            .applyToSocketSettings(socket -> socket
                .connectTimeout(config.getConnectTimeoutMs(), TimeUnit.MILLISECONDS));

        if (credential != null) {
            builder.credential(credential);
        }

        return builder.build();
    }

    /**
     * Verifies server reachability, authentication, and database access via ping.
     */
    private void verifyConnection(String databaseName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        database.runCommand(new Document("ping", 1));
        MongoLogger.debug("Commande ping réussie sur la base '" + databaseName + "'.");
    }

    @Override
    public MongoClient getClient() {
        if (!connected || mongoClient == null) {
            throw new IllegalStateException("Connexion MongoDB non initialisée ou fermée");
        }
        return mongoClient;
    }

    @Override
    public MongoDatabase getDatabase(String databaseName) {
        return getClient().getDatabase(databaseName);
    }

    @Override
    public String getDatabaseName() {
        if (mongoConfig == null) {
            return configService.getConfig().getString("mongodb.database", "krakenlevels");
        }
        return mongoConfig.getDatabaseName();
    }

    @Override
    public boolean isConnected() {
        if (!connected || mongoClient == null) {
            return false;
        }
        try {
            verifyConnection(getDatabaseName());
            return true;
        } catch (Exception exception) {
            MongoLogger.debug("Vérification de connexion échouée", exception);
            return false;
        }
    }

    @Override
    public synchronized void disconnect() {
        if (mongoClient != null) {
            MongoLogger.debug("Fermeture du MongoClient...");
            closeClientSilently();
        }
        connected = false;
        mongoConfig = null;
    }

    private void closeClientSilently() {
        if (mongoClient == null) {
            return;
        }
        try {
            mongoClient.close();
        } catch (Exception exception) {
            MongoLogger.debug("Erreur lors de la fermeture du client", exception);
        } finally {
            mongoClient = null;
        }
    }
}
