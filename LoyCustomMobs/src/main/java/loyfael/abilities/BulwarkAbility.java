package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Ability that grants a temporary shield and repels attackers when the mob is hurt.
 */
public class BulwarkAbility extends MobAbility {

    public BulwarkAbility() {
        super("Rempart", "Erige un bouclier protecteur en ripostant",
              AbilityTrigger.ON_DAMAGED, 120); // 6 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (mob.isDead()) {
            return false;
        }

        AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : mob.getHealth();
        double extraAbsorption = Math.max(4.0, maxHealth * 0.05);
        mob.setAbsorptionAmount(Math.min(mob.getAbsorptionAmount() + extraAbsorption, 18.0));

    mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 1, false, false, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false, true));

        mob.getWorld().spawnParticle(Particle.SWEEP_ATTACK, mob.getLocation().add(0, 1.0, 0), 12, 0.4, 0.4, 0.4, 0.0);
        mob.getWorld().playSound(mob.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.9f);

        if (target != null && target.isOnline()) {
            Vector push = target.getLocation().toVector().subtract(mob.getLocation().toVector());
            if (push.lengthSquared() > 0.0001) {
                push.normalize().multiply(1.2);
                push.setY(0.5);
                target.setVelocity(push);
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 0, false, true, true));
        }

        return true;
    }
}
