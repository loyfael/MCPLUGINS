package loyfael.listener

import loyfael.Main
import loyfael.model.Shop
import loyfael.model.ShopItem
import loyfael.util.ShopInteractionUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.*
import org.bukkit.block.data.type.WallSign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * Gestionnaire de création de shops via clic droit avec panneau en main
 */
class ShopCreationListener(private val plugin: Main) : Listener {

    private val pendingConfigurations = mutableMapOf<UUID, ShopConfiguration>()

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return

        // === CRÉATION DE SHOP ===
        val itemInHand = player.inventory.itemInMainHand
        if (isSignMaterial(itemInHand.type) && isChestBlock(clickedBlock)) {
            if (player.isSneaking) {
                plugin.logger.info("[DEBUG] Joueur ${player.name} accroupi - Placement normal du panneau autorisé")
                return
            } else {
                plugin.logger.info("[DEBUG] Joueur ${player.name} debout - Déclenchement création shop")
                handleShopCreation(event, player, clickedBlock)
                return
            }
        }

        // === ÉDITION DE SHOP ===
        if (clickedBlock.state is Sign) {
            val sign = clickedBlock.state as Sign
            val lines = ShopInteractionUtils.getSignLines(sign)
            if (isShopSign(lines)) {
                handleShopEdit(event, player, sign)
            }
        }
    }

    private fun handleShopCreation(event: PlayerInteractEvent, player: Player, chestBlock: Block) {
        event.isCancelled = true

        if (!player.hasPermission("aetherplayershop.create")) {
            player.sendMessage("§cVous n'avez pas la permission de créer des shops.")
            return
        }

        val chest = chestBlock.state as Chest
        val validation = ShopItemValidation.validate(chest)
        if (!validation.valid) {
            player.sendMessage("§c${validation.errorMessage}")
            return
        }

        if (plugin.configManager.isCatalogOnly()) {
            player.sendMessage("§eCe serveur est en mode catalogue. Crée tes shops sur le serveur §6${plugin.configManager.primaryServer}§e.")
            return
        }

        plugin.shopManager.getPlayerShopCount(player.uniqueId)
            .thenAccept { count ->
                val maxShops = getMaxShopsFromPermissions(player, plugin.configManager.maxShopsPerPlayer)
                if (count >= maxShops && !player.hasPermission("aetherplayershop.bypasslimit")) {
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        player.sendMessage("§cVous avez atteint la limite de $maxShops shops.")
                    })
                    return@thenAccept
                }

                val config = ShopConfiguration(
                    player.uniqueId,
                    null,
                    chestBlock.location,
                    validation.shopItem!!,
                    validation.totalQuantity
                )

                pendingConfigurations[player.uniqueId] = config

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    openShopConfigurationMenu(player, config)
                })
            }
    }

    private fun handleShopEdit(event: PlayerInteractEvent, player: Player, sign: Sign) {
        event.isCancelled = true

        val lines = ShopInteractionUtils.getSignLines(sign)
        val ownerName = lines[3]

        if (ownerName != player.name && !player.hasPermission("aetherplayershop.admin")) {
            player.sendMessage("§cVous ne pouvez modifier que vos propres shops.")
            return
        }

        plugin.shopManager.getShopAtLocation(sign.location)
            .thenAccept outer@{ shop ->
                if (shop == null) {
                    val chestBlock = ShopInteractionUtils.findAdjacentChest(sign.block)
                    if (chestBlock != null) {
                        plugin.shopManager.getShopAtLocation(chestBlock.location)
                            .thenAccept inner@{ shop2 ->
                                if (shop2 == null) {
                                    player.sendMessage("§cShop introuvable en base de données.")
                                    return@inner
                                }
                                Bukkit.getScheduler().runTask(plugin, Runnable {
                                    setupEditConfig(player, sign, shop2)
                                })
                            }
                        return@outer
                    } else {
                        player.sendMessage("§cShop introuvable en base de données.")
                        return@outer
                    }
                }
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    setupEditConfig(player, sign, shop)
                })
            }
    }

    private fun setupEditConfig(player: Player, sign: Sign, shop: Shop) {
        val config = ShopConfiguration(
            player.uniqueId,
            sign.location,
            shop.getLocation()!!,
            shop.item,
            shop.stock
        )
        config.shopType = shop.type
        config.price = shop.price
        config.teleportPolicy = shop.teleportPolicy
        pendingConfigurations[player.uniqueId] = config
        openShopConfigurationMenu(player, config)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onSignBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.state !is Sign) return

        val sign = block.state as Sign
        val lines = ShopInteractionUtils.getSignLines(sign)
        if (!isShopSign(lines)) return

        val player = event.player
        val ownerName = lines[3]

        if (ownerName != player.name && !player.hasPermission("aetherplayershop.admin")) {
            event.isCancelled = true
            player.sendMessage("§cVous ne pouvez détruire que vos propres shops.")
            return
        }

        plugin.shopManager.deleteShopAtLocation(block.location)
            .thenAccept { success ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (success) {
                        player.sendMessage("§aShop supprimé avec succès !")
                    } else {
                        player.sendMessage("§6Shop supprimé (non trouvé en base).")
                    }
                })
            }
    }

    private fun openShopConfigurationMenu(player: Player, config: ShopConfiguration) {
        val menu = Bukkit.createInventory(null, 54, 
            Component.text("Configuration du shop").color(NamedTextColor.DARK_GRAY))

        // Ligne 1: Type et infos
        val sellItem = ItemStack(Material.EMERALD_BLOCK)
        sellItem.editMeta {
            it.displayName(Component.text("§a§l✅ BOUTIQUE DE VENTE"))
            it.lore(listOf(
                Component.text("§7Vous vendez vos items"),
                Component.text("§7aux autres joueurs"),
                Component.empty(),
                Component.text("§7💡 Les joueurs vous achètent vos items"),
                Component.text("§7💰 Vous recevez l'argent automatiquement"),
                Component.empty(),
                Component.text("§a§l>>> MODE SÉLECTIONNÉ <<<")
            ))
        }
        menu.setItem(0, sellItem)

        val infoItem = ItemStack(Material.NETHER_STAR)
        infoItem.editMeta {
            it.displayName(Component.text("§6§l✨ Votre Shop ✨"))
            it.lore(listOf(
                Component.text("§7Configurez votre boutique"),
                Component.text("§7avec ce menu intuitif"),
                Component.empty(),
                Component.text("§eItem: §f${config.shopItem.displayName}"),
                Component.text("§eQuantité: §f${config.totalQuantity}")
            ))
        }
        menu.setItem(4, infoItem)

        val tpPolicy = ItemStack(Material.ENDER_PEARL)
        tpPolicy.editMeta {
            it.displayName(Component.text("§dVeux-tu laisser les joueurs se téléporter à ton shop?"))
            it.lore(listOf(
                Component.text("§aOui - Clic droit"),
                Component.text("§cNon - Clic gauche"),
                Component.empty(),
                Component.text("§7Actuel: §e${getTeleportPolicyDisplay(config.teleportPolicy)}")
            ))
        }
        menu.setItem(8, tpPolicy)

        for (i in listOf(1, 2, 3, 5, 6, 7)) {
            menu.setItem(i, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"))
        }

        // Bordures
        listOf(9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 52, 53).forEach {
            menu.setItem(it, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"))
        }

        // Item principal (slot 13)
        val displayItem = config.shopItem.toDisplayItemStack(1)
        displayItem.editMeta {
            it.displayName(Component.text("§6§l» ${config.shopItem.displayName} §6§l«"))
            it.lore(listOf(
                Component.text("§7Ceci est l'item de votre shop"),
                Component.empty(),
                Component.text("§eQuantité disponible: §a${config.totalQuantity}"),
                Component.text("§eType: §f${formatMaterialName(config.shopItem.material)}")
            ))
        }
        menu.setItem(13, displayItem)

        // Ajout info prix moyen (async)
        plugin.shopManager.getAveragePriceForItem(config.shopItem, 14).thenAccept { avg ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val updated = menu.getItem(13) ?: return@Runnable
                updated.editMeta { meta ->
                    val lore = (meta.lore() ?: listOf()).toMutableList()
                    lore.add(Component.empty())
                    if (avg == null) {
                        lore.add(Component.text("§7Prix moyen: §8Cet item n'a pas encore été mis en vente."))
                    } else {
                        lore.add(Component.text("§7Prix moyen: §e${String.format("%.2f", avg)}§6◎"))
                    }
                    meta.lore(lore)
                }
                player.updateInventory()
            })
        }

        // Contrôles de prix (ligne 4: slots 28-34)
        createPriceButton(menu, 28, Material.IRON_BLOCK, "§c§l🔻 -1000◎", -1000.0,
            listOf("§7Diminuer le prix de 1000◎", "§8Grosse réduction"))
        createPriceButton(menu, 29, Material.IRON_INGOT, "§c§l🔻 -100◎", -100.0,
            listOf("§7Diminuer le prix de 100◎", "§8Réduction moyenne"))
        createPriceButton(menu, 30, Material.IRON_NUGGET, "§c§l🔻 -1◎", -1.0,
            listOf("§7Diminuer le prix de 1◎", "§8Ajustement précis"))
        createPriceButton(menu, 32, Material.GOLD_NUGGET, "§a§l🔺 +1◎", 1.0,
            listOf("§7Augmenter le prix de 1◎", "§8Ajustement précis"))
        createPriceButton(menu, 33, Material.GOLD_INGOT, "§a§l🔺 +100◎", 100.0,
            listOf("§7Augmenter le prix de 100◎", "§8Augmentation moyenne"))
        createPriceButton(menu, 34, Material.GOLD_BLOCK, "§a§l🔺 +1000◎", 1000.0,
            listOf("§7Augmenter le prix de 1000◎", "§8Grosse augmentation"))

        menu.setItem(31, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"))

        // Reset (slot 40)
        val resetItem = ItemStack(Material.BARRIER)
        resetItem.editMeta {
            it.displayName(Component.text("§c§l🔄 RESET"))
            it.lore(listOf(
                Component.text("§7Remettre le prix à 1.00◎"),
                Component.empty(),
                Component.text("§eUtile pour recommencer")
            ))
        }
        menu.setItem(40, resetItem)

        // Actions finales
        val cancelItem = ItemStack(Material.RED_TERRACOTTA)
        cancelItem.editMeta {
            it.displayName(Component.text("§c§l❌ ANNULER & FERMER"))
            it.lore(listOf(
                Component.text("§7Fermer sans sauvegarder"),
                Component.text("§cAucune modification ne sera appliquée"),
                Component.empty(),
                Component.text("§8Le shop ne sera pas créé")
            ))
        }
        menu.setItem(48, cancelItem)

        val editing = config.signLocation != null
        val validateItem = ItemStack(Material.LIME_TERRACOTTA)
        validateItem.editMeta {
            it.displayName(Component.text(if (editing) "§a§l✅ SAUVEGARDER" else "§a§l✅ CRÉER LE SHOP"))
            val lore = mutableListOf<Component>()
            lore.add(Component.text(if (editing) "§7Mettre à jour ce shop" else "§7Finaliser la création du shop"))
            lore.add(Component.empty())
            lore.add(Component.text("§a📊 Résumé:"))
            lore.add(Component.text("§fType: §e${if (config.shopType == Shop.ShopType.SELL) "VENTE" else "ACHAT"}"))
            lore.add(Component.text("§fPrix: §e${String.format("%.2f", config.price)}◎"))
            lore.add(Component.text("§fItem: §e${config.shopItem.displayName}"))
            lore.add(Component.text("§fQuantité: §e${config.totalQuantity}"))
            lore.add(Component.empty())
            lore.add(Component.text(if (editing) "§a§lClique pour sauvegarder!" else "§a§lClique pour confirmer!"))
            it.lore(lore)
        }
        menu.setItem(50, validateItem)

        player.openInventory(menu)
    }

    private fun createDecorativeItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        item.editMeta { it.displayName(Component.text(name)) }
        return item
    }

    @Suppress("UNUSED_PARAMETER")
    private fun createPriceButton(menu: Inventory, slot: Int, material: Material, name: String, change: Double, extraLore: List<String>) {
        val item = ItemStack(material)
        item.editMeta {
            it.displayName(Component.text(name))
            val lore = extraLore.map { line -> Component.text(line) }.toMutableList()
            lore.add(Component.empty())
            lore.add(Component.text("§8Clic: modifier le prix"))
            it.lore(lore)
        }
        menu.setItem(slot, item)
    }

    private fun isShopSign(lines: Array<String>): Boolean {
        if (lines.size < 3) return false
        val firstLine = lines[0].lowercase()
        return firstLine.contains("[shop]") || firstLine.contains("[buy]") || firstLine.contains("[sell]") ||
                firstLine.contains("[achat]") || firstLine.contains("[vente]")
    }

    private fun refreshConfigMenu(player: Player, config: ShopConfiguration) {
        openShopConfigurationMenu(player, config)
    }

    private fun finalizeShopCreation(player: Player, config: ShopConfiguration) {
        player.closeInventory()

        val editing = config.signLocation != null
        val initialStock = if (config.shopType == Shop.ShopType.SELL) config.totalQuantity else 0

        val future = if (editing) {
            plugin.shopManager.getShopAtLocation(config.chestLocation)
                .thenCompose { existing ->
                    if (existing == null) return@thenCompose java.util.concurrent.CompletableFuture.completedFuture(false)
                    existing.price = config.price
                    existing.teleportPolicy = config.teleportPolicy
                    plugin.shopManager.updateShop(existing)
                }
        } else {
            plugin.shopManager.createShop(player, config.chestLocation, config.shopType, config.shopItem,
                config.price, initialStock, config.teleportPolicy)
        }

        future.thenAccept { success ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (success) {
                    if (editing) {
                        player.sendMessage("§a§lShop mis à jour !")
                    } else {
                        placePanelAutomatically(config, player)
                        player.sendMessage("§a§lShop créé avec succès !")
                        player.sendMessage("§7Item: §e${config.shopItem.displayName}")
                    }
                    player.sendMessage("§7Prix: §e${String.format("%.2f", config.price)}◎")
                    player.sendMessage("§7Téléportation: §e${getTeleportPolicyDisplay(config.teleportPolicy)}")

                    if (!editing) removeSignFromInventory(player)

                    if (plugin.configManager.isSoundEnabled()) {
                        player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                    }
                    if (plugin.configManager.isParticleEnabled()) {
                        player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, config.chestLocation.add(0.5, 1.0, 0.5), 10)
                    }
                } else {
                    player.sendMessage(if (editing) "§c§lErreur lors de la mise à jour !" else "§c§lErreur lors de la création du shop !")
                }
                pendingConfigurations.remove(player.uniqueId)
            })
        }
    }

    private fun placePanelAutomatically(config: ShopConfiguration, player: Player) {
        val chestBlock = config.chestLocation.block
        val playerLoc = player.location
        val chestLoc = chestBlock.location.add(0.5, 0.5, 0.5)

        val deltaX = playerLoc.x - chestLoc.x
        val deltaZ = playerLoc.z - chestLoc.z

        val targetBlock = if (Math.abs(deltaX) > Math.abs(deltaZ)) {
            if (deltaX > 0) chestBlock.getRelative(1, 0, 0) else chestBlock.getRelative(-1, 0, 0)
        } else {
            if (deltaZ > 0) chestBlock.getRelative(0, 0, 1) else chestBlock.getRelative(0, 0, -1)
        }

        if (targetBlock.type == Material.AIR || targetBlock.type.isAir || targetBlock.isEmpty) {
            targetBlock.type = Material.OAK_WALL_SIGN
            if (targetBlock.blockData is WallSign) {
                val wallSign = targetBlock.blockData as WallSign
                wallSign.facing = when {
                    Math.abs(deltaX) > Math.abs(deltaZ) -> if (deltaX > 0) BlockFace.EAST else BlockFace.WEST
                    else -> if (deltaZ > 0) BlockFace.SOUTH else BlockFace.NORTH
                }
                targetBlock.blockData = wallSign
            }

            if (targetBlock.state is Sign) {
                config.signLocation = targetBlock.location
                updateSignWithShopInfo(config, player)
                player.sendMessage("§aPanneau placé automatiquement sur la face du coffre face à vous !")
                return
            }
        }

        val otherFaces = listOf(
            chestBlock.getRelative(1, 0, 0), chestBlock.getRelative(-1, 0, 0),
            chestBlock.getRelative(0, 0, 1), chestBlock.getRelative(0, 0, -1),
            chestBlock.getRelative(0, 1, 0)
        )

        for (face in otherFaces) {
            if (face.type == Material.AIR || face.type.isAir || face.isEmpty) {
                if (face.y == chestBlock.y) {
                    face.type = Material.OAK_WALL_SIGN
                    if (face.blockData is WallSign) {
                        val wallSign = face.blockData as WallSign
                        wallSign.facing = when {
                            face.x > chestBlock.x -> BlockFace.WEST
                            face.x < chestBlock.x -> BlockFace.EAST
                            face.z > chestBlock.z -> BlockFace.NORTH
                            else -> BlockFace.SOUTH
                        }
                        face.blockData = wallSign
                    }
                } else {
                    face.type = Material.OAK_SIGN
                }

                if (face.state is Sign) {
                    config.signLocation = face.location
                    updateSignWithShopInfo(config, player)
                    player.sendMessage("§6Panneau placé automatiquement à côté du coffre !")
                    return
                }
            }
        }

        player.sendMessage("§6Impossible de placer automatiquement le panneau. Placez-le manuellement à côté du coffre.")
    }

    private fun removeSignFromInventory(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        if (isSignMaterial(itemInHand.type)) {
            if (itemInHand.amount > 1) {
                itemInHand.amount -= 1
            } else {
                player.inventory.setItemInMainHand(ItemStack(Material.AIR))
            }
        }
    }

    private fun cancelShopCreation(player: Player) {
        pendingConfigurations.remove(player.uniqueId)
        player.closeInventory()
        player.sendMessage("§6Création du shop annulée.")
    }

    private fun isSignMaterial(material: Material): Boolean {
        return material.name.endsWith("_SIGN") && !material.name.contains("WALL")
    }

    private fun isChestBlock(block: Block): Boolean {
        return block.type == Material.CHEST || block.type == Material.TRAPPED_CHEST
    }

    private fun updateSignWithShopInfo(config: ShopConfiguration, player: Player) {
        val signBlock = config.signLocation?.block ?: return
        if (signBlock.state !is Sign) return

        val sign = signBlock.state as Sign
        val shopTypeDisplay = if (config.shopType == Shop.ShopType.SELL) "§c[VENTE]" else "§a[ACHAT]"

        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(0, Component.text(shopTypeDisplay))
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(1,
            Component.text("§e${String.format("%.2f", config.price)}§6◎"))
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(2,
            Component.text(formatItemDisplayName(config.shopItem)))
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(3, Component.text("§8${player.name}"))

        sign.update()
    }

    private fun formatItemDisplayName(shopItem: ShopItem): String {
        // Si l'item a un nom custom, on affiche UNIQUEMENT le nom custom (pas le type)
        return if (shopItem.customName != null && shopItem.customName.isNotEmpty()) {
            "§5${truncateText(shopItem.customName, 15)}"
        } else {
            // Sinon on affiche le nom du matériau
            val materialName = formatMaterialName(shopItem.material)
            "§3$materialName"
        }
    }

    private fun formatMaterialName(material: Material): String {
        val name = material.name.lowercase().replace('_', ' ')
        val words = name.split(" ")
        val result = words.joinToString(" ") { word ->
            if (word.isNotEmpty()) word[0].uppercase() + word.substring(1) else ""
        }
        return truncateText(result, 12)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.view.title() != Component.text("Configuration du shop").color(NamedTextColor.DARK_GRAY)) return

        event.isCancelled = true

        val config = pendingConfigurations[player.uniqueId] ?: return
        val slot = event.slot

        when (slot) {
            28 -> { config.adjustPrice(-1000.0); refreshConfigMenu(player, config) }
            29 -> { config.adjustPrice(-100.0); refreshConfigMenu(player, config) }
            30 -> { config.adjustPrice(-1.0); refreshConfigMenu(player, config) }
            32 -> { config.adjustPrice(1.0); refreshConfigMenu(player, config) }
            33 -> { config.adjustPrice(100.0); refreshConfigMenu(player, config) }
            34 -> { config.adjustPrice(1000.0); refreshConfigMenu(player, config) }
            40 -> { config.price = 1.0; refreshConfigMenu(player, config) }
            8 -> {
                if (event.isRightClick) config.teleportPolicy = Shop.TeleportPolicy.ALLOW_TP
                else if (event.isLeftClick) config.teleportPolicy = Shop.TeleportPolicy.BLOCK_TP_AND_SUGGEST_DM
                refreshConfigMenu(player, config)
            }
            50 -> finalizeShopCreation(player, config)
            48 -> cancelShopCreation(player)
        }
    }

    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text else text.substring(0, maxLength - 2) + ".."
    }

    private fun getMaxShopsFromPermissions(player: Player, fallback: Int): Int {
        var max = fallback
        player.effectivePermissions.forEach { pai ->
            val perm = pai.permission
            if (perm.startsWith("aetherplayershop.shops.")) {
                val tail = perm.substring("aetherplayershop.shops.".length)
                try {
                    max = maxOf(max, tail.toInt())
                } catch (ignored: NumberFormatException) {}
            }
        }
        return max
    }

    private fun getTeleportPolicyDisplay(policy: Shop.TeleportPolicy): String {
        return when (policy) {
            Shop.TeleportPolicy.ALLOW_TP -> "Autoriser"
            Shop.TeleportPolicy.BLOCK_TP_AND_SUGGEST_DM -> "Bloquer & suggérer MP"
        }
    }
}
