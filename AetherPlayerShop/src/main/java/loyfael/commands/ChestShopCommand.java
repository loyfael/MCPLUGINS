package loyfael.commands;

import loyfael.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande principale du plugin AetherPlayerShop
 */
public class ChestShopCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public ChestShopCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                           @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            if (sender instanceof Player player) {
                // Ouvrir le menu principal
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    new loyfael.gui.ShopMenuGUI(plugin).openMainMenu(player);
                });
                return true;
            } else {
                showHelp(sender);
                return true;
            }
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help", "aide" -> {
                showHelp(sender);
                return true;
            }

            case "info" -> {
                if (sender instanceof Player player) {
                    showPlayerInfo(player);
                } else {
                    sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
                }
                return true;
            }

            case "reload" -> {
                if (!sender.hasPermission("aetherplayershop.admin")) {
                    sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
                    return true;
                }

                plugin.getConfigManager().reload();
                plugin.getCacheManager().invalidateCache();
                sender.sendMessage("§aConfiguration rechargée avec succès!");
                return true;
            }

            case "stats" -> {
                if (!sender.hasPermission("aetherplayershop.admin")) {
                    sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
                    return true;
                }

                showStats(sender);
                return true;
            }

            case "delete" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
                    return true;
                }

                if (!player.hasPermission("aetherplayershop.manage")) {
                    player.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
                    return true;
                }

                handleDeleteCommand(player, args);
                return true;
            }

            default -> {
                sender.sendMessage("§cCommande inconnue. Utilisez §e/chestshop help §cpour l'aide.");
                return true;
            }
        }
    }

    private void showHelp(@NotNull CommandSender sender) {
        sender.sendMessage("§6§l=== AetherPlayerShop - Aide ===");
        sender.sendMessage("§e/chestshop §7- Ouvre le menu principal des shops");
        sender.sendMessage("§e/chestshop info §7- Affiche vos informations de shops");
        sender.sendMessage("§e/chestshop help §7- Affiche cette aide");

        if (sender.hasPermission("aetherplayershop.manage")) {
            sender.sendMessage("§e/chestshop delete §7- Supprime un de vos shops");
        }

        if (sender.hasPermission("aetherplayershop.admin")) {
            sender.sendMessage("§c/chestshop reload §7- Recharge la configuration");
            sender.sendMessage("§c/chestshop stats §7- Affiche les statistiques du plugin");
        }

        sender.sendMessage("");
        sender.sendMessage("§7Pour créer un shop:");
        sender.sendMessage("§71. Placez un coffre avec les items à vendre");
        sender.sendMessage("§72. Placez une pancarte à côté du coffre");
        sender.sendMessage("§73. Écrivez sur la pancarte:");
        sender.sendMessage("§7   Ligne 1: §e[SELL] §7ou §e[BUY]");
        sender.sendMessage("§7   Ligne 2: §eprix (ex: 10◎)");
        sender.sendMessage("§7   Ligne 3: §equantité (ex: 64)");
        sender.sendMessage("§7   Ligne 4: §elaissez vide");
    }

    private void showPlayerInfo(@NotNull Player player) {
        plugin.getShopManager().getPlayerShops(player.getUniqueId())
            .thenAccept(shops -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§6§l=== Vos Shops ===");
                    player.sendMessage("§7Nombre de shops: §e" + shops.size() + "§7/§e" +
                        plugin.getConfigManager().getMaxShopsPerPlayer());

                    if (shops.isEmpty()) {
                        player.sendMessage("§7Vous n'avez aucun shop actif.");
                        player.sendMessage("§7Créez un shop en plaçant une pancarte à côté d'un coffre!");
                        return;
                    }

                    player.sendMessage("§7Liste de vos shops:");
                    for (int i = 0; i < Math.min(shops.size(), 10); i++) {
                        var shop = shops.get(i);
                        String status = shop.isActive() ? "§aActif" : "§cInactif";
                        player.sendMessage("§8" + (i + 1) + ". §e" + shop.getItem().getDisplayName() +
                            " §7- " + status + " §7- §e" + String.format("%.2f", shop.getPrice()) + "§6◎");
                    }

                    if (shops.size() > 10) {
                        player.sendMessage("§7... et " + (shops.size() - 10) + " autres shops.");
                    }
                });
            });
    }

    private void showStats(@NotNull CommandSender sender) {
        // Récupération des statistiques du cache
        var cacheStats = plugin.getCacheManager().getCacheStats();

        sender.sendMessage("§6§l=== Statistiques AetherPlayerShop ===");
        sender.sendMessage("§7Cache:");
        sender.sendMessage("§7  - Shops en cache: §e" + cacheStats.totalCached);
        sender.sendMessage("§7  - Entrées expirées: §e" + cacheStats.expiredEntries);
        sender.sendMessage("§7  - Total accès: §e" + cacheStats.totalAccesses);

        // Statistiques MongoDB (asynchrone)
        plugin.getMongoManager().executeAsync(() -> {
            try {
                long totalShops = plugin.getMongoManager().getShopsCollection().countDocuments();
                long activeShops = plugin.getMongoManager().getShopsCollection()
                    .countDocuments(new org.bson.Document("active", true));
                long totalTransactions = plugin.getMongoManager().getTransactionsCollection().countDocuments();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§7Base de données:");
                    sender.sendMessage("§7  - Total shops: §e" + totalShops);
                    sender.sendMessage("§7  - Shops actifs: §e" + activeShops);
                    sender.sendMessage("§7  - Total transactions: §e" + totalTransactions);
                });
            } catch (Exception e) {
                sender.sendMessage("§cErreur lors de la récupération des statistiques MongoDB.");
            }
            return null;
        });
    }

    private void handleDeleteCommand(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUtilisation: /chestshop delete <ID>");
            player.sendMessage("§7Utilisez §e/chestshop info §7pour voir vos shops avec leurs IDs.");
            return;
        }

        String shopId = args[1];

        // Vérification que le shop appartient au joueur
        plugin.getShopManager().getShop(shopId)
            .thenAccept(shop -> {
                if (shop == null) {
                    player.sendMessage("§cShop introuvable.");
                    return;
                }

                if (!shop.getOwnerUUID().equals(player.getUniqueId()) &&
                    !player.hasPermission("aetherplayershop.admin")) {
                    player.sendMessage("§cCe shop ne vous appartient pas.");
                    return;
                }

                // Suppression du shop
                plugin.getShopManager().deleteShop(shopId)
                    .thenAccept(success -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (success) {
                                player.sendMessage("§aShop supprimé avec succès!");
                            } else {
                                player.sendMessage("§cErreur lors de la suppression du shop.");
                            }
                        });
                    });
            });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                              @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = Arrays.asList("help", "info");

            if (sender.hasPermission("aetherplayershop.manage")) {
                suggestions.add("delete");
            }

            if (sender.hasPermission("aetherplayershop.admin")) {
                suggestions.add("reload");
                suggestions.add("stats");
            }

            return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        return null;
    }
}
