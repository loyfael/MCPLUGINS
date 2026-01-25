package loyfael.integrations

import loyfael.ClassePlugin
import loyfael.data.Classe
import loyfael.manager.ClasseManager
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

/**
 * PlaceholderAPI Extension for NuvaPeoples plugin
 */
class NuvaPlaceholderExpansion(private val plugin: ClassePlugin) : PlaceholderExpansion() {
    
    private val classeManager: ClasseManager by lazy { plugin.classeManager }
    
    /**
     * Extension identifier in PlaceholderAPI
     */
    override fun getIdentifier(): String = "nuva"
    
    /**
     * Extension author
     */
    override fun getAuthor(): String = "Loyfael"
    
    /**
     * Extension version
     */
    override fun getVersion(): String = plugin.pluginMeta.version
    
    /**
     * Extension description
     */
    override fun getDescription(): String = "Placeholders pour NuvaPeoples - Classes/Peuples Minecraft"
    
    /**
     * Indicates that the extension should persist
     */
    override fun persist(): Boolean = true
    
    /**
     * Indicates that the extension can be used offline
     */
    override fun canRegister(): Boolean = true
    
    /**
     * Placeholder processing
     */
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null
        
        return when (params.lowercase()) {
            // Basic class info
            "classe" -> {
                // %nuva_classe% - Nom de la classe du joueur au singulier avec couleur
                val classe = classeManager.getClasse(player) ?: Classe.AME_ERRANTE
                if (classe == Classe.AME_ERRANTE) {
                    "§fÂme errante" // Blanc pour âme errante
                } else {
                    getClasseColor(classe) + classe.getSingularName()
                }
            }
            
            "classe_singulier" -> {
                // %nuva_classe_singulier% - Nom singulier sans couleur
                val classe = classeManager.getClasse(player) ?: Classe.AME_ERRANTE
                classe.getSingularName()
            }
            
            "classe_complete" -> {
                // %nuva_classe_complete% - Nom complet avec emoji coloré (singulier)
                val classe = classeManager.getClasse(player) ?: Classe.AME_ERRANTE
                if (classe == Classe.AME_ERRANTE) {
                    "§f👻 Âme errante" // Blanc pour âme errante
                } else {
                    getClasseColor(classe) + classe.emoji + "§f " + classe.getSingularName()
                }
            }
            
            "classe_emoji" -> {
                // %nuva_classe_emoji% - Emoji de la classe avec couleur dédiée
                val classe = classeManager.getClasse(player) ?: Classe.AME_ERRANTE
                getClasseColor(classe) + classe.emoji
            }
            
            "classe_emoji_brut" -> {
                // %nuva_classe_emoji_brut% - Emoji de la classe sans couleur
                val classe = classeManager.getClasse(player) ?: Classe.AME_ERRANTE
                classe.emoji
            }
            
            "a_classe" -> {
                // %nuva_a_classe% - true/false si le joueur a une classe
                val classe = classeManager.getClasse(player)
                (classe != null && classe != Classe.AME_ERRANTE).toString()
            }
            
            "classe_couleur" -> {
                // %nuva_classe_couleur% - Couleur associée à la classe
                val classe = classeManager.getClasse(player) ?: Classe.AME_ERRANTE
                getClasseColor(classe)
            }
            
            "date_selection" -> {
                // %nuva_date_selection% - Date de sélection de la classe
                val playerData = classeManager.getPlayerData(player)
                playerData?.let {
                    java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(it.selectionDate))
                } ?: "Jamais"
            }
            
            "passifs_nombre" -> {
                // %nuva_passifs_nombre% - Nombre de passifs actifs
                val playerData = classeManager.getPlayerData(player)
                playerData?.activePassives?.size?.toString() ?: "0"
            }
            
            "passifs_liste" -> {
                // %nuva_passifs_liste% - Liste des passifs séparés par des virgules
                val classe = classeManager.getClasse(player) ?: Classe.AME_ERRANTE
                if (classe.passives.isEmpty()) {
                    "Aucun"
                } else {
                    classe.passives.joinToString(", ")
                }
            }
            
            "temps_joue" -> {
                // %nuva_temps_joue% - Temps joué avec cette classe (en heures)
                val playerData = classeManager.getPlayerData(player)
                playerData?.let {
                    val heures = (System.currentTimeMillis() - it.selectionDate) / (1000 * 60 * 60)
                    "${heures}h"
                } ?: "0h"
            }
            
            "cooldowns_actifs" -> {
                // %nuva_cooldowns_actifs% - Nombre de cooldowns actifs
                val playerData = classeManager.getPlayerData(player)
                val activeCooldowns = playerData?.activePassives?.values?.count { 
                    it.getRemainingCooldown() > 0 
                } ?: 0
                activeCooldowns.toString()
            }
            
            "is_bastorgnes" -> (classeManager.getClasse(player) == Classe.BASTORGNES).toString()
            "is_tartinuits" -> (classeManager.getClasse(player) == Classe.TARTINUITS).toString()
            "is_sylvounets" -> (classeManager.getClasse(player) == Classe.SYLVOUNETS).toString()
            "is_grosuki" -> (classeManager.getClasse(player) == Classe.GROSUKI).toString()
            "is_bricobrak" -> (classeManager.getClasse(player) == Classe.BRICOBRAK).toString()
            "is_miraziens" -> (classeManager.getClasse(player) == Classe.MIRAZIENS).toString()
            "is_ame_errante" -> (classeManager.getClasse(player) == Classe.AME_ERRANTE).toString()
            
            // Statistics for all classes
            "total_joueurs" -> classeManager.getOnlinePlayersWithClass().size.toString()
            "joueurs_bastorgnes" -> classeManager.getPlayersWithClasse(Classe.BASTORGNES).size.toString()
            "joueurs_tartinuits" -> classeManager.getPlayersWithClasse(Classe.TARTINUITS).size.toString()
            "joueurs_sylvounets" -> classeManager.getPlayersWithClasse(Classe.SYLVOUNETS).size.toString()
            "joueurs_grosuki" -> classeManager.getPlayersWithClasse(Classe.GROSUKI).size.toString()
            "joueurs_bricobrak" -> classeManager.getPlayersWithClasse(Classe.BRICOBRAK).size.toString()
            "joueurs_miraziens" -> classeManager.getPlayersWithClasse(Classe.MIRAZIENS).size.toString()
            "joueurs_ames_errantes" -> classeManager.getPlayersWithClasse(Classe.AME_ERRANTE).size.toString()
            
            // Server statistics
            "classe_populaire" -> {
                // %nuva_classe_populaire% - Classe la plus populaire
                val stats = classeManager.getClasseStatistics()
                stats.maxByOrNull { it.second }?.first?.displayName ?: "Aucune"
            }
            
            "pourcentage_bastorgnes" -> {
                val total = classeManager.getOnlinePlayersWithClass().size
                if (total == 0) "0%" else {
                    val bastorgnes = classeManager.getPlayersWithClasse(Classe.BASTORGNES).size
                    "${(bastorgnes * 100) / total}%"
                }
            }
            
            "pourcentage_tartinuits" -> {
                val total = classeManager.getOnlinePlayersWithClass().size
                if (total == 0) "0%" else {
                    val tartinuits = classeManager.getPlayersWithClasse(Classe.TARTINUITS).size
                    "${(tartinuits * 100) / total}%"
                }
            }
            
            // Add other percentage placeholders similarly...
            
            else -> null // Unknown placeholder
        }
    }
    
    /**
     * Get color associated with class
     */
    private fun getClasseColor(classe: Classe): String {
        return when (classe) {
            Classe.BASTORGNES -> "§c"     // Rouge
            Classe.TARTINUITS -> "§6"     // Orange/Or
            Classe.SYLVOUNETS -> "§a"     // Vert
            Classe.MIRAZIENS -> "§e"      // Jaune
            Classe.BRICOBRAK -> "§3"      // Cyan foncé
            Classe.GROSUKI -> "§5"        // Violet
            Classe.AME_ERRANTE -> "§7"    // Gris
        }
    }
}
