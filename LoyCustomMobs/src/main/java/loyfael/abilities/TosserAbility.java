package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Ability that tosses attackers away with velocity
 */
public class TosserAbility extends MobAbility {

    public TosserAbility() {
        super("Tosser", "Throws attackers away when damaged",
              AbilityTrigger.ON_DAMAGED, 60); // 3 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        // Don't affect sneaking or creative players
        if (target.isSneaking() || target.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return false;
        }

        // Calculate knockback direction (from mob to player)
        Vector direction = target.getLocation().toVector().subtract(mob.getLocation().toVector());
        direction.normalize();
        direction.multiply(2.0); // Knockback strength
        direction.setY(0.8); // Add upward component

        // Apply velocity
        target.setVelocity(direction);

        return true;
    }
}
