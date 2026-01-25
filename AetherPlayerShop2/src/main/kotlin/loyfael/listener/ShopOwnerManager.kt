package loyfael.listener

import loyfael.Main
import loyfael.model.Shop
import loyfael.util.ShopInteractionUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestionnaire des actions spéciales pour les propriétaires de shops
 */
class ShopOwnerManager(private val plugin: Main) : Listener {

    private val pendingDeletions: MutableMap<UUID, PendingDeletion> = ConcurrentHashMap()

    /**
     * Gère la destruction d'un shop par son propriétaire (clic gauche)
     */
    fun handleOwnerDestroyShop(player: Player, shop: Shop, signBlock: Block) {
        val playerUUID = player.uniqueId

        // Vérifier s'il y a déjà une confirmation en attente
        val existing = pendingDeletions[playerUUID]
        if (existing != null && existing.shopId == shop.id && !existing.isExpired()) {
            // Confirmation reçue - procéder à la suppression
            plugin.logger.info("[DEBUG] Confirmation de suppression reçue pour le shop ${shop.id}")

            // Nettoyer la confirmation en attente
            pendingDeletions.remove(playerUUID)

            // Effectuer la suppression
            executeShopDeletion(player, shop, signBlock)
            return
        }

        // Première demande - demander confirmation
        plugin.logger.info("[DEBUG] Propriétaire ${player.name} demande la suppression du shop ${shop.id}")

        player.sendMessage("§6§l⚠ DESTRUCTION DU SHOP ⚠")
        player.sendMessage("§eÊtes-vous sûr de vouloir détruire ce shop ?")
        player.sendMessage("§7- Stock restant: §e${shop.stock}")
        player.sendMessage("§7- Prix: §e${String.format("%.2f", shop.price)}◎")
        player.sendMessage("§c§lClic gauche À NOUVEAU dans les 10 secondes pour CONFIRMER")
        player.sendMessage("§a§lClic droit ou attendez 10s pour ANNULER")

        // Enregistrer la demande de confirmation
        val confirmation = PendingDeletion(shop.id, shop.ownerName, System.currentTimeMillis())
        pendingDeletions[playerUUID] = confirmation

        // Programmer l'expiration de la confirmation
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val pending = pendingDeletions.remove(playerUUID)
            if (pending != null && !pending.isExpired()) {
                player.sendMessage("§a✓ Suppression du shop annulée (délai expiré)")
            }
        }, 200L) // 10 secondes
    }

    /**
     * Annule la suppression en attente (clic droit du propriétaire)
     */
    fun cancelPendingDeletion(player: Player) {
        val pending = pendingDeletions.remove(player.uniqueId)
        if (pending != null && !pending.isExpired()) {
            player.sendMessage("§a✓ Suppression du shop annulée")
            plugin.logger.info("[DEBUG] Suppression du shop ${pending.shopId} annulée par ${player.name}")
        }
    }

    /**
     * Exécute réellement la suppression du shop
     */
    private fun executeShopDeletion(player: Player, shop: Shop, signBlock: Block) {
        plugin.shopManager.deleteShop(shop.id)
            .thenAccept { success ->
                // Utiliser le thread principal pour modifier les blocs
                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (success) {
                        // Supprimer le panneau
                        signBlock.type = Material.AIR

                        player.sendMessage("§c✗ Shop détruit avec succès !")
                        player.sendMessage("§7Vous pouvez récupérer les items restants dans le coffre")
                        plugin.logger.info("[SUCCESS] Shop ${shop.id} détruit par son propriétaire ${player.name}")
                    } else {
                        player.sendMessage("§cErreur lors de la suppression du shop en base de données.")
                        plugin.logger.severe("[ERROR] Impossible de supprimer le shop ${shop.id} de la base de données")
                    }
                })
            }
    }

    /**
     * Gère la modification d'un shop par son propriétaire (clic droit)
     */
    fun handleOwnerModifyShop(player: Player, shop: Shop) {
        // Annuler toute suppression en attente
        cancelPendingDeletion(player)

        plugin.logger.info("[DEBUG] Propriétaire ${player.name} modifie le shop ${shop.id}")

        // Ouvrir le menu graphique d'édition
        plugin.shopEditMenuGUI.openEditMenu(player, shop)
    }

    /**
     * Rafraîchit l'affichage de la pancarte d'un shop
     */
    fun refreshShopSignDisplay(shop: Shop, player: Player) {
        try {
            // Construire la location du shop
            val world = Bukkit.getWorld(shop.world)
            if (world == null) {
                plugin.logger.warning("[WARNING] Monde introuvable pour le shop ${shop.id}")
                return
            }

            val shopLocation = Location(world, shop.x.toDouble(), shop.y.toDouble(), shop.z.toDouble())

            // Trouver la pancarte à cette position ou adjacente
            var signBlock = shopLocation.block
            if (signBlock.state !is org.bukkit.block.Sign) {
                // Chercher dans les blocs adjacents
                signBlock = ShopInteractionUtils.findAdjacentShopSign(shopLocation.block) ?: return
            }

            if (signBlock.state is org.bukkit.block.Sign) {
                // Trouver le coffre associé pour obtenir le stock réel
                val chestBlock = ShopInteractionUtils.findAdjacentChest(signBlock)
                val chestInventory = (chestBlock?.state as? Chest)?.inventory

                // Mettre à jour l'affichage de la pancarte
                ShopInteractionUtils.updateShopSignDisplay(shop, signBlock, chestInventory)

                player.sendMessage("§a✨ Pancarte mise à jour automatiquement !")
                plugin.logger.info("[SUCCESS] Pancarte du shop ${shop.id} mise à jour après changement de prix")
            } else {
                plugin.logger.warning("[WARNING] Pancarte introuvable pour le shop ${shop.id}")
                player.sendMessage("§e⚠ Pancarte non trouvée pour la mise à jour automatique")
            }
        } catch (e: Exception) {
            plugin.logger.severe("[ERROR] Erreur lors de la mise à jour de la pancarte: ${e.message}")
            e.printStackTrace()
            player.sendMessage("§c✗ Erreur lors de la mise à jour de la pancarte")
        }
    }

    /**
     * Classe interne pour gérer les suppressions en attente
     */
    private data class PendingDeletion(
        val shopId: String,
        val ownerName: String,
        val timestamp: Long
    ) {
        companion object {
            private const val EXPIRATION_TIME = 10000L // 10 secondes
        }

        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > EXPIRATION_TIME
        }
    }
}
