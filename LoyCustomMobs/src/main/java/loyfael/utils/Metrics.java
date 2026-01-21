package loyfael.utils;

import loyfael.LoyCustomMobs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

/**
 * bStats metrics collection for LoyCustomMobs
 * Simplified version for modern Paper plugins
 */
public class Metrics {

    private final Plugin plugin;
    private final int serviceId;

    public Metrics(LoyCustomMobs plugin, int serviceId) {
        this.plugin = plugin;
        this.serviceId = serviceId;

        // Check if metrics are enabled
        if (!plugin.getConfigManager().isMetricsEnabled()) {
            plugin.getLogger().info("Metrics disabled in config.");
            return;
        }

        // Submit data
        startSubmitting();
    }

    private void startSubmitting() {
        // Start metrics collection task
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                submitData();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not submit metrics data", e);
            }
        }, 100L, 12000L); // Submit every 10 minutes
    }

    private void submitData() {
        // Basic metrics data
        JsonObjectBuilder dataBuilder = new JsonObjectBuilder()
                .appendField("serverUUID", getServerUUID())
                .appendField("playerAmount", Bukkit.getOnlinePlayers().size())
                .appendField("onlineMode", Bukkit.getOnlineMode() ? 1 : 0)
                .appendField("bukkitVersion", Bukkit.getVersion())
                .appendField("bukkitName", Bukkit.getName())
                .appendField("javaVersion", System.getProperty("java.version"))
                .appendField("osName", System.getProperty("os.name"))
                .appendField("osArch", System.getProperty("os.arch"))
                .appendField("osVersion", System.getProperty("os.version"))
                .appendField("coreCount", Runtime.getRuntime().availableProcessors());

        // Plugin specific metrics
        LoyCustomMobs customMobsPlugin = (LoyCustomMobs) plugin;
        dataBuilder.appendField("activeMobsCount", customMobsPlugin.getMobManager().getActiveMobs().size());

        var stats = customMobsPlugin.getMobManager().getStatistics();
        dataBuilder.appendField("registeredAbilities", (Integer) stats.get("registeredAbilities"));
        dataBuilder.appendField("spawnChance", (Double) stats.get("spawnChance"));

        // Send the data (simplified - in production you'd send to bStats API)
        plugin.getLogger().info("Metrics data collected successfully.");
    }

    private String getServerUUID() {
        File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
        File uuidFile = new File(bStatsFolder, "serverUuid");

        if (!uuidFile.exists()) {
            bStatsFolder.mkdirs();
            try {
                String uuid = UUID.randomUUID().toString();
                YamlConfiguration config = new YamlConfiguration();
                config.set("serverUuid", uuid);
                config.save(uuidFile);
                return uuid;
            } catch (Exception e) {
                return UUID.randomUUID().toString();
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(uuidFile);
        return config.getString("serverUuid", UUID.randomUUID().toString());
    }

    /**
     * Simple JSON builder for metrics data
     */
    private static class JsonObjectBuilder {
        private final StringBuilder builder = new StringBuilder();
        private boolean first = true;

        public JsonObjectBuilder() {
            builder.append("{");
        }

        public JsonObjectBuilder appendField(String key, String value) {
            if (!first) {
                builder.append(",");
            }
            builder.append("\"").append(key).append("\":\"").append(value).append("\"");
            first = false;
            return this;
        }

        public JsonObjectBuilder appendField(String key, int value) {
            if (!first) {
                builder.append(",");
            }
            builder.append("\"").append(key).append("\":").append(value);
            first = false;
            return this;
        }

        public JsonObjectBuilder appendField(String key, double value) {
            if (!first) {
                builder.append(",");
            }
            builder.append("\"").append(key).append("\":").append(value);
            first = false;
            return this;
        }

        public String build() {
            return builder.append("}").toString();
        }
    }
}
