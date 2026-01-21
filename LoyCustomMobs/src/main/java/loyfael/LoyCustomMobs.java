package loyfael;

import loyfael.api.LoyCustomMobsAPI;
import loyfael.commands.MainCommand;
import loyfael.listeners.MobListener;
import loyfael.managers.ConfigManager;
import loyfael.managers.MobManager;
import loyfael.managers.LootManager;
import loyfael.managers.GuiManager;
import loyfael.utils.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for LoyCustomMobs
 * A modern Paper plugin for custom mobs with unique abilities and behaviors
 *
 * @author loyfael
 * @version 2.0.0
 * @since 1.21.7
 */
public class LoyCustomMobs extends JavaPlugin {

    private static LoyCustomMobs instance;

    // Managers
    private ConfigManager configManager;
    private MobManager mobManager;
    private LootManager lootManager;
    private GuiManager guiManager;

    // Metrics
    private Metrics metrics;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("Starting LoyCustomMobs v" + getPluginMeta().getVersion());

        try {
            // Initialize managers
            initializeManagers();

            // Register commands
            registerCommands();

            // Register listeners
            registerListeners();

            // Initialize metrics
            initializeMetrics();

            // Initialize API
            initializeAPI();

            getLogger().info("LoyCustomMobs has been successfully enabled!");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable LoyCustomMobs", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (mobManager != null) {
            mobManager.cleanup();
        }

        getLogger().info("LoyCustomMobs has been disabled.");
    }

    /**
     * Initialize all plugin managers
     */
    private void initializeManagers() {
        getLogger().info("Initializing managers...");

        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        lootManager = new LootManager(this);
        lootManager.initialize();

        mobManager = new MobManager(this);
        mobManager.initialize();

        guiManager = new GuiManager(this);
        guiManager.initialize();
    }

    /**
     * Register plugin commands
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");

        MainCommand mainCommand = new MainCommand(this);
        getCommand("loycustommobs").setExecutor(mainCommand);
        getCommand("loycustommobs").setTabCompleter(mainCommand);
    }

    /**
     * Register event listeners
     */
    private void registerListeners() {
        getLogger().info("Registering listeners...");

        MobListener mobListener = new MobListener(this);
        getServer().getPluginManager().registerEvents(mobListener, this);
    }

    /**
     * Initialize metrics
     */
    private void initializeMetrics() {
        try {
            metrics = new Metrics(this, 12345); // Replace with actual bStats plugin ID
            getLogger().info("Metrics initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize metrics", e);
        }
    }

    /**
     * Initialize the API
     */
    private void initializeAPI() {
        try {
            LoyCustomMobsAPI.initialize(this);
            getLogger().info("API initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize API", e);
        }
    }

    // Getters for managers
    public static LoyCustomMobs getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}
