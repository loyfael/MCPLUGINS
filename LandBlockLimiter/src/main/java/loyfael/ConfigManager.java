package loyfael;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<Material, Integer> blockLimits;
    private final Map<Material, String> blockTranslations;
    private FileConfiguration config;

    // Settings
    private boolean debug;
    private boolean checkBuildPermissions;

    // Performance settings
    private int cacheExpiryMinutes;
    private int maxChunksPerTick;
    private int scanIntervalTicks;
    private boolean asyncScanning;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.blockLimits = new HashMap<>();
        this.blockTranslations = new HashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Clear existing limits
        blockLimits.clear();
        blockTranslations.clear();

        // Load block limits
        List<String> blocks = config.getStringList("blocks");
        for (String blockEntry : blocks) {
            if (blockEntry.contains(":")) {
                String[] parts = blockEntry.split(":");
                if (parts.length == 2) {
                    try {
                        Material material = Material.valueOf(parts[0].toUpperCase());
                        int limit = Integer.parseInt(parts[1]);
                        blockLimits.put(material, limit);
                        if (debug) {
                            plugin.getLogger().info("Loaded limit: " + material + " = " + limit);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material or limit: " + blockEntry);
                    }
                }
            }
        }

        // Load block translations
        if (config.getConfigurationSection("block_translations") != null) {
            for (String key : config.getConfigurationSection("block_translations").getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    String translation = config.getString("block_translations." + key);
                    blockTranslations.put(material, translation);
                    if (debug) {
                        plugin.getLogger().info("Loaded translation: " + material + " = " + translation);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material for translation: " + key);
                }
            }
        }

        // Load settings
        debug = config.getBoolean("settings.debug", false);
        checkBuildPermissions = config.getBoolean("settings.check_build_permissions", true);

        // Load performance settings
        cacheExpiryMinutes = config.getInt("performance.cache_expiry_minutes", 5);
        maxChunksPerTick = config.getInt("performance.max_chunks_per_tick", 2);
        scanIntervalTicks = config.getInt("performance.scan_interval_ticks", 10);
        asyncScanning = config.getBoolean("performance.async_scanning", true);

        plugin.getLogger().info("Configuration loaded with " + blockLimits.size() + " block limits.");
        plugin.getLogger().info("Performance: Cache=" + cacheExpiryMinutes + "min, Chunks/tick=" +
            maxChunksPerTick + ", Async=" + asyncScanning);
    }

    public Map<Material, Integer> getBlockLimits() {
        return blockLimits;
    }

    public Integer getBlockLimit(Material material) {
        return blockLimits.get(material);
    }

    public boolean hasLimit(Material material) {
        return blockLimits.containsKey(material);
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean shouldCheckBuildPermissions() {
        return checkBuildPermissions;
    }

    // Performance getters
    public long getCacheExpiryMs() {
        return cacheExpiryMinutes * 60 * 1000L;
    }

    public int getMaxChunksPerTick() {
        return maxChunksPerTick;
    }

    public int getScanIntervalTicks() {
        return scanIntervalTicks;
    }

    public boolean isAsyncScanningEnabled() {
        return asyncScanning;
    }

    public String getBlockTranslation(Material material) {
        return blockTranslations.get(material);
    }

    public boolean hasTranslation(Material material) {
        return blockTranslations.containsKey(material);
    }
}
