package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that calls down lightning on the mob's target when it acquires one.
 */
public class StormCallerAbility extends MobAbility {

    public StormCallerAbility() {
        super("Appel de tempête", "Frappe sa nouvelle cible avec un éclair et la marque",
              AbilityTrigger.ON_TARGET_ACQUIRED, 140); // 7 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null || mob.isDead()) {
            return false;
        }

        Location strikeLocation = target.getLocation();
        World world = strikeLocation.getWorld();
        if (world == null) {
            return false;
        }

        world.strikeLightningEffect(strikeLocation);
        world.playSound(strikeLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.25f);
        world.spawnParticle(Particle.ELECTRIC_SPARK, strikeLocation.clone().add(0, 1.2, 0), 32, 0.6, 0.8, 0.6, 0.15);

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0, false, true, true));

        target.damage(2.0, mob);
        return true;
    }
}
