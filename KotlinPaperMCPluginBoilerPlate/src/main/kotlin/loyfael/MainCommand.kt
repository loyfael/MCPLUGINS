package loyfael

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Commande principale du plugin
 * 
 * Gère les sous-commandes : help, reload, info
 */
class MainCommand(
    private val plugin: MyPlugin,
    private val configManager: ConfigManager,
    private val databaseManager: DatabaseManager
) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "help" -> {
                sendHelp(sender)
            }
            
            "reload" -> {
                if (!sender.hasPermission("myplugin.reload")) {
                    sendMessage(sender, configManager.getMessage("no-permission"))
                    return true
                }
                
                try {
                    plugin.reloadPluginConfig()
                    sendMessage(sender, configManager.getMessage("reload-success"))
                } catch (e: Exception) {
                    sendMessage(sender, "&cErreur lors du rechargement : ${e.message}")
                    plugin.logger.severe("Erreur lors du rechargement : ${e.message}")
                }
            }
            
            "info" -> {
                sendInfo(sender)
            }
            
            "debug" -> {
                if (!sender.hasPermission("myplugin.debug")) {
                    sendMessage(sender, configManager.getMessage("no-permission"))
                    return true
                }
                
                sendDebugInfo(sender)
            }
            
            else -> {
                sendMessage(sender, configManager.getMessage("error.unknown-command"))
            }
        }
        
        return true
    }
    
    /**
     * Envoie l'aide à un joueur
     */
    private fun sendHelp(sender: CommandSender) {
        sendMessage(sender, configManager.getRawMessage("help.header"))
        
        configManager.getHelpCommands().forEach { helpLine ->
            sendMessage(sender, helpLine)
        }
        
        sendMessage(sender, configManager.getRawMessage("help.footer"))
    }
    
    /**
     * Envoie les informations du plugin
     */
    private fun sendInfo(sender: CommandSender) {
        val pluginMeta = plugin.pluginMeta
        
        sendMessage(sender, "&8&m-----------&r &bInformations Plugin &8&m-----------")
        sendMessage(sender, "&eNom: &f${pluginMeta.name}")
        sendMessage(sender, "&eVersion: &f${pluginMeta.version}")
        sendMessage(sender, "&eAuteur(s): &f${pluginMeta.authors.joinToString(", ")}")
        sendMessage(sender, "&eDescription: &f${pluginMeta.description}")
        sendMessage(sender, "&eAPI Version: &f${pluginMeta.apiVersion}")
        sendMessage(sender, "&8&m-----------------------------------------")
    }
    
    /**
     * Envoie les informations de debug
     */
    private fun sendDebugInfo(sender: CommandSender) {
        sendMessage(sender, "&8&m-----------&r &bInformations Debug &8&m-----------")
        sendMessage(sender, "&eBase de données: &f${configManager.getDatabaseType().uppercase()}")
        sendMessage(sender, "&eDebug activé: &f${if (configManager.isDebugEnabled()) "&aOui" else "&cNon"}")
        sendMessage(sender, "&eLangue: &f${configManager.getLanguage()}")
        
        // Test de connexion à la base de données
        val dbStatus = if (databaseManager.testConnection()) "&aConnectée" else "&cDéconnectée"
        sendMessage(sender, "&eÉtat BDD: $dbStatus")
        
        sendMessage(sender, "&8&m-----------------------------------------")
    }
    
    /**
     * Envoie un message formaté à un CommandSender
     */
    private fun sendMessage(sender: CommandSender, message: String) {
        val component = LegacyComponentSerializer.legacyAmpersand().deserialize(message)
        
        when (sender) {
            is Player -> sender.sendMessage(component)
            else -> sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(component))
        }
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> {
                val subcommands = mutableListOf("help", "info")
                
                if (sender.hasPermission("myplugin.reload")) {
                    subcommands.add("reload")
                }
                
                if (sender.hasPermission("myplugin.debug")) {
                    subcommands.add("debug")
                }
                
                subcommands.filter { it.startsWith(args[0].lowercase()) }
            }
            else -> emptyList()
        }
    }
}