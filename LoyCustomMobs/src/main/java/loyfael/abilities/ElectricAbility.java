package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LightningStrike;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

/**
 * Ability that strikes the target with lightning and applies electrical effects
 */
public class ElectricAbility extends MobAbility {

    public ElectricAbility() {
        super("Electric", "Strikes targets with lightning and electrical damage",
              AbilityTrigger.ON_ATTACK, 100); // 5 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        Location strikeLocation = target.getLocation();

        // Strike with lightning (visual only, no fire)
        LightningStrike lightning = strikeLocation.getWorld().strikeLightningEffect(strikeLocation);

        // Apply electrical effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 1)); // Disorientation
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2)); // Muscle spasms

        // Damage the target
        target.damage(4.0, mob);

        // Chain lightning to nearby players (dans un rayon plus petit)
        Collection<Player> nearbyPlayers = strikeLocation.getWorld().getNearbyEntities(strikeLocation, 5, 5, 5)
            .stream()
            .filter(entity -> entity instanceof Player && !entity.equals(target))
            .map(entity -> (Player) entity)
            .toList();

        for (Player nearby : nearbyPlayers) {
            // Chain to nearby player
            LightningStrike chainLightning = nearby.getLocation().getWorld()
                .strikeLightningEffect(nearby.getLocation());

            nearby.damage(2.0, mob);
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1));

            // Chain effect particles
            createElectricChain(strikeLocation, nearby.getLocation());
        }

        // Electric particles around mob
        mob.getLocation().getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
            mob.getLocation().add(0, 1, 0), 15, 0.8, 0.8, 0.8, 0.1);

        return true;
    }

    private void createElectricChain(Location from, Location to) {
        // Create electric particle chain between two points
        double distance = from.distance(to);
        int particleCount = (int) (distance * 3); // 3 particles per block

        for (int i = 0; i <= particleCount; i++) {
            double t = (double) i / particleCount;
            Location particleLoc = from.clone().add(
                to.toVector().subtract(from.toVector()).multiply(t)
            );

            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0.1, 0.1, 0.1, 0.1);
        }

        to.getWorld().playSound(to, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
    }
}
