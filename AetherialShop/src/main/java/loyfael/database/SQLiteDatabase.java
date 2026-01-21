package loyfael.database;

import loyfael.model.ShopItem;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLiteDatabase {
    
    private final JavaPlugin plugin;
    private final Logger logger;
    private final String dbPath;
    private Connection connection;
    
    public SQLiteDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dbPath = plugin.getDataFolder().getAbsolutePath() + File.separator + 
                     plugin.getConfig().getString("database.file", "shop_data.db");
        
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try {
            // Créer le dossier s'il n'existe pas
            File dbFile = new File(dbPath);
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            
            // Créer la connexion
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            // Créer les tables
            createTables();
            
            logger.info("✓ Base de données SQLite initialisée: " + dbFile.getName());
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'initialisation de la base de données", e);
        }
    }
    
    private void createTables() throws SQLException {
        String createShopRotationsTable = """
            CREATE TABLE IF NOT EXISTS shop_rotations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL UNIQUE,
                items_count INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createShopItemsTable = """
            CREATE TABLE IF NOT EXISTS shop_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rotation_date TEXT NOT NULL,
                material TEXT NOT NULL,
                name TEXT,
                min_price INTEGER NOT NULL,
                max_price INTEGER NOT NULL,
                current_price INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                max_stock INTEGER NOT NULL,
                current_stock INTEGER NOT NULL,
                FOREIGN KEY (rotation_date) REFERENCES shop_rotations(date)
            )
        """;
        
        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_name TEXT NOT NULL,
                player_uuid TEXT NOT NULL,
                item_material TEXT NOT NULL,
                item_name TEXT,
                quantity INTEGER NOT NULL,
                price_per_unit REAL NOT NULL,
                total_amount REAL NOT NULL,
                transaction_type TEXT NOT NULL CHECK (transaction_type IN ('BUY', 'SELL')),
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createShopRotationsTable);
            stmt.execute(createShopItemsTable);
            stmt.execute(createTransactionsTable);
            logger.info("✓ Tables de base de données créées/vérifiées");
        }
    }
    
    public void saveRotation(String date, List<ShopItem> items) {
        try {
            connection.setAutoCommit(false);
            
            // Sauvegarder la rotation
            String insertRotation = "INSERT OR REPLACE INTO shop_rotations (date, items_count) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertRotation)) {
                pstmt.setString(1, date);
                pstmt.setInt(2, items.size());
                pstmt.executeUpdate();
            }
            
            // Supprimer les anciens items de cette date
            String deleteOldItems = "DELETE FROM shop_items WHERE rotation_date = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteOldItems)) {
                pstmt.setString(1, date);
                pstmt.executeUpdate();
            }
            
            // Sauvegarder les nouveaux items
            String insertItem = """
                INSERT INTO shop_items (rotation_date, material, name, min_price, max_price, 
                                      current_price, amount, max_stock, current_stock) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement pstmt = connection.prepareStatement(insertItem)) {
                for (ShopItem item : items) {
                    pstmt.setString(1, date);
                    pstmt.setString(2, item.getMaterial().name());
                    pstmt.setString(3, item.getName());
                    pstmt.setInt(4, item.getMinPrice());
                    pstmt.setInt(5, item.getMaxPrice());
                    pstmt.setInt(6, item.getCurrentPrice());
                    pstmt.setInt(7, item.getAmount());
                    pstmt.setInt(8, item.getMaxStock());
                    pstmt.setInt(9, item.getCurrentStock());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            connection.commit();
            logger.info("✓ Rotation sauvegardée en base: " + date + " (" + items.size() + " items)");
            
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.log(Level.SEVERE, "Erreur lors du rollback", rollbackEx);
            }
            logger.log(Level.SEVERE, "Erreur lors de la sauvegarde de la rotation", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Erreur lors de la réactivation de l'autocommit", e);
            }
        }
    }
    
    public void logTransaction(String playerName, String playerUuid, ShopItem item, 
                              int quantity, double pricePerUnit, double totalAmount, 
                              String transactionType) {
        String insertTransaction = """
            INSERT INTO transactions (player_name, player_uuid, item_material, item_name, 
                                    quantity, price_per_unit, total_amount, transaction_type) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertTransaction)) {
            pstmt.setString(1, playerName);
            pstmt.setString(2, playerUuid);
            pstmt.setString(3, item.getMaterial().name());
            pstmt.setString(4, item.getName());
            pstmt.setInt(5, quantity);
            pstmt.setDouble(6, pricePerUnit);
            pstmt.setDouble(7, totalAmount);
            pstmt.setString(8, transactionType);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Erreur lors de l'enregistrement de la transaction", e);
        }
    }
    
    public List<ShopItem> loadRotation(String date) {
        List<ShopItem> items = new ArrayList<>();
        String query = """
            SELECT material, name, min_price, max_price, current_price, 
                   amount, max_stock, current_stock 
            FROM shop_items 
            WHERE rotation_date = ?
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, date);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Reconstruction des ShopItems depuis la base
                    // Note: Cette méthode nécessiterait un constructeur adapté dans ShopItem
                    // Pour l'instant, retourner une liste vide et utiliser le système existant
                    logger.info("Item trouvé en base: " + rs.getString("name"));
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Erreur lors du chargement de la rotation", e);
        }
        
        return items;
    }
    
    public boolean hasRotationForDate(String date) {
        String query = "SELECT COUNT(*) FROM shop_rotations WHERE date = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, date);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Erreur lors de la vérification de la rotation", e);
        }
        
        return false;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("✓ Connexion base de données fermée");
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Erreur lors de la fermeture de la base", e);
        }
    }
}
