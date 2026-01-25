package loyfael.model

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.Damageable

/**
 * Modèle représentant un item dans un shop avec ses métadonnées
 */
class ShopItem(itemStack: ItemStack) {

    private val referenceItem: ItemStack = itemStack.clone().apply { amount = 1 }

    val material: Material = referenceItem.type
    val amount: Int = itemStack.amount.coerceAtLeast(1)
    val customNameComponent: Component?
    val customName: String?
    val lore: List<String>?
    val enchantments: Map<Enchantment, Int>
    val customModelData: Int
    private val strictMatching: Boolean

    init {
        val meta = referenceItem.itemMeta
        if (meta != null) {
            customNameComponent = if (meta.hasDisplayName()) meta.displayName() else null
            customName = customNameComponent?.let { PlainTextComponentSerializer.plainText().serialize(it) }

            lore = if (meta.hasLore()) {
                meta.lore()?.map { component ->
                    PlainTextComponentSerializer.plainText().serialize(component)
                }
            } else null

            enchantments = meta.enchants.toMap()
            customModelData = getCustomModelDataSafe(meta)
            strictMatching = hasSignificantMetadata(meta)
        } else {
            customNameComponent = null
            customName = null
            lore = null
            enchantments = emptyMap()
            customModelData = 0
            strictMatching = false
        }
    }

    /**
     * Crée un ItemStack à partir de ce ShopItem
     */
    fun toItemStack(): ItemStack = referenceItem.clone().apply {
        amount = this@ShopItem.amount
    }

    /**
     * Crée un ItemStack de démonstration pour l'affichage en GUI
     */
    fun toDisplayItemStack(displayAmount: Int): ItemStack {
        return referenceItem.clone().apply {
            amount = minOf(displayAmount, maxStackSize)
        }
    }

    /**
     * Vérifie si cet item correspond exactement à un ItemStack donné
     */
    fun matches(other: ItemStack): Boolean {
        return if (strictMatching) {
            referenceItem.isSimilar(other)
        } else {
            other.type == material
        }
    }

    /**
     * Propriété pour le nom d'affichage de l'item
     */
    val displayName: String
        get() {
            if (!customName.isNullOrEmpty()) {
                return customName
            }

            // Formatage du nom du matériau
            val materialName = material.name.lowercase().replace('_', ' ')
            return materialName.replaceFirstChar { it.uppercase() }
        }

    fun displayComponent(): Component {
        return customNameComponent ?: Component.text(displayName)
    }

    val requiresExactMatch: Boolean
        get() = strictMatching

    /**
     * Méthodes utilitaires pour encapsuler les API dépréciées
     */
    companion object {
        @Suppress("DEPRECATION")
        private fun getCustomModelDataSafe(meta: ItemMeta): Int {
            return try {
                if (meta.hasCustomModelData()) meta.customModelData else 0
            } catch (e: Exception) {
                0
            }
        }

        private fun hasSignificantMetadata(meta: ItemMeta): Boolean {
            if (meta.hasDisplayName() || meta.hasLore() || meta.hasEnchants() || meta.isUnbreakable || meta.hasAttributeModifiers()) {
                return true
            }

            if (meta is Damageable && meta.damage > 0) {
                return true
            }

            if (getCustomModelDataSafe(meta) > 0) {
                return true
            }

            return meta.persistentDataContainer.keys.isNotEmpty()
        }
    }
}
