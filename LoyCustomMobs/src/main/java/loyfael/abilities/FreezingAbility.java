package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that freezes targets by giving them slowness and placing ice around them
 */
public class FreezingAbility extends MobAbility {

    public FreezingAbility() {
        super("Freezing", "Freezes targets with ice and extreme slowness",
              AbilityTrigger.ON_ATTACK, 80); // 4 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        // Apply freezing effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4)); // 5 seconds, level 5
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 2)); // Mining fatigue
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, -5)); // Negative jump boost

        // Create ice around the player temporarily
        Location playerLoc = target.getLocation();

        // Place ice blocks around player (will be removed after effect)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Skip center block

                Location iceLoc = playerLoc.clone().add(x, 0, z);
                if (iceLoc.getBlock().getType() == Material.AIR ||
                    iceLoc.getBlock().getType() == Material.WATER) {

                    // Store original block type for restoration
                    Material originalType = iceLoc.getBlock().getType();
                    iceLoc.getBlock().setType(Material.ICE);

                    // Schedule ice removal
                    mob.getServer().getScheduler().runTaskLater(
                        mob.getServer().getPluginManager().getPlugin("LoyCustomMobs"),
                        () -> {
                            if (iceLoc.getBlock().getType() == Material.ICE) {
                                iceLoc.getBlock().setType(originalType);
                            }
                        },
                        100L // 5 seconds
                    );
                }
            }
        }

        return true;
    }
}
