package loyfael.listeners;

import loyfael.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handles player death events based on SafeMode status
 * Manages keepInventory functionality per player
 */
public class PlayerDeathListener implements Listener {

    private final Main plugin;

    public PlayerDeathListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        boolean isSafeMode = plugin.getSafeModeManager().isSafeMode(player);

        if (isSafeMode) {
            // Safe Mode: Keep inventory
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Send confirmation message
            player.sendMessage(plugin.getConfigManager().getMessage("death-safe-mode"));

            // Send detailed explanation
            player.sendMessage("");
            player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("§8│               §a§lOBJETS PRÉSERVÉS                         §8│");
            player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("§8│  §a✓ §7Votre inventaire a été sauvegardé");
            player.sendMessage("§8│  §a✓ §7Votre expérience a été préservée");
            player.sendMessage("§8│  §a✓ §7Grâce au §a§lMODE SÉCURISÉ");
            player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("");

        } else {
            // Unsafe Mode: Lose inventory (default Minecraft behavior)
            event.setKeepInventory(false);
            event.setKeepLevel(false);

            // Send warning message
            player.sendMessage(plugin.getConfigManager().getMessage("death-unsafe-mode"));

            // Send detailed explanation
            player.sendMessage("");
            player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("§8│               §c§lOBJETS PERDUS                            §8│");
            player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("§8│  §c⚠ §7Vos objets ont été perdus");
            player.sendMessage("§8│  §c⚠ §7Conséquence du §c§lMODE COMBAT");
            player.sendMessage("§8│  §7Récupérez-les rapidement ou");
            player.sendMessage("§8│  §7utilisez §f/safemode §7pour être protégé");
            player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("");
        }
    }
}
