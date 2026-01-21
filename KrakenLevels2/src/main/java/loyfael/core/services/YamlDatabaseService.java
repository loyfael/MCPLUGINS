package loyfael.core.services;

import loyfael.api.interfaces.IConfigurationService;
import loyfael.utils.Utils;
import loyfael.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implémentation YAML du service de base de données
 * Principe de substitution de Liskov : peut remplacer AbstractDatabaseService
 */
public class YamlDatabaseService extends AbstractDatabaseService {

    private File dataFile;
    private FileConfiguration dataConfig;

    public YamlDatabaseService(IConfigurationService configService) {
        super(configService);
    }

    @Override
    protected boolean doInitialize() {
        try {
            File dataFolder = Main.getInstance().getDataFolder();
            dataFile = new File(dataFolder, "data.yml");

            if (!dataFile.exists()) {
                if (!dataFile.createNewFile()) {
                    throw new IOException("Impossible de créer le fichier data.yml");
                }
                Utils.sendConsoleLog("&aFichier data.yml créé.");
            }

            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            Utils.sendConsoleLog("&aFichier YAML chargé avec " + dataConfig.getKeys(true).size() + " entrées.");

            return true;

        } catch (Exception e) {
            Utils.sendConsoleLog("&cErreur lors de l'initialisation YAML: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void doDisconnect() {
        try {
            if (dataConfig != null && dataFile != null) {
                dataConfig.save(dataFile);
                Utils.sendConsoleLog("&eDonnées YAML sauvegardées.");
            }
        } catch (IOException e) {
            Utils.sendConsoleLog("&cErreur lors de la sauvegarde YAML: " + e.getMessage());
        }
    }

    @Override
    public void saveData(String key, Object value) {
        validateKey(key);
        ensureConnected();

        synchronized (dataConfig) {
            dataConfig.set(key, value);
            dataConfig.set(key + "_lastUpdated", System.currentTimeMillis());

            // Sauvegarde asynchrone pour éviter les blocages
            Main.getInstance().getServer().getScheduler().runTaskAsynchronously(
                Main.getInstance(), this::saveToFile);
        }
    }

    @Override
    public Optional<Object> getData(String key) {
        validateKey(key);
        ensureConnected();

        synchronized (dataConfig) {
            Object value = dataConfig.get(key);
            return Optional.ofNullable(value);
        }
    }

    @Override
    public boolean deleteData(String key) {
        validateKey(key);
        ensureConnected();

        synchronized (dataConfig) {
            boolean existed = dataConfig.contains(key);
            if (existed) {
                dataConfig.set(key, null);
                dataConfig.set(key + "_lastUpdated", null);

                // Sauvegarde asynchrone
                Main.getInstance().getServer().getScheduler().runTaskAsynchronously(
                    Main.getInstance(), this::saveToFile);
            }
            return existed;
        }
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        ensureConnected();

        synchronized (dataConfig) {
            return dataConfig.contains(key);
        }
    }

    @Override
    public Map<String, Object> getDataByPrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Le préfixe ne peut pas être null");
        }
        ensureConnected();

        Map<String, Object> results = new HashMap<>();

        synchronized (dataConfig) {
            Set<String> keys = dataConfig.getKeys(true);
            for (String key : keys) {
                if (key.startsWith(prefix) && !key.endsWith("_lastUpdated")) {
                    Object value = dataConfig.get(key);
                    if (value != null) {
                        results.put(key, value);
                    }
                }
            }
        }

        return results;
    }

    @Override
    public void backup() {
        ensureConnected();

        try {
            File backupDir = new File(Main.getInstance().getDataFolder(), "backups");
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                throw new IOException("Impossible de créer le dossier de sauvegarde");
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            File backupFile = new File(backupDir, "data_backup_" + timestamp + ".yml");

            synchronized (dataConfig) {
                dataConfig.save(backupFile);
            }

            Utils.sendConsoleLog("&aSauvegarde YAML créée: " + backupFile.getName());

        } catch (Exception e) {
            Utils.sendConsoleLog("&cErreur lors de la sauvegarde YAML: " + e.getMessage());
        }
    }

    /**
     * Méthode privée pour sauvegarder le fichier de manière thread-safe
     */
    private void saveToFile() {
        try {
            synchronized (dataConfig) {
                dataConfig.save(dataFile);
            }
        } catch (IOException e) {
            Utils.sendConsoleLog("&cErreur lors de la sauvegarde du fichier YAML: " + e.getMessage());
        }
    }

    /**
     * Recharge les données depuis le fichier
     */
    public void reload() {
        ensureConnected();

        synchronized (dataConfig) {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            Utils.sendConsoleLog("&aDonnées YAML rechargées.");
        }
    }
}
