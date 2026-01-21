package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that drains health from targets and gives it to the mob
 */
public class SapperAbility extends MobAbility {

    public SapperAbility() {
        super("Sapper", "Drains health from targets",
              AbilityTrigger.ON_ATTACK, 80); // 4 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        // Drain 2-4 health from target
        double drainAmount = 2 + (Math.random() * 2); // 2-4 health
        double targetHealth = target.getHealth();

        if (targetHealth > drainAmount) {
            target.setHealth(targetHealth - drainAmount);

            // Heal the mob
            double mobHealth = mob.getHealth();
            double maxHealth = mob.getAttribute(Attribute.MAX_HEALTH).getValue();

            if (mobHealth < maxHealth) {
                mob.setHealth(Math.min(maxHealth, mobHealth + drainAmount));
            }

            // Apply hunger effect to target
            target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 1)); // 3 seconds

            return true;
        }

        return false;
    }
}
