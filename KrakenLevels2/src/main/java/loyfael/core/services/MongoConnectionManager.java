package loyfael.core.services;

import loyfael.api.interfaces.IMongoConnectionManager;
import loyfael.api.interfaces.IConfigurationService;
import loyfael.utils.Utils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import org.bson.Document;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages shared MongoDB connection for all services
 * Single responsibility: MongoDB connection lifecycle
 * Singleton per JVM instance
 */
public class MongoConnectionManager implements IMongoConnectionManager {

    private final IConfigurationService configService;
    private MongoClient mongoClient;
    private volatile boolean isConnected = false;
    private static final int CONNECTION_TEST_TIMEOUT = 5000;

    // Suppress MongoDB driver logging
    static {
        Logger mongoLogger = Logger.getLogger("org.mongodb");
        mongoLogger.setLevel(Level.OFF);
        Logger mongoDriverLogger = Logger.getLogger("com.mongodb");
        mongoDriverLogger.setLevel(Level.OFF);
    }

    public MongoConnectionManager(IConfigurationService configService) {
        this.configService = configService;
    }

    @Override
    public void initialize() throws Exception {
        if (isConnected) {
            Utils.sendConsoleLog("&7[MongoDB] Déjà connecté");
            return;
        }

        try {
            // Build connection string with proper credential handling
            String connectionString = buildConnectionString();
            
            // Create MongoDB client settings with proper credentials
            MongoClientSettings settings = buildMongoClientSettings(connectionString);
            
            // Create the shared MongoDB client
            this.mongoClient = MongoClients.create(settings);
            
            // Extract database name for testing
            String databaseName = configService.getConfig().getString("mongodb.database", "krakenlevels");
            
            // Test connection with authentication
            testConnection(databaseName);
            
            isConnected = true;
            logConnectionSuccess(connectionString);
            
        } catch (Exception e) {
            isConnected = false;
            logConnectionFailure(e);
            throw e;
        }
    }

    /**
     * Build MongoDB connection string from configuration
     */
    private String buildConnectionString() {
        // Try to get URI from config first
        String uri = configService.getConfig().getString("mongodb.uri", "");
        
        if (!uri.isEmpty()) {
            Utils.sendConsoleLog("&7[MongoDB] Utilisation de l'URI MongoDB configurée");
            return uri;
        }
        
        // Fall back to building from individual parameters
        String host = configService.getConfig().getString("mongodb.host", "localhost");
        int port = configService.getConfig().getInt("mongodb.port", 27017);
        String username = configService.getConfig().getString("mongodb.username", "");
        String password = configService.getConfig().getString("mongodb.password", "");
        String authSource = configService.getConfig().getString("mongodb.auth-source", "admin");
        String databaseName = configService.getConfig().getString("mongodb.database", "krakenlevels");

        // Build connection string with proper URL encoding and parameters
        if (username.isEmpty() || password.isEmpty()) {
            return String.format("mongodb://%s:%d/%s?serverSelectionTimeoutMS=%d&connectTimeoutMS=%d",
                host, port, databaseName, CONNECTION_TEST_TIMEOUT, CONNECTION_TEST_TIMEOUT);
        } else {
            String encodedUsername = urlEncode(username);
            String encodedPassword = urlEncode(password);
            return String.format("mongodb://%s:%s@%s:%d/%s?authSource=%s&serverSelectionTimeoutMS=%d&connectTimeoutMS=%d",
                encodedUsername, encodedPassword, host, port, databaseName, authSource, 
                CONNECTION_TEST_TIMEOUT, CONNECTION_TEST_TIMEOUT);
        }
    }

    /**
     * Build MongoClientSettings ensuring credentials are properly set
     */
    private MongoClientSettings buildMongoClientSettings(String connectionString) {
        try {
            // Parse connection string to extract credentials and other settings
            ConnectionString connStr = new ConnectionString(connectionString);
            
            // Builder will automatically extract credentials from ConnectionString
            // The ConnectionString already contains all necessary parameters
            MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyConnectionString(connStr);
            
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la construction des paramètres MongoDB", e);
        }
    }

    /**
     * Test connection with actual authentication
     */
    private void testConnection(String databaseName) throws Exception {
        try {
            if (mongoClient == null) {
                throw new IllegalStateException("MongoClient not initialized");
            }
            
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            
            // Run ping command to test authentication
            // This will fail if credentials are invalid
            database.runCommand(new Document("ping", 1));
            
        } catch (Exception e) {
            if (mongoClient != null) {
                try {
                    mongoClient.close();
                } catch (Exception ignored) {
                }
            }
            mongoClient = null;
            throw new RuntimeException("Erreur lors du test de connexion MongoDB: " + e.getMessage(), e);
        }
    }

    /**
     * Log connection success
     */
    private void logConnectionSuccess(String connectionString) {
        try {
            String databaseName = configService.getConfig().getString("mongodb.database", "krakenlevels");
            String host = configService.getConfig().getString("mongodb.host", "localhost");
            int port = configService.getConfig().getInt("mongodb.port", 27017);
            String username = configService.getConfig().getString("mongodb.username", "");
            
            String auth = username.isEmpty() ? "anonyme" : "✓ authentifié";
            Utils.sendConsoleLog("&a[KrakenLevels] Connecté à MongoDB (&e" + host + ":" + port + " &a/ &e" + 
                databaseName + " &a/ &e" + auth + "&a)");
        } catch (Exception e) {
            Utils.sendConsoleLog("&a[KrakenLevels] Connecté à MongoDB");
        }
    }

    /**
     * Log connection failure
     */
    private void logConnectionFailure(Exception e) {
        String cause = e.getMessage();
        if (cause == null) {
            cause = e.getClass().getSimpleName();
        }
        
        // Shorten common error messages
        if (cause.contains("Unauthorized")) {
            cause = "Authentification échouée (identifiants invalides)";
        } else if (cause.contains("connect")) {
            cause = "Impossible de se connecter au serveur";
        } else if (cause.contains("timeout")) {
            cause = "Délai d'expiration dépassé";
        }
        
        Utils.sendConsoleLog("&c[KrakenLevels] Impossible de se connecter à MongoDB.");
        Utils.sendConsoleLog("&cCause : " + cause);
    }

    /**
     * URL-encode a string for use in MongoDB URI
     */
    private String urlEncode(String input) {
        try {
            return java.net.URLEncoder.encode(input, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return input;
        }
    }

    @Override
    public MongoClient getClient() {
        if (!isConnected || mongoClient == null) {
            throw new IllegalStateException("MongoDB connection not initialized or already closed");
        }
        return mongoClient;
    }

    @Override
    public MongoDatabase getDatabase(String databaseName) {
        return getClient().getDatabase(databaseName);
    }

    @Override
    public boolean isConnected() {
        if (!isConnected || mongoClient == null) {
            return false;
        }
        
        try {
            // Quick check - try to get admin database
            mongoClient.getDatabase("admin");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                isConnected = false;
                Utils.sendConsoleLog("&7[MongoDB] Déconnecté");
            } catch (Exception e) {
                Utils.sendConsoleLog("&c[MongoDB] Erreur lors de la déconnexion: " + e.getMessage());
            } finally {
                mongoClient = null;
            }
        }
    }
}
