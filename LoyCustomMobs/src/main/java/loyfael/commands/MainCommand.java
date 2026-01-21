package loyfael.commands;

import loyfael.LoyCustomMobs;
import loyfael.models.CustomMob;
import loyfael.models.MobRarity;
import loyfael.utils.PerformanceMonitor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for LoyCustomMobs
 */
public class MainCommand implements CommandExecutor, TabCompleter {

    private final LoyCustomMobs plugin;

    public MainCommand(LoyCustomMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("loycustommobs.admin")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelpMessage(sender);
            case "reload" -> handleReload(sender);
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "stats" -> handleStats(sender);
            case "debug" -> handleDebug(sender);
            case "performance" -> handlePerformance(sender);
            default -> sendHelpMessage(sender);
        }

        return true;
    }

    /**
     * Send help message
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6=== LoyCustomMobs Commands ===");
        sender.sendMessage("§e/loycustommobs help §7- Affiche cette aide");
        sender.sendMessage("§e/loycustommobs reload §7- Recharge la configuration");
        sender.sendMessage("§e/loycustommobs spawn <type> <rarity> §7- Spawn un mob personnalisé");
        sender.sendMessage("§e/loycustommobs remove <all|nearby> §7- Supprime des mobs");
        sender.sendMessage("§e/loycustommobs list §7- Liste les mobs actifs");
        sender.sendMessage("§e/loycustommobs stats §7- Affiche les statistiques");
        sender.sendMessage("§e/loycustommobs debug §7- Informations de debug");
        sender.sendMessage("§e/loycustommobs performance §7- Rapport de performance");
    }

    /**
     * Handle reload command
     */
    private void handleReload(CommandSender sender) {
        try {
            plugin.getConfigManager().loadConfigs();
            plugin.getMobManager().reload();
            plugin.getLootManager().reload();
            plugin.getGuiManager().reload();

            sender.sendMessage("§aConfiguration rechargée avec succès !");
        } catch (Exception e) {
            sender.sendMessage("§cErreur lors du rechargement : " + e.getMessage());
            plugin.getLogger().severe("Erreur lors du rechargement : " + e.getMessage());
        }
    }

    /**
     * Handle spawn command
     */
    private void handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUtilisation: /loycustommobs spawn <type> <rarity>");
            sender.sendMessage("§cTypes: ZOMBIE, SKELETON, CREEPER, etc.");
            sender.sendMessage("§cRaretés: Commun, Peu commun, Rare, Épique, Légendaire, Mythique");
            return;
        }

        try {
            EntityType entityType = EntityType.valueOf(args[1].toUpperCase());
            MobRarity rarity = MobRarity.valueOf(args[2].toUpperCase());

            Location spawnLocation = player.getLocation();
            CustomMob customMob = plugin.getMobManager().spawnCustomMob(spawnLocation, entityType, rarity);

            if (customMob != null) {
                sender.sendMessage("§aMob personnalisé spawné : §f" + rarity.getDisplayName() + " " + entityType.name());
            } else {
                sender.sendMessage("§cErreur lors du spawn du mob.");
            }

        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cType d'entité ou rareté invalide !");
        } catch (Exception e) {
            sender.sendMessage("§cErreur lors du spawn : " + e.getMessage());
        }
    }

    /**
     * Handle remove command
     */
    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUtilisation: /loycustommobs remove <all|nearby>");
            return;
        }

        int removed = 0;

        switch (args[1].toLowerCase()) {
            case "all" -> {
                Collection<CustomMob> activeMobs = plugin.getMobManager().getActiveMobs();
                for (CustomMob mob : new ArrayList<>(activeMobs)) {
                    if (mob.getEntity() != null && !mob.getEntity().isDead()) {
                        mob.getEntity().remove();
                        plugin.getMobManager().removeCustomMob(mob.getId());
                        removed++;
                    }
                }
            }
            case "nearby" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
                    return;
                }

                Collection<CustomMob> activeMobs = plugin.getMobManager().getActiveMobs();
                for (CustomMob mob : new ArrayList<>(activeMobs)) {
                    if (mob.getEntity() != null && !mob.getEntity().isDead() &&
                        mob.getEntity().getLocation().distance(player.getLocation()) <= 50) {
                        mob.getEntity().remove();
                        plugin.getMobManager().removeCustomMob(mob.getId());
                        removed++;
                    }
                }
            }
            default -> {
                sender.sendMessage("§cUtilisation: /loycustommobs remove <all|nearby>");
                return;
            }
        }

        sender.sendMessage("§a" + removed + " mobs personnalisés supprimés.");
    }

    /**
     * Handle list command
     */
    private void handleList(CommandSender sender) {
        Collection<CustomMob> activeMobs = plugin.getMobManager().getActiveMobs();

        if (activeMobs.isEmpty()) {
            sender.sendMessage("§cAucun mob personnalisé actif.");
            return;
        }

        sender.sendMessage("§6=== Mobs Personnalisés Actifs (" + activeMobs.size() + ") ===");

        int count = 0;
        for (CustomMob mob : activeMobs) {
            if (count >= 10) {
                sender.sendMessage("§7... et " + (activeMobs.size() - 10) + " autres");
                break;
            }

            String mobInfo = String.format("§7- §%c%s §f%s §7(Vies: %d, Abilities: %d)",
                getRarityColorChar(mob.getRarity()),
                mob.getRarity().getDisplayName(),
                mob.getEntity().getType().name(),
                mob.getLives(),
                mob.getAbilities().size()
            );

            sender.sendMessage(mobInfo);
            count++;
        }
    }

    /**
     * Handle stats command
     */
    private void handleStats(CommandSender sender) {
        var stats = plugin.getMobManager().getStatistics();

        sender.sendMessage("§6=== Statistiques LoyCustomMobs ===");
        sender.sendMessage("§eMobs actifs: §f" + stats.get("activeMobs"));
        sender.sendMessage("§eAbilities enregistrées: §f" + stats.get("registeredAbilities"));
        sender.sendMessage("§eStatut: §f" + (((Boolean) stats.get("enabled")) ? "§aActivé" : "§cDésactivé"));
        sender.sendMessage("§eChance de spawn: §f" + String.format("%.1f%%", ((Double) stats.get("spawnChance")) * 100));

    // Cache statistics if available
        try {
            var cacheStats = plugin.getMobManager().getCacheStats();
            if (cacheStats != null) {
                sender.sendMessage("§eCache: §f" + cacheStats.get("currentSize") + "/" + cacheStats.get("maxSize") +
                                 " (§a" + String.format("%.1f%%", (Double) cacheStats.get("fillPercentage")) + "§f)");
            }
        } catch (Exception e) {
            // Cache stats not available
        }
    }

    /**
     * Handle debug command
     */
    private void handleDebug(CommandSender sender) {
        sender.sendMessage("§6=== Debug LoyCustomMobs ===");
        sender.sendMessage("§eVersion du plugin: §f" + plugin.getPluginMeta().getVersion());
        sender.sendMessage("§eVersion du serveur: §f" + plugin.getServer().getVersion());
        sender.sendMessage("§eJoueurs en ligne: §f" + plugin.getServer().getOnlinePlayers().size());

        // Memory info
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        sender.sendMessage("§eMémoire utilisée: §f" + usedMemory + "MB / " + maxMemory + "MB");

        // Managers status
        sender.sendMessage("§eGestionnaires:");
        sender.sendMessage("§7  - MobManager: §a" + (plugin.getMobManager() != null ? "OK" : "ERREUR"));
        sender.sendMessage("§7  - LootManager: §a" + (plugin.getLootManager() != null ? "OK" : "ERREUR"));
        sender.sendMessage("§7  - GuiManager: §a" + (plugin.getGuiManager() != null ? "OK" : "ERREUR"));
        sender.sendMessage("§7  - ConfigManager: §a" + (plugin.getConfigManager() != null ? "OK" : "ERREUR"));
    }

    /**
     * Handle performance command
     */
    private void handlePerformance(CommandSender sender) {
        sender.sendMessage("§6=== Rapport de Performance ===");

        try {
            // Basic performance info
            sender.sendMessage("§eTPS estimé: §f" + String.format("%.2f", getCurrentTPS()));
            sender.sendMessage("§eMobs actifs: §f" + plugin.getMobManager().getActiveMobs().size());

            // Memory usage
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            double memoryPercent = (double) usedMemory / maxMemory * 100;

            String memoryColor = memoryPercent > 80 ? "§c" : memoryPercent > 60 ? "§e" : "§a";
            sender.sendMessage("§eMémoire: " + memoryColor + usedMemory + "MB / " + maxMemory + "MB " +
                             "(" + String.format("%.1f%%", memoryPercent) + ")");

        } catch (Exception e) {
            sender.sendMessage("§cErreur lors de la génération du rapport de performance.");
        }
    }

    /**
     * Get current TPS (approximation)
     */
    private double getCurrentTPS() {
        try {
            return plugin.getServer().getTPS()[0];
        } catch (Exception e) {
            return 20.0; // Default value if TPS not available
        }
    }

    /**
     * Get rarity color character
     */
    private char getRarityColorChar(MobRarity rarity) {
        return switch (rarity) {
            case COMMON -> 'f';      // White
            case UNCOMMON -> 'a';    // Green
            case RARE -> '9';        // Blue
            case EPIC -> 'd';        // Purple
            case LEGENDARY -> '6';   // Gold
            case MYTHIC -> 'c';      // Red
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("loycustommobs.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("help", "reload", "spawn", "remove", "list", "stats", "debug", "performance")
                    .stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "spawn" -> {
                    return Arrays.stream(EntityType.values())
                            .filter(type -> type.isSpawnable() && type.isAlive())
                            .map(EntityType::name)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "remove" -> {
                    return Arrays.asList("all", "nearby")
                            .stream()
                            .filter(option -> option.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("spawn")) {
            return Arrays.stream(MobRarity.values())
                    .map(MobRarity::name)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
