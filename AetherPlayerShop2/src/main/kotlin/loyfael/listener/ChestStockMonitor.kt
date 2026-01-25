package loyfael.listener

import loyfael.Main
import loyfael.model.Shop
import loyfael.util.ShopInteractionUtils
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory

/**
 * Monitore les changements de stock dans les coffres des shops
 * Met à jour automatiquement l'affichage des pancartes
 */
class ChestStockMonitor(private val plugin: Main) : Listener {

    /**
     * Détecte la fermeture d'un coffre et vérifie s'il s'agit d'un shop
     * Met à jour l'affichage de la pancarte si nécessaire
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChestClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        // Vérifier si l'inventaire fermé est un coffre
        val chest = event.inventory.holder as? Chest ?: return
        val chestBlock = chest.block

        plugin.logger.info("[DEBUG] Fermeture coffre détectée - Position: " +
                "${chestBlock.world.name} ${chestBlock.x},${chestBlock.y},${chestBlock.z}")

        // Rechercher une pancarte de shop adjacente
        val signBlock = ShopInteractionUtils.findAdjacentShopSign(chestBlock)
        if (signBlock == null) {
            plugin.logger.info("[DEBUG] Aucune pancarte de shop adjacente trouvée")
            return
        }

        plugin.logger.info("[DEBUG] Pancarte de shop trouvée - Position: " +
                "${signBlock.x},${signBlock.y},${signBlock.z}")

        // Récupérer le shop associé à cette pancarte de manière asynchrone
        plugin.shopManager.getShopAtLocationWithAdjacent(signBlock.location)
            .thenAccept { shop ->
                if (shop == null) {
                    plugin.logger.warning("[DEBUG] Shop non trouvé pour la pancarte")
                    return@thenAccept
                }

                // Revenir au thread principal pour manipuler les blocs et inventaires
                plugin.server.scheduler.runTask(plugin, Runnable {
                    updateShopSignAfterStockChange(shop, signBlock, chestBlock, player)
                })
            }
            .exceptionally { throwable ->
                plugin.logger.warning("[ERROR] Erreur lors de la récupération du shop: ${throwable.message}")
                null
            }
    }

    /**
     * Met à jour l'affichage de la pancarte après un changement de stock
     */
    private fun updateShopSignAfterStockChange(shop: Shop, signBlock: Block, chestBlock: Block, player: Player) {
        try {
            plugin.logger.info("[DEBUG] Mise à jour de l'affichage du shop ${shop.id}")

            // Obtenir l'inventaire du coffre
            val chest = chestBlock.state as? Chest
            if (chest == null) {
                plugin.logger.warning("[WARNING] Le bloc n'est plus un coffre")
                return
            }

            val chestInventory = chest.inventory

            // Calculer le nouveau stock
            val previousStock = shop.stock
            val currentStock = ShopInteractionUtils.countItemsInInventory(chestInventory, shop.item.toItemStack())

            plugin.logger.info("[DEBUG] Stock - Ancien: $previousStock, Nouveau: $currentStock")

            // FORCER la mise à jour de la pancarte IMMÉDIATEMENT
            plugin.logger.info("[DEBUG] Forçage de la mise à jour de la pancarte...")
            ShopInteractionUtils.updateShopSignDisplay(shop, signBlock, chestInventory)

            // Forcer la mise à jour physique du bloc
            val sign = signBlock.state as? Sign
            if (sign != null) {
                sign.update(true, true) // Force update avec notification aux joueurs
                plugin.logger.info("[SUCCESS] Pancarte physiquement mise à jour")
            }

            plugin.logger.info("[SUCCESS] Affichage pancarte mis à jour - Stock: $currentStock")

            // Messages informatifs pour le propriétaire
            if (shop.ownerUUID == player.uniqueId) {
                when {
                    currentStock == 0 && previousStock > 0 -> {
                        // Stock épuisé
                        player.sendMessage("§c⚠ Votre shop est maintenant en RUPTURE DE STOCK !")
                        player.sendMessage("§7Remettez des items dans le coffre pour réactiver les ventes")
                        player.sendMessage("§a✨ Pancarte mise à jour automatiquement")
                    }
                    currentStock > 0 && previousStock == 0 -> {
                        // Stock restauré - TRÈS IMPORTANT
                        player.sendMessage("§a✅ Votre shop a été RÉAPPROVISIONNÉ !")
                        player.sendMessage("§7Stock disponible: §e$currentStock items")
                        player.sendMessage("§7Les clients peuvent à nouveau acheter !")
                        player.sendMessage("§a✨ Pancarte mise à jour automatiquement")

                        // Double vérification - forcer une autre mise à jour
                        plugin.server.scheduler.runTaskLater(plugin, Runnable {
                            ShopInteractionUtils.updateShopSignDisplay(shop, signBlock, chestInventory)
                            val sign2 = signBlock.state as? Sign
                            sign2?.update(true, true)
                            plugin.logger.info("[SUCCESS] Double vérification - Pancarte re-mise à jour")
                        }, 2L)
                    }
                    currentStock != previousStock -> {
                        // Changement de stock normal
                        if (currentStock > previousStock) {
                            player.sendMessage("§a📦 Stock augmenté ! Nouveau stock: §e$currentStock")
                            player.sendMessage("§a✨ Pancarte mise à jour automatiquement")
                        } else {
                            player.sendMessage("§e📦 Stock réduit. Stock restant: §e$currentStock")
                            if (currentStock in 1..9) {
                                player.sendMessage("§6⚠ Attention: Stock bientôt épuisé !")
                            }
                            player.sendMessage("§a✨ Pancarte mise à jour automatiquement")
                        }
                    }
                }
            }

            // Mettre à jour le stock en base de données si nécessaire
            if (currentStock != previousStock) {
                updateShopStockInDatabase(shop, currentStock)
            }

        } catch (e: Exception) {
            plugin.logger.severe("[ERROR] Erreur lors de la mise à jour du stock: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Met à jour le stock du shop en base de données
     */
    private fun updateShopStockInDatabase(shop: Shop, newStock: Int) {
        plugin.shopManager.updateShopStock(shop.id, newStock)
            .thenAccept { success ->
                if (success) {
                    // Mettre à jour l'objet shop en mémoire
                    shop.stock = newStock
                    plugin.logger.info("[SUCCESS] Stock mis à jour en BD - Shop ${shop.id} : $newStock")
                } else {
                    plugin.logger.warning("[WARNING] Échec de la mise à jour du stock en BD pour ${shop.id}")
                }
            }
            .exceptionally { throwable ->
                plugin.logger.severe("[ERROR] Erreur lors de la mise à jour du stock: ${throwable.message}")
                null
            }
    }
}
