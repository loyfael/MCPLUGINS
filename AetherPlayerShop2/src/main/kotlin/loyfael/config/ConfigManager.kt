package loyfael.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

/**
 * Gestionnaire de configuration du plugin
 */
class ConfigManager(private val plugin: JavaPlugin) {

    private lateinit var config: FileConfiguration

    fun loadConfig() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config

        // Valeurs par défaut si absentes
        setDefaults()
    }

    private fun setDefaults() {
        // Base de données MySQL + Hikari
        config.addDefault("database.type", "mysql")
        config.addDefault("database.host", "127.0.0.1")
        config.addDefault("database.port", 3306)
        config.addDefault("database.name", "aetherplayershop")
        config.addDefault("database.user", "root")
        config.addDefault("database.password", "password")
        config.addDefault("database.hikari.maximumPoolSize", 10)
        config.addDefault("database.hikari.minimumIdle", 2)
        config.addDefault("database.hikari.connectionTimeoutMs", 30000)
        config.addDefault("database.hikari.idleTimeoutMs", 600000)
        config.addDefault("database.hikari.maxLifetimeMs", 1800000)

        // Mode serveur (catalogue seulement)
        config.addDefault("server.mode", "ACTIVE") // ACTIVE | CATALOG_ONLY
        config.addDefault("server.primaryServer", "skyland")
        config.addDefault("catalogOnly.clickMessage", "Connecte-toi au serveur skyland pour faire un achat !")
        config.addDefault("catalogOnly.suggestCommand", "/server skyland")

        // Shop & téléportation
        config.addDefault("shop.max-shops-per-player", 6)
        config.addDefault("shop.teleport-enabled", true)
        config.addDefault("shop.teleport-delay", 3)

        // GUI
        config.addDefault("gui.menu-title", "§8§lCatalogue")
        config.addDefault("gui.items-per-page", 45)

        // Cache & effets
        config.addDefault("cache.max-size", 1000)
        config.addDefault("cache.expire-minutes", 30)
        config.addDefault("effects.sound-enabled", true)
        config.addDefault("effects.particle-enabled", true)

        config.options().copyDefaults(true)
        plugin.saveConfig()
    }

    // MySQL + Hikari
    fun getDbHost(): String = config.getString("database.host", "127.0.0.1")!!
    fun getDbPort(): Int = config.getInt("database.port", 3306)
    fun getDbName(): String = config.getString("database.name", "aetherplayershop")!!
    fun getDbUser(): String = config.getString("database.user", "root")!!
    fun getDbPassword(): String = config.getString("database.password", "password")!!
    fun getHikariMaximumPoolSize(): Int = config.getInt("database.hikari.maximumPoolSize", 10)
    fun getHikariMinimumIdle(): Int = config.getInt("database.hikari.minimumIdle", 2)
    fun getHikariConnectionTimeoutMs(): Long = config.getLong("database.hikari.connectionTimeoutMs", 30000L)
    fun getHikariIdleTimeoutMs(): Long = config.getLong("database.hikari.idleTimeoutMs", 600000L)
    fun getHikariMaxLifetimeMs(): Long = config.getLong("database.hikari.maxLifetimeMs", 1800000L)

    // Mode serveur
    fun getServerMode(): String = config.getString("server.mode", "ACTIVE")!!
    fun isCatalogOnly(): Boolean = "CATALOG_ONLY".equals(getServerMode(), ignoreCase = true)
    fun getCatalogSuggestCommand(): String = 
        config.getString("catalogOnly.suggestCommand", "/server skyland")!!

    // Shop Configuration
    fun isTeleportEnabled(): Boolean = config.getBoolean("shop.teleport-enabled", true)

    // GUI Configuration  
    val menuTitle: String get() = config.getString("gui.menu-title", "§8Catalogue")!!
    val itemsPerPage: Int get() = config.getInt("gui.items-per-page", 45)
    val catalogClickMessage: String get() = config.getString("catalogOnly.clickMessage", "Connecte-toi au serveur skyland pour faire un achat !")!!
    val teleportDelay: Int get() = config.getInt("shop.teleport-delay", 3)
    val primaryServer: String get() = config.getString("server.primaryServer", "skyland")!!
    val maxShopsPerPlayer: Int get() = config.getInt("shop.max-shops-per-player", 6)
    val inactivityDays: Int get() = config.getInt("shop.inactivity-days", 30)

    // Cache Configuration
    fun getCacheMaxSize(): Int = config.getInt("cache.max-size", 1000)
    fun getCacheExpireMinutes(): Int = config.getInt("cache.expire-minutes", 30)

    // Effects Configuration
    fun isSoundEnabled(): Boolean = config.getBoolean("effects.sound-enabled", true)
    fun isParticleEnabled(): Boolean = config.getBoolean("effects.particle-enabled", true)

    fun reload() {
        plugin.reloadConfig()
        config = plugin.config
    }
}
