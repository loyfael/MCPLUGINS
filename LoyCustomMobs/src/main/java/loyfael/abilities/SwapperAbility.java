package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Ability that swaps positions with the target player
 */
public class SwapperAbility extends MobAbility {

    public SwapperAbility() {
        super("Swapper", "Instantly swaps positions with the target",
              AbilityTrigger.ON_DAMAGED, 120); // 6 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        Location mobLocation = mob.getLocation().clone();
        Location playerLocation = target.getLocation().clone();

        // Distance check for safety
        if (mobLocation.distance(playerLocation) > 20.0) {
            return false;
        }

        // Pre-swap effects
        spawnSwapEffects(mobLocation, true);
        spawnSwapEffects(playerLocation, true);

        // Perform the swap
        boolean mobTeleported = mob.teleport(playerLocation);
        boolean playerTeleported = target.teleport(mobLocation);

        if (mobTeleported && playerTeleported) {
            // Post-swap effects
            spawnSwapEffects(mob.getLocation(), false);
            spawnSwapEffects(target.getLocation(), false);

            return true;
        }

        return false;
    }

    private void spawnSwapEffects(Location location, boolean isPreSwap) {
        if (isPreSwap) {
            location.getWorld().spawnParticle(Particle.PORTAL, location.add(0, 1, 0), 30, 0.5, 1.0, 0.5, 0.2);
            location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
        } else {
            location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, location.add(0, 1, 0), 25, 0.5, 1.0, 0.5, 0.1);
            location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        }
    }
}
