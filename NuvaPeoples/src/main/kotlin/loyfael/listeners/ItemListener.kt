package loyfael.listeners

import loyfael.ClassePlugin
import loyfael.data.PassiveType
import loyfael.manager.ClasseManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * Gestionnaire des événements liés aux items et à la durabilité
 */
class ItemListener(private val plugin: ClassePlugin) : Listener {
    
    private val classeManager: ClasseManager by lazy { plugin.classeManager }
    
    /**
     * Gère la perte de durabilité des items
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        val player = event.player
        val item = event.item
        val damage = event.damage
        
        val playerData = classeManager.getPlayerData(player) ?: return
        
        // Outils bricoleurs (Bricobrak) - Réduction perte durabilité
        if (playerData.hasPassive(PassiveType.OUTILS_BRICOLEURS)) {
            if (plugin.config.getBoolean("passifs.outils_bricoleurs.enabled", true)) {
                if (isOutil(item) || isArme(item)) {
                    val reduction = plugin.config.getDouble("passifs.outils_bricoleurs.reduction_durabilite", 0.20)
                    
                    // Calculer la réduction des dégâts à l'item
                    val nouveauxDegats = (damage * (1.0 - reduction)).toInt()
                    
                    if (nouveauxDegats < damage) {
                        event.damage = maxOf(1, nouveauxDegats) // Minimum 1 point de durabilité
                        
                        if (plugin.config.getBoolean("debug", false)) {
                            plugin.logger.info("${player.name} - Réduction durabilité : $damage → ${event.damage}")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Vérifie si l'item est un outil
     */
    private fun isOutil(item: ItemStack): Boolean {
        return when (item.type.name) {
            // Pelles
            "WOODEN_SHOVEL", "STONE_SHOVEL", "IRON_SHOVEL", "GOLDEN_SHOVEL", "DIAMOND_SHOVEL", "NETHERITE_SHOVEL",
            // Pioches
            "WOODEN_PICKAXE", "STONE_PICKAXE", "IRON_PICKAXE", "GOLDEN_PICKAXE", "DIAMOND_PICKAXE", "NETHERITE_PICKAXE",
            // Haches (aussi armes)
            "WOODEN_AXE", "STONE_AXE", "IRON_AXE", "GOLDEN_AXE", "DIAMOND_AXE", "NETHERITE_AXE",
            // Houes
            "WOODEN_HOE", "STONE_HOE", "IRON_HOE", "GOLDEN_HOE", "DIAMOND_HOE", "NETHERITE_HOE",
            // Outils spéciaux
            "SHEARS", "FLINT_AND_STEEL", "FISHING_ROD", "CARROT_ON_A_STICK", "WARPED_FUNGUS_ON_A_STICK" -> true
            else -> false
        }
    }
    
    /**
     * Vérifie si l'item est une arme
     */
    private fun isArme(item: ItemStack): Boolean {
        return when (item.type.name) {
            // Épées
            "WOODEN_SWORD", "STONE_SWORD", "IRON_SWORD", "GOLDEN_SWORD", "DIAMOND_SWORD", "NETHERITE_SWORD",
            // Arc et arbalète
            "BOW", "CROSSBOW",
            // Trident
            "TRIDENT",
            // Mace (1.21)
            "MACE" -> true
            else -> false
        }
    }
}
