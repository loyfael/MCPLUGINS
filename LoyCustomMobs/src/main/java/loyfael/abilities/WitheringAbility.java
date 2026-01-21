package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that applies wither effect to targets
 */
public class WitheringAbility extends MobAbility {

    public WitheringAbility() {
        super("Withering", "Applies wither effect when attacking",
              AbilityTrigger.ON_ATTACK, 40); // 2 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        // Apply wither effect
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 1)); // 6 seconds, level 2

        return true;
    }
}
