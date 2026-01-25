package loyfael.commands

import loyfael.AetherPlayerDelivery
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Handler pour les commandes GUI
 * Permet d'ouvrir l'interface graphique principale
 */
class GUICommandHandler(private val plugin: AetherPlayerDelivery) : CommandExecutor {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Vérifier que c'est un joueur
        if (sender !is Player) {
            sender.sendMessage("§cCette commande ne peut être exécutée que par un joueur !")
            return true
        }
        
        when (command.name.lowercase()) {
            "delivery", "livraison", "aether" -> {
                handleMainCommand(sender, args)
                return true
            }
        }
        
        return false
    }
    
    /**
     * Gère la commande principale
     */
    private fun handleMainCommand(player: Player, args: Array<out String>) {
        if (args.isEmpty()) {
            // Ouvrir l'interface principale
            plugin.guiManager.ouvrirGUILivraisons(player)
            return
        }
        
        // Gérer les sous-commandes
        when (args[0].lowercase()) {
            "gui", "menu", "interface" -> {
                plugin.guiManager.ouvrirGUILivraisons(player)
            }
            "help", "aide" -> {
                envoyerAide(player)
            }
            else -> {
                player.sendMessage("§cSous-commande inconnue. Utilisez /delivery help pour l'aide.")
            }
        }
    }
    
    /**
     * Envoie l'aide des commandes GUI
     */
    private fun envoyerAide(player: Player) {
        val messages = arrayOf(
            "§6§l=== AetherPlayerDelivery - Aide ===",
            "§e/delivery §7- Ouvre l'interface principale",
            "§e/delivery gui §7- Ouvre l'interface principale", 
            "§e/delivery help §7- Affiche cette aide",
            "§6§l====================================="
        )
        messages.forEach { player.sendMessage(it) }
    }
}