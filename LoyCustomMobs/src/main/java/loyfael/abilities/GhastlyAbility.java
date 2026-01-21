package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Ability that makes the mob shoot fireballs like a Ghast
 */
public class GhastlyAbility extends MobAbility {

    public GhastlyAbility() {
        super("Ghastly", "Shoots fireballs at targets",
              AbilityTrigger.ON_TARGET_ACQUIRED, 120); // 6 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        Location mobLoc = mob.getEyeLocation();
        Location targetLoc = target.getLocation().add(0, 1, 0); // Aim at player's body

        // Calculate direction vector
        Vector direction = targetLoc.toVector().subtract(mobLoc.toVector()).normalize();

        // Spawn fireball
        Fireball fireball = mob.getWorld().spawn(mobLoc, Fireball.class);
        fireball.setDirection(direction);
        fireball.setShooter(mob);
        fireball.setYield(1.5f); // Explosion power
        fireball.setIsIncendiary(true); // Sets fire

        // Add sound and particle effects
        mob.getWorld().playSound(mobLoc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
        mob.getWorld().spawnParticle(Particle.FLAME, mobLoc, 10, 0.5, 0.5, 0.5, 0.1);

        return true;
    }
}
