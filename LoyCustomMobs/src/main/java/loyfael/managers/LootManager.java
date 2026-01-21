package loyfael.managers;

import loyfael.LoyCustomMobs;
import loyfael.models.CustomMob;
import loyfael.models.MobRarity;
import loyfael.utils.MobFlavor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Gestionnaire avancé des loots pour les mobs custom.
 */
public class LootManager {
        private static final List<PotionType> RANDOM_POTIONS = List.of(
            PotionType.SWIFTNESS,
        PotionType.REGENERATION,
        PotionType.STRENGTH,
        PotionType.FIRE_RESISTANCE,
        PotionType.INVISIBILITY,
        PotionType.NIGHT_VISION,
        PotionType.WATER_BREATHING,
        PotionType.LUCK
    );

    private static final Map<String, Enchantment> ENCHANTMENT_INDEX = buildEnchantmentIndex();

    private final LoyCustomMobs plugin;
    private final Random random = new Random();

    private Map<MobRarity, RarityBookSetting> raritySettings = new EnumMap<>(MobRarity.class);
    private Map<String, EnchantmentPool> enchantmentPools = new HashMap<>();
    private Map<String, CustomItemDefinition> customItems = new HashMap<>();
    private Map<EntityType, MobLootTable> mobLootTables = new EnumMap<>(EntityType.class);
    private MobLootTable defaultLootTable;

    private boolean enhancedDropsEnabled;
    private int maxBonusDrops;

    public LootManager(LoyCustomMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialisation et chargement des tables de loot.
     */
    public void initialize() {
        plugin.getLogger().info("Initialisation du LootManager...");
        loadConfiguration();
        loadLootDefinitions();
        plugin.getLogger().info("Tables de loot chargées pour " + mobLootTables.size() + " types spécifiques");
    }

    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfig();
        this.enhancedDropsEnabled = config.getBoolean("loot.enhanced-drops", true);
        this.maxBonusDrops = Math.max(1, config.getInt("loot.max-bonus-drops", 3));
    }

    private void loadLootDefinitions() {
        FileConfiguration lootConfig = plugin.getConfigManager().getLootConfig();
        if (lootConfig == null) {
            plugin.getLogger().warning("Impossible de charger le fichier loot.yml – aucune table personnalisée ne sera appliquée");
            raritySettings.clear();
            enchantmentPools.clear();
            customItems.clear();
            mobLootTables.clear();
            defaultLootTable = null;
            return;
        }

        loadRaritySettings(lootConfig);
        loadEnchantmentPools(lootConfig);
        loadCustomItems(lootConfig);
        loadMobTables(lootConfig);
    }

    private void loadRaritySettings(FileConfiguration config) {
        raritySettings = new EnumMap<>(MobRarity.class);
        ConfigurationSection section = config.getConfigurationSection("rarity-settings");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection raritySection = section.getConfigurationSection(key);
                if (raritySection == null) {
                    continue;
                }
                try {
                    MobRarity rarity = MobRarity.valueOf(key.toUpperCase(Locale.ROOT));
                    int minCount = raritySection.getInt("enchant-count.min", 1);
                    int maxCount = raritySection.getInt("enchant-count.max", minCount);
                    int minLevel = raritySection.getInt("levels.min", 1);
                    int maxLevel = raritySection.getInt("levels.max", minLevel);
                    List<Enchantment> special = parseEnchantmentList(raritySection.getStringList("special-enchants"));
                    raritySettings.put(rarity, new RarityBookSetting(minCount, maxCount, minLevel, maxLevel, special));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "Rareté inconnue dans loot.yml: " + key, ex);
                }
            }
        }

        // Valeurs de secours pour les raretés non configurées
        for (MobRarity rarity : MobRarity.values()) {
            raritySettings.putIfAbsent(rarity, defaultSettingFor(rarity));
        }
    }

    private void loadEnchantmentPools(FileConfiguration config) {
        enchantmentPools = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("enchantment-pools");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection poolSection = section.getConfigurationSection(key);
                if (poolSection == null) {
                    continue;
                }
                EnchantmentPool pool = new EnchantmentPool();
                for (String enchantKey : poolSection.getKeys(false)) {
                    Enchantment enchantment = resolveEnchantment(enchantKey);
                    if (enchantment == null) {
                        plugin.getLogger().warning("Enchantement inconnu dans le pool " + key + " : " + enchantKey);
                        continue;
                    }
                    int weight = Math.max(1, poolSection.getInt(enchantKey, 1));
                    pool.add(enchantment, weight);
                }
                if (!pool.isEmpty()) {
                    enchantmentPools.put(key.toUpperCase(Locale.ROOT), pool);
                }
            }
        }
        enchantmentPools.putIfAbsent("DEFAULT", defaultPool());
    }

    private void loadCustomItems(FileConfiguration config) {
        customItems = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("custom-items");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }

            boolean book = itemSection.getBoolean("book", false);
            String materialName = itemSection.getString("material");
            Material material = null;
            if (!book || materialName != null) {
                if (materialName == null) {
                    plugin.getLogger().warning("Objet custom " + key + " sans material défini");
                } else {
                    material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
                    if (material == null) {
                        plugin.getLogger().warning("Material inconnu pour l'objet custom " + key + " : " + materialName);
                        continue;
                    }
                }
            }

            Map<Enchantment, Integer> enchantments = parseEnchantmentMap(itemSection.getConfigurationSection("enchantments"));
            List<String> lore = itemSection.getStringList("lore");
            String name = itemSection.getString("name");
            boolean unbreakable = itemSection.getBoolean("unbreakable", false);
            boolean scaleWithTier = itemSection.getBoolean("scale-with-tier", false);

            customItems.put(key.toUpperCase(Locale.ROOT), new CustomItemDefinition(
                material,
                book,
                name,
                lore,
                enchantments,
                unbreakable,
                scaleWithTier
            ));
        }
    }

    private void loadMobTables(FileConfiguration config) {
        mobLootTables = new EnumMap<>(EntityType.class);
        defaultLootTable = null;

        ConfigurationSection lootSection = config.getConfigurationSection("loot");
        if (lootSection == null) {
            plugin.getLogger().warning("Section 'loot' manquante dans loot.yml");
            return;
        }

        for (String key : lootSection.getKeys(false)) {
            ConfigurationSection mobSection = lootSection.getConfigurationSection(key);
            if (mobSection == null) {
                continue;
            }

            MobLootTable table = parseMobTable(mobSection);
            if ("DEFAULT".equalsIgnoreCase(key)) {
                defaultLootTable = table;
                continue;
            }

            try {
                EntityType entityType = EntityType.valueOf(key.toUpperCase(Locale.ROOT));
                mobLootTables.put(entityType, table);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Type de mob inconnu dans loot.yml : " + key);
            }
        }
    }

    private MobLootTable parseMobTable(ConfigurationSection section) {
        List<LootDropDefinition> guaranteed = parseDrops(section, "guaranteed", false);
        List<LootDropDefinition> secondary = parseDrops(section, "secondary", true);
        List<LootDropDefinition> rare = parseDrops(section, "rare", true);
        List<LootDropDefinition> legendary = parseDrops(section, "legendary", true);
        return new MobLootTable(guaranteed, secondary, rare, legendary);
    }

    private List<LootDropDefinition> parseDrops(ConfigurationSection section, String path, boolean expectChance) {
        List<Map<?, ?>> rawList = section.getMapList(path);
        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }

        List<LootDropDefinition> drops = new ArrayList<>();
        for (Map<?, ?> raw : rawList) {
            if (!(raw instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Map<String, Object> map = rawMap.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof String)
                .collect(Collectors.toMap(entry -> (String) entry.getKey(), Map.Entry::getValue));

            boolean book = getBoolean(map, "book", false);
            String customKey = getString(map, "custom-item");
            String materialName = getString(map, "item");
            Material material = null;
            DropType dropType;
            CustomItemDefinition customDefinition = null;

            if (customKey != null) {
                customDefinition = customItems.get(customKey.toUpperCase(Locale.ROOT));
                if (customDefinition == null) {
                    plugin.getLogger().warning("Objet custom inconnu dans loot.yml : " + customKey);
                    continue;
                }
                dropType = DropType.CUSTOM;
            } else if (book) {
                dropType = DropType.BOOK;
            } else if (materialName != null) {
                material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
                if (material == null) {
                    plugin.getLogger().warning("Material inconnu dans loot.yml : " + materialName);
                    continue;
                }
                dropType = DropType.MATERIAL;
            } else {
                plugin.getLogger().warning("Entrée de loot invalide : " + map);
                continue;
            }

            double chance = expectChance ? getDouble(map, "chance", 0.0D) : getDouble(map, "chance", 1.0D);
            if (expectChance && chance <= 0.0D) {
                continue;
            }

            int minAmount = getInt(map, "min", getInt(map, "min-amount", 1));
            int maxAmount = getInt(map, "max", getInt(map, "max-amount", minAmount));
            if (minAmount > maxAmount) {
                int swap = minAmount;
                minAmount = maxAmount;
                maxAmount = swap;
            }

            String pool = getString(map, "pool");
            Map<String, Object> metadata = extractMap(map.get("metadata"));
            Map<String, Object> overrideMap = extractMap(map.get("overrides"));
            BookOverride override = parseBookOverride(overrideMap);

            boolean scaleWithTier = getBoolean(map, "scale-with-tier",
                dropType == DropType.MATERIAL || (dropType == DropType.CUSTOM && customDefinition != null && customDefinition.scaleWithTier()));

            drops.add(new LootDropDefinition(
                dropType,
                material,
                customKey,
                chance,
                minAmount,
                maxAmount,
                pool,
                override,
                scaleWithTier,
                metadata
            ));
        }

        return drops;
    }

    private Map<String, Object> extractMap(Object source) {
        if (!(source instanceof Map<?, ?> rawMap)) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    private BookOverride parseBookOverride(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return BookOverride.EMPTY;
        }

        Map<String, Object> countMap = extractMap(map.get("enchant-count"));
        Map<String, Object> levelMap = extractMap(map.get("levels"));

        Integer minCount = countMap.isEmpty() ? null : getIntObject(countMap, "min");
        Integer maxCount = countMap.isEmpty() ? null : getIntObject(countMap, "max");
        Integer minLevel = levelMap.isEmpty() ? null : getIntObject(levelMap, "min");
        Integer maxLevel = levelMap.isEmpty() ? null : getIntObject(levelMap, "max");
        List<Enchantment> special = parseEnchantmentList(asStringList(map.get("special-enchants")));

        if (minCount == null && maxCount == null && minLevel == null && maxLevel == null && special.isEmpty()) {
            return BookOverride.EMPTY;
        }

        return new BookOverride(minCount, maxCount, minLevel, maxLevel, special);
    }

    /**
     * Génère les loots pour l’événement et renvoie le détail.
     */
    public LootResult generateCustomLoot(EntityDeathEvent event, CustomMob customMob, Player killer) {
        if (!enhancedDropsEnabled) {
            return LootResult.EMPTY;
        }

        MobLootTable table = resolveMobTable(customMob.getEntity().getType());
        if (table == null) {
            return LootResult.EMPTY;
        }

        double lootMultiplier = customMob.getRarity().computeLootMultiplier(0.1 + random.nextDouble() * 0.4);
        List<ItemStack> generated = new ArrayList<>();
        List<String> descriptors = new ArrayList<>();

        generated.addAll(generateGuaranteedDrops(table.guaranteed(), customMob.getRarity(), lootMultiplier, descriptors));

        int optionalTaken = 0;
        optionalTaken += rollOptionalCategory(table.secondary(), customMob.getRarity(), lootMultiplier, descriptors, generated);
        if (optionalTaken < maxBonusDrops) {
            optionalTaken += rollOptionalCategory(table.rare(), customMob.getRarity(), lootMultiplier, descriptors, generated);
        }
        if (optionalTaken < maxBonusDrops) {
            rollOptionalCategory(table.legendary(), customMob.getRarity(), lootMultiplier, descriptors, generated);
        }

        if (!generated.isEmpty()) {
            event.getDrops().addAll(generated);
        }

        return generated.isEmpty() ? LootResult.EMPTY : new LootResult(Collections.unmodifiableList(generated), lootMultiplier, descriptors);
    }

    private List<ItemStack> generateGuaranteedDrops(List<LootDropDefinition> entries, MobRarity rarity, double lootMultiplier, List<String> descriptors) {
        List<ItemStack> drops = new ArrayList<>();
        for (LootDropDefinition def : entries) {
            ItemStack item = createItem(def, rarity);
            if (item == null) {
                continue;
            }
            applyLootMultiplier(item, lootMultiplier, def.scaleWithTier());
            drops.add(item);
            descriptors.add(describeItem(item));
        }
        return drops;
    }

    private int rollOptionalCategory(List<LootDropDefinition> entries, MobRarity rarity, double lootMultiplier, List<String> descriptors, List<ItemStack> collector) {
        if (entries.isEmpty()) {
            return 0;
        }
        List<LootDropDefinition> shuffled = new ArrayList<>(entries);
        Collections.shuffle(shuffled, random);

        for (LootDropDefinition def : shuffled) {
            if (random.nextDouble() > def.chance()) {
                continue;
            }
            ItemStack item = createItem(def, rarity);
            if (item == null) {
                continue;
            }
            applyLootMultiplier(item, lootMultiplier, def.scaleWithTier());
            collector.add(item);
            descriptors.add(describeItem(item));
            return 1;
        }
        return 0;
    }

    private ItemStack createItem(LootDropDefinition def, MobRarity rarity) {
        return switch (def.dropType()) {
            case MATERIAL -> createMaterialItem(def);
            case BOOK -> createBookItem(def, rarity);
            case CUSTOM -> createCustomItem(def);
        };
    }

    private ItemStack createMaterialItem(LootDropDefinition def) {
        if (def.material() == null) {
            return null;
        }
        int amount = randomBetween(def.minAmount(), def.maxAmount());
        ItemStack item = new ItemStack(def.material(), amount);

        if (!def.metadata().isEmpty() && item.getItemMeta() instanceof PotionMeta potionMeta) {
            applyPotionMetadata(potionMeta, def.metadata());
            item.setItemMeta(potionMeta);
        }
        return item;
    }

    private ItemStack createBookItem(LootDropDefinition def, MobRarity rarity) {
        EnchantmentPool pool = resolvePool(def.pool());
        if (pool == null || pool.isEmpty()) {
            return null;
        }

        RarityBookSetting base = raritySettings.getOrDefault(rarity, defaultSettingFor(rarity));
        BookOverride override = def.override();

        int minCount = override.minCount() != null ? override.minCount() : base.minCount();
        int maxCount = override.maxCount() != null ? override.maxCount() : base.maxCount();
        int minLevel = override.minLevel() != null ? override.minLevel() : base.minLevel();
        int maxLevel = override.maxLevel() != null ? override.maxLevel() : base.maxLevel();
        int enchantCount = randomBetween(minCount, maxCount);

        List<Enchantment> special = !override.special().isEmpty() ? override.special() : base.special();

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        Set<Enchantment> used = new HashSet<>();

        if (!special.isEmpty() && random.nextDouble() < 0.35) {
            Enchantment specialEnchant = special.get(random.nextInt(special.size()));
            int level = Math.min(specialEnchant.getMaxLevel(), Math.max(minLevel, maxLevel));
            meta.addStoredEnchant(specialEnchant, Math.max(1, level), true);
            used.add(specialEnchant);
        }

        while (meta.getStoredEnchants().size() < enchantCount) {
            Enchantment drawn = pool.draw(random, used);
            if (drawn == null) {
                break;
            }
            int level = Math.min(drawn.getMaxLevel(), randomBetween(minLevel, maxLevel));
            meta.addStoredEnchant(drawn, Math.max(1, level), true);
            used.add(drawn);
        }

        Component display = LegacyComponentSerializer.legacySection().deserialize("§bGrimoire curieux (" + rarity.getDisplayName() + ")");
        meta.displayName(display);
        List<Component> lore = new ArrayList<>();
        lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Affinité : §f" + (def.pool() == null ? "Généraliste" : def.pool().toUpperCase(Locale.ROOT))));
        lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Nombre d'enchantements : §f" + meta.getStoredEnchants().size()));
        meta.lore(lore);

        book.setItemMeta(meta);
        return book;
    }

    private ItemStack createCustomItem(LootDropDefinition def) {
        String key = def.customKey();
        if (key == null) {
            return null;
        }
        CustomItemDefinition definition = customItems.get(key.toUpperCase(Locale.ROOT));
        if (definition == null) {
            plugin.getLogger().warning("Objet custom introuvable : " + key);
            return null;
        }

        Material material = definition.book() ? Material.ENCHANTED_BOOK : Objects.requireNonNullElse(definition.material(), Material.PAPER);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (definition.book() && meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : definition.enchantments().entrySet()) {
                storageMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
            }
            meta = storageMeta;
        } else if (!definition.enchantments().isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : definition.enchantments().entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }

        if (definition.name() != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(MobFlavor.colorize(definition.name())));
        }
        if (!definition.lore().isEmpty()) {
            List<Component> loreComponents = definition.lore().stream()
                .map(line -> LegacyComponentSerializer.legacySection().deserialize(MobFlavor.colorize(line)))
                .collect(Collectors.toList());
            meta.lore(loreComponents);
        }
        if (definition.unbreakable()) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        }
        if (!definition.enchantments().isEmpty()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        item.setAmount(Math.min(item.getMaxStackSize(), randomBetween(def.minAmount(), def.maxAmount())));
        return item;
    }

    private void applyLootMultiplier(ItemStack item, double multiplier, boolean scaleWithTier) {
        if (!scaleWithTier) {
            return;
        }
        if (item.getMaxStackSize() <= 1) {
            return;
        }
        int scaled = (int) Math.round(item.getAmount() * multiplier);
        int clamped = Math.max(1, Math.min(item.getMaxStackSize(), scaled));
        item.setAmount(clamped);
    }

    private void applyPotionMetadata(PotionMeta meta, Map<String, Object> metadata) {
        Object potionValue = metadata.get("potion");
        if (potionValue == null) {
            return;
        }
        String potionKey = potionValue.toString();
        if ("random".equalsIgnoreCase(potionKey)) {
            meta.setBasePotionType(randomPotionType());
            return;
        }
        try {
            PotionType type = PotionType.valueOf(potionKey.toUpperCase(Locale.ROOT));
            meta.setBasePotionType(type);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Potion inconnue dans loot.yml : " + potionKey);
        }
    }

    private PotionType randomPotionType() {
        return RANDOM_POTIONS.get(random.nextInt(RANDOM_POTIONS.size()));
    }

    private String describeItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String baseName;
        if (meta != null && meta.hasDisplayName()) {
            baseName = LegacyComponentSerializer.legacySection().serialize(meta.displayName());
        } else {
            baseName = formatMaterialName(item.getType());
        }
        return baseName + " §7x" + item.getAmount();
    }

    private MobLootTable resolveMobTable(EntityType type) {
        return mobLootTables.getOrDefault(type, defaultLootTable);
    }

    private EnchantmentPool resolvePool(String key) {
        if (key == null) {
            return enchantmentPools.get("DEFAULT");
        }
        return enchantmentPools.getOrDefault(key.toUpperCase(Locale.ROOT), enchantmentPools.get("DEFAULT"));
    }

    private EnchantmentPool defaultPool() {
        EnchantmentPool pool = new EnchantmentPool();
    pool.add(Enchantment.PROTECTION, 3);
    pool.add(Enchantment.UNBREAKING, 2);
    pool.add(Enchantment.SHARPNESS, 1);
        return pool;
    }

    private RarityBookSetting defaultSettingFor(MobRarity rarity) {
        return switch (rarity) {
            case COMMON, UNCOMMON -> new RarityBookSetting(1, 2, 1, 2, Collections.emptyList());
            case RARE -> new RarityBookSetting(2, 2, 3, 4, Collections.emptyList());
            case EPIC, LEGENDARY, MYTHIC -> new RarityBookSetting(2, 3, 5, 5, List.of(
                Enchantment.MENDING,
                Enchantment.INFINITY
            ));
        };
    }

    private List<Enchantment> parseEnchantmentList(List<String> keys) {
        List<Enchantment> list = new ArrayList<>();
        for (String key : keys) {
            Enchantment enchantment = resolveEnchantment(key);
            if (enchantment != null) {
                list.add(enchantment);
            } else {
                plugin.getLogger().warning("Enchantement inconnu : " + key);
            }
        }
        return list;
    }

    private Map<Enchantment, Integer> parseEnchantmentMap(ConfigurationSection section) {
        Map<Enchantment, Integer> result = new HashMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            Enchantment enchantment = resolveEnchantment(key);
            if (enchantment == null) {
                plugin.getLogger().warning("Enchantement inconnu pour un objet custom : " + key);
                continue;
            }
            result.put(enchantment, section.getInt(key, 1));
        }
        return result;
    }

    private Enchantment resolveEnchantment(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toUpperCase(Locale.ROOT)
            .replace("MINECRAFT:", "")
            .replace(':', '_')
            .replace(' ', '_');
        return ENCHANTMENT_INDEX.get(normalized);
    }

    private static Map<String, Enchantment> buildEnchantmentIndex() {
        Map<String, Enchantment> map = new HashMap<>();
        registerEnchantment(map, Enchantment.PROTECTION, "PROTECTION", "PROTECTION_ENVIRONMENTAL");
        registerEnchantment(map, Enchantment.SHARPNESS, "SHARPNESS", "DAMAGE_ALL");
        registerEnchantment(map, Enchantment.UNBREAKING, "UNBREAKING", "DURABILITY");
        registerEnchantment(map, Enchantment.POWER, "POWER", "ARROW_DAMAGE");
        registerEnchantment(map, Enchantment.PUNCH, "PUNCH", "ARROW_KNOCKBACK");
        registerEnchantment(map, Enchantment.INFINITY, "INFINITY", "ARROW_INFINITE");
        registerEnchantment(map, Enchantment.FIRE_ASPECT, "FIRE_ASPECT");
        registerEnchantment(map, Enchantment.FLAME, "FLAME", "ARROW_FIRE");
        registerEnchantment(map, Enchantment.LOOTING, "LOOTING");
        registerEnchantment(map, Enchantment.BANE_OF_ARTHROPODS, "BANE_OF_ARTHROPODS");
        registerEnchantment(map, Enchantment.SWEEPING_EDGE, "SWEEPING_EDGE");
        registerEnchantment(map, Enchantment.EFFICIENCY, "EFFICIENCY", "DIG_SPEED");
        registerEnchantment(map, Enchantment.SILK_TOUCH, "SILK_TOUCH");
        registerEnchantment(map, Enchantment.FORTUNE, "FORTUNE", "LOOT_BONUS_BLOCKS");
        registerEnchantment(map, Enchantment.FIRE_PROTECTION, "FIRE_PROTECTION");
        registerEnchantment(map, Enchantment.SMITE, "SMITE", "DAMAGE_UNDEAD");
        registerEnchantment(map, Enchantment.KNOCKBACK, "KNOCKBACK");
        registerEnchantment(map, Enchantment.MENDING, "MENDING");
        return Map.copyOf(map);
    }

    private static void registerEnchantment(Map<String, Enchantment> map, Enchantment enchantment, String... aliases) {
        for (String alias : aliases) {
            map.put(alias.toUpperCase(Locale.ROOT), enchantment);
        }
    }

    private int randomBetween(int min, int max) {
        if (min >= max) {
            return Math.max(1, min);
        }
        return min + random.nextInt(max - min + 1);
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return def;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        String stringValue = value.toString().trim();
        return stringValue.isEmpty() ? null : stringValue;
    }

    private double getDouble(Map<String, Object> map, String key, double def) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    private Integer getIntObject(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<String> asStringList(Object source) {
        if (source instanceof List<?> rawList) {
            List<String> list = new ArrayList<>();
            for (Object element : rawList) {
                if (element != null) {
                    list.add(element.toString());
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    private String formatMaterialName(Material material) {
        return Arrays.stream(material.name().toLowerCase(Locale.ROOT).split("_"))
            .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
            .collect(Collectors.joining(" "));
    }

    /**
     * Rechargement complet.
     */
    public void reload() {
        loadConfiguration();
        loadLootDefinitions();
        plugin.getLogger().info("LootManager rechargé");
    }

    // === Structures internes ===

    private record MobLootTable(
        List<LootDropDefinition> guaranteed,
        List<LootDropDefinition> secondary,
        List<LootDropDefinition> rare,
        List<LootDropDefinition> legendary
    ) {
        MobLootTable {
            guaranteed = guaranteed == null ? Collections.emptyList() : List.copyOf(guaranteed);
            secondary = secondary == null ? Collections.emptyList() : List.copyOf(secondary);
            rare = rare == null ? Collections.emptyList() : List.copyOf(rare);
            legendary = legendary == null ? Collections.emptyList() : List.copyOf(legendary);
        }
    }

    private enum DropType {
        MATERIAL,
        BOOK,
        CUSTOM
    }

    private record LootDropDefinition(
        DropType dropType,
        Material material,
        String customKey,
        double chance,
        int minAmount,
        int maxAmount,
        String pool,
        BookOverride override,
        boolean scaleWithTier,
        Map<String, Object> metadata
    ) {
        LootDropDefinition {
            metadata = metadata == null ? Collections.emptyMap() : Map.copyOf(metadata);
            override = override == null ? BookOverride.EMPTY : override;
        }
    }

    private record BookOverride(
        Integer minCount,
        Integer maxCount,
        Integer minLevel,
        Integer maxLevel,
        List<Enchantment> special
    ) {
        private static final BookOverride EMPTY = new BookOverride(null, null, null, null, Collections.emptyList());

        BookOverride {
            special = special == null ? Collections.emptyList() : List.copyOf(special);
        }
    }

    private record RarityBookSetting(int minCount, int maxCount, int minLevel, int maxLevel, List<Enchantment> special) {
        RarityBookSetting {
            special = special == null ? Collections.emptyList() : List.copyOf(special);
        }
    }

    private record CustomItemDefinition(
        Material material,
        boolean book,
        String name,
        List<String> lore,
        Map<Enchantment, Integer> enchantments,
        boolean unbreakable,
        boolean scaleWithTier
    ) {
        CustomItemDefinition {
            lore = lore == null ? Collections.emptyList() : List.copyOf(lore);
            enchantments = enchantments == null ? Collections.emptyMap() : Map.copyOf(enchantments);
        }
    }

    private static final class EnchantmentPool {
        private final List<Entry> entries = new ArrayList<>();

        void add(Enchantment enchantment, int weight) {
            entries.add(new Entry(enchantment, weight));
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        Enchantment draw(Random random, Set<Enchantment> excluded) {
            int totalWeight = 0;
            for (Entry entry : entries) {
                if (!excluded.contains(entry.enchantment())) {
                    totalWeight += entry.weight();
                }
            }
            if (totalWeight <= 0) {
                return null;
            }
            int roll = random.nextInt(totalWeight);
            for (Entry entry : entries) {
                if (excluded.contains(entry.enchantment())) {
                    continue;
                }
                roll -= entry.weight();
                if (roll < 0) {
                    return entry.enchantment();
                }
            }
            return null;
        }

        private record Entry(Enchantment enchantment, int weight) {
        }
    }

    public static class LootResult {
        public static final LootResult EMPTY = new LootResult(Collections.emptyList(), 1.0, Collections.emptyList());

        private final List<ItemStack> drops;
        private final double multiplier;
        private final List<String> descriptors;

        public LootResult(List<ItemStack> drops, double multiplier, List<String> descriptors) {
            this.drops = drops;
            this.multiplier = multiplier;
            this.descriptors = descriptors;
        }

        public List<ItemStack> drops() {
            return drops;
        }

        public double multiplier() {
            return multiplier;
        }

        public List<String> descriptors() {
            return descriptors;
        }

        public boolean isEmpty() {
            return drops.isEmpty();
        }
    }
}
