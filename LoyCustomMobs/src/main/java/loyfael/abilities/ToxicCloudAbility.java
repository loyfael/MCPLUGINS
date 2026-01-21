package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ability that releases a lingering toxic cloud when the mob dies.
 */
public class ToxicCloudAbility extends MobAbility {

    public ToxicCloudAbility() {
        super("Nuage toxique", "Diffuse un nuage empoisonné à sa mort",
              AbilityTrigger.ON_DEATH, 20); // Cooldown irrelevant but kept for consistency
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (mob.isDead()) {
            return false;
        }

        Location location = mob.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

    AreaEffectCloud cloud = (AreaEffectCloud) world.spawnEntity(location, EntityType.AREA_EFFECT_CLOUD);
    cloud.setRadius(3.5f);
    cloud.setRadiusPerTick(-0.02f);
    cloud.setDuration(160);
    cloud.setWaitTime(10);
    cloud.setParticle(Particle.CLOUD);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 1), true);
    cloud.addCustomEffect(new PotionEffect(PotionEffectType.NAUSEA, 120, 0), true);

        world.playSound(location, Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 0.6f);
    world.spawnParticle(Particle.WITCH, location.clone().add(0, 0.5, 0), 24, 1.2, 0.6, 1.2, 0.05);

        return true;
    }
}
