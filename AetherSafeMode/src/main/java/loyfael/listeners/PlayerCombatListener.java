package loyfael.listeners;

import loyfael.Main;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Handles PvP combat based on player SafeMode status
 * Integrates with WorldGuard and Lands for zone protection
 */
public class PlayerCombatListener implements Listener {

    private final Main plugin;

    public PlayerCombatListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Only handle damage to players
        if (!(event.getEntity() instanceof Player victim)) return;
        
        Player attacker = null;
        
        // Direct player vs player damage
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }
        // Projectile damage (arrows, tridents, etc.)
        else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }
        
        // If no player attacker found, don't interfere
        if (attacker == null) return;

        // Check if combat should be blocked
        if (shouldBlockCombat(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    /**
     * Determine if combat should be blocked based on SafeMode status and zone protection
     */
    private boolean shouldBlockCombat(Player attacker, Player victim) {
        boolean attackerSafe = plugin.getSafeModeManager().isSafeMode(attacker);
        boolean victimSafe = plugin.getSafeModeManager().isSafeMode(victim);

        // Check attacker's SafeMode status
        if (attackerSafe) {
            attacker.sendMessage(plugin.getConfigManager().getMessage("cannot-attack-safe-mode"));
            return true;
        }

        // Check victim's SafeMode status
        if (victimSafe) {
            attacker.sendMessage(plugin.getConfigManager().getMessage("cannot-attack-safe-player"));
            // // Send informative message to victim too
            // victim.sendMessage(plugin.getConfigManager().getPrefix() +
            //     "§7Un joueur a tenté de vous attaquer, mais vous êtes protégé par le §a§lMODE SÉCURISÉ");
            return true;
        }

        // Check WorldGuard protection if available
        if (plugin.hasWorldGuard()) {
            if (!plugin.getWorldGuardHook().canPvP(attacker.getLocation())) {
                attacker.sendMessage(plugin.getConfigManager().getPrefix() +
                    "&7Le PvP est désactivé dans cette zone.");
                return true;
            }
        }

        // Check Lands protection if available
        if (plugin.hasLands()) {
            if (!plugin.getLandsHook().canPvP(attacker, victim)) {
                attacker.sendMessage(plugin.getConfigManager().getPrefix() +
                    "&7Le PvP n'est pas autorisé sur ce terrain.");
                return true;
            }
        }

        // Combat is allowed - both players are in unsafe mode and no zone protection
        return false;
    }
}
