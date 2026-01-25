package loyfael.manager

import loyfael.Main
import loyfael.model.Shop
import loyfael.model.ShopItem
import loyfael.model.Transaction
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Gestionnaire des transactions d'achat et de vente
 */
class TransactionManager(private val plugin: Main) {

    /**
     * Traite une transaction d'achat (ouvre le menu d'achat)
     */
    fun handleBuyTransaction(player: Player, shop: Shop) {
        try {
            plugin.logger.info("[DEBUG] Traitement transaction d'achat - Joueur: ${player.name}, Shop: ${shop.id}")

            val item = shop.item.toItemStack()

            // Vérifier le stock physique dans le coffre
            val chestLocation = Location(
                Bukkit.getWorld(shop.world),
                shop.x.toDouble(), shop.y.toDouble(), shop.z.toDouble()
            )

            val chestBlock = chestLocation.block
            if (!chestBlock.type.name.contains("CHEST")) {
                plugin.logger.severe("[ERROR] Coffre introuvable à la position du shop ${shop.id}")
                player.sendMessage("§cErreur: coffre du shop introuvable.")
                return
            }

            val chest = chestBlock.state as Chest
            val chestInventory = chest.inventory

            // Synchroniser le stock réel avec la base de données
            val realStock = calculateRealStock(chestInventory, item)

            // Mettre à jour le stock en base si différent
            if (realStock != shop.stock) {
                plugin.logger.info("[DEBUG] Synchronisation du stock - DB: ${shop.stock}, Coffre: $realStock -> Mise à jour")
                shop.stock = realStock
                plugin.shopManager.updateShop(shop)
            }

            // Vérifier s'il y a l'item dans le coffre
            if (realStock <= 0 || !chestInventory.containsAtLeast(item, 1)) {
                plugin.logger.info("[DEBUG] Achat refusé - Item non disponible dans le coffre (Stock réel: $realStock)")
                player.sendMessage("§c✖ Le coffre est vide !")
                player.sendMessage("§7Le propriétaire doit remettre du stock pour que vous puissiez acheter.")

                // Mettre à jour le stock à 0
                shop.stock = 0
                plugin.shopManager.updateShop(shop)
                return
            }

            // Ouvrir le menu d'achat
            plugin.purchaseMenuGUI.openPurchaseMenu(player, shop, chestInventory)

        } catch (e: Exception) {
            plugin.logger.severe("[ERROR] Erreur critique lors de handleBuyTransaction: ${e.message}")
            e.printStackTrace()
            player.sendMessage("§cErreur critique lors de l'achat. Contactez un administrateur.")
        }
    }

    /**
     * Traite une transaction de vente
     */
    fun handleSellTransaction(player: Player, shop: Shop) {
        try {
            plugin.logger.info("[DEBUG] Tentative de vente - Joueur: ${player.name}, Shop: ${shop.id}")

            // Vérifications similaires mais pour la vente
            if (!player.hasPermission("aetherplayershop.sell")) {
                player.sendMessage("§cVous n'avez pas la permission de vendre dans les shops.")
                return
            }

            if (shop.ownerUUID == player.uniqueId) {
                player.sendMessage("§cVous ne pouvez pas vendre dans votre propre shop.")
                return
            }

            // Vérifier que le joueur a l'item requis
            val requiredItem = shop.item.toItemStack()
            if (!hasMatchingItem(player, requiredItem)) {
                player.sendMessage("§cVous n'avez pas l'item requis: §e${shop.item.displayName}")
                return
            }

            // Vérifier que le propriétaire du shop a assez d'argent
            val price = shop.price
            val ownerUUID = shop.ownerUUID
            val ownerBalance = plugin.economy.getBalance(Bukkit.getOfflinePlayer(ownerUUID))

            plugin.logger.info("[DEBUG] Vérification économique vente - Balance propriétaire: $ownerBalance, Prix à payer: $price")

            if (ownerBalance < price) {
                player.sendMessage("§cLe propriétaire du shop n'a pas assez d'argent pour acheter vos items.")
                return
            }

            // Effectuer la transaction de vente
            performSellTransaction(player, shop)

        } catch (e: Exception) {
            plugin.logger.severe("[ERROR] Erreur lors de handleSellTransaction: ${e.message}")
            e.printStackTrace()
            player.sendMessage("§cErreur lors de la vente: ${e.message}")
        }
    }

    /**
     * Exécute la transaction de vente
     */
    private fun performSellTransaction(player: Player, shop: Shop) {
        try {
            plugin.logger.info("[DEBUG] Exécution de la transaction de vente - Joueur: ${player.name}, Shop: ${shop.id}")

            val price = shop.price
            val item = shop.item.toItemStack()

            // Retirer l'item de l'inventaire du joueur
            player.inventory.removeItem(item)

            // Créditer l'argent au joueur
            val response: EconomyResponse = plugin.economy.depositPlayer(player, price)
            if (!response.transactionSuccess()) {
                player.sendMessage("§cErreur lors du crédit de l'argent: ${response.errorMessage}")
                plugin.logger.severe("[ERROR] Échec crédit argent - Joueur: ${player.name}, Montant: $price, Erreur: ${response.errorMessage}")
                return
            }

            // Mettre à jour le stock du shop
            shop.stock = shop.stock + 1
            plugin.shopManager.updateShop(shop)
                .thenAccept { success ->
                    if (!success) {
                        plugin.logger.severe("[ERROR] Échec mise à jour stock pour shop ${shop.id}")
                        // Rollback de la transaction
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            plugin.economy.withdrawPlayer(player, price)
                            player.inventory.addItem(item)
                            player.sendMessage("§cErreur lors de la mise à jour du shop. Transaction annulée.")
                        })
                        return@thenAccept
                    }

                    plugin.logger.info("[SUCCESS] Transaction de vente réussie - Joueur: ${player.name}, Shop: ${shop.id}, Montant: $price")
                }

            // Messages de confirmation
            player.sendMessage("§aVente effectuée ! Item: §e${shop.item.displayName}§a, Prix: §e${String.format("%.2f", price)}◎")

            // Enregistrer la transaction
            val transaction = Transaction(
                shop.id,
                player.uniqueId,
                shop.ownerUUID,
                Transaction.TransactionType.SELL,
                shop.item,
                price,
                1
            )
            plugin.shopManager.recordTransaction(transaction)

        } catch (e: Exception) {
            plugin.logger.severe("[ERROR] Erreur critique lors de performSellTransaction: ${e.message}")
            e.printStackTrace()
            player.sendMessage("§cErreur critique lors de la vente. Contactez un administrateur.")
        }
    }

    /**
     * Calcule le stock réel dans le coffre
     */
    fun calculateRealStock(chestInventory: Inventory, targetItem: ItemStack): Int {
        var realStock = 0
        for (invItem in chestInventory.contents) {
            if (invItem != null && invItem.isSimilar(targetItem)) {
                realStock += invItem.amount
            }
        }
        return realStock
    }

    /**
     * Vérifie si le joueur a un item correspondant
     */
    private fun hasMatchingItem(player: Player, item: ItemStack): Boolean {
        for (inventoryItem in player.inventory.contents) {
            if (inventoryItem != null && inventoryItem.isSimilar(item)) {
                return true
            }
        }
        return false
    }
}
