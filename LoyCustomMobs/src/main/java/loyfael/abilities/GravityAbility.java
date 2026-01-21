package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that makes targets levitate when near solid ground
 */
public class GravityAbility extends MobAbility {

    public GravityAbility() {
        super("Gravity", "Makes targets levitate when attacking",
              AbilityTrigger.ON_ATTACK, 120); // 6 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        // Don't affect sneaking or creative players
        if (target.isSneaking() || target.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return false;
        }

        // Check if player is standing on solid ground
        Location feetBlock = target.getLocation();
        feetBlock.setY(feetBlock.getY() - 2.0);
        Block block = feetBlock.getWorld().getBlockAt(feetBlock);

        if (!block.getType().equals(Material.AIR)) {
            // Apply levitation effect
            target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 120, 2)); // 6 seconds, level 3
            return true;
        }

        return false;
    }
}
