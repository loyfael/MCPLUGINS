package loyfael.listeners;

import loyfael.LoyCustomMobs;
import loyfael.managers.LootManager;
import loyfael.managers.MobManager;
import loyfael.models.CustomMob;
import loyfael.models.AbilityTrigger;
import loyfael.models.MobAbility;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Main event listener for custom mob interactions
 */
public class MobListener implements Listener {
    private final LoyCustomMobs plugin;
    private final MobManager mobManager;

    public MobListener(LoyCustomMobs plugin) {
        this.plugin = plugin;
        this.mobManager = plugin.getMobManager();
    }

    /**
     * Handle creature spawning - attempt to create custom mobs
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Skip if event is cancelled
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        // Check if this should become a custom mob
        if (mobManager.shouldConvertMob(entity)) {
            // Delay conversion to next tick to ensure entity is fully spawned
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    mobManager.convertToCustomMob(entity);
                }
            });
        }
    }

    /**
     * Handle entity damage by entity - trigger mob abilities
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;

        // Handle custom mob attacking
        if (event.getDamager() instanceof LivingEntity damager) {
            CustomMob customMob = getCustomMob(damager);
            if (customMob != null && event.getEntity() instanceof Player target) {
                // Trigger ON_ATTACK abilities avec plus de chance
                if (Math.random() < 0.6) { // 60% de chance au lieu de 100%
                    triggerAbilities(customMob, AbilityTrigger.ON_ATTACK, target);
                }
            }
        }

        // Handle custom mob being damaged
        if (event.getEntity() instanceof LivingEntity entity) {
            CustomMob customMob = getCustomMob(entity);
            if (customMob != null) {
                Player attacker = null;
                if (event.getDamager() instanceof Player) {
                    attacker = (Player) event.getDamager();
                }

                // Trigger ON_DAMAGED abilities avec plus de chance
                if (Math.random() < 0.4) { // 40% de chance
                    triggerAbilities(customMob, AbilityTrigger.ON_DAMAGED, attacker);
                }

                // Check for low health trigger
                double healthAfterDamage = entity.getHealth() - event.getFinalDamage();
                var maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    double maxHealth = maxHealthAttr.getValue();
                    double lowHealthTrigger = maxHealth * customMob.getRarity().getLowHealthThreshold();

                    if (healthAfterDamage <= lowHealthTrigger) {
                        triggerAbilities(customMob, AbilityTrigger.ON_LOW_HEALTH, attacker);
                    }

                    if (healthAfterDamage <= maxHealth * 0.20) {
                        handleCriticalHealth(customMob, entity, attacker);
                    }
                }
            }
        }
    }

    /**
     * Handle entity death - manage lives system and loot
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        CustomMob customMob = getCustomMob(entity);

        if (customMob != null) {
            Player killer = entity.getKiller();

            // Trigger ON_DEATH abilities
            triggerAbilities(customMob, AbilityTrigger.ON_DEATH, killer);

            // Handle mob death in manager
            mobManager.handleMobDeath(customMob, killer);

            // Modify drops and experience
            modifyMobDrops(event, customMob, killer);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        EntityDamageEvent lastDamage = event.getEntity().getLastDamageCause();
        if (!(lastDamage instanceof EntityDamageByEntityEvent damageEvent)) {
            return;
        }
        if (!(damageEvent.getDamager() instanceof LivingEntity damager)) {
            return;
        }

        CustomMob customMob = getCustomMob(damager);
        if (customMob == null) {
            return;
        }

        LivingEntity mobEntity = customMob.getEntity();
        var maxHealthAttr = mobEntity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }

        double maxHealth = maxHealthAttr.getValue();
        mobEntity.setHealth(Math.min(maxHealth, mobEntity.getHealth() + maxHealth * 0.25));
        mobEntity.getWorld().spawnParticle(Particle.HEART, mobEntity.getLocation().add(0, 1, 0), 12, 0.4, 0.6, 0.4, 0.02);
        mobEntity.getWorld().playSound(mobEntity.getLocation(), Sound.ENTITY_EVOKER_CELEBRATE, 1f, 1.2f);
    }

    /**
     * Handle entity target changes - trigger proximity abilities
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!(event.getTarget() instanceof Player target)) return;

        CustomMob customMob = getCustomMob(entity);
        if (customMob != null) {
            Player priorityTarget = selectPriorityTarget(customMob, entity, target);

            if (priorityTarget == null) {
                if (hasSafeMode(target)) {
                    event.setCancelled(true);
                }
                return;
            }

            if (!priorityTarget.equals(target)) {
                event.setTarget(priorityTarget);
            }

            triggerAbilities(customMob, AbilityTrigger.ON_TARGET_ACQUIRED, priorityTarget);
        }
    }

    /**
     * Handle player disconnect - clean up any player-specific data if needed
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up any player-specific data if needed
        // This could include removing the player from any tracking lists
    }

    /**
     * Handle chunk unload - clean up mobs in unloaded chunks
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        // Handle custom mobs in unloading chunks
        Entity[] entities = event.getChunk().getEntities();
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                CustomMob customMob = getCustomMob(livingEntity);
                if (customMob != null) {
                    // Could save mob data here if persistence is needed
                    mobManager.removeCustomMob(customMob.getId());
                }
            }
        }
    }

    /**
     * Get custom mob from entity
     */
    private CustomMob getCustomMob(LivingEntity entity) {
        if (!entity.hasMetadata("CustomMob")) {
            return null;
        }

        for (MetadataValue value : entity.getMetadata("CustomMobId")) {
            if (value.getOwningPlugin() == plugin) {
                try {
                    UUID mobId = UUID.fromString(value.asString());
                    return mobManager.getCustomMob(mobId);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid CustomMobId metadata: " + value.asString());
                }
            }
        }

        return null;
    }

    /**
     * Trigger abilities of a specific type
     */
    private void triggerAbilities(CustomMob customMob, AbilityTrigger trigger, Player target) {
        for (MobAbility ability : customMob.getAbilities()) {
            if (ability.getTrigger() == trigger) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        ability.execute(customMob.getEntity(), target);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error executing ability " + ability.getName() + ": " + e.getMessage());
                    }
                });
            }
        }
    }

    private Player selectPriorityTarget(CustomMob customMob, LivingEntity entity, Player originalTarget) {
        Location origin = entity.getLocation();
        List<Player> candidates = entity.getWorld().getPlayers().stream()
            .filter(player -> player.isOnline() && !player.isDead())
            .filter(player -> player.getGameMode() != GameMode.SPECTATOR)
            .filter(player -> origin.distanceSquared(player.getLocation()) <= 30 * 30)
            .filter(player -> !hasSafeMode(player))
            .toList();

        if (candidates.isEmpty()) {
            return hasSafeMode(originalTarget) ? null : originalTarget;
        }

        return candidates.stream()
            .max(Comparator.comparingInt((Player p) -> p.getStatistic(Statistic.PLAYER_KILLS))
                .thenComparingDouble(p -> -origin.distanceSquared(p.getLocation())))
            .orElse(hasSafeMode(originalTarget) ? null : originalTarget);
    }

    private boolean hasSafeMode(Player player) {
        return player != null && player.getScoreboardTags().contains("SafeMode");
    }

    private void handleCriticalHealth(CustomMob customMob, LivingEntity entity, Player attacker) {
        var maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }

        double maxHealth = maxHealthAttr.getValue();
        if (entity.getHealth() > maxHealth * 0.20) {
            return;
        }

        String family = Optional.ofNullable(customMob.getFamilyTag()).orElse("");
        switch (family) {
            case "Magique" -> attemptEmergencyTeleport(entity);
            case "Enflammé" -> igniteArea(entity, attacker);
            case "Visqueux" -> applyViscousDebuff(attacker);
            default -> {
                if (entity.getType() == EntityType.ENDERMAN) {
                    attemptEmergencyTeleport(entity);
                }
            }
        }
    }

    private void attemptEmergencyTeleport(LivingEntity entity) {
        Location origin = entity.getLocation();
        for (int i = 0; i < 6; i++) {
            double offsetX = ThreadLocalRandom.current().nextDouble(-6.0, 6.0);
            double offsetY = ThreadLocalRandom.current().nextDouble(0.0, 3.0);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-6.0, 6.0);
            Location target = origin.clone().add(offsetX, offsetY, offsetZ);

            if (!target.getBlock().isPassable()) {
                continue;
            }
            Location headSpace = target.clone().add(0, 1, 0);
            if (!headSpace.getBlock().isPassable()) {
                continue;
            }

            entity.teleport(target);
            entity.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            entity.getWorld().spawnParticle(Particle.PORTAL, target, 32, 0.5, 0.5, 0.5, 0.2);
            break;
        }
    }

    private void igniteArea(LivingEntity entity, Player attacker) {
        entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 0.8, 0), 36, 0.8, 0.5, 0.8, 0.02);
        entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.1f);
        if (attacker != null) {
            attacker.setFireTicks(Math.max(attacker.getFireTicks(), 60));
        }
    }

    private void applyViscousDebuff(Player attacker) {
        if (attacker == null) {
            return;
        }
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, true, true, true));
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, true, true, true));
    }

    private void sendDeathMessages(Player killer, CustomMob customMob, int exp, LootManager.LootResult lootResult) {
        String mobName = Optional.ofNullable(customMob.getDisplayName()).orElse(customMob.getEntity().getName());
        String quip = Optional.ofNullable(customMob.getDeathQuip()).orElse("retourne dans sa flaque.");
        String header = "§a+" + exp + " XP §7– §fLe §e" + mobName + " §7" + quip;
        killer.sendMessage(LegacyComponentSerializer.legacySection().deserialize(header));

        if (!lootResult.isEmpty()) {
            String lootLine = "§7Butin bonus (§f" + String.format(Locale.ROOT, "%.1fx", lootResult.multiplier()) + "§7) : §f"
                + String.join("§7, §f", lootResult.descriptors());
            killer.sendMessage(LegacyComponentSerializer.legacySection().deserialize(lootLine));
        }
    }

    /**
     * Modify drops and experience for custom mobs
     */
    private void modifyMobDrops(EntityDeathEvent event, CustomMob customMob, Player killer) {
        int baseExp = event.getDroppedExp();
        if (baseExp <= 0) {
            baseExp = Math.max(4, 5 * customMob.getRarity().getTier());
        }

        int finalExp = (int) Math.round(baseExp * customMob.getRarity().getXpMultiplier());
        event.setDroppedExp(finalExp);

        LootManager.LootResult lootResult = plugin.getLootManager().generateCustomLoot(event, customMob, killer);

        if (killer != null) {
            sendDeathMessages(killer, customMob, finalExp, lootResult);
        }
    }
}
