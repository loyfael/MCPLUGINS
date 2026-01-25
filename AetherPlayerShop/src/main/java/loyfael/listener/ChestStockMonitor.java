package loyfael.listener;

import loyfael.Main;
import loyfael.model.Shop;
import loyfael.util.ShopInteractionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Monitore les changements de stock dans les coffres des shops
 * Met à jour automatiquement l'affichage des pancartes
 */
public class ChestStockMonitor implements Listener {

    private final Main plugin;

    public ChestStockMonitor(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Détecte la fermeture d'un coffre et vérifie s'il s'agit d'un shop
     * Met à jour l'affichage de la pancarte si nécessaire
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChestClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Vérifier si l'inventaire fermé est un coffre
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;

        Block chestBlock = chest.getBlock();

        plugin.getLogger().info("[DEBUG] Fermeture coffre détectée - Position: " +
            chestBlock.getWorld().getName() + " " +
            chestBlock.getX() + "," + chestBlock.getY() + "," + chestBlock.getZ());

        // Rechercher une pancarte de shop adjacente
        Block signBlock = ShopInteractionUtils.findAdjacentShopSign(chestBlock);
        if (signBlock == null) {
            plugin.getLogger().info("[DEBUG] Aucune pancarte de shop adjacente trouvée");
            return;
        }

        plugin.getLogger().info("[DEBUG] Pancarte de shop trouvée - Position: " +
            signBlock.getX() + "," + signBlock.getY() + "," + signBlock.getZ());

        // Récupérer le shop associé à cette pancarte de manière asynchrone
        plugin.getShopManager().getShopAtLocationWithAdjacent(signBlock.getLocation())
            .thenAccept(shop -> {
                if (shop == null) {
                    plugin.getLogger().warning("[DEBUG] Shop non trouvé pour la pancarte");
                    return;
                }

                // Revenir au thread principal pour manipuler les blocs et inventaires
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    updateShopSignAfterStockChange(shop, signBlock, chestBlock, player);
                });
            })
            .exceptionally(throwable -> {
                plugin.getLogger().warning("[ERROR] Erreur lors de la récupération du shop: " + throwable.getMessage());
                return null;
            });
    }

    /**
     * Met à jour l'affichage de la pancarte après un changement de stock
     */
    private void updateShopSignAfterStockChange(@NotNull Shop shop, @NotNull Block signBlock,
                                              @NotNull Block chestBlock, @NotNull Player player) {
        try {
            plugin.getLogger().info("[DEBUG] Mise à jour de l'affichage du shop " + shop.getId());

            // Obtenir l'inventaire du coffre
            if (!(chestBlock.getState() instanceof Chest chest)) {
                plugin.getLogger().warning("[WARNING] Le bloc n'est plus un coffre");
                return;
            }

            Inventory chestInventory = chest.getInventory();

            // Calculer le nouveau stock
            int previousStock = shop.getStock();
            int currentStock = ShopInteractionUtils.countItemsInInventory(chestInventory, shop.getItem().toItemStack());

            plugin.getLogger().info("[DEBUG] Stock - Ancien: " + previousStock + ", Nouveau: " + currentStock);

            // FORCER la mise à jour de la pancarte IMMÉDIATEMENT
            plugin.getLogger().info("[DEBUG] Forçage de la mise à jour de la pancarte...");
            ShopInteractionUtils.updateShopSignDisplay(shop, signBlock, chestInventory);

            // Forcer la mise à jour physique du bloc
            if (signBlock.getState() instanceof org.bukkit.block.Sign sign) {
                sign.update(true, true); // Force update avec notification aux joueurs
                plugin.getLogger().info("[SUCCESS] Pancarte physiquement mise à jour");
            }

            plugin.getLogger().info("[SUCCESS] Affichage pancarte mis à jour - Stock: " + currentStock);

            // Messages informatifs pour le propriétaire
            if (shop.getOwnerUUID().equals(player.getUniqueId())) {
                if (currentStock == 0 && previousStock > 0) {
                    // Stock épuisé
                    player.sendMessage("§c⚠ Votre shop est maintenant en RUPTURE DE STOCK !");
                    player.sendMessage("§7Remettez des items dans le coffre pour réactiver les ventes");
                    player.sendMessage("§a✨ Pancarte mise à jour automatiquement");
                } else if (currentStock > 0 && previousStock == 0) {
                    // Stock restauré - TRÈS IMPORTANT
                    player.sendMessage("§a✅ Votre shop a été RÉAPPROVISIONNÉ !");
                    player.sendMessage("§7Stock disponible: §e" + currentStock + " items");
                    player.sendMessage("§7Les clients peuvent à nouveau acheter !");
                    player.sendMessage("§a✨ Pancarte mise à jour automatiquement");

                    // Double vérification - forcer une autre mise à jour
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        ShopInteractionUtils.updateShopSignDisplay(shop, signBlock, chestInventory);
                        if (signBlock.getState() instanceof org.bukkit.block.Sign sign2) {
                            sign2.update(true, true);
                        }
                        plugin.getLogger().info("[SUCCESS] Double vérification - Pancarte re-mise à jour");
                    }, 2L);

                } else if (currentStock != previousStock) {
                    // Changement de stock normal
                    if (currentStock > previousStock) {
                        player.sendMessage("§a📦 Stock augmenté ! Nouveau stock: §e" + currentStock);
                        player.sendMessage("§a✨ Pancarte mise à jour automatiquement");
                    } else {
                        player.sendMessage("§e📦 Stock réduit. Stock restant: §e" + currentStock);
                        if (currentStock < 10 && currentStock > 0) {
                            player.sendMessage("§6⚠ Attention: Stock bientôt épuisé !");
                        }
                        player.sendMessage("§a✨ Pancarte mise à jour automatiquement");
                    }
                }
            }

            // Mettre à jour le stock en base de données si nécessaire
            if (currentStock != previousStock) {
                updateShopStockInDatabase(shop, currentStock);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[ERROR] Erreur lors de la mise à jour du stock: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Met à jour le stock du shop en base de données
     */
    private void updateShopStockInDatabase(@NotNull Shop shop, int newStock) {
        plugin.getShopManager().updateShopStock(shop.getId(), newStock)
            .thenAccept(success -> {
                if (success) {
                    // Mettre à jour l'objet shop en mémoire
                    shop.setStock(newStock);
                    plugin.getLogger().info("[SUCCESS] Stock mis à jour en BD - Shop " + shop.getId() + " : " + newStock);
                } else {
                    plugin.getLogger().warning("[WARNING] Échec de la mise à jour du stock en BD pour " + shop.getId());
                }
            })
            .exceptionally(throwable -> {
                plugin.getLogger().severe("[ERROR] Erreur lors de la mise à jour du stock: " + throwable.getMessage());
                return null;
            });
    }
}
