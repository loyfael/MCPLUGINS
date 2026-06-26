package loyfael.litefish.mechanics;

import loyfael.litefish.LiteFish;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Custom void fishing mechanics similar to LavaFishing
 * Allows fishing in the void with floating hook and mystical effects
 */
public class VoidFishing {
    
    public static Map<FishHook, VoidFishing> bobberList = new HashMap<>();
    private static final Random random = new Random();
    
    private final FishHook bobber;
    private final Player player;
    private BukkitTask task;
    
    // Virtual fish movement in the void
    private Location fishLocation;
    private Vector direction;
    private boolean fishSpawned = false;
    private int fishMoveTimer = 0;
    
    // Timing mechanics
    private int startFishing;
    private int catchSide;
    private int ticks = 0; // Track ticks for particle timing
    
    // Void specific constants
    private static final double VOID_SURFACE_HEIGHT = -59.0;
    private boolean entryEffectPlayed = false;
    
    public VoidFishing(LiteFish plugin, FishHook bobber, Player player) {
        this.bobber = bobber;
        this.player = player;
        
        // SÉCURITÉ : Vérifier si l'hameçon est déjà attaché à quelque chose (FAILLE !)
        if (bobber.getHookedEntity() != null) {
            // L'hameçon est accroché à quelque chose (joueur, mob, etc.)
            // NE PAS démarrer la pêche du void !
            return;
        }
        
        // Start void fishing
        
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
        
        bobberList.put(bobber, this);
        
        // TIMING plus raisonnable : 5-30 secondes pour l'APPARITION du poisson (100-600 ticks)
        int baseTiming = random.nextInt(500) + 100; // 5-30 seconds
        
        // SUPPORT LURE : Réduit le temps d'apparition selon le niveau d'enchantment
        this.startFishing = applyLureEffect(player, baseTiming);
        this.catchSide = random.nextInt(80) + 30; // Unused for now
        
        // NE PAS spawner le poisson tout de suite - attendre le timer!
        // startGoFish(); // SUPPRIMÉ!
    }
    
    /**
     * Apply Lure enchantment effect to reduce fish spawn time
     */
    private int applyLureEffect(Player player, int baseTiming) {
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (rod != null && rod.getType().name().contains("FISHING_ROD")) {
            int lureLevel = rod.getEnchantmentLevel(Enchantment.LURE);
            if (lureLevel > 0) {
                // Lure reduces fish spawn time: Level 1 = -20%, Level 2 = -40%, Level 3 = -60%
                double reduction = lureLevel * 0.20; // 20% per level
                int reducedTiming = (int) (baseTiming * (1.0 - reduction));
                
                // Minimum 2 seconds (40 ticks) even with max Lure
                return Math.max(40, reducedTiming);
            }
        }
        return baseTiming;
    }

    private void tick() {
        ticks++;
        Location bobberLoc = bobber.getLocation();
        
        // SÉCURITÉ : Vérifier si l'hameçon est attaché à un joueur (FAILLE !)
        if (bobber.getHookedEntity() != null) {
            // L'hameçon est accroché à quelque chose (joueur, mob, etc.)
            // ANNULER complètement la pêche du void !
            stop();
            return;
        }
        
        // Check if hook is in the void
        if (isInVoid(bobberLoc)) {
            // NOUVEAU COMPORTEMENT LIQUIDE RÉALISTE
            simulateVoidLiquidPhysics(bobberLoc);
            
            if (!entryEffectPlayed) {
                spawnVoidEntryEffects(bobberLoc);
                entryEffectPlayed = true;
            }
            
            // Move virtual fish towards hook (only if spawned)
            if (fishSpawned) {
                moveVirtualFish();
            }
            
            // Spawn ambient particles
            spawnVoidParticles();
        }
        
        // SUPPRIMÉ downSide - décrémente directement startFishing !
        this.startFishing--;
        
        // NOUVEAU : Quand le timer atteint 0, faire apparaître le poisson (pas la bite!)
        if (this.startFishing <= 0 && !fishSpawned) {
            // APPARITION DU POISSON après le délai 5-30 secondes + Lure
            startGoFish();
            fishSpawned = true;
            return;
        }
    }
    
    /**
     * Simule la physique d'un liquide mystique du void avec gravité réaliste
     * 
     * NE PAS TOUCHER ! MARCHE TRÈS BIEN !
     */
    private void simulateVoidLiquidPhysics(Location bobberLoc) {
        Vector velocity = bobber.getVelocity();
        double currentY = bobberLoc.getY();
        
        // Gravité du void (plus faible que l'air mais présente)
        double voidGravity = -0.08; // Plus fort que l'ancien système
        
        // Résistance du fluide void (comme de l'eau épaisse)
        double fluidResistance = 0.85; // 15% de résistance
        
        // Flottabilité - l'hameçon tend vers la "surface" du void
        double targetDepth = VOID_SURFACE_HEIGHT;
        double buoyancyForce = 0.0;
        
        if (currentY < targetDepth) {
            // Plus on est profond, plus la flottabilité pousse vers le haut
            double depthDifference = targetDepth - currentY;
            buoyancyForce = Math.min(0.12, depthDifference * 0.04); // Force proportionnelle
        } else if (currentY > targetDepth + 0.3) {
            // Si trop haut, une légère poussée vers le bas
            buoyancyForce = -0.03;
        }
        
        // Applique la physique du void
        velocity.setY(velocity.getY() + voidGravity + buoyancyForce);
        
        // Résistance du fluide (ralentit le mouvement)
        velocity.multiply(fluidResistance);
        
        // Mouvement ondulant mystique (très subtil)
        if (Math.abs(velocity.getY()) < 0.02) {
            double mysticalCurrent = Math.sin(ticks * 0.05) * 0.008; // Courant mystique
            velocity.setY(velocity.getY() + mysticalCurrent);
        }
        
        // Limite la vitesse maximale (terminal velocity dans le void)
        if (velocity.getY() < -0.15) velocity.setY(-0.15);
        if (velocity.getY() > 0.15) velocity.setY(0.15);
        
        // Applique la nouvelle vitesse
        bobber.setVelocity(velocity);
    }
    
    private void startGoFish() {
        Location bobberLoc = bobber.getLocation();
        
        // Spawn virtual fish at a random location around the bobber
        fishLocation = new Location(
            bobberLoc.getWorld(),
            bobberLoc.getX() + (random.nextInt(9) - 4), // -4 to +4 blocks
            VOID_SURFACE_HEIGHT - 1, // Slightly below the surface
            bobberLoc.getZ() + (random.nextInt(9) - 4)
        );
        
        // Direction towards the bobber
        direction = bobberLoc.toVector().subtract(fishLocation.toVector()).normalize();
    }
    
    private void moveVirtualFish() {
        if (fishLocation == null) return;
        
        fishMoveTimer++;
        Location bobberLoc = bobber.getLocation();
        
        // Move fish towards bobber
        fishLocation.add(direction.clone().multiply(0.1)); // Slow approach
        
        // Spawn cloud particles at fish location to show it moving
        if (fishMoveTimer % 5 == 0) { // Every 5 ticks
            fishLocation.getWorld().spawnParticle(
                Particle.CLOUD, 
                fishLocation, 
                3, 
                0.2, 0.1, 0.2, 
                0.02
            );
        }
        
        // Check if fish reached the bobber
        double distance = fishLocation.distance(bobberLoc);
        if (distance < 1.0) { // Fish reached the hook
            // BITE IMMÉDIATE quand le poisson atteint l'hameçon !
            triggerVoidBite();
        }
    }
    
    private void spawnVoidEntryEffects(Location location) {
        // Subtle airy entry effect - clouds spreading outward
        location.getWorld().spawnParticle(Particle.CLOUD, location, 20, 1.5, 0.5, 1.5, 0.1);
        
        // Light airy sounds instead of dark portal sounds
        player.playSound(location, Sound.BLOCK_WOOL_BREAK, 0.5f, 0.3f); // Soft whoosh
    }
    
    /**
     * NE PAS TOUCHER ! MARCHE TRÈS BIEN !
     */
    private void spawnVoidParticles() {
        Location bobberLoc = bobber.getLocation();
        
        // Particules ambiantes discrètes
        if (ticks % 100 == 0) { // Every 5 seconds - moins fréquent
            bobberLoc.getWorld().spawnParticle(
                Particle.CLOUD, 
                bobberLoc, 
                2, // Moins de particules
                0.3, 0.15, 0.3, // Spread plus petit
                0.005 // Mouvement très doux
            );
            
            // Very rare gentle wind sound
            if (Math.random() < 0.02) { // 2% chance
                player.playSound(bobberLoc, Sound.BLOCK_WOOL_BREAK, 0.05f, 1.2f);
            }
        }
        
        // Effet mystique TRÈS subtil - moins fréquent
        if (ticks % 200 == 0) { // Every 10 seconds seulement
            // Juste 2 particules en cercle
            for (int i = 0; i < 2; i++) {
                double angle = (i * Math.PI * 2) / 2; // 2 points seulement
                double x = Math.cos(angle) * 0.4; // Rayon plus petit
                double z = Math.sin(angle) * 0.4;
                
                Location particleLoc = bobberLoc.clone().add(x, 0.05, z);
                bobberLoc.getWorld().spawnParticle(
                    Particle.CLOUD, 
                    particleLoc, 
                    1, 
                    0.05, 0.02, 0.05, 
                    0.002
                );
            }
        }
    }
    
    /**
     * Triggers a void fishing bite using vanilla PlayerFishEvent for consistent behavior
     */
    private void triggerVoidBite() {
        // Clean up virtual fish immediately when bite triggers
        cleanupVirtualFish();
        
        // Get the LiteFish plugin to simulate vanilla bite behavior
        Location biteLoc = bobber.getLocation();
        
        // EFFET DE MORSURE SUBTIL ET APPROPRIÉ
        // Son doux mais perceptible
        player.playSound(biteLoc, Sound.BLOCK_WOOL_BREAK, 0.6f, 0.4f); // Whoosh doux
        
        // Particules discrètes mais visibles
        biteLoc.getWorld().spawnParticle(Particle.CLOUD, biteLoc, 12, 0.6, 0.3, 0.6, 0.03);
        
        // Quelques particules mystiques (très subtiles)
        for (int i = 0; i < 3; i++) {
            Location upLoc = biteLoc.clone().add(
                (Math.random() - 0.5) * 0.8, 
                Math.random() * 0.5, 
                (Math.random() - 0.5) * 0.8
            );
            biteLoc.getWorld().spawnParticle(Particle.ENCHANT, upLoc, 1, 0.05, 0.05, 0.05, 0.01);
        }
        
        // Create hook pulling effect - simulate fish pulling like in water
        createHookPullingEffect(biteLoc);
        
        // Instead of directly triggering mini-game, create vanilla fishing event
        // This will be caught by FishingListener with our 20%/80% system
        try {
            // Create a realistic BITE event that behaves like water fishing
            PlayerFishEvent biteEvent = new PlayerFishEvent(
                player,
                null, // No caught entity yet, just bite
                bobber,
                PlayerFishEvent.State.BITE
            );
            org.bukkit.Bukkit.getPluginManager().callEvent(biteEvent);
            
        } catch (Exception e) {
            player.sendMessage("§c[ERREUR] Impossible de déclencher la morsure du vide: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        
        // Clean up virtual fish particles and location
        cleanupVirtualFish();
        
        bobberList.remove(bobber);
        
        // Silent stop - no more portal sounds/particles when reeling in
        // The void fishing ends quietly like the wind
    }
    
    /**
     * Cleans up virtual fish particles and location to prevent particle spam
     */
    private void cleanupVirtualFish() {
        if (fishLocation != null) {
            // Reset fish variables
            fishLocation = null;
            direction = null;
            fishSpawned = false;
            fishMoveTimer = 0;
        }
    }
    
    public static boolean isVoidFishing(FishHook hook) {
        return bobberList.containsKey(hook);
    }
    
    public static VoidFishing getVoidFishing(FishHook hook) {
        return bobberList.get(hook);
    }
    
    public static boolean isInVoid(Location location) {
        // EXPANDED VOID FISHING AREA:
        // - Starting from Y-60 and below (core void fishing zone)
        // - Activation possible from Y-64 (expanded access zone)
        // - Allow distant hook casting to reach void areas
        
        // Core void zone: Y-60 and below
        if (location.getY() <= -60) {
            return true;
        }
        
        // Expanded activation zone: Y-64 to Y-60
        if (location.getY() <= -56 && location.getY() > -60) {
            // In this zone, check if there's clear void access below
            for (int y = 1; y <= 8; y++) {
                Location checkLoc = location.clone().add(0, -y, 0);
                if (checkLoc.getY() <= -60) {
                    // We can reach the void zone, enable void fishing
                    return true;
                }
                Material blockType = checkLoc.getBlock().getType();
                if (blockType != Material.AIR && 
                    blockType != Material.CAVE_AIR &&
                    blockType != Material.VOID_AIR) {
                    return false; // Solid block blocking void access
                }
            }
        }
        
        // Extended detection for hooks cast towards void areas
        // Check if there's a clear path to void below (up to 20 blocks)
        for (int y = 0; y < 20; y++) {
            Location checkLoc = location.clone().add(0, -y, 0);
            if (checkLoc.getY() <= -60) {
                return true; // Clear path to void found
            }
            Material blockType = checkLoc.getBlock().getType();
            if (blockType != Material.AIR && 
                blockType != Material.CAVE_AIR &&
                blockType != Material.VOID_AIR) {
                return false; // Solid ground found, not void fishing
            }
        }
        
        return false; // No void access found
    }
    
    public static void stopAll() {
        for (VoidFishing voidFishing : bobberList.values()) {
            voidFishing.stop();
        }
        bobberList.clear();
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public FishHook getHook() {
        return bobber;
    }
    
    /**
     * Creates a hook pulling effect - simulates fish pulling the hook down slightly
     */
    private void createHookPullingEffect(Location hookLocation) {
        // Instead of moving the hook down (which causes fall into void)
        // Create a gentle bobbing effect like in water
        new BukkitRunnable() {
            private int ticks = 0;
            private final Location originalLoc = hookLocation.clone();
            
            @Override
            public void run() {
                if (!bobber.isValid() || ticks >= 20) { // 1 second effect
                    // Return hook to original position
                    if (bobber.isValid()) {
                        bobber.teleport(originalLoc);
                    }
                    cancel();
                    return;
                }
                
                // Gentle bobbing motion like water fishing
                double bobHeight = Math.sin(ticks * 0.3) * 0.1; // Small vertical movement
                double sideMove = Math.sin(ticks * 0.2) * 0.05; // Tiny side movement
                
                Location newLoc = originalLoc.clone();
                newLoc.add(sideMove, bobHeight, 0);
                
                // Move the actual hook entity gently
                if (bobber.isValid()) {
                    bobber.teleport(newLoc);
                    
                    // Add pulling effect particles (clouds instead of water)
                    newLoc.getWorld().spawnParticle(Particle.CLOUD, newLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
                
                ticks++;
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("LiteFish"), 0L, 1L);
    }
}
