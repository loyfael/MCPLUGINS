package loyfael.abilities;

import loyfael.models.MobAbility;
import loyfael.models.AbilityTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

/**
 * Ability that spawns undead minions when damaged
 */
public class NecromancerAbility extends MobAbility {

    public NecromancerAbility() {
        super("Necromancer", "Spawns undead minions when low on health",
              AbilityTrigger.ON_LOW_HEALTH, 200); // 10 second cooldown
    }

    @Override
    protected boolean performAbility(LivingEntity mob, Player target) {
        if (target == null) return false;

        Location spawnLoc = mob.getLocation();

        // Spawn 2-3 undead minions around the mob
        int minionCount = 2 + (int)(Math.random() * 2); // 2-3 minions

        for (int i = 0; i < minionCount; i++) {
            // Random offset around the mob
            double offsetX = (Math.random() - 0.5) * 4; // -2 to +2 blocks
            double offsetZ = (Math.random() - 0.5) * 4;
            Location minionSpawn = spawnLoc.clone().add(offsetX, 0, offsetZ);

            // Ensure spawn location is safe
            if (minionSpawn.getBlock().getType().isSolid()) {
                minionSpawn.add(0, 1, 0);
            }

            // Randomly spawn skeleton or zombie
            LivingEntity minion;
            if (Math.random() < 0.5) {
                minion = (Skeleton) spawnLoc.getWorld().spawnEntity(minionSpawn, EntityType.SKELETON);
            } else {
                minion = (Zombie) spawnLoc.getWorld().spawnEntity(minionSpawn, EntityType.ZOMBIE);
            }

            // Make minion target the player
            if (minion instanceof Skeleton) {
                ((Skeleton) minion).setTarget(target);
            } else if (minion instanceof Zombie) {
                ((Zombie) minion).setTarget(target);
            }

            // Set custom name
            minion.setCustomName("§8Larbin de " + mob.getCustomName());
            minion.setCustomNameVisible(true);
        }

        return true;
    }
}
