package loyfael.litefish.managers;

import loyfael.litefish.LiteFish;
import loyfael.litefish.models.FishDrop;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Manages fishing drop configurations and calculations
 */
public class DropManager {
    
    private final LiteFish plugin;
    private final Map<String, FishDrop> drops;
    private final Map<Biome, List<String>> biomeDrops;
    private final Random random;
    
    public DropManager(LiteFish plugin) {
        this.plugin = plugin;
        this.drops = new HashMap<>();
        this.biomeDrops = new HashMap<>();
        this.random = new Random();
        loadDrops();
    }
    
    public void loadDrops() {
        drops.clear();
        biomeDrops.clear();
        
        FileConfiguration config = plugin.getConfigManager().getDropsConfig();
        ConfigurationSection dropsSection = config.getConfigurationSection("drops");
        
        if (dropsSection == null) {
            plugin.getLogger().warning("No drops section found in drops.yml");
            createDefaultDrops();
            return;
        }
        
        for (String dropKey : dropsSection.getKeys(false)) {
            ConfigurationSection dropSection = dropsSection.getConfigurationSection(dropKey);
            
            if (dropSection != null) {
                try {
                    // Charge les enchantements s'ils existent
                    Map<Enchantment, Integer> enchantments = new HashMap<>();
                    boolean randomEnchantments = false;
                    ConfigurationSection enchantSection = dropSection.getConfigurationSection("enchantments");
                    if (enchantSection != null) {
                        // Vérifier si c'est des enchantements aléatoires
                        if (enchantSection.contains("RANDOM")) {
                            randomEnchantments = true;
                        } else {
                            // Enchantements spécifiques
                            for (String enchantName : enchantSection.getKeys(false)) {
                                try {
                                    Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                                    if (enchant != null) {
                                        int level = enchantSection.getInt(enchantName, 1);
                                        enchantments.put(enchant, level);
                                    } else {
                                        plugin.getLogger().warning("Enchantement inconnu: " + enchantName + " pour le drop " + dropKey);
                                    }
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Erreur lors du chargement de l'enchantement " + enchantName + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                    
                    FishDrop drop = new FishDrop(
                        dropKey,
                        dropSection.getString("display-name", dropKey),
                        Material.valueOf(dropSection.getString("material", "COD")),
                        dropSection.getDouble("chance", 10.0),
                        dropSection.getInt("experience", 5),
                        dropSection.getDouble("price", 10.0),
                        dropSection.getStringList("biomes"),
                        dropSection.getString("nexo-item", null),
                        enchantments,
                        randomEnchantments
                    );
                    
                    drops.put(dropKey, drop);
                    
                    // Map drops to biomes
                    List<String> allowedBiomes = drop.getBiomes();
                    if (allowedBiomes.isEmpty()) {
                        // If no biomes specified, add to all biomes
                        for (Biome biome : Biome.values()) {
                            biomeDrops.computeIfAbsent(biome, k -> new ArrayList<>()).add(dropKey);
                        }
                    } else {
                        for (String biomeName : allowedBiomes) {
                            try {
                                Biome biome = Biome.valueOf(biomeName.toUpperCase());
                                biomeDrops.computeIfAbsent(biome, k -> new ArrayList<>()).add(dropKey);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid biome in drop " + dropKey + ": " + biomeName);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error loading drop " + dropKey + ": " + e.getMessage());
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + drops.size() + " fishing drops");
    }
    
    private void createDefaultDrops() {
        // Common fish
        drops.put("cod", new FishDrop("cod", "Cod", Material.COD, 40.0, 5, 10.0, Arrays.asList("OCEAN", "COLD_OCEAN"), null));
        drops.put("salmon", new FishDrop("salmon", "Salmon", Material.SALMON, 25.0, 7, 15.0, Arrays.asList("RIVER", "FROZEN_RIVER"), null));
        drops.put("tropical_fish", new FishDrop("tropical_fish", "Tropical Fish", Material.TROPICAL_FISH, 15.0, 10, 25.0, Arrays.asList("WARM_OCEAN", "LUKEWARM_OCEAN"), null));
        drops.put("pufferfish", new FishDrop("pufferfish", "Pufferfish", Material.PUFFERFISH, 10.0, 15, 40.0, Arrays.asList("WARM_OCEAN"), null));
        
        // Treasure items
        drops.put("nautilus_shell", new FishDrop("nautilus_shell", "Nautilus Shell", Material.NAUTILUS_SHELL, 2.0, 25, 100.0, new ArrayList<>(), null));
        drops.put("heart_of_the_sea", new FishDrop("heart_of_the_sea", "Heart of the Sea", Material.HEART_OF_THE_SEA, 0.1, 100, 1000.0, Arrays.asList("DEEP_OCEAN"), null));
        
        plugin.getLogger().info("Created default drop configurations");
    }
    
    /**
     * Checks if a custom drop should be given based on individual drop chances
     * This respects the actual percentage chance of each drop
     */
    public Optional<FishDrop> tryGetCustomDrop(Biome biome) {
        List<String> availableDrops = biomeDrops.get(biome);
        if (availableDrops == null || availableDrops.isEmpty()) {
            availableDrops = biomeDrops.get(Biome.OCEAN); // Fallback to ocean drops
            if (availableDrops == null || availableDrops.isEmpty()) {
                return Optional.empty();
            }
        }
        
        // Check each drop individually against its chance
        for (String dropKey : availableDrops) {
            FishDrop drop = drops.get(dropKey);
            if (drop != null) {
                // Convert chance to probability (assuming chance is a percentage)
                double probability = drop.getChance() / 100.0;
                double roll = random.nextDouble();
                
                // Roll for this specific drop
                if (roll < probability) {
                    plugin.getLogger().info("Player received custom drop: " + drop.getDisplayName() + " (chance: " + drop.getChance() + "%, roll: " + roll + ")");
                    return Optional.of(drop);
                }
            }
        }
        
        return Optional.empty(); // No custom drop this time
    }
    
    public Optional<FishDrop> getRandomDrop(Biome biome) {
        return getRandomDrop(biome, false);
    }
    
    public Optional<FishDrop> getRandomDrop(Biome biome, boolean isLavaFishing) {
        List<String> availableDrops;
        
        if (isLavaFishing) {
            // Special lava drops
            availableDrops = getLavaDrops();
        } else {
            availableDrops = biomeDrops.get(biome);
            if (availableDrops == null || availableDrops.isEmpty()) {
                availableDrops = biomeDrops.get(Biome.OCEAN); // Fallback to ocean drops
                if (availableDrops == null || availableDrops.isEmpty()) {
                    return Optional.empty();
                }
            }
        }
        
        // Calculate total weight
        double totalWeight = 0.0;
        List<FishDrop> weightedDrops = new ArrayList<>();
        
        for (String dropKey : availableDrops) {
            FishDrop drop = drops.get(dropKey);
            if (drop != null) {
                totalWeight += drop.getChance();
                weightedDrops.add(drop);
            }
        }
        
        if (weightedDrops.isEmpty() || totalWeight <= 0) {
            return Optional.empty();
        }
        
        // Random selection based on weight
        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0.0;
        
        for (FishDrop drop : weightedDrops) {
            currentWeight += drop.getChance();
            if (randomValue <= currentWeight) {
                return Optional.of(drop);
            }
        }
        
        // Fallback to last drop if something goes wrong
        return Optional.of(weightedDrops.get(weightedDrops.size() - 1));
    }
    
    private List<String> getLavaDrops() {
        // Return all drops that are available in Nether biomes (lava fishing)
        List<String> lavaDrops = new ArrayList<>();
        
        // Get all drops that have Nether biomes
        String[] netherBiomes = {"NETHER_WASTES", "CRIMSON_FOREST", "WARPED_FOREST", "SOUL_SAND_VALLEY", "BASALT_DELTAS"};
        
        for (FishDrop drop : drops.values()) {
            for (String biome : drop.getBiomes()) {
                for (String netherBiome : netherBiomes) {
                    if (biome.equals(netherBiome)) {
                        lavaDrops.add(drop.getKey());
                        break; // Found one match, no need to check other biomes for this drop
                    }
                }
            }
        }
        
        // If no specific lava drops found, create fallback
        if (lavaDrops.isEmpty()) {
            createLavaDrops();
            lavaDrops.add("lava_fish");
            lavaDrops.add("obsidian_shard");
            lavaDrops.add("fire_crystal");
        }
        
        return lavaDrops;
    }
    
    private void createLavaDrops() {
        // Create special lava fishing drops
        drops.put("lava_fish", new FishDrop("lava_fish", "§cLava Fish", Material.TROPICAL_FISH, 30.0, 20, 50.0, Arrays.asList("NETHER_WASTES"), null));
        drops.put("obsidian_shard", new FishDrop("obsidian_shard", "§8Obsidian Shard", Material.OBSIDIAN, 15.0, 30, 100.0, Arrays.asList("NETHER_WASTES"), null));
        drops.put("fire_crystal", new FishDrop("fire_crystal", "§6Fire Crystal", Material.FIRE_CHARGE, 5.0, 50, 200.0, Arrays.asList("NETHER_WASTES"), null));
        
        plugin.getLogger().info("Created lava fishing drops");
    }
    
    public ItemStack createDropItem(FishDrop drop) {
        ItemStack item;
        
        // Check if it's a Nexo item
        if (drop.getNexoItem() != null && plugin.getNexoHook().isEnabled()) {
            item = plugin.getNexoHook().createNexoItem(drop.getNexoItem());
            if (item == null) {
                // Fallback to material if Nexo item creation fails
                item = new ItemStack(drop.getMaterial());
            }
        } else {
            item = new ItemStack(drop.getMaterial());
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Set display name - check if empty string for vanilla names
            String displayName = drop.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                meta.setDisplayName("§r" + displayName);
            }
            // If displayName is empty string or null, keep vanilla name
            
            item.setItemMeta(meta);
        }
        
        // Apply enchantments
        if (drop.isRandomEnchantments()) {
            // Apply random enchantments
            applyRandomEnchantments(item);
        } else if (drop.hasEnchantments()) {
            // Apply specific enchantments
            for (Map.Entry<Enchantment, Integer> enchant : drop.getEnchantments().entrySet()) {
                applyEnchantment(item, enchant.getKey(), enchant.getValue());
            }
        }
        
        // SYSTÈME FERRAGE : Replace ferrage items with normal items
        item = replaceFerrage(item);
        
        return item;
    }
    
    /**
     * SYSTÈME FERRAGE : Replace ferrage items with normal variants
     * Example: IRON_SWORD with ferrage -> IRON_SWORD without ferrage
     */
    private ItemStack replaceFerrage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return item;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return item; // No custom name, probably not a ferrage item
        }
        
        String displayName = meta.getDisplayName();
        
        // Check if this looks like a ferrage item (contains ferrage indicators)
        if (displayName.toLowerCase().contains("ferrage") || 
            displayName.toLowerCase().contains("forgé") ||
            displayName.toLowerCase().contains("renforcé")) {
            
            // Create a new item without the ferrage aspects
            ItemStack normalItem = new ItemStack(item.getType(), item.getAmount());
            
            // Keep enchantments but remove ferrage-specific display name
            if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
                ItemMeta normalMeta = normalItem.getItemMeta();
                if (normalMeta != null) {
                    // Copy enchantments
                    for (Map.Entry<Enchantment, Integer> enchant : item.getItemMeta().getEnchants().entrySet()) {
                        normalMeta.addEnchant(enchant.getKey(), enchant.getValue(), true);
                    }
                    
                    // Remove custom display name to show vanilla name
                    // (Don't set display name = vanilla name will show)
                    
                    normalItem.setItemMeta(normalMeta);
                }
            }
            
            return normalItem;
        }
        
        return item; // Not a ferrage item, return as-is
    }

    private void applyRandomEnchantments(ItemStack item) {
        // Get all possible enchantments for this item type
        List<Enchantment> possibleEnchantments = new ArrayList<>();
        
        for (Enchantment enchantment : Enchantment.values()) {
            if (item.getType() == Material.ENCHANTED_BOOK || enchantment.canEnchantItem(item)) {
                possibleEnchantments.add(enchantment);
            }
        }
        
        if (possibleEnchantments.isEmpty()) {
            return;
        }
        
        // Randomly select 1-3 enchantments
        int numEnchantments = random.nextInt(3) + 1;
        numEnchantments = Math.min(numEnchantments, possibleEnchantments.size());
        
        Collections.shuffle(possibleEnchantments);
        
        for (int i = 0; i < numEnchantments; i++) {
            Enchantment enchantment = possibleEnchantments.get(i);
            int maxLevel = enchantment.getMaxLevel();
            int level = random.nextInt(maxLevel) + 1;
            
            applyEnchantment(item, enchantment, level);
        }
    }

    private void applyEnchantment(ItemStack item, Enchantment enchantment, int level) {
        if (item.getType() == Material.ENCHANTED_BOOK) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
                storageMeta.addStoredEnchant(enchantment, level, true);
                item.setItemMeta(storageMeta);
                return;
            }
        }

        item.addUnsafeEnchantment(enchantment, level);
    }
    
    public Optional<FishDrop> getDrop(String key) {
        return Optional.ofNullable(drops.get(key));
    }
    
    public Map<String, FishDrop> getAllDrops() {
        return new HashMap<>(drops);
    }
    
    public List<FishDrop> getDropsForBiome(Biome biome) {
        List<String> dropKeys = biomeDrops.get(biome);
        if (dropKeys == null) return new ArrayList<>();
        
        return dropKeys.stream()
                .map(drops::get)
                .filter(Objects::nonNull)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Get drops for biome by string name (for special fishing like void/lava)
     */
    public List<FishDrop> getDropsForBiome(String biomeName) {
        List<FishDrop> result = new ArrayList<>();
        
        for (FishDrop drop : drops.values()) {
            if (drop.getBiomes().contains(biomeName)) {
                result.add(drop);
            }
        }
        
        return result;
    }
    
    public void reload() {
        loadDrops();
    }
    
    public int getTotalDrops() {
        return drops.size();
    }
    
    public double getTotalChanceForBiome(Biome biome) {
        return getDropsForBiome(biome).stream()
                .mapToDouble(FishDrop::getChance)
                .sum();
    }
}
