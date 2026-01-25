package loyfael.commands;

import loyfael.Main;
import loyfael.model.Shop;
import loyfael.manager.ShopManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Commande de diagnostic pour les shops - utile pour déboguer les problèmes
 */
public class ShopDebugCommand implements CommandExecutor {

    private final Main plugin;

    public ShopDebugCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                           @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("aetherplayershop.debug")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6=== Commandes de diagnostic AetherPlayerShop ===");
            sender.sendMessage("§e/shopdebug lookup §7- Recherche le shop à votre position");
            sender.sendMessage("§e/shopdebug cleanup <x> <y> <z> §7- Nettoie les shops autour d'une position");
            sender.sendMessage("§e/shopdebug ghost §7- Nettoie les shops fantômes à votre position");
            sender.sendMessage("§e/shopdebug stats §7- Affiche les statistiques des shops");
            sender.sendMessage("§e/shopdebug cache §7- Affiche les informations du cache");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "lookup" -> handleLookup(sender);
            case "cleanup" -> handleCleanup(sender, args);
            case "ghost" -> handleGhostCleanup(sender);
            case "stats" -> handleStats(sender);
            case "cache" -> handleCache(sender);
            default -> {
                sender.sendMessage("§cSous-commande inconnue. Utilisez /shopdebug pour voir l'aide.");
                return true;
            }
        }

        return true;
    }

    private void handleLookup(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
            return;
        }

        Location loc = player.getLocation();
        sender.sendMessage("§6=== Diagnostic Shop à votre position ===");
        sender.sendMessage("§7Position: " + loc.getWorld().getName() + " " +
            loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());

        plugin.getShopManager().getShopAtLocation(loc)
            .thenAccept(shop -> {
                if (shop != null) {
                    sender.sendMessage("§a✓ Shop trouvé:");
                    sender.sendMessage("§7- ID: §e" + shop.getId());
                    sender.sendMessage("§7- Propriétaire: §e" + shop.getOwnerName());
                    sender.sendMessage("§7- Type: §e" + shop.getType());
                    sender.sendMessage("§7- Prix: §e" + shop.getPrice() + "◎");
                    sender.sendMessage("§7- Stock: §e" + shop.getStock());
                    sender.sendMessage("§7- Actif: §e" + shop.isActive());
                } else {
                    sender.sendMessage("§c✗ Aucun shop trouvé à cette position.");

                    // Compter tous les shops dans la base
                    plugin.getShopManager().searchShops(new ShopManager.ShopSearchFilter())
                        .thenAccept(allShops -> {
                            sender.sendMessage("§7Total shops en base: §e" + allShops.size());
                        });
                }
            });
    }

    private void handleCleanup(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length != 4) {
            sender.sendMessage("§cUsage: /shopdebug cleanup <x> <y> <z>");
            return;
        }

        try {
            int x = Integer.parseInt(args[1]);
            int y = Integer.parseInt(args[2]);
            int z = Integer.parseInt(args[3]);

            Location loc;
            if (sender instanceof Player player) {
                loc = new Location(player.getWorld(), x, y, z);
            } else {
                sender.sendMessage("§cCette commande nécessite un monde. Utilisez en tant que joueur.");
                return;
            }

            sender.sendMessage("§6Nettoyage forcé des shops autour de " + x + "," + y + "," + z);

            plugin.getShopManager().forceDeleteShopAtLocation(loc)
                .thenAccept(success -> {
                    if (success) {
                        sender.sendMessage("§a✓ Nettoyage effectué avec succès.");
                    } else {
                        sender.sendMessage("§c✗ Aucun shop trouvé à nettoyer.");
                    }
                });

        } catch (NumberFormatException e) {
            sender.sendMessage("§cCoordonnées invalides. Utilisez des nombres entiers.");
        }
    }

    private void handleGhostCleanup(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
            return;
        }

        Location loc = player.getLocation();
        sender.sendMessage("§6=== Nettoyage des shops fantômes à votre position ===");
        sender.sendMessage("§7Position: " + loc.getWorld().getName() + " " +
            loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());

        plugin.getShopManager().cleanupGhostShopsAtLocation(loc)
            .thenAccept(result -> {
                if (result > 0) {
                    sender.sendMessage("§a✓ " + result + " shop(s) fantôme(s) nettoyé(s).");
                } else {
                    sender.sendMessage("§c✗ Aucun shop fantôme trouvé à cette position.");
                }
            });
    }

    private void handleStats(@NotNull CommandSender sender) {
        sender.sendMessage("§6=== Statistiques des shops ===");

        plugin.getShopManager().searchShops(new ShopManager.ShopSearchFilter())
            .thenAccept(allShops -> {
                long sellShops = allShops.stream().filter(s -> s.getType() == Shop.ShopType.SELL).count();

                sender.sendMessage("§7Total des shops: §e" + allShops.size());
                sender.sendMessage("§7Shops de vente: §e" + sellShops);

                // Grouper par monde
                Map<String, Long> shopsByWorld = allShops.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        Shop::getWorld,
                        java.util.stream.Collectors.counting()
                    ));

                sender.sendMessage("§7Répartition par monde:");
                shopsByWorld.forEach((world, count) ->
                    sender.sendMessage("§7  - " + world + ": §e" + count)
                );
            });
    }

    private void handleCache(@NotNull CommandSender sender) {
        sender.sendMessage("§6=== Informations du cache ===");

        // Afficher les statistiques du cache
        var cacheStats = plugin.getCacheManager().getCacheStats();
        sender.sendMessage("§7Shops en cache: §e" + cacheStats.totalCached);
        sender.sendMessage("§7Entrées expirées: §e" + cacheStats.expiredEntries);
        sender.sendMessage("§7Total des accès: §e" + cacheStats.totalAccesses);

        if (cacheStats.totalCached > 0) {
            double expiredRatio = (double) cacheStats.expiredEntries / cacheStats.totalCached * 100;
            sender.sendMessage("§7Taux d'expiration: §e" + String.format("%.1f%%", expiredRatio));
        }
    }
}
