package loyfael.litefish.mechanics;

import loyfael.litefish.LiteFish;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
 * Handles custom lava fishing mechanics since Minecraft doesn't natively support fishing in lava
 */
public class LavaFishing {
    
    public static Map<FishHook, LavaFishing> bobberList = new HashMap<>();
    private static final Random random = new Random();
    
    private final FishHook bobber;
    private final Player player;
    private BukkitTask task;
    
    // Virtual fish movement
    private Location fishLocation;
    private Vector direction;
    private double speed = 0.1;
    
    // Timing mechanics
    private int startFishing;
    private int catchSide;
    
    public LavaFishing(LiteFish plugin, FishHook bobber, Player player) {
        this.bobber = bobber;
        this.player = player;
        
        // Start the lava fishing simulation
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
        
        // Register this lava fishing session
        bobberList.put(bobber, this);
        
        // TIMING plus raisonnable : 5-30 secondes (100-600 ticks)
        int baseTiming = random.nextInt(500) + 100; // 5-30 seconds
        
        // SUPPORT LURE : Réduit le temps selon le niveau d'enchantement
        this.startFishing = -applyLureEffect(player, baseTiming); // Négatif pour le système de countdown
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
    
    /**
     * Main tick method that handles lava fishing simulation
     */
    private void tick() {
        // Check if bobber is still valid
        if (bobber.isDead()) {
            stop();
            return;
        }
        
        // Initial lava contact detection
        if (fishLocation == null) {
            Location hookLoc = bobber.getLocation();
            Block block = hookLoc.getBlock();
            
            // Check current block and blocks around/below for lava
            boolean foundLava = false;
            if (block.getType() == Material.LAVA) {
                foundLava = true;
            } else {
                // Check below and around
                for (int x = -1; x <= 1 && !foundLava; x++) {
                    for (int y = -1; y <= 1 && !foundLava; y++) {
                        for (int z = -1; z <= 1 && !foundLava; z++) {
                            Block checkBlock = hookLoc.clone().add(x, y, z).getBlock();
                            if (checkBlock.getType() == Material.LAVA) {
                                foundLava = true;
                                block = checkBlock; // Use this lava block as reference
                            }
                        }
                    }
                }
            }
            
            if (foundLava) {
                lavaContact();
            }
            return;
        }
        
        // Startup phase
        if (startFishing < 0) {
            startFishing++;
            if (startFishing == 0) {
                startGoFish();
            }
            update();
            return;
        }
        
        // Main fishing simulation
        if (startFishing == 0) {
            update();
            
            // Move virtual fish
            fishLocation.add(direction.clone().multiply(speed));
            
            // Spawn lava particles for fish movement
            bobber.getWorld().spawnParticle(
                Particle.LAVA, 
                fishLocation, 
                1, 
                0.1, 0.1, 0.1, 
                0.001
            );
            
            // Check if fish has moved too far from lava
            Block belowFish = fishLocation.getBlock().getRelative(BlockFace.DOWN);
            if (belowFish.getType() != Material.LAVA) {
                // Fish escaped, restart with realistic delay
                int baseTiming = random.nextInt(500) + 100; // 5-30 seconds
                startFishing = -applyLureEffect(bobber.getWorld().getPlayers().get(0), baseTiming); // Apply lure effect
                bobber.getWorld().playSound(fishLocation, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                bobber.getWorld().spawnParticle(
                    Particle.LAVA, 
                    fishLocation, 
                    20, 
                    0.1, 0.1, 0.1, 
                    0.001
                );
                return;
            }
            
            // Check if fish is close enough to bite
            double distance = bobber.getLocation().distance(fishLocation);
            if (distance < 0.5) {
                // BITE!
                startFishing = 60;
                catchSide = 75;
                
                // Create hook pulling effect for lava fishing
                createLavaHookPullingEffect(bobber.getLocation());
                
                // Create artificial BITE event
                PlayerFishEvent biteEvent = new PlayerFishEvent(
                    player,
                    null,
                    bobber,
                    PlayerFishEvent.State.BITE
                );
                Bukkit.getPluginManager().callEvent(biteEvent);
                
                // Sound and visual effects
                bobber.getWorld().playSound(
                    bobber.getLocation(), 
                    Sound.BLOCK_FIRE_EXTINGUISH, 
                    1.0f, 1.0f
                );
                bobber.getWorld().spawnParticle(
                    Particle.LAVA, 
                    bobber.getLocation(), 
                    20, 
                    0.1, 0.1, 0.1, 
                    0.001
                );
            }
        } else {
            update();
        }
    }
    
    /**
     * Stops the lava fishing simulation
     */
    public void stop() {
        if (task != null) {
            task.cancel();
        }
        bobberList.remove(bobber);
    }
    
    /**
     * Handles the fire event (when player reels in)
     */
    public void fire(PlayerFishEvent event) {
        // Don't interfere if mini-game is enabled - we'll handle this through the normal fishing listener
        // Just let the event pass through to our main fishing system
        
        // Handle reel in during bite phase
        if (event.getState() == PlayerFishEvent.State.REEL_IN && startFishing > 0) {
            // Create artificial CAUGHT_FISH event
            PlayerFishEvent catchEvent = new PlayerFishEvent(
                player,
                null,
                bobber,
                PlayerFishEvent.State.CAUGHT_FISH
            );
            Bukkit.getPluginManager().callEvent(catchEvent);
            
            // Success sound and effects
            bobber.getWorld().playSound(
                bobber.getLocation(), 
                Sound.BLOCK_LAVA_POP, 
                1.0f, 2.0f
            );
            bobber.getWorld().spawnParticle(
                Particle.FLAME, 
                bobber.getLocation(), 
                20, 
                0.3, 0.3, 0.3, 
                0.01
            );
        }
    }
    
    /**
     * Updates bobber position and fishing timer
     */
    private void update() {
        // Decrease fishing timer
        if (startFishing > 1) {
            startFishing--;
            if (startFishing == 1) {
                // Reset for another try with realistic delay - our main fishing system will handle mini-game logic
                int baseTiming = random.nextInt(500) + 100; // 5-30 seconds
                startFishing = -applyLureEffect(player, baseTiming);
            }
        }
        
        // Ensure bobber stays at lava surface
        if (fishLocation != null) {
            Location currentLocation = bobber.getLocation();
            Block surfaceBlock = currentLocation.getBlock();
            
            // Find the actual lava surface
            if (surfaceBlock.getType() == Material.LAVA) {
                // If we're in lava, go up until we find the surface
                while (surfaceBlock.getRelative(BlockFace.UP).getType() == Material.LAVA) {
                    surfaceBlock = surfaceBlock.getRelative(BlockFace.UP);
                }
                
                // Set bobber to float just above the lava surface
                double targetY = surfaceBlock.getLocation().getY() + 0.9;
                if (currentLocation.getY() < targetY - 0.1 || currentLocation.getY() > targetY + 0.2) {
                    Location newLocation = currentLocation.clone();
                    newLocation.setY(targetY);
                    bobber.teleport(newLocation);
                }
            }
        }
        
        // Handle bite phase
        if (catchSide > 0) {
            catchSide--;
            if (catchSide > 70) {
                // Pull down during bite
                bobber.setVelocity(new Vector(0, -0.025, 0));
            } else if (catchSide <= 10) {
                // Pull up after bite
                bobber.setVelocity(new Vector(0, 0.035, 0));
            }
        }
        
        // Prevent fire damage
        bobber.setFireTicks(0);
    }
    
    /**
     * Starts the virtual fish simulation
     */
    private void startGoFish() {
        Block block = bobber.getLocation().getBlock();
        
        // If bobber is in air, check below
        if (block.getType() == Material.AIR) {
            block = block.getRelative(BlockFace.DOWN);
        }
        
        // Create random fish location near the bobber
        fishLocation = block.getLocation().add(
            random.nextInt(9) - 4,  // -4 to 4
            1.1,                    // Slightly above lava surface
            random.nextInt(9) - 4   // -4 to 4
        );
        
        // Ensure fish is above lava
        Block belowFish = fishLocation.getBlock().getRelative(BlockFace.DOWN);
        if (belowFish.getType() != Material.LAVA) {
            // Not above lava, restart with realistic delay
            int baseTiming = random.nextInt(500) + 100; // 5-30 seconds
            startFishing = -applyLureEffect(player, baseTiming);
            return;
        }
        
        // Calculate direction from fish to bobber
        direction = bobber.getLocation().toVector()
            .subtract(fishLocation.toVector())
            .normalize();
        direction.setY(0).normalize(); // Keep fish at surface level
    }
    
    /**
     * Handles initial contact with lava surface
     */
    private void lavaContact() {
        Block block = bobber.getLocation().getBlock();
        
        // Find the lava surface (topmost lava block)
        while (block.getRelative(BlockFace.UP).getType() == Material.LAVA) {
            block = block.getRelative(BlockFace.UP);
        }
        
        // Position bobber exactly at lava surface (on top of the lava block)
        Location surfaceLocation = block.getLocation().clone();
        surfaceLocation.setY(surfaceLocation.getY() + 0.9); // Just above the lava surface
        surfaceLocation.setX(bobber.getLocation().getX());
        surfaceLocation.setZ(bobber.getLocation().getZ());
        
        // Teleport bobber to surface and keep it there
        bobber.teleport(surfaceLocation);
        bobber.setVelocity(new Vector(0, 0, 0)); // Stop any downward movement
        
        // Sound and visual effects for lava contact
        bobber.getWorld().playSound(
            surfaceLocation, 
            Sound.BLOCK_LAVA_POP, 
            1.0f, 1.0f
        );
        
        // Add lava particles
        bobber.getWorld().spawnParticle(
            Particle.LAVA, 
            surfaceLocation, 
            10, 
            0.2, 0.1, 0.2, 
            0.01
        );
        
        // Mark initial fish location at bobber position
        fishLocation = surfaceLocation.clone();
    }
    
    /**
     * Checks if a fish hook is in lava fishing mode
     */
    public static boolean isLavaFishing(FishHook hook) {
        return bobberList.containsKey(hook);
    }
    
    /**
     * Gets the lava fishing instance for a hook
     */
    public static LavaFishing getLavaFishing(FishHook hook) {
        return bobberList.get(hook);
    }
    
    /**
     * Stops all active lava fishing sessions (called on plugin disable)
     */
    public static void stopAll() {
        for (LavaFishing lavaFishing : bobberList.values()) {
            lavaFishing.stop();
        }
        bobberList.clear();
    }
    
    /**
     * Gets the player associated with this lava fishing session
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Creates a hook pulling effect for lava fishing - simulates fish pulling the hook down
     */
    private void createLavaHookPullingEffect(Location hookLocation) {
        // Animate hook being pulled down for realistic fishing feel
        new BukkitRunnable() {
            private int ticks = 0;
            private final Location originalLoc = hookLocation.clone();
            
            @Override
            public void run() {
                if (!bobber.isValid() || ticks >= 10) { // 0.5 second effect
                    cancel();
                    return;
                }
                
                // Gently pull hook down with lava-specific movement
                double pullDown = Math.sin(ticks * 0.4) * 0.3; // Slightly stronger pull for lava
                double wobble = Math.sin(ticks * 0.6) * 0.15; // More wobble in lava
                
                Location newLoc = originalLoc.clone();
                newLoc.add(wobble, -pullDown, 0);
                
                // Move the actual hook entity
                if (bobber.isValid()) {
                    bobber.teleport(newLoc);
                    
                    // Add lava-specific pulling effect particles
                    newLoc.getWorld().spawnParticle(Particle.LAVA, newLoc, 3, 0.1, 0.1, 0.1, 0.01);
                    newLoc.getWorld().spawnParticle(Particle.FLAME, newLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
                
                ticks++;
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("LiteFish"), 0L, 1L);
    }
}
