package loyfael

import loyfael.commands.ChestShopCommand
import loyfael.commands.ShopDebugCommand
import loyfael.config.ConfigManager
import loyfael.database.MySqlManager
import loyfael.gui.PurchaseMenuGUI
import loyfael.gui.ShopEditMenuGUI
import loyfael.gui.ShopMenuGUI
import loyfael.gui.TriumphShopMenuGUI
import loyfael.listener.ChestStockMonitor
import loyfael.listener.ShopCreationListener
import loyfael.listener.ShopInteractionListener
import loyfael.listener.ShopOwnerManager
import loyfael.manager.CacheManager
import loyfael.manager.ShopManager
import loyfael.manager.TransactionManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin

/**
 * Plugin principal AetherPlayerShop - Version Kotlin
 * 
 * Système de shops joueur avec MySQL/Hikari, Vault et TriumphGUI
 * 
 * @author Loyfael
 * @version 2.0.0
 */
class Main : JavaPlugin() {

    companion object {
        lateinit var instance: Main
            private set
    }

    // Économie (Vault)
    lateinit var economy: net.milkbowl.vault.economy.Economy
        private set

    // Base de données
    lateinit var mySqlManager: MySqlManager
        private set

    // Configuration
    lateinit var configManager: ConfigManager
        private set

    // Managers
    lateinit var shopManager: ShopManager
        private set
    lateinit var cacheManager: CacheManager
        private set
    lateinit var transactionManager: TransactionManager
        private set
    lateinit var shopOwnerManager: ShopOwnerManager
        private set

    // GUIs
    lateinit var purchaseMenuGUI: PurchaseMenuGUI
        private set
    lateinit var shopEditMenuGUI: ShopEditMenuGUI
        private set
    lateinit var shopMenuGUI: ShopMenuGUI
        private set

    override fun onEnable() {
        instance = this

        // Initialisation de la configuration
        configManager = ConfigManager(this)
        configManager.loadConfig()

        // Connexion MySQL (Hikari)
        mySqlManager = MySqlManager(this)
        if (!mySqlManager.connect()) {
            logger.severe("Impossible de se connecter à MySQL. Arrêt du plugin.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Configuration de l'économie (Vault)
        if (!setupEconomy()) {
            logger.severe("Vault non trouvé. Arrêt du plugin.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Initialisation des managers
        cacheManager = CacheManager()
        shopManager = ShopManager(this)
        transactionManager = TransactionManager(this)
        shopOwnerManager = ShopOwnerManager(this)
        purchaseMenuGUI = PurchaseMenuGUI(this)
        shopEditMenuGUI = ShopEditMenuGUI(this)
        shopMenuGUI = ShopMenuGUI(this)

        // Enregistrement des listeners
        registerListeners()

        // Enregistrement des commandes
        registerCommands()

        logger.info("AetherPlayerShop activé avec succès avec TriumphGUI!")
    }

    override fun onDisable() {
        if (::mySqlManager.isInitialized) {
            mySqlManager.disconnect()
        }
        if (::cacheManager.isInitialized) {
            cacheManager.clearCache()
        }
        logger.info("AetherPlayerShop désactivé.")
    }

    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }
        val rsp: RegisteredServiceProvider<net.milkbowl.vault.economy.Economy>? = 
            server.servicesManager.getRegistration(net.milkbowl.vault.economy.Economy::class.java)
        if (rsp == null) {
            return false
        }
        economy = rsp.getProvider()
        return true
    }

    private fun registerListeners() {
        val pm = server.pluginManager
        pm.registerEvents(ShopCreationListener(this), this)
        pm.registerEvents(ShopInteractionListener(this), this)
        pm.registerEvents(ChestStockMonitor(this), this)
        pm.registerEvents(shopOwnerManager, this)
        pm.registerEvents(purchaseMenuGUI, this)
        pm.registerEvents(shopEditMenuGUI, this)
        pm.registerEvents(shopMenuGUI, this)
    }

    private fun registerCommands() {
        getCommand("chestshop")?.setExecutor(ChestShopCommand(this))
        getCommand("shopinfo")?.setExecutor(TriumphShopMenuGUI(this))
        getCommand("shopdebug")?.setExecutor(ShopDebugCommand(this))
    }
}
