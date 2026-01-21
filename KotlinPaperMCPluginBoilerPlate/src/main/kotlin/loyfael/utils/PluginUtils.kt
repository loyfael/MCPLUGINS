package loyfael.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.round

/**
 * Classe utilitaire contenant des méthodes communes
 */
object PluginUtils {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    
    /**
     * Convertit un timestamp en string formaté
     */
    fun formatTimestamp(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Formate une durée en millisecondes en string lisible
     */
    fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}j ${hours % 24}h ${minutes % 60}m"
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
    
    /**
     * Convertit un message coloré en Component Adventure
     */
    fun parseMessage(message: String): Component {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message)
    }
    
    /**
     * Crée un Component avec une couleur spécifique
     */
    fun createColoredMessage(text: String, color: NamedTextColor, bold: Boolean = false): Component {
        var component = Component.text(text).color(color)
        if (bold) {
            component = component.decoration(TextDecoration.BOLD, true)
        }
        return component
    }
    
    /**
     * Crée un ItemStack avec nom et lore
     */
    fun createItemStack(
        material: Material,
        name: String,
        lore: List<String> = emptyList(),
        amount: Int = 1
    ): ItemStack {
        val item = ItemStack(material, amount)
        val meta: ItemMeta = item.itemMeta ?: return item
        
        // Nom
        if (name.isNotEmpty()) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name))
        }
        
        // Lore
        if (lore.isNotEmpty()) {
            val loreComponents = lore.map { 
                LegacyComponentSerializer.legacyAmpersand().deserialize(it) 
            }
            meta.lore(loreComponents)
        }
        
        item.itemMeta = meta
        return item
    }
    
    /**
     * Valide si une chaîne est un UUID valide
     */
    fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    
    /**
     * Capitalise la première lettre d'une chaîne
     */
    fun capitalize(input: String): String {
        return if (input.isEmpty()) input
        else input.substring(0, 1).uppercase() + input.substring(1).lowercase()
    }
    
    /**
     * Formate un nom d'enum en texte lisible
     * Ex: MY_ENUM_VALUE -> My Enum Value
     */
    fun formatEnumName(enumName: String): String {
        return enumName.split("_").joinToString(" ") { capitalize(it) }
    }
    
    /**
     * Calcule le pourcentage entre deux valeurs
     */
    fun calculatePercentage(current: Int, maximum: Int): Double {
        return if (maximum == 0) 0.0 else (current.toDouble() / maximum.toDouble()) * 100.0
    }
    
    /**
     * Arrondit un nombre à un certain nombre de décimales
     */
    fun roundToDecimal(value: Double, decimals: Int): Double {
        val factor = 10.0.pow(decimals.toDouble())
        return round(value * factor) / factor
    }
    
    /**
     * Vérifie si une version est plus récente qu'une autre
     * Format: x.y.z
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(currentParts.size, latestParts.size)
        
        for (i in 0 until maxLength) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            
            when {
                latestPart > currentPart -> return true
                latestPart < currentPart -> return false
            }
        }
        
        return false
    }
    
    /**
     * Crée une barre de progression textuelle
     */
    fun createProgressBar(
        current: Int, 
        maximum: Int, 
        length: Int = 20, 
        filledChar: Char = '█', 
        emptyChar: Char = '░'
    ): String {
        val percentage = if (maximum == 0) 0.0 else current.toDouble() / maximum.toDouble()
        val filledLength = (percentage * length).toInt()
        
        val filled = filledChar.toString().repeat(filledLength)
        val empty = emptyChar.toString().repeat(length - filledLength)
        
        return "&a$filled&7$empty &f$current&7/&f$maximum"
    }
    
    /**
     * Convertit des millisecondes en format MM:SS
     */
    fun formatTimeMinutesSeconds(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}