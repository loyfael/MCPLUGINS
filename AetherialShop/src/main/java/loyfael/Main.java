package loyfael;

import loyfael.interfaces.IEconomyService;
import loyfael.interfaces.IItemService;
import loyfael.interfaces.IShopRepository;
import loyfael.interfaces.IShopService;
import loyfael.services.ItemService;
import loyfael.services.ShopRepository;
import loyfael.services.ShopService;
import loyfael.services.VaultEconomyService;
import loyfael.services.DiscordService;
import loyfael.database.SQLiteDatabase;
import loyfael.cache.ShopCache;
import loyfael.commands.AdminCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private static Main instance;
    private IEconomyService economyService;
    private IShopService shopService;
    private ShopCache cache;
    private SQLiteDatabase database;
    private DiscordService discordService;

    @Override
    public void onEnable() {
        instance = this;

        try {
            getLogger().info("Démarrage AetherialShop en cours..");

            // Initialiser le cache en premier
            cache = new ShopCache(getLogger());
            getLogger().info("✓ Cache haute performance initialisé");

            if (!setupEconomy()) {
                getLogger().severe("ERREUR CRITIQUE: Vault n'est pas installé ou aucun plugin d'économie trouvé!");
                getLogger().severe("Plugins requis: Vault + un plugin d'économie (ex: EssentialsX)");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            saveDefaultConfig();
            if (!new File(getDataFolder(), "items.yml").exists()) {
                saveResource("items.yml", false);
            }

            // Initialiser la base de données SQLite
            database = new SQLiteDatabase(this);
            getLogger().info("✓ Base de données SQLite initialisée");

            // Initialiser Discord
            discordService = new DiscordService(this);
            if (discordService.isEnabled()) {
                getLogger().info("✓ Service Discord activé");
            } else {
                getLogger().info("⚠ Service Discord désactivé (vérifiez config.yml)");
            }

            initializeServices();

            registerCommandsAndEvents();

            shopService.startRotationScheduler();

            getLogger().info("AetherialShop à été activé avec succès.");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "ERREUR FATALE lors de l'activation du plugin!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            getLogger().info("=== ARRÊT AETHERIALSHOP OPTIMISÉ ===");

            if (shopService != null) {
                getLogger().info("Sauvegarde des données du shop...");
                // La sauvegarde sera gérée automatiquement par les services
            }

            // Fermer la base de données
            if (database != null) {
                database.close();
                getLogger().info("✓ Base de données fermée proprement");
            }

            // Fermer le cache proprement
            if (cache != null) {
                cache.shutdown();
                getLogger().info("✓ Cache fermé proprement");
            }

            getLogger().info("=== AETHERIALSHOP OPTIMISÉ DÉSACTIVÉ ===");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erreur lors de la désactivation du plugin", e);
        }
    }

    private void initializeServices() {
        try {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                throw new RuntimeException("Service économique Vault non trouvé");
            }

            Economy vaultEconomy = rsp.getProvider();
            if (vaultEconomy == null) {
                throw new RuntimeException("Provider économique Vault null");
            }

            getLogger().info("Provider économique: " + vaultEconomy.getName());

            // Créer les services avec cache intégré
            economyService = new VaultEconomyService(vaultEconomy, cache);
            IItemService itemService = new ItemService(this);
            IShopRepository shopRepository = new ShopRepository(this, cache);
            shopService = new ShopService(this, itemService, shopRepository, economyService);

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erreur lors de l'initialisation des services", e);
            throw new RuntimeException("Impossible d'initialiser les services", e);
        }
    }

    private void registerCommandsAndEvents() {
        try {
            if (getCommand("dailyshop") == null) {
                throw new RuntimeException("Commande 'dailyshop' non trouvée dans plugin.yml");
            }
            
            if (getCommand("ashop") == null) {
                throw new RuntimeException("Commande 'ashop' non trouvée dans plugin.yml");
            }

            getCommand("dailyshop").setExecutor(new ShopCommand(shopService));
            getCommand("ashop").setExecutor(new AdminCommand(shopService, discordService, getLogger()));
            getServer().getPluginManager().registerEvents(new ShopGUI(shopService, economyService), this);
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erreur lors de l'enregistrement des commandes/événements", e);
            throw new RuntimeException("Impossible d'enregistrer les commandes/événements", e);
        }
    }

    private boolean setupEconomy() {
        try {
            if (getServer().getPluginManager().getPlugin("Vault") == null) {
                getLogger().severe("Plugin Vault non trouvé!");
                return false;
            }

            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                getLogger().severe("Aucun service économique enregistré!");
                getLogger().severe("Veuillez installer un plugin d'économie (EssentialsX, CMI, etc.)");
                return false;
            }

            getLogger().info("✓ Service économique trouvé: " + rsp.getProvider().getName());
            return rsp.getProvider() != null;

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erreur lors de la configuration de l'économie", e);
            return false;
        }
    }

    public static Main getInstance() {
        return instance;
    }

    public IEconomyService getEconomyService() {
        return economyService;
    }

    public IShopService getShopService() {
        return shopService;
    }
    
    public SQLiteDatabase getDatabase() {
        return database;
    }
    
    public DiscordService getDiscordService() {
        return discordService;
    }
}