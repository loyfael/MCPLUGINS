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

/**
 * Ability that lets a mob siphon health from its victim to heal itself.
 */
public class VampiricAbility extends MobAbility {

    public VampiricAbility() {
        super("Vampirisme", "Aspire la vitalité de sa cible pour se soigner",
              AbilityTrigger.ON_ATTACK, 80); // 4 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null || !target.isOnline() || mob.isDead()) {
            return false;
        }

        AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : mob.getHealth();
        double healAmount = Math.max(2.0, maxHealth * 0.08);

        mob.setHealth(Math.min(maxHealth, mob.getHealth() + healAmount));
        mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, 1.0, 0), 8, 0.4, 0.6, 0.4, 0.01);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_VEX_CHARGE, 1.0f, 0.7f);

        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 1, false, true, true));

        return true;
    }
}
