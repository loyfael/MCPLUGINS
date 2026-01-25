package loyfael.listener

import loyfael.model.Shop
import loyfael.model.ShopItem
import org.bukkit.Location
import java.util.*

/**
 * Configuration temporaire d'un shop en cours de création
 */
data class ShopConfiguration(
    val playerUUID: UUID,
    var signLocation: Location?,
    val chestLocation: Location,
    val shopItem: ShopItem,
    val totalQuantity: Int,
    var shopType: Shop.ShopType = Shop.ShopType.SELL,
    var teleportPolicy: Shop.TeleportPolicy = Shop.TeleportPolicy.ALLOW_TP
) {
    var price: Double = 1.0
        set(value) {
            field = maxOf(0.01, value) // Min 1 centime
        }

    fun adjustPrice(change: Double) {
        price = price + change
    }
}
