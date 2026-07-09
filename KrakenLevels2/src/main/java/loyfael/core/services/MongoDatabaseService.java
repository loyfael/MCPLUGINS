package loyfael.core.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import loyfael.api.interfaces.IConfigurationService;
import loyfael.api.interfaces.IMongoConnectionManager;
import loyfael.core.mongodb.MongoExceptionHandler;
import loyfael.core.mongodb.MongoLogger;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MongoDB implementation of the database service.
 * Uses the shared {@link IMongoConnectionManager} — never creates its own client.
 */
public class MongoDatabaseService extends AbstractDatabaseService {

    private static final String COLLECTION_NAME = "playerdata";

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
            if (!connectionManager.isConnected()) {
                MongoLogger.error("Le gestionnaire de connexion MongoDB n'est pas disponible.");
                return false;
            }

            database = connectionManager.getDatabase(connectionManager.getDatabaseName());
            collection = database.getCollection(COLLECTION_NAME);
            return true;

        } catch (Exception exception) {
            MongoExceptionHandler.logFailure(exception);
            return false;
        }
    }

    @Override
    protected void doDisconnect() {
        database = null;
        collection = null;
    }

    @Override
    public void saveData(String key, Object value) {
        validateKey(key);
        ensureConnected();

        try {
            Document filter = new Document("_id", key);
            String serverName = configService.getConfig().getString("server.name", "unknown-server");
            long currentTime = System.currentTimeMillis();

            Document metadata = new Document()
                .append("lastModified", currentTime)
                .append("lastModifiedBy", serverName)
                .append("version", 1);

            Document document = new Document("_id", key)
                .append("data", value)
                .append("lastUpdated", currentTime)
                .append("metadata", metadata);

            collection.replaceOne(filter, document,
                new com.mongodb.client.model.ReplaceOptions().upsert(true));

        } catch (Exception exception) {
            MongoLogger.error("Erreur lors de la sauvegarde MongoDB : "
                + MongoExceptionHandler.toUserMessage(exception));
            MongoLogger.debug("Détail sauvegarde", exception);
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

        } catch (Exception exception) {
            MongoLogger.error("Erreur lors de la lecture MongoDB : "
                + MongoExceptionHandler.toUserMessage(exception));
            MongoLogger.debug("Détail lecture", exception);
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

        } catch (Exception exception) {
            MongoLogger.error("Erreur lors de la suppression MongoDB : "
                + MongoExceptionHandler.toUserMessage(exception));
            MongoLogger.debug("Détail suppression", exception);
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

        } catch (Exception exception) {
            MongoLogger.error("Erreur lors de la vérification MongoDB : "
                + MongoExceptionHandler.toUserMessage(exception));
            MongoLogger.debug("Détail existence", exception);
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

        } catch (Exception exception) {
            MongoLogger.error("Erreur lors de la recherche MongoDB : "
                + MongoExceptionHandler.toUserMessage(exception));
            MongoLogger.debug("Détail recherche par préfixe", exception);
        }

        return results;
    }

    @Override
    public void backup() {
        ensureConnected();
    }
}
