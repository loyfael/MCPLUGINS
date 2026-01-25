package loyfael.commands

import loyfael.ClassePlugin
import loyfael.data.Classe
import loyfael.manager.ClasseManager
import loyfael.manager.GUIManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Gestionnaire de la commande principale /classe
 */
class ClasseCommand(private val plugin: ClassePlugin) : CommandExecutor, TabCompleter {
    
    private val classeManager: ClasseManager by lazy { plugin.classeManager }
    private val guiManager: GUIManager by lazy { plugin.guiManager }
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val isPlayer = sender is Player
        val player = sender as? Player
        // If sender is console/non-player, only allow the admin subcommand "set/forcer"
        val isSetSub = args.isNotEmpty() && (args[0].equals("set", true) || args[0].equals("forcer", true))
        if (!isPlayer && !isSetSub) {
            sender.sendMessage("§cCette commande doit être exécutée par un joueur. §7(Indice: la console peut utiliser §e/classe set <joueur> <classe>§7)")
            return true
        }
        
        when {
            args.isEmpty() -> {
                val p = player ?: return true
                // Toujours afficher les informations de la classe actuelle, même pour âme errante
                guiManager.ouvrirGUIInfo(p)
            }
            
            args[0].equals("choisir", ignoreCase = true) -> {
                val p = player ?: return true
                if (!p.hasPermission("nuva.classe.choisir")) {
                    p.sendMessage("§cVous n'avez pas la permission de choisir une classe !")
                    return true
                }
                
                // Ouvrir la GUI de sélection de classe
                guiManager.ouvrirGUISelection(p)
            }
            
            args[0].equals("list", ignoreCase = true) || args[0].equals("liste", ignoreCase = true) -> {
                val p = player ?: return true
                // Afficher la liste des classes disponibles
                afficherListeClasses(p)
            }
            
            args[0].equals("info", ignoreCase = true) -> {
                val p = player ?: return true
                if (args.size >= 2) {
                    // Afficher les infos d'une classe spécifique
                    val nomClasse = args[1].uppercase()
                    try {
                        val classe = Classe.valueOf(nomClasse)
                        afficherInfoClasseSpecifique(p, classe)
                    } catch (e: IllegalArgumentException) {
                        p.sendMessage("§cClasse inconnue : ${args[1]}")
                        afficherListeClasses(p)
                    }
                } else {
                    // Ouvrir la GUI d'informations pour sa classe actuelle
                    guiManager.ouvrirGUIInfo(p)
                }
            }
            
            args[0].equals("set", ignoreCase = true) || args[0].equals("forcer", ignoreCase = true) -> {
                if (!sender.hasPermission("nuvapeoples.admin")) {
                    sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande !")
                    return true
                }
                
                if (args.size >= 3) {
                    val targetName = args[1]
                    val nomClasse = args[2].uppercase()
                    
                    val target = plugin.server.getPlayer(targetName)
                    if (target == null) {
                        sender.sendMessage("§cJoueur introuvable : $targetName")
                        return true
                    }
                    
                    try {
                        val classe = Classe.valueOf(nomClasse)
                        // Current class of the target
                        val currentClasse = classeManager.getClasse(target) ?: Classe.AME_ERRANTE

                        // Early exit: same class requested
                        if (currentClasse == classe) {
                            sender.sendMessage("§e${target.name}§7 possède déjà la classe §e${classe.getFullDisplayName()}§7.")
                            target.sendMessage("§7Vous êtes déjà membre du peuple §e${classe.getFullDisplayName()}§7.")
                            return true
                        }

                        // Guard: require reset to Âme Errante before assigning a new class
                        if (classe != Classe.AME_ERRANTE && currentClasse != Classe.AME_ERRANTE) {
                            sender.sendMessage("§cVous ne pouvez pas changer de classe de cette manière. Redevenez d'abord §7Âme errante§c avant d'en choisir une nouvelle. Allez voir Erratus dans sa cabane volante !")
                            // Also notify target player so they're aware, especially when command is run from console
                            target.sendMessage("§cVous devez redevenir §7Âme errante§c avant de choisir une nouvelle classe. Allez voir Erratus dans sa cabane volante à Bazarolis !")
                            return true
                        }

                        if (classeManager.changeClasse(target, classe)) {
                            sender.sendMessage("§aClasse de ${target.name} changée vers §e${classe.getFullDisplayName()}§a !")
                            target.sendMessage("§aVotre classe a été changée vers §e${classe.getFullDisplayName()}§a par un administrateur.")
                        } else {
                            sender.sendMessage("§cErreur lors du changement de classe !")
                        }
                    } catch (e: IllegalArgumentException) {
                        sender.sendMessage("§cClasse inconnue : ${args[2]}")
                        if (player != null) afficherListeClasses(player) else sender.sendMessage("§eUtilisez /classe list pour les classes disponibles.")
                    }
                } else {
                    sender.sendMessage("§cUsage : /classe set <joueur> <classe>")
                }
            }
            
            args[0].equals("reload", ignoreCase = true) -> {
                val p = player ?: return true
                if (!p.hasPermission("nuvapeoples.admin")) {
                    p.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande !")
                    return true
                }
                
                try {
                    // Recharger la configuration
                    rechargerConfiguration(p)
                } catch (e: Exception) {
                    p.sendMessage("§cErreur lors du rechargement : ${e.message}")
                }
            }
            
            args[0].equals("stats", ignoreCase = true) || args[0].equals("statistiques", ignoreCase = true) -> {
                val p = player ?: return true
                if (!p.hasPermission("nuvapeoples.admin")) {
                    p.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande !")
                    return true
                }
                
                afficherStatistiques(p)
            }
            
            else -> {
                // Commande inconnue
                val p = player ?: return true
                afficherAide(p)
            }
        }
        
        return true
    }
    
    /**
     * Affiche les informations de classe du joueur (supprimée - remplacée par GUI directement)
     */
    
    /**
     * Recharge la configuration
     */
    private fun rechargerConfiguration(player: Player) {
        plugin.reloadConfig()
        classeManager.reload()
        player.sendMessage("§aConfiguration rechargée avec succès !")
    }
    
    /**
     * Affiche la liste des classes disponibles
     */
    private fun afficherListeClasses(player: Player) {
        player.sendMessage("§6═══ Classes Disponibles ═══")
        
        Classe.values().filter { it != Classe.AME_ERRANTE }.forEach { classe ->
            player.sendMessage("§e${classe.name.lowercase()} §7- ${classe.getFullDisplayName()}")
        }
        
        player.sendMessage("")
        player.sendMessage("§7Utilisez §e/classe info <classe>§7 pour plus d'informations")
        player.sendMessage("§7Utilisez §e/classe choisir§7 pour ouvrir le menu de sélection")
    }
    
    /**
     * Affiche les informations d'une classe spécifique
     */
    private fun afficherInfoClasseSpecifique(player: Player, classe: Classe) {
        player.sendMessage("§6═══ ${classe.getFullDisplayName()} ═══")
        
        if (classe.passives.isNotEmpty()) {
            player.sendMessage("§ePassifs :")
            classe.passives.forEach { passif ->
                player.sendMessage("§7• $passif")
            }
        } else {
            player.sendMessage("§7Aucun passif spécial")
        }
        
        player.sendMessage("")
        player.sendMessage("§7Utilisez §e/classe choisir§7 pour ouvrir le menu de sélection")
    }
    
    /**
     * Affiche les statistiques des classes
     */
    private fun afficherStatistiques(player: Player) {
        val stats = classeManager.getClasseStatistics()
        val statsMap = stats.toMap()
        val totalJoueurs = statsMap.values.sum()
        
        player.sendMessage("§6═══ Statistiques des Classes ═══")
        player.sendMessage("§7Total joueurs connectés : §e$totalJoueurs")
        player.sendMessage("")
        
        Classe.values().forEach { classe ->
            val nombre = statsMap.getOrDefault(classe, 0)
            val pourcentage = if (totalJoueurs > 0) {
                String.format("%.1f%%", (nombre.toDouble() / totalJoueurs) * 100)
            } else "0%"
            
            val display = if (classe == Classe.AME_ERRANTE) "§8Âmes errantes" else "§e${classe.getFullDisplayName()}"
            player.sendMessage("$display §7: §f$nombre joueurs §7($pourcentage)")
        }
    }
    
    /**
     * Affiche l'aide de la commande
     */
    private fun afficherAide(player: Player) {
        player.sendMessage("§6═══ Aide - NuvaPeoples ═══")
        player.sendMessage("§e/classe §7- Afficher les informations de votre classe")
        player.sendMessage("§e/classe choisir §7- Ouvrir le menu de sélection")
        player.sendMessage("§e/classe list §7- Liste des classes disponibles")
        player.sendMessage("§e/classe info <classe> §7- Informations d'une classe")
        
        if (player.hasPermission("nuvapeoples.admin")) {
            player.sendMessage("§c/classe set <joueur> <classe> §7- Forcer classe (admin)")
            player.sendMessage("§c/classe reload §7- Recharger la config (admin)")
            player.sendMessage("§c/classe stats §7- Statistiques (admin)")
        }
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()
        
        return when (args.size) {
            1 -> {
                val baseCommands = mutableListOf("choisir", "list", "liste", "info")
                if (sender.hasPermission("nuvapeoples.admin")) {
                    baseCommands.addAll(listOf("set", "forcer", "reload", "stats", "statistiques"))
                }
                baseCommands.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            
            2 -> {
                when (args[0].lowercase()) {
                    "info" -> {
                        Classe.values()
                            .filter { it != Classe.AME_ERRANTE }
                            .map { it.name.lowercase() }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    "set", "forcer" -> {
                        if (sender.hasPermission("nuvapeoples.admin")) {
                            plugin.server.onlinePlayers
                                .map { it.name }
                                .filter { it.startsWith(args[1], ignoreCase = true) }
                        } else emptyList()
                    }
                    else -> emptyList()
                }
            }
            
            3 -> {
                when (args[0].lowercase()) {
                    "set", "forcer" -> {
                        if (sender.hasPermission("nuvapeoples.admin")) {
                            Classe.values()
                                .map { it.name.lowercase() }
                                .filter { it.startsWith(args[2], ignoreCase = true) }
                        } else emptyList()
                    }
                    else -> emptyList()
                }
            }
            
            else -> emptyList()
        }
    }
}
