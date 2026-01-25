package loyfael.gui

import loyfael.Main
import loyfael.model.Shop
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * Menu graphique d'édition des shops - Interface UX épurée et intuitive
 */
class ShopEditMenuGUI(private val plugin: Main) : Listener {

    companion object {
        private val editSessions = mutableMapOf<UUID, EditSession>()
    }

    fun openEditMenu(player: Player, shop: Shop) {
        plugin.logger.info("[DEBUG] Ouverture du menu d'édition - Joueur: ${player.name}, Shop: ${shop.id}")

        if (shop.ownerUUID != player.uniqueId) {
            player.sendMessage("§cVous n'êtes pas le propriétaire de ce shop !")
            return
        }

        val menu = Bukkit.createInventory(null, 45,
            Component.text("Édition - ${shop.item.displayName}")
                .color(NamedTextColor.DARK_GRAY))

        val session = EditSession(shop, shop.price)
        editSessions[player.uniqueId] = session

        setupEditMenu(menu, player, session)
        player.openInventory(menu)
    }

    private fun setupEditMenu(menu: Inventory, @Suppress("UNUSED_PARAMETER") player: Player, session: EditSession) {
        val shop = session.shop
    val currentPrice = session.tempPrice
    val originalPrice = shop.price
    val priceChanged = Math.abs(currentPrice - originalPrice) > 0.01
    val teleportPolicyChanged = session.tempTeleportPolicy != shop.teleportPolicy
    val hasChanges = priceChanged || teleportPolicyChanged

        menu.clear()

        // === ENTOURAGE DÉCORATIF ===
        for (i in 0..8) {
            menu.setItem(i, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
        }
        for (i in 36..44) {
            menu.setItem(i, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
        }
        listOf(9, 17, 18, 26, 27, 35).forEach {
            menu.setItem(it, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE))
        }

        // === ITEM DU SHOP (slot 13) ===
        val shopItem = shop.item.toItemStack()
        shopItem.editMeta {
            it.displayName(Component.text("🏪 ${shop.item.displayName}", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
            val shopLore = mutableListOf<Component>()
            shopLore.add(Component.empty())
            shopLore.add(Component.text("═══ MODIFICATION DU PRIX ═══", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
            shopLore.add(Component.empty())
            shopLore.add(Component.text("📊 Stock: ${shop.stock}", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            shopLore.add(Component.text("💰 Prix actuel: ${String.format("%.2f", originalPrice)}$", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            if (priceChanged) {
                shopLore.add(Component.text("💲 Nouveau prix: ${String.format("%.2f", currentPrice)}$",
                    if (currentPrice > originalPrice) NamedTextColor.GREEN else NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
            }
            val teleportLine = Component.text("🧭 Téléportation: ", NamedTextColor.AQUA)
                .append(Component.text(getTeleportPolicyDisplay(session.tempTeleportPolicy),
                    if (teleportPolicyChanged) NamedTextColor.GOLD else NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false)
            shopLore.add(Component.empty())
            shopLore.add(teleportLine)
            if (teleportPolicyChanged) {
                shopLore.add(Component.text("Ancien: ${getTeleportPolicyDisplay(shop.teleportPolicy)}", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            }
            shopLore.add(Component.empty())
            shopLore.add(Component.text("👑 ${shop.ownerName}", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
            it.lore(shopLore)
        }
        menu.setItem(13, shopItem)

        // === CONTRÔLES DE PRIX (ligne 3: 19-25) ===
        createPriceButton(menu, 19, Material.IRON_BLOCK, "§c§l🔻 -10◎", -10.0, listOf("§7Diminuer le prix de 10◎", "§8Grosse réduction"))
        createPriceButton(menu, 20, Material.IRON_INGOT, "§c§l🔻 -1◎", -1.0, listOf("§7Diminuer le prix de 1◎", "§8Réduction moyenne"))
        createPriceButton(menu, 21, Material.IRON_NUGGET, "§c§l🔻 -0.1◎", -0.1, listOf("§7Diminuer le prix de 0.1◎", "§8Ajustement précis"))

        // RESET (slot 22)
        val resetBtn = ItemStack(Material.RECOVERY_COMPASS)
        resetBtn.editMeta {
            it.displayName(Component.text("🔄 RESET", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            it.lore(listOf(
                Component.empty(),
                Component.text("Remet le prix original", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                Component.text(String.format("%.2f◎", originalPrice), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ))
        }
        menu.setItem(22, resetBtn)

        createPriceButton(menu, 23, Material.GOLD_NUGGET, "§a§l🔺 +0.1◎", 0.1, listOf("§7Augmenter le prix de 0.1◎", "§8Ajustement précis"))
        createPriceButton(menu, 24, Material.GOLD_INGOT, "§a§l🔺 +1◎", 1.0, listOf("§7Augmenter le prix de 1◎", "§8Augmentation moyenne"))
        createPriceButton(menu, 25, Material.GOLD_BLOCK, "§a§l🔺 +10◎", 10.0, listOf("§7Augmenter le prix de 10◎", "§8Grosse augmentation"))

        // === CONTRÔLES AVANCÉS (ligne 4: 29, 30, 32) ===
        createPriceButton(menu, 29, Material.FIRE_CHARGE, "§c§l🔻 -100◎", -100.0, listOf("§7Diminuer le prix de 100◎"))
        createPriceButton(menu, 30, Material.SHEARS, "§e÷ 2", 0.0, listOf("§7Divise le prix par 2"))
        createPriceButton(menu, 32, Material.NETHER_STAR, "§a§l🔺 +100◎", 100.0, listOf("§7Augmenter le prix de 100◎"))

        // === TÉLÉPORTATION (slot 31) ===
        val teleportButtonMaterial = if (session.tempTeleportPolicy == Shop.TeleportPolicy.ALLOW_TP)
            Material.ENDER_PEARL else Material.REDSTONE_TORCH
        val teleportBtn = ItemStack(teleportButtonMaterial)
        teleportBtn.editMeta {
            val allowingTeleport = session.tempTeleportPolicy == Shop.TeleportPolicy.ALLOW_TP
            val title = if (allowingTeleport) "🚀 Téléportation autorisée" else "⛔ Téléportation bloquée"
            val titleColor = if (allowingTeleport) NamedTextColor.GREEN else NamedTextColor.RED
            it.displayName(Component.text(title, titleColor).decoration(TextDecoration.ITALIC, false))
            val lore = mutableListOf<Component>()
            lore.add(Component.empty())
            lore.add(Component.text("Cliquez pour basculer la politique", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text("Actuel: ${getTeleportPolicyDisplay(session.tempTeleportPolicy)}", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text("Ancien: ${getTeleportPolicyDisplay(shop.teleportPolicy)}", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            if (teleportPolicyChanged) {
                lore.add(Component.text("✓ Modification en attente", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
            }
            it.lore(lore)
        }
        menu.setItem(31, teleportBtn)

        // === ACTIONS FINALES (ligne 5: 39-41) ===
        val cancelBtn = ItemStack(Material.BARRIER)
        cancelBtn.editMeta {
            it.displayName(Component.text("❌ Annuler", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
            it.lore(listOf(
                Component.empty(),
                Component.text("Fermer sans sauvegarder", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            ))
        }
        menu.setItem(39, cancelBtn)

        val saveMaterial = if (hasChanges && currentPrice > 0) Material.EMERALD_BLOCK else Material.GRAY_CONCRETE
        val saveBtn = ItemStack(saveMaterial)
        saveBtn.editMeta {
            if (hasChanges && currentPrice > 0) {
                it.displayName(Component.text("💾 SAUVEGARDER", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                val lore = mutableListOf<Component>()
                lore.add(Component.empty())
                if (priceChanged) {
                    lore.add(Component.text("Nouveau prix: ${String.format("%.2f", currentPrice)}$", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                }
                if (teleportPolicyChanged) {
                    lore.add(Component.text("Téléportation: ${getTeleportPolicyDisplay(shop.teleportPolicy)} → ${getTeleportPolicyDisplay(session.tempTeleportPolicy)}", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                }
                lore.add(Component.empty())
                lore.add(Component.text("👆 CLIQUER POUR CONFIRMER", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                it.lore(lore)
            } else {
                it.displayName(Component.text("⚪ Aucune modification", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                it.lore(listOf(
                    Component.empty(),
                    Component.text("Modifiez le prix ou la téléportation", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                ))
            }
        }
        menu.setItem(40, saveBtn)

        val deleteBtn = ItemStack(if (session.pendingDeletion) Material.TNT else Material.REDSTONE)
        deleteBtn.editMeta {
            if (session.pendingDeletion) {
                it.displayName(Component.text("💥 CONFIRMER SUPPRESSION", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false))
                it.lore(listOf(
                    Component.empty(),
                    Component.text("⚠ CLIQUEZ À NOUVEAU POUR CONFIRMER", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                    Component.text("Le shop sera définitivement supprimé", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                ))
            } else {
                it.displayName(Component.text("🗑 Supprimer", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false))
                it.lore(listOf(
                    Component.empty(),
                    Component.text("Supprimer définitivement ce shop", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                    Component.text("Stock: ${shop.stock}", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                ))
            }
        }
        menu.setItem(41, deleteBtn)
    }

    private fun createPriceButton(menu: Inventory, slot: Int, material: Material, name: String, priceChange: Double, extraLore: List<String>) {
        val button = ItemStack(material)
        button.editMeta {
            it.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
            val lore = extraLore.map { line -> Component.text(line).decoration(TextDecoration.ITALIC, false) }.toMutableList()
            if (priceChange != 0.0) {
                if (priceChange > 0) {
                    lore.add(Component.text("+${String.format("%.2f", priceChange)} $", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                } else {
                    lore.add(Component.text("${String.format("%.2f", priceChange)} $", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                }
            }
            lore.add(Component.empty())
            lore.add(Component.text("👆 CLIQUEZ !", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            it.lore(lore)
        }
        menu.setItem(slot, button)
    }

    private fun createDecorativeGlass(material: Material): ItemStack {
        val glass = ItemStack(material)
        glass.editMeta { it.displayName(Component.text("")) }
        return glass
    }

    private fun getTeleportPolicyDisplay(policy: Shop.TeleportPolicy): String = when (policy) {
        Shop.TeleportPolicy.ALLOW_TP -> "Autoriser"
        Shop.TeleportPolicy.BLOCK_TP_AND_SUGGEST_DM -> "Bloquer & suggérer MP"
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = event.view.title().toString()
        if (!title.contains("Édition")) return

        event.isCancelled = true

        val session = editSessions[player.uniqueId]
        if (session == null || session.isExpired()) {
            player.sendMessage("§c⏰ Session d'édition expirée. Rouvrez le menu d'édition !")
            player.closeInventory()
            return
        }

        val clickedItem = event.currentItem ?: return
        val slot = event.slot

        when (slot) {
            19 -> if (clickedItem.type == Material.IRON_BLOCK) {
                session.adjustPrice(-10.0)
                setupEditMenu(event.inventory, player, session)
                player.sendMessage("§e📉 -10◎ (Prix: ${String.format("%.2f", session.tempPrice)}◎)")
            }
            20 -> if (clickedItem.type == Material.IRON_INGOT) {
                session.adjustPrice(-1.0)
                setupEditMenu(event.inventory, player, session)
                player.sendMessage("§e📉 -1◎ (Prix: ${String.format("%.2f", session.tempPrice)}◎)")
            }
            21 -> if (clickedItem.type == Material.IRON_NUGGET) {
                session.adjustPrice(-0.1)
                setupEditMenu(event.inventory, player, session)
                player.sendMessage("§e📉 -0.1◎ (Prix: ${String.format("%.2f", session.tempPrice)}◎)")
            }
            22 -> if (clickedItem.type == Material.RECOVERY_COMPASS) {
                session.tempPrice = session.shop.price
                setupEditMenu(event.inventory, player, session)
                player.sendMessage("§e🔄 Prix restauré: ${String.format("%.2f", session.tempPrice)}◎")
            }
            23 -> if (clickedItem.type == Material.GOLD_NUGGET) {
                session.adjustPrice(0.1)
                setupEditMenu(event.inventory, player, session)
                player.sendMessage("§e📈 +0.1◎ (Prix: ${String.format("%.2f", session.tempPrice)}◎)")
            }
            24 -> if (clickedItem.type == Material.GOLD_INGOT) {
                session.adjustPrice(1.0)
                setupEditMenu(event.inventory, player, session)
                player.sendMessage("§e📈 +1◎ (Prix: ${String.format("%.2f", session.tempPrice)}◎)")
            }
            25 -> if (clickedItem.type == Material.GOLD_BLOCK) {
                session.adjustPrice(10.0)
                setupEditMenu(event.inventory, player, session)
                player.sendMessage("§e📈 +10◎ (Prix: ${String.format("%.2f", session.tempPrice)}◎)")
            }
            29 -> if (clickedItem.type == Material.FIRE_CHARGE) {
                session.adjustPrice(-100.0)
                setupEditMenu(event.inventory, player, session)
                player.sendMessage("§e📉 -100◎ (Prix: ${String.format("%.2f", session.tempPrice)}◎)")
            }
            30 -> if (clickedItem.type == Material.SHEARS) {
                session.tempPrice = maxOf(0.01, session.tempPrice / 2)
                setupEditMenu(event.inventory, player, session)
                player.sendMessage("§e💰 Prix divisé par 2 (Prix: ${String.format("%.2f", session.tempPrice)}◎)")
            }
            31 -> if (clickedItem.type == Material.ENDER_PEARL || clickedItem.type == Material.REDSTONE_TORCH) {
                session.toggleTeleportPolicy()
                setupEditMenu(event.inventory, player, session)
                val newPolicy = session.tempTeleportPolicy
                val policyMessage = if (newPolicy == Shop.TeleportPolicy.ALLOW_TP) {
                    "§a🚀 Téléportation autorisée"
                } else {
                    "§c⛔ Téléportation bloquée (invite en MP)"
                }
                player.sendMessage(policyMessage)
            }
            32 -> if (clickedItem.type == Material.NETHER_STAR) {
                session.adjustPrice(100.0)
                setupEditMenu(event.inventory, player, session)
                player.sendMessage("§e📈 +100◎ (Prix: ${String.format("%.2f", session.tempPrice)}◎)")
            }
            39 -> if (clickedItem.type == Material.BARRIER) {
                player.closeInventory()
                editSessions.remove(player.uniqueId)
                player.sendMessage("§c❌ Édition annulée")
            }
            40 -> if (clickedItem.type == Material.EMERALD_BLOCK) {
                player.sendMessage("§a💾 Sauvegarde en cours...")
                saveShopChanges(player, session)
            }
            41 -> {
                if (session.pendingDeletion && clickedItem.type == Material.TNT) {
                    executeShopDeletion(player, session)
                } else if (!session.pendingDeletion && clickedItem.type == Material.REDSTONE) {
                    confirmShopDeletion(player, session)
                }
            }
        }
    }

    private fun saveShopChanges(player: Player, session: EditSession) {
        val shop = session.shop
        val oldPrice = shop.price
        val newPrice = session.tempPrice
        val oldPolicy = shop.teleportPolicy
        val newPolicy = session.tempTeleportPolicy
        val priceChanged = Math.abs(oldPrice - newPrice) > 0.01
        val teleportPolicyChanged = oldPolicy != newPolicy

        if (newPrice <= 0) {
            player.sendMessage("§cErreur: Le prix doit être supérieur à 0 !")
            return
        }

        if (!priceChanged && !teleportPolicyChanged) {
            player.sendMessage("§eAucune modification à appliquer !")
            return
        }

        val updatedShop = shop.copy(price = newPrice, teleportPolicy = newPolicy)

        plugin.shopManager.updateShop(updatedShop)
            .thenAccept { success ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (success) {
                        if (priceChanged) {
                            shop.price = newPrice
                            updateShopSignDisplay(shop, player)
                        }
                        if (teleportPolicyChanged) {
                            shop.teleportPolicy = newPolicy
                        }

                        player.sendMessage("§a§l✓ Shop modifié avec succès !")
                        if (priceChanged) {
                            player.sendMessage("§7Ancien prix: §c${String.format("%.2f", oldPrice)}$")
                            player.sendMessage("§7Nouveau prix: §a${String.format("%.2f", newPrice)}$")
                            player.sendMessage("§a✨ Pancarte mise à jour automatiquement !")
                        }
                        if (teleportPolicyChanged) {
                            player.sendMessage("§7Téléportation: §e${getTeleportPolicyDisplay(oldPolicy)} §7→ §a${getTeleportPolicyDisplay(newPolicy)}")
                        }

                        player.closeInventory()
                        editSessions.remove(player.uniqueId)

                        val changesSummary = buildString {
                            if (priceChanged) {
                                append("Prix: ${String.format("%.2f", oldPrice)}$ → ${String.format("%.2f", newPrice)}$")
                            }
                            if (teleportPolicyChanged) {
                                if (isNotEmpty()) append(" | ")
                                append("Téléport: ${oldPolicy.name} → ${newPolicy.name}")
                            }
                        }
                        plugin.logger.info("[SUCCESS] Shop ${shop.id} modifié par ${player.name} - $changesSummary")
                    } else {
                        player.sendMessage("§cErreur lors de la sauvegarde des modifications !")
                        plugin.logger.severe("[ERROR] Impossible de sauvegarder les modifications du shop ${shop.id}")
                    }
                })
            }
    }

    private fun updateShopSignDisplay(shop: Shop, player: Player) {
        try {
            val shopLoc = shop.getLocation() ?: return
            val world = plugin.server.getWorld(shopLoc.world!!.name)
            if (world == null) {
                plugin.logger.warning("[WARNING] Monde introuvable pour le shop ${shop.id}")
                return
            }

            val shopLocation = Location(world, shopLoc.x.toDouble(), shopLoc.y.toDouble(), shopLoc.z.toDouble())
            var signBlock = shopLocation.block
            if (signBlock.state !is Sign) {
                signBlock = findAdjacentShopSign(shopLocation.block) ?: return
            }

            if (signBlock.state is Sign) {
                val chestBlock = findAdjacentChest(signBlock)
                var chestInventory: org.bukkit.inventory.Inventory? = null

                if (chestBlock != null && chestBlock.state is Chest) {
                    val chest = chestBlock.state as Chest
                    chestInventory = chest.inventory
                }

                updateSignText(shop, signBlock, chestInventory)
                plugin.logger.info("[SUCCESS] Pancarte du shop ${shop.id} mise à jour automatiquement")
            } else {
                plugin.logger.warning("[WARNING] Pancarte introuvable pour le shop ${shop.id}")
                player.sendMessage("§e⚠ Pancarte non trouvée pour la mise à jour automatique")
            }
        } catch (e: Exception) {
            plugin.logger.severe("[ERROR] Erreur lors de la mise à jour de la pancarte: ${e.message}")
            player.sendMessage("§c✗ Erreur lors de la mise à jour de la pancarte")
        }
    }

    private fun findAdjacentShopSign(block: org.bukkit.block.Block): org.bukkit.block.Block? {
        val faces = arrayOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)
        for (face in faces) {
            val adjacent = block.getRelative(face)
            if (adjacent.state is Sign) return adjacent
        }
        return null
    }

    private fun findAdjacentChest(block: org.bukkit.block.Block): org.bukkit.block.Block? {
        val faces = arrayOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)
        for (face in faces) {
            val adjacent = block.getRelative(face)
            if (adjacent.state is Chest) return adjacent
        }
        return null
    }

    private fun updateSignText(shop: Shop, signBlock: org.bukkit.block.Block, chestInventory: org.bukkit.inventory.Inventory?) {
        if (signBlock.state !is Sign) return
        val sign = signBlock.state as Sign

        try {
            var realStock = 0
            if (chestInventory != null) {
                val targetItem = shop.item.toItemStack()
                chestInventory.contents.filterNotNull().forEach { item ->
                    if (item.isSimilar(targetItem)) {
                        realStock += item.amount
                    }
                }
            }

            val shopType = if (shop.type == Shop.ShopType.SELL) "[VENTE]" else "[ACHAT]"
            val typeColor = if (shop.type == Shop.ShopType.SELL) NamedTextColor.RED else NamedTextColor.GREEN

            val lines = listOf(
                Component.text(shopType, typeColor, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.text(String.format("%.2f", shop.price), NamedTextColor.YELLOW)
                    .append(Component.text("◎", NamedTextColor.GOLD))
                    .decoration(TextDecoration.ITALIC, false),
                Component.text(shop.item.displayName, NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false),
                Component.text("Stock: $realStock", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            )

            val signSide = sign.getSide(org.bukkit.block.sign.Side.FRONT)
            for (i in lines.indices.take(4)) {
                signSide.line(i, lines[i])
            }

            sign.update()
        } catch (e: Exception) {
            plugin.logger.severe("[ERROR] Impossible de mettre à jour la pancarte: ${e.message}")
        }
    }

    private fun executeShopDeletion(player: Player, session: EditSession) {
        val shop = session.shop

        plugin.shopManager.deleteShop(shop.id)
            .thenAccept { success ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (success) {
                        try {
                            val shopLoc = shop.getLocation() ?: return@Runnable
                            val world = plugin.server.getWorld(shopLoc.world!!.name)
                            if (world != null) {
                                val shopLocation = Location(world, shopLoc.x.toDouble(), shopLoc.y.toDouble(), shopLoc.z.toDouble())
                                var signBlock: org.bukkit.block.Block? = shopLocation.block
                                if (signBlock?.state !is Sign) {
                                    signBlock = findAdjacentShopSign(shopLocation.block)
                                }

                                if (signBlock != null && signBlock.state is Sign) {
                                    signBlock.type = Material.AIR
                                }
                            }
                        } catch (e: Exception) {
                            plugin.logger.warning("[WARNING] Erreur lors de la suppression physique de la pancarte: ${e.message}")
                        }

                        player.sendMessage("§c§l✓ Shop supprimé définitivement !")
                        player.sendMessage("§7Vous pouvez récupérer les items restants dans le coffre")

                        player.closeInventory()
                        editSessions.remove(player.uniqueId)

                        plugin.logger.info("[SUCCESS] Shop ${shop.id} supprimé par ${player.name}")
                    } else {
                        player.sendMessage("§cErreur lors de la suppression du shop !")
                        plugin.logger.severe("[ERROR] Impossible de supprimer le shop ${shop.id}")
                    }
                })
            }
    }

    private fun confirmShopDeletion(player: Player, session: EditSession) {
        val shop = session.shop

        player.sendMessage("§c§l⚠ CONFIRMATION DE SUPPRESSION ⚠")
        player.sendMessage("§eÊtes-vous vraiment sûr de vouloir supprimer ce shop ?")
        player.sendMessage("§7- Article: §e${shop.item.displayName}")
        player.sendMessage("§7- Stock restant: §e${shop.stock}")
        player.sendMessage("§7- Prix: §e${String.format("%.2f", shop.price)}$")
        player.sendMessage("§c§lCliquez à nouveau sur TNT pour CONFIRMER la suppression")
        player.sendMessage("§a§lOu cliquez sur ANNULER pour revenir en arrière")

        session.pendingDeletion = true
        session.deletionConfirmationTime = System.currentTimeMillis()

        setupEditMenu(player.openInventory.topInventory, player, session)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val currentSession = editSessions[player.uniqueId]
            if (currentSession != null && currentSession.pendingDeletion) {
                currentSession.pendingDeletion = false
                if (player.openInventory.topInventory.size == 45) {
                    setupEditMenu(player.openInventory.topInventory, player, currentSession)
                    player.sendMessage("§a✓ Délai de confirmation expiré - Suppression annulée")
                }
            }
        }, 200L)
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val title = event.view.title().toString()
        if (title.contains("Édition")) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val session = editSessions[player.uniqueId]
                if (session != null && session.isExpired()) {
                    editSessions.remove(player.uniqueId)
                }
            }, 60L)
        }
    }

    data class EditSession(
        val shop: Shop,
        var tempPrice: Double,
        var tempTeleportPolicy: Shop.TeleportPolicy = shop.teleportPolicy,
        var pendingDeletion: Boolean = false,
        var deletionConfirmationTime: Long = 0,
        private val creationTime: Long = System.currentTimeMillis()
    ) {
        fun adjustPrice(adjustment: Double) {
            tempPrice = maxOf(0.01, tempPrice + adjustment)
            if (tempPrice > 1000000) tempPrice = 1000000.0
        }

        fun isExpired(): Boolean = System.currentTimeMillis() - creationTime > 300000

        fun isDeletionConfirmationExpired(): Boolean = 
            pendingDeletion && (System.currentTimeMillis() - deletionConfirmationTime > 10000)

        fun toggleTeleportPolicy() {
            tempTeleportPolicy = when (tempTeleportPolicy) {
                Shop.TeleportPolicy.ALLOW_TP -> Shop.TeleportPolicy.BLOCK_TP_AND_SUGGEST_DM
                Shop.TeleportPolicy.BLOCK_TP_AND_SUGGEST_DM -> Shop.TeleportPolicy.ALLOW_TP
            }
        }
    }
}

