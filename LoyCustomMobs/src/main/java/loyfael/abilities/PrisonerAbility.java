package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;
import java.util.List;

/**
 * Ability that traps targets with temporary spider webs and slowing effects
 */
public class PrisonerAbility extends MobAbility {

    public PrisonerAbility() {
        super("Prisoner", "Traps targets with webs and slowing effects",
              AbilityTrigger.ON_LOW_HEALTH, 180); // 9 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        Location center = target.getLocation();

        // Create web trap around player
        List<Location> webLocations = createWebTrap(center);

        // Apply strong slowing effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4)); // Very slow movement
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 3)); // Slow mining
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 2)); // Weaker attacks
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, -10)); // Can't jump

        // Visual effects
        center.getWorld().spawnParticle(Particle.WITCH, center.add(0, 2, 0), 50, 2.0, 2.0, 2.0, 0.1);
        center.getWorld().spawnParticle(Particle.CRIT, center, 30, 1.5, 1.5, 1.5, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 0.5f);

        // Schedule web removal after 5 seconds
        var plugin = mob.getServer().getPluginManager().getPlugin("LoyCustomMobs");
        if (plugin != null) {
            mob.getServer().getScheduler().runTaskLater(
                plugin,
                () -> removeWebTrap(webLocations, center),
                100L // 5 seconds
            );
        }

        return true;
    }

    private List<Location> createWebTrap(Location center) {
        List<Location> webLocations = new ArrayList<>();

        // Create webs around the player (2x2x2 area)
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    // Place webs strategically to trap without completely blocking
                    if ((Math.abs(x) == 1 && Math.abs(z) == 1) || // corners
                        (y == 1 && (Math.abs(x) == 1 || Math.abs(z) == 1))) { // middle level edges

                        Location webLoc = center.clone().add(x, y, z);

                        // Only place if air or replaceable
                        if (webLoc.getBlock().getType() == Material.AIR ||
                            webLoc.getBlock().getType() == Material.SHORT_GRASS ||
                            webLoc.getBlock().getType() == Material.TALL_GRASS) {

                            webLoc.getBlock().setType(Material.COBWEB);
                            webLocations.add(webLoc.clone());
                        }
                    }
                }
            }
        }

        return webLocations;
    }

    private void removeWebTrap(List<Location> webLocations, Location center) {
        // Remove the webs
        for (Location webLoc : webLocations) {
            if (webLoc.getBlock().getType() == Material.COBWEB) {
                webLoc.getBlock().setType(Material.AIR);
            }
        }

        // Break effect with safe particles
        center.getWorld().spawnParticle(Particle.EXPLOSION, center.add(0, 1, 0), 10, 1.0, 1.0, 1.0, 0.1);
        center.getWorld().spawnParticle(Particle.CLOUD, center, 20, 1.5, 1.5, 1.5, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_SPIDER_DEATH, 1.0f, 0.8f);
    }
}
