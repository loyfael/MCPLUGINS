package loyfael.litefish;

import loyfael.litefish.commands.LiteFishCommand;
import loyfael.litefish.config.ConfigManager;
import loyfael.litefish.events.FishingListener;
import loyfael.litefish.hooks.NexoHook;
import loyfael.litefish.hooks.VaultHook;
import loyfael.litefish.hooks.WorldGuardHook;
import loyfael.litefish.managers.BiomeManager;
import loyfael.litefish.managers.DropManager;
import loyfael.litefish.managers.EconomyManager;
import loyfael.litefish.managers.PlayerDataManager;
import loyfael.litefish.managers.TreasureManager;
import loyfael.litefish.mechanics.LavaFishing;
import loyfael.litefish.mechanics.VoidFishing;
import loyfael.litefish.minigame.FishingMiniGame;
import loyfael.litefish.minigame.VoidFishingMiniGame;
import loyfael.litefish.seasons.SeasonManager;
import loyfael.litefish.tournament.TournamentManager;
import loyfael.litefish.utils.MessageUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * LiteFish - Simplified Advanced Fishing Plugin
 * 
 * Features:
 * - Custom fishing drops by biome
 * - Economy integration (Vault)
 * - WorldGuard region support
 * - Nexo items integration
 * - Tournament system
 * - Custom baits and rods
 * 
 * @author Loyfael (based on original by Azlagor)
 * @version 1.0.0
 */
public class LiteFish extends JavaPlugin {
    
    private static LiteFish instance;
    
    // Core managers
    private ConfigManager configManager;
    private BiomeManager biomeManager;
    private DropManager dropManager;
    private PlayerDataManager playerDataManager;
    private EconomyManager economyManager;
    private TournamentManager tournamentManager;
    private SeasonManager seasonManager;
    private TreasureManager treasureManager;
    
    // Event listeners
    private FishingListener fishingListener;
    private FishingMiniGame fishingMiniGame;
    private VoidFishingMiniGame voidFishingMiniGame;
    
    // Plugin hooks
    private VaultHook vaultHook;
    private WorldGuardHook worldGuardHook;
    private NexoHook nexoHook;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize configuration
        if (!initializeConfig()) {
            getLogger().severe("Failed to initialize configuration!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize managers
        initializeManagers();
        
        // Setup plugin hooks
        setupHooks();
        
        // Register events and commands
        registerEvents();
        registerCommands();
        
        // Startup message
        getLogger().info("LiteFish v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info("Hooked into: " + getHookedPlugins());
    }
    
    @Override
    public void onDisable() {
        // Save player data
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        
        // Cleanup mini-game
        if (fishingMiniGame != null) {
            fishingMiniGame.disable();
        }
        
        // Cleanup void mini-game - SYSTÈME SÉPARÉ
        if (voidFishingMiniGame != null) {
            voidFishingMiniGame.shutdown();
        }
        
        // Cleanup special fishing mechanics
        LavaFishing.stopAll();
        VoidFishing.stopAll();
        
        // Cleanup hooks
        if (vaultHook != null) vaultHook.disable();
        if (worldGuardHook != null) worldGuardHook.disable();
        if (nexoHook != null) nexoHook.disable();
        
        getLogger().info("LiteFish has been disabled!");
    }
    
    private boolean initializeConfig() {
        try {
            configManager = new ConfigManager(this);
            configManager.loadConfigs();
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing configuration", e);
            return false;
        }
    }
    
    private void initializeManagers() {
        biomeManager = new BiomeManager(this);
        dropManager = new DropManager(this);
        playerDataManager = new PlayerDataManager(this);
        economyManager = new EconomyManager(this);
        tournamentManager = new TournamentManager(this);
        seasonManager = new SeasonManager();
        treasureManager = new TreasureManager(this);
    }
    
    private void setupHooks() {
        // Vault integration
        vaultHook = new VaultHook(this);
        if (vaultHook.setupEconomy()) {
            getLogger().info("Hooked into Vault for economy support");
        }
        
        // WorldGuard integration
        worldGuardHook = new WorldGuardHook(this);
        if (worldGuardHook.isEnabled()) {
            getLogger().info("Hooked into WorldGuard for region support");
        }
        
        // Nexo integration
        nexoHook = new NexoHook(this);
        if (nexoHook.isEnabled()) {
            getLogger().info("Hooked into Nexo for custom items support");
        }
    }
    
    private void registerEvents() {
        fishingListener = new FishingListener(this);
        fishingMiniGame = new FishingMiniGame(this);
        voidFishingMiniGame = new VoidFishingMiniGame(this); // SYSTÈME VOID SÉPARÉ
        getServer().getPluginManager().registerEvents(fishingListener, this);
        getServer().getPluginManager().registerEvents(new loyfael.litefish.gui.GUIListener(), this);
        getServer().getPluginManager().registerEvents(new loyfael.litefish.events.TreasureListener(this), this);
    }
    
    private void registerCommands() {
        LiteFishCommand commandExecutor = new LiteFishCommand(this);
        getCommand("lfish").setExecutor(commandExecutor);
        getCommand("lfish").setTabCompleter(commandExecutor);
    }
    
    private String getHookedPlugins() {
        StringBuilder hooks = new StringBuilder();
        if (vaultHook.isEnabled()) hooks.append("Vault ");
        if (worldGuardHook.isEnabled()) hooks.append("WorldGuard ");
        if (nexoHook.isEnabled()) hooks.append("Nexo ");
        return hooks.length() > 0 ? hooks.toString().trim() : "None";
    }
    
    // Getters
    public static LiteFish getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public BiomeManager getBiomeManager() {
        return biomeManager;
    }
    
    public DropManager getDropManager() {
        return dropManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public FishingListener getFishingListener() {
        return fishingListener;
    }
    
    public FishingMiniGame getFishingMiniGame() {
        return fishingMiniGame;
    }
    
    public VoidFishingMiniGame getVoidFishingMiniGame() {
        return voidFishingMiniGame;
    }
    
    public VaultHook getVaultHook() {
        return vaultHook;
    }
    
    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }
    
    public NexoHook getNexoHook() {
        return nexoHook;
    }
    
    public TournamentManager getTournamentManager() {
        return tournamentManager;
    }
    
    public SeasonManager getSeasonManager() {
        return seasonManager;
    }
    
    public TreasureManager getTreasureManager() {
        return treasureManager;
    }
    
    public NamespacedKey getKey(String key) {
        return new NamespacedKey(this, key);
    }
    
    public void reload() {
        // Reload configuration
        configManager.reloadConfigs();
        
        // Reload managers
        biomeManager.reload();
        dropManager.reload();
        treasureManager.reload();
        
        MessageUtils.sendConsole("&aLiteFish has been reloaded!");
    }
}
