package loyfael;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import net.milkbowl.vault.economy.Economy;

import java.util.Map;

// Import of the new pure SOLID architecture
import loyfael.api.interfaces.*;
import loyfael.core.ServiceContainer;
import loyfael.core.services.*;
import loyfael.listeners.ImprovedEventListener;
import loyfael.gui.services.ModernGuiService;
import loyfael.utils.Utils;
import loyfael.utils.hooks.PlaceholderAPIHook;

/**
 * Main class with 100% pure SOLID architecture
 * Single responsibility: services orchestration
 * Dependency inversion: everything goes through interfaces
 */
public final class Main extends JavaPlugin {

    private static Main instance;

    // SOLID services container
    private ServiceContainer serviceContainer;

    // Vault economic support
    private Economy economy = null;

    // PlaceholderAPI hook
    private PlaceholderAPIHook placeholderHook;

    // Tâche de sauvegarde périodique
    private int saveTaskId = -1;

    // Main services (access via interfaces only)
    private IConfigurationService configurationService;
    private IDatabaseService databaseService;
    private ICacheService cacheService;
    private IPlayerService playerService;
    private INotificationService notificationService;
    private IMissionService missionService;
    private IGuiService guiService;
    private ILevelsConfigService levelsConfigService;

    @Override
    public void onEnable() {
        instance = this;

        try {
            // 1. Initialize services container
            initializeServiceContainer();

            // 2. Initialize Vault economy
            setupEconomy();

            // 3. Register all services
            registerAllServices();

            // 4. Initialize services in dependency order
            serviceContainer.initializeServices();

            // 5. Start synchronization service now that all services are ready
            ISynchronizationService syncService = serviceContainer.getService(ISynchronizationService.class);
            if (syncService != null) {
                syncService.start();
            }

            // 6. Create default resources
            createDefaultResources();

            // 7. Register commands and listeners
            registerCommandsAndListeners();

            // 8. Initialize PlaceholderAPI hook
            initializePlaceholderAPI();

            // 9. Start periodic save task if configured
            startPeriodicSaveTask();

            Utils.sendConsoleLog("&aKrakenLevels enabled successfully!");

        } catch (Exception e) {
            Utils.sendConsoleLog("&cCritical error during enable: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            // Stop the periodic save task
            stopPeriodicSaveTask();

            // Save all modified data before shutdown
            if (missionService instanceof MissionService) {
                ((MissionService) missionService).saveAllModifiedData();
            }

            // Proper shutdown of all services
            if (serviceContainer != null) {
                serviceContainer.shutdownServices();
            }
        } catch (Exception e) {
            Utils.sendConsoleLog("&cError during shutdown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            instance = null;
        }
    }

    /**
     * Initialize IoC services container
     */
    private void initializeServiceContainer() {
        serviceContainer = new ServiceContainer(getLogger());
    }

    /**
     * Register all services in IoC container
     * Respects dependency order for inversion of control
     */
    private void registerAllServices() {
        // 1. Base services (no dependencies)
        configurationService = new ConfigurationService();
        serviceContainer.registerService(IConfigurationService.class, configurationService);

        cacheService = new CacheService();
        serviceContainer.registerService(ICacheService.class, cacheService);

        // 2. Services with basic dependencies
        databaseService = createOptimalDatabaseService();
        serviceContainer.registerService(IDatabaseService.class, databaseService);

        notificationService = new NotificationService(configurationService);
        serviceContainer.registerService(INotificationService.class, notificationService);

        // 2.5. Levels configuration service (reads levels.yml)
        levelsConfigService = new LevelsConfigService();
        serviceContainer.registerService(ILevelsConfigService.class, levelsConfigService);

        // 3. Business services (depend on base services)
        playerService = new PlayerService(databaseService, cacheService);
        serviceContainer.registerService(IPlayerService.class, playerService);

        // 4. Complex services (depend on business services)
        missionService = new MissionService(playerService, notificationService, levelsConfigService);
        serviceContainer.registerService(IMissionService.class, missionService);

        // 5. Modern GUI services (depend on business services)
        guiService = new loyfael.gui.services.ModernGuiService(playerService, notificationService, levelsConfigService);
        serviceContainer.registerService(IGuiService.class, guiService);

        // 6. Synchronization service (depends on database, cache, and configuration)
        ISynchronizationService synchronizationService = new loyfael.core.services.SynchronizationService(
            databaseService, cacheService, configurationService);
        serviceContainer.registerService(ISynchronizationService.class, synchronizationService);
    }

    /**
     * Create optimal database service according to configuration
     * Open/closed principle: extensible without modification
     */
    private IDatabaseService createOptimalDatabaseService() {
        // Temporary use of Bukkit config for bootstrap
        boolean useMongoDB = getConfig().getBoolean("database.use-mongodb", true);

        if (useMongoDB) {
            return new MongoDatabaseService(configurationService);
        } else {
            return new YamlDatabaseService(configurationService);
        }
    }

    /**
     * Create default resource files
     */
    private void createDefaultResources() {
        try {
            // Main config
            saveDefaultConfig();

            // Data files
            String[] resourceFiles = {
                "messages.yml",
                "levels.yml",
                "materials.yml"
            };

            for (String fileName : resourceFiles) {
                if (!new java.io.File(getDataFolder(), fileName).exists()) {
                    saveResource(fileName, false);
                }
            }

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError creating resources: " + e.getMessage());
            throw new RuntimeException("Unable to create necessary resources", e);
        }
    }

    /**
     * Register commands and listeners with new architecture
     */
    private void registerCommandsAndListeners() {
        PluginManager pm = getServer().getPluginManager();

        // SOLID Commands
        getCommand("mission").setExecutor(new loyfael.commands.MissionCMD());
        getCommand("mission").setTabCompleter(new loyfael.commands.MissionCMD());

        // SOLID Listeners - Complete modern architecture
        pm.registerEvents(new ImprovedEventListener(), this);
        pm.registerEvents(new loyfael.listeners.ModernGuiListener(), this);
        pm.registerEvents(new loyfael.listeners.SynchronizationListener(), this);
    }

    /**
     * Initialize PlaceholderAPI hook
     */
    private void initializePlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        placeholderHook = new PlaceholderAPIHook(this);
        placeholderHook.register();
    }

    // ====================================
    // SERVICES ACCESS (SOLID ARCHITECTURE)
    // ====================================

    public static Main getInstance() {
        return instance;
    }

    /**
     * IoC container access for extensions
     */
    public ServiceContainer getServiceContainer() {
        return serviceContainer;
    }

    /**
     * Main services - Direct access via interfaces
     */
    public IConfigurationService getConfigurationService() {
        return configurationService;
    }

    public IDatabaseService getDatabaseService() {
        return databaseService;
    }

    public ICacheService getCacheService() {
        return cacheService;
    }

    public IPlayerService getPlayerService() {
        return playerService;
    }

    public INotificationService getNotificationService() {
        return notificationService;
    }

    public IMissionService getMissionService() {
        return missionService;
    }

    public IGuiService getGuiService() {
        return guiService;
    }

    public ISynchronizationService getSynchronizationService() {
        return serviceContainer.getService(ISynchronizationService.class);
    }

    /**
     * API publique pour les autres plugins
     * Méthode sécurisée pour ouvrir la GUI des niveaux depuis un autre plugin
     * 
     * @param player Le joueur pour qui ouvrir la GUI
     */
    public static void openLevelsGUI(Player player) {
        openLevelsGUI(player, 0);
    }

    /**
     * API publique pour les autres plugins
     * Méthode sécurisée pour ouvrir la GUI des niveaux depuis un autre plugin
     * 
     * @param player Le joueur pour qui ouvrir la GUI
     * @param page La page à afficher (0 = première page)
     */
    public static void openLevelsGUI(Player player, int page) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        Main instance = getInstance();
        if (instance == null) {
            throw new IllegalStateException("KrakenLevels plugin is not loaded");
        }

        // Ensure the operation runs on the main thread
        Runnable openTask = () -> {
            try {
                IGuiService guiService = instance.getGuiService();
                if (guiService == null) {
                    player.sendMessage("§cError: GUI service not available");
                    return;
                }

                // Close any existing GUI before opening the new one
                if (guiService instanceof ModernGuiService) {
                    ((ModernGuiService) guiService).cleanupPlayerGui(player);
                }

                // Wait one tick to ensure cleanup is done
                Bukkit.getScheduler().runTaskLater(instance, () -> {
                    try {
                        Map<String, Object> parameters = new java.util.HashMap<>();
                        parameters.put("page", Math.max(0, page));
                        guiService.openGui(player, "levels", parameters);
                    } catch (Exception e) {
                        instance.getLogger().warning("Error opening levels GUI for " + player.getName() + ": " + e.getMessage());
                        player.sendMessage("§cError opening the levels menu");
                    }
                }, 1L);

            } catch (Exception e) {
                instance.getLogger().severe("Critical error in openLevelsGUI: " + e.getMessage());
                player.sendMessage("§cCritical error while opening the levels menu");
            }
        };

        if (Bukkit.isPrimaryThread()) {
            openTask.run();
        } else {
            Bukkit.getScheduler().runTask(instance, openTask);
        }
    }

    public ILevelsConfigService getLevelsConfigService() {
        return levelsConfigService;
    }

    /**
     * Initialize Vault economy
     */
    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }

        economy = rsp.getProvider();
    }

    /**
     * Return Vault economy instance
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Starts the periodic save task if configured
     */
    private void startPeriodicSaveTask() {
        if (configurationService == null) return;

        String saveMode = configurationService.getConfig().getString("system.save-strategy.mode", "immediate");
        if (!"delayed".equals(saveMode)) return;

        int intervalSeconds = configurationService.getConfig().getInt("system.save-strategy.interval", 0);
        if (intervalSeconds <= 0) return; // Pas de sauvegarde périodique, seulement à la déconnexion

    // Convert to ticks (20 ticks = 1 second)
        long intervalTicks = intervalSeconds * 20L;

        saveTaskId = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (missionService instanceof MissionService) {
                ((MissionService) missionService).saveAllModifiedData();
            }
        }, intervalTicks, intervalTicks).getTaskId();

        Utils.sendConsoleLog("&ePeriodic save task started (interval: " + intervalSeconds + "s)");
    }

    /**
     * Stops the periodic save task
     */
    private void stopPeriodicSaveTask() {
        if (saveTaskId != -1) {
            getServer().getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
            Utils.sendConsoleLog("&ePeriodic save task stopped");
        }
    }
}
