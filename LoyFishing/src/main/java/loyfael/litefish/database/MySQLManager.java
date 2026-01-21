package loyfael.litefish.database;

import loyfael.litefish.LiteFish;
import loyfael.litefish.managers.PlayerDataManager;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.Map;

/**
 * Manages MySQL database connections and operations
 */
public class MySQLManager {
    
    private final LiteFish plugin;
    private Connection connection;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    
    public MySQLManager(LiteFish plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfigManager().getConfig().getString("database.host", "localhost");
        this.port = plugin.getConfigManager().getConfig().getInt("database.port", 3306);
        this.database = plugin.getConfigManager().getConfig().getString("database.database", "litefish");
        this.username = plugin.getConfigManager().getConfig().getString("database.username", "root");
        this.password = plugin.getConfigManager().getConfig().getString("database.password", "");
    }
    
    /**
     * Connect to MySQL database
     */
    public boolean connect() {
        if (!plugin.getConfigManager().getConfig().getBoolean("database.enabled", false)) {
            return false;
        }
        
        try {
            if (connection != null && !connection.isClosed()) {
                return true;
            }
            
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";
            connection = DriverManager.getConnection(url, username, password);
            
            createTables();
            plugin.getLogger().info("Successfully connected to MySQL database");
            return true;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Disconnect from database
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Disconnected from MySQL database");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error disconnecting from database: " + e.getMessage());
        }
    }
    
    /**
     * Create necessary tables
     */
    private void createTables() throws SQLException {
        String playerDataQuery = "CREATE TABLE IF NOT EXISTS litefish_players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "total_fish_caught INT DEFAULT 0," +
                "total_experience INT DEFAULT 0," +
                "total_money_earned DOUBLE DEFAULT 0.0," +
                "last_fishing_time BIGINT DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
        
        String fishDataQuery = "CREATE TABLE IF NOT EXISTS litefish_fish_data (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "fish_key VARCHAR(50) NOT NULL," +
                "amount_caught INT DEFAULT 0," +
                "FOREIGN KEY (player_uuid) REFERENCES litefish_players(uuid) ON DELETE CASCADE," +
                "UNIQUE KEY unique_player_fish (player_uuid, fish_key)" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(playerDataQuery);
            stmt.execute(fishDataQuery);
        }
    }
    
    /**
     * Save player data to database
     */
    public void savePlayerData(Player player) {
        if (connection == null) return;
        
        try {
            String query = "INSERT INTO litefish_players (uuid, name, total_fish_caught, total_experience, total_money_earned, last_fishing_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "name = VALUES(name), " +
                    "total_fish_caught = VALUES(total_fish_caught), " +
                    "total_experience = VALUES(total_experience), " +
                    "total_money_earned = VALUES(total_money_earned), " +
                    "last_fishing_time = VALUES(last_fishing_time)";
            
            PlayerDataManager.PlayerFishingData playerData = plugin.getPlayerDataManager().getPlayerData(player);
            
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.setInt(3, playerData.getTotalFishCaught());
                stmt.setInt(4, playerData.getTotalExperience());
                stmt.setDouble(5, playerData.getTotalMoneyEarned());
                stmt.setLong(6, playerData.getLastFishingTime());
                
                stmt.executeUpdate();
            }
            
            // Save fish data
            saveFishData(player);
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving player data: " + e.getMessage());
        }
    }
    
    /**
     * Save fish data for a player
     */
    private void saveFishData(Player player) throws SQLException {
        PlayerDataManager.PlayerFishingData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        Map<String, Integer> fishCaught = playerData.getFishCaught();
        
        // Clear existing data
        String deleteQuery = "DELETE FROM litefish_fish_data WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteQuery)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.executeUpdate();
        }
        
        // Insert new data
        String insertQuery = "INSERT INTO litefish_fish_data (player_uuid, fish_key, amount_caught) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
            for (Map.Entry<String, Integer> entry : fishCaught.entrySet()) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, entry.getKey());
                stmt.setInt(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    
    /**
     * Check if connected to database
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
