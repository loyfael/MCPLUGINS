package loyfael.core.services;

import loyfael.api.interfaces.IConfigurationService;
import loyfael.api.interfaces.IMongoConnectionManager;
import loyfael.utils.Utils;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MongoDB implementation of the database service
 * Liskov substitution principle: can replace AbstractDatabaseService
 * Uses shared MongoDB connection manager
 */
public class MongoDatabaseService extends AbstractDatabaseService {

    private final IMongoConnectionManager connectionManager;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    public MongoDatabaseService(IConfigurationService configService, IMongoConnectionManager connectionManager) {
        super(configService);
        this.connectionManager = connectionManager;
    }

    @Override
    protected boolean doInitialize() {
        try {
            // Connection is already initialized by the connection manager
            if (!connectionManager.isConnected()) {
                Utils.sendConsoleLog("&cMongoDB connection manager not initialized");
                return false;
            }

            // Get database from shared connection
            String databaseName = configService.getConfig().getString("mongodb.database", "krakenlevels");
            database = connectionManager.getDatabase(databaseName);
            collection = database.getCollection("playerdata");

            return true;

        } catch (Exception e) {
            Utils.sendConsoleLog("&cMongoDB initialization error: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void doDisconnect() {
        // Connection manager handles closing the shared connection
        // This service just releases its local references
        database = null;
        collection = null;
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
}
