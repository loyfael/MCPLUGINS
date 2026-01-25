package loyfael.gui

import loyfael.Main
import loyfael.manager.ShopManager
import loyfael.model.Shop
import loyfael.model.ShopItem
import loyfael.util.ShopInteractionUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Sign
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.WallSign
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * GUI principal du menu central global - Système avancé inspiré des hôtels des ventes
 */
class ShopMenuGUI(private val plugin: Main) : CommandExecutor, Listener, InventoryHolder {

    private val activeSessions = ConcurrentHashMap<UUID, MenuSession>()
    private val actionCooldowns = ConcurrentHashMap<String, Long>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        startPeriodicRefresh()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.")
            return true
        }

        if (!sender.hasPermission("aetherplayershop.menu")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.")
            return true
        }

        openMainMenu(sender)
        return true
    }

    fun openMainMenu(player: Player) {
        plugin.shopManager.searchShops(ShopManager.ShopSearchFilter())
            .thenAccept { shops ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val session = MenuSession(player, shops)
                    activeSessions[player.uniqueId] = session
                    displayMainMenu(session)
                })
            }
    }

    private fun displayMainMenu(session: MenuSession) {
        val title = plugin.configManager.menuTitle
        val inventory = Bukkit.createInventory(this, 54, Component.text(title.toString()))

        val grouped = session.groupShopsByMaterial()
        val materials = grouped.keys.toList()

        // Afficher maximum 45 items par page (pour laisser place aux boutons en bas)
        val itemsPerPage = 45
        val startIndex = session.currentPage * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, materials.size)

        var gridIndex = 0
        for (i in startIndex until endIndex) {
            val material = materials[i]
            val shopsForMaterial = grouped[material] ?: continue
            val displayItem = createMaterialDisplayItem(material, shopsForMaterial)
            inventory.setItem(gridIndex++, displayItem)
        }

        // Ajouter des vitres décoratives en bordure
        addDecorativeBorder(inventory)
        
        addNavigationButtons(inventory, session, materials.size)
        addFilterButtons(inventory, session)

        session.player.openInventory(inventory)
    }

    private fun createMaterialDisplayItem(material: Material, shops: List<Shop>): ItemStack {
        val representativeShop = shops.minByOrNull { it.price } ?: shops.first()
        val representativeItem = resolveDisplayShopItem(representativeShop)
        val icon = representativeItem.toDisplayItemStack(1)

        icon.editMeta {
            representativeItem.customNameComponent?.let { customName ->
                it.displayName(customName.decoration(TextDecoration.ITALIC, false))
            } ?: run {
                it.displayName(Component.text(formatMaterialName(material), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false))
            }

            val avgPrice = shops.map { it.price }.average()
            val cheapestShop = shops.minByOrNull { it.price }
            val totalStock = shops.sumOf { it.stock }

            val lore = mutableListOf<Component>()
            lore.add(Component.empty())
            lore.add(Component.text("§8▪ §7Shops disponibles: §e${shops.size}")
                .decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text("§8▪ §7Stock total: §e$totalStock")
                .decoration(TextDecoration.ITALIC, false))

            if (avgPrice.isFinite()) {
                lore.add(Component.text("§8▪ §7Prix moyen: §6${String.format("%.2f", avgPrice)}§e◎")
                    .decoration(TextDecoration.ITALIC, false))
            }

            cheapestShop?.let { shop ->
                lore.add(Component.empty())
                lore.add(Component.text("§6⭐ Meilleur prix")
                    .decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("  §8▸ §7Prix: §a${String.format("%.2f", shop.price)}§e◎")
                    .decoration(TextDecoration.ITALIC, false))
                lore.add(Component.text("  §8▸ §7Vendeur: §e${shop.ownerName}")
                    .decoration(TextDecoration.ITALIC, false))
            }

            lore.add(Component.empty())
            lore.add(Component.text("§a§l▶ §aClic gauche: §fEn savoir plus")
                .decoration(TextDecoration.ITALIC, false))

            it.lore(lore)
        }
        return icon
    }

    fun openMaterialShops(player: Player, material: Material) {
        val filter = ShopManager.ShopSearchFilter().apply {
            this.material = material
            sortBy = ShopManager.ShopSearchFilter.SortBy.PRICE_ASC
        }

        plugin.shopManager.searchShops(filter)
            .thenAccept { shops ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    var session = activeSessions[player.uniqueId]
                    if (session == null) {
                        session = MenuSession(player, shops)
                        activeSessions[player.uniqueId] = session
                    }
                    displayShopList(session, shops, material)
                })
            }
    }

    private fun displayShopList(session: MenuSession, shops: List<Shop>, material: Material) {
        session.setDetailView(shops, material.name)
        displayShopList(session.player, shops, material.name)
    }

    private fun displayShopList(player: Player, shops: List<Shop>, title: String) {
        val inventory = Bukkit.createInventory(this, 54,
            Component.text("Shops - ${formatMaterialName(Material.valueOf(title))}")
                .color(NamedTextColor.DARK_GRAY))

        val session = activeSessions[player.uniqueId] ?: MenuSession(player, shops).also {
            activeSessions[player.uniqueId] = it
        }

        val itemsPerPage = 45
        val startIndex = session.detailPage * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, shops.size)

        for (i in startIndex until endIndex) {
            val shop = shops[i]
            val displayItem = createShopDisplayItem(shop)
            inventory.setItem(i - startIndex, displayItem)
        }

        addDetailNavigationButtons(inventory, session, shops.size)
        player.openInventory(inventory)
    }

    private fun createShopDisplayItem(shop: Shop): ItemStack {
        val displaySource = resolveDisplayShopItem(shop)
        val item = displaySource.toDisplayItemStack(displaySource.amount)
        item.editMeta {
            displaySource.customNameComponent?.let { customName ->
                it.displayName(customName.decoration(TextDecoration.ITALIC, false))
            }

            val lore = mutableListOf<Component>()
            lore.add(Component.empty())
            lore.add(Component.text("§6● §lInformations")
                .decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text("  §8▸ §7Type: §e${shop.type.displayName}")
                .decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text("  §8▸ §7Prix unitaire: §6${String.format("%.2f", shop.price)}§e◎")
                .decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text("  §8▸ §7Stock: §e${shop.stock}")
                .decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text("  §8▸ §7Vendeur: §e${shop.ownerName}")
                .decoration(TextDecoration.ITALIC, false))
            
            lore.add(Component.empty())

            if (plugin.configManager.isTeleportEnabled()) {
                lore.add(Component.text("§a§l▶ §aClic gauche: §fSe téléporter")
                    .decoration(TextDecoration.ITALIC, false))
            }
            lore.add(Component.text("§e§l▶ §eMAJ + Clic: §fPlus d'infos")
                .decoration(TextDecoration.ITALIC, false))

            it.lore(lore)
        }
        return item
    }

    private fun resolveDisplayShopItem(shop: Shop): ShopItem {
        val current = shop.item
        if (current.customNameComponent != null || current.requiresExactMatch) {
            return current
        }

        val chestLocation = shop.getLocation() ?: return current
        val world = chestLocation.world
        if (world != null) {
            val chunkX = chestLocation.blockX shr 4
            val chunkZ = chestLocation.blockZ shr 4
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                return current
            }
        }

        val chestState = chestLocation.block.state as? Chest ?: return current

        val matchingStack = chestState.inventory.contents.firstOrNull { stack ->
            stack != null && (current.requiresExactMatch && current.matches(stack) || stack.type == current.material)
        }

        return updateShopItemMetadataIfNeeded(shop, matchingStack)
    }

    private fun updateShopItemMetadataIfNeeded(shop: Shop, candidate: ItemStack?): ShopItem {
        if (candidate == null) return shop.item

        val current = shop.item
        val enrichedItem = ShopItem(candidate.clone().apply { amount = current.amount })
        val hasMetadata = enrichedItem.customNameComponent != null || enrichedItem.requiresExactMatch

        if (!hasMetadata) {
            return current
        }

        if (current.customNameComponent == null || !current.requiresExactMatch) {
            shop.item = enrichedItem
            plugin.shopManager.updateShopItemData(shop.id, enrichedItem)
            return enrichedItem
        }

        return current
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) return
        if (event.inventory.holder != this) return

        event.isCancelled = true

        val player = event.whoClicked as Player
        val session = activeSessions[player.uniqueId] ?: return
        val clickedItem = event.currentItem ?: return
        if (clickedItem.type == Material.AIR) return

        handleMenuClick(player, session, event)
    }

    private data class ShopSelection(
        val shop: Shop,
        val location: Location,
        val chest: Chest,
        val chestInventory: org.bukkit.inventory.Inventory
    )

    private fun selectBestShopForMaterial(
        player: Player,
        session: MenuSession,
        material: Material,
        excludeOwned: Boolean = false
    ): ShopSelection? {
        val grouped = session.groupShopsByMaterial()
        val shops = grouped[material]
        if (shops == null || shops.isEmpty()) {
            player.sendMessage("§8[§c✘§8] §cAucun shop disponible avec du stock pour ${formatMaterialName(material)} !")
            return null
        }

        val availableShops = shops
            .filter { it.active && it.stock > 0 && (!excludeOwned || it.ownerUUID != player.uniqueId) }
            .sortedBy { it.price }
        if (availableShops.isEmpty()) {
            val message = if (excludeOwned) {
                "§8[§c✘§8] §cAucun shop d'un autre joueur disponible avec du stock pour ${formatMaterialName(material)} !"
            } else {
                "§8[§c✘§8] §cAucun shop disponible avec du stock pour ${formatMaterialName(material)} !"
            }
            player.sendMessage(message)
            return null
        }

        val bestShop = availableShops.first()
        val location = bestShop.getLocation()
        if (location == null) {
            player.sendMessage("§8[§c✘§8] §cImpossible de trouver la localisation du shop.")
            return null
        }

        val signBlock = location.block
        val chestBlock = ShopInteractionUtils.findAdjacentChest(signBlock)
        if (chestBlock == null || chestBlock.state !is Chest) {
            player.sendMessage("§8[§c✘§8] §cCoffre du shop introuvable !")
            return null
        }

        val chestState = chestBlock.state as Chest
        val chestInventory = chestState.inventory
        val actualStock = ShopInteractionUtils.countItemsInInventory(chestInventory, bestShop.item.toItemStack())
        if (actualStock <= 0) {
            player.sendMessage("§8[§c✘§8] §cLe meilleur shop n'a plus de stock ! Essayez un autre shop.")
            return null
        }

        val candidateStack = chestInventory.contents.firstOrNull { stack ->
            stack != null && (bestShop.item.requiresExactMatch && bestShop.item.matches(stack) || stack.type == bestShop.item.material)
        }
        updateShopItemMetadataIfNeeded(bestShop, candidateStack)

        return ShopSelection(bestShop, location, chestState, chestInventory)
    }

    private fun handleMenuClick(player: Player, session: MenuSession, event: InventoryClickEvent) {
        val slot = event.slot
        val item = event.currentItem ?: return

        when (item.type) {
            Material.ARROW -> {
                handleNavigation(player, session, item)
                return
            }
            Material.HOPPER -> {
                openFilterMenu(player, session)
                return
            }
            Material.BARRIER -> {
                session.setMainView()
                displayMainMenu(session)
                return
            }
            Material.BOOK -> {
                handlePageBookClick(player, session, event)
                return
            }
            Material.GRAY_STAINED_GLASS_PANE -> {
                // Les vitres décoratives ne font rien
                return
            }
            else -> {}
        }

        if (session.isInMainView()) {
            if (event.isLeftClick) {
                handleDetailView(player, session, item.type)
            }
            return
        }

        if (session.isInDetailView() && event.isLeftClick && plugin.configManager.isTeleportEnabled()) {
            handleShopTeleport(player, session, slot)
        }
    }

    private fun shouldProcessAction(player: Player, action: String, cooldownMs: Long = 200L): Boolean {
        val key = "${player.uniqueId}:$action"
        val now = System.currentTimeMillis()
        val last = actionCooldowns[key]
        if (last != null && now - last < cooldownMs) {
            return false
        }
        actionCooldowns[key] = now
        return true
    }

    private fun handlePageBookClick(player: Player, session: MenuSession, event: InventoryClickEvent) {
        val itemsPerPage = 45

        if (session.isInDetailView()) {
            val totalItems = session.getCurrentDetailList().size
            if (totalItems <= itemsPerPage) return
            val totalPages = (totalItems + itemsPerPage - 1) / itemsPerPage

            if (event.isLeftClick && session.detailPage < totalPages - 1) {
                session.detailPage++
                displayShopList(player, session.getCurrentDetailList(), session.selectedMaterialName ?: "")
            } else if (event.isRightClick && session.detailPage > 0) {
                session.detailPage--
                displayShopList(player, session.getCurrentDetailList(), session.selectedMaterialName ?: "")
            }
            return
        }

        val grouped = session.groupShopsByMaterial()
        val totalItems = grouped.size
        if (totalItems <= itemsPerPage) return
        val totalPages = (totalItems + itemsPerPage - 1) / itemsPerPage

        if (event.isLeftClick && session.currentPage < totalPages - 1) {
            session.currentPage++
            displayMainMenu(session)
        } else if (event.isRightClick && session.currentPage > 0) {
            session.currentPage--
            displayMainMenu(session)
        }
    }

    /**
     * Téléporte directement au meilleur shop (prix le plus bas avec stock) pour le matériau donné
     */
    private fun handleTeleportToBestShop(player: Player, session: MenuSession, material: Material) {
        if (!shouldProcessAction(player, "teleport-main")) return
    val selection = selectBestShopForMaterial(player, session, material, excludeOwned = true) ?: return

        player.closeInventory()

        val delay = plugin.configManager.teleportDelay
        if (delay > 0) {
            player.sendMessage("§8[§6⏱§8] §eTéléportation au shop dans §6$delay§e secondes...")
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                teleportPlayerToShop(player, selection.location, selection.shop)
            }, (delay * 20L))
        } else {
            teleportPlayerToShop(player, selection.location, selection.shop)
        }
    }

    private fun handleShopTeleport(player: Player, session: MenuSession, slot: Int) {
        if (!shouldProcessAction(player, "teleport-detail")) return
        val targetShop = session.getDetailShopAtSlot(slot) ?: return

        if (targetShop.ownerUUID == player.uniqueId) {
            player.sendMessage("§8[§c✘§8] §cVous ne pouvez pas vous téléporter à votre propre shop ! §7Dans le serveur §bskyland§7, utilisez votre §a§l/lands spawn §7ou §a§l/home§7 pour retourner à votre base.")
            return
        }

        if (plugin.configManager.isCatalogOnly()) {
            player.sendMessage("§e${plugin.configManager.catalogClickMessage}")
            return
        }

        if (targetShop.teleportPolicy == Shop.TeleportPolicy.BLOCK_TP_AND_SUGGEST_DM) {
            player.sendMessage("§8[§c✘§8] §e${targetShop.ownerName} §cn'autorise pas la téléportation directe à ce shop.")
            player.sendMessage("§8[§6✉§8] §7Contacte-le par message privé : §e/msg ${targetShop.ownerName} §7[message]")
            return
        }

        val location = targetShop.getLocation()!!
        player.closeInventory()
        val delay = plugin.configManager.teleportDelay

        if (delay > 0) {
            player.sendMessage("§8[§6⏱§8] §eTéléportation au shop dans §6$delay§e secondes...")
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                teleportPlayerToShop(player, location, targetShop)
            }, (delay * 20L))
        } else {
            teleportPlayerToShop(player, location, targetShop)
        }
    }

    private fun teleportPlayerToShop(player: Player, location: org.bukkit.Location, shop: Shop) {
        val chestBlock = location.block
        val rawSignBlock = when {
            chestBlock.state is Sign -> chestBlock
            else -> ShopInteractionUtils.findAdjacentShopSign(chestBlock)
        }

        val teleportLoc = computeTeleportPosition(chestBlock.location, rawSignBlock)
        player.teleport(teleportLoc)
        player.sendMessage("§8[§6✦§8] §aVous avez été téléporté au shop de §e${shop.ownerName}§a !")
    }

    private fun computeTeleportPosition(chestLocation: Location, signBlock: Block?): Location {
        val chestCenter = chestLocation.clone().add(0.5, 0.0, 0.5)

        if (signBlock != null && signBlock.state is Sign) {
            val signCenter = signBlock.location.clone().add(0.5, 0.0, 0.5)
            val wallSign = signBlock.blockData as? WallSign

            if (wallSign != null) {
                val facing = wallSign.facing
                val frontBase = signBlock.location.clone().add(facing.modX.toDouble(), 0.0, facing.modZ.toDouble())
                val candidate = frontBase.add(0.5, 0.0, 0.5)
                val safe = ensureSafeTeleport(candidate)
                orientTowards(safe, signCenter.clone().add(0.0, 0.5, 0.0))
                safe.pitch = 0f
                return safe
            }

            val safe = ensureSafeTeleport(signCenter)
            orientTowards(safe, chestCenter.clone().add(0.0, 0.5, 0.0))
            safe.pitch = 0f
            return safe
        }

        val fallback = ensureSafeTeleport(chestCenter)
        orientTowards(fallback, chestCenter.clone().add(0.0, 0.5, 0.0))
        fallback.pitch = 0f
        return fallback
    }

    private fun ensureSafeTeleport(base: Location): Location {
        val result = base.clone()
        var feetBlock = result.block
        var headBlock = feetBlock.getRelative(BlockFace.UP)

        if (!feetBlock.isPassable || !headBlock.isPassable) {
            result.add(0.0, 1.0, 0.0)
            feetBlock = result.block
            headBlock = feetBlock.getRelative(BlockFace.UP)

            if (!feetBlock.isPassable || !headBlock.isPassable) {
                result.add(0.0, 1.0, 0.0)
            }
        }

        result.add(0.0, 0.1, 0.0)
        return result
    }

    private fun orientTowards(location: Location, target: Location) {
        val direction = target.toVector().subtract(location.toVector()).apply { y = 0.0 }
        if (direction.lengthSquared() > 0.0001) {
            location.direction = direction
        }
    }

    private fun addNavigationButtons(inventory: Inventory, session: MenuSession, totalItems: Int) {
        val itemsPerPage = 45 // Utiliser 45 comme dans displayMainMenu
        val totalPages = (totalItems + itemsPerPage - 1) / itemsPerPage

        if (session.currentPage > 0) {
            val prevItem = ItemStack(Material.ARROW)
            prevItem.editMeta {
                it.displayName(Component.text("§a◀ Page précédente"))
            }
            inventory.setItem(48, prevItem)
        }

        if (session.currentPage < totalPages - 1) {
            val nextItem = ItemStack(Material.ARROW)
            nextItem.editMeta {
                it.displayName(Component.text("§aPage suivante ▶"))
            }
            inventory.setItem(50, nextItem)
        }

        val infoItem = ItemStack(Material.BOOK)
        infoItem.editMeta {
            it.displayName(Component.text("§6Page ${session.currentPage + 1}/$totalPages"))
            it.lore(listOf(
                Component.empty(),
                Component.text("§a§l▶ §aClic gauche: §fPage suivante")
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("§b§l▶ §bClic droit: §fPage précédente")
                    .decoration(TextDecoration.ITALIC, false)
            ))
        }
        inventory.setItem(49, infoItem)
    }

    private fun addFilterButtons(inventory: Inventory, @Suppress("UNUSED_PARAMETER") session: MenuSession) {
        val filterItem = ItemStack(Material.HOPPER)
        filterItem.editMeta {
            it.displayName(Component.text("Filtres avancés")
                .color(NamedTextColor.WHITE))
            it.lore(listOf(
                Component.text("§7Cliquez pour ouvrir"),
                Component.text("§7les options de filtrage")
            ))
        }
        inventory.setItem(53, filterItem)
    }

    private fun addDetailNavigationButtons(inventory: Inventory, session: MenuSession, totalShops: Int) {
        addNavigationButtons(inventory, session, totalShops)

        val backItem = ItemStack(Material.BARRIER)
        backItem.editMeta {
            it.displayName(Component.text("§c◀ Retour au menu principal"))
        }
        inventory.setItem(45, backItem)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleNavigation(player: Player, session: MenuSession, item: ItemStack) {
        val displayName = PlainTextComponentSerializer.plainText().serialize(item.itemMeta.displayName() ?: Component.empty())

        if (displayName.contains("précédente")) {
            if (session.isInDetailView()) {
                session.detailPage = maxOf(0, session.detailPage - 1)
                displayShopList(session.player, session.getCurrentDetailList(), session.selectedMaterialName ?: "")
            } else {
                session.currentPage = maxOf(0, session.currentPage - 1)
                displayMainMenu(session)
            }
        } else if (displayName.contains("suivante")) {
            if (session.isInDetailView()) {
                session.detailPage++
                displayShopList(session.player, session.getCurrentDetailList(), session.selectedMaterialName ?: "")
            } else {
                session.currentPage++
                displayMainMenu(session)
            }
        }
    }

    private fun handleDetailView(@Suppress("UNUSED_PARAMETER") player: Player, session: MenuSession, material: Material) {
        openMaterialShops(session.player, material)
    }

    /**
     * Ouvre le menu d'achat pour le meilleur shop du matériau (prix le plus bas avec stock)
     * et téléporte le joueur selon ses réglages
     */
    private fun handleBuyFromMainMenu(player: Player, session: MenuSession, material: Material) {
        if (!shouldProcessAction(player, "open-purchase")) return
        val selection = selectBestShopForMaterial(player, session, material) ?: return

        player.closeInventory()

        // Ouvrir le menu d'achat (PAS de téléportation ici)
        plugin.purchaseMenuGUI.openPurchaseMenu(player, selection.shop, selection.chestInventory)
    }

    private fun openFilterMenu(@Suppress("UNUSED_PARAMETER") player: Player, @Suppress("UNUSED_PARAMETER") session: MenuSession) {
        player.sendMessage("§6Menu de filtrage en développement...")
    }

    private fun startPeriodicRefresh() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            plugin.cacheManager.getMostPopularShops(100)
        }, 0L, 20L * 60L)
    }

    private fun formatMaterialName(material: Material): String {
        val name = material.name.lowercase().replace('_', ' ')
        return name.split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Ajoute une bordure décorative avec des vitres
     */
    private fun addDecorativeBorder(inventory: Inventory) {
        val glassPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        glassPane.editMeta {
            it.displayName(Component.text(" "))
        }
        
        // Ligne du haut (slots 45-53)
        for (i in 45..53) {
            inventory.setItem(i, glassPane)
        }
    }

    override fun getInventory(): Inventory {
        return Bukkit.createInventory(this, 54, 
            Component.text("Catalogue").color(NamedTextColor.DARK_GRAY))
    }

    private class MenuSession(
        val player: Player,
        val allShops: List<Shop>
    ) {
        var currentPage = 0
        var detailPage = 0
        private var inDetailView = false
        private var currentDetailList = emptyList<Shop>()
        var selectedMaterialName: String? = null

        fun groupShopsByMaterial(): Map<Material, List<Shop>> {
            return allShops.groupBy { it.item.material }
        }

        fun isInMainView() = !inDetailView
        fun isInDetailView() = inDetailView

        fun setDetailView(list: List<Shop>, materialName: String) {
            this.inDetailView = true
            this.currentDetailList = list
            this.detailPage = 0
            this.selectedMaterialName = materialName
        }

        fun setMainView() {
            this.inDetailView = false
            this.currentDetailList = emptyList()
            this.selectedMaterialName = null
        }

        fun getCurrentDetailList() = currentDetailList

        fun getDetailShopAtSlot(slot: Int): Shop? {
            val index = detailPage * 45 + slot
            return currentDetailList.getOrNull(index)
        }

    }
}

