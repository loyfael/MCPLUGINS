package loyfael.gui

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import dev.triumphteam.gui.guis.PaginatedGui
import loyfael.Main
import loyfael.manager.ShopManager
import loyfael.model.Shop
import loyfael.util.ShopInteractionUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * GUI principal modernisé avec TriumphGUI - Menu central global selon le cahier des charges
 */
class TriumphShopMenuGUI(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.")
            return true
        }

        if (!sender.hasPermission("aetherplayershop.menu")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.")
            return true
        }

        openMainShopMenu(sender)
        return true
    }

    fun openMainShopMenu(player: Player) {
        plugin.shopManager.searchShops(ShopManager.ShopSearchFilter())
            .thenAccept { shops ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    createMainMenu(player, shops)
                })
            }
    }

    private fun createMainMenu(player: Player, shops: List<Shop>) {
        val groupedShops = shops.filter { it.active }
            .groupBy { it.item.material }

        val gui = Gui.paginated()
            .title(Component.text("Catalogue")
                .color(NamedTextColor.DARK_GRAY))
            .rows(6)
            .pageSize(28)
            .create()

        for ((material, materialShops) in groupedShops) {
            val materialItem = createMaterialGuiItem(material, materialShops, player)
            gui.addItem(materialItem)
        }

        setupNavigationButtons(gui, player, groupedShops.size)
        setupActionButtons(gui, player)

        gui.open(player)
        playOpenSound(player)
    }

    private fun createMaterialGuiItem(material: Material, shops: List<Shop>, player: Player): GuiItem {
        val avgPrice = shops.map { it.price }.average()
        val cheapestShop = shops.minByOrNull { it.price }
        val totalStock = shops.sumOf { it.stock }

        val loreComponents = mutableListOf<Component>()
        loreComponents.add(Component.text("§7Shops disponibles: §e${shops.size}"))
        loreComponents.add(Component.text("§7Stock total: §e$totalStock"))
        loreComponents.add(Component.empty())

        if (avgPrice.isFinite()) {
            loreComponents.add(Component.text("§7Prix moyen: §e${String.format("%.2f", avgPrice)}§6◎"))
        }

        cheapestShop?.let { cheapest ->
            loreComponents.add(Component.text("§7Prix le plus bas: §e${String.format("%.2f", cheapest.price)}§6◎"))
            loreComponents.add(Component.text("§7Vendeur: §e${cheapest.ownerName}"))
        }

        loreComponents.add(Component.empty())
        loreComponents.add(Component.text("§a▶ Clic gauche: Acheter au meilleur prix", NamedTextColor.GREEN))
        loreComponents.add(Component.text("§b▶ Clic droit: Voir tous les shops", NamedTextColor.AQUA))
        loreComponents.add(Component.text("§e▶ Shift + Clic: Informations détaillées", NamedTextColor.YELLOW))

        @Suppress("UNCHECKED_CAST")
        val item = ItemBuilder.from(material)
            .name(Component.text(formatMaterialName(material))
                .color(NamedTextColor.WHITE))
            .lore(loreComponents as MutableList<Component?>)
            .build()

        return ItemBuilder.from(item)
            .asGuiItem { event ->
                event.isCancelled = true

                when {
                    event.isShiftClick -> showMaterialDetails(player, material, shops)
                    event.isRightClick -> openMaterialShops(player, material)
                    event.isLeftClick -> openBestShopForMaterial(player, material, shops)
                }

                playClickSound(player)
            }
    }

    private fun openMaterialShops(player: Player, material: Material) {
        val filter = ShopManager.ShopSearchFilter().apply {
            this.material = material
            sortBy = ShopManager.ShopSearchFilter.SortBy.PRICE_ASC
        }

        plugin.shopManager.searchShops(filter)
            .thenAccept { shops ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    createMaterialShopsMenu(player, material, shops)
                })
            }
    }

    private fun createMaterialShopsMenu(player: Player, material: Material, shops: List<Shop>) {
        val gui = Gui.paginated()
            .title(Component.text("Shops - ${formatMaterialName(material)}")
                .color(NamedTextColor.GOLD))
            .rows(6)
            .pageSize(28)
            .create()

        for (shop in shops) {
            if (!shop.active) continue
            val shopItem = createShopGuiItem(shop, player)
            gui.addItem(shopItem)
        }

        setupNavigationButtons(gui, player, shops.size)
        setupBackButton(gui, player)

        gui.open(player)
        playOpenSound(player)
    }

    private fun createShopGuiItem(shop: Shop, player: Player): GuiItem {
        val displayItem = shop.item.toDisplayItemStack(shop.item.amount)

        val shopLoc = shop.getLocation()!!
        val loreComponents: MutableList<Component?> = mutableListOf(
            Component.text("§7Type: §e${shop.type.displayName}"),
            Component.text("§7Prix unitaire: §e${String.format("%.2f", shop.price)}§6◎"),
            Component.text("§7Stock: §e${shop.stock}"),
            Component.text("§7Vendeur: §e${shop.ownerName}"),
            Component.empty(),
            Component.text("§7Localisation:"),
            Component.text("§8• Monde: §7${shopLoc.world.name}"),
            Component.text("§8• Position: §7${shopLoc.blockX}, ${shopLoc.blockY}, ${shopLoc.blockZ}"),
            Component.empty(),
            Component.text("§a▶ Clic gauche: Se téléporter au shop", NamedTextColor.GREEN),
            Component.text("§b▶ Clic droit: Plus d'informations", NamedTextColor.AQUA)
        )

        val item = ItemBuilder.from(displayItem)
            .name(Component.text(shop.item.displayName)
                .color(NamedTextColor.WHITE))
            .lore(loreComponents)
            .build()

        return ItemBuilder.from(item)
            .asGuiItem { event ->
                event.isCancelled = true

                if (event.isRightClick) {
                    showShopDetails(player, shop)
                } else if (event.isLeftClick) {
                    // Clic gauche = téléportation
                    handleShopTeleport(player, shop)
                }

                playClickSound(player)
            }
    }

    private fun handleShopTeleport(player: Player, shop: Shop) {
        if (!plugin.configManager.isTeleportEnabled()) {
            player.sendMessage("§8[§c✘§8] §cLa téléportation est désactivée.")
            return
        }

        val location = shop.getLocation()
        if (location == null) {
            player.sendMessage("§8[§c✘§8] §cImpossible de trouver la localisation du shop.")
            return
        }

        player.closeInventory()
        val delay = plugin.configManager.teleportDelay

        if (delay > 0) {
            player.sendMessage("§8[§6⏱§8] §eTéléportation au shop dans §6$delay§e secondes...")
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                teleportPlayer(player, location, shop)
            }, (delay * 20L))
        } else {
            teleportPlayer(player, location, shop)
        }
    }

    private fun teleportPlayer(player: Player, location: org.bukkit.Location, shop: Shop) {
        // Téléporter le joueur exactement à la position de la pancarte (au sol, pas au-dessus)
        val teleportLoc = location.clone().add(0.5, 0.0, 0.5)
        
        // Récupérer le bloc de la pancarte pour orienter le joueur
        val signBlock = location.block
        val signData = signBlock.blockData
        
        // Orienter le joueur vers la pancarte si c'est une pancarte murale
        if (signData is org.bukkit.block.data.type.WallSign) {
            val facing = signData.facing
            teleportLoc.yaw = when (facing) {
                org.bukkit.block.BlockFace.NORTH -> 180f
                org.bukkit.block.BlockFace.SOUTH -> 0f
                org.bukkit.block.BlockFace.WEST -> 90f
                org.bukkit.block.BlockFace.EAST -> -90f
                else -> 0f
            }
            teleportLoc.pitch = 0f
        }
        
        player.teleport(teleportLoc)

        player.sendMessage("§8[§6✦§8] §aVous avez été téléporté au shop de §e${shop.ownerName}§a !")

        if (plugin.configManager.isSoundEnabled()) {
            player.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
        }

        if (plugin.configManager.isParticleEnabled()) {
            player.spawnParticle(org.bukkit.Particle.PORTAL,
                player.location.add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5, 0.1)
        }
    }

    private fun setupNavigationButtons(gui: PaginatedGui, @Suppress("UNUSED_PARAMETER") player: Player, @Suppress("UNUSED_PARAMETER") totalItems: Int) {
        gui.setItem(5, 3, ItemBuilder.from(Material.ARROW)
            .name(Component.text("§a◀ Page précédente", NamedTextColor.GREEN))
            .asGuiItem { event ->
                event.isCancelled = true
                gui.previous()
                playClickSound(player)
            })

        gui.setItem(5, 4, ItemBuilder.from(Material.BOOK)
            .name(Component.text("§6Informations", NamedTextColor.GOLD))
            .lore(listOf(
                Component.text("§7Page actuelle: §e${gui.currentPageNum + 1}"),
                Component.text("§7Navigation: §eUtilisez les flèches")
            ))
            .asGuiItem { event -> event.isCancelled = true })

        gui.setItem(5, 5, ItemBuilder.from(Material.ARROW)
            .name(Component.text("§aPage suivante ▶", NamedTextColor.GREEN))
            .asGuiItem { event ->
                event.isCancelled = true
                gui.next()
                playClickSound(player)
            })
    }

    private fun setupActionButtons(gui: PaginatedGui, player: Player) {
        gui.setItem(5, 1, ItemBuilder.from(Material.COMPASS)
            .name(Component.text("§bActualiser", NamedTextColor.AQUA))
            .lore(Component.text("§7Cliquez pour actualiser la liste"))
            .asGuiItem { event ->
                event.isCancelled = true
                gui.close(player)
                openMainShopMenu(player)
            })

        gui.setItem(5, 7, ItemBuilder.from(Material.HOPPER)
            .name(Component.text("Filtres avancés")
                .color(NamedTextColor.WHITE))
            .lore(listOf(
                Component.text("§7Filtrer par:"),
                Component.text("§8• Prix"),
                Component.text("§8• Vendeur"),
                Component.text("§8• Type d'item")
            ))
            .asGuiItem { event ->
                event.isCancelled = true
                openAdvancedFilters(player)
            })
    }

    private fun setupBackButton(gui: PaginatedGui, player: Player) {
        gui.setItem(5, 1, ItemBuilder.from(Material.BARRIER)
            .name(Component.text("§c◀ Retour au menu principal", NamedTextColor.RED))
            .asGuiItem { event ->
                event.isCancelled = true
                gui.close(player)
                openMainShopMenu(player)
            })
    }

    private fun showMaterialDetails(player: Player, material: Material, shops: List<Shop>) {
        player.sendMessage("§6=== Détails pour ${formatMaterialName(material)} ===")
        player.sendMessage("§7Shops disponibles: §e${shops.size}")
        player.sendMessage("§7Stock total: §e${shops.sumOf { it.stock }}")

        val avgPrice = shops.map { it.price }.average()
        if (avgPrice.isFinite()) {
            player.sendMessage("§7Prix moyen: §e${String.format("%.2f", avgPrice)}§6◎")
        }
    }

    /**
     * Ouvre le menu d'achat pour le meilleur shop du matériau (prix le plus bas avec stock)
     * et téléporte le joueur selon ses réglages
     */
    private fun openBestShopForMaterial(player: Player, material: Material, shops: List<Shop>) {
        // Fermer le menu actuel
        player.closeInventory()
        
        // Filtrer les shops actifs avec du stock et trier par prix
        val availableShops = shops
            .filter { it.active && it.stock > 0 }
            .sortedBy { it.price }
        
        if (availableShops.isEmpty()) {
            player.sendMessage("§8[§c✘§8] §cAucun shop disponible avec du stock pour ${formatMaterialName(material)} !")
            return
        }
        
        // Prendre le meilleur shop (prix le plus bas)
        val bestShop = availableShops.first()
        
        // Vérifier le stock réel dans le coffre
        val location = bestShop.getLocation()
        if (location == null) {
            player.sendMessage("§cImpossible de trouver la localisation du shop.")
            return
        }
        
        val signBlock = location.block
        val chestBlock = ShopInteractionUtils.findAdjacentChest(signBlock)
        
        if (chestBlock == null || chestBlock.state !is org.bukkit.block.Chest) {
            // Message supprimé : la gestion se fait dans la GUI principale
            return
        }
        
        val chest = chestBlock.state as org.bukkit.block.Chest
        val chestInventory = chest.inventory
        val actualStock = ShopInteractionUtils.countItemsInInventory(chestInventory, bestShop.item.toItemStack())
        
        if (actualStock <= 0) {
            player.sendMessage("§8[§c✘§8] §cLe meilleur shop n'a plus de stock ! Essayez un autre shop.")
            return
        }
        
        // Ouvrir le menu d'achat
        plugin.purchaseMenuGUI.openPurchaseMenu(player, bestShop, chestInventory)
        
        // Téléporter le joueur selon les réglages (après un court délai pour que le menu s'ouvre)
        if (plugin.configManager.isTeleportEnabled()) {
            val delay = plugin.configManager.teleportDelay
            
            if (delay > 0) {
                player.sendMessage("§8[§6⏱§8] §eTéléportation au shop dans §6$delay§e secondes...")
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    teleportPlayer(player, location, bestShop)
                }, (delay * 20L))
            } else {
                // Téléportation immédiate après un court délai pour laisser le menu s'ouvrir
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    teleportPlayer(player, location, bestShop)
                }, 5L) // 0.25 secondes
            }
        }
    }

    private fun showShopDetails(player: Player, shop: Shop) {
        player.sendMessage("§6=== Détails du shop ===")
        player.sendMessage("§7Propriétaire: §e${shop.ownerName}")
        player.sendMessage("§7Type: §e${shop.type.displayName}")
        player.sendMessage("§7Item: §e${shop.item.displayName}")
        player.sendMessage("§7Prix: §e${String.format("%.2f", shop.price)}§6◎")
        player.sendMessage("§7Stock: §e${shop.stock}")
    }

    private fun openAdvancedFilters(player: Player) {
        player.sendMessage("§6Filtres avancés (à développer)")
    }

    private fun playOpenSound(player: Player) {
        if (plugin.configManager.isSoundEnabled()) {
            player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f)
        }
    }

    private fun playClickSound(player: Player) {
        if (plugin.configManager.isSoundEnabled()) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f)
        }
    }

    private fun formatMaterialName(material: Material): String {
        val name = material.name.lowercase().replace('_', ' ')
        return name.split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }
}

