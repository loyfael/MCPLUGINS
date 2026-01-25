package loyfael

import loyfael.commands.ClasseCommand
import loyfael.integrations.NuvaPlaceholderExpansion
import loyfael.listeners.ItemListener
import loyfael.listeners.PassiveListener
import loyfael.manager.ClasseManager
import loyfael.manager.DatabaseManager
import loyfael.manager.GUIManager
import loyfael.manager.PassiveManager
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

/**
 * Plugin NuvaPeoples - Classes/Peuples Minecraft PaperMC 1.21.8
 * 
 * Plugin permettant aux joueurs de choisir une classe/peuple avec des passifs spécifiques.
 * Intégration avec MySQL, Triumph GUI et PlaceholderAPI.
 * 
 * @author Loyfael
 * @version 1.0.0
 */
class ClassePlugin : JavaPlugin() {
    
    // Plugin managers
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var classeManager: ClasseManager
        private set
    lateinit var passiveManager: PassiveManager
        private set
    lateinit var guiManager: GUIManager
        private set
    
    // PlaceholderAPI expansion
    private var placeholderExpansion: NuvaPlaceholderExpansion? = null
    
    override fun onEnable() {
        logger.info("═══════════════════════════════════════")
        logger.info("    NuvaPeoples v${pluginMeta.version}")
        logger.info("    Plugin de Classes/Peuples Minecraft")
        logger.info("    par ${pluginMeta.authors.joinToString(", ")}")
        logger.info("═══════════════════════════════════════")
        
        try {
            // 1. Load configuration
            initializeConfiguration()
            
            // 2. Initialize database
            initializeDatabase()
            
            // 3. Initialize managers
            initializeManagers()
            
            // 4. Register listeners
            registerListeners()
            
            // 5. Register commands
            registerCommands()
            
            // 6. Initialize PlaceholderAPI
            initializePlaceholderAPI()
            
            logger.info("✓ NuvaPeoples chargé avec succès!")
            
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "✗ Erreur lors du chargement de NuvaPeoples", e)
            server.pluginManager.disablePlugin(this)
        }
    }
    
    override fun onDisable() {
        logger.info("Arrêt de NuvaPeoples...")
        
        try {
            // Stop managers
            if (::passiveManager.isInitialized) {
                passiveManager.shutdown()
            }
            
            // Close database connections
            if (::databaseManager.isInitialized) {
                databaseManager.close()
            }
            
            // Unregister PlaceholderAPI expansion
            placeholderExpansion?.unregister()
            
            logger.info("✓ NuvaPeoples arrêté proprement")
            
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Erreur lors de l'arrêt de NuvaPeoples", e)
        }
    }
    
    /**
     * Initialize plugin configuration
     */
    private fun initializeConfiguration() {
        // Save default config if not exists
        saveDefaultConfig()
        
        logger.info("Configuration chargée")
    }
    
    /**
     * Initialize database connection
     */
    private fun initializeDatabase() {
        databaseManager = DatabaseManager(this)
        
        databaseManager.initialize().thenAccept { success ->
            if (success) {
                logger.info("✓ Base de données initialisée")
            } else {
                logger.severe("✗ Erreur d'initialisation de la base de données")
                throw RuntimeException("Database initialization failed")
            }
        }
    }
    
    /**
     * Initialize all managers
     */
    private fun initializeManagers() {
        // Initialize PassiveManager first
        passiveManager = PassiveManager(this)
        
        // Initialize ClasseManager (depends on PassiveManager)
        classeManager = ClasseManager(this)
        
        // Initialize GUIManager
        guiManager = GUIManager(this, classeManager)
        
        passiveManager.initialize()
        
        logger.info("✓ Gestionnaires initialisés")
    }
    
    /**
     * Register event listeners
     */
    private fun registerListeners() {
        server.pluginManager.registerEvents(PassiveListener(this), this)
        server.pluginManager.registerEvents(ItemListener(this), this)
        
        logger.info("✓ Listeners enregistrés")
    }
    
    /**
     * Register commands
     */
    private fun registerCommands() {
        getCommand("classe")?.setExecutor(ClasseCommand(this))
        
        logger.info("✓ Commandes enregistrées")
    }
    
    /**
     * Initialize PlaceholderAPI integration
     */
    private fun initializePlaceholderAPI() {
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = NuvaPlaceholderExpansion(this)
            if (placeholderExpansion?.register() == true) {
                logger.info("✓ PlaceholderAPI intégré")
            } else {
                logger.warning("✗ Échec d'intégration PlaceholderAPI")
            }
        } else {
            logger.warning("PlaceholderAPI non trouvé - Placeholders indisponibles")
        }
    }
    
    /**
     * Get plugin instance
     */
    companion object {
        @JvmStatic
        lateinit var instance: ClassePlugin
            private set
    }
    
    init {
        instance = this
    }
    
    /**
     * Reload plugin configuration and data
     */
    fun reloadPlugin() {
        try {
            // Reload configuration
            reloadConfig()
            
            // Reload managers
            if (::classeManager.isInitialized) {
                classeManager.reload()
            }
            
            logger.info("✓ Plugin rechargé avec succès")
            
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Erreur lors du rechargement", e)
        }
    }
}
