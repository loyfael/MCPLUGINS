package loyfael.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import loyfael.Main;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MongoManager {

    private final Main plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> shopsCollection;
    private MongoCollection<Document> transactionsCollection;
    private final ExecutorService executor;

    public MongoManager(Main plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(4);
    }

    public boolean connect() {
        try {
            String connectionString = plugin.getConfigManager().getMongoConnectionString();
            String databaseName = plugin.getConfigManager().getMongoDatabaseName();

            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(databaseName);

            shopsCollection = database.getCollection("shops");
            transactionsCollection = database.getCollection("transactions");

            createIndexes();

            plugin.getLogger().info("Connexion MongoDB établie avec succès!");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la connexion MongoDB: " + e.getMessage());
            return false;
        }
    }

    private void createIndexes() {
        CompletableFuture.runAsync(() -> {
            try {
                // Index pour les recherches par matériau d'item
                shopsCollection.createIndex(
                    Indexes.ascending("item.material"),
                    new IndexOptions().background(true)
                );

                // Index pour les recherches par prix
                shopsCollection.createIndex(
                    Indexes.ascending("price"),
                    new IndexOptions().background(true)
                );

                // Index pour les recherches par propriétaire
                shopsCollection.createIndex(
                    Indexes.ascending("ownerUUID"),
                    new IndexOptions().background(true)
                );

                // Index composé pour recherches multi-critères (syntaxe corrigée)
                shopsCollection.createIndex(
                    new org.bson.Document()
                        .append("item.material", 1)
                        .append("price", 1)
                        .append("type", 1),
                    new IndexOptions().background(true)
                );

                // Index pour localisation géographique des shops (syntaxe corrigée)
                shopsCollection.createIndex(
                    new org.bson.Document()
                        .append("world", 1)
                        .append("x", 1)
                        .append("y", 1)
                        .append("z", 1),
                    new IndexOptions().background(true)
                );

                // Index pour les transactions par date
                transactionsCollection.createIndex(
                    Indexes.descending("date"),
                    new IndexOptions().background(true)
                );

                // Index pour les transactions par shop
                transactionsCollection.createIndex(
                    Indexes.ascending("shopId"),
                    new IndexOptions().background(true)
                );

                plugin.getLogger().info("Index MongoDB créés avec succès!");

            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de la création des index: " + e.getMessage());
            }
        }, executor);
    }

    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            plugin.getLogger().info("Connexion MongoDB fermée.");
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    // Exécution asynchrone des opérations
    public CompletableFuture<Void> executeAsync(@NotNull Runnable operation) {
        return CompletableFuture.runAsync(operation, executor);
    }

    public <T> CompletableFuture<T> executeAsync(@NotNull java.util.function.Supplier<T> operation) {
        return CompletableFuture.supplyAsync(operation, executor);
    }

    // Getters
    public MongoCollection<Document> getShopsCollection() {
        return shopsCollection;
    }

    public MongoCollection<Document> getTransactionsCollection() {
        return transactionsCollection;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
