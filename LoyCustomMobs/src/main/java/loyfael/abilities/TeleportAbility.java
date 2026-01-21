package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that makes the mob teleport behind players
 */
public class TeleportAbility extends MobAbility {

    public TeleportAbility() {
        super("Teleporter", "Teleports behind targets",
              AbilityTrigger.ON_TARGET_ACQUIRED, 100); // 5 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        // Calculate position behind the player
        Location targetLoc = target.getLocation();
        org.bukkit.util.Vector direction = targetLoc.getDirection().multiply(-2); // 2 blocks behind
        Location teleportLoc = targetLoc.add(direction);

        // Ensure the location is safe
        if (teleportLoc.getBlock().getType().isSolid()) {
            teleportLoc.add(0, 1, 0); // Move up if blocked
        }

        // Teleport the mob
        mob.teleport(teleportLoc);

        // Add brief invisibility effect
        mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0)); // 1 second

        return true;
    }
}
