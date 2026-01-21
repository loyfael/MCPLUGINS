package loyfael.models;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Represents a special ability that custom mobs can have
 */
public abstract class MobAbility {
    protected final String name;
    protected final String description;
    protected final AbilityTrigger trigger;
    protected final int cooldown; // in ticks
    protected long lastUsed;

    public MobAbility(String name, String description, AbilityTrigger trigger, int cooldown) {
        this.name = name;
        this.description = description;
        this.trigger = trigger;
        this.cooldown = cooldown;
        this.lastUsed = 0;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AbilityTrigger getTrigger() {
        return trigger;
    }

    public int getCooldown() {
        return cooldown;
    }

    /**
     * Check if the ability is ready to be used (not on cooldown)
     */
    public boolean isReady() {
        return System.currentTimeMillis() - lastUsed >= (cooldown * 50); // convert ticks to ms
    }

    /**
     * Execute the ability
     * @param mob The mob using the ability
     * @param target The target of the ability (can be null)
     * @return true if the ability was successfully executed
     */
    public boolean execute(LivingEntity mob, Player target) {
        if (!isReady()) {
            return false;
        }

        boolean success = performAbility(mob, target);
        if (success) {
            lastUsed = System.currentTimeMillis();
        }

        return success;
    }

    /**
     * Abstract method to be implemented by specific abilities
     */
    protected abstract boolean performAbility(LivingEntity mob, Player target);

    /**
     * Called when the mob spawns with this ability
     */
    public void onSpawn(LivingEntity mob) {
        // Default implementation - can be overridden
    }

    /**
     * Called when the mob dies
     */
    public void onDeath(LivingEntity mob, Player killer) {
        // Default implementation - can be overridden
    }

    @Override
    public String toString() {
        return name;
    }
}
