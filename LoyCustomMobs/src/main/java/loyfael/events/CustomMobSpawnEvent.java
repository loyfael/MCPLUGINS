package loyfael.events;

import loyfael.models.CustomMob;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a custom mob spawns
 */
public class CustomMobSpawnEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Entity entity;
    private final CustomMob customMob;
    private boolean cancelled;

    public CustomMobSpawnEvent(Entity entity, CustomMob customMob) {
        this.entity = entity;
        this.customMob = customMob;
        this.cancelled = false;
    }

    /**
     * Get the entity that became a custom mob
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Get the custom mob data
     */
    public CustomMob getCustomMob() {
        return customMob;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
