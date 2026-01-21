package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Optimized Ender ability that teleports the mob to its target with enhanced logic
 */
public class EnderAbility extends MobAbility {

    private static final double MAX_TELEPORT_DISTANCE = 32.0;
    private static final double MIN_TELEPORT_DISTANCE = 5.0;

    public EnderAbility() {
        super("Ender", "Teleports to target location with smart positioning",
              AbilityTrigger.ON_TARGET_ACQUIRED, 100); // 5 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null || target.isDead()) return false;

        Location targetLocation = target.getLocation();
        Location originalLocation = mob.getLocation();

        // Distance check for performance
        double distance = originalLocation.distance(targetLocation);
        if (distance < MIN_TELEPORT_DISTANCE || distance > MAX_TELEPORT_DISTANCE) {
            return false;
        }

        // Find optimal teleport position (behind player)
        Location teleportLocation = findOptimalTeleportLocation(target, originalLocation);
        if (teleportLocation == null) {
            teleportLocation = targetLocation; // Fallback
        }

        // Pre-teleport effects (async for performance)
        spawnTeleportEffects(originalLocation, false);

        // Teleport mob
        boolean success = mob.teleport(teleportLocation);

        if (success) {
            // Post-teleport effects
            spawnTeleportEffects(teleportLocation, true);
        }

        return success;
    }

    /**
     * Find optimal teleport location behind the target
     */
    private Location findOptimalTeleportLocation(Player target, Location originalLocation) {
        Vector direction = target.getLocation().getDirection();
        direction.multiply(-2.5); // 2.5 blocks behind player

        Location behindPlayer = target.getLocation().add(direction);

        // Ensure safe teleport location
        if (isSafeLocation(behindPlayer)) {
            return behindPlayer;
        }

        // Try alternative positions in a circle around target
        for (int angle = 0; angle < 360; angle += 45) {
            double radians = Math.toRadians(angle);
            double offsetX = Math.cos(radians) * 3.0;
            double offsetZ = Math.sin(radians) * 3.0;

            Location alternative = target.getLocation().add(offsetX, 0, offsetZ);
            if (isSafeLocation(alternative)) {
                return alternative;
            }
        }

        return null; // No safe location found
    }

    /**
     * Check if location is safe for teleportation
     */
    private boolean isSafeLocation(Location location) {
        if (location.getBlock().getType().isSolid()) return false;
        if (location.add(0, 1, 0).getBlock().getType().isSolid()) return false;
        if (!location.subtract(0, 2, 0).getBlock().getType().isSolid()) return false; // Need ground

        return true;
    }

    /**
     * Optimized particle effects
     */
    private void spawnTeleportEffects(Location location, boolean isDestination) {
        // Reduced particle count for performance
        location.getWorld().spawnParticle(
            Particle.PORTAL,
            location.add(0, 1, 0),
            15, // Reduced from 20
            0.3, 0.8, 0.3, // Smaller spread
            0.1
        );

        location.getWorld().playSound(
            location,
            Sound.ENTITY_ENDERMAN_TELEPORT,
            0.8f, // Slightly quieter
            isDestination ? 1.2f : 0.8f // Different pitch for destination
        );
    }
}
