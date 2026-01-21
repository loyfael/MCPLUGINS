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
 * Ability that creates a magnetic force pulling nearby players towards the mob
 */
public class MagneticAbility extends MobAbility {

    public MagneticAbility() {
        super("Magnetic", "Pulls nearby players towards the mob with magnetic force",
              AbilityTrigger.PERIODIC, 140); // 7 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        Location mobLoc = mob.getLocation();
        boolean affectedSomeone = false;

        // Find all nearby players within 8 blocks
        for (Player nearby : mobLoc.getWorld().getPlayers()) {
            double distance = nearby.getLocation().distance(mobLoc);

            if (distance <= 8.0 && distance > 1.0) { // Don't affect too close players
                // Calculate pull force (stronger when closer)
                double pullStrength = (8.0 - distance) / 8.0 * 1.5; // Max 1.5 pull strength

                // Calculate direction vector from player to mob
                Vector pullDirection = mobLoc.toVector().subtract(nearby.getLocation().toVector());
                pullDirection.normalize();
                pullDirection.multiply(pullStrength);
                pullDirection.setY(pullDirection.getY() + 0.2); // Add slight upward component

                // Apply magnetic pull
                nearby.setVelocity(pullDirection);

                // Visual and sound effects
                Location playerLoc = nearby.getLocation();
                playerLoc.getWorld().spawnParticle(Particle.DUST,
                    playerLoc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);

                affectedSomeone = true;
            }
        }

        if (affectedSomeone) {
            // Sound effect at mob location
            mobLoc.getWorld().playSound(mobLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);

            // Particle effect around mob
            mobLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                mobLoc.add(0, 1, 0), 20, 2.0, 2.0, 2.0, 0.1);
        }

        return affectedSomeone;
    }
}
