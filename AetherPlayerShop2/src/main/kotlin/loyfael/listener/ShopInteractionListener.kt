package loyfael.listener

import loyfael.Main
import loyfael.model.Shop
import loyfael.util.ShopInteractionUtils
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

/**
 * Gestionnaire simplifié des interactions avec les shops physiques
 */
class ShopInteractionListener(private val plugin: Main) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return

        // Interaction avec une pancarte de shop
        if (block.state is Sign) {
            val sign = block.state as Sign
            val lines = ShopInteractionUtils.getSignLines(sign)
            if (ShopInteractionUtils.isShopSign(lines)) {
                val player = event.player

                // Éviter l'interaction du propriétaire en shift (réservé à la config)
                if (lines[3] == player.name && player.isSneaking) {
                    return // Géré par ShopCreationListener
                }

                event.isCancelled = true
                handleShopInteraction(player, block, event.action)
            }
        }
        // Interaction avec un coffre de shop
        else if (block.type.name.contains("CHEST")) {
            handleChestInteraction(event)
        }
    }

    private fun handleShopInteraction(player: Player, signBlock: Block, action: Action) {
        plugin.logger.info("[DEBUG] Interaction avec shop - Joueur: ${player.name}, " +
                "Position: ${signBlock.location.world.name} ${signBlock.x},${signBlock.y},${signBlock.z}, " +
                "Action: $action")

        plugin.shopManager.getShopAtLocationWithAdjacent(signBlock.location)
            .thenAccept { shop ->
                if (shop == null) {
                    plugin.logger.warning("[ERROR] Shop introuvable à la position: " +
                            "${signBlock.location.world.name} ${signBlock.x},${signBlock.y},${signBlock.z}")
                    player.sendMessage("§cShop introuvable ou inactif. Contactez un administrateur si le problème persiste.")
                    return@thenAccept
                }

                plugin.logger.info("[DEBUG] Shop trouvé - ID: ${shop.id}, Type: ${shop.type}, " +
                        "Propriétaire: ${shop.ownerName}, Stock: ${shop.stock}, Prix: ${shop.price}")

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    try {
                        val isBuyAction = action == Action.LEFT_CLICK_BLOCK
                        val isOwner = shop.ownerUUID == player.uniqueId

                        // Gestion spéciale pour les propriétaires
                        if (isOwner) {
                            if (isBuyAction) {
                                // Clic gauche du propriétaire = Détruire le shop
                                plugin.shopOwnerManager.handleOwnerDestroyShop(player, shop, signBlock)
                            } else {
                                // Clic droit du propriétaire = Modifier le shop
                                plugin.shopOwnerManager.handleOwnerModifyShop(player, shop)
                            }
                            return@Runnable
                        }

                        // Vérifications préliminaires pour les non-propriétaires
                        if (!performBasicChecks(player, shop, isBuyAction)) {
                            return@Runnable
                        }

                        // Déléguer aux gestionnaires spécialisés
                        if (shop.type == Shop.ShopType.SELL && isBuyAction) {
                            // Obtenir le coffre associé pour vérifier le stock réel
                            val chestBlock = ShopInteractionUtils.findAdjacentChest(signBlock)
                            if (chestBlock != null && chestBlock.state is Chest) {
                                val chest = chestBlock.state as Chest
                                val chestInventory = chest.inventory
                                val actualStock = ShopInteractionUtils.countItemsInInventory(chestInventory, shop.item.toItemStack())

                                // Vérifier s'il y a du stock
                                if (actualStock <= 0) {
                                    player.sendMessage("§c✗ Ce shop n'a plus de stock disponible !")
                                    player.sendMessage("§7Revenez plus tard quand le propriétaire aura réapprovisionné.")

                                    // Mettre à jour l'affichage de la pancarte pour montrer la rupture de stock
                                    ShopInteractionUtils.updateShopSignDisplay(shop, signBlock, chestInventory)
                                    return@Runnable
                                }

                                // Il y a du stock - ouvrir le menu d'achat
                                plugin.purchaseMenuGUI.openPurchaseMenu(player, shop, chestInventory)

                                // Mettre à jour l'affichage de la pancarte avec le stock correct
                                ShopInteractionUtils.updateShopSignDisplay(shop, signBlock, chestInventory)
                            } else {
                                // Message supprimé : la gestion se fait dans la GUI
                            }
                        } else {
                            // Seul SELL existe maintenant - toujours clic gauche attendu
                            player.sendMessage("§cUtilisez clic gauche pour acheter dans ce shop.")
                            plugin.logger.info("[DEBUG] Mauvaise action - Attendu: clic gauche, " +
                                    "Reçu: ${if (isBuyAction) "clic gauche" else "clic droit"}")
                        }
                    } catch (e: Exception) {
                        plugin.logger.severe("[ERROR] Erreur lors du traitement de l'interaction shop: ${e.message}")
                        e.printStackTrace()
                        player.sendMessage("§cUne erreur est survenue lors de l'interaction avec le shop. Erreur: ${e.message}")
                    }
                })
            }
            .exceptionally { throwable ->
                plugin.logger.severe("[ERROR] Erreur async lors de la récupération du shop: ${throwable.message}")
                throwable.printStackTrace()
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    player.sendMessage("§cErreur de base de données lors de l'accès au shop. Contactez un administrateur.")
                })
                null
            }
    }

    /**
     * Effectue les vérifications de base avant toute transaction
     */
    private fun performBasicChecks(player: Player, shop: Shop, isBuyAction: Boolean): Boolean {
        // Vérification des permissions
        val permission = if (isBuyAction) "aetherplayershop.buy" else "aetherplayershop.sell"
        if (!player.hasPermission(permission)) {
            val action = if (isBuyAction) "acheter" else "vendre"
            player.sendMessage("§cVous n'avez pas la permission de $action dans les shops.")
            return false
        }

        // Vérification du stock pour les achats
        if (isBuyAction && shop.stock <= 0) {
            plugin.logger.info("[DEBUG] Achat refusé - Stock épuisé")
            player.sendMessage("§cCe shop n'a plus de stock disponible.")
            return false
        }

        // Vérification de l'économie
        val economy = plugin.economy

        // Vérification de l'argent pour les achats
        if (isBuyAction) {
            val playerBalance = economy.getBalance(player)
            val price = shop.price

            if (playerBalance < price) {
                plugin.logger.info("[DEBUG] Achat refusé - Argent insuffisant")
                player.sendMessage("§cVous n'avez pas assez d'argent. Prix: §e${String.format("%.2f", price)}§c◎, " +
                        "Votre argent: §e${String.format("%.2f", playerBalance)}§c◎")
                return false
            }

            // Vérification de l'espace inventaire
            if (!ShopInteractionUtils.hasInventorySpace(player, shop.item.toItemStack())) {
                plugin.logger.info("[DEBUG] Achat refusé - Inventaire plein")
                player.sendMessage("§cVous n'avez pas assez de place dans votre inventaire.")
                return false
            }
        }

        return true
    }

    private fun handleChestInteraction(event: PlayerInteractEvent) {
        // Empêcher l'ouverture directe des coffres de shop par des non-propriétaires
        val chestBlock = event.clickedBlock ?: return
        val player = event.player

        // Recherche d'une pancarte de shop adjacente
        val shopSign = ShopInteractionUtils.findAdjacentShopSign(chestBlock)
        if (shopSign != null) {
            val sign = shopSign.state as Sign
            val lines = ShopInteractionUtils.getSignLines(sign)

            // Si ce n'est pas le propriétaire ou un admin
            if (lines[3] != player.name && !player.hasPermission("aetherplayershop.admin")) {
                event.isCancelled = true
                player.sendMessage("§cVous ne pouvez pas accéder directement au coffre du shop.")
            }
        }
    }
}
