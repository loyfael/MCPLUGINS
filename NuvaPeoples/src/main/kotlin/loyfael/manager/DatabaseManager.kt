package loyfael.manager

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import loyfael.ClassePlugin
import loyfael.data.Classe
import loyfael.data.PlayerData
import org.bukkit.configuration.file.FileConfiguration
import java.io.File
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

/**
 * Database manager supporting both MySQL and SQLite with automatic fallback
 */
class DatabaseManager(private val plugin: ClassePlugin) {
    
    private lateinit var dataSource: HikariDataSource
    private val config: FileConfiguration = plugin.config
    private var usingMysql = false
    
    /**
     * Initializes database connection based on configuration
     */
    fun initialize(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val databaseType = config.getString("database.type", "sqlite")?.lowercase()
            
            when (databaseType) {
                "mysql" -> {
                    if (initializeMySQL()) {
                        // usingMysql is already set to true in initializeMySQL()
                        plugin.logger.info("✓ Base de données MySQL connectée avec succès")
                        true
                    } else {
                        plugin.logger.severe("✗ Impossible de se connecter à MySQL")
                        false
                    }
                }
                "sqlite" -> {
                    if (initializeSQLite()) {
                        usingMysql = false
                        plugin.logger.info("✓ Base de données SQLite initialisée avec succès")
                        true
                    } else {
                        plugin.logger.severe("✗ Impossible d'initialiser SQLite")
                        false
                    }
                }
                else -> {
                    plugin.logger.severe("✗ Type de base de données non supporté: $databaseType")
                    false
                }
            }
        }
    }
    
    /**
     * Initialize MySQL connection
     */
    private fun initializeMySQL(): Boolean {
        return try {
            usingMysql = true // Set this FIRST before creating tables
            
            val host = config.getString("database.mysql.host")
            val port = config.getInt("database.mysql.port")
            val database = config.getString("database.mysql.database")
            val username = config.getString("database.mysql.username")
            val poolSize = config.getInt("database.pool_size", 10)
            
            val hikariConfig = HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC&sql_mode="
                this.username = username
                password = config.getString("database.mysql.password")
                maximumPoolSize = poolSize
                
                // Optimized configuration for Minecraft
                connectionTimeout = 30000 // 30 seconds
                idleTimeout = 600000 // 10 minutes
                maxLifetime = 1800000 // 30 minutes
                leakDetectionThreshold = 60000 // 1 minute
                
                // Performance parameters
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")
                addDataSourceProperty("rewriteBatchedStatements", "true")
            }
            
            plugin.logger.info("🔧 [DEBUG] JDBC URL: ${hikariConfig.jdbcUrl}")
            
            dataSource = HikariDataSource(hikariConfig)
            
            // Test connection
            dataSource.connection.use { 
                plugin.logger.info("✅ [DEBUG] Connexion réussie !")
            }
            
            // Create tables if they don't exist
            createTables()
            true
            
        } catch (e: Exception) {
            usingMysql = false // Reset on error
            plugin.logger.log(Level.WARNING, "Erreur de connexion MySQL: ${e.message}")
            plugin.logger.severe("❌ [DEBUG] Stack trace MySQL: ${e.stackTraceToString()}")
            false
        }
    }
    
    /**
     * Initialize SQLite connection
     */
    private fun initializeSQLite(): Boolean {
        return try {
            // Create plugin data folder if not exists
            val dataFolder = plugin.dataFolder
            if (!dataFolder.exists()) {
                dataFolder.mkdirs()
            }
            
            val dbFileName = config.getString("database.sqlite.file", "data.db") ?: "data.db"
            val dbFile = File(dataFolder, dbFileName)
            
            val hikariConfig = HikariConfig().apply {
                driverClassName = "org.sqlite.JDBC"  // Spécifier le driver SQLite
                jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
                maximumPoolSize = 1 // SQLite doesn't support multiple connections well
                connectionTimeout = 30000
                
                // SQLite specific settings
                addDataSourceProperty("foreign_keys", "true")
                addDataSourceProperty("journal_mode", "WAL")
                addDataSourceProperty("synchronous", "NORMAL")
            }
            
            dataSource = HikariDataSource(hikariConfig)
            
            // Test connection
            dataSource.connection.use { }
            
            // Create tables if they don't exist
            createTables()
            true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur de connexion SQLite: ${e.message}")
            false
        }
    }
    
    /**
     * Creates necessary tables (supports both MySQL and SQLite)
     */
    private fun createTables() {
        try {
            getConnection().use { connection ->
                val statement = connection.createStatement()
                
                // Main table for players and their classes
                val createTableSQL = if (usingMysql) {
                    // MySQL version with ENUM for better compatibility
                    """
                        CREATE TABLE IF NOT EXISTS nuva_joueurs (
                            uuid VARCHAR(36) PRIMARY KEY,
                            classe ENUM('AME_ERRANTE', 'BASTORGNES', 'TARTINUITS', 'SYLVOUNETS', 'GROSUKI', 'BRICOBRAK', 'MIRAZIENS') NOT NULL DEFAULT 'AME_ERRANTE',
                            date_selection TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            derniere_connexion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            INDEX idx_classe (classe),
                            INDEX idx_derniere_connexion (derniere_connexion)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """.trimIndent()
                } else {
                    // SQLite version
                    """
                        CREATE TABLE IF NOT EXISTS nuva_joueurs (
                            uuid TEXT PRIMARY KEY,
                            classe TEXT NOT NULL DEFAULT 'AME_ERRANTE',
                            date_selection INTEGER DEFAULT (strftime('%s', 'now')),
                            derniere_connexion INTEGER DEFAULT (strftime('%s', 'now'))
                        )
                    """.trimIndent()
                }
                
                statement.execute(createTableSQL)
                
                // Create indexes for SQLite (MySQL indexes are created in table definition)
                if (!usingMysql) {
                    try {
                        statement.execute("CREATE INDEX IF NOT EXISTS idx_classe ON nuva_joueurs(classe)")
                        statement.execute("CREATE INDEX IF NOT EXISTS idx_derniere_connexion ON nuva_joueurs(derniere_connexion)")
                    } catch (e: SQLException) {
                        // Indexes may already exist, ignore
                    }
                }
                
                plugin.logger.info("Tables de base de données créées/vérifiées (${if (usingMysql) "MySQL" else "SQLite"})")
            }
        } catch (e: Exception) {
            plugin.logger.severe("❌ [DEBUG] Erreur création table: ${e.message}")
            plugin.logger.severe("❌ [DEBUG] Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }
    
    /**
     * Gets a connection from the pool
     */
    fun getConnection(): Connection = dataSource.connection
    
    /**
     * Get timestamp conversion function based on database type
     */
    private fun getTimestampFunction(column: String): String {
        return if (usingMysql) {
            "UNIX_TIMESTAMP($column)"
        } else {
            "strftime('%s', $column)"
        }
    }
    
    /**
     * Saves player data to database (supports both MySQL and SQLite)
     */
    fun savePlayer(playerData: PlayerData): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                getConnection().use { connection ->
                    val sql = if (usingMysql) {
                        // MySQL version with ON DUPLICATE KEY UPDATE
                        """
                            INSERT INTO nuva_joueurs (uuid, classe, date_selection, derniere_connexion)
                            VALUES (?, ?, FROM_UNIXTIME(?), FROM_UNIXTIME(?))
                            ON DUPLICATE KEY UPDATE 
                            classe = VALUES(classe),
                            date_selection = VALUES(date_selection),
                            derniere_connexion = VALUES(derniere_connexion)
                        """.trimIndent()
                    } else {
                        // SQLite version with ON CONFLICT REPLACE
                        """
                            INSERT OR REPLACE INTO nuva_joueurs (uuid, classe, date_selection, derniere_connexion)
                            VALUES (?, ?, ?, ?)
                        """.trimIndent()
                    }
                    
                    connection.prepareStatement(sql).use { statement ->
                        statement.setString(1, playerData.uuid.toString())
                        statement.setString(2, playerData.classe.name)
                        statement.setLong(3, playerData.selectionDate / 1000)
                        statement.setLong(4, System.currentTimeMillis() / 1000)
                        
                        val result = statement.executeUpdate()
                        
                        if (config.getBoolean("debug", false)) {
                            plugin.logger.info("Données sauvées pour ${playerData.uuid} - Classe: ${playerData.classe.displayName}")
                        }
                        
                        result > 0
                    }
                }
            } catch (e: SQLException) {
                plugin.logger.log(Level.SEVERE, "Erreur lors de la sauvegarde du joueur ${playerData.uuid}", e)
                false
            }
        }
    }
    
    /**
     * Loads player data from database
     */
    fun loadPlayer(uuid: UUID): CompletableFuture<PlayerData?> {
        return CompletableFuture.supplyAsync {
            try {
                getConnection().use { connection ->
                    val sql = "SELECT classe, ${getTimestampFunction("date_selection")} as date_selection FROM nuva_joueurs WHERE uuid = ?"
                    
                    connection.prepareStatement(sql).use { statement ->
                        statement.setString(1, uuid.toString())
                        
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                val classeName = resultSet.getString("classe")
                                val selectionDate = resultSet.getLong("date_selection") * 1000L
                                
                                val classe = Classe.fromString(classeName) ?: Classe.AME_ERRANTE
                                
                                val playerData = PlayerData(uuid, classe, selectionDate)
                                
                                if (config.getBoolean("debug", false)) {
                                    plugin.logger.info("Data loaded for $uuid - Class: ${classe.displayName}")
                                }
                                
                                playerData
                            } else {
                                // New player, create default entry
                                val newPlayer = PlayerData(uuid)
                                savePlayer(newPlayer)
                                newPlayer
                            }
                        }
                    }
                }
            } catch (e: SQLException) {
                plugin.logger.log(Level.SEVERE, "Error loading player $uuid", e)
                null
            }
        }
    }
    
    /**
     * Gets class statistics
     */
    fun getClassStatistics(): CompletableFuture<Map<Classe, Int>> {
        return CompletableFuture.supplyAsync {
            try {
                getConnection().use { connection ->
                    val sql = "SELECT classe, COUNT(*) as nombre FROM nuva_joueurs GROUP BY classe"
                    
                    connection.prepareStatement(sql).use { statement ->
                        statement.executeQuery().use { resultSet ->
                            val statistics = mutableMapOf<Classe, Int>()
                            
                            while (resultSet.next()) {
                                val classeName = resultSet.getString("classe")
                                val count = resultSet.getInt("nombre")
                                
                                val classe = Classe.fromString(classeName) ?: Classe.AME_ERRANTE
                                statistics[classe] = count
                            }
                            
                            statistics
                        }
                    }
                }
            } catch (e: SQLException) {
                plugin.logger.log(Level.SEVERE, "Error retrieving statistics", e)
                emptyMap()
            }
        }
    }
    
    /**
     * Cleans old records (inactive players for more than X days)
     */
    fun cleanOldRecords(inactiveDays: Int = 365): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                getConnection().use { connection ->
                    val sql = if (usingMysql) {
                        "DELETE FROM nuva_joueurs WHERE derniere_connexion < DATE_SUB(NOW(), INTERVAL ? DAY)"
                    } else {
                        "DELETE FROM nuva_joueurs WHERE derniere_connexion < (strftime('%s', 'now') - (? * 24 * 3600))"
                    }
                    
                    connection.prepareStatement(sql).use { statement ->
                        statement.setInt(1, inactiveDays)
                        
                        val deleted = statement.executeUpdate()
                        
                        if (deleted > 0) {
                            plugin.logger.info("$deleted anciens enregistrements supprimés")
                        }
                        
                        deleted
                    }
                }
            } catch (e: SQLException) {
                plugin.logger.log(Level.SEVERE, "Erreur lors du nettoyage", e)
                0
            }
        }
    }
    
    /**
     * Tests database connection
     */
    fun testConnection(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                getConnection().use { connection ->
                    connection.prepareStatement("SELECT 1").use { statement ->
                        statement.executeQuery().use { resultSet ->
                            resultSet.next() && resultSet.getInt(1) == 1
                        }
                    }
                }
            } catch (e: SQLException) {
                plugin.logger.log(Level.WARNING, "Connection test failed", e)
                false
            }
        }
    }
    
    /**
     * Properly closes database connection
     */
    fun close() {
        if (::dataSource.isInitialized && !dataSource.isClosed) {
            dataSource.close()
            plugin.logger.info("Database connection closed")
        }
    }
}
