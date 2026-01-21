package loyfael.abilities;

import loyfael.models.AbilityTrigger;
import loyfael.models.MobAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that shrouds nearby players in a blinding sandstorm when the mob acquires a target.
 */
public class SandstormAbility extends MobAbility {

    public SandstormAbility() {
        super("Tempête de sable", "Aveugle les joueurs à son approche", AbilityTrigger.ON_TARGET_ACQUIRED, 180);
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (mob.isDead()) {
            return false;
        }

        Location origin = mob.getLocation();
        World world = origin.getWorld();
        if (world == null) {
            return false;
        }

        BlockData sandData = Material.SAND.createBlockData();
        boolean affected = false;

        for (Player nearby : world.getPlayers()) {
            if (!nearby.getWorld().equals(world) || nearby.isDead()) {
                continue;
            }
            if (nearby.getLocation().distanceSquared(origin) > 100.0) { // 10 block radius
                continue;
            }

            nearby.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, true, true));
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, true, true));
            world.spawnParticle(Particle.FALLING_DUST, nearby.getLocation().add(0, 1, 0), 24, 0.6, 0.8, 0.6, sandData);
            affected = true;
        }

        if (affected) {
            world.playSound(origin, Sound.WEATHER_RAIN_ABOVE, 1.2f, 0.45f);
            world.spawnParticle(Particle.CLOUD, origin.clone().add(0, 1.2, 0), 60, 3.5, 1.0, 3.5, 0.02);
        }

        return affected;
    }
}
