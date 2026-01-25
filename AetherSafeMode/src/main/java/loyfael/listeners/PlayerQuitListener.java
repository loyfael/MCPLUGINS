package loyfael.listeners;

import loyfael.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player quit events
 * Saves player SafeMode data to database
 */
public class PlayerQuitListener implements Listener {

    private final Main plugin;

    public PlayerQuitListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Save player's SafeMode status to database
        plugin.getSafeModeManager().savePlayerMode(player);

        // Remove player from memory cache
        plugin.getSafeModeManager().removePlayer(player.getUniqueId());
    }
}
