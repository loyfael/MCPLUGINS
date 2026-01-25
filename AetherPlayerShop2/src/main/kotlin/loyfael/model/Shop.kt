package loyfael.model

import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.Instant
import java.util.*

/**
 * Modèle représentant un shop selon les spécifications
 */
data class Shop(
    val id: String,
    val ownerUUID: UUID,
    val ownerName: String,
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val type: ShopType,
    var item: ShopItem,
    var price: Double,
    var stock: Int,
    val createdAt: Instant = Instant.now(),
    var active: Boolean = true,
    var teleportPolicy: TeleportPolicy = TeleportPolicy.ALLOW_TP,
    var lastActivity: Instant = Instant.now()
) {

    constructor(
        id: String,
        ownerUUID: UUID,
        ownerName: String,
        location: Location,
        type: ShopType,
        item: ShopItem,
        price: Double,
        stock: Int
    ) : this(
        id = id,
        ownerUUID = ownerUUID,
        ownerName = ownerName,
        world = location.world.name,
        x = location.blockX,
        y = location.blockY,
        z = location.blockZ,
        type = type,
        item = item,
        price = price,
        stock = stock
    )

    /**
     * Obtient la location du shop
     */
    fun getLocation(): Location? {
        val world = Bukkit.getWorld(this.world) ?: return null
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }

    /**
     * Vérifie si le shop correspond à une location donnée
     */
    fun isAtLocation(location: Location): Boolean {
        return location.world.name == world &&
                location.blockX == x &&
                location.blockY == y &&
                location.blockZ == z
    }

    /**
     * Alias pour getLocation
     */
    fun getChestLocation(): Location? = getLocation()

    /**
     * Types de shop
     */
    enum class ShopType(val displayName: String) {
        SELL("Vente");
        // BUY("Achat") // Point d'entrée conservé pour usage futur

        companion object {
            /**
             * Obtient les types de shop disponibles
             */
            fun getAvailableTypes(): Array<ShopType> {
                return arrayOf(SELL) // Pour réactiver ACHAT : arrayOf(SELL, BUY)
            }
        }

        /**
         * Vérifie si ce type est disponible
         */
        fun isAvailable(): Boolean {
            return this == SELL // Pour réactiver ACHAT : this == SELL || this == BUY
        }
    }

    /**
     * Politiques de téléportation
     */
    enum class TeleportPolicy {
        ALLOW_TP,
        BLOCK_TP_AND_SUGGEST_DM
    }
}
