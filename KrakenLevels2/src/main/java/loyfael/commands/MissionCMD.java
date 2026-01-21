package loyfael.commands;

import loyfael.Main;
import loyfael.api.interfaces.IPlayerService;
import loyfael.api.interfaces.INotificationService;
import loyfael.api.interfaces.ISynchronizationService;
import loyfael.api.interfaces.IGuiService;
import loyfael.api.interfaces.IPlayerService.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

public class MissionCMD implements CommandExecutor, TabCompleter {

    // Services injectés via les interfaces
    private final INotificationService notificationService;
    private final IPlayerService playerService;
    private final IGuiService guiService;

    public MissionCMD() {
        Main main = Main.getInstance();
        this.notificationService = main.getNotificationService();
        this.playerService = main.getPlayerService();
        this.guiService = main.getGuiService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Par défaut, ouvrir le menu des missions si aucun argument
            if (sender instanceof Player) {
                return handleOpenCommand(sender, args);
            } else {
                showHelp(sender);
                return true;
            }
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "info":
                return handleInfoCommand(sender, args);
            case "reset":
                return handleResetCommand(sender, args);
            case "set":
                return handleSetCommand(sender, args);
            case "testsync":
                return handleTestSyncCommand(sender, args);
            case "player":
                return handlePlayerCommand(sender, args);
            default:
                showHelp(sender);
                return true;
        }
    }

    /**
     * Ouvre le menu des missions
     */
    private boolean handleOpenCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("krakenlevels.use")) {
            notificationService.sendMessage(player, "messages.no-permission");
            return true;
        }

        // Vérifier si le joueur existe, sinon le créer
        String playerUuid = player.getUniqueId().toString();
        if (!playerService.playerExists(playerUuid)) {
            playerService.createPlayer(playerUuid, player.getName());
        }

        // Utiliser le service GUI pour ouvrir le menu des niveaux/missions
        try {
            Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(), () -> {
                // Ouvrir le menu des missions via le service GUI
                java.util.Map<String, Object> parameters = new java.util.HashMap<>();
                parameters.put("playerUuid", playerUuid);
                guiService.openGui(player, "levels", parameters);
            });
        } catch (Exception e) {
            player.sendMessage("§cErreur lors de l'ouverture du menu: " + e.getMessage());
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        INotificationService notificationService = Main.getInstance().getServiceContainer().getService(INotificationService.class);
        if (sender instanceof Player) {
            notificationService.sendMessage((Player) sender, "commands.help.info");
            notificationService.sendMessage((Player) sender, "commands.help.reset");
            notificationService.sendMessage((Player) sender, "commands.help.set");
            notificationService.sendMessage((Player) sender, "commands.help.testsync");
            notificationService.sendMessage((Player) sender, "commands.help.player");
        } else {
            sender.sendMessage("§6=== Commandes Missions ===");
            sender.sendMessage("§e/mission info §7- Affiche vos informations");
            sender.sendMessage("§e/mission reset <joueur> §7- Reset un joueur");
            sender.sendMessage("§e/mission set <joueur> <niveau> §7- Définit le niveau");
            sender.sendMessage("§e/mission testsync [info|force|status|clear] [joueur] §7- Test synchronisation");
            sender.sendMessage("§e/mission player <pseudo> [serveur] §7- Voir niveau par serveur");
        }
    }

    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        Player player = (Player) sender;
        if (!hasPermissionForCommand(player, "info")) {
            notificationService.sendMessage(player, "messages.no-permission");
            return true;
        }

        String targetPlayerName = args.length > 1 ? args[1] : player.getName();
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        if (targetPlayer == null && !targetPlayerName.equals(player.getName())) {
            player.sendMessage("§cJoueur introuvable: " + targetPlayerName);
            return true;
        }

        String targetUuid = targetPlayer != null ? targetPlayer.getUniqueId().toString() : player.getUniqueId().toString();

        int level = playerService.getPlayerLevel(targetUuid);

        notificationService.sendMessage(player, "commands.mission-info.level", 
            targetPlayerName, String.valueOf(level));

        return true;
    }

    private boolean handleResetCommand(CommandSender sender, String[] args) {
        if (!hasPermissionForCommand(sender, "reset")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /mission reset <joueur>");
            return true;
        }

        String targetPlayerName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        if (targetPlayer == null) {
            sender.sendMessage("§cJoueur introuvable: " + targetPlayerName);
            return true;
        }

        String targetUuid = targetPlayer.getUniqueId().toString();
        IPlayerService playerService = Main.getInstance().getServiceContainer().getService(IPlayerService.class);

        try {
            playerService.setPlayerLevel(targetUuid, 0);
            sender.sendMessage("§aLe joueur " + targetPlayerName + " a été reset au niveau 0.");
            
            if (sender instanceof Player) {
                INotificationService notificationService = Main.getInstance().getServiceContainer().getService(INotificationService.class);
                notificationService.sendMessage(targetPlayer, "commands.reset.success");
            }
        } catch (Exception e) {
            sender.sendMessage("§cErreur lors du reset: " + e.getMessage());
        }

        return true;
    }

    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (!hasPermissionForCommand(sender, "set")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /mission set <joueur> <niveau>");
            return true;
        }

        String targetPlayerName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        if (targetPlayer == null) {
            sender.sendMessage("§cJoueur introuvable: " + targetPlayerName);
            return true;
        }

        try {
            int level = Integer.parseInt(args[2]);
            String targetUuid = targetPlayer.getUniqueId().toString();
            
            IPlayerService playerService = Main.getInstance().getServiceContainer().getService(IPlayerService.class);
            playerService.setPlayerLevel(targetUuid, level);
            
            sender.sendMessage("§aLe niveau de " + targetPlayerName + " a été défini à " + level + ".");
            
            if (sender instanceof Player) {
                INotificationService notificationService = Main.getInstance().getServiceContainer().getService(INotificationService.class);
                notificationService.sendMessage(targetPlayer, "commands.set.success", String.valueOf(level));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cLe niveau doit être un nombre valide.");
        } catch (Exception e) {
            sender.sendMessage("§cErreur lors de la définition du niveau: " + e.getMessage());
        }

        return true;
    }

    private boolean handleTestSyncCommand(CommandSender sender, String[] args) {
        if (!hasPermissionForCommand(sender, "testsync")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§6=== Commandes Test Sync ===");
            sender.sendMessage("§e/mission testsync info <joueur> §7- Métadonnées de sync");
            sender.sendMessage("§e/mission testsync force <joueur> §7- Force une synchronisation");
            sender.sendMessage("§e/mission testsync status §7- État du service");
            sender.sendMessage("§e/mission testsync clear <joueur> §7- Vide cache + sync complète");
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "info":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /mission testsync info <joueur>");
                    return true;
                }
                return handleTestSyncInfo(sender, args[2]);

            case "force":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /mission testsync force <joueur>");
                    return true;
                }
                return handleTestSyncForce(sender, args[2]);

            case "status":
                return handleTestSyncStatus(sender);

            case "clear":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /mission testsync clear <joueur>");
                    return true;
                }
                return handleTestSyncClear(sender, args[2]);

            default:
                sender.sendMessage("§cAction invalide. Utilisez: info, force, status, clear");
                return true;
        }
    }

    private boolean handlePlayerCommand(CommandSender sender, String[] args) {
        if (!hasPermissionForCommand(sender, "player")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /mission player <pseudo> [serveur]");
            return true;
        }

        String targetPlayerName = args[1];

        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage("§cJoueur introuvable: " + targetPlayerName);
            return true;
        }

        String targetUuid = targetPlayer.getUniqueId().toString();

        try {
            sender.sendMessage("§6=== Informations Mission - " + targetPlayerName + " ===");

            // Informations serveur actuel
            IPlayerService playerService = Main.getInstance().getServiceContainer().getService(IPlayerService.class);
            int currentLevel = playerService.getPlayerLevel(targetUuid);
            
            String currentServer = Main.getInstance().getConfigurationService().getConfig().getString("server.name", "serveur-actuel");
            sender.sendMessage("§e📍 Serveur actuel (" + currentServer + "):");
            sender.sendMessage("  §7├─ Niveau: §a" + currentLevel);
            sender.sendMessage("  §7└─ Statut: §aConnecté");

            // Récupération des données de synchronisation
            sender.sendMessage("§e🔄 Récupération des données de synchronisation...");
            
            // Essayons de récupérer directement les données du joueur
            Optional<PlayerData> playerDataOpt = playerService.getPlayerData(targetUuid);
            
            if (playerDataOpt.isPresent()) {
                PlayerData playerData = playerDataOpt.get();
                sender.sendMessage("§e📊 Données de synchronisation:");
                sender.sendMessage("  §7├─ Niveau: §a" + playerData.getLevel());
                sender.sendMessage("  §7├─ Dernière connexion: §a" + new java.util.Date(playerData.getLastSeen()));
                
                if (playerData.getLevel() != currentLevel) {
                    sender.sendMessage("  §7└─ §c⚠️ DÉSYNCHRONISATION DÉTECTÉE!");
                } else {
                    sender.sendMessage("  §7└─ §a✅ Synchronisé");
                }
            } else {
                sender.sendMessage("  §7└─ §c❌ Aucune donnée trouvée dans la base");
            }

        } catch (Exception e) {
            sender.sendMessage("§cErreur lors de la récupération des informations: " + e.getMessage());
        }

        return true;
    }

    private boolean handleTestSyncInfo(CommandSender sender, String playerName) {
        try {
            Player targetPlayer = Bukkit.getPlayerExact(playerName);
            if (targetPlayer == null) {
                sender.sendMessage("§cJoueur introuvable: " + playerName);
                return true;
            }

            String playerUuid = targetPlayer.getUniqueId().toString();
            sender.sendMessage("§6=== Info Sync - " + playerName + " ===");
            
            ISynchronizationService syncService = Main.getInstance().getServiceContainer().getService(ISynchronizationService.class);
            syncService.forceSync(playerUuid).thenAccept(result -> {
                if (result) {
                    sender.sendMessage("§a✅ Synchronisation réussie pour " + playerName);
                } else {
                    sender.sendMessage("§c❌ Échec de la synchronisation pour " + playerName);
                }
            }).exceptionally(throwable -> {
                sender.sendMessage("§cErreur lors de la synchronisation: " + throwable.getMessage());
                return null;
            });

        } catch (Exception e) {
            sender.sendMessage("§cErreur: " + e.getMessage());
        }
        return true;
    }

    private boolean handleTestSyncForce(CommandSender sender, String playerName) {
        try {
            Player targetPlayer = Bukkit.getPlayerExact(playerName);
            if (targetPlayer == null) {
                sender.sendMessage("§cJoueur introuvable: " + playerName);
                return true;
            }

            String playerUuid = targetPlayer.getUniqueId().toString();
            sender.sendMessage("§e🔄 Force la synchronisation de " + playerName + "...");
            
            ISynchronizationService syncService = Main.getInstance().getServiceContainer().getService(ISynchronizationService.class);
            syncService.forceSync(playerUuid).thenAccept(result -> {
                if (result) {
                    sender.sendMessage("§a✅ Synchronisation forcée réussie pour " + playerName);
                } else {
                    sender.sendMessage("§c❌ Échec de la synchronisation forcée pour " + playerName);
                }
            }).exceptionally(throwable -> {
                sender.sendMessage("§cErreur lors de la synchronisation forcée: " + throwable.getMessage());
                return null;
            });

        } catch (Exception e) {
            sender.sendMessage("§cErreur: " + e.getMessage());
        }
        return true;
    }

    private boolean handleTestSyncStatus(CommandSender sender) {
        try {
            boolean dbConnected = Main.getInstance().getDatabaseService() != null
                && Main.getInstance().getDatabaseService().isConnected();
            boolean useMongo = Main.getInstance().getConfig().getBoolean("database.use-mongodb", true);

            sender.sendMessage("§6=== Synchronization Service Status ===");
            sender.sendMessage("§7Synchronization service: §aActive");
            if (useMongo) {
                sender.sendMessage("§7MongoDB: " + (dbConnected ? "§aConnected" : "§cNot connected"));
                if (!dbConnected) {
                    sender.sendMessage("§7Hint: Check MongoDB host/port/credentials in config.yml or disable database.use-mongodb to use YAML storage.");
                }
            } else {
                sender.sendMessage("§7Storage: §eYAML files (MongoDB disabled)");
            }
            sender.sendMessage("§7Cache: §aOperational");
            
        } catch (Exception e) {
            sender.sendMessage("§cError while checking status: " + e.getMessage());
        }
        return true;
    }

    private boolean handleTestSyncClear(CommandSender sender, String playerName) {
        try {
            Player targetPlayer = Bukkit.getPlayerExact(playerName);
            if (targetPlayer == null) {
                sender.sendMessage("§cJoueur introuvable: " + playerName);
                return true;
            }

            String playerUuid = targetPlayer.getUniqueId().toString();
            sender.sendMessage("§e🧹 Nettoyage du cache et synchronisation complète de " + playerName + "...");
            
            // Vider le cache du joueur
            Main.getInstance().getServiceContainer().getService(loyfael.api.interfaces.ICacheService.class)
                .invalidatePlayer(playerUuid);
            
            // Forcer une synchronisation
            ISynchronizationService syncService = Main.getInstance().getServiceContainer().getService(ISynchronizationService.class);
            syncService.forceSync(playerUuid).thenAccept(result -> {
                if (result) {
                    sender.sendMessage("§a✅ Cache vidé et synchronisation complète réussie pour " + playerName);
                } else {
                    sender.sendMessage("§c❌ Cache vidé mais échec de la synchronisation pour " + playerName);
                }
            }).exceptionally(throwable -> {
                sender.sendMessage("§cErreur lors du clear: " + throwable.getMessage());
                return null;
            });

        } catch (Exception e) {
            sender.sendMessage("§cErreur: " + e.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            // Première suggestion : sous-commandes
            List<String> subCommands = Arrays.asList("info", "reset", "set", "testsync", "player");
            suggestions.addAll(subCommands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList()));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            // Suggestions pour les commandes qui prennent un nom de joueur
            if (Arrays.asList("info", "reset", "set", "player").contains(subCommand)) {
                suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList()));
            } else if ("testsync".equals(subCommand)) {
                List<String> syncActions = Arrays.asList("info", "force", "status", "clear");
                suggestions.addAll(syncActions.stream()
                    .filter(action -> action.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if ("testsync".equals(subCommand) && !args[1].equals("status")) {
                // Pour testsync info, force, clear - suggérer des noms de joueurs
                suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList()));
            } else if ("player".equals(subCommand)) {
                // Pour la commande player, suggérer des serveurs
                suggestions.add("lobby-1");
                suggestions.add("lobby-2");
                suggestions.add("survival-1");
            }
        }

        return suggestions;
    }

    private boolean hasPermissionForCommand(CommandSender sender, String subCommand) {
        switch (subCommand) {
            case "info":
                return sender.hasPermission("krakenlevels.use");
            case "reset":
                return sender.hasPermission("krakenlevels.reset");
            case "set":
                return sender.hasPermission("krakenlevels.set");
            case "testsync":
                return sender.hasPermission("krakenlevels.testsync");
            case "player":
                return sender.hasPermission("krakenlevels.admin.player");
            default:
                return false;
        }
    }
}
