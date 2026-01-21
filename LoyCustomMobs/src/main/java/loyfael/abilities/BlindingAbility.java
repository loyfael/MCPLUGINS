package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that blinds attackers
 */
public class BlindingAbility extends MobAbility {

    public BlindingAbility() {
        super("Blinding", "Blinds attackers when damaged",
              AbilityTrigger.ON_DAMAGED, 60); // 3 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        // Apply blindness effect
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0)); // 5 seconds

        return true;
    }
}
