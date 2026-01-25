package loyfael.managers

import loyfael.AetherPlayerDelivery
import org.bukkit.configuration.file.FileConfiguration

/**
 * Gestionnaire de configuration
 * Centralise l'accès aux paramètres de configuration
 */
class ConfigManager(private val plugin: AetherPlayerDelivery) {
    
    private val config: FileConfiguration = plugin.config
    
    /**
     * Initialise le gestionnaire de configuration
     */
    fun initialize() {
        // Sauvegarder le config.yml par défaut si il n'existe pas
        plugin.saveDefaultConfig()
        
        // Vérifier la version de configuration
        val configVersion = config.getInt("config-version", 0)
        if (configVersion < 1) {
            plugin.logger.warning("Configuration obsolète détectée. Mise à jour recommandée.")
        }
        
        plugin.logger.info("§aConfigManager initialisé !")
    }
    
    // === GETTERS POUR LES SECTIONS PRINCIPALES ===
    
    /**
     * Mode du serveur (principal ou satellite)
     */
    fun getServerMode(): String {
        return config.getString("mode", "principal") ?: "principal"
    }
    
    /**
     * Vérifie si le serveur est en mode principal
     */
    fun isPrincipalMode(): Boolean {
        return getServerMode().equals("principal", ignoreCase = true)
    }
    
    /**
     * Vérifie si le serveur est en mode satellite
     */
    fun isSatelliteMode(): Boolean {
        return getServerMode().equals("satellite", ignoreCase = true)
    }
    
    /**
     * Paramètres MySQL
     */
    fun getMySQLConfig(): MySQLConfig {
        return MySQLConfig(
            host = config.getString("mysql.host", "localhost")!!,
            port = config.getInt("mysql.port", 3306),
            database = config.getString("mysql.database", "aether_delivery")!!,
            username = config.getString("mysql.username", "root")!!,
            password = config.getString("mysql.password", "")!!,
            maximumPoolSize = config.getInt("mysql.pool.maximum-pool-size", 10),
            minimumIdle = config.getInt("mysql.pool.minimum-idle", 5),
            connectionTimeout = config.getLong("mysql.pool.connection-timeout", 30000),
            idleTimeout = config.getLong("mysql.pool.idle-timeout", 600000),
            maxLifetime = config.getLong("mysql.pool.max-lifetime", 1800000)
        )
    }
    
    /**
     * Paramètres de livraison
     */
    fun getLivraisonConfig(): LivraisonConfig {
        return LivraisonConfig(
            delaiRecuperation = config.getInt("livraison.delai_recuperation", 3),
            remboursementExpire = config.getInt("livraison.remboursement_expire", 50),
            quantiteMax = config.getInt("livraison.quantite_max", 2304),
            maxCommandesParJoueur = config.getInt("livraison.max_commandes_par_joueur", 2),
            delaiMinimum = config.getInt("livraison.delai_minimum", 1),
            delaiMaximum = config.getInt("livraison.delai_maximum", 7)
        )
    }
    
    /**
     * Items autorisés pour les commandes
     */
    fun getItemsAutorises(): List<String> {
        return config.getStringList("items_autorises")
    }
    
    /**
     * Paramètres des notifications
     */
    fun getNotificationConfig(): NotificationConfig {
        return NotificationConfig(
            chatEnabled = config.getBoolean("notifications.channels.chat", true),
            actionBarEnabled = config.getBoolean("notifications.channels.action-bar", true),
            titleEnabled = config.getBoolean("notifications.channels.title", false),
            soundEnabled = config.getBoolean("notifications.channels.sound", true)
        )
    }
    
    /**
     * Paramètres de réputation
     */
    fun getReputationConfig(): ReputationConfig {
        return ReputationConfig(
            penaliteEchec = config.getDouble("reputation.penalite-echec", 0.1),
            bonusSucces = config.getDouble("reputation.bonus-succes", 0.05),
            penaliteAnnulation = config.getDouble("reputation.penalite-annulation", 0.2),
            reputationMinimum = config.getDouble("reputation.reputation-minimum", 0.0)
        )
    }
    
    /**
     * Message de configuration avec préfixe
     */
    fun getMessage(key: String, defaultValue: String = ""): String {
        val prefix = config.getString("general.prefix", "&8[&6AetherDelivery&8] &r") ?: ""
        val message = config.getString("messages.$key", defaultValue) ?: defaultValue
        return "$prefix$message".replace("&", "§")
    }
    
    /**
     * Message de configuration sans préfixe
     */
    fun getMessageRaw(key: String, defaultValue: String = ""): String {
        return (config.getString("messages.$key", defaultValue) ?: defaultValue).replace("&", "§")
    }
    
    /**
     * Mode debug activé
     */
    fun isDebugMode(): Boolean {
        return config.getBoolean("general.debug", false)
    }
    
    /**
     * Langue du plugin
     */
    fun getLanguage(): String {
        return config.getString("general.language", "fr") ?: "fr"
    }
    
    /**
     * Recharge la configuration
     */
    fun reload() {
        plugin.reloadConfig()
        plugin.logger.info("§aConfiguration rechargée !")
    }
}

/**
 * Configuration MySQL
 */
data class MySQLConfig(
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int,
    val minimumIdle: Int,
    val connectionTimeout: Long,
    val idleTimeout: Long,
    val maxLifetime: Long
)

/**
 * Configuration des livraisons
 */
data class LivraisonConfig(
    val delaiRecuperation: Int,
    val remboursementExpire: Int,
    val quantiteMax: Int,
    val maxCommandesParJoueur: Int,
    val delaiMinimum: Int,
    val delaiMaximum: Int
)

/**
 * Configuration des notifications
 */
data class NotificationConfig(
    val chatEnabled: Boolean,
    val actionBarEnabled: Boolean,
    val titleEnabled: Boolean,
    val soundEnabled: Boolean
)

/**
 * Configuration de la réputation
 */
data class ReputationConfig(
    val penaliteEchec: Double,
    val bonusSucces: Double,
    val penaliteAnnulation: Double,
    val reputationMinimum: Double
)
