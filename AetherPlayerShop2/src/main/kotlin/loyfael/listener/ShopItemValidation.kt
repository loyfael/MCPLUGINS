package loyfael.listener

import loyfael.model.ShopItem
import org.bukkit.block.Chest
import org.bukkit.inventory.ItemStack

/**
 * Classe de validation du contenu d'un coffre pour la création de shop
 */
data class ShopItemValidation(
    val valid: Boolean,
    val errorMessage: String,
    val shopItem: ShopItem?,
    val totalQuantity: Int
) {
    companion object {
        /**
         * Valide le contenu d'un coffre selon les règles
         */
        fun validate(chest: Chest): ShopItemValidation {
            val contents = chest.inventory.contents

            // Vérifier que le coffre n'est pas vide
            val hasItems = contents.any { it != null && it.amount > 0 }

            if (!hasItems) {
                return ShopItemValidation(
                    false,
                    "Le coffre doit contenir des items pour créer un shop.",
                    null,
                    0
                )
            }

            // Trouver le premier item non-null comme référence
            val referenceItem = contents.firstOrNull { it != null && it.amount > 0 }
                ?: return ShopItemValidation(
                    false,
                    "Erreur lors de la lecture du coffre.",
                    null,
                    0
                )

            // Vérifier que TOUS les items sont identiques
            var totalQuantity = 0
            for (item in contents) {
                if (item != null && item.amount > 0) {
                    if (!item.isSimilar(referenceItem)) {
                        return ShopItemValidation(
                            false,
                            "Le coffre doit contenir UN SEUL type d'item. Retirez les autres items différents.",
                            null,
                            0
                        )
                    }
                    totalQuantity += item.amount
                }
            }

            // Créer le ShopItem à partir de l'item de référence
            val shopItem = ShopItem(referenceItem)

            return ShopItemValidation(true, "", shopItem, totalQuantity)
        }
    }
}
