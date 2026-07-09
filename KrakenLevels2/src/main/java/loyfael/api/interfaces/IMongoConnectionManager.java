package loyfael.api.interfaces;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * Interface for shared MongoDB connection management
 * Single responsibility: MongoDB connection lifecycle
 */
public interface IMongoConnectionManager {
    
    /**
     * Initialize the MongoDB connection with configuration
     * @throws Exception if connection fails
     */
    void initialize() throws Exception;
    
    /**
     * Get the shared MongoClient instance
     * @return MongoDB client, or null if not initialized
     */
    MongoClient getClient();
    
    /**
     * Get a specific database
     * @param databaseName name of the database
     * @return MongoDB database instance
     */
    MongoDatabase getDatabase(String databaseName);
    
    /**
     * Check if connection is established and authenticated
     * @return true if connected and authenticated
     */
    boolean isConnected();
    
    /**
     * Disconnect and close the MongoDB client
     */
    void disconnect();
}
