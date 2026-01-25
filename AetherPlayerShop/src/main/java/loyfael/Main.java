package loyfael;

import loyfael.commands.ChestShopCommand;
import loyfael.config.ConfigManager;
import loyfael.database.MongoManager;
import loyfael.gui.PurchaseMenuGUI;
import loyfael.gui.ShopEditMenuGUI;
import loyfael.gui.TriumphShopMenuGUI;
import loyfael.listener.ShopInteractionListener;
import loyfael.listener.ShopCreationListener;
import loyfael.listener.ShopOwnerManager;
import loyfael.manager.ShopManager;
import loyfael.manager.CacheManager;
import loyfael.manager.TransactionManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private static Main instance;
    private Economy economy;
    private MongoManager mongoManager;
    private ConfigManager configManager;
    private ShopManager shopManager;
    private CacheManager cacheManager;
    private TransactionManager transactionManager;
    private ShopOwnerManager shopOwnerManager;
    private PurchaseMenuGUI purchaseMenuGUI;
    private ShopEditMenuGUI shopEditMenuGUI;

    @Override
    public void onEnable() {
        instance = this;

        // Configuration initialization
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // MongoDB connection
        mongoManager = new MongoManager(this);
        if (!mongoManager.connect()) {
            getLogger().severe("Impossible de se connecter à MongoDB. Arrêt du plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Economy setup (Vault)
        if (!setupEconomy()) {
            getLogger().severe("Vault non trouvé. Arrêt du plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Managers initialization
        cacheManager = new CacheManager();
        shopManager = new ShopManager(this);
        transactionManager = new TransactionManager(this);
        shopOwnerManager = new ShopOwnerManager(this);
        purchaseMenuGUI = new PurchaseMenuGUI(this);
        shopEditMenuGUI = new ShopEditMenuGUI(this);

        // Event listeners
        registerListeners();

        // Commands
        registerCommands();

        getLogger().info("AetherPlayerShop activé avec succès avec TriumphGUI!");
    }

    @Override
    public void onDisable() {
        if (mongoManager != null) {
            mongoManager.disconnect();
        }
        if (cacheManager != null) {
            cacheManager.clearCache();
        }
        getLogger().info("AetherPlayerShop désactivé.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ShopCreationListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(purchaseMenuGUI, this);
        getServer().getPluginManager().registerEvents(shopEditMenuGUI, this);
    }

    private void registerCommands() {
        getCommand("chestshop").setExecutor(new ChestShopCommand(this));
        getCommand("shopinfo").setExecutor(new TriumphShopMenuGUI(this));
        getCommand("shopdebug").setExecutor(new loyfael.commands.ShopDebugCommand(this));
    }

    // Getters
    public static Main getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public MongoManager getMongoManager() { return mongoManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public ShopManager getShopManager() { return shopManager; }
    public CacheManager getCacheManager() { return cacheManager; }
    public TransactionManager getTransactionManager() { return transactionManager; }
    public ShopOwnerManager getShopOwnerManager() { return shopOwnerManager; }
    public PurchaseMenuGUI getPurchaseMenuGUI() { return purchaseMenuGUI; }
    public ShopEditMenuGUI getShopEditMenuGUI() { return shopEditMenuGUI; }
}