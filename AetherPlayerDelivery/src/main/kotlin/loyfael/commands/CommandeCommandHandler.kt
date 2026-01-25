package loyfael.commands

import kotlinx.coroutines.*
import loyfael.AetherPlayerDelivery
import loyfael.gui.GUIManager
import loyfael.managers.CommandeManager
import loyfael.managers.LivraisonManager
import loyfael.managers.SchedulerManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Gestionnaire de la commande /commande
 * Gère toutes les sous-commandes liées aux commandes clients
 */
class CommandeCommandHandler(
    private val plugin: AetherPlayerDelivery,
    private val guiManager: GUIManager,
    private val commandeManager: CommandeManager
) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.config.getString("messages.player-only") ?: "Cette commande est réservée aux joueurs !")
            return true
        }
        
        // Vérifier la permission de base
        if (!sender.hasPermission("aetherdelivery.client")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "Vous n'avez pas la permission !")
            return true
        }
        
        when (args.getOrNull(0)?.lowercase()) {
            null, "menu", "gui" -> {
                // Ouvrir le menu principal
                guiManager.ouvrirGUILivraisons(sender)
            }
            
            "creer", "create" -> {
                // Ouvrir le menu de création de commande
                guiManager.ouvrirGUICommande(sender)
            }
            
            "list", "liste" -> {
                // Ouvrir le menu "Mes commandes"
                guiManager.ouvrirGUIMesCommandes(sender)
            }
            
            "annuler", "cancel" -> {
                // Annuler une commande
                if (args.size < 2) {
                    sender.sendMessage("§cUtilisation: /commande annuler <ID>")
                    return true
                }
                
                val commandeId = args[1].toLongOrNull()
                if (commandeId == null) {
                    sender.sendMessage("§cID de commande invalide !")
                    return true
                }
                
                commandeManager.launch {
                    val result = commandeManager.cancelCommande(sender, commandeId)
                    when (result) {
                        is loyfael.managers.CommandeResult.SUCCESS -> {
                            sender.sendMessage("§a${result.message}")
                        }
                        else -> {
                            sender.sendMessage("§cImpossible d'annuler cette commande")
                        }
                    }
                }
            }
            
            "recuperer", "recover" -> {
                // Récupérer une commande livrée
                if (args.size < 2) {
                    sender.sendMessage("§cUtilisation: /commande recuperer <ID>")
                    return true
                }
                
                val commandeId = args[1].toLongOrNull()
                if (commandeId == null) {
                    sender.sendMessage("§cID de commande invalide !")
                    return true
                }
                
                // Cette fonctionnalité sera gérée via le GUI ou LivraisonManager
                sender.sendMessage("§eUtilisez le menu GUI pour récupérer vos commandes")
            }
            
            "info" -> {
                // Informations sur une commande
                if (args.size < 2) {
                    sender.sendMessage("§cUtilisation: /commande info <ID>")
                    return true
                }
                
                val commandeId = args[1].toLongOrNull()
                if (commandeId == null) {
                    sender.sendMessage("§cID de commande invalide !")
                    return true
                }
                
                commandeManager.launch {
                    val commande = commandeManager.getCommande(commandeId)
                    if (commande == null || commande.uuidClient != sender.uniqueId) {
                        sender.sendMessage("§cCommande introuvable ou ne vous appartenant pas !")
                        return@launch
                    }
                    
                    sender.sendMessage("§6=== Commande #${commande.id} ===")
                    sender.sendMessage("§eItem: §f${commande.nomItem}")
                    sender.sendMessage("§eQuantité: §f${commande.quantite}")
                    sender.sendMessage("§ePrix: §f${plugin.economieManager.formatMoney(commande.prixTotal)}")
                    sender.sendMessage("§eStatut: §f${commande.statut}")
                    sender.sendMessage("§eCréée le: §f${commande.createdAt}")
                    sender.sendMessage("§eDélai: §f${commande.deadline}")
                }
            }
            
            "help", "aide" -> {
                showHelp(sender)
            }
            
            else -> {
                sender.sendMessage("§cCommande inconnue ! Utilisez §e/commande help §cpour voir les commandes disponibles.")
            }
        }
        
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("aetherdelivery.client")) return emptyList()
        
        return when (args.size) {
            1 -> listOf("menu", "creer", "liste", "annuler", "recuperer", "info", "help")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "annuler", "cancel", "recuperer", "recover", "info" -> {
                    // TODO: Suggérer les IDs des commandes du joueur
                    emptyList()
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
    
    private fun showHelp(player: Player) {
        player.sendMessage("§6=== AetherDelivery - Commandes Client ===")
        player.sendMessage("§e/commande menu §7- Ouvre le menu principal")
        player.sendMessage("§e/commande creer §7- Créer une nouvelle commande")
        player.sendMessage("§e/commande liste §7- Voir vos commandes")
        player.sendMessage("§e/commande annuler <ID> §7- Annuler une commande")
        player.sendMessage("§e/commande info <ID> §7- Informations sur une commande")
        player.sendMessage("§e/commande help §7- Affiche cette aide")
    }
}
