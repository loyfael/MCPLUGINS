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
 * Ability that hurls spectral chains to drag its victim closer.
 */
public class AbyssalChainAbility extends MobAbility {

    public AbyssalChainAbility() {
        super("Chaînes abyssales", "Attire brutalement sa cible vers lui", AbilityTrigger.ON_ATTACK, 100);
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null || mob.isDead()) {
            return false;
        }

        Location mobLocation = mob.getLocation();
        World world = mobLocation.getWorld();
        if (world == null || !target.getWorld().equals(world)) {
            return false;
        }

        Vector pull = mobLocation.toVector().subtract(target.getLocation().toVector());
        if (pull.lengthSquared() < 1.0E-4) {
            pull = new Vector(0, 0.3, 0);
        } else {
            pull.normalize().multiply(1.4);
            pull.setY(Math.min(0.6, Math.max(0.3, pull.getY() + 0.3)));
        }

        target.setVelocity(pull);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1, false, true, true));

        world.spawnParticle(Particle.PORTAL, target.getLocation().clone().add(0, 1, 0), 30, 0.6, 0.8, 0.6, 0.2);
        world.playSound(mobLocation, Sound.ENTITY_ZOGLIN_ATTACK, 1.1f, 0.6f);
        return true;
    }
}
