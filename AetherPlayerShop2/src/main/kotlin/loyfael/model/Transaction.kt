package loyfael.model

import java.time.Instant
import java.util.*

/**
 * Modèle représentant une transaction
 */
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val shopId: String,
    val buyerUUID: UUID,
    val sellerUUID: UUID,
    val type: TransactionType,
    val item: ShopItem,
    val price: Double,
    val quantity: Int,
    val date: Instant = Instant.now()
) {
    constructor(
        shopId: String,
        buyerUUID: UUID,
        sellerUUID: UUID,
        type: TransactionType,
        item: ShopItem,
        price: Double,
        quantity: Int
    ) : this(
        id = UUID.randomUUID().toString(),
        shopId = shopId,
        buyerUUID = buyerUUID,
        sellerUUID = sellerUUID,
        type = type,
        item = item,
        price = price,
        quantity = quantity
    )

    /**
     * Types de transaction
     */
    enum class TransactionType(val displayName: String) {
        BUY("Achat"),
        SELL("Vente")
    }
}
