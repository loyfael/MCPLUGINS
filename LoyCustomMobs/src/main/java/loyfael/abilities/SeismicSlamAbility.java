package loyfael.abilities;

import loyfael.models.AbilityTrigger;
import loyfael.models.MobAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Ability that unleashes a seismic shockwave when the mob is struck.
 */
public class SeismicSlamAbility extends MobAbility {

    public SeismicSlamAbility() {
        super("Onde sismique", "Projette une onde de choc autour de lui", AbilityTrigger.ON_DAMAGED, 140);
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (mob.isDead()) {
            return false;
        }

        Location origin = mob.getLocation();
        World world = origin.getWorld();
        if (world == null) {
            return false;
        }

        boolean affected = false;
        for (Player nearby : world.getPlayers()) {
            if (!nearby.isOnline() || nearby.isDead()) {
                continue;
            }
            if (!nearby.getWorld().equals(world)) {
                continue;
            }

            double distanceSquared = nearby.getLocation().distanceSquared(origin);
            if (distanceSquared > 36.0) { // 6 block radius
                continue;
            }

            Vector push = nearby.getLocation().toVector().subtract(origin.toVector());
            if (push.lengthSquared() < 1.0E-4) {
                push = new Vector(0, 0.5, 0);
            } else {
                push.normalize().multiply(1.2);
                push.setY(0.6);
            }

            nearby.setVelocity(push);
            nearby.damage(2.5, mob);
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, true, true));
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 100, 0, false, true, true));
            affected = true;
        }

        if (affected) {
            Material ground = origin.clone().subtract(0, 1, 0).getBlock().getType();
            if (!ground.isSolid()) {
                ground = Material.DEEPSLATE;
            }
            BlockData data = ground.createBlockData();
            world.spawnParticle(Particle.BLOCK, origin, 45, 1.3, 0.2, 1.3, 0.02, data);
            world.playSound(origin, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.7f);
        }

        return affected;
    }
}
