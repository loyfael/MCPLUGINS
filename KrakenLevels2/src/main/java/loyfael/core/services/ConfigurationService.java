package loyfael.core.services;

import loyfael.api.interfaces.IConfigurationService;
import loyfael.Main;
import loyfael.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe configuration service
 * Single-responsibility principle: configuration management only
 */
public class ConfigurationService implements IConfigurationService {

    private volatile FileConfiguration config;
    private volatile FileConfiguration messages;
    private File configFile;
    private File messagesFile;
    private boolean initialized = false;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    @Override
    public void initialize() {
        writeLock.lock();
        try {
            if (initialized) return;

            File dataFolder = Main.getInstance().getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new IOException("Impossible de créer le dossier de données");
            }

            // Initialiser config.yml
            configFile = new File(dataFolder, "config.yml");
            if (!configFile.exists()) {
                Main.getInstance().saveDefaultConfig();
            }
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Removed forced MongoDB values (was temporarily hardcoded)
            
            // Diagnostic log to verify loaded file
            Utils.sendConsoleLog("&eConfiguration loaded from: " + configFile.getAbsolutePath());
            String mongoHost = config.getString("mongodb.host", "not set");
            int mongoPort = config.getInt("mongodb.port", 0);
            Utils.sendConsoleLog("&eMongoDB values read: host=" + mongoHost + ", port=" + mongoPort);

            // Initialiser messages.yml
            messagesFile = new File(dataFolder, "messages.yml");
            if (!messagesFile.exists()) {
                Main.getInstance().saveResource("messages.yml", false);
            }
            messages = YamlConfiguration.loadConfiguration(messagesFile);

            validateConfiguration();
            initialized = true;

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError during configuration initialization: " + e.getMessage());
            throw new RuntimeException("Failed to initialize configuration", e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void reload() {
        writeLock.lock();
        try {
            if (!initialized) {
                initialize();
                return;
            }

            config = YamlConfiguration.loadConfiguration(configFile);
            messages = YamlConfiguration.loadConfiguration(messagesFile);
            validateConfiguration();

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError while reloading configuration: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void save() {
        readLock.lock();
        try {
            if (!initialized || config == null) {
                return;
            }

            config.save(configFile);

        } catch (IOException e) {
            Utils.sendConsoleLog("&cError while saving configuration: " + e.getMessage());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public FileConfiguration getConfig() {
        readLock.lock();
        try {
            if (!initialized) {
                throw new IllegalStateException("Configuration service not initialized");
            }
            return config;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isInitialized() {
        readLock.lock();
        try {
            return initialized;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void shutdown() {
        writeLock.lock();
        try {
            if (initialized) {
                save();
                initialized = false;
                Utils.sendConsoleLog("&eConfiguration service shut down cleanly.");
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Validate configuration and apply default values if needed
     */
    private void validateConfiguration() {
        boolean changed = false;

        // Validate essential parameters
        if (!config.contains("system.use-mongodb")) {
            config.set("system.use-mongodb", true);
            changed = true;
        }

        if (!config.contains("system.cache-duration")) {
            config.set("system.cache-duration", 30);
            changed = true;
        }

        if (!config.contains("system.auto-save-interval")) {
            config.set("system.auto-save-interval", 300);
            changed = true;
        }

        if (!config.contains("gui.items-per-page")) {
            config.set("gui.items-per-page", 45);
            changed = true;
        }

        if (changed) {
            save();
            Utils.sendConsoleLog("&eConfiguration updated with missing default values.");
        }
    }

    @Override
    public String getMessage(String path, Object... placeholders) {
        readLock.lock();
        try {
            if (!initialized || messages == null) {
                return "&cMessage not found: " + path;
            }

            String message = messages.getString("messages." + path);
            if (message == null) {
                return "&cMessage not found: " + path;
            }

            // Appliquer les couleurs
            message = Utils.colorize(message);

            // Appliquer les placeholders si fournis
            if (placeholders != null && placeholders.length > 0) {
                message = MessageFormat.format(message, placeholders);
            }

            return message;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getMessage(String path) {
        return getMessage(path, (Object[]) null);
    }
}
