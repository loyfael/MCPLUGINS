package loyfael.listeners;

import loyfael.Main;
import loyfael.api.interfaces.IPlayerService;
import loyfael.api.interfaces.IMissionService;
import loyfael.api.interfaces.INotificationService;
import loyfael.core.services.MissionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener principal amélioré avec architecture SOLID
 * Principe de responsabilité unique : gestion des événements uniquement
 * Principe d'inversion de dépendance : utilise les services via interfaces
 */
public class ImprovedEventListener implements Listener {

    private final IPlayerService playerService;
    private final IMissionService missionService;
    private final INotificationService notificationService;

    // Cache local pour éviter les rafraîchissements trop fréquents
    private long lastCacheRefresh = 0;
    private static final long CACHE_REFRESH_COOLDOWN = 5000; // 5 secondes

    public ImprovedEventListener() {
        Main main = Main.getInstance();
        this.playerService = main.getPlayerService();
        this.missionService = main.getMissionService();
        this.notificationService = main.getNotificationService();
    }

    /**
     * Rafraîchit le cache intelligemment avec cooldown pour éviter le spam
     */
    private void smartCacheRefresh() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheRefresh > CACHE_REFRESH_COOLDOWN) {
            if (missionService instanceof MissionService) {
                ((MissionService) missionService).refreshMissionsCache();
                lastCacheRefresh = currentTime;
            }
        }
    }

    /**
     * Gère la connexion d'un joueur
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerUuid = player.getUniqueId().toString();

        // Traitement asynchrone pour éviter de bloquer le thread principal
        Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                // Vérifier si le joueur existe, sinon le créer
                if (!playerService.playerExists(playerUuid)) {
                    playerService.createPlayer(playerUuid, player.getName());
                } else {
                    // Mettre à jour la dernière connexion
                    playerService.updateLastSeen(playerUuid);
                }

                // Rafraîchir le cache des missions actives (nouveau joueur connecté)
                if (missionService instanceof MissionService) {
                    ((MissionService) missionService).refreshMissionsCache();
                }

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la connexion du joueur " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Gère la déconnexion d'un joueur
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerUuid = player.getUniqueId().toString();

        // Traitement asynchrone
        Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                // Sauvegarder toutes les données modifiées du joueur avant déconnexion
                if (missionService instanceof MissionService) {
                    ((MissionService) missionService).cleanupPlayerData(playerUuid);
                }

                // Mettre à jour la dernière déconnexion
                playerService.updateLastSeen(playerUuid);

                // Rafraîchir le cache des missions actives (joueur déconnecté)
                if (missionService instanceof MissionService) {
                    ((MissionService) missionService).refreshMissionsCache();
                }

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la déconnexion du joueur " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Traite tous les événements pour les missions automatiquement - Optimisé pour les performances
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (missionService instanceof MissionService) {
            MissionService ms = (MissionService) missionService;

            if (!ms.hasActiveBlockBreakMissions()) {
                smartCacheRefresh(); // Utilise le rafraîchissement intelligent
                if (!ms.hasActiveBlockBreakMissions()) {
                    return;
                }
            }
        }
        missionService.processEvent(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (missionService instanceof MissionService) {
            MissionService ms = (MissionService) missionService;

            if (!ms.hasActiveBlockPlaceMissions()) {
                smartCacheRefresh(); // Utilise le rafraîchissement intelligent
                if (!ms.hasActiveBlockPlaceMissions()) {
                    return;
                }
            }
        }
        missionService.processEvent(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (missionService instanceof MissionService) {
            MissionService ms = (MissionService) missionService;

            if (!ms.hasActiveKillMissions()) {
                smartCacheRefresh(); // Utilise le rafraîchissement intelligent
                if (!ms.hasActiveKillMissions()) {
                    return;
                }
            }
        }

        if (event.getEntity().getKiller() instanceof Player) {
            Player killer = (Player) event.getEntity().getKiller();
            missionService.processEvent(killer, event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (missionService instanceof MissionService) {
            MissionService ms = (MissionService) missionService;

            if (!ms.hasActiveFishMissions()) {
                smartCacheRefresh(); // Utilise le rafraîchissement intelligent
                if (!ms.hasActiveFishMissions()) {
                    return;
                }
            }
        }
        missionService.processEvent(event.getPlayer(), event);
    }
}
