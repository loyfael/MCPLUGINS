package loyfael.antiVillagerLag;

import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import loyfael.antiVillagerLag.commands.OptimizeCommand;
import loyfael.antiVillagerLag.commands.ReloadCommand;
import loyfael.antiVillagerLag.commands.RemoveChangesCommand;
import loyfael.antiVillagerLag.commands.UnoptimizeCommand;
import loyfael.antiVillagerLag.commands.StatusCommand;
import loyfael.antiVillagerLag.commands.InfoCommand;
import loyfael.antiVillagerLag.commands.MendingCommand;
import loyfael.antiVillagerLag.events.EventListener;
import loyfael.antiVillagerLag.utils.VillagerUtilities;
import loyfael.antiVillagerLag.utils.TaskManager;
import loyfael.antiVillagerLag.utils.VillagerCache;

import java.io.File;
import java.io.IOException;

public final class AntiVillagerLag extends JavaPlugin {

    @Override
    public void onEnable() {

        // Performance optimization initialization
        VillagerUtilities.initializeKeys(this);
        TaskManager.initialize(this);

        // Command Registration
        getCommand("avlreload").setExecutor(new ReloadCommand(this));
        getCommand("avloptimize").setExecutor(new OptimizeCommand(this));
        getCommand("avlunoptimize").setExecutor(new UnoptimizeCommand(this));
        getCommand("avlremove").setExecutor(new RemoveChangesCommand(this));
        getCommand("avlstatus").setExecutor(new StatusCommand(this));
        getCommand("avlinfo").setExecutor(new InfoCommand(this));

        // Event Registration
        EventListener eventListener = new EventListener(this);
        getServer().getPluginManager().registerEvents(eventListener, this);
        
        // Register Mending Command
        getCommand("mending").setExecutor(new MendingCommand(this, eventListener));

        // Config Stuff
        saveDefaultConfig();
        updateConfig();

        TaskManager.runAsync(() -> {
            VillagerUtilities.updateNameTags(this);
            VillagerUtilities.updateStandingOnBlocks(this);
            VillagerUtilities.updateWorkstationBlocks(this);
            VillagerUtilities.updateRestockTimes(this);
        }).thenRun(() -> {
            getLogger().info("AntiVillagerLag optimizations loaded - ready for 2000+ villagers!");

            // START the optimized automatic restock system
            startAutomaticRestockSystem();
        });

        // Bstats Code
        int pluginId = 15890;
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        TaskManager.shutdown();
        VillagerCache.clearCache();

        getLogger().info("AntiVillagerLag optimizations cleaned up");
    }

    // Configuration File Updater
    public Configuration cfg = this.getConfig().getDefaults();
    public void updateConfig() {
        try {
            if(new File(getDataFolder() + "/config.yml").exists()) {
                boolean changesMade = false;
                YamlConfiguration tmp = new YamlConfiguration();
                tmp.load(getDataFolder() + "/config.yml");
                for(String str : cfg.getKeys(true)) {
                    if(!tmp.getKeys(true).contains(str)) {
                        tmp.set(str, cfg.get(str));
                        changesMade = true;
                    }
                }
                if(changesMade)
                    tmp.save(getDataFolder() + "/config.yml");
            }
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ultra-optimized restock system: "Lazy Loading"
     * Does nothing in background - only restocks when a player interacts
     */
    private void startAutomaticRestockSystem() {
        // NO repetitive task!
        // Restock happens automatically in EventListener when needed
        getLogger().info("Lazy restock system enabled (on-demand restock)");
    }
}
