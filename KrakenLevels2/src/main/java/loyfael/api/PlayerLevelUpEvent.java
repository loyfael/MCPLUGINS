package loyfael.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event triggered when a player levels up
 * Allows other plugins to react to level changes
 */
public final class PlayerLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int newLevel;
    private final int previousLevel;
    private boolean cancelled = false;

    /**
     * Constructor for the level up event
     * @param player The player who levels up
     * @param newLevel The new level reached
     */
    public PlayerLevelUpEvent(Player player, int newLevel) {
        this.player = player;
        this.newLevel = newLevel;
        this.previousLevel = newLevel - 1; // Previous level calculated
    }

    /**
     * Constructor with specific previous level
     * @param player The player who levels up
     * @param newLevel The new level reached
     * @param previousLevel The previous level
     */
    public PlayerLevelUpEvent(Player player, int newLevel, int previousLevel) {
        this.player = player;
        this.newLevel = newLevel;
        this.previousLevel = previousLevel;
    }

    /**
     * Gets the concerned player
     * @return The player who levels up
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the new level reached
     * @return The new level
     */
    public int getNewLevel() {
        return newLevel;
    }

    /**
     * Gets the previous level
     * @return The previous level
     */
    public int getPreviousLevel() {
        return previousLevel;
    }

    /**
     * Checks if the event is cancelled
     * @return true if the event is cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether the event is cancelled
     * @param cancelled true to cancel the event
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Gets the list of event handlers
     * @return The list of handlers
     */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the static list of handlers
     * @return The static list of handlers
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
