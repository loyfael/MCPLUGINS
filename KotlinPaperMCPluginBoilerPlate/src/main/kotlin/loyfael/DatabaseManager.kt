package loyfael

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException
import java.util.logging.Level
import javax.sql.DataSource

/**
 * Gestionnaire de base de données
 * 
 * Cette classe gère la connexion à la base de données (SQLite ou MySQL)
 * et fournit des méthodes pour exécuter des requêtes.
 */
class DatabaseManager(private val plugin: MyPlugin, private val configManager: ConfigManager) {
    
    private var dataSource: DataSource? = null
    
    /**
     * Initialise la base de données
     */
    fun initializeDatabase(): Boolean {
        return try {
            when (configManager.getDatabaseType()) {
                "mysql" -> initializeMySQL()
                "sqlite" -> initializeSQLite()
                else -> {
                    plugin.logger.severe("Type de base de données non supporté : ${configManager.getDatabaseType()}")
                    return false
                }
            }
            
            // Création des tables
            createTables()
            
            plugin.logger.info("Base de données initialisée avec succès (${configManager.getDatabaseType().uppercase()})")
            true
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur lors de l'initialisation de la base de données !", e)
            false
        }
    }
    
    /**
     * Initialise la connexion MySQL avec HikariCP
     */
    private fun initializeMySQL() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${configManager.getMySQLHost()}:${configManager.getMySQLPort()}/${configManager.getMySQLDatabase()}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            username = configManager.getMySQLUsername()
            password = configManager.getMySQLPassword()
            driverClassName = "com.mysql.cj.jdbc.Driver"
            
            // Configuration du pool
            maximumPoolSize = configManager.getMaxPoolSize()
            minimumIdle = configManager.getMinimumIdle()
            connectionTimeout = configManager.getConnectionTimeout()
            idleTimeout = configManager.getIdleTimeout()
            maxLifetime = configManager.getMaxLifetime()
            
            // Propriétés additionnelles
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("useLocalSessionState", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
            addDataSourceProperty("cacheResultSetMetadata", "true")
            addDataSourceProperty("cacheServerConfiguration", "true")
            addDataSourceProperty("elideSetAutoCommits", "true")
            addDataSourceProperty("maintainTimeStats", "false")
        }
        
        dataSource = HikariDataSource(config)
    }
    
    /**
     * Initialise la connexion SQLite avec HikariCP
     */
    private fun initializeSQLite() {
        val databaseFile = File(plugin.dataFolder, configManager.getSQLiteFile())
        
        // Création du dossier parent si nécessaire
        if (!databaseFile.parentFile.exists()) {
            databaseFile.parentFile.mkdirs()
        }
        
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"
            driverClassName = "org.sqlite.JDBC"
            
            // Configuration optimisée pour SQLite
            maximumPoolSize = 1 // SQLite ne supporte qu'une connexion en écriture
            connectionTimeout = 30000
            
            // Propriétés SQLite
            addDataSourceProperty("journal_mode", "WAL")
            addDataSourceProperty("synchronous", "NORMAL")
            addDataSourceProperty("cache_size", "10000")
            addDataSourceProperty("foreign_keys", "true")
        }
        
        dataSource = HikariDataSource(config)
    }
    
    /**
     * Crée les tables nécessaires
     */
    private fun createTables() {
        useConnection { connection ->
            // Exemple de table utilisateur
            val createUserTable = """
                CREATE TABLE IF NOT EXISTS users (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    created_at ${getTimestampFunction()} DEFAULT ${getCurrentTimestamp()},
                    updated_at ${getTimestampFunction()} DEFAULT ${getCurrentTimestamp()}
                )
            """.trimIndent()
            
            connection.prepareStatement(createUserTable).use { statement ->
                statement.executeUpdate()
            }
            
            // Ajoutez d'autres tables ici...
            
            if (configManager.isDebugEnabled()) {
                plugin.logger.info("Tables créées ou vérifiées")
            }
        }
    }
    
    /**
     * Obtient une connexion à la base de données
     */
    @Throws(SQLException::class)
    fun getConnection(): Connection {
        return dataSource?.connection 
            ?: throw SQLException("DataSource non initialisé")
    }
    
    /**
     * Exécute une opération avec une connexion automatiquement fermée
     */
    fun <T> useConnection(operation: (Connection) -> T): T {
        return getConnection().use(operation)
    }
    
    /**
     * Exécute une requête de mise à jour (INSERT, UPDATE, DELETE)
     */
    fun executeUpdate(sql: String, vararg parameters: Any): Int {
        return useConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                parameters.forEachIndexed { index, param ->
                    statement.setObject(index + 1, param)
                }
                statement.executeUpdate()
            }
        }
    }
    
    /**
     * Exécute une requête de sélection
     */
    fun <T> executeQuery(sql: String, parameters: Array<Any> = emptyArray(), mapper: (java.sql.ResultSet) -> T): List<T> {
        return useConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                parameters.forEachIndexed { index, param ->
                    statement.setObject(index + 1, param)
                }
                statement.executeQuery().use { resultSet ->
                    val results = mutableListOf<T>()
                    while (resultSet.next()) {
                        results.add(mapper(resultSet))
                    }
                    results
                }
            }
        }
    }
    
    /**
     * Obtient la fonction timestamp appropriée selon le type de base de données
     */
    private fun getTimestampFunction(): String {
        return when (configManager.getDatabaseType()) {
            "mysql" -> "TIMESTAMP"
            "sqlite" -> "INTEGER"
            else -> "TIMESTAMP"
        }
    }
    
    /**
     * Obtient la valeur timestamp actuelle selon le type de base de données
     */
    private fun getCurrentTimestamp(): String {
        return when (configManager.getDatabaseType()) {
            "mysql" -> "CURRENT_TIMESTAMP"
            "sqlite" -> "strftime('%s', 'now')"
            else -> "CURRENT_TIMESTAMP"
        }
    }
    
    /**
     * Ferme la connexion à la base de données
     */
    fun closeConnection() {
        try {
            (dataSource as? HikariDataSource)?.close()
            plugin.logger.info("Connexion à la base de données fermée")
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Erreur lors de la fermeture de la base de données", e)
        }
    }
    
    /**
     * Teste la connexion à la base de données
     */
    fun testConnection(): Boolean {
        return try {
            useConnection { connection ->
                connection.isValid(5)
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Test de connexion échoué", e)
            false
        }
    }
}