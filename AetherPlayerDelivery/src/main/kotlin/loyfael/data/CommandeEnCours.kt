package loyfael.data

import org.bukkit.Material
import java.time.LocalDateTime
import java.util.*

/**
 * Commande temporaire en cours de création dans les GUIs
 */
data class CommandeEnCours(
    val clientUuid: UUID,
    var material: Material? = null,
    var quantity: Int = 1,
    var price: Double = 0.0,
    var deadline: LocalDateTime = LocalDateTime.now().plusDays(7),
    var step: EtapeCreation = EtapeCreation.SELECTION_ITEM
)

enum class EtapeCreation {
    SELECTION_ITEM,
    CONFIGURATION_QUANTITE,
    CONFIGURATION_PRIX,
    CONFIGURATION_DELAI,
    CONFIRMATION
}