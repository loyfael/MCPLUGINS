package loyfael.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import loyfael.Main
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Supplier

/**
 * Gestionnaire MySQL avec HikariCP pour un accès SQL rapide et fiable
 */
class MySqlManager(private val plugin: Main) {

    private var dataSource: HikariDataSource? = null
    private val executor: ExecutorService = Executors.newFixedThreadPool(8)

    fun connect(): Boolean {
        return try {
            val cfg = plugin.configManager
            
            plugin.logger.info("==================================================")
            plugin.logger.info("Tentative de connexion à MySQL...")
            plugin.logger.info("Host: ${cfg.getDbHost()}")
            plugin.logger.info("Port: ${cfg.getDbPort()}")
            plugin.logger.info("Database: ${cfg.getDbName()}")
            plugin.logger.info("User: ${cfg.getDbUser()}")
            plugin.logger.info("==================================================")
            
            val hc = HikariConfig().apply {
                val jdbcUrl = "jdbc:mysql://${cfg.getDbHost()}:${cfg.getDbPort()}/${cfg.getDbName()}" +
                        "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                this.jdbcUrl = jdbcUrl
                this.username = cfg.getDbUser()
                this.password = cfg.getDbPassword()

                this.maximumPoolSize = cfg.getHikariMaximumPoolSize()
                this.minimumIdle = cfg.getHikariMinimumIdle()
                this.connectionTimeout = cfg.getHikariConnectionTimeoutMs()
                this.idleTimeout = cfg.getHikariIdleTimeoutMs()
                this.maxLifetime = cfg.getHikariMaxLifetimeMs()

                this.poolName = "AetherPlayerShop-Hikari"
                this.leakDetectionThreshold = 20_000L
            }

            dataSource = HikariDataSource(hc)

            plugin.logger.info("✅ Connexion MySQL établie avec succès !")

            // Initialisation du schéma
            initializeSchema()
            true
        } catch (e: SQLException) {
            plugin.logger.severe("❌ Erreur SQL lors de la connexion MySQL:")
            plugin.logger.severe("   Message: ${e.message}")
            plugin.logger.severe("")
            plugin.logger.severe("🔧 Vérifications à faire:")
            plugin.logger.severe("   1. MySQL est-il démarré? (XAMPP/WAMP/Service)")
            plugin.logger.severe("   2. Le port 3306 est-il accessible?")
            plugin.logger.severe("   3. L'utilisateur '${plugin.configManager.getDbUser()}' existe-t-il?")
            plugin.logger.severe("   4. Le mot de passe est-il correct?")
            plugin.logger.severe("   5. La base '${plugin.configManager.getDbName()}' existe-t-elle?")
            plugin.logger.severe("")
            plugin.logger.severe("💡 Pour créer la base de données, exécutez:")
            plugin.logger.severe("   mysql -u root -p")
            plugin.logger.severe("   CREATE DATABASE aetherplayershop;")
            plugin.logger.severe("   GRANT ALL PRIVILEGES ON aetherplayershop.* TO 'root'@'localhost';")
            e.printStackTrace()
            false
        } catch (e: Exception) {
            plugin.logger.severe("❌ Erreur inattendue lors de la connexion MySQL: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun initializeSchema() {
        try {
            plugin.logger.info("Initialisation du schéma de la base de données...")
            getConnection().use { conn ->
                conn.createStatement().use { st ->
                    st.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS shops (
                          id VARCHAR(36) PRIMARY KEY,
                          owner_uuid VARCHAR(36) NOT NULL,
                          owner_name VARCHAR(32) NOT NULL,
                          world VARCHAR(64) NOT NULL,
                          x INT NOT NULL,
                          y INT NOT NULL,
                          z INT NOT NULL,
                          type VARCHAR(16) NOT NULL,
                          price DOUBLE NOT NULL,
                          stock INT NOT NULL,
                          teleport_policy VARCHAR(32) NOT NULL DEFAULT 'ALLOW_TP',
                          item_material VARCHAR(64) NOT NULL,
                          item_data LONGBLOB NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          active TINYINT(1) NOT NULL DEFAULT 1,
                          KEY idx_owner (owner_uuid),
                          KEY idx_world_xyz (world, x, y, z),
                          KEY idx_material (item_material),
                          KEY idx_active (active)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                        """.trimIndent()
                    )

                    st.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS transactions (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          buyer_uuid VARCHAR(36) NOT NULL,
                          seller_uuid VARCHAR(36) NOT NULL,
                          shop_id VARCHAR(36) NOT NULL,
                          item_key VARCHAR(128) NOT NULL,
                          unit_price DOUBLE NOT NULL,
                          quantity INT NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          KEY idx_buyer (buyer_uuid),
                          KEY idx_seller (seller_uuid),
                          KEY idx_item (item_key),
                          KEY idx_created (created_at),
                          CONSTRAINT fk_shop FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                        """.trimIndent()
                    )
                    
                    plugin.logger.info("✅ Schéma de base de données initialisé avec succès!")
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("⚠️ Impossible de créer le schéma MySQL automatiquement: ${e.message}")
            plugin.logger.warning("Les tables seront peut-être créées plus tard ou doivent être créées manuellement.")
        }
    }

    fun disconnect() {
        try {
            dataSource?.close()
        } catch (ignored: Exception) {
        }
        executor.shutdown()
        plugin.logger.info("Connexion MySQL fermée.")
    }

    @Throws(SQLException::class)
    fun getConnection(): Connection {
        if (dataSource == null) throw SQLException("DataSource not initialized")
        return dataSource!!.connection
    }

    fun executeAsync(runnable: Runnable): CompletableFuture<Void> {
        return CompletableFuture.runAsync(runnable, executor)
    }

    fun <T> supplyAsync(supplier: Supplier<T>): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(supplier, executor)
    }
}
