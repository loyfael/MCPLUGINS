package loyfael.commands

import loyfael.AetherPlayerDelivery
import loyfael.gui.GUIManager
import loyfael.managers.LivraisonManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class LivraisonCommandHandler(
    private val plugin: AetherPlayerDelivery,
    private val guiManager: GUIManager,
    private val livraisonManager: LivraisonManager
) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande est réservée aux joueurs !")
            return true
        }
        
        // Simple implementation for now - open available orders menu for deliverers
        guiManager.ouvrirGUILivraisonsDisponibles(sender)
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return emptyList()
    }
}
