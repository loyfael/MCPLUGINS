package loyfael.services;

import loyfael.interfaces.IItemService;
import loyfael.model.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemService implements IItemService {

    private final JavaPlugin plugin;
    private final List<ShopItem> availableItems;
    private final Logger logger;

    public ItemService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.availableItems = new ArrayList<>();
        this.logger = plugin.getLogger();

        try {
            loadAvailableItems();
            logger.info("ItemService initialisé avec succès - " + availableItems.size() + " items disponibles");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique lors de l'initialisation d'ItemService", e);
            throw new RuntimeException("Impossible d'initialiser ItemService", e);
        }
    }

    @Override
    public List<ShopItem> loadAvailableItems() {
        try {
            availableItems.clear();

            loadFromConfigFile();

            return availableItems;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors du chargement des items", e);
            return availableItems;
        }
    }
    
    /**
     * Charge depuis items.yml mais filtre uniquement les items vanilla
     */
    private void loadFromConfigFile() {
        try {

            File itemsFile = new File(plugin.getDataFolder(), "items.yml");

            if (!itemsFile.exists()) {
                logger.info("Fichier items.yml introuvable, utilisation des items vanilla par défaut uniquement");
                return;
            }

            FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
            List<?> itemsList = itemsConfig.getList("items");

            if (itemsList == null) {
                logger.info("Section 'items' introuvable dans items.yml, utilisation des vanilla uniquement");
                return;
            }

            int loadedCount = 0;
            int errorCount = 0;

            for (Object obj : itemsList) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) obj;

                    // Validation des champs obligatoires
                    String materialName = (String) itemMap.get("material");
                    if (materialName == null || materialName.trim().isEmpty()) {
                        logger.warning("Material manquant ou vide, item ignoré");
                        errorCount++;
                        continue;
                    }

                    Material material;
                    try {
                        material = Material.valueOf(materialName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        logger.warning("Material invalide ignoré: " + materialName);
                        errorCount++;
                        continue;
                    }

                    // FILTRE: Vérifier que c'est un item valide
                    if (material.isLegacy() || !material.isItem()) {
                        logger.info("Item invalide ignoré: " + materialName);
                        errorCount++;
                        continue;
                    }

                    // Vérifier si c'est un item vanilla
                    // Option 1: Vanilla si 'vanilla: true' OU si pas de 'name' custom
                    boolean hasVanillaFlag = itemMap.containsKey("vanilla") && 
                                            Boolean.TRUE.equals(itemMap.get("vanilla"));
                    boolean hasCustomName = itemMap.containsKey("name") && 
                                           itemMap.get("name") != null && 
                                           !((String) itemMap.get("name")).trim().isEmpty();
                    
                    boolean isVanilla = hasVanillaFlag || !hasCustomName;

                    String name = null;
                    List<String> lore = null;
                    
                    if (!isVanilla) {
                        // Pour les items custom (ont un nom), récupérer nom et lore
                        name = (String) itemMap.get("name");
                        @SuppressWarnings("unchecked")
                        List<String> rawLore = (List<String>) itemMap.get("lore");
                        lore = rawLore;
                    }
                    // Pour les items vanilla, name et lore restent null (affichage par défaut)

                    // Validation des prix
                    @SuppressWarnings("unchecked")
                    Map<String, Object> priceRange = (Map<String, Object>) itemMap.get("price_range");
                    if (priceRange == null) {
                        logger.warning("Section price_range manquante pour " + materialName + ", item ignoré");
                        errorCount++;
                        continue;
                    }

                    Object minObj = priceRange.get("min");
                    Object maxObj = priceRange.get("max");

                    int minPrice = minObj instanceof Number ? ((Number) minObj).intValue() : -1;
                    int maxPrice = maxObj instanceof Number ? ((Number) maxObj).intValue() : -1;

                    if (minPrice <= 0 || maxPrice <= 0 || minPrice >= maxPrice) {
                        logger.warning("Prix invalides pour " + materialName + " (min=" + minPrice + ", max=" + maxPrice + "), item ignoré");
                        errorCount++;
                        continue;
                    }

                    Object amountObj = itemMap.get("amount");
                    Object stockObj = itemMap.get("stock");

                    int amount = amountObj instanceof Number ? ((Number) amountObj).intValue() : 1;
                    int stock = stockObj instanceof Number ? ((Number) stockObj).intValue() : 1;

                    if (amount <= 0) amount = 1;
                    if (stock <= 0) {
                        logger.warning("Stock invalide pour " + materialName + " (" + stock + "), item ignoré");
                        errorCount++;
                        continue;
                    }

                    // Créer l'objet ShopItem avec le paramètre nbt
                    String nbt = (String) itemMap.get("nbt"); // Récupérer le nbt optionnel
                    ShopItem shopItem = new ShopItem(material, name, lore, minPrice, maxPrice, amount, stock, nbt);
                    availableItems.add(shopItem);
                    loadedCount++;

                    if (isVanilla) {
                        if (hasVanillaFlag) {
                            logger.fine("Item vanilla chargé (vanilla: true): " + materialName);
                        } else {
                            logger.fine("Item vanilla chargé (pas de nom custom): " + materialName);
                        }
                    } else {
                        logger.fine("Item custom chargé: " + materialName + " (" + name + ")");
                    }

                } catch (Exception e) {
                    logger.warning("Erreur lors du traitement d'un item: " + e.getMessage());
                    errorCount++;
                }
            }

            logger.info("Chargement terminé - " + loadedCount + " items chargés avec succès, " + errorCount + " erreurs");

            if (availableItems.isEmpty()) {
                logger.severe("ATTENTION: Aucun item valide chargé! Le shop sera vide!");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique lors du chargement des items", e);
        }
    }

    @Override
    public List<ShopItem> generateDailyItems(int count) {
        try {
            logger.info("Génération des items quotidiens - Demandé: " + count + ", Disponibles: " + availableItems.size());

            if (availableItems.isEmpty()) {
                logger.severe("Impossible de générer les items quotidiens: aucun item disponible!");
                return new ArrayList<>();
            }

            if (count <= 0) {
                logger.warning("Nombre d'items demandé invalide: " + count + ", utilisation de la valeur par défaut: 9");
                count = 9;
            }

            List<ShopItem> shuffled = new ArrayList<>(availableItems);
            Collections.shuffle(shuffled);

            List<ShopItem> dailyItems = new ArrayList<>();
            int actualCount = Math.min(count, shuffled.size());

            for (int i = 0; i < actualCount; i++) {
                try {
                    ShopItem original = shuffled.get(i);

                    if (original == null) {
                        logger.warning("Item original null à l'index " + i + ", ignoré");
                        continue;
                    }

                    ShopItem copy = createShopItem(
                        original.getMaterial().name(),
                        original.getName(),
                        original.getLore(),
                        original.getMinPrice(),
                        original.getMaxPrice(),
                        original.getAmount(),
                        original.getMaxStock(),
                        original.getNbt()
                    );

                    // Générer un stock quotidien aléatoire (toujours > 50)
                    copy.generateRandomDailyStock();

                    dailyItems.add(copy);

                    logger.fine("Item quotidien généré: " + (copy.getName() != null ? copy.getName() : copy.getMaterial().name()) +
                               " (prix initial: " + copy.getCurrentPrice() + ", stock: " + copy.getCurrentStock() + ")");

                } catch (Exception e) {
                    logger.log(Level.WARNING, "Erreur lors de la génération d'un item quotidien à l'index " + i, e);
                }
            }

            logger.info("Génération terminée - " + dailyItems.size() + "/" + count + " items générés");

            if (dailyItems.isEmpty()) {
                logger.severe("ALERTE: Aucun item quotidien généré! Le shop sera vide aujourd'hui!");
            }

            return dailyItems;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique lors de la génération des items quotidiens", e);
            return new ArrayList<>();
        }
    }

    @Override
    public ShopItem createShopItem(String material, String name, List<String> lore,
                                  int minPrice, int maxPrice, int amount, int stock, String nbt) {
        try {
            if (material == null || material.trim().isEmpty()) {
                throw new IllegalArgumentException("Material ne peut pas être null ou vide");
            }

            Material mat = Material.valueOf(material.toUpperCase());

            if (minPrice <= 0 || maxPrice <= 0 || minPrice > maxPrice) {
                throw new IllegalArgumentException("Prix invalides: min=" + minPrice + ", max=" + maxPrice);
            }

            if (amount <= 0 || stock <= 0) {
                throw new IllegalArgumentException("Quantités invalides: amount=" + amount + ", stock=" + stock);
            }

            ShopItem item = new ShopItem(mat, name, lore, minPrice, maxPrice, amount, stock, nbt);

            return item;

        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Erreur lors de la création d'un ShopItem - Material: " + material +
                      ", Nom: " + name + ", Erreur: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique lors de la création d'un ShopItem", e);
            throw new RuntimeException("Impossible de créer le ShopItem", e);
        }
    }
}
