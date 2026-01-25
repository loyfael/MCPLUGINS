package loyfael.util

import loyfael.model.Shop
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Utilitaires pour les interactions avec les shops
 */
object ShopInteractionUtils {

    /**
     * Vérifie si une pancarte est une pancarte de shop
     */
    fun isShopSign(lines: Array<String>): Boolean {
        if (lines.isEmpty()) return false
        val firstLine = lines[0].lowercase()
        return firstLine.contains("[achat]") || firstLine.contains("[vente]") ||
                firstLine.contains("[buy]") || firstLine.contains("[sell]") ||
                firstLine.contains("[shop]")
    }

    /**
     * Trouve une pancarte de shop adjacente à un coffre
     */
    fun findAdjacentShopSign(chestBlock: Block): Block? {
        // Recherche dans les 6 directions adjacentes
        val adjacents = arrayOf(
            chestBlock.getRelative(1, 0, 0), chestBlock.getRelative(-1, 0, 0),
            chestBlock.getRelative(0, 1, 0), chestBlock.getRelative(0, -1, 0),
            chestBlock.getRelative(0, 0, 1), chestBlock.getRelative(0, 0, -1)
        )

        for (adjacent in adjacents) {
            if (adjacent.state is Sign) {
                val sign = adjacent.state as Sign
                if (isShopSign(getSignLines(sign))) {
                    return adjacent
                }
            }
        }

        return null
    }

    /**
     * Trouve un coffre adjacent à une pancarte de shop
     */
    fun findAdjacentChest(signBlock: Block): Block? {
        // Recherche dans les 6 directions adjacentes
        val adjacents = arrayOf(
            signBlock.getRelative(1, 0, 0), signBlock.getRelative(-1, 0, 0),
            signBlock.getRelative(0, 1, 0), signBlock.getRelative(0, -1, 0),
            signBlock.getRelative(0, 0, 1), signBlock.getRelative(0, 0, -1)
        )

        for (adjacent in adjacents) {
            if (adjacent.type.name.contains("CHEST")) {
                return adjacent
            }
        }

        return null
    }

    /**
     * Obtient les lignes d'une pancarte en tant que tableau de String
     */
    fun getSignLines(sign: Sign): Array<String> {
        val lines = Array(4) { "" }
        for (i in 0..3) {
            val line = sign.getSide(org.bukkit.block.sign.Side.FRONT).line(i)
            lines[i] = PlainTextComponentSerializer.plainText().serialize(line)
        }
        return lines
    }

    /**
     * Met à jour l'affichage d'une pancarte de shop selon le stock disponible
     */
    fun updateShopSignDisplay(shop: Shop, signBlock: Block, chestInventory: Inventory?) {
        if (signBlock.state !is Sign) return
        val sign = signBlock.state as Sign

        // Vérifier si le shop est inactif
        if (!shop.active) {
            updateSignToInactive(sign, shop)
            sign.update(true)
            return
        }

        // Calculer le stock réel dans le coffre si disponible
        val actualStock = chestInventory?.let {
            countItemsInInventory(it, shop.item.toItemStack())
        } ?: shop.stock

        // Déterminer l'affichage selon le stock
        if (actualStock <= 0) {
            // Pas de stock - afficher une croix rouge
            updateSignToOutOfStock(sign, shop)
        } else {
            // Stock disponible - afficher normalement
            updateSignToInStock(sign, shop, actualStock)
        }

        // Appliquer les changements
        sign.update(true)
    }

    /**
     * Met à jour la pancarte pour indiquer que le shop est inactif
     */
    private fun updateSignToInactive(sign: Sign, shop: Shop) {
        val frontSide = sign.getSide(org.bukkit.block.sign.Side.FRONT)

        frontSide.line(0, Component.text("[INACTIF]", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
        frontSide.line(1, Component.text("⏸ Désactivé", NamedTextColor.GRAY, TextDecoration.BOLD))
        
        // Afficher le nom de l'item (custom ou matériau)
        val itemDisplay = shop.item.customName?.takeIf { it.isNotEmpty() }
            ?: formatMaterialName(shop.item.material)
        frontSide.line(2, Component.text(itemDisplay, NamedTextColor.DARK_GRAY))
        
        frontSide.line(3, Component.text("Clic droit = Réactiver", NamedTextColor.GRAY))
    }

    /**
     * Met à jour la pancarte pour indiquer "rupture de stock"
     */
    private fun updateSignToOutOfStock(sign: Sign, shop: Shop) {
        val frontSide = sign.getSide(org.bukkit.block.sign.Side.FRONT)

        // Ligne 1: Type de shop avec indication de rupture
        val shopType = if (shop.type == Shop.ShopType.SELL) "[VENTE]" else "[ACHAT]"
        frontSide.line(0, Component.text(shopType, NamedTextColor.DARK_RED, TextDecoration.BOLD))

        // Ligne 2: Croix rouge + "RUPTURE"
        frontSide.line(1, Component.text("✗ RUPTURE", NamedTextColor.RED, TextDecoration.BOLD))

        // Ligne 3: Nom de l'item - UNIQUEMENT le nom custom si disponible
        val itemDisplay = shop.item.customName?.takeIf { it.isNotEmpty() }
            ?: formatMaterialName(shop.item.material)
        frontSide.line(2, Component.text(itemDisplay, NamedTextColor.GRAY))

        // Ligne 4: Propriétaire
        frontSide.line(3, Component.text(shop.ownerName, NamedTextColor.DARK_GRAY))
    }

    /**
     * Met à jour la pancarte pour l'affichage normal avec stock
     */
    private fun updateSignToInStock(sign: Sign, shop: Shop, stock: Int) {
        val frontSide = sign.getSide(org.bukkit.block.sign.Side.FRONT)

        // Ligne 1: Type de shop
        val shopType = if (shop.type == Shop.ShopType.SELL) "[VENTE]" else "[ACHAT]"
        val typeColor = if (shop.type == Shop.ShopType.SELL) NamedTextColor.GREEN else NamedTextColor.BLUE
        frontSide.line(0, Component.text(shopType, typeColor, TextDecoration.BOLD))

        // Ligne 2: Prix
        val priceText = String.format("%.2f◎", shop.price)
        frontSide.line(1, Component.text(priceText, NamedTextColor.GOLD, TextDecoration.BOLD))

        // Ligne 3: Nom de l'item - UNIQUEMENT le nom custom si disponible
        val itemDisplay = shop.item.customName?.takeIf { it.isNotEmpty() }
            ?: formatMaterialName(shop.item.material)
        frontSide.line(2, Component.text(itemDisplay, NamedTextColor.WHITE))

        // Ligne 4: Stock
        val stockInfo = "Stock: $stock"
        frontSide.line(3, Component.text(stockInfo, NamedTextColor.YELLOW))
    }

    /**
     * Formate le nom d'un matériau de manière lisible
     */
    private fun formatMaterialName(material: Material): String {
        return material.name.lowercase()
            .replace('_', ' ')
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    /**
     * Compte les items d'un type donné dans un inventaire
     */
    /**
     * Compte les items d'un type donné dans un inventaire
     * Utilise une comparaison avancée pour supporter les items customisés
     */
    fun countItemsInInventory(inventory: Inventory, targetItem: ItemStack): Int {
        var count = 0
        val shopItem = loyfael.model.ShopItem(targetItem)
        
        for (item in inventory.contents) {
            if (item != null && item.type != Material.AIR) {
                // Utiliser la méthode matches de ShopItem pour une comparaison précise
                if (shopItem.matches(item)) {
                    count += item.amount
                }
            }
        }
        return count
    }

    /**
     * Vérifie si un joueur peut effectuer une transaction sur un shop
     */
    fun canPerformTransaction(player: Player, shop: Shop, isBuyAction: Boolean): Boolean {
        // Vérifier si c'est le propriétaire
        if (shop.ownerUUID == player.uniqueId) {
            return false
        }

        // Vérifier si le shop est actif
        if (!shop.active) {
            player.sendMessage("§cCe shop est actuellement inactif.")
            return false
        }

        // Vérifier le type d'action par rapport au type de shop
        if (shop.type == Shop.ShopType.SELL && !isBuyAction) {
            player.sendMessage("§cCeci est un shop de vente. Faites clic gauche pour acheter.")
            return false
        }

        return true
    }

    /**
     * Vérifie si le joueur a assez d'espace dans son inventaire
     */
    fun hasInventorySpace(player: Player, item: ItemStack): Boolean {
        var emptySlots = 0
        var partialStacks = 0

        for (slot in player.inventory.contents) {
            if (slot == null || slot.type == Material.AIR) {
                emptySlots++
            } else if (slot.isSimilar(item) && slot.amount < slot.maxStackSize) {
                partialStacks += slot.maxStackSize - slot.amount
            }
        }

        // Calculer si on peut stocker au moins un item
        return emptySlots > 0 || partialStacks > 0
    }

    /**
     * Crée un item stack représentant un shop sans stock
     */
    fun createOutOfStockDisplayItem(shop: Shop): ItemStack {
        val outOfStockItem = ItemStack(Material.BARRIER)
        val meta = outOfStockItem.itemMeta
        if (meta != null) {
            meta.displayName(Component.text("✗ RUPTURE DE STOCK", NamedTextColor.RED, TextDecoration.BOLD))
            meta.lore(
                listOf(
                    Component.empty(),
                    Component.text("Item: ${shop.item.displayName}", NamedTextColor.GRAY),
                    Component.text("Prix: ${String.format("%.2f", shop.price)}◎", NamedTextColor.GRAY),
                    Component.text("Propriétaire: ${shop.ownerName}", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Ce shop n'a plus de stock disponible", NamedTextColor.RED),
                    Component.text("Revenez plus tard !", NamedTextColor.YELLOW)
                )
            )
            outOfStockItem.itemMeta = meta
        }
        return outOfStockItem
    }
}
