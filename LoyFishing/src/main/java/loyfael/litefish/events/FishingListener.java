package loyfael.litefish.events;

import loyfael.litefish.LiteFish;
import loyfael.litefish.minigame.FishingMiniGame;
import loyfael.litefish.mechanics.LavaFishing;
import loyfael.litefish.mechanics.VoidFishing;
import loyfael.litefish.models.FishDrop;
import loyfael.litefish.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles all fishing-related events
 */
public class FishingListener implements Listener {
    
    private final LiteFish plugin;
    private final Map<Integer, BukkitRunnable> lavaMonitors = new HashMap<>(); // Track lava monitoring tasks
    private final Map<Integer, FishDrop> pendingCustomDrops = new HashMap<>(); // Track pending custom drops for CAUGHT_FISH
    
    // ====== WATER FISHING CONFIGURATION (Modulable) ======
    // Keep vanilla most of the time; mini-game is rare and rewarding
    private static final double WATER_ANIMATION_RATE = 0.15;    // 15% chance for animation mini-game
    private static final double WATER_CUSTOM_DROP_RATE = 0.01;  // 1% generic custom drops (preserve vanilla)
    private static final double WATER_VANILLA_RATE = 0.84;      // 84% chance for vanilla items
    // Note: ANIMATION_RATE + CUSTOM_DROP_RATE + VANILLA_RATE should = 1.0 (100%)
    
    public FishingListener(LiteFish plugin) {
        this.plugin = plugin;
    }

    /**
     * Select a rarer drop for a biome by biasing toward low-chance items.
     * Strategy: filter to <= 10% chance; if empty, pick among the 5 lowest chance items with inverse weighting.
     */
    private Optional<FishDrop> getRareDropForBiome(Biome biome) {
        java.util.List<loyfael.litefish.models.FishDrop> drops = plugin.getDropManager().getDropsForBiome(biome);
        if (drops.isEmpty()) return Optional.empty();

        java.util.List<loyfael.litefish.models.FishDrop> rare = new java.util.ArrayList<>();
        for (loyfael.litefish.models.FishDrop d : drops) {
            if (d.getChance() <= 10.0) rare.add(d);
        }
        boolean usingFullPool = rare.isEmpty();
        java.util.List<loyfael.litefish.models.FishDrop> pool = usingFullPool ? new java.util.ArrayList<>(drops) : rare;

        // If using full pool, take the 5 lowest chance items to ensure rarity
        if (usingFullPool) {
            pool.sort(java.util.Comparator.comparingDouble(loyfael.litefish.models.FishDrop::getChance));
            pool = pool.subList(0, Math.min(5, pool.size()));
        }

        // Inverse-weight selection: weight = 1 / (chance + 0.01)
        double total = 0.0;
        for (loyfael.litefish.models.FishDrop d : pool) {
            total += 1.0 / (d.getChance() + 0.01);
        }
        double r = Math.random() * total;
        double acc = 0.0;
        for (loyfael.litefish.models.FishDrop d : pool) {
            acc += 1.0 / (d.getChance() + 0.01);
            if (r <= acc) return Optional.of(d);
        }
        return Optional.of(pool.get(pool.size() - 1));
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        
        // PROTECTION ABSOLUE : Si mini-jeu void actif, empêche TOUT événement fishing
        if (plugin.getVoidFishingMiniGame() != null && 
            isVoidMiniGameActive(player)) {
            // Annule COMPLÈTEMENT l'événement pendant le mini-jeu
            event.setCancelled(true);
            return;
        }
        
        Location location = event.getHook().getLocation();
        
        // Handle different fishing states
        switch (event.getState()) {
            case FISHING:
                // Player starts fishing - check for special fishing types
                
                // Check for void fishing first
                if (plugin.getConfigManager().isVoidFishingEnabled() && VoidFishing.isInVoid(location)) {
                    // Start void fishing
                    new VoidFishing(plugin, event.getHook(), player);
                    return;
                }
                
                // Start monitoring for lava contact (normal behavior)
                startLavaMonitoring(event.getHook(), player);
                break;
                
            case BITE:
                // Check if this is void fishing
                if (VoidFishing.isVoidFishing(event.getHook())) {
                    // Handle void fishing bite directly - no delays needed
                    event.setCancelled(true);
                    handleVoidFishingBite(player, event.getHook(), location);
                    return;
                }

                // Check if this is lava fishing
                if (LavaFishing.isLavaFishing(event.getHook())) {
                    // Handle lava fishing bite directly - no delays needed
                    event.setCancelled(true);
                    handleLavaFishingBite(player, event.getHook(), location);
                    return;
                }

                // Normal water fishing - apply custom drops
                if (!VoidFishing.isVoidFishing(event.getHook()) && !LavaFishing.isLavaFishing(event.getHook())) {
                    handleWaterFishingBite(player, event, location);
                }
                break;            
                
            case CAUGHT_FISH:
                // Check if this is void fishing
                if (VoidFishing.isVoidFishing(event.getHook())) {
                    // Handle void fishing catch - cancel vanilla and let our system handle it
                    event.setCancelled(true);
                    VoidFishing voidFishing = VoidFishing.getVoidFishing(event.getHook());
                    if (voidFishing != null) {
                        voidFishing.stop(); // Clean up the void fishing session
                    }
                    handleVoidCatch(player, location);
                    return;
                }
                
                // Check if this is lava fishing
                if (LavaFishing.isLavaFishing(event.getHook())) {
                    // Handle lava fishing catch - cancel vanilla and let our system handle it
                    event.setCancelled(true);
                    LavaFishing lavaFishing = LavaFishing.getLavaFishing(event.getHook());
                    if (lavaFishing != null) {
                        lavaFishing.stop(); // Clean up the lava fishing session
                    }
                    handleLavaCatch(player, location);
                    return;
                }
                
                // Normal water fishing - deliver pending mini-game rewards if any; otherwise keep vanilla
                if (!VoidFishing.isVoidFishing(event.getHook()) && !LavaFishing.isLavaFishing(event.getHook())) {
                    // FIRST: if a water mini-game stored a pending catch, apply it now
                    if (FishingMiniGame.hasPendingCatch(player)) {
                        FishingMiniGame.PendingCatch pending = FishingMiniGame.getPendingCatch(player);
                        if (pending != null) {
                            // Replace caught entity item if present; else add directly to inventory
                            if (event.getCaught() != null && event.getCaught() instanceof org.bukkit.entity.Item) {
                                org.bukkit.entity.Item itemEntity = (org.bukkit.entity.Item) event.getCaught();
                                itemEntity.setItemStack(pending.getItem());
                            } else {
                                if (player.getInventory().firstEmpty() != -1) {
                                    player.getInventory().addItem(pending.getItem());
                                } else {
                                    event.getHook().getWorld().dropItemNaturally(event.getHook().getLocation(), pending.getItem());
                                }
                            }
                            // Give stored experience
                            if (plugin.getConfigManager().getConfig().getBoolean("fishing.vanilla-exp", true)) {
                                player.giveExp(pending.getExperience());
                            }
                            // Do not deposit money here (already handled in mini-game giveRewards)
                            return; // Done handling this catch
                        }
                    }
                    // Preserve vanilla: clear any stored custom drop but don't replace
                    pendingCustomDrops.remove(event.getHook().getEntityId());
                    // Keep vanilla (still run region checks)
                    handleNormalCatch(event);
                } else {
                    handleNormalCatch(event);
                }
                break;
                
            case FAILED_ATTEMPT:
            case REEL_IN:
                // Vérifie s'il y a un mini-game void actif pour ce joueur
                if (plugin.getVoidFishingMiniGame() != null && 
                    isVoidMiniGameActive(player)) {
                    // EMPÊCHE la récupération pendant le mini-game void
                    event.setCancelled(true);
                    return; // On garde l'hameçon en place
                }
                
                // If a water mini-game stored a pending catch, deliver it on reel-in/fail
                if (FishingMiniGame.hasPendingCatch(player)) {
                    FishingMiniGame.PendingCatch pending = FishingMiniGame.getPendingCatch(player);
                    if (pending != null) {
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(pending.getItem());
                        } else {
                            event.getHook().getWorld().dropItemNaturally(event.getHook().getLocation(), pending.getItem());
                        }
                        if (plugin.getConfigManager().getConfig().getBoolean("fishing.vanilla-exp", true)) {
                            player.giveExp(pending.getExperience());
                        }
                    }
                }

                // Clean up pending custom drops
                pendingCustomDrops.remove(event.getHook().getEntityId());
                
                // Stop all special fishing types
                stopLavaMonitoring(event.getHook().getEntityId());
                
                // Stop void fishing if active
                if (VoidFishing.isVoidFishing(event.getHook())) {
                    VoidFishing voidFishing = VoidFishing.getVoidFishing(event.getHook());
                    if (voidFishing != null) {
                        voidFishing.stop();
                    }
                }
                break;
                
            default:
                break;
        }
    }
    
    private void handleNormalCatch(PlayerFishEvent event) {
        // Add custom drops in addition to vanilla fishing
        
        Player player = event.getPlayer();
        Location location = event.getHook().getLocation();
        
        // Check WorldGuard permissions
        if (plugin.getWorldGuardHook().isEnabled()) {
            if (!plugin.getWorldGuardHook().canFish(player, location)) {
                MessageUtils.sendMessage(player, "You cannot fish in this region!");
                event.setCancelled(true);
                return;
            }
        }
        
        // Get biome at fishing location
        Biome biome = location.getBlock().getBiome();
        
        // Try to get a custom drop based on individual drop chances
        Optional<FishDrop> dropOpt = plugin.getDropManager().tryGetCustomDrop(biome);
        if (!dropOpt.isPresent()) {
            // No custom drop this time, let vanilla fishing continue normally
            return;
        }
        
        // We have a custom drop - replace the vanilla item
        FishDrop drop = dropOpt.get();
        
        // Create the custom item
        ItemStack dropItem = plugin.getDropManager().createDropItem(drop);
        
        // Replace the caught item with our custom item
        if (event.getCaught() != null && event.getCaught() instanceof org.bukkit.entity.Item) {
            // Set the custom item as the caught item's ItemStack
            org.bukkit.entity.Item itemEntity = (org.bukkit.entity.Item) event.getCaught();
            itemEntity.setItemStack(dropItem);
        }
        
        // Calculate bonus experience
        ItemStack rod = player.getInventory().getItemInMainHand();
        double rodPower = plugin.getNexoHook().getFishingRodPower(rod);
        ItemStack bait = player.getInventory().getItemInOffHand();
        double baitLuck = plugin.getNexoHook().getBaitLuckBonus(bait);
        int bonusExp = (int) (drop.getExperience() * rodPower * baitLuck);
        
        // Add bonus experience on top of vanilla experience
        if (plugin.getConfigManager().getConfig().getBoolean("fishing.vanilla-exp", true)) {
            player.giveExp(bonusExp);
        }
        
        // Consume bait if it's a custom bait
        if (plugin.getNexoHook().isCustomBait(bait)) {
            if (Math.random() < 0.1) { // 10% chance to consume bait
                bait.setAmount(bait.getAmount() - 1);
                MessageUtils.sendMessage(player, "&eYour bait was consumed!");
            }
        }
        
        // Record statistics for custom drop
        plugin.getPlayerDataManager().addFishCaught(player, drop.getKey(), 1);
    }
    
    /**
     * Starts monitoring a fishing hook for lava contact
     */
    private void startLavaMonitoring(FishHook hook, Player player) {
        // Create monitoring task
        BukkitRunnable monitor = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if hook still exists
                if (hook.isDead() || !hook.isValid()) {
                    stopLavaMonitoring(hook.getEntityId());
                    return;
                }
                
                // Check if hook is near lava
                if (isNearLava(hook.getLocation())) {
                    // Start lava fishing!
                    new LavaFishing(plugin, hook, player);
                    stopLavaMonitoring(hook.getEntityId());
                }
            }
        };
        
        // Start monitoring (check every 2 ticks)
        monitor.runTaskTimer(plugin, 0L, 2L);
        lavaMonitors.put(hook.getEntityId(), monitor);
    }
    
    /**
     * Stops monitoring a hook for lava contact
     */
    private void stopLavaMonitoring(int hookEntityId) {
        BukkitRunnable monitor = lavaMonitors.remove(hookEntityId);
        if (monitor != null) {
            monitor.cancel();
        }
    }
    
    /**
     * Checks if a location is near lava (for lava fishing detection)
     */
    private boolean isNearLava(Location location) {
        // Check the block and surrounding blocks for lava
        if (location.getBlock().getType().toString().contains("LAVA")) {
            return true;
        }
        
        // Check blocks in a small radius around the hook
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location checkLoc = location.clone().add(x, y, z);
                    if (checkLoc.getBlock().getType().toString().contains("LAVA")) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Handles fishing catch in lava
     */
    private void handleLavaCatch(Player player, Location location) {
        // Get random lava drop
        Optional<FishDrop> dropOpt = plugin.getDropManager().getRandomDrop(location.getBlock().getBiome(), true);
        if (!dropOpt.isPresent()) {
            // Fallback to vanilla lava fish
            return;
        }
        
        FishDrop drop = dropOpt.get();
        
        // Create the item
        ItemStack dropItem = plugin.getDropManager().createDropItem(drop);
        
        // Apply fishing rod bonuses with lava bonus
        ItemStack rod = player.getInventory().getItemInMainHand();
        double rodPower = plugin.getNexoHook().getFishingRodPower(rod) * 1.5; // 50% bonus for lava fishing
        
        // Apply bait bonuses with lava bonus
        ItemStack bait = player.getInventory().getItemInOffHand();
        double baitLuck = plugin.getNexoHook().getBaitLuckBonus(bait) * 1.2; // 20% bonus for lava fishing
        
        // Calculate final experience with bonuses
        int finalExp = (int) (drop.getExperience() * rodPower * baitLuck);
        
        // Give the item directly to inventory or drop it
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(dropItem);
        } else {
            location.getWorld().dropItemNaturally(location, dropItem);
        }
        
        // Give experience if enabled
        if (plugin.getConfigManager().getConfig().getBoolean("fishing.vanilla-exp", true)) {
            player.giveExp(finalExp);
        }
        
        // Record statistics
        plugin.getPlayerDataManager().addFishCaught(player, drop.getKey(), 1);
    }

    /**
     * Handles successful void fishing catch
     */
    private void handleVoidCatch(Player player, Location location) {
        // Get appropriate void drop
        Optional<FishDrop> dropOpt = plugin.getDropManager().getRandomDrop(Biome.THE_VOID, false);
        FishDrop drop;
        
        if (dropOpt.isPresent()) {
            drop = dropOpt.get();
        } else {
            // Fallback void creature
            drop = new FishDrop("void_entity", "§5Entité du Vide", 
                Material.PHANTOM_MEMBRANE, 75.0, 50, 20.0, 
                java.util.Arrays.asList("THE_VOID"), "");
        }
        
        // Create and give the item
        ItemStack fishItem = plugin.getDropManager().createDropItem(drop);
        player.getInventory().addItem(fishItem);
        
        // Give experience
        player.giveExp((int) drop.getExperience());
        
        // Record statistics
        plugin.getPlayerDataManager().addFishCaught(player, drop.getKey(), 1);
    }
    
    /**
     * Check if player has an active void mini-game
     */
    private boolean isVoidMiniGameActive(Player player) {
        return plugin.getVoidFishingMiniGame().hasActiveSession(player);
    }
    
    /**
     * Handle void fishing bite - direct processing without delays
     */
    private void handleVoidFishingBite(Player player, FishHook hook, Location location) {
        // 20% chance for mini-game, 80% for direct drop
        double roll = Math.random();
        if (roll < 0.20) {
            // DEBUG: Temporary message to verify 20% rate
            // Start void mini-game
            startVoidFishingMiniGame(player, hook, location);
        } else {
            // DEBUG: Temporary message to verify 80% rate  
            // Give direct void drop
            giveDirectVoidDrop(player, location);
            // Clean up void fishing session
            VoidFishing voidFishing = VoidFishing.getVoidFishing(hook);
            if (voidFishing != null) {
                voidFishing.stop();
            }
        }
    }
    
    /**
     * Handle lava fishing bite - direct processing without delays
     */
    private void handleLavaFishingBite(Player player, FishHook hook, Location location) {
        // 20% chance for mini-game, 80% for direct drop
        double roll = Math.random();
        if (roll < 0.20) {
            // DEBUG: Temporary message to verify 20% rate
            player.sendMessage("§8[DEBUG] Lava mini-game triggered (" + String.format("%.1f", roll * 100) + "%)");
            // Start lava mini-game
            startLavaFishingMiniGame(player, hook, location);
        } else {
            // DEBUG: Temporary message to verify 80% rate
            // Give direct lava drop
            giveDirectLavaDrop(player, location, hook);
        }
    }
    
    /**
     * Handle water fishing bite - modulable rates: animation, custom drops, vanilla
     */
    private void handleWaterFishingBite(Player player, PlayerFishEvent event, Location location) {
        // Sanity check: ensure rates sum to ~1.0 to avoid misconfiguration
        double sanity = WATER_ANIMATION_RATE + WATER_CUSTOM_DROP_RATE + WATER_VANILLA_RATE;
        if (Math.abs(sanity - 1.0) > 1e-6) {
            plugin.getLogger().warning(String.format("Water fishing rates do not sum to 1.0 (%.3f)", sanity));
        }
        double roll = Math.random();
        
        if (roll < WATER_ANIMATION_RATE) {
            // TIER 1: Animation mini-game (5% by default)
            event.setCancelled(true); // Cancel vanilla since we're doing animation
            startWaterFishingAnimation(player, event.getHook(), location);
            
        } else if (roll < WATER_ANIMATION_RATE + WATER_CUSTOM_DROP_RATE) {
            // TIER 2 disabled (0%) to preserve vanilla water loot
            // Intentionally do nothing here
            
        } else {
            // TIER 3: Vanilla items (80% by default)
            // Let vanilla fishing handle this normally - don't cancel the event
        }
    }

    /**
     * Start void fishing mini-game
     */
    private void startVoidFishingMiniGame(Player player, FishHook hook, Location location) {
        // Get a void drop for the mini-game (prefer rarer items)
        Optional<FishDrop> dropOpt = getRareDropForBiome(Biome.THE_VOID);
        if (!dropOpt.isPresent()) {
            dropOpt = plugin.getDropManager().getRandomDrop(Biome.THE_VOID, false);
        }
        
        if (dropOpt.isPresent()) {
            FishDrop drop = dropOpt.get();
            plugin.getVoidFishingMiniGame().startVoidMiniGame(
                player, 
                drop.getKey(),
                drop.getDisplayName(),
                (int) drop.getExperience(),
                (int) drop.getChance(),
                location, 
                hook
            );
        } else {
            // Fallback - give direct drop instead
            giveDirectVoidDrop(player, location);
            VoidFishing voidFishing = VoidFishing.getVoidFishing(hook);
            if (voidFishing != null) {
                voidFishing.stop();
            }
        }
    }
    
    /**
     * Start water fishing animation (5% of the time)
     */
    private void startWaterFishingAnimation(Player player, FishHook hook, Location location) {
        // Cancel the vanilla event since we're handling it with animation
        // (Note: we can't cancel here since the event was already passed, but the mini-game will handle the catch)
        
        // Get biome at fishing location
        Biome biome = location.getBlock().getBiome();
        
        // Prefer rarer drop selection for the mini-game; fallback to random drop
        Optional<FishDrop> dropOpt = getRareDropForBiome(biome);
        if (!dropOpt.isPresent()) {
            dropOpt = plugin.getDropManager().getRandomDrop(biome, false);
        }
        
        if (dropOpt.isPresent()) {
            FishDrop drop = dropOpt.get();
            plugin.getFishingMiniGame().startMiniGame(
                player, 
                drop,
                location, 
                false, // isLavaFishing = false (this is water fishing)
                hook
            );
        } else {
            // Fallback - get a generic water drop or let vanilla handle
            // Try to get any water-compatible drop
            dropOpt = plugin.getDropManager().getRandomDrop(Biome.OCEAN, false);
            if (dropOpt.isPresent()) {
                FishDrop drop = dropOpt.get();
                plugin.getFishingMiniGame().startMiniGame(
                    player, 
                    drop,
                    location, 
                    false, // isLavaFishing = false
                    hook
                );
            } else {
                // Ultimate fallback - let vanilla handle this 5% as well
                player.sendMessage("§8[DEBUG] No water drops configured, letting vanilla handle");
            }
        }
    }

    /**
     * Start lava fishing mini-game
     */
    private void startLavaFishingMiniGame(Player player, FishHook hook, Location location) {
        // Get a lava drop for the mini-game (prefer rarer items)
        Optional<FishDrop> dropOpt = getRareDropForBiome(Biome.NETHER_WASTES);
        if (!dropOpt.isPresent()) {
            dropOpt = plugin.getDropManager().getRandomDrop(Biome.NETHER_WASTES, false);
        }
        
        if (dropOpt.isPresent()) {
            FishDrop drop = dropOpt.get();
            plugin.getFishingMiniGame().startMiniGame(
                player, 
                drop,
                location, 
                true, // isLavaFishing
                hook
            );
        } else {
            // Fallback - give direct drop instead
            giveDirectLavaDrop(player, location, hook);
        }
    }

    /**
     * Give direct void drop without animation (80% of the time)
     */
    private void giveDirectVoidDrop(Player player, Location location) {
        // Get a random drop for void fishing using the standard system
        Optional<FishDrop> dropOpt = plugin.getDropManager().getRandomDrop(Biome.THE_VOID, false);
        FishDrop drop;
        
        if (dropOpt.isPresent()) {
            drop = dropOpt.get();
        } else {
            // Fallback if no void drops configured
            drop = new FishDrop("void_entity", "§5Entité du Vide", 
                Material.PHANTOM_MEMBRANE, 45.0, 40, 150.0, 
                java.util.Arrays.asList("THE_VOID"), "");
        }
        
        giveDropToPlayer(player, drop);
    }
    
    /**
     * Give direct lava drop without animation (80% of the time)
     */
    private void giveDirectLavaDrop(Player player, Location location, FishHook hook) {
        // Get a random drop for lava fishing using the standard system
        Optional<FishDrop> dropOpt = plugin.getDropManager().getRandomDrop(Biome.NETHER_WASTES, false);
        FishDrop drop;
        
        if (dropOpt.isPresent()) {
            drop = dropOpt.get();
        } else {
            // Fallback if no lava drops configured
            drop = new FishDrop("lava_fish", "§cPoisson de Lave", 
                Material.TROPICAL_FISH, 35.0, 25, 75.0, 
                java.util.Arrays.asList("NETHER_WASTES"), "");
        }
        
        giveDropToPlayer(player, drop);
        
        // IMPORTANT: Stop the lava fishing session since we got a catch
        LavaFishing lavaFishing = LavaFishing.getLavaFishing(hook);
        if (lavaFishing != null) {
            lavaFishing.stop();
        }
    }
    
    /**
     * Give a drop to player with all rewards (item, exp, money, stats)
     */
    private void giveDropToPlayer(Player player, FishDrop drop) {
        // Create and give the item
        ItemStack fishItem = plugin.getDropManager().createDropItem(drop);
        player.getInventory().addItem(fishItem);
        
        // Give experience
        player.giveExp((int) drop.getExperience());
        
        // Record statistics
        plugin.getPlayerDataManager().addFishCaught(player, drop.getKey(), 1);
    }
    
    /**
     * Store a custom drop to apply later when CAUGHT_FISH event occurs
     */
    // Tier-2 custom drop storage removed to preserve vanilla water loot.
    
}
