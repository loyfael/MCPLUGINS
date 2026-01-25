package loyfael

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import loyfael.commands.AdminCommandHandler
import loyfael.commands.CommandeCommandHandler
import loyfael.commands.GUICommandHandler
import loyfael.commands.LivraisonCommandHandler
import loyfael.gui.GUIManager
import loyfael.managers.*
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

/**
 * Classe principale du plugin AetherPlayerDelivery
 * Plugin de commandes et livraisons entre joueurs pour PaperMC 1.21.8
 */
class AetherPlayerDelivery : JavaPlugin() {
    
    companion object {
        /**
         * Instance statique du plugin pour accès global
         */
        lateinit var instance: AetherPlayerDelivery
            private set
    }
    
    // === Managers ===
    lateinit var configManager: ConfigManager
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var economieManager: EconomieManager
        private set
    lateinit var notificationManager: NotificationManager
        private set
    lateinit var commandeManager: CommandeManager
        private set
    lateinit var livraisonManager: LivraisonManager
        private set
    lateinit var schedulerManager: SchedulerManager
        private set
    lateinit var guiManager: GUIManager
        private set
    
    // === Command Handlers ===
    private lateinit var guiCommandHandler: GUICommandHandler
    private lateinit var commandeCommandHandler: CommandeCommandHandler
    private lateinit var livraisonCommandHandler: LivraisonCommandHandler
    private lateinit var adminCommandHandler: AdminCommandHandler
    
    /**
     * Méthode appelée lors de l'activation du plugin
     */
    override fun onEnable() {
        // Initialisation de l'instance statique
        instance = this
        
        logger.info("§a=== Démarrage de AetherPlayerDelivery ===")
        logger.info("§7Version: ${pluginMeta.version}")
        logger.info("§7Serveur: ${server.name} ${server.version}")
        
        try {
            // Initialisation de la configuration
            initializeConfig()
            
            // Vérification du mode serveur
            val mode = configManager.getServerMode()
            logger.info("§7Mode serveur: §e$mode")
            
            // Initialisation des managers de base
            initializeBaseManagers()
            
            if (configManager.isPrincipalMode()) {
                // Mode principal : toutes les fonctionnalités
                logger.info("§7Initialisation en mode §ePRINCIPAL")
                initializePrincipalMode()
            } else {
                // Mode satellite : lecture seule
                logger.info("§7Initialisation en mode §eSATELLITE")
                initializeSatelliteMode()
            }
            
            // Initialisation de l'interface utilisateur
            initializeGUI()
            
            // Enregistrement des commandes (après GUI)
            registerCommands()
            initializeGUI()
            
            logger.info("§aAetherPlayerDelivery activé avec succès !")
            logger.info("§7Utilisez /commande ou /livraison pour commencer")
            
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Erreur lors de l'activation du plugin !", e)
            isEnabled = false
            return
        }
    }
    
    /**
     * Méthode appelée lors de la désactivation du plugin
     */
    override fun onDisable() {
        logger.info("§c=== Arrêt de AetherPlayerDelivery ===")
        
        try {
            // Arrêter le scheduler en premier
            if (::schedulerManager.isInitialized) {
                schedulerManager.shutdown()
            }
            
            // Fermer les autres managers
            if (::databaseManager.isInitialized) {
                databaseManager.shutdown()
            }
            
            if (::economieManager.isInitialized) {
                economieManager.shutdown()
            }
            
            if (::notificationManager.isInitialized) {
                notificationManager.shutdown()
            }
            
            if (::commandeManager.isInitialized) {
                commandeManager.shutdown()
            }
            
            if (::livraisonManager.isInitialized) {
                livraisonManager.shutdown()
            }
            
            logger.info("§cAetherPlayerDelivery désactivé avec succès !")
            
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Erreur lors de la désactivation du plugin !", e)
        }
    }
    
    /**
     * Initialise la configuration
     */
    private fun initializeConfig() {
        configManager = ConfigManager(this)
        configManager.initialize()
    }
    
    /**
     * Initialise les managers de base (communs aux deux modes)
     */
    private fun initializeBaseManagers() {
        runBlocking {
            // Base de données (obligatoire pour les deux modes)
            databaseManager = DatabaseManager(this@AetherPlayerDelivery)
            databaseManager.initialize()
            
            // Notifications
            notificationManager = NotificationManager(this@AetherPlayerDelivery)
        }
    }
    
    /**
     * Initialise le mode principal (serveur de gestion)
     */
    private fun initializePrincipalMode() {
        runBlocking {
            // Économie (Vault)
            economieManager = EconomieManager(this@AetherPlayerDelivery)
            if (!economieManager.initialize()) {
                throw RuntimeException("Impossible d'initialiser l'économie (Vault)")
            }
            
            // Managers métier
            commandeManager = CommandeManager(this@AetherPlayerDelivery, databaseManager, economieManager, notificationManager)
            livraisonManager = LivraisonManager(this@AetherPlayerDelivery, databaseManager, economieManager, notificationManager)
            
            // Scheduler (tâches périodiques)
            schedulerManager = SchedulerManager(this@AetherPlayerDelivery, commandeManager, livraisonManager, notificationManager)
            schedulerManager.startScheduledTasks()
        }
        
        logger.info("§aMode principal initialisé - Toutes les fonctionnalités activées")
    }
    
    /**
     * Initialise le mode satellite (serveur de notifications uniquement)
     */
    private fun initializeSatelliteMode() {
        // En mode satellite, on n'initialise que les managers nécessaires aux notifications
        // Les managers d'économie et de logique métier ne sont pas nécessaires
        
        logger.info("§aMode satellite initialisé - Notifications uniquement")
        logger.info("§7Les commandes de gestion sont désactivées en mode satellite")
    }
    
    /**
     * Enregistre les commandes du plugin
     */
    private fun registerCommands() {
        // Commandes toujours disponibles
        adminCommandHandler = AdminCommandHandler(this, if (::schedulerManager.isInitialized) schedulerManager else null!!)
        getCommand("aetherdelivery")?.setExecutor(adminCommandHandler)
        getCommand("aetherdelivery")?.tabCompleter = adminCommandHandler
        
        if (configManager.isPrincipalMode()) {
            // Commandes disponibles uniquement en mode principal
            guiCommandHandler = GUICommandHandler(this)
            commandeCommandHandler = CommandeCommandHandler(this, guiManager, commandeManager)
            livraisonCommandHandler = LivraisonCommandHandler(this, guiManager, livraisonManager)
            
            getCommand("delivery")?.setExecutor(guiCommandHandler)
            getCommand("commande")?.setExecutor(commandeCommandHandler)
            getCommand("commande")?.tabCompleter = commandeCommandHandler
            
            getCommand("livraison")?.setExecutor(livraisonCommandHandler)
            getCommand("livraison")?.tabCompleter = livraisonCommandHandler
            
            logger.info("§7Commandes client et livreur enregistrées")
        } else {
            logger.info("§7Mode satellite: Commandes client/livreur désactivées")
        }
        
        logger.info("§7Commandes d'administration enregistrées")
    }
    
    /**
     * Initialise l'interface graphique
     */
    private fun initializeGUI() {
        if (configManager.isPrincipalMode()) {
            guiManager = GUIManager(this)
            logger.info("§7Interface graphique initialisée")
        }
    }
}
