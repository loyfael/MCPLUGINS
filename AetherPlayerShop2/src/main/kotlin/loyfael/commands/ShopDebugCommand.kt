package loyfael.commands

import loyfael.Main
import loyfael.manager.ShopManager
import loyfael.model.Shop
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Commande de diagnostic pour les shops
 */
class ShopDebugCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("aetherplayershop.debug")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§6=== Commandes de diagnostic AetherPlayerShop ===")
            sender.sendMessage("§e/shopdebug lookup §7- Recherche le shop à votre position")
            sender.sendMessage("§e/shopdebug cleanup <x> <y> <z> §7- Nettoie les shops autour d'une position")
            sender.sendMessage("§e/shopdebug ghost §7- Nettoie les shops fantômes à votre position")
            sender.sendMessage("§e/shopdebug stats §7- Affiche les statistiques des shops")
            sender.sendMessage("§e/shopdebug cache §7- Affiche les informations du cache")
            return true
        }

        val subCommand = args[0].lowercase()

        when (subCommand) {
            "lookup" -> handleLookup(sender)
            "cleanup" -> handleCleanup(sender, args)
            "ghost" -> handleGhostCleanup(sender)
            "stats" -> handleStats(sender)
            "cache" -> handleCache(sender)
            else -> {
                sender.sendMessage("§cSous-commande inconnue. Utilisez /shopdebug pour voir l'aide.")
                return true
            }
        }

        return true
    }

    private fun handleLookup(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.")
            return
        }

        val loc = sender.location
        sender.sendMessage("§6=== Diagnostic Shop à votre position ===")
        sender.sendMessage(
            "§7Position: ${loc.world.name} ${loc.blockX},${loc.blockY},${loc.blockZ}"
        )

        plugin.shopManager.getShopAtLocation(loc)
            .thenAccept { shop ->
                if (shop != null) {
                    sender.sendMessage("§a✓ Shop trouvé:")
                    sender.sendMessage("§7- ID: §e${shop.id}")
                    sender.sendMessage("§7- Propriétaire: §e${shop.ownerName}")
                    sender.sendMessage("§7- Type: §e${shop.type}")
                    sender.sendMessage("§7- Prix: §e${shop.price}◎")
                    sender.sendMessage("§7- Stock: §e${shop.stock}")
                    sender.sendMessage("§7- Actif: §e${shop.active}")
                } else {
                    sender.sendMessage("§c✗ Aucun shop trouvé à cette position.")

                    // Compter tous les shops dans la base
                    plugin.shopManager.searchShops(ShopManager.ShopSearchFilter())
                        .thenAccept { allShops ->
                            sender.sendMessage("§7Total shops en base: §e${allShops.size}")
                        }
                }
            }
    }

    private fun handleCleanup(sender: CommandSender, args: Array<out String>) {
        if (args.size != 4) {
            sender.sendMessage("§cUsage: /shopdebug cleanup <x> <y> <z>")
            return
        }

        try {
            val x = args[1].toInt()
            val y = args[2].toInt()
            val z = args[3].toInt()

            val loc: Location = if (sender is Player) {
                Location(sender.world, x.toDouble(), y.toDouble(), z.toDouble())
            } else {
                sender.sendMessage("§cCette commande nécessite un monde. Utilisez en tant que joueur.")
                return
            }

            sender.sendMessage("§6Nettoyage forcé des shops autour de $x,$y,$z")

            plugin.shopManager.forceDeleteShopAtLocation(loc)
                .thenAccept { success ->
                    if (success) {
                        sender.sendMessage("§a✓ Nettoyage effectué avec succès.")
                    } else {
                        sender.sendMessage("§c✗ Aucun shop trouvé à nettoyer.")
                    }
                }

        } catch (e: NumberFormatException) {
            sender.sendMessage("§cCoordonnées invalides. Utilisez des nombres entiers.")
        }
    }

    private fun handleGhostCleanup(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.")
            return
        }

        val loc = sender.location
        sender.sendMessage("§6=== Nettoyage des shops fantômes à votre position ===")
        sender.sendMessage(
            "§7Position: ${loc.world.name} ${loc.blockX},${loc.blockY},${loc.blockZ}"
        )

        plugin.shopManager.cleanupGhostShopsAtLocation(loc)
            .thenAccept { result ->
                if (result > 0) {
                    sender.sendMessage("§a✓ $result shop(s) fantôme(s) nettoyé(s).")
                } else {
                    sender.sendMessage("§c✗ Aucun shop fantôme trouvé à cette position.")
                }
            }
    }

    private fun handleStats(sender: CommandSender) {
        sender.sendMessage("§6=== Statistiques des shops ===")

        plugin.shopManager.searchShops(ShopManager.ShopSearchFilter())
            .thenAccept { allShops ->
                val sellShops = allShops.count { it.type == Shop.ShopType.SELL }

                sender.sendMessage("§7Total des shops: §e${allShops.size}")
                sender.sendMessage("§7Shops de vente: §e$sellShops")

                // Grouper par monde
                val shopsByWorld = allShops.groupBy { it.world }
                    .mapValues { it.value.size }

                sender.sendMessage("§7Répartition par monde:")
                shopsByWorld.forEach { (world, count) ->
                    sender.sendMessage("§7  - $world: §e$count")
                }
            }
    }

    private fun handleCache(sender: CommandSender) {
        sender.sendMessage("§6=== Informations du cache ===")

        // Afficher les statistiques du cache
        val cacheStats = plugin.cacheManager.getCacheStats()
        sender.sendMessage("§7Shops en cache: §e${cacheStats.totalCached}")
        sender.sendMessage("§7Entrées expirées: §e${cacheStats.expiredEntries}")
        sender.sendMessage("§7Total des accès: §e${cacheStats.totalAccesses}")

        if (cacheStats.totalCached > 0) {
            val expiredRatio = cacheStats.expiredEntries.toDouble() / cacheStats.totalCached * 100
            sender.sendMessage("§7Taux d'expiration: §e${String.format("%.1f%%", expiredRatio)}")
        }
    }
}
