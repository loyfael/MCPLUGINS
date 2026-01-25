package loyfael.database;

import loyfael.Main;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages database connections and operations
 * Supports both SQLite and MySQL
 */
public class DatabaseManager {

    private final Main plugin;
    private Connection connection;
    private final String databaseType;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.databaseType = plugin.getConfigManager().getDatabaseType();
        initializeDatabase();
    }

    /**
     * Initialize database connection and create tables
     */
    private void initializeDatabase() {
        try {
            if (databaseType.equalsIgnoreCase("mysql")) {
                initializeMySQL();
            } else {
                initializeSQLite();
            }
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de l'initialisation de la base de données : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize MySQL connection
     */
    private void initializeMySQL() throws SQLException {
        String host = plugin.getConfigManager().getMySQLHost();
        int port = plugin.getConfigManager().getMySQLPort();
        String database = plugin.getConfigManager().getMySQLDatabase();
        String username = plugin.getConfigManager().getMySQLUsername();
        String password = plugin.getConfigManager().getMySQLPassword();

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
        connection = DriverManager.getConnection(url, username, password);
    }

    /**
     * Initialize SQLite connection
     */
    private void initializeSQLite() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String path = new File(dataFolder, plugin.getConfigManager().getSQLiteFile()).getAbsolutePath();
        String url = "jdbc:sqlite:" + path;
        connection = DriverManager.getConnection(url);
    }

    /**
     * Create necessary tables
     */
    private void createTables() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS player_modes (
                uuid VARCHAR(36) PRIMARY KEY,
                safe_mode BOOLEAN NOT NULL DEFAULT TRUE,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (PreparedStatement stmt = connection.prepareStatement(createTable)) {
            stmt.executeUpdate();
        }
    }

    /**
     * Get player's safe mode status
     */
    public boolean getPlayerMode(UUID uuid) {
        String query = "SELECT safe_mode FROM player_modes WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("safe_mode");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Erreur lors de la récupération du mode joueur : " + e.getMessage());
        }

        // Default to safe mode if not found or error
        return true;
    }

    /**
     * Set player's safe mode status
     */
    public void setPlayerMode(UUID uuid, boolean safeMode) {
        String query = """
            INSERT INTO player_modes (uuid, safe_mode) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE safe_mode = ?, last_updated = CURRENT_TIMESTAMP
            """;

        // For SQLite, use different syntax
        if (databaseType.equalsIgnoreCase("sqlite")) {
            query = """
                INSERT OR REPLACE INTO player_modes (uuid, safe_mode) VALUES (?, ?)
                """;
        }

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.setBoolean(2, safeMode);

            if (databaseType.equalsIgnoreCase("mysql")) {
                stmt.setBoolean(3, safeMode);
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Erreur lors de la sauvegarde du mode joueur : " + e.getMessage());
        }
    }

    /**
     * Close database connection properly
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                // Vérifier si la connexion est encore active avant de fermer
                if (!connection.isClosed()) {
                    plugin.getLogger().info("§7Fermeture de la connexion à la base de données...");

                    // Forcer la validation des transactions en attente
                    if (!connection.getAutoCommit()) {
                        connection.commit();
                    }

                    // Fermer la connexion avec un timeout
                    connection.close();
                    plugin.getLogger().info("§a✓ Connexion à la base de données fermée proprement");
                } else {
                    plugin.getLogger().info("§7Connexion à la base de données déjà fermée");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("§cErreur lors de la fermeture de la base de données : " + e.getMessage());
                e.printStackTrace();

                // Forcer la fermeture si possible
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException ignored) {
                    // Ignore les erreurs lors de la fermeture forcée
                }
            } finally {
                connection = null;
            }
        } else {
            plugin.getLogger().info("§7Aucune connexion à la base de données à fermer");
        }
    }

    /**
     * Check if connection is valid
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Reconnect to database if needed
     */
    public void reconnect() {
        closeConnection();
        initializeDatabase();
    }
}
