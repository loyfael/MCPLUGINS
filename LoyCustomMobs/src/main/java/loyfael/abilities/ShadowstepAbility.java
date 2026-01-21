package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Ability that allows the mob to blink behind its target and deliver a crippling strike.
 */
public class ShadowstepAbility extends MobAbility {

    public ShadowstepAbility() {
        super("Ombre furtive", "Se téléporte derrière sa cible pour attaquer", AbilityTrigger.ON_ATTACK, 80);
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null || mob.isDead()) {
            return false;
        }

        Location targetLocation = target.getLocation();
        World world = targetLocation.getWorld();
        if (world == null) {
            return false;
        }

        Location behind = findBehindLocation(targetLocation);
        if (behind == null) {
            behind = mob.getLocation();
        }

        mob.teleport(behind);
        world.playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, behind.clone().add(0, 1, 0), 16, 0.4, 0.6, 0.4, 0.02);

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, false, true, true));
        target.damage(3.0, mob);

        return true;
    }

    private Location findBehindLocation(Location targetLocation) {
        Vector backwards = targetLocation.getDirection().clone();
        if (backwards.lengthSquared() < 1.0E-4) {
            backwards = new Vector(0, 0, -1);
        }
        backwards.normalize().multiply(-1.5);

        Location candidate = targetLocation.clone().add(backwards);
        candidate.setY(Math.floor(candidate.getY()) + 0.1);
        if (isTeleportSafe(candidate)) {
            return candidate;
        }

        // Try the four cardinal directions as a fallback
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians(i * 90.0);
            Vector offset = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(1.5);
            Location alternate = targetLocation.clone().add(offset);
            alternate.setY(Math.floor(alternate.getY()) + 0.1);
            if (isTeleportSafe(alternate)) {
                return alternate;
            }
        }
        return null;
    }

    private boolean isTeleportSafe(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        return location.getBlock().isPassable() && location.clone().add(0, 1, 0).getBlock().isPassable();
    }
}
