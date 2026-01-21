package loyfael.abilities;

import loyfael.models.AbilityTrigger;
import loyfael.models.MobAbility;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Ability that detonates in a burst of thorns when the mob dies.
 */
public class ThornBurstAbility extends MobAbility {

    public ThornBurstAbility() {
        super("Explosion d'épines", "Libère un nuage d'épines mortelles", AbilityTrigger.ON_DEATH, 20);
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        Location location = mob.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        boolean affected = false;
        for (Player nearby : world.getPlayers()) {
            if (!nearby.getWorld().equals(world) || nearby.isDead()) {
                continue;
            }
            if (nearby.getLocation().distanceSquared(location) > 25.0) { // 5 block radius
                continue;
            }

            nearby.damage(4.0, mob);
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1, false, true, true));
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true, true));

            Vector push = nearby.getLocation().toVector().subtract(location.toVector());
            if (push.lengthSquared() > 1.0E-4) {
                push.normalize().multiply(0.75);
                push.setY(0.4);
                nearby.setVelocity(push);
            }
            affected = true;
        }

        if (affected) {
            world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, location.clone().add(0, 1, 0), 40, 1.2, 0.8, 1.2, 0.05);
            world.playSound(location, Sound.BLOCK_BAMBOO_BREAK, 1.0f, 0.5f);
        }

        return affected;
    }
}
