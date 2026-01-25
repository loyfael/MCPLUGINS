package loyfael.commands

import loyfael.Main
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachmentInfo

/**
 * Commande principale du plugin AetherPlayerShop
 */
class ChestShopCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            if (sender is Player) {
                // Ouvrir le menu principal
                plugin.server.scheduler.runTask(plugin, Runnable {
                    plugin.shopMenuGUI.openMainMenu(sender)
                })
                return true
            } else {
                showHelp(sender)
                return true
            }
        }

        val subCommand = args[0].lowercase()

        return when (subCommand) {
            "help", "aide" -> {
                showHelp(sender)
                true
            }

            "info" -> {
                if (sender is Player) {
                    showPlayerInfo(sender)
                } else {
                    sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.")
                }
                true
            }

            "reload" -> {
                if (!sender.hasPermission("aetherplayershop.admin")) {
                    sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.")
                    return true
                }

                plugin.configManager.reload()
                plugin.cacheManager.invalidateCache()
                sender.sendMessage("§aConfiguration rechargée avec succès!")
                true
            }

            "stats" -> {
                if (!sender.hasPermission("aetherplayershop.admin")) {
                    sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.")
                    return true
                }

                showStats(sender)
                true
            }

            "delete" -> {
                if (sender !is Player) {
                    sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.")
                    return true
                }

                if (!sender.hasPermission("aetherplayershop.manage")) {
                    sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.")
                    return true
                }

                handleDeleteCommand(sender, args)
                true
            }

            else -> {
                sender.sendMessage("§cCommande inconnue. Utilisez §e/chestshop help §cpour l'aide.")
                true
            }
        }
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== AetherPlayerShop - Aide ===")
        sender.sendMessage("§e/chestshop §7- Ouvre le menu principal des shops")
        sender.sendMessage("§e/chestshop info §7- Affiche vos informations de shops")
        sender.sendMessage("§e/chestshop help §7- Affiche cette aide")

        if (sender.hasPermission("aetherplayershop.manage")) {
            sender.sendMessage("§e/chestshop delete §7- Supprime un de vos shops")
        }

        if (sender.hasPermission("aetherplayershop.admin")) {
            sender.sendMessage("§c/chestshop reload §7- Recharge la configuration")
            sender.sendMessage("§c/chestshop stats §7- Affiche les statistiques du plugin")
        }

        sender.sendMessage("")
        sender.sendMessage("§7Pour créer un shop:")
        sender.sendMessage("§71. Remplis un coffre avec §eun seul type d'item")
        sender.sendMessage("§72. Prends une §epancarte §7en main")
        sender.sendMessage("§73. §eClic droit §7sur le coffre (debout) pour ouvrir la création")
        sender.sendMessage("§74. §8Astuce: accroupi = placement normal du panneau")
    }

    private fun showPlayerInfo(player: Player) {
        plugin.shopManager.getPlayerShops(player.uniqueId)
            .thenAccept { shops ->
                plugin.server.scheduler.runTask(plugin, Runnable {
                    val max = getMaxShopsFromPermissions(player, plugin.configManager.maxShopsPerPlayer)
                    player.sendMessage("§6§l=== Vos Shops ===")
                    player.sendMessage("§7Nombre de shops: §e${shops.size}§7/§e$max")

                    if (shops.isEmpty()) {
                        player.sendMessage("§7Vous n'avez aucun shop actif.")
                        player.sendMessage("§7Créez un shop en plaçant une pancarte à côté d'un coffre!")
                        return@Runnable
                    }

                    player.sendMessage("§7Liste de vos shops:")
                    for (i in 0 until minOf(shops.size, 10)) {
                        val shop = shops[i]
                        val status = if (shop.active) "§aActif" else "§cInactif"
                        player.sendMessage(
                            "§8${i + 1}. §e${shop.item.displayName} §7- $status §7- §e${
                                String.format(
                                    "%.2f",
                                    shop.price
                                )
                            }§6◎"
                        )
                    }

                    if (shops.size > 10) {
                        player.sendMessage("§7... et ${shops.size - 10} autres shops.")
                    }
                })
            }
    }

    private fun showStats(sender: CommandSender) {
        // Récupération des statistiques du cache
        val cacheStats = plugin.cacheManager.getCacheStats()

        sender.sendMessage("§6§l=== Statistiques AetherPlayerShop ===")
        sender.sendMessage("§7Cache:")
        sender.sendMessage("§7  - Shops en cache: §e${cacheStats.totalCached}")
        sender.sendMessage("§7  - Entrées expirées: §e${cacheStats.expiredEntries}")
        sender.sendMessage("§7  - Total accès: §e${cacheStats.totalAccesses}")

        // Statistiques MySQL (asynchrone)
        plugin.mySqlManager.supplyAsync<Unit> {
            var totalShops = 0L
            var activeShops = 0L
            var totalTransactions = 0L

            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement("SELECT COUNT(*) FROM shops").use { ps ->
                        ps.executeQuery().use { rs ->
                            if (rs.next()) totalShops = rs.getLong(1)
                        }
                    }
                    conn.prepareStatement("SELECT COUNT(*) FROM shops WHERE active=1").use { ps ->
                        ps.executeQuery().use { rs ->
                            if (rs.next()) activeShops = rs.getLong(1)
                        }
                    }
                    conn.prepareStatement("SELECT COUNT(*) FROM transactions").use { ps ->
                        ps.executeQuery().use { rs ->
                            if (rs.next()) totalTransactions = rs.getLong(1)
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage("§cErreur lors de la récupération des statistiques MySQL.")
                })
                return@supplyAsync
            }

            val fTotalShops = totalShops
            val fActiveShops = activeShops
            val fTotalTransactions = totalTransactions

            plugin.server.scheduler.runTask(plugin, Runnable {
                sender.sendMessage("§7Base de données:")
                sender.sendMessage("§7  - Total shops: §e$fTotalShops")
                sender.sendMessage("§7  - Shops actifs: §e$fActiveShops")
                sender.sendMessage("§7  - Total transactions: §e$fTotalTransactions")
            })
        }
    }

    private fun handleDeleteCommand(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§cUtilisation: /chestshop delete <ID>")
            player.sendMessage("§7Utilisez §e/chestshop info §7pour voir vos shops avec leurs IDs.")
            return
        }

        val shopId = args[1]

        // Vérification que le shop appartient au joueur
        plugin.shopManager.getShop(shopId)
            .thenAccept { shop ->
                if (shop == null) {
                    player.sendMessage("§cShop introuvable.")
                    return@thenAccept
                }

                if (shop.ownerUUID != player.uniqueId && !player.hasPermission("aetherplayershop.admin")) {
                    player.sendMessage("§cCe shop ne vous appartient pas.")
                    return@thenAccept
                }

                // Suppression du shop
                plugin.shopManager.deleteShop(shopId)
                    .thenAccept { success ->
                        plugin.server.scheduler.runTask(plugin, Runnable {
                            if (success) {
                                player.sendMessage("§aShop supprimé avec succès!")
                            } else {
                                player.sendMessage("§cErreur lors de la suppression du shop.")
                            }
                        })
                    }
            }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            val suggestions = mutableListOf("help", "info")

            if (sender.hasPermission("aetherplayershop.manage")) {
                suggestions.add("delete")
            }

            if (sender.hasPermission("aetherplayershop.admin")) {
                suggestions.add("reload")
                suggestions.add("stats")
            }

            return suggestions.filter { it.lowercase().startsWith(args[0].lowercase()) }
        }

        return null
    }

    private fun getMaxShopsFromPermissions(player: Player, fallback: Int): Int {
        var max = fallback
        for (pai: PermissionAttachmentInfo in player.effectivePermissions) {
            val perm = pai.permission
            if (!perm.startsWith("aetherplayershop.shops.")) continue
            val tail = perm.substring("aetherplayershop.shops.".length)
            try {
                max = maxOf(max, tail.toInt())
            } catch (ignored: NumberFormatException) {
            }
        }
        return max
    }
}
