package loyfael;

import loyfael.commands.SafeModeCommand;
import loyfael.database.DatabaseManager;
import loyfael.gui.ConfirmationGUI;
import loyfael.hooks.LandsHook;
import loyfael.hooks.WorldGuardHook;
import loyfael.listeners.*;
import loyfael.managers.ConfigManager;
import loyfael.managers.SafeModeManager;
import loyfael.placeholders.AetherSafeModeExpansion;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for AetherSafeMode plugin
 * Provides SafeMode functionality with WorldGuard and Lands integration
 * Optimized for performance and proper resource management
 */
public class Main extends JavaPlugin {

    // Singleton instance for global access
    private static Main instance;

    // Core managers - initialized in proper order
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private SafeModeManager safeModeManager;

    // Optional hooks for external plugins
    private WorldGuardHook worldGuardHook;
    private LandsHook landsHook;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("§7Initialisation d'AetherSafeMode...");

        // Initialize managers in proper dependency order
        initializeManagers();

        // Register commands and listeners
        registerCommands();
        registerListeners();

        // Initialize optional hooks
        initializeHooks();

        // Initialize PlaceholderAPI if available
        initializePlaceholderAPI();

        getLogger().info("§a✓ AetherSafeMode activé avec succès !");
        getLogger().info("§7Version: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        getLogger().info("§7Arrêt d'AetherSafeMode...");

        // Save all player data before shutdown
        if (safeModeManager != null) {
            safeModeManager.saveAllPlayers();
        }

        // Close database connections
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }

        getLogger().info("§c✗ AetherSafeMode désactivé.");
        instance = null;
    }

    /**
     * Initialize all managers in proper order
     */
    private void initializeManagers() {
        try {
            // Config must be first
            configManager = new ConfigManager(this);
            getLogger().info("§7- Configuration chargée");

            // Database depends on config
            databaseManager = new DatabaseManager(this);
            getLogger().info("§7- Base de données initialisée");

            // SafeMode manager depends on database
            safeModeManager = new SafeModeManager(this);
            getLogger().info("§7- Gestionnaire de modes initialisé");

        } catch (Exception e) {
            getLogger().severe("§cErreur lors de l'initialisation des managers: " + e.getMessage());
            e.printStackTrace();
            setEnabled(false);
        }
    }

    /**
     * Register plugin commands
     */
    private void registerCommands() {
        SafeModeCommand safeModeCommand = new SafeModeCommand(this);
        getCommand("safemode").setExecutor(safeModeCommand);
        getCommand("safemode").setTabCompleter(safeModeCommand);
        getLogger().info("§7- Commandes enregistrées");
    }

    /**
     * Register event listeners
     */
    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
        pm.registerEvents(new PlayerCombatListener(this), this);
        pm.registerEvents(new PlayerDeathListener(this), this);

        getLogger().info("§7- Listeners enregistrés");
    }

    /**
     * Initialize optional plugin hooks
     */
    private void initializeHooks() {
        // WorldGuard integration
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                worldGuardHook = new WorldGuardHook();
                getLogger().info("§7- Hook WorldGuard activé");
            } catch (Exception e) {
                getLogger().warning("§eImpossible d'initialiser WorldGuard: " + e.getMessage());
            }
        }

        // Lands integration
        if (getServer().getPluginManager().getPlugin("Lands") != null) {
            try {
                landsHook = new LandsHook(getServer().getPluginManager().getPlugin("Lands"));
                getLogger().info("§7- Hook Lands activé");
            } catch (Exception e) {
                getLogger().warning("§eImpossible d'initialiser Lands: " + e.getMessage());
            }
        }
    }

    /**
     * Initialize PlaceholderAPI integration if available
     */
    private void initializePlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AetherSafeModeExpansion(this).register();
            getLogger().info("§7- PlaceholderAPI intégré");
        }
    }

    // Getters for managers and hooks
    public static Main getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SafeModeManager getSafeModeManager() {
        return safeModeManager;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public LandsHook getLandsHook() {
        return landsHook;
    }

    public boolean hasWorldGuard() {
        return worldGuardHook != null;
    }

    public boolean hasLands() {
        return landsHook != null;
    }
}
