package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Ability that makes arrows home in on targets
 * Based on the original ArrowHomingTask
 */
public class ArrowHomingAbility extends MobAbility {

    public ArrowHomingAbility() {
        super("Homing Arrows", "Arrows fired by this mob will home in on targets",
              AbilityTrigger.ON_ATTACK, 40); // 2 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        // Find any arrows nearby that were shot by this mob
        mob.getNearbyEntities(20, 20, 20).stream()
            .filter(entity -> entity instanceof Arrow)
            .map(entity -> (Arrow) entity)
            .filter(arrow -> arrow.getShooter() == mob)
            .forEach(arrow -> new ArrowHomingTask(arrow, target, mob.getServer().getPluginManager().getPlugin("LoyCustomMobs")));

        return true;
    }

    /**
     * Arrow homing task - makes arrows curve towards target
     */
    private static class ArrowHomingTask extends BukkitRunnable {
        private final Arrow arrow;
        private final LivingEntity target;

        public ArrowHomingTask(Arrow arrow, LivingEntity target, Plugin plugin) {
            this.arrow = arrow;
            this.target = target;
            this.runTaskTimer(plugin, 1L, 1L);
        }

        @Override
        public void run() {
            try {
                double speed = arrow.getVelocity().length();
                if (arrow.isOnGround() || arrow.isDead() || target.isDead()) {
                    cancel();
                    return;
                }

                Vector toTarget = target.getLocation().clone().add(new Vector(0.0, 0.5, 0.0))
                                       .subtract(arrow.getLocation()).toVector();

                Vector dirVelocity = arrow.getVelocity().clone().normalize();
                Vector dirToTarget = toTarget.clone().normalize();
                double angle = dirVelocity.angle(dirToTarget);

                double newSpeed = 0.9 * speed + 0.14;

                Vector newVelocity;
                if (angle < 0.12) {
                    newVelocity = dirVelocity.clone().multiply(newSpeed);
                } else {
                    Vector newDir = dirVelocity.clone().multiply((angle - 0.12) / angle)
                                              .add(dirToTarget.clone().multiply(0.12 / angle));
                    newDir.normalize();
                    newVelocity = newDir.clone().multiply(newSpeed);
                }

                arrow.setVelocity(newVelocity.add(new Vector(0.0, 0.03, 0.0)));
            } catch (Exception ignored) {
                cancel();
            }
        }
    }
}
