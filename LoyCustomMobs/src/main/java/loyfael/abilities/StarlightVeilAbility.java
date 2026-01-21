package loyfael.abilities;

import loyfael.models.AbilityTrigger;
import loyfael.models.MobAbility;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that wraps the mob in a radiant protective veil upon spawning.
 */
public class StarlightVeilAbility extends MobAbility {

    public StarlightVeilAbility() {
        super("Voile stellaire", "Arrive auréolé d'une protection lumineuse", AbilityTrigger.ON_SPAWN, 0);
    }

    @Override
    public void onSpawn(LivingEntity mob) {
        mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 60, 0, false, false, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 60, 1, false, false, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 40, 0, false, false, true));

        mob.getWorld().spawnParticle(Particle.END_ROD, mob.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.02);
        mob.getWorld().playSound(mob.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.4f);
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        return false;
    }
}
