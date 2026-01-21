package loyfael.listeners;

import loyfael.Main;
import loyfael.gui.services.ModernGuiService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener pour les interactions GUI modernes
 * Principe de responsabilité unique : gestion des événements GUI uniquement
 */
public class ModernGuiListener implements Listener {

    private final ModernGuiService guiService;

    public ModernGuiListener() {
        this.guiService = (ModernGuiService) Main.getInstance().getServiceContainer()
            .getService(loyfael.api.interfaces.IGuiService.class);
    }

    /**
     * Gère les clics dans les inventaires GUI
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Vérifier si le joueur a une GUI ouverte
        if (!guiService.isGuiOpen(player)) return;

        // CORRECTION: Ne bloquer QUE les clics dans l'inventaire supérieur (GUI)
        // Si le joueur clique dans son inventaire personnel (bottom), on laisse faire
        if (event.getClickedInventory() == player.getInventory()) {
            return; // Permettre les manipulations dans l'inventaire personnel
        }

        // Bloquer uniquement les clics dans l'inventaire de la GUI (top)
        event.setCancelled(true);

        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();

        // Déléguer le clic au service GUI de façon asynchrone pour éviter les conflits
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            try {
                guiService.handleInventoryClick(player, slot, clickedItem);
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Erreur lors du traitement du clic GUI pour " + player.getName() + ": " + e.getMessage());
                // En cas d'erreur, fermer proprement la GUI
                if (guiService instanceof ModernGuiService) {
                    ((ModernGuiService) guiService).cleanupPlayerGui(player);
                }
            }
        });
    }

    /**
     * Gère la fermeture des inventaires
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();

        // Nettoyer les données de GUI et restaurer l'état du joueur
        if (guiService.isGuiOpen(player)) {
            // Programmer le nettoyage de façon asynchrone pour éviter les conflits
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                // Nettoyer explicitement les données GUI
                if (guiService instanceof ModernGuiService) {
                    ((ModernGuiService) guiService).cleanupPlayerGui(player);
                }
                
                // Forcer une mise à jour de l'inventaire du joueur pour restaurer l'état
                player.updateInventory();
                
                // Optionnel: ajouter un petit délai pour s'assurer que tout est restauré
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    player.updateInventory();
                }, 1L);
            });
        }
    }

    /**
     * Nettoie les données GUI à la déconnexion du joueur
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Nettoyer les GUIs ouvertes lors de la déconnexion
        if (guiService instanceof ModernGuiService) {
            ((ModernGuiService) guiService).onPlayerDisconnect(player);
        }
    }
}
