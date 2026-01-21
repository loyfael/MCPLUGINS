package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that creates an explosion effect when the mob takes damage
 */
public class ExplosiveAbility extends MobAbility {

    public ExplosiveAbility() {
        super("Explosive", "Creates an explosion when damaged",
              AbilityTrigger.ON_DAMAGED, 60); // 3 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        Location loc = mob.getLocation();

        // Create explosion effect
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // Damage nearby players
        mob.getNearbyEntities(4, 4, 4).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .forEach(player -> {
                double distance = player.getLocation().distance(loc);
                double damage = Math.max(1, 6 - distance); // 6 damage at center, decreasing with distance
                player.damage(damage, mob);

                // Add knockback effect
                org.bukkit.util.Vector knockback = player.getLocation().toVector()
                    .subtract(loc.toVector()).normalize().multiply(1.5);
                knockback.setY(0.5);
                player.setVelocity(knockback);
            });

        return true;
    }
}
