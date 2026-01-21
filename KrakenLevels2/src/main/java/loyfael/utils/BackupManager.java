package loyfael.utils;

import loyfael.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Configuration backup manager
 * Only handles configuration files backup since data is stored in MongoDB
 */
public class BackupManager {

    private final Main plugin;
    private final File backupFolder;
    private final SimpleDateFormat dateFormat;
    private static final int MAX_BACKUPS_PER_TYPE = 20;

    public BackupManager() {
        this.plugin = Main.getInstance();
        this.backupFolder = new File(plugin.getDataFolder(), "backup");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        createBackupFolder();
    }

    /**
     * Creates the backup folder if it doesn't exist
     */
    private void createBackupFolder() {
        if (!backupFolder.exists()) {
            if (backupFolder.mkdirs()) {
                Utils.sendConsoleLog("&eBackup folder created: " + backupFolder.getPath());
            } else {
                Utils.sendConsoleLog("&cUnable to create backup folder");
            }
        }
    }

    /**
     * Creates a timestamped backup of a configuration file
     * @param originalFile The file to backup
     * @param fileType The file type (e.g., "config", "levels", "messages")
     * @return true if backup was created successfully
     */
    public boolean createConfigBackup(File originalFile, String fileType) {
        try {
            if (originalFile == null || !originalFile.exists()) {
                Utils.sendConsoleLog("&cBackup failed: File does not exist or is null - " + fileType);
                return false;
            }

            String timestamp = dateFormat.format(new Date());
            String backupFileName = fileType + "_" + timestamp + ".yml";
            File backupFile = new File(backupFolder, backupFileName);

            // Ensure backup folder exists
            if (!backupFolder.exists() && !backupFolder.mkdirs()) {
                Utils.sendConsoleLog("&cBackup failed: Could not create backup folder");
                return false;
            }

            Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Utils.sendConsoleLog("&aConfiguration backup created: " + backupFileName);

            // Clean old backups with error handling
            try {
                cleanOldBackups(fileType);
            } catch (Exception e) {
                Utils.sendConsoleLog("&eWarning: Failed to clean old backups: " + e.getMessage());
                // Continue execution, not critical
            }

            return true;

        } catch (IOException e) {
            Utils.sendConsoleLog("&cError creating configuration backup for " + fileType + ": " + e.getMessage());
            plugin.getLogger().severe("Backup creation failed for " + fileType + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            Utils.sendConsoleLog("&cUnexpected error during backup for " + fileType + ": " + e.getMessage());
            plugin.getLogger().severe("Unexpected backup error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates backups of all configuration files
     * @return true if all backups were created successfully
     */
    public boolean createAllConfigBackups() {
        Utils.sendConsoleLog("&eStarting configuration backup...");

        boolean success = true;
        String[] configFiles = {"config.yml", "levels.yml", "messages.yml", "materials.yml"};
        String[] fileTypes = {"config", "levels", "messages", "materials"};

        for (int i = 0; i < configFiles.length; i++) {
            try {
                File configFile = new File(plugin.getDataFolder(), configFiles[i]);
                boolean result = createConfigBackup(configFile, fileTypes[i]);
                success &= result;

                if (!result) {
                    Utils.sendConsoleLog("&cFailed to backup " + configFiles[i]);
                }
            } catch (Exception e) {
                Utils.sendConsoleLog("&cError backing up " + configFiles[i] + ": " + e.getMessage());
                plugin.getLogger().severe("Config backup error for " + configFiles[i] + ": " + e.getMessage());
                success = false;
            }
        }

        if (success) {
            Utils.sendConsoleLog("&aAll configuration files backed up successfully!");
        } else {
            Utils.sendConsoleLog("&cSome configuration backups failed. Check logs for details.");
        }

        return success;
    }

    /**
     * Cleans old backups, keeping only the latest ones
     */
    private void cleanOldBackups(String fileType) {
        try {
            File[] backupFiles = backupFolder.listFiles((dir, name) -> name.startsWith(fileType + "_"));
            if (backupFiles == null) {
                Utils.sendConsoleLog("&eNo backup files found for type: " + fileType);
                return;
            }

            if (backupFiles.length <= MAX_BACKUPS_PER_TYPE) {
                return; // No cleanup needed
            }

            java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));

            int deletedCount = 0;
            for (int i = 0; i < backupFiles.length - MAX_BACKUPS_PER_TYPE; i++) {
                try {
                    if (backupFiles[i].delete()) {
                        Utils.sendConsoleLog("&eOld backup deleted: " + backupFiles[i].getName());
                        deletedCount++;
                    } else {
                        Utils.sendConsoleLog("&cFailed to delete old backup: " + backupFiles[i].getName());
                    }
                } catch (Exception e) {
                    Utils.sendConsoleLog("&cError deleting backup file " + backupFiles[i].getName() + ": " + e.getMessage());
                }
            }

            if (deletedCount > 0) {
                Utils.sendConsoleLog("&aCleanup completed: " + deletedCount + " old backups removed for " + fileType);
            }

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError during backup cleanup for " + fileType + ": " + e.getMessage());
            plugin.getLogger().warning("Backup cleanup error: " + e.getMessage());
        }
    }

    /**
     * Gets the backup folder
     */
    public File getBackupFolder() {
        return backupFolder;
    }
}
