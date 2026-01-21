package loyfael.core.services;

import loyfael.api.interfaces.IConfigurationService;
import loyfael.utils.Utils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MongoDB implementation of the database service
 * Liskov substitution principle: can replace AbstractDatabaseService
 */
public class MongoDatabaseService extends AbstractDatabaseService {

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    // Store last used configuration values for diagnostics & reload detection
    private String lastHost;
    private int lastPort;
    private String lastUsername;
    private String lastDatabaseName;

    public MongoDatabaseService(IConfigurationService configService) {
        super(configService);
    }

    @Override
    protected boolean doInitialize() {
        try {
            // Read MongoDB configuration from config.yml
            String host = configService.getConfig().getString("mongodb.host", "localhost");
            int port = configService.getConfig().getInt("mongodb.port", 27017);
            String username = configService.getConfig().getString("mongodb.username", "");
            String password = configService.getConfig().getString("mongodb.password", "");
            String databaseName = configService.getConfig().getString("mongodb.database", "krakenlevels");

            // Construire la chaîne de connexion avec paramètres optimisés pour réduire le trafic
            String connectionString;
            if (username.isEmpty() || password.isEmpty()) {
                connectionString = "mongodb://" + host + ":" + port + "/" + databaseName +
                    "?maxPoolSize=3&minPoolSize=1&maxIdleTimeMS=600000&serverSelectionTimeoutMS=5000&connectTimeoutMS=10000";
            } else {
                // URL-encode special characters in username and password
                String encodedUsername = urlEncode(username);
                String encodedPassword = urlEncode(password);
                connectionString = "mongodb://" + encodedUsername + ":" + encodedPassword + "@" + host + ":" + port + "/" + databaseName +
                    "?authSource=admin&maxPoolSize=3&minPoolSize=1&maxIdleTimeMS=600000&serverSelectionTimeoutMS=5000&connectTimeoutMS=10000";
            }

            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(databaseName);
            collection = database.getCollection("playerdata");

            // One-time connection test
            database.runCommand(new Document("ping", 1));
            // Memorize configuration used
            lastHost = host;
            lastPort = port;
            lastUsername = username;
            lastDatabaseName = databaseName;

            // Log sanitized connection info for diagnostics
            String maskedUser = (username == null || username.isEmpty()) ? "(anonymous)" : username;
            Utils.sendConsoleLog("&aMongoDB connected successfully &7[host=" + host + ":" + port + ", db=" + databaseName + ", user=" + maskedUser + "]");
            return true;

        } catch (Exception e) {
            Utils.sendConsoleLog("&cMongoDB connection error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * URL-encodes a string for use in a MongoDB URL
     */
    private String urlEncode(String input) {
        try {
            return java.net.URLEncoder.encode(input, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 is always supported
            return input;
        }
    }

    @Override
    protected void doDisconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            collection = null;
        }
    }

    @Override
    public void saveData(String key, Object value) {
        validateKey(key);
        ensureConnected();

        try {
            Document filter = new Document("_id", key);
            
            // Retrieve server name from configuration for synchronization
            String serverName = configService.getConfig().getString("server.name", "unknown-server");
            long currentTime = System.currentTimeMillis();
            
            // Create metadata for cross-server synchronization
            Document metadata = new Document()
                .append("lastModified", currentTime)
                .append("lastModifiedBy", serverName)
                .append("version", 1);

            Document document = new Document("_id", key)
                .append("data", value)
                .append("lastUpdated", currentTime)
                .append("metadata", metadata); // Add synchronization metadata

            collection.replaceOne(filter, document,
                new com.mongodb.client.model.ReplaceOptions().upsert(true));

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError while saving to MongoDB: " + e.getMessage());
        }
    }

    @Override
    public Optional<Object> getData(String key) {
        validateKey(key);
        ensureConnected();

        try {
            Document filter = new Document("_id", key);
            Document result = collection.find(filter).first();

            if (result != null && result.containsKey("data")) {
                return Optional.of(result.get("data"));
            }

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError while fetching from MongoDB: " + e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public boolean deleteData(String key) {
        validateKey(key);
        ensureConnected();

        try {
            Document filter = new Document("_id", key);
            return collection.deleteOne(filter).getDeletedCount() > 0;

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError while deleting in MongoDB: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        ensureConnected();

        try {
            Document filter = new Document("_id", key);
            return collection.countDocuments(filter) > 0;

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError while checking existence in MongoDB: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getDataByPrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null");
        }
        ensureConnected();

        Map<String, Object> results = new HashMap<>();

        try {
            Document filter = new Document("_id",
                new Document("$regex", "^" + prefix).append("$options", "i"));

            collection.find(filter).forEach(doc -> {
                String id = doc.getString("_id");
                Object data = doc.get("data");
                if (id != null && data != null) {
                    results.put(id, data);
                }
            });

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError while fetching by prefix in MongoDB: " + e.getMessage());
        }

        return results;
    }

    @Override
    public void backup() {
        ensureConnected();
        // MongoDB backups are generally handled server-side
        // No backup logs here
    }

    // Exposed for reload diagnostics
    public String getLastHost() { return lastHost; }
    public int getLastPort() { return lastPort; }
    public String getLastUsername() { return lastUsername; }
    public String getLastDatabaseName() { return lastDatabaseName; }
}
