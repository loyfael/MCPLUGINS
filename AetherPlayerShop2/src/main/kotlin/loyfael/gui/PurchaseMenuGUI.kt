package loyfael.gui

import loyfael.Main
import loyfael.model.Shop
import loyfael.model.Transaction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * Menu d'achat SÉCURISÉ avec Bukkit Inventory classique
 * Tous les clics sont bloqués sauf les boutons fonctionnels
 */
class PurchaseMenuGUI(private val plugin: Main) : Listener {

    private val purchaseSessions = mutableMapOf<UUID, PurchaseSession>()
    
    // Slots des boutons cliquables (tous les autres sont bloqués)
    private val CLICKABLE_SLOTS = setOf(11, 12, 13, 14, 15, 30, 31, 32)

    fun openPurchaseMenu(player: Player, shop: Shop, chestInventory: Inventory) {
        val actualStock = countItemsInInventory(chestInventory, shop.item.toItemStack())
        if (actualStock == 0) {
            player.sendMessage("§c❌ Cette boutique n'a plus de stock !")
            return
        }

        val session = PurchaseSession(shop, chestInventory, actualStock, 1)
        purchaseSessions[player.uniqueId] = session
        
        val menu = Bukkit.createInventory(null, 45, Component.text("Achat - ${shop.item.displayName}").color(NamedTextColor.DARK_GRAY))
        setupPurchaseMenu(menu, player, session)
        player.openInventory(menu)
    }

    private fun setupPurchaseMenu(menu: Inventory, player: Player, session: PurchaseSession) {
        val shop = session.shop
        val unitPrice = shop.price
        val playerBalance = plugin.economy.getBalance(player)
        val currentQuantity = session.selectedQuantity
        val totalPrice = unitPrice * currentQuantity
        val canAfford = playerBalance >= totalPrice
        val hasStock = currentQuantity <= session.availableStock

        // Vider le menu
        menu.clear()

        decorateBackground(menu)

        val shopLocation = shop.getLocation()
        val headerLore = mutableListOf<Component>()
        headerLore.add(Component.empty())
        headerLore.add(plainLine("Propriétaire: ${shop.ownerName}", NamedTextColor.GRAY))
        headerLore.add(plainLine("Type: ${shop.type.displayName}", NamedTextColor.GRAY))
        if (shopLocation != null) {
            headerLore.add(plainLine("Monde: ${shopLocation.world?.name ?: shop.world}", NamedTextColor.DARK_AQUA))
            headerLore.add(plainLine("Coordonnées: ${shopLocation.blockX}, ${shopLocation.blockY}, ${shopLocation.blockZ}", NamedTextColor.DARK_AQUA))
        } else {
            headerLore.add(plainLine("Monde: ${shop.world}", NamedTextColor.DARK_AQUA))
        }
        headerLore.add(Component.empty())
        headerLore.add(plainLine("Ajustez la quantité puis confirmez votre achat.", NamedTextColor.WHITE))
        menu.setItem(4, createPanelItem(
            Material.NETHER_STAR,
            boldTitle("Boutique • ${shop.item.displayName}", NamedTextColor.AQUA),
            headerLore,
            glow = true
        ))

        // === LIGNE 2: CONTRÔLES DE QUANTITÉ ===
        createQuantityButton(menu, 11, Material.REDSTONE_BLOCK, "◀◀ -10", currentQuantity >= 10, "Retirer 10 articles", NamedTextColor.RED)
        createQuantityButton(menu, 12, Material.REDSTONE, "◀ -1", currentQuantity > 1, "Retirer 1 article", NamedTextColor.RED)

        val resetBtn = ItemStack(Material.RECOVERY_COMPASS)
        resetBtn.editMeta {
            it.displayName(boldTitle("↺ Réinitialiser", NamedTextColor.YELLOW))
            it.lore(listOf(
                Component.empty(),
                plainLine("Ramène la quantité à 1.", NamedTextColor.WHITE),
                plainLine("Idéal pour repartir de zéro.", NamedTextColor.GRAY),
                Component.empty(),
                plainLine("Cliquez pour réinitialiser.", NamedTextColor.YELLOW)
            ))
            it.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        menu.setItem(13, resetBtn)

        createQuantityButton(menu, 14, Material.EMERALD, "+1 ▶", currentQuantity < session.availableStock, "Ajouter 1 article", NamedTextColor.GREEN)
        createQuantityButton(menu, 15, Material.EMERALD_BLOCK, "+10 ▶▶", currentQuantity + 10 <= session.availableStock, "Ajouter 10 articles", NamedTextColor.GREEN)

        // === LIGNE 3: INFORMATIONS PRINCIPALES ===
        val moneyLore = mutableListOf<Component>()
        moneyLore.add(Component.empty())
        moneyLore.add(plainLine("Solde: ${String.format("%.2f", playerBalance)} $", NamedTextColor.YELLOW))
        if (canAfford) {
            moneyLore.add(plainLine("Solde suffisant", NamedTextColor.GREEN))
            moneyLore.add(plainLine("Reste après achat: ${String.format("%.2f", playerBalance - totalPrice)} $", NamedTextColor.GREEN))
        } else {
            moneyLore.add(plainLine("Argent insuffisant", NamedTextColor.RED))
            moneyLore.add(plainLine("Manque: ${String.format("%.2f", totalPrice - playerBalance)} $", NamedTextColor.RED))
        }
        menu.setItem(20, createPanelItem(
            Material.GOLD_BLOCK,
            boldTitle("Votre solde", NamedTextColor.GOLD),
            moneyLore,
            glow = canAfford
        ))

        // ARTICLE PRINCIPAL (slot 22)
        val mainItem = shop.item.toItemStack()
        mainItem.amount = minOf(currentQuantity, 64)
        mainItem.editMeta {
            it.displayName(boldTitle("🎯 ${shop.item.displayName}", NamedTextColor.YELLOW))
            val mainLore = mutableListOf<Component>()
            mainLore.add(Component.empty())
            mainLore.add(plainLine("Quantité: $currentQuantity", NamedTextColor.AQUA))
            mainLore.add(plainLine("Prix unitaire: ${String.format("%.2f", unitPrice)} $", NamedTextColor.WHITE))
            mainLore.add(plainLine("Total: ${String.format("%.2f", totalPrice)} $", if (canAfford) NamedTextColor.GREEN else NamedTextColor.RED))
            mainLore.add(Component.empty())
            when {
                !canAfford -> mainLore.add(plainLine("Argent insuffisant", NamedTextColor.RED))
                !hasStock -> mainLore.add(plainLine("Stock insuffisant", NamedTextColor.RED))
                else -> mainLore.add(plainLine("Prêt à être acheté", NamedTextColor.GREEN))
            }
            it.lore(mainLore)
            it.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            if (canAfford && hasStock) {
                it.addEnchant(Enchantment.UNBREAKING, 1, true)
                it.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
        }
        menu.setItem(22, mainItem)

        // Informations sur le stock (slot 24)
        val stockMaterial = when {
            session.availableStock > 10 -> Material.CHEST
            session.availableStock > 0 -> Material.ENDER_CHEST
            else -> Material.BARRIER
        }
        val stockLore = mutableListOf<Component>()
        stockLore.add(Component.empty())
        stockLore.add(plainLine("Disponible: ${session.availableStock}", NamedTextColor.AQUA))
        val remainingAfterPurchase = session.availableStock - currentQuantity
        if (remainingAfterPurchase >= 0) {
            stockLore.add(plainLine("Après achat: ${maxOf(remainingAfterPurchase, 0)}", NamedTextColor.WHITE))
        }
        stockLore.add(plainLine("Propriétaire: ${shop.ownerName}", NamedTextColor.GREEN))
        menu.setItem(24, createPanelItem(
            stockMaterial,
            boldTitle("Stock disponible", NamedTextColor.BLUE),
            stockLore
        ))

        val statusMaterial: Material
        val statusTitle: String
        val statusColor: NamedTextColor
        when {
            !hasStock -> {
                statusMaterial = Material.REDSTONE_LAMP
                statusTitle = "Stock insuffisant"
                statusColor = NamedTextColor.RED
            }
            !canAfford -> {
                statusMaterial = Material.REDSTONE_LAMP
                statusTitle = "Solde insuffisant"
                statusColor = NamedTextColor.RED
            }
            else -> {
                statusMaterial = Material.SEA_LANTERN
                statusTitle = "Prêt à confirmer"
                statusColor = NamedTextColor.GREEN
            }
        }

        val statusLore = mutableListOf<Component>()
        statusLore.add(Component.empty())
        statusLore.add(plainLine("Quantité choisie: $currentQuantity", NamedTextColor.AQUA))
        statusLore.add(plainLine("Total: ${String.format("%.2f", totalPrice)} $", NamedTextColor.YELLOW))
        if (canAfford) {
            statusLore.add(plainLine("Reste après achat: ${String.format("%.2f", playerBalance - totalPrice)} $", NamedTextColor.GREEN))
        } else {
            statusLore.add(plainLine("Manque: ${String.format("%.2f", totalPrice - playerBalance)} $", NamedTextColor.RED))
        }
        if (!hasStock) {
            statusLore.add(plainLine("Stock disponible: ${session.availableStock}", NamedTextColor.RED))
        }
        menu.setItem(29, createPanelItem(
            statusMaterial,
            boldTitle(statusTitle, statusColor),
            statusLore,
            glow = canAfford && hasStock
        ))

        val tipsLore = listOf(
            Component.empty(),
            plainLine("Utilisez MAX pour remplir automatiquement.", NamedTextColor.GRAY),
            plainLine("Les boutons ajustent instantanément la commande.", NamedTextColor.GRAY),
            plainLine("Vous pouvez fermer à tout moment avec Annuler.", NamedTextColor.GRAY)
        )
        menu.setItem(33, createPanelItem(
            Material.BOOK,
            boldTitle("Conseils rapides", NamedTextColor.LIGHT_PURPLE),
            tipsLore
        ))

        // === LIGNE 4: ACTIONS FINALES ===
        val cancelBtn = ItemStack(Material.BARRIER)
        cancelBtn.editMeta {
            it.displayName(boldTitle("Annuler", NamedTextColor.RED))
            it.lore(listOf(
                Component.empty(),
                plainLine("Fermer sans finaliser l'achat.", NamedTextColor.WHITE),
                plainLine("Votre sélection n'est pas conservée.", NamedTextColor.GRAY)
            ))
            it.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        menu.setItem(30, cancelBtn)

        // ACHETER (slot 31)
        val confirmMaterial = if (canAfford && hasStock) Material.EMERALD_BLOCK else Material.REDSTONE_BLOCK
        val confirmBtn = ItemStack(confirmMaterial)
        confirmBtn.editMeta {
            when {
                canAfford && hasStock -> {
                    it.displayName(boldTitle("Confirmer l'achat", NamedTextColor.GREEN))
                    it.lore(listOf(
                        Component.empty(),
                        plainLine("• ${currentQuantity}× ${shop.item.displayName}", NamedTextColor.WHITE),
                        plainLine("• Total: ${String.format("%.2f", totalPrice)} $", NamedTextColor.YELLOW),
                        Component.empty(),
                        plainLine("Cliquez pour valider.", NamedTextColor.GREEN)
                    ))
                    it.addEnchant(Enchantment.UNBREAKING, 1, true)
                    it.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                }
                !hasStock -> {
                    it.displayName(boldTitle("Stock insuffisant", NamedTextColor.RED))
                    it.lore(listOf(
                        Component.empty(),
                        plainLine("Réduisez la quantité demandée.", NamedTextColor.WHITE),
                        plainLine("Stock: ${session.availableStock} / Demandé: $currentQuantity", NamedTextColor.RED)
                    ))
                }
                else -> {
                    it.displayName(boldTitle("Argent insuffisant", NamedTextColor.RED))
                    it.lore(listOf(
                        Component.empty(),
                        plainLine("Vous avez besoin de plus de fonds.", NamedTextColor.WHITE),
                        plainLine("Manque: ${String.format("%.2f", totalPrice - playerBalance)} $", NamedTextColor.RED)
                    ))
                }
            }
            it.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        menu.setItem(31, confirmBtn)

        // MAX (slot 32)
        val maxBtn = ItemStack(Material.PURPLE_CONCRETE)
        maxBtn.editMeta {
            val maxAffordable = (playerBalance / unitPrice).toInt()
            val actualMax = minOf(maxAffordable, session.availableStock)
            it.displayName(boldTitle("Quantité MAX", NamedTextColor.LIGHT_PURPLE))
            it.lore(listOf(
                Component.empty(),
                plainLine("Disponible: $actualMax articles", NamedTextColor.YELLOW),
                plainLine("Coût estimé: ${String.format("%.2f", actualMax * unitPrice)} $", NamedTextColor.YELLOW),
                Component.empty(),
                plainLine("Cliquez pour utiliser le maximum.", NamedTextColor.LIGHT_PURPLE)
            ))
            it.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        menu.setItem(32, maxBtn)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // Vérifier si c'est notre menu
        if (event.view.topInventory.type != InventoryType.CHEST) return
        val title = event.view.title()
        if (!title.toString().contains("Achat")) return

        // SÉCURITÉ: Bloquer TOUS les clics par défaut
        event.isCancelled = true

        val session = purchaseSessions[player.uniqueId]
        if (session == null || session.isExpired()) {
            player.sendMessage("§c⏰ Session d'achat expirée. Cliquez à nouveau sur le panneau pour recommencer !")
            player.closeInventory()
            return
        }

        // Ne traiter que les clics dans le menu du haut
        if (event.clickedInventory != event.view.topInventory) return

        val clickedItem = event.currentItem ?: return
        val slot = event.slot

        // SÉCURITÉ: Autoriser uniquement les slots des boutons fonctionnels
        if (slot !in CLICKABLE_SLOTS) return

        when (slot) {
            11 -> if (clickedItem.type == Material.REDSTONE_BLOCK) {
                session.adjustQuantity(-10)
                setupPurchaseMenu(event.inventory, player, session)
                player.sendMessage("§e📉 -10 articles (Total: ${session.selectedQuantity})")
            }
            12 -> if (clickedItem.type == Material.REDSTONE) {
                session.adjustQuantity(-1)
                setupPurchaseMenu(event.inventory, player, session)
                player.sendMessage("§e📉 -1 article (Total: ${session.selectedQuantity})")
            }
            13 -> if (clickedItem.type == Material.RECOVERY_COMPASS) {
                session.selectedQuantity = 1
                setupPurchaseMenu(event.inventory, player, session)
                player.sendMessage("§e↺ Quantité remise à 1")
            }
            14 -> if (clickedItem.type == Material.EMERALD) {
                session.adjustQuantity(1)
                setupPurchaseMenu(event.inventory, player, session)
                player.sendMessage("§e📈 +1 article (Total: ${session.selectedQuantity})")
            }
            15 -> if (clickedItem.type == Material.EMERALD_BLOCK) {
                session.adjustQuantity(10)
                setupPurchaseMenu(event.inventory, player, session)
                player.sendMessage("§e📈 +10 articles (Total: ${session.selectedQuantity})")
            }
            30 -> if (clickedItem.type == Material.BARRIER) {
                player.closeInventory()
                purchaseSessions.remove(player.uniqueId)
                player.sendMessage("§c❌ Achat annulé")
            }
            31 -> if (clickedItem.type == Material.EMERALD_BLOCK) {
                player.sendMessage("§a💳 Traitement de votre achat en cours...")
                executePurchase(player, session)
            }
            32 -> if (clickedItem.type == Material.PURPLE_CONCRETE) {
                val playerBalance = plugin.economy.getBalance(player)
                val unitPrice = session.shop.price
                val maxAffordable = (playerBalance / unitPrice).toInt()
                val actualMax = minOf(maxAffordable, session.availableStock)
                if (actualMax > 0) {
                    session.selectedQuantity = actualMax
                    setupPurchaseMenu(event.inventory, player, session)
                    player.sendMessage("§e📈 Quantité au MAXIMUM: $actualMax articles")
                    player.sendMessage("§7💰 Coût total: ${String.format("%.2f", actualMax * unitPrice)}$")
                } else {
                    player.sendMessage("§c❌ Impossible d'acheter même 1 article !")
                    player.sendMessage("§7💸 Vous avez besoin de ${String.format("%.2f", unitPrice)}$ minimum")
                }
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val title = event.view.title()
        if (title.toString().contains("Achat")) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val session = purchaseSessions[player.uniqueId]
                if (session != null && session.isExpired()) {
                    purchaseSessions.remove(player.uniqueId)
                }
            }, 60L)
        }
    }

    private fun createQuantityButton(
        menu: Inventory,
        slot: Int,
        material: Material,
        name: String,
        enabled: Boolean,
        description: String,
        highlightColor: NamedTextColor
    ) {
        val actualMaterial = if (enabled) material else Material.GRAY_CONCRETE
        val button = ItemStack(actualMaterial)
        button.editMeta {
            val color = if (enabled) highlightColor else NamedTextColor.DARK_GRAY
            it.displayName(Component.text(name, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
            val lore = mutableListOf<Component>()
            lore.add(Component.empty())
            if (enabled) {
                lore.add(plainLine(description, NamedTextColor.WHITE))
                lore.add(Component.empty())
                lore.add(plainLine("Cliquez pour ajuster.", NamedTextColor.YELLOW))
                it.addEnchant(Enchantment.UNBREAKING, 1, true)
                it.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            } else {
                lore.add(plainLine("Option indisponible.", NamedTextColor.DARK_GRAY))
            }
            it.lore(lore)
            it.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        menu.setItem(slot, button)
    }

    private fun createDecorativeGlass(material: Material, name: String): ItemStack {
        val glass = ItemStack(material)
        glass.editMeta {
            it.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false))
            it.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
        }
        return glass
    }

    private fun decorateBackground(menu: Inventory) {
        val softGlass = createDecorativeGlass(Material.GRAY_STAINED_GLASS_PANE, " ")
        val accentGlass = createDecorativeGlass(Material.CYAN_STAINED_GLASS_PANE, " ")
        val shadowGlass = createDecorativeGlass(Material.BLACK_STAINED_GLASS_PANE, " ")

        val topRow = 0..8
        val bottomRow = 36..44
        (topRow + bottomRow).forEach { menu.setItem(it, softGlass.clone()) }
        listOf(9, 17, 18, 26, 27, 35).forEach { menu.setItem(it, softGlass.clone()) }
        listOf(10, 16, 28, 34).forEach { menu.setItem(it, accentGlass.clone()) }
        listOf(19, 23, 29, 33).forEach { menu.setItem(it, shadowGlass.clone()) }
    }

    private fun boldTitle(text: String, color: NamedTextColor): Component =
        Component.text(text, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)

    private fun plainLine(text: String, color: NamedTextColor): Component =
        Component.text(text, color).decoration(TextDecoration.ITALIC, false)

    private fun createPanelItem(material: Material, title: Component, lore: List<Component>, glow: Boolean = false): ItemStack {
        val item = ItemStack(material)
        item.editMeta {
            it.displayName(title.decoration(TextDecoration.ITALIC, false))
            if (lore.isNotEmpty()) {
                it.lore(lore.map { line -> line.decoration(TextDecoration.ITALIC, false) })
            }
            it.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            if (glow) {
                it.addEnchant(Enchantment.UNBREAKING, 1, true)
                it.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
        }
        return item
    }

    private fun executePurchase(player: Player, session: PurchaseSession) {
        try {
            val shop = session.shop
            val chestInventory = session.chestInventory
            val quantity = session.selectedQuantity
            val totalPrice = shop.price * quantity

            if (plugin.economy.getBalance(player) < totalPrice) {
                player.sendMessage("§cVous n'avez pas assez d'argent pour cet achat !")
                return
            }

            val actualStock = countItemsInInventory(chestInventory, shop.item.toItemStack())
            if (quantity > actualStock) {
                player.sendMessage("§cStock insuffisant ! Stock disponible: $actualStock")
                return
            }

            if (!removeItemsFromInventory(chestInventory, shop.item.toItemStack(), quantity)) {
                player.sendMessage("§cErreur lors du retrait des items du stock !")
                return
            }

            val economyResponse = plugin.economy.withdrawPlayer(player, totalPrice)
            if (!economyResponse.transactionSuccess()) {
                player.sendMessage("§cErreur lors du paiement !")
                addItemsToInventory(chestInventory, shop.item.toItemStack(), quantity)
                return
            }

            val itemToGive = shop.item.toItemStack()
            giveItemsToPlayer(player, itemToGive, quantity)

            val shopOwner = Bukkit.getOfflinePlayer(shop.ownerUUID)
            if (plugin.economy.hasAccount(shopOwner)) {
                plugin.economy.depositPlayer(shopOwner, totalPrice)
            }

            val newStock = maxOf(0, countItemsInInventory(chestInventory, shop.item.toItemStack()))
            plugin.shopManager.updateShopStock(shop.id, newStock)

            val transaction = Transaction(shop.id, player.uniqueId, shop.ownerUUID, Transaction.TransactionType.BUY, shop.item, shop.price, quantity)
            plugin.shopManager.recordTransaction(transaction)

            player.sendMessage("§a§l✓ Achat effectué avec succès !")
            player.sendMessage("§7Quantité: §e$quantity")
            player.sendMessage("§7Prix total: §e${String.format("%.2f", totalPrice)}◎")
            player.sendMessage("§7Argent restant: §e${String.format("%.2f", plugin.economy.getBalance(player))}◎")

            player.closeInventory()
            purchaseSessions.remove(player.uniqueId)
        } catch (e: Exception) {
            plugin.logger.severe("Erreur lors de l'achat: ${e.message}")
            player.sendMessage("§cUne erreur est survenue lors de l'achat !")
        }
    }

    private fun countItemsInInventory(inventory: Inventory, targetItem: ItemStack): Int {
        val shopItem = loyfael.model.ShopItem(targetItem)
        return inventory.contents.filterNotNull()
            .filter { shopItem.matches(it) }
            .sumOf { it.amount }
    }

    private fun removeItemsFromInventory(inventory: Inventory, targetItem: ItemStack, quantity: Int): Boolean {
        val shopItem = loyfael.model.ShopItem(targetItem)
        var remaining = quantity
        for (i in 0 until inventory.size) {
            if (remaining <= 0) break
            val item = inventory.getItem(i)
            if (item != null && shopItem.matches(item)) {
                val available = item.amount
                if (available <= remaining) {
                    inventory.setItem(i, null)
                    remaining -= available
                } else {
                    item.amount = available - remaining
                    remaining = 0
                }
            }
        }
        return remaining == 0
    }

    private fun addItemsToInventory(inventory: Inventory, targetItem: ItemStack, quantity: Int) {
        var remaining = quantity
        val itemToAdd = targetItem.clone()

        for (i in 0 until inventory.size) {
            if (remaining <= 0) break
            val item = inventory.getItem(i)
            if (item != null && item.isSimilar(targetItem)) {
                val maxStack = item.maxStackSize
                val current = item.amount
                val canAdd = minOf(remaining, maxStack - current)
                if (canAdd > 0) {
                    item.amount = current + canAdd
                    remaining -= canAdd
                }
            }
        }

        while (remaining > 0) {
            val stackSize = minOf(remaining, itemToAdd.maxStackSize)
            itemToAdd.amount = stackSize
            inventory.addItem(itemToAdd.clone())
            remaining -= stackSize
        }
    }

    private fun giveItemsToPlayer(player: Player, item: ItemStack, quantity: Int) {
        var remaining = quantity
        val itemToGive = item.clone()

        while (remaining > 0) {
            val stackSize = minOf(remaining, item.maxStackSize)
            itemToGive.amount = stackSize
            val leftOver = player.inventory.addItem(itemToGive.clone())
            if (leftOver.isNotEmpty()) {
                leftOver.values.forEach { player.world.dropItemNaturally(player.location, it) }
                player.sendMessage("§6Inventaire plein ! Les items ont été jetés au sol.")
            }
            remaining -= stackSize
        }
    }

    data class PurchaseSession(
        val shop: Shop,
        val chestInventory: Inventory,
        val availableStock: Int,
        var selectedQuantity: Int,
        private val creationTime: Long = System.currentTimeMillis()
    ) {
        fun adjustQuantity(adjustment: Int) {
            selectedQuantity = maxOf(1, minOf(selectedQuantity + adjustment, availableStock))
        }

        fun isExpired(): Boolean = System.currentTimeMillis() - creationTime > 300000 // 5 minutes
    }
}
