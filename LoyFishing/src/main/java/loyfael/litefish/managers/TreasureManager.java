package loyfael.litefish.managers;

import loyfael.litefish.LiteFish;
import loyfael.litefish.models.BigLootDrop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Manages treasure chests and big loot drops
 */
public class TreasureManager {
    
    private final LiteFish plugin;
    private final Map<String, BigLootDrop> treasures;
    private final List<BigLootDrop> treasurePool;
    
    public TreasureManager(LiteFish plugin) {
        this.plugin = plugin;
        this.treasures = new HashMap<>();
        this.treasurePool = new ArrayList<>();
        loadTreasures();
    }
    
    public void loadTreasures() {
        treasures.clear();
        treasurePool.clear();
        
        FileConfiguration config = plugin.getConfigManager().getDropsConfig();
        ConfigurationSection treasuresSection = config.getConfigurationSection("treasures");
        
        if (treasuresSection == null) {
            createDefaultTreasures();
            return;
        }
        
        for (String treasureKey : treasuresSection.getKeys(false)) {
            ConfigurationSection treasureSection = treasuresSection.getConfigurationSection(treasureKey);
            if (treasureSection == null) continue;
            
            try {
                BigLootDrop treasure = new BigLootDrop(
                    treasureKey,
                    treasureSection.getString("display-name", treasureKey),
                    Material.valueOf(treasureSection.getString("material", "CHEST")),
                    treasureSection.getDouble("chance", 1.0),
                    treasureSection.getInt("experience", 50),
                    treasureSection.getDouble("price", 500.0),
                    treasureSection.getStringList("lore"),
                    treasureSection.getBoolean("nexo-item", false),
                    treasureSection.getString("nexo-id", "")
                );
                
                treasures.put(treasureKey, treasure);
                treasurePool.add(treasure);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load treasure: " + treasureKey + " - " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + treasures.size() + " treasure configurations");
    }
    
    private void createDefaultTreasures() {
        plugin.getLogger().info("Creating default treasure configurations...");
        
        // Treasure Chest - Common
        BigLootDrop treasureChest = new BigLootDrop(
            "treasure_chest",
            "§6§lCoffre au Trésor",
            Material.CHEST,
            60.0, // 60% of all treasure catches
            100,
            1000.0,
            Arrays.asList(
                "§7Un coffre mystérieux rempli",
                "§7de richesses diverses !",
                "§e§lClic droit pour ouvrir"
            ),
            false,
            ""
        );
        
        // Pirate Barrel - Uncommon
        BigLootDrop pirateBarrel = new BigLootDrop(
            "pirate_barrel",
            "§c§lTonneau de Pirate",
            Material.BARREL,
            30.0, // 30% of all treasure catches
            150,
            2000.0,
            Arrays.asList(
                "§7Un tonneau abandonné par",
                "§7des pirates d'autrefois...",
                "§e§lClic droit pour ouvrir"
            ),
            false,
            ""
        );
        
        // Ancient Chest - Rare
        BigLootDrop ancientChest = new BigLootDrop(
            "ancient_chest",
            "§5§lCoffre Antique",
            Material.ENDER_CHEST,
            10.0, // 10% of all treasure catches
            300,
            5000.0,
            Arrays.asList(
                "§7Un coffre ancien émettant",
                "§7une aura mystique...",
                "§d§lContient des objets légendaires !",
                "§e§lClic droit pour ouvrir"
            ),
            false,
            ""
        );
        
        treasures.put("treasure_chest", treasureChest);
        treasures.put("pirate_barrel", pirateBarrel);
        treasures.put("ancient_chest", ancientChest);
        
        treasurePool.addAll(treasures.values());
        
        saveTreasuresToConfig();
        plugin.getLogger().info("Created default treasure configurations");
    }
    
    /**
     * Check if a treasure should be caught (0.5% chance)
     */
    public boolean shouldCatchTreasure() {
        return Math.random() < 0.005; // 0.5% chance
    }
    
    /**
     * Get a random treasure from the pool
     */
    public Optional<BigLootDrop> getRandomTreasure() {
        if (treasurePool.isEmpty()) {
            return Optional.empty();
        }
        
        double totalWeight = treasurePool.stream().mapToDouble(BigLootDrop::getChance).sum();
        double random = Math.random() * totalWeight;
        double currentWeight = 0;
        
        for (BigLootDrop treasure : treasurePool) {
            currentWeight += treasure.getChance();
            if (random <= currentWeight) {
                return Optional.of(treasure);
            }
        }
        
        // Fallback to first treasure
        return Optional.of(treasurePool.get(0));
    }
    
    /**
     * Create treasure item with proper NBT data
     */
    public ItemStack createTreasureItem(BigLootDrop treasure) {
        ItemStack item = new ItemStack(treasure.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(treasure.getDisplayName());
            
            // Add lore with treasure info
            List<String> lore = new ArrayList<>(treasure.getLore());
            lore.add("");
            lore.add("§7§oTrouvé en pêchant...");
            lore.add("§8ID: " + treasure.getId());
            
            meta.setLore(lore);
            
            // Add NBT data to identify as treasure
            meta.getPersistentDataContainer().set(
                plugin.getKey("treasure_id"), 
                org.bukkit.persistence.PersistentDataType.STRING, 
                treasure.getId()
            );
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Open treasure and give rewards to player
     */
    public void openTreasure(Player player, ItemStack treasureItem) {
        ItemMeta meta = treasureItem.getItemMeta();
        if (meta == null) return;
        
        String treasureId = meta.getPersistentDataContainer().get(
            plugin.getKey("treasure_id"), 
            org.bukkit.persistence.PersistentDataType.STRING
        );
        
        if (treasureId == null) return;
        
        BigLootDrop treasure = treasures.get(treasureId);
        if (treasure == null) return;
        
        // Generate treasure contents
        List<ItemStack> rewards = generateTreasureContents(treasure);
        
        // Give rewards to player
        for (ItemStack reward : rewards) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(reward);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), reward);
            }
        }
        
        // Give experience
        player.giveExp(treasure.getExperience());
        
        // Play sound and effects
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        player.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 10);
        
        // Remove one treasure from inventory
        treasureItem.setAmount(treasureItem.getAmount() - 1);
        
        // Send message
        player.sendMessage("§6§l[Trésor] §7Vous avez ouvert un " + treasure.getDisplayName() + " §7!");
    }
    
    private List<ItemStack> generateTreasureContents(BigLootDrop treasure) {
        List<ItemStack> contents = new ArrayList<>();
        
        switch (treasure.getId()) {
            case "treasure_chest":
                // Common treasure contents
                contents.add(new ItemStack(Material.GOLD_INGOT, 2 + (int)(Math.random() * 4))); // 2-5 gold
                contents.add(new ItemStack(Material.EMERALD, 1 + (int)(Math.random() * 3))); // 1-3 emeralds
                if (Math.random() < 0.3) contents.add(new ItemStack(Material.DIAMOND, 1)); // 30% chance diamond
                break;
                
            case "pirate_barrel":
                // Pirate treasure contents
                contents.add(new ItemStack(Material.GOLD_INGOT, 4 + (int)(Math.random() * 6))); // 4-9 gold
                contents.add(new ItemStack(Material.EMERALD, 3 + (int)(Math.random() * 5))); // 3-7 emeralds
                contents.add(new ItemStack(Material.DIAMOND, 1 + (int)(Math.random() * 2))); // 1-2 diamonds
                if (Math.random() < 0.1) contents.add(new ItemStack(Material.NETHERITE_SCRAP, 1)); // 10% netherite scrap
                break;
                
            case "ancient_chest":
                // Legendary treasure contents
                contents.add(new ItemStack(Material.DIAMOND, 3 + (int)(Math.random() * 5))); // 3-7 diamonds
                contents.add(new ItemStack(Material.EMERALD, 5 + (int)(Math.random() * 10))); // 5-14 emeralds
                contents.add(new ItemStack(Material.NETHERITE_SCRAP, 1 + (int)(Math.random() * 2))); // 1-2 netherite scrap
                if (Math.random() < 0.5) contents.add(new ItemStack(Material.ANCIENT_DEBRIS, 1)); // 50% ancient debris
                break;
        }
        
        return contents;
    }
    
    private void saveTreasuresToConfig() {
        FileConfiguration config = plugin.getConfigManager().getDropsConfig();
        
        for (BigLootDrop treasure : treasures.values()) {
            String path = "treasures." + treasure.getId();
            config.set(path + ".display-name", treasure.getDisplayName());
            config.set(path + ".material", treasure.getMaterial().name());
            config.set(path + ".chance", treasure.getChance());
            config.set(path + ".experience", treasure.getExperience());
            config.set(path + ".price", treasure.getPrice());
            config.set(path + ".lore", treasure.getLore());
            config.set(path + ".nexo-item", treasure.isNexoItem());
            config.set(path + ".nexo-id", treasure.getNexoItemId());
        }
        
        plugin.getConfigManager().saveDropsConfig();
    }
    
    public void reload() {
        loadTreasures();
    }
    
    public int getTotalTreasures() {
        return treasures.size();
    }
    
    public Map<String, BigLootDrop> getAllTreasures() {
        return new HashMap<>(treasures);
    }
}
