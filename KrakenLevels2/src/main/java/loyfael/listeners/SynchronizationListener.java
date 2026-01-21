package loyfael.listeners;

import loyfael.Main;
import loyfael.api.interfaces.ISynchronizationService;
import loyfael.api.interfaces.IConfigurationService;
import loyfael.utils.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Bukkit;

/**
 * Listener pour la synchronisation automatique des joueurs
 * Principe de responsabilité unique : synchronisation lors des connexions/déconnexions
 */
public class SynchronizationListener implements Listener {

    private final ISynchronizationService syncService;
    private final IConfigurationService configService;

    public SynchronizationListener() {
        this.syncService = Main.getInstance().getSynchronizationService();
        this.configService = Main.getInstance().getConfigurationService();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Utils.sendConsoleLog("&9[SYNC] Joueur connecté: " + event.getPlayer().getName());
        
        if (!configService.getConfig().getBoolean("synchronization.enabled", false)) {
            Utils.sendConsoleLog("&c[SYNC] Synchronisation désactivée dans la config");
            return;
        }

        if (!configService.getConfig().getBoolean("synchronization.auto-sync.on-join", true)) {
            Utils.sendConsoleLog("&c[SYNC] Auto-sync à la connexion désactivé");
            return;
        }

        String playerUuid = event.getPlayer().getUniqueId().toString();
        Utils.sendConsoleLog("&9[SYNC] Démarrage de la synchronisation pour " + event.getPlayer().getName() + " (UUID: " + playerUuid + ")");
        
        // Synchroniser les données du joueur depuis les autres serveurs (petit délai pour laisser le temps à la connexion)
        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
            try {
                Utils.sendConsoleLog("&9[SYNC] Appel de forceSync pour " + event.getPlayer().getName());
                syncService.forceSync(playerUuid).thenAccept(success -> {
                    if (success) {
                        Utils.sendConsoleLog("&a[SYNC] ✅ Synchronisation réussie pour " + event.getPlayer().getName());
                        
                        // Notifier le joueur si activé
                        if (configService.getConfig().getBoolean("synchronization.notify-players", false)) {
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                                event.getPlayer().sendMessage("§aDonnées synchronisées avec le réseau !");
                            });
                        }
                    } else {
                        Utils.sendConsoleLog("&c[SYNC] ❌ Échec de la synchronisation pour " + event.getPlayer().getName());
                    }
                });
            } catch (Exception e) {
                Utils.sendConsoleLog("&c[SYNC] Erreur lors de la synchronisation de " + event.getPlayer().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, 20L); // 1 seconde de délai
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!configService.getConfig().getBoolean("synchronization.enabled", false)) {
            return;
        }

        if (!configService.getConfig().getBoolean("synchronization.auto-sync.on-quit", true)) {
            return;
        }

        String playerUuid = event.getPlayer().getUniqueId().toString();
        
        // Synchroniser les données avant que le joueur parte
        try {
            syncService.syncPlayerData(playerUuid).thenAccept(success -> {
                if (success) {
                    Utils.sendConsoleLog("&aDonnées sauvegardées et synchronisées pour " + event.getPlayer().getName());
                } else {
                    Utils.sendConsoleLog("&eÉchec de la synchronisation finale pour " + event.getPlayer().getName());
                }
            });
        } catch (Exception e) {
            Utils.sendConsoleLog("&cErreur lors de la synchronisation de déconnexion pour " + event.getPlayer().getName() + ": " + e.getMessage());
        }
    }
}
