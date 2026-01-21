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

/**
 * Ability that plunges its victim into darkness with a chilling swoop.
 */
public class UmbralShroudAbility extends MobAbility {

    public UmbralShroudAbility() {
        super("Voile ombral", "Plonge sa cible dans l'obscurité", AbilityTrigger.ON_ATTACK, 120);
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null || mob.isDead()) {
            return false;
        }

        Location targetLocation = target.getLocation();
        World world = targetLocation.getWorld();
        if (world == null) {
            return false;
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, false, true, true));

    world.spawnParticle(Particle.SOUL_FIRE_FLAME, targetLocation.clone().add(0, 1, 0), 30, 0.6, 0.8, 0.6, 0.01);
        world.playSound(targetLocation, Sound.ENTITY_PHANTOM_SWOOP, 0.8f, 0.4f);
        return true;
    }
}
