package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that applies poison effect to attackers
 */
public class PoisonousAbility extends MobAbility {

    public PoisonousAbility() {
        super("Poisonous", "Applies poison to attackers",
              AbilityTrigger.ON_ATTACK, 20); // 1 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        // Apply poison effect
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1)); // 5 seconds, level 2

        return true;
    }
}
