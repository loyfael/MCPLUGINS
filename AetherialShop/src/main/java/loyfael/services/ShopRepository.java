package loyfael.services;

import loyfael.interfaces.IShopRepository;
import loyfael.model.ShopItem;
import loyfael.cache.ShopCache;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShopRepository implements IShopRepository {

    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Logger logger;
    private final ShopCache cache;
    private final ScheduledExecutorService saveExecutor;
    private volatile boolean saveScheduled = false;

    public ShopRepository(JavaPlugin plugin, ShopCache cache) {
        this.plugin = plugin;
        this.cache = cache;
        this.logger = plugin.getLogger();
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.saveExecutor = Executors.newSingleThreadScheduledExecutor();

        try {
            loadConfiguration();
            logger.info("ShopRepository initialisé avec cache optimisé - Fichier: " + dataFile.getAbsolutePath());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique lors de l'initialisation du ShopRepository", e);
            throw new RuntimeException("Impossible d'initialiser le repository", e);
        }
    }

    private void loadConfiguration() {
        try {
            if (!dataFile.exists()) {
                logger.info("Fichier data.yml inexistant, création d'un nouveau fichier");
                dataConfig = new YamlConfiguration();

                // Créer le dossier parent si nécessaire
                if (!dataFile.getParentFile().exists()) {
                    boolean created = dataFile.getParentFile().mkdirs();
                    logger.info("Création du dossier de données: " + created);
                }
            } else {
                logger.info("Chargement du fichier data.yml existant");
                dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors du chargement de la configuration", e);
            dataConfig = new YamlConfiguration(); // Fallback
        }
    }

    @Override
    public void saveShopData(List<ShopItem> items, String date) {
        try {
            if (items == null || date == null || date.trim().isEmpty()) {
                logger.severe("saveShopData appelé avec paramètres invalides");
                return;
            }

            // Marquer les données comme modifiées dans le cache
            cache.markShopDataDirty(date);

            // Planifier une sauvegarde différée pour éviter les I/O fréquentes
            scheduleDeferredSave(items, date);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique lors de saveShopData", e);
        }
    }

    private void scheduleDeferredSave(List<ShopItem> items, String date) {
        if (saveScheduled) {
            return; // Une sauvegarde est déjà planifiée
        }

        saveScheduled = true;
        saveExecutor.schedule(() -> {
            try {
                performActualSave(items, date);
            } finally {
                saveScheduled = false;
            }
        }, 2, TimeUnit.SECONDS); // Délai de 2 secondes pour grouper les sauvegardes
    }

    private void performActualSave(List<ShopItem> items, String date) {
        try {
            logger.fine("Début sauvegarde différée - Date: " + date + ", Items: " + items.size());

            dataConfig.set("current_rotation.date", date);

            List<Object> itemsList = new ArrayList<>();
            int savedCount = 0;

            for (ShopItem item : items) {
                try {
                    if (item == null) continue;

                    // Optimisation: réutiliser la même section temporaire
                    ConfigurationSection section = dataConfig.createSection("temp_" + savedCount);
                    section.set("material", item.getMaterial().name());
                    section.set("name", item.getName());
                    section.set("price", item.getCurrentPrice());
                    section.set("amount", item.getAmount());
                    section.set("stock", item.getCurrentStock());

                    itemsList.add(section.getValues(false));
                    savedCount++;

                } catch (Exception e) {
                    logger.log(Level.WARNING, "Erreur lors de la sauvegarde d'un item", e);
                }
            }

            dataConfig.set("current_rotation.items", itemsList);

            // Sauvegarde asynchrone du fichier
            final int finalSavedCount = savedCount;
            final String finalDate = date;
            CompletableFuture.runAsync(() -> {
                try {
                    dataConfig.save(dataFile);
                    cache.markShopDataDirty(finalDate); // Marquer comme propre après sauvegarde
                    logger.fine("Sauvegarde asynchrone réussie - " + finalSavedCount + " items");
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Échec de la sauvegarde asynchrone", e);
                    createBackup(finalSavedCount);
                }
            });

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la sauvegarde différée", e);
        }
    }

    private void createBackup(int itemCount) {
        try {
            File backupFile = new File(dataFile.getParent(), "data_backup_" + System.currentTimeMillis() + ".yml");
            dataConfig.save(backupFile);
            logger.warning("Sauvegarde de secours créée: " + backupFile.getName() + " (" + itemCount + " items)");
        } catch (IOException backupError) {
            logger.log(Level.SEVERE, "Échec même de la sauvegarde de secours!", backupError);
        }
    }

    @Override
    public List<ShopItem> loadShopData() {
        List<ShopItem> items = new ArrayList<>();

        try {
            String today = LocalDate.now().toString();

            // Vérifier d'abord le cache
            ShopCache.CachedShopData cached = cache.getCachedData(today);
            if (cached != null && !cached.isDirty()) {
                logger.info("Cache hit pour loadShopData - " + cached.items.size() + " items depuis le cache");
                return new ArrayList<>(cached.items);
            }

            logger.info("Chargement depuis le fichier (cache miss)");
            List<?> itemsList = dataConfig.getList("current_rotation.items");

            if (itemsList == null) {
                return items;
            }

            // Optimisation: pré-allouer la taille de la liste
            items = new ArrayList<>(itemsList.size());
            int loadedCount = 0;

            for (Object obj : itemsList) {
                try {
                    if (!(obj instanceof ConfigurationSection section)) {
                        continue;
                    }

                    String materialName = section.getString("material");
                    if (materialName == null) continue;

                    Material material = Material.valueOf(materialName);
                    String name = section.getString("name");
                    int price = section.getInt("price");
                    int amount = section.getInt("amount");
                    int stock = section.getInt("stock");

                    if (price < 0 || amount <= 0 || stock < 0) {
                        continue;
                    }

                    ShopItem item = new ShopItem(material, name, null, price, price, amount, stock, null);
                    item.setCurrentStock(stock);
                    item.setCurrentPrice(price);
                    items.add(item);
                    loadedCount++;

                } catch (Exception e) {
                    logger.log(Level.WARNING, "Erreur lors du chargement d'un item", e);
                }
            }

            // Mettre en cache le résultat
            cache.cacheData(today, items);
            logger.info("Chargement terminé - " + loadedCount + " items mis en cache");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique lors du chargement des données", e);
        }

        return items;
    }

    @Override
    public String getLastRotationDate() {
        try {
            String date = dataConfig.getString("current_rotation.date", "");
            logger.fine("Date de dernière rotation récupérée: " + (date.isEmpty() ? "aucune" : date));
            return date;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la récupération de la date de rotation", e);
            return "";
        }
    }

    @Override
    public boolean hasValidData() {
        try {
            String savedDate = getLastRotationDate();
            String today = LocalDate.now().toString();
            boolean sameDate = today.equals(savedDate);
            boolean hasItems = !loadShopData().isEmpty();

            boolean isValid = sameDate && hasItems;

            logger.info("Validation des données - Date sauvée: " + savedDate + ", Aujourd'hui: " + today +
                       ", Même date: " + sameDate + ", A des items: " + hasItems + ", Valide: " + isValid);

            return isValid;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la validation des données", e);
            return false;
        }
    }

    public void shutdown() {
        try {
            if (saveScheduled) {
                logger.info("Attente de la sauvegarde en cours...");
                saveExecutor.shutdown();
                if (!saveExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    saveExecutor.shutdownNow();
                }
            }
            logger.info("ShopRepository fermé proprement");
        } catch (InterruptedException e) {
            saveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
