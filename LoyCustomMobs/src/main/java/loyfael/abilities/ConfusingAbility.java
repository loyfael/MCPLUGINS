package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that confuses attackers with nausea and slowness
 */
public class ConfusingAbility extends MobAbility {

    public ConfusingAbility() {
        super("Confusing", "Confuses attackers with disorienting effects",
              AbilityTrigger.ON_DAMAGED, 100); // 5 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        // Apply confusion effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 120, 1)); // 6 seconds nausea
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0)); // 4 seconds slowness
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 1)); // 5 seconds mining fatigue

        return true;
    }
}
