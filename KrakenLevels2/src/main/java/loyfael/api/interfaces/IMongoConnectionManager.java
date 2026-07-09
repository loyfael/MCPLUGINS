package loyfael.api.interfaces;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * Interface for shared MongoDB connection management.
 * A single {@link MongoClient} instance must be used across the entire plugin.
 */
public interface IMongoConnectionManager {

    /**
     * Loads configuration, creates the client, and verifies connectivity and authentication.
     *
     * @throws Exception if connection, authentication, or database access fails
     */
    void initialize() throws Exception;

    /**
     * Disconnects and re-initializes with the current configuration.
     *
     * @throws Exception if reconnection fails
     */
    void reconnect() throws Exception;

    /**
     * Returns the shared {@link MongoClient} instance.
     *
     * @throws IllegalStateException if not connected
     */
    MongoClient getClient();

    /**
     * Returns a handle to the requested database.
     */
    MongoDatabase getDatabase(String databaseName);

    /**
     * Returns the configured default database name.
     */
    String getDatabaseName();

    /**
     * Checks whether the connection is alive by running a ping command.
     */
    boolean isConnected();

    /**
     * Closes the shared client and releases all resources.
     */
    void disconnect();
}
