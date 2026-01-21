package loyfael.managers;

import loyfael.LoyCustomMobs;
import loyfael.models.CustomMob;
import loyfael.models.MobRarity;
import loyfael.models.MobAbility;
import loyfael.abilities.*;
import loyfael.events.CustomMobSpawnEvent;
import loyfael.utils.MobCache;
import loyfael.utils.MobFlavor;
import org.bukkit.Location;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages custom mobs spawning, tracking, and abilities
 */
public class MobManager {
    private final LoyCustomMobs plugin;
    private final Map<UUID, CustomMob> activeMobs;
    private final Map<String, Class<? extends MobAbility>> registeredAbilities;
    private final Random random;
    private final MobCache mobCache;

    // Configuration values
    private boolean enabled;
    private double spawnChance;
    private List<EntityType> allowedEntities;
    private List<String> disabledWorlds;

    public MobManager(LoyCustomMobs plugin) {
        this.plugin = plugin;
        this.activeMobs = new ConcurrentHashMap<>();
        this.registeredAbilities = new HashMap<>();
        this.random = new Random();
        this.mobCache = new MobCache();
    }

    /**
     * Initialize the mob manager
     */
    public void initialize() {
        plugin.getLogger().info("Initializing MobManager...");

        loadConfiguration();
        registerDefaultAbilities();

        plugin.getLogger().info("MobManager initialized with " + registeredAbilities.size() + " abilities");
    }

    /**
     * Load configuration values
     */
    private void loadConfiguration() {
        this.enabled = plugin.getConfig().getBoolean("mobs.enabled", true);
        this.spawnChance = plugin.getConfig().getDouble("mobs.spawn-chance", 0.1);

        List<String> entityNames = plugin.getConfig().getStringList("mobs.allowed-entities");
        this.allowedEntities = new ArrayList<>();
        for (String name : entityNames) {
            try {
                EntityType type = EntityType.valueOf(name.toUpperCase());
                allowedEntities.add(type);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid entity type in config: " + name);
            }
        }

        this.disabledWorlds = plugin.getConfig().getStringList("mobs.disabled-worlds");
    }

    /**
     * Register default mob abilities
     */
    private void registerDefaultAbilities() {
        plugin.getLogger().info("Registering default abilities...");

        // Original abilities
        registerAbility("arrow_homing", ArrowHomingAbility.class);
        registerAbility("explosive", ExplosiveAbility.class);
        registerAbility("poisonous", PoisonousAbility.class);
        registerAbility("teleport", TeleportAbility.class);

        // Newly migrated abilities from old system
        registerAbility("withering", WitheringAbility.class);
        registerAbility("blinding", BlindingAbility.class);
        registerAbility("necromancer", NecromancerAbility.class);
        registerAbility("sapper", SapperAbility.class);
        registerAbility("confusing", ConfusingAbility.class);
        registerAbility("ghastly", GhastlyAbility.class);
        registerAbility("tosser", TosserAbility.class);
        registerAbility("gravity", GravityAbility.class);
        registerAbility("ender", EnderAbility.class);

        // New creative abilities (optimized edition)
        registerAbility("freezing", FreezingAbility.class);
        registerAbility("magnetic", MagneticAbility.class);
        registerAbility("swapper", SwapperAbility.class);
        registerAbility("prisoner", PrisonerAbility.class);
        registerAbility("electric", ElectricAbility.class);

        // Fresh additions
        registerAbility("vampiric", VampiricAbility.class);
        registerAbility("storm_caller", StormCallerAbility.class);
        registerAbility("bulwark", BulwarkAbility.class);
        registerAbility("toxic_cloud", ToxicCloudAbility.class);

    // Overflow additions for extra variety
    registerAbility("shadowstep", ShadowstepAbility.class);
    registerAbility("seismic_slam", SeismicSlamAbility.class);
    registerAbility("sandstorm", SandstormAbility.class);
    registerAbility("frenzy", FrenzyAbility.class);
    registerAbility("thorn_burst", ThornBurstAbility.class);
    registerAbility("starlight_veil", StarlightVeilAbility.class);
    registerAbility("abyssal_chain", AbyssalChainAbility.class);
    registerAbility("umbral_shroud", UmbralShroudAbility.class);
    }

    /**
     * Register a new mob ability
     */
    public void registerAbility(String name, Class<? extends MobAbility> abilityClass) {
        registeredAbilities.put(name.toLowerCase(), abilityClass);
        plugin.getLogger().info("Registered ability: " + name);
    }

    /**
     * Check if a mob should be converted to a custom mob
     */
    public boolean shouldConvertMob(LivingEntity entity) {
        if (!enabled) return false;
        if (entity.hasMetadata("CustomMob")) return false;
        if (!allowedEntities.contains(entity.getType())) return false;
        if (disabledWorlds.contains(entity.getWorld().getName())) return false;

        return random.nextDouble() < spawnChance;
    }

    /**
     * Convert a regular mob to a custom mob
     */
    public CustomMob convertToCustomMob(LivingEntity entity) {
        if (entity.hasMetadata("CustomMob")) {
            return getCustomMob(entity.getUniqueId());
        }

        // Vérifier les limites avant conversion
        if (!checkSpawnLimits(entity)) {
            return null;
        }

        MobRarity rarity = MobRarity.getRandomRarity();
        List<MobAbility> abilities = generateRandomAbilities(rarity);

        CustomMob customMob = new CustomMob(entity, rarity, abilities);

        // Apply metadata
        entity.setMetadata("CustomMob", new FixedMetadataValue(plugin, true));
        entity.setMetadata("CustomMobId", new FixedMetadataValue(plugin, customMob.getId().toString()));

        // Apply visual effects and name
        applyCustomMobEffects(customMob);

        // Store the mob
        activeMobs.put(customMob.getId(), customMob);

        // Fire custom spawn event
        CustomMobSpawnEvent spawnEvent = new CustomMobSpawnEvent(entity, customMob);
        plugin.getServer().getPluginManager().callEvent(spawnEvent);

        // If event was cancelled, remove the mob
        if (spawnEvent.isCancelled()) {
            activeMobs.remove(customMob.getId());
            entity.removeMetadata("CustomMob", plugin);
            entity.removeMetadata("CustomMobId", plugin);
            return null;
        }

        // Log seulement en mode debug
        if (plugin.getConfig().getBoolean("plugin.debug", false)) {
            plugin.getLogger().info("Converted " + entity.getType() + " to " + rarity + " custom mob");
        }

        return customMob;
    }

    /**
     * Check spawn limits to prevent overpopulation
     */
    private boolean checkSpawnLimits(LivingEntity entity) {
        // Limite par monde
        int maxPerWorld = plugin.getConfig().getInt("performance.limits.max-mobs-per-world", 20);
        long worldCount = activeMobs.values().stream()
            .filter(mob -> mob.getEntity().getWorld().equals(entity.getWorld()))
            .count();

        if (worldCount >= maxPerWorld) {
            return false;
        }

        // Limite par chunk
        int maxPerChunk = plugin.getConfig().getInt("performance.limits.max-mobs-per-chunk", 2);
        long chunkCount = activeMobs.values().stream()
            .filter(mob -> {
                var mobChunk = mob.getEntity().getLocation().getChunk();
                var entityChunk = entity.getLocation().getChunk();
                return mobChunk.getX() == entityChunk.getX() &&
                       mobChunk.getZ() == entityChunk.getZ() &&
                       mobChunk.getWorld().equals(entityChunk.getWorld());
            })
            .count();

        if (chunkCount >= maxPerChunk) {
            return false;
        }

        return true;
    }

    /**
     * Generate random abilities for a mob based on rarity
     */
    private List<MobAbility> generateRandomAbilities(MobRarity rarity) {
        List<MobAbility> abilities = new ArrayList<>();
        int min = Math.max(1, rarity.getMinAbilities());
        int max = Math.max(min, rarity.getMaxAbilities());
        int range = max - min;
        int abilityCount = min + (range > 0 ? random.nextInt(range + 1) : 0);

        List<String> availableAbilities = new ArrayList<>(registeredAbilities.keySet());
        Collections.shuffle(availableAbilities);

        abilityCount = Math.min(abilityCount, availableAbilities.size());

        for (int i = 0; i < Math.min(abilityCount, availableAbilities.size()); i++) {
            String abilityName = availableAbilities.get(i);
            try {
                Class<? extends MobAbility> abilityClass = registeredAbilities.get(abilityName);
                MobAbility ability = abilityClass.getDeclaredConstructor().newInstance();
                abilities.add(ability);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to create ability: " + abilityName, e);
            }
        }

        return abilities;
    }

    /**
     * Apply visual effects and naming to custom mob
     */
    private void applyCustomMobEffects(CustomMob customMob) {
        LivingEntity entity = customMob.getEntity();
        MobRarity rarity = customMob.getRarity();

        var randomSource = MobFlavor.random();
        var nameBundle = MobFlavor.generateName(entity.getType(), rarity, randomSource);

        customMob.setDisplayName(nameBundle.displayName());
        customMob.setEpithet(nameBundle.familyLabel());
        customMob.setFamilyTag(nameBundle.familyLabel());
        customMob.setDeathQuip(MobFlavor.randomDeathQuip(randomSource));

        String rarityColor = "§" + getRarityColor(rarity);
        String displayName = rarityColor + nameBundle.displayName();

        Component displayComponent = LegacyComponentSerializer.legacySection().deserialize(displayName);
        entity.customName(displayComponent);
        entity.setCustomNameVisible(true);

        applyStatScaling(entity, rarity);
        applyThematicEffects(entity, nameBundle.theme());

        for (MobAbility ability : customMob.getAbilities()) {
            ability.onSpawn(entity);
        }
    }

    private void applyStatScaling(LivingEntity entity, MobRarity rarity) {
    AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double boosted = maxHealth.getBaseValue() * rarity.getHealthMultiplier();
            maxHealth.setBaseValue(boosted);
            entity.setHealth(boosted);
        }

    AttributeInstance attack = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(attack.getBaseValue() * rarity.getDamageMultiplier());
        }

    AttributeInstance speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            double boostedSpeed = speed.getBaseValue() * rarity.getSpeedMultiplier();
            // éviter les vitesses absurdes
            speed.setBaseValue(Math.min(boostedSpeed, speed.getBaseValue() + 0.4));
        }

        if (rarity.getTier() >= 4) {
            AttributeInstance follow = entity.getAttribute(Attribute.FOLLOW_RANGE);
            if (follow != null) {
                follow.setBaseValue(follow.getBaseValue() * 1.2);
            }
        }

    AttributeInstance knockback = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            double base = knockback.getBaseValue();
            knockback.setBaseValue(Math.min(1.0, base + 0.1 * rarity.getTier()));
        }

        // Résistance aléatoire 10-30 %
        int amplifier = random.nextBoolean() ? 0 : 1; // I ou II
        entity.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE,
            20 * 60 * 15,
            amplifier,
            true,
            false,
            false
        ));

        if (rarity.getTier() >= 3) {
            entity.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                20 * 30,
                0,
                true,
                false,
                false
            ));
        }
    }

    private void applyThematicEffects(LivingEntity entity, MobFlavor.MobTheme theme) {
        switch (theme) {
            case ENFLAMME -> {
                entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE,
                    20 * 60 * 10,
                    0,
                    true,
                    false,
                    false
                ));
            }
            case AERIEN -> {
                entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW_FALLING,
                    20 * 60 * 5,
                    0,
                    true,
                    false,
                    false
                ));
            }
            case AQUATIQUE -> {
                entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.WATER_BREATHING,
                    20 * 60 * 10,
                    0,
                    true,
                    false,
                    false
                ));
            }
            case SQUELETTE -> {
                entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH,
                    20 * 60 * 5,
                    0,
                    true,
                    false,
                    false
                ));
            }
            case MAGIQUE -> {
                entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING,
                    20 * 20 * 30,
                    0,
                    true,
                    false,
                    false
                ));
            }
            default -> {
                // pas d'effet spécifique
            }
        }
    }

    /**
     * Get color code for rarity
     */
    private char getRarityColor(MobRarity rarity) {
        return switch (rarity) {
            case COMMON -> 'f';      // White
            case UNCOMMON -> 'a';    // Green
            case RARE -> '9';        // Blue
            case EPIC -> 'd';        // Purple
            case LEGENDARY -> '6';   // Gold
            case MYTHIC -> 'c';      // Red
        };
    }

    /**
     * Get a custom mob by UUID
     */
    public CustomMob getCustomMob(UUID id) {
        return activeMobs.get(id);
    }

    /**
     * Remove a custom mob from tracking
     */
    public void removeCustomMob(UUID id) {
        activeMobs.remove(id);
    }

    /**
     * Get all active custom mobs
     */
    public Collection<CustomMob> getActiveMobs() {
        return new ArrayList<>(activeMobs.values());
    }

    /**
     * Handle mob death
     */
    public void handleMobDeath(CustomMob customMob, Player killer) {
        // Call ability death handlers
        for (MobAbility ability : customMob.getAbilities()) {
            ability.onDeath(customMob.getEntity(), killer);
        }

        // Clean up GUI elements
        plugin.getGuiManager().removeBossBar(customMob.getId());

        // Remove from tracking
        removeCustomMob(customMob.getId());
    }

    /**
     * Spawn a custom mob at location
     */
    public CustomMob spawnCustomMob(Location location, EntityType type, MobRarity rarity) {
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);

        List<MobAbility> abilities = generateRandomAbilities(rarity);
        CustomMob customMob = new CustomMob(entity, rarity, abilities);

        // Apply metadata and effects
        entity.setMetadata("CustomMob", new FixedMetadataValue(plugin, true));
        entity.setMetadata("CustomMobId", new FixedMetadataValue(plugin, customMob.getId().toString()));
        applyCustomMobEffects(customMob);

        activeMobs.put(customMob.getId(), customMob);

        // Call custom event
        CustomMobSpawnEvent spawnEvent = new CustomMobSpawnEvent(entity, customMob);
        plugin.getServer().getPluginManager().callEvent(spawnEvent);

        return customMob;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        activeMobs.clear();
        plugin.getLogger().info("MobManager cleaned up");
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeMobs", activeMobs.size());
        stats.put("registeredAbilities", registeredAbilities.size());
        stats.put("enabled", enabled);
        stats.put("spawnChance", spawnChance);
        return stats;
    }

    /**
     * Reload the mob manager configuration
     */
    public void reload() {
        plugin.getLogger().info("Reloading MobManager...");
        loadConfiguration();
        plugin.getLogger().info("MobManager reloaded successfully");
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> cacheStats = new HashMap<>();
        if (mobCache != null) {
            var stats = mobCache.getStats();
            cacheStats.put("currentSize", stats.currentSize);
            cacheStats.put("maxSize", stats.maxSize);
            cacheStats.put("fillPercentage", stats.fillPercentage);
        } else {
            cacheStats.put("currentSize", 0);
            cacheStats.put("maxSize", 0);
            cacheStats.put("fillPercentage", 0.0);
        }
        return cacheStats;
    }
}
