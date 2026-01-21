package loyfael.abilities;

import loyfael.models.AbilityTrigger;
import loyfael.models.MobAbility;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that enrages the mob when it reaches low health, granting powerful buffs.
 */
public class FrenzyAbility extends MobAbility {

    public FrenzyAbility() {
        super("Frénésie", "Gagne une force démente lorsqu'il faiblit", AbilityTrigger.ON_LOW_HEALTH, 160);
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (mob.isDead()) {
            return false;
        }

        AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : mob.getHealth();
        double healAmount = Math.max(4.0, maxHealth * 0.12);
        mob.setHealth(Math.min(maxHealth, mob.getHealth() + healAmount));

    mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 1, false, false, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 1, false, false, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1, false, false, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, false, false, true));

    mob.getWorld().spawnParticle(Particle.CRIMSON_SPORE, mob.getLocation().add(0, 1, 0), 50, 0.6, 1.0, 0.6, 0.01);
        mob.getWorld().spawnParticle(Particle.CRIT, mob.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.02);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_WARDEN_ANGRY, 1.0f, 0.75f);

        return true;
    }
}
