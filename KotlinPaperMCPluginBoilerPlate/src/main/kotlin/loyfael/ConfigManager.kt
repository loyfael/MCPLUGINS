package loyfael

import org.bukkit.configuration.file.FileConfiguration

/**
 * Gestionnaire de configuration du plugin
 * 
 * Cette classe centralise l'accès à tous les paramètres de configuration
 * et fournit des méthodes typées pour récupérer les valeurs.
 */
class ConfigManager(private val plugin: MyPlugin) {
    
    private val config: FileConfiguration = plugin.config
    
    // === PARAMÈTRES GÉNÉRAUX ===
    
    /**
     * Récupère la langue configurée
     */
    fun getLanguage(): String {
        return config.getString("general.language", "fr")!!
    }
    
    /**
     * Récupère le préfixe des messages
     */
    fun getPrefix(): String {
        return config.getString("general.prefix", "&8[&bMyPlugin&8] &r")!!
    }
    
    /**
     * Vérifie si le debug est activé
     */
    fun isDebugEnabled(): Boolean {
        return config.getBoolean("general.debug", false)
    }
    
    /**
     * Vérifie si la vérification des mises à jour est activée
     */
    fun isUpdateCheckEnabled(): Boolean {
        return config.getBoolean("general.check-updates", true)
    }
    
    // === PARAMÈTRES BASE DE DONNÉES ===
    
    /**
     * Récupère le type de base de données
     */
    fun getDatabaseType(): String {
        return config.getString("database.type", "sqlite")!!.lowercase()
    }
    
    /**
     * Récupère le nom du fichier SQLite
     */
    fun getSQLiteFile(): String {
        return config.getString("database.sqlite.file", "data.db")!!
    }
    
    /**
     * Récupère l'hôte MySQL
     */
    fun getMySQLHost(): String {
        return config.getString("database.mysql.host", "localhost")!!
    }
    
    /**
     * Récupère le port MySQL
     */
    fun getMySQLPort(): Int {
        return config.getInt("database.mysql.port", 3306)
    }
    
    /**
     * Récupère le nom de la base de données MySQL
     */
    fun getMySQLDatabase(): String {
        return config.getString("database.mysql.database", "myplugin")!!
    }
    
    /**
     * Récupère le nom d'utilisateur MySQL
     */
    fun getMySQLUsername(): String {
        return config.getString("database.mysql.username", "root")!!
    }
    
    /**
     * Récupère le mot de passe MySQL
     */
    fun getMySQLPassword(): String {
        return config.getString("database.mysql.password", "password")!!
    }
    
    /**
     * Récupère la taille maximale du pool de connexions
     */
    fun getMaxPoolSize(): Int {
        return config.getInt("database.mysql.connection.maximum-pool-size", 10)
    }
    
    /**
     * Récupère le nombre minimum de connexions inactives
     */
    fun getMinimumIdle(): Int {
        return config.getInt("database.mysql.connection.minimum-idle", 2)
    }
    
    /**
     * Récupère le timeout de connexion
     */
    fun getConnectionTimeout(): Long {
        return config.getLong("database.mysql.connection.connection-timeout", 30000L)
    }
    
    /**
     * Récupère le timeout d'inactivité
     */
    fun getIdleTimeout(): Long {
        return config.getLong("database.mysql.connection.idle-timeout", 600000L)
    }
    
    /**
     * Récupère la durée de vie maximale d'une connexion
     */
    fun getMaxLifetime(): Long {
        return config.getLong("database.mysql.connection.max-lifetime", 1800000L)
    }
    
    // === MESSAGES ===
    
    /**
     * Récupère un message formaté avec le préfixe
     */
    fun getMessage(path: String, defaultValue: String = ""): String {
        val message = config.getString("messages.$path", defaultValue) ?: defaultValue
        return if (message.isEmpty()) defaultValue else getPrefix() + message
    }
    
    /**
     * Récupère un message sans préfixe
     */
    fun getRawMessage(path: String, defaultValue: String = ""): String {
        return config.getString("messages.$path", defaultValue) ?: defaultValue
    }
    
    /**
     * Récupère la liste des commandes d'aide
     */
    fun getHelpCommands(): List<String> {
        return config.getStringList("messages.help.commands")
    }
    
    // === PARAMÈTRES AVANCÉS ===
    
    /**
     * Récupère l'intervalle de sauvegarde automatique
     */
    fun getAutoSaveInterval(): Long {
        return config.getLong("advanced.auto-save-interval", 5L)
    }
    
    /**
     * Récupère la taille du cache
     */
    fun getCacheSize(): Int {
        return config.getInt("advanced.cache-size", 1000)
    }
    
    /**
     * Vérifie si les métriques sont activées
     */
    fun isMetricsEnabled(): Boolean {
        return config.getBoolean("advanced.enable-metrics", true)
    }
    
    // === UTILITAIRES ===
    
    /**
     * Vérifie si une clé existe dans la configuration
     */
    fun hasKey(path: String): Boolean {
        return config.contains(path)
    }
    
    /**
     * Récupère la version de la configuration
     */
    fun getConfigVersion(): Int {
        return config.getInt("config-version", 1)
    }
    
    /**
     * Charge la configuration depuis le fichier
     */
    fun loadConfig() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        
        if (isDebugEnabled()) {
            plugin.logger.info("§aConfiguration chargée avec succès")
        }
    }
    
    /**
     * Recharge la configuration depuis le fichier
     */
    fun reloadConfig() {
        plugin.reloadConfig()
        
        if (isDebugEnabled()) {
            plugin.logger.info("§aConfiguration rechargée")
        }
    }
}