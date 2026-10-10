package loyfael.listener;

import loyfael.Main;
import loyfael.model.Shop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des actions spéciales pour les propriétaires de shops
 */
public class ShopOwnerManager {

    private final Main plugin;
    private final Map<UUID, PendingDeletion> pendingDeletions;

    public ShopOwnerManager(Main plugin) {
        this.plugin = plugin;
        this.pendingDeletions = new ConcurrentHashMap<>();
    }

    /**
     * Gère la destruction d'un shop par son propriétaire (clic gauche)
     */
    public void handleOwnerDestroyShop(@NotNull Player player, @NotNull Shop shop, @NotNull Block signBlock) {
        UUID playerUUID = player.getUniqueId();

        // Vérifier s'il y a déjà une confirmation en attente
        PendingDeletion existing = pendingDeletions.get(playerUUID);
        if (existing != null && existing.shopId.equals(shop.getId()) && !existing.isExpired()) {
            // Confirmation reçue - procéder à la suppression
            plugin.getLogger().info("[DEBUG] Confirmation de suppression reçue pour le shop " + shop.getId());

            // Nettoyer la confirmation en attente
            pendingDeletions.remove(playerUUID);

            // Effectuer la suppression
            executeShopDeletion(player, shop, signBlock);
            return;
        }

        // Première demande - demander confirmation
        plugin.getLogger().info("[DEBUG] Propriétaire " + player.getName() + " demande la suppression du shop " + shop.getId());

        player.sendMessage("§6§l⚠ DESTRUCTION DU SHOP ⚠");
        player.sendMessage("§eÊtes-vous sûr de vouloir détruire ce shop ?");
        player.sendMessage("§7- Stock restant: §e" + shop.getStock());
        player.sendMessage("§7- Prix: §e" + String.format("%.2f", shop.getPrice()) + "◎");
        player.sendMessage("§c§lClic gauche À NOUVEAU dans les 10 secondes pour CONFIRMER");
        player.sendMessage("§a§lClic droit ou attendez 10s pour ANNULER");

        // Enregistrer la demande de confirmation
        PendingDeletion confirmation = new PendingDeletion(shop.getId(), shop.getOwnerName(), System.currentTimeMillis());
        pendingDeletions.put(playerUUID, confirmation);

        // Programmer l'expiration de la confirmation
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PendingDeletion pending = pendingDeletions.remove(playerUUID);
            if (pending != null && !pending.isExpired()) {
                player.sendMessage("§a✓ Suppression du shop annulée (délai expiré)");
            }
        }, 200L); // 10 secondes
    }

    /**
     * Annule la suppression en attente (clic droit du propriétaire)
     */
    public void cancelPendingDeletion(@NotNull Player player) {
        PendingDeletion pending = pendingDeletions.remove(player.getUniqueId());
        if (pending != null && !pending.isExpired()) {
            player.sendMessage("§a✓ Suppression du shop annulée");
            plugin.getLogger().info("[DEBUG] Suppression du shop " + pending.shopId + " annulée par " + player.getName());
        }
    }

    /**
     * Exécute réellement la suppression du shop
     */
    private void executeShopDeletion(@NotNull Player player, @NotNull Shop shop, @NotNull Block signBlock) {
        plugin.getShopManager().deleteShop(shop.getId())
            .thenAccept(success -> {
                // Utiliser le thread principal pour modifier les blocs
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        // Supprimer le panneau
                        signBlock.setType(org.bukkit.Material.AIR);

                        player.sendMessage("§c✗ Shop détruit avec succès !");
                        player.sendMessage("§7Vous pouvez récupérer les items restants dans le coffre");
                        plugin.getLogger().info("[SUCCESS] Shop " + shop.getId() +
                            " détruit par son propriétaire " + player.getName());
                    } else {
                        player.sendMessage("§cErreur lors de la suppression du shop en base de données.");
                        plugin.getLogger().severe("[ERROR] Impossible de supprimer le shop " + shop.getId() + " de la base de données");
                    }
                });
            });
    }

    /**
     * Gère la modification d'un shop par son propriétaire (clic droit)
     */
    public void handleOwnerModifyShop(@NotNull Player player, @NotNull Shop shop) {
        // Annuler toute suppression en attente
        cancelPendingDeletion(player);

        plugin.getLogger().info("[DEBUG] Propriétaire " + player.getName() + " modifie le shop " + shop.getId());

        // Ouvrir le nouveau menu graphique d'édition au lieu de la conversation
        plugin.getShopEditMenuGUI().openEditMenu(player, shop);
    }

    /**
     * Classe interne pour gérer les suppressions en attente
     */
    private static class PendingDeletion {
        public final String shopId;
        public final String ownerName;
        public final long timestamp;
        private static final long EXPIRATION_TIME = 10000; // 10 secondes

        public PendingDeletion(String shopId, String ownerName, long timestamp) {
            this.shopId = shopId;
            this.ownerName = ownerName;
            this.timestamp = timestamp;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRATION_TIME;
        }
    }

    /**
     * Met à jour l'affichage de la pancarte après un changement de prix
     */
    private void updateShopSignAfterPriceChange(@NotNull Shop shop) {
        // Trouver la pancarte du shop et mettre à jour son affichage
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Pour l'instant, on fait confiance au système de cache pour se mettre à jour
                // lors de la prochaine interaction. Une amélioration future pourrait
                // localiser et mettre à jour directement la pancarte.
                plugin.getLogger().info("[DEBUG] Prix mis à jour pour le shop " + shop.getId() +
                    ", la pancarte se mettra à jour lors de la prochaine interaction");
            } catch (Exception e) {
                plugin.getLogger().warning("[WARNING] Erreur mineure lors de la mise à jour de l'affichage: " + e.getMessage());
            }
        });
    }
}
