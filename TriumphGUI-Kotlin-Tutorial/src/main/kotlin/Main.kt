package loyfael

import loyfael.GUIManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    // GUIManager pour gérer toutes les interfaces
    lateinit var guiManager: GUIManager
        private set

    override fun onEnable() {
        // Initialisation du GUIManager
        guiManager = GUIManager(this)

        // Message de démarrage
        logger.info("§a✓ Plugin Triumph-GUI + Kotlin activé !")
        logger.info("§7Configuration testée sur Paper/Leaf 1.21.8")
        logger.info("§7Utilisez /menu ou /demo pour tester les interfaces")
    }

    override fun onDisable() {
        logger.info("§c✗ Plugin Triumph-GUI + Kotlin désactivé")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Vérification que l'expéditeur est un joueur
        if (sender !is Player) {
            sender.sendMessage("§cCette commande est réservée aux joueurs !")
            return true
        }

        when (command.name.lowercase()) {
            "menu" -> {
                // Vérification des permissions
                if (!sender.hasPermission("exemple.menu")) {
                    sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande !")
                    return true
                }

                // Ouverture du menu principal
                guiManager.openMainMenu(sender)
                return true
            }

            "demo" -> {
                // Vérification des permissions
                if (!sender.hasPermission("exemple.demo")) {
                    sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande !")
                    return true
                }

                // Menu de démonstration complet
                if (args.isEmpty()) {
                    guiManager.openDemoMenu(sender)
                } else {
                    when (args[0].lowercase()) {
                        "list", "liste" -> {
                            guiManager.openDynamicListExample(sender)
                        }
                        "nav", "navigation" -> {
                            guiManager.openNavigationMenu(sender)
                        }
                        "utils", "utilitaires" -> {
                            guiManager.openUtilitiesMenu(sender)
                        }
                        else -> {
                            sender.sendMessage("§cSous-commande inconnue !")
                            sender.sendMessage("§7Disponibles: list, nav, utils")
                        }
                    }
                }
                return true
            }
        }

        return false
    }
}