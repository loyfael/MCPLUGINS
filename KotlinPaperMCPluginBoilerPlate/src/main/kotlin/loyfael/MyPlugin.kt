package loyfael

import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

/**
 * Classe principale du plugin MyPlugin
 * 
 * Cette classe hérite de JavaPlugin et contient les méthodes
 * d'activation et de désactivation du plugin.
 */
class MyPlugin : JavaPlugin() {
    
    companion object {
        /**
         * Instance statique du plugin pour accès global
         */
        lateinit var instance: MyPlugin
            private set
    }
    
    // === Managers ===
    private lateinit var configManager: ConfigManager
    private lateinit var databaseManager: DatabaseManager
    
    /**
     * Méthode appelée lors de l'activation du plugin
     */
    override fun onEnable() {
        // Initialisation de l'instance statique
        instance = this
        
        try {
            // === CHARGEMENT DE LA CONFIGURATION ===
            configManager = ConfigManager(this)
            configManager.loadConfig()
            
            // Message de démarrage avec version
            if (configManager.isDebugEnabled()) {
                logger.info("§eMode debug activé")
            }
            
            // === INITIALISATION BASE DE DONNÉES ===
            databaseManager = DatabaseManager(this, configManager)
            if (databaseManager.initializeDatabase()) {
                logger.info("§aBase de données initialisée avec succès")
            } else {
                logger.severe("§cErreur lors de l'initialisation de la base de données")
                server.pluginManager.disablePlugin(this)
                return
            }
            
            logger.info("§aMyPlugin activé avec succès ! Version: ${pluginMeta.version}")
            
            // === ENREGISTREMENT DES COMMANDES ===
            registerCommands()
            
            // === ENREGISTREMENT DES LISTENERS ===
            registerListeners()
            
            // === TÂCHES ASYNCHRONES ===
            setupAsyncTasks()
            
            // Message final
            logger.info("§a✓ Toutes les fonctionnalités de MyPlugin sont opérationnelles !")
            
        } catch (exception: Exception) {
            logger.log(Level.SEVERE, "§cErreur critique lors du chargement du plugin:", exception)
            server.pluginManager.disablePlugin(this)
        }
    }
    
    /**
     * Méthode appelée lors de la désactivation du plugin
     */
    override fun onDisable() {
        try {
            // Fermeture des connexions base de données
            if (::databaseManager.isInitialized) {
                databaseManager.closeConnection()
                logger.info("§aConnexion à la base de données fermée")
            }
            
            // Annulation des tâches asynchrones
            server.scheduler.cancelTasks(this)
            logger.info("§aTâches asynchrones annulées")
            
            logger.info("§cMyPlugin désactivé avec succès !")
            
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "§eErreur lors de la fermeture du plugin:", exception)
        }
    }
    
    /**
     * Enregistrement des commandes du plugin
     */
    private fun registerCommands() {
        try {
            val mainCommand = MainCommand(this, configManager, databaseManager)
            getCommand("myplugin")?.setExecutor(mainCommand)
            getCommand("myplugin")?.tabCompleter = mainCommand
            
            if (configManager.isDebugEnabled()) {
                logger.info("§a✓ Commandes enregistrées avec succès")
            }
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "§eErreur lors de l'enregistrement des commandes:", exception)
        }
    }
    
    /**
     * Enregistrement des listeners d'événements
     */
    private fun registerListeners() {
        try {
            server.pluginManager.registerEvents(PlayerConnectionListener(this, databaseManager), this)
            
            if (configManager.isDebugEnabled()) {
                logger.info("§a✓ Listeners enregistrés avec succès")
            }
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "§eErreur lors de l'enregistrement des listeners:", exception)
        }
    }
    
    /**
     * Configuration des tâches asynchrones
     */
    private fun setupAsyncTasks() {
        try {
            val autoSaveInterval = configManager.getAutoSaveInterval()
            
            // Tâche de sauvegarde automatique
            if (autoSaveInterval > 0) {
                server.scheduler.runTaskTimerAsynchronously(this, Runnable {
                    // Sauvegarde automatique
                    if (configManager.isDebugEnabled()) {
                        logger.info("Sauvegarde automatique...")
                    }
                }, 20L * 60L * autoSaveInterval, 20L * 60L * autoSaveInterval)
            }
            
            if (configManager.isDebugEnabled()) {
                logger.info("§a✓ Tâches asynchrones configurées")
            }
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "§eErreur lors de la configuration des tâches:", exception)
        }
    }
    
    /**
     * Rechargement de la configuration du plugin
     */
    fun reloadPluginConfig(): Boolean {
        return try {
            configManager.reloadConfig()
            logger.info("§aConfiguration rechargée avec succès !")
            true
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "§eErreur lors du rechargement de la configuration:", exception)
            false
        }
    }
    
    /**
     * Getter pour le ConfigManager
     */
    fun getConfigManager(): ConfigManager = configManager
    
    /**
     * Getter pour le DatabaseManager
     */
    fun getDatabaseManager(): DatabaseManager = databaseManager
}