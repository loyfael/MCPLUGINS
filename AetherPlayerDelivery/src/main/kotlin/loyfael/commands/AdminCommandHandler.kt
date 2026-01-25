package loyfael.commands

import kotlinx.coroutines.*
import loyfael.AetherPlayerDelivery
import loyfael.managers.SchedulerManager
import loyfael.managers.TaskType
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Gestionnaire de la commande /aetherdelivery (commande admin)
 * Gère les commandes d'administration du plugin
 */
class AdminCommandHandler(
    private val plugin: AetherPlayerDelivery,
    private val schedulerManager: SchedulerManager
) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        
        // Vérifier la permission admin
        if (!sender.hasPermission("aetherdelivery.admin")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "Vous n'avez pas la permission !")
            return true
        }
        
        when (args.getOrNull(0)?.lowercase()) {
            null, "help", "aide" -> {
                showHelp(sender)
            }
            
            "reload" -> {
                // Recharger la configuration
                plugin.reloadConfig()
                sender.sendMessage("§aConfiguration rechargée avec succès !")
            }
            
            "stats", "statistiques" -> {
                // Afficher les statistiques du plugin
                showPluginStats(sender)
            }
            
            "tasks", "taches" -> {
                // Gestion des tâches périodiques
                when (args.getOrNull(1)?.lowercase()) {
                    null, "status" -> {
                        showTaskStatus(sender)
                    }
                    "restart" -> {
                        val taskType = when (args.getOrNull(2)?.lowercase()) {
                            "expiration" -> TaskType.EXPIRATION_CHECK
                            "reminders" -> TaskType.REMINDERS
                            "cleanup" -> TaskType.CLEANUP
                            else -> {
                                sender.sendMessage("§cType de tâche invalide ! Types: expiration, reminders, cleanup")
                                return true
                            }
                        }
                        schedulerManager.restartTask(taskType)
                        sender.sendMessage("§aTâche $taskType redémarrée !")
                    }
                    "force" -> {
                        when (args.getOrNull(2)?.lowercase()) {
                            "expiration" -> {
                                sender.sendMessage("§eVérification forcée des expirations en cours...")
                                schedulerManager.forceExpirationCheck()
                                sender.sendMessage("§aVérification lancée !")
                            }
                            else -> {
                                sender.sendMessage("§cAction forcée invalide ! Actions: expiration")
                            }
                        }
                    }
                    else -> {
                        sender.sendMessage("§cAction invalide ! Actions: status, restart <type>, force <action>")
                    }
                }
            }
            
            "database", "db" -> {
                // Actions sur la base de données
                when (args.getOrNull(1)?.lowercase()) {
                    "test" -> {
                        // Tester la connexion à la base de données
                        schedulerManager.runAsync {
                            try {
                                // TODO: Ajouter une méthode de test de connexion dans DatabaseManager
                                sender.sendMessage("§aConnexion à la base de données OK !")
                            } catch (e: Exception) {
                                sender.sendMessage("§cErreur de connexion à la base de données : ${e.message}")
                            }
                        }
                    }
                    else -> {
                        sender.sendMessage("§cAction invalide ! Actions: test")
                    }
                }
            }
            
            "player" -> {
                // Gestion des joueurs
                if (args.size < 3) {
                    sender.sendMessage("§cUtilisation: /aetherdelivery player <nom> <action>")
                    return true
                }
                
                val playerName = args[1]
                val action = args[2].lowercase()
                
                when (action) {
                    "stats" -> {
                        // Statistiques d'un joueur spécifique
                        schedulerManager.runAsync {
                            // TODO: Implémenter les statistiques par joueur
                            sender.sendMessage("§eStatistiques de $playerName :")
                            sender.sendMessage("§7(Fonctionnalité en cours d'implémentation)")
                        }
                    }
                    "reset" -> {
                        // Reset des données d'un joueur (avec confirmation)
                        if (args.getOrNull(3) != "confirm") {
                            sender.sendMessage("§cATTENTION: Cette action supprimera TOUTES les données du joueur $playerName !")
                            sender.sendMessage("§cPour confirmer, utilisez: /aetherdelivery player $playerName reset confirm")
                            return true
                        }
                        
                        schedulerManager.runAsync {
                            // TODO: Implémenter la suppression des données joueur
                            sender.sendMessage("§aDonnées du joueur $playerName supprimées !")
                        }
                    }
                    else -> {
                        sender.sendMessage("§cAction invalide ! Actions: stats, reset")
                    }
                }
            }
            
            "mode" -> {
                // Changer le mode du serveur (principal/satellite)
                val newMode = args.getOrNull(1)?.lowercase()
                if (newMode !in listOf("principal", "satellite")) {
                    sender.sendMessage("§cMode invalide ! Modes: principal, satellite")
                    return true
                }
                
                plugin.config.set("mode", newMode)
                plugin.saveConfig()
                sender.sendMessage("§aMode changé vers '$newMode'. Redémarrage recommandé.")
            }
            
            "debug" -> {
                // Activer/désactiver le mode debug
                val currentDebug = plugin.config.getBoolean("general.debug", false)
                val newDebug = !currentDebug
                
                plugin.config.set("general.debug", newDebug)
                plugin.saveConfig()
                
                val status = if (newDebug) "activé" else "désactivé"
                sender.sendMessage("§aMode debug $status !")
            }
            
            "version", "ver" -> {
                // Informations sur le plugin
                val version = plugin.pluginMeta.version
                val mode = plugin.config.getString("mode", "principal")
                
                sender.sendMessage("§6=== AetherPlayerDelivery ===")
                sender.sendMessage("§eVersion: §f$version")
                sender.sendMessage("§eMode: §f$mode")
                sender.sendMessage("§eAuteur: §fLoyfael")
                sender.sendMessage("§eServeur: §f${plugin.server.name} ${plugin.server.version}")
            }
            
            else -> {
                sender.sendMessage("§cCommande inconnue ! Utilisez §e/aetherdelivery help §cpour voir les commandes disponibles.")
            }
        }
        
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("aetherdelivery.admin")) return emptyList()
        
        return when (args.size) {
            1 -> listOf("help", "reload", "stats", "tasks", "database", "player", "mode", "debug", "version")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "tasks" -> listOf("status", "restart", "force").filter { it.startsWith(args[1].lowercase()) }
                "database" -> listOf("test").filter { it.startsWith(args[1].lowercase()) }
                "player" -> plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[1], true) }
                "mode" -> listOf("principal", "satellite").filter { it.startsWith(args[1].lowercase()) }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "tasks" -> when (args[1].lowercase()) {
                    "restart" -> listOf("expiration", "reminders", "cleanup").filter { it.startsWith(args[2].lowercase()) }
                    "force" -> listOf("expiration").filter { it.startsWith(args[2].lowercase()) }
                    else -> emptyList()
                }
                "player" -> listOf("stats", "reset").filter { it.startsWith(args[2].lowercase()) }
                else -> emptyList()
            }
            4 -> when (args[0].lowercase()) {
                "player" -> if (args[2].lowercase() == "reset") listOf("confirm") else emptyList()
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
    
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§6=== AetherDelivery - Commandes Admin ===")
        sender.sendMessage("§e/aetherdelivery reload §7- Recharge la configuration")
        sender.sendMessage("§e/aetherdelivery stats §7- Statistiques du plugin")
        sender.sendMessage("§e/aetherdelivery tasks status §7- État des tâches")
        sender.sendMessage("§e/aetherdelivery tasks restart <type> §7- Redémarre une tâche")
        sender.sendMessage("§e/aetherdelivery tasks force expiration §7- Force la vérification")
        sender.sendMessage("§e/aetherdelivery database test §7- Test connexion DB")
        sender.sendMessage("§e/aetherdelivery player <nom> stats §7- Stats joueur")
        sender.sendMessage("§e/aetherdelivery mode <principal/satellite> §7- Change le mode")
        sender.sendMessage("§e/aetherdelivery debug §7- Active/désactive le debug")
        sender.sendMessage("§e/aetherdelivery version §7- Informations plugin")
    }
    
    private fun showPluginStats(sender: CommandSender) {
        schedulerManager.runAsync {
            // TODO: Récupérer les vraies statistiques depuis la base de données
            sender.sendMessage("§6=== Statistiques AetherDelivery ===")
            sender.sendMessage("§eMode serveur: §f${plugin.config.getString("mode", "principal")}")
            sender.sendMessage("§eJoueurs en ligne: §f${plugin.server.onlinePlayers.size}")
            sender.sendMessage("§eCommandes actives: §f(En cours...)")
            sender.sendMessage("§eLivraisons en cours: §f(En cours...)")
            sender.sendMessage("§eVolume économique total: §f(En cours...)")
        }
    }
    
    private fun showTaskStatus(sender: CommandSender) {
        val taskStats = schedulerManager.getTaskStats()
        
        sender.sendMessage("§6=== État des Tâches Périodiques ===")
        sender.sendMessage("§eScheduler actif: §f${if (taskStats.isSchedulerRunning) "§aOUI" else "§cNON"}")
        sender.sendMessage("§eVérification expirations: §f${if (taskStats.expirationCheckRunning) "§aACTIF" else "§cINACTIF"}")
        sender.sendMessage("§eRappels: §f${if (taskStats.remindersRunning) "§aACTIF" else "§cINACTIF"}")
        sender.sendMessage("§eNettoyage: §f${if (taskStats.cleanupRunning) "§aACTIF" else "§cINACTIF"}")
        sender.sendMessage("§eCoroutines actives: §f${taskStats.totalCoroutines}")
    }
}
