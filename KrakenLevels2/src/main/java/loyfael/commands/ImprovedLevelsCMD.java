package loyfael.commands;

import loyfael.Main;
import loyfael.api.interfaces.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import loyfael.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Commande principale refactorisée avec architecture SOLID
 * Principe de responsabilité unique : gestion des commandes uniquement
 * Principe d'inversion de dépendance : utilise les services via interfaces
 */
public final class ImprovedLevelsCMD implements CommandExecutor, TabCompleter {

    // Services injectés via les interfaces (inversion de dépendance)
    private final IConfigurationService configService;
    private final IPlayerService playerService;
    private final INotificationService notificationService;
    private final ICacheService cacheService;
    private final IDatabaseService databaseService;
    private final IGuiService guiService; // Service GUI ajouté
    private final IMissionService missionService; // Service Mission ajouté
    private final ILevelsConfigService levelsConfigService;

    // Sous-commandes disponibles
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "help", "reload", "open", "stats", "reset", "give", "setlevel", "info", "cache", "top", "missions", "testmoney", "testext", "testprogress", "testsync", "player"
    );

    public ImprovedLevelsCMD() {
        Main main = Main.getInstance();
        this.configService = main.getConfigurationService();
        this.playerService = main.getPlayerService();
        this.notificationService = main.getNotificationService();
        this.cacheService = main.getCacheService();
        this.databaseService = main.getDatabaseService();
        this.guiService = main.getGuiService(); // Récupération du service GUI
        this.missionService = main.getMissionService(); // Récupération du service de missions
        this.levelsConfigService = main.getServiceContainer().getService(ILevelsConfigService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Validation de base
        if (args.length == 0) {
            if (sender instanceof Player) {
                return handleOpenCommand(sender, args);
            } else {
                return handleHelpCommand(sender, args);
            }
        }

        String subCommand = args[0].toLowerCase();

        // Dispatcher des sous-commandes avec validation des permissions
      return switch (subCommand) {
        case "help" -> handleHelpCommand(sender, args);
        case "reload" -> handleReloadCommand(sender, args);
        case "open" -> handleOpenCommand(sender, args);
        case "stats" -> handleStatsCommand(sender, args);
        case "reset" -> handleResetCommand(sender, args);
        case "give" -> handleGiveCommand(sender, args);
    case "setlevel" -> handleSetLevelCommand(sender, args);
        case "info" -> handleInfoCommand(sender, args);
        case "cache" -> handleCacheCommand(sender, args);
        case "top" -> handleTopCommand(sender, args);
        case "missions" -> handleMissionsCommand(sender, args);
        case "testmoney" -> handleTestMoneyCommand(sender, args);
        case "testext" -> handleTestExternalCommand(sender, args); // Nouvelle commande de test
        case "testprogress" -> handleTestProgressCommand(sender, args); // Test progression missions
        case "testsync" -> handleTestSyncCommand(sender, args); // Test synchronisation inter-serveur
        case "player" -> handlePlayerCommand(sender, args); // Voir les niveaux d'un joueur par serveur
        default -> {
          notificationService.sendMessage((Player) sender, "commands.unknown", subCommand);
          yield true;
        }
      };
    }

    /**
     * Affiche l'aide des commandes
     */
    private boolean handleHelpCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("krakenlevels.help")) {
            notificationService.sendMessage((Player) sender, "commands.no-permission");
            return true;
        }

        notificationService.sendMessage((Player) sender, "commands.help.header");
        for (String cmd : SUBCOMMANDS) {
            if (hasPermissionForCommand(sender, cmd)) {
                notificationService.sendMessage((Player) sender, "commands.help." + cmd);
            }
        }
        return true;
    }

    /**
     * Recharge la configuration
     */
    private boolean handleReloadCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("krakenlevels.admin.reload")) {
            notificationService.sendMessage((Player) sender, "commands.no-permission");
            return true;
        }

        CompletableFuture.runAsync(() -> {
            try {
                configService.reload();
                notificationService.reloadMessages();

                // Reconnect to MongoDB if it's the active backend (ensures new host/port changes are applied)
                try {
                    IDatabaseService db = Main.getInstance().getDatabaseService();
                    if (db instanceof loyfael.core.services.MongoDatabaseService mongoDb) {
                        String beforeHost = mongoDb.getLastHost();
                        int beforePort = mongoDb.getLastPort();
                        db.disconnect();
                        boolean ok = db.initialize();
                        String afterHost = (mongoDb.getLastHost() == null ? "?" : mongoDb.getLastHost());
                        int afterPort = mongoDb.getLastPort();
                        Utils.sendConsoleLog("&eReload MongoDB: before=" + beforeHost + ":" + beforePort + " -> after=" + afterHost + ":" + afterPort + " status=" + (ok?"OK":"FAIL"));
                    }
                } catch (Exception ex) {
                    Utils.sendConsoleLog("&cErreur lors du rechargement MongoDB: " + ex.getMessage());
                }

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    notificationService.sendMessage((Player) sender, "commands.reload.success");
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    notificationService.sendMessage((Player) sender, "commands.reload.error", e.getMessage());
                });
            }
        });

        return true;
    }

    /**
     * Ouvre l'interface graphique des niveaux
     */
    private boolean handleOpenCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            notificationService.logConsole("commands.player-only");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("krakenlevels.use")) {
            notificationService.sendMessage(player, "commands.no-permission");
            return true;
        }

        // Vérifier si le joueur existe, sinon le créer
        String playerUuid = player.getUniqueId().toString();
        if (!playerService.playerExists(playerUuid)) {
            playerService.createPlayer(playerUuid, player.getName());
        }

        // Utiliser le service GUI pour ouvrir le menu des niveaux
        try {
            Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(), () -> {
                // Ouvrir le menu des niveaux via le service GUI
                java.util.Map<String, Object> parameters = new java.util.HashMap<>();
                parameters.put("playerUuid", playerUuid);
                guiService.openGui(player, "levels", parameters);
                // Message de succès supprimé pour éviter l'encombrement du chat
            });
        } catch (Exception e) {
            notificationService.sendMessage(player, "commands.open.error", e.getMessage());
        }

        return true;
    }

    /**
     * Affiche les statistiques d'un joueur
     */
    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            notificationService.logConsole("commands.player-only");
            return true;
        }

        Player player = (Player) sender;
        String targetPlayerName = args.length > 1 ? args[1] : player.getName();

        CompletableFuture.runAsync(() -> {
            try {
                Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
                if (targetPlayer == null) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        notificationService.sendMessage(player, "commands.stats.player-not-found", targetPlayerName);
                    });
                    return;
                }

                String targetUuid = targetPlayer.getUniqueId().toString();
                playerService.getPlayerData(targetUuid).ifPresentOrElse(
                    playerData -> {
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            notificationService.sendMessage(player, "commands.stats.header", targetPlayer.getName());
                            notificationService.sendMessage(player, "commands.stats.level", playerData.getLevel());
                            notificationService.sendMessage(player, "commands.stats.buttons", playerData.getButtonAmount());
                        });
                    },
                    () -> {
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            notificationService.sendMessage(player, "commands.stats.no-data", targetPlayerName);
                        });
                    }
                );

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    notificationService.sendMessage(player, "commands.stats.error", e.getMessage());
                });
            }
        });

        return true;
    }

    /**
     * Remet à zéro les données d'un joueur
     */
    private boolean handleResetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("krakenlevels.admin.reset")) {
            notificationService.sendMessage((Player) sender, "commands.no-permission");
            return true;
        }

        if (args.length < 2) {
            notificationService.sendMessage((Player) sender, "commands.reset.usage");
            return true;
        }

        String targetPlayerName = args[1];

        CompletableFuture.runAsync(() -> {
            try {
                Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
                if (targetPlayer == null) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        notificationService.sendMessage((Player) sender, "commands.reset.player-not-found", targetPlayerName);
                    });
                    return;
                }

                String targetUuid = targetPlayer.getUniqueId().toString();

                // Créer un nouveau profil (reset)
                playerService.createPlayer(targetUuid, targetPlayer.getName());

                // Invalider le cache
                cacheService.invalidatePlayer(targetUuid);

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    notificationService.sendMessage((Player) sender, "commands.reset.success", targetPlayerName);
                    if (targetPlayer.isOnline()) {
                        notificationService.sendMessage(targetPlayer, "commands.reset.target-notification");
                    }
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    notificationService.sendMessage((Player) sender, "commands.reset.error", e.getMessage());
                });
            }
        });

        return true;
    }

    /**
     * Donne des niveaux à un joueur
     */
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("krakenlevels.admin.give")) {
            notificationService.sendMessage((Player) sender, "commands.no-permission");
            return true;
        }

        if (args.length < 3) {
            notificationService.sendMessage((Player) sender, "commands.give.usage");
            return true;
        }

        String targetPlayerName = args[1];
        int levels;

        try {
            levels = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            notificationService.sendMessage((Player) sender, "commands.give.invalid-number", args[2]);
            return true;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
                if (targetPlayer == null) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        notificationService.sendMessage((Player) sender, "commands.give.player-not-found", targetPlayerName);
                    });
                    return;
                }

                String targetUuid = targetPlayer.getUniqueId().toString();
                int currentLevel = playerService.getPlayerLevel(targetUuid);
                int newLevel = Math.max(0, currentLevel + levels);

                playerService.updatePlayerLevel(targetUuid, newLevel);

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    notificationService.sendMessage((Player) sender, "commands.give.success",
                        targetPlayerName, levels, newLevel);
                    if (targetPlayer.isOnline()) {
                        notificationService.sendMessage(targetPlayer, "commands.give.target-notification",
                            levels, newLevel);
                    }
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    notificationService.sendMessage((Player) sender, "commands.give.error", e.getMessage());
                });
            }
        });

        return true;
    }

    /**
     * Définit directement le niveau d'un joueur et applique les récompenses associées
     */
    private boolean handleSetLevelCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("krakenlevels.admin.setlevel")) {
            if (sender instanceof Player player) {
                notificationService.sendMessage(player, "commands.no-permission");
            } else {
                sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            }
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /levels setlevel <joueur> <niveau>");
            return true;
        }

        String targetPlayerName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        if (targetPlayer == null) {
            sender.sendMessage("§cJoueur introuvable: " + targetPlayerName);
            return true;
        }

        int targetLevel;
        try {
            targetLevel = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cLe niveau doit être un nombre entier valide.");
            return true;
        }

        if (targetLevel <= 0) {
            sender.sendMessage("§cLe niveau doit être supérieur à 0.");
            return true;
        }

        if (levelsConfigService == null || levelsConfigService.getLevelConfig(targetLevel) == null) {
            sender.sendMessage("§cLe niveau " + targetLevel + " n'existe pas dans levels.yml.");
            return true;
        }

        String targetUuid = targetPlayer.getUniqueId().toString();
        if (!playerService.playerExists(targetUuid)) {
            playerService.createPlayer(targetUuid, targetPlayer.getName());
        }

        boolean rewarded = missionService.forceCompleteLevel(targetPlayer, targetLevel);
        if (!rewarded) {
            sender.sendMessage("§cImpossible de définir le niveau. Vérifiez que le niveau est supérieur au niveau actuel du joueur.");
            return true;
        }

        sender.sendMessage("§a" + targetPlayer.getName() + " atteint désormais le niveau " + targetLevel + " et reçoit les récompenses correspondantes.");
        targetPlayer.sendMessage("§aVotre niveau a été fixé à " + targetLevel + " et vous recevez les récompenses associées.");
        return true;
    }

    /**
     * Affiche les informations du plugin
     */
    @SuppressWarnings("deprecation")
    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("krakenlevels.info")) {
            notificationService.sendMessage((Player) sender, "commands.no-permission");
            return true;
        }

        String version = Main.getInstance().getDescription().getVersion();
        String status = databaseService.isConnected() ? "§aConnecté" : "§cDéconnecté";

        notificationService.sendMessage((Player) sender, "commands.info.header");
        notificationService.sendMessage((Player) sender, "commands.info.version", version);
        notificationService.sendMessage((Player) sender, "commands.info.database-status", status);
        notificationService.sendMessage((Player) sender, "commands.info.cache-stats", cacheService.getStats());

        return true;
    }

    /**
     * Gère les commandes de cache
     */
    private boolean handleCacheCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("krakenlevels.admin.cache")) {
            notificationService.sendMessage((Player) sender, "commands.no-permission");
            return true;
        }

        if (args.length < 2) {
            notificationService.sendMessage((Player) sender, "commands.cache.usage");
            return true;
        }

        String cacheAction = args[1].toLowerCase();

        switch (cacheAction) {
            case "clear":
                cacheService.clear();
                notificationService.sendMessage((Player) sender, "commands.cache.cleared");
                break;
            case "stats":
                notificationService.sendMessage((Player) sender, "commands.cache.stats", cacheService.getStats());
                break;
            default:
                notificationService.sendMessage((Player) sender, "commands.cache.usage");
                break;
        }

        return true;
    }

    /**
     * Affiche le classement des joueurs
     */
    private boolean handleTopCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("krakenlevels.top")) {
            notificationService.sendMessage((Player) sender, "commands.no-permission");
            return true;
        }

        int limit = 10; // Par défaut
        if (args.length > 1) {
            try {
                limit = Math.min(50, Math.max(1, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {}
        }

        final int finalLimit = limit;

        CompletableFuture.runAsync(() -> {
            try {
                List<IPlayerService.PlayerData> topPlayers = playerService.getTopPlayers(finalLimit);

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    notificationService.sendMessage((Player) sender, "commands.top.header", finalLimit);

                    for (int i = 0; i < topPlayers.size(); i++) {
                        IPlayerService.PlayerData playerData = topPlayers.get(i);
                        notificationService.sendMessage((Player) sender, "commands.top.entry",
                            (i + 1), playerData.getName(), playerData.getLevel());
                    }
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    notificationService.sendMessage((Player) sender, "commands.top.error", e.getMessage());
                });
            }
        });

        return true;
    }

    /**
     * Gère les commandes de missions
     */
    private boolean handleMissionsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            notificationService.logConsole("commands.player-only");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("krakenlevels.missions")) {
            notificationService.sendMessage(player, "commands.no-permission");
            return true;
        }

        // TODO: Implémenter quand IMissionService sera ajouté à Main
        notificationService.sendMessage(player, "commands.missions.coming-soon");
        return true;
    }

    /**
     * Gère la commande testmoney - déclenche la vérification des missions d'argent
     */
    private boolean handleTestMoneyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            notificationService.logConsole("commands.player-only");
            return true;
        }

        Player player = (Player) sender;

        // Pour les tests, pas besoin de permission spéciale
        String playerUuid = player.getUniqueId().toString();

        // S'assurer que le joueur a un profil
        if (!playerService.playerExists(playerUuid)) {
            playerService.createPlayer(playerUuid, player.getName());

            // Assigner automatiquement la mission pour passer niveau 1
            IMissionService.Mission level1Mission = new IMissionService.Mission(
                "1",
                "Niveau 1 - Test",
                "currency",
                100,
                java.util.Map.of("cost", 100.0)
            );
            missionService.assignMission(playerUuid, level1Mission);
            notificationService.sendMessage(player, "test.level-mission-assigned", 1);
        }

        // Le système vérifie automatiquement les missions via les événements
        // Plus besoin d'appel manuel à checkMoneyProgress
        notificationService.sendMessage(player, "test.system-active", "levels.yml");

        return true;
    }

    /**
     * Vérifie si l'utilisateur a la permission pour une commande
     */
    private boolean hasPermissionForCommand(CommandSender sender, String command) {
        switch (command) {
            case "reload":
            case "reset":
            case "give":
            case "setlevel":
            case "cache":
            case "player":
                return sender.hasPermission("krakenlevels.admin." + command);
            case "help":
            case "open":
            case "stats":
            case "info":
            case "top":
            case "missions":
                return sender.hasPermission("krakenlevels." + command);
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Complétion des sous-commandes basée sur les permissions
            for (String subCmd : SUBCOMMANDS) {
                if (subCmd.toLowerCase().startsWith(args[0].toLowerCase()) &&
                    hasPermissionForCommand(sender, subCmd)) {
                    completions.add(subCmd);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            // Complétion des noms de joueurs pour certaines commandes
            if (Arrays.asList("stats", "reset", "give", "setlevel", "player").contains(subCommand)) {
                String partial = args[1].toLowerCase();
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                });
            }

            // Complétion spécifique pour la commande cache
            if ("cache".equals(subCommand)) {
                for (String action : Arrays.asList("clear", "stats")) {
                    if (action.startsWith(args[1].toLowerCase())) {
                        completions.add(action);
                    }
                }
            }

            // Complétion spécifique pour la commande testprogress
            if ("testprogress".equals(subCommand)) {
                for (String action : Arrays.asList("info", "add", "reset", "save", "reload")) {
                    if (action.startsWith(args[1].toLowerCase())) {
                        completions.add(action);
                    }
                }
            }

            // Complétion spécifique pour la commande player
            if ("player".equals(subCommand)) {
                if (args.length == 2) {
                    // Complétion des noms de joueurs connectés
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        String playerName = onlinePlayer.getName();
                        if (playerName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(playerName);
                        }
                    }
                } else if (args.length == 3) {
                    // Complétion des noms de serveurs configurés
                    var configService = Main.getInstance().getConfigurationService();
                    if (configService != null) {
                        String currentServer = configService.getConfig().getString("server.name", "lobby-1");
                        // Suggérer quelques serveurs communs + le serveur actuel
                        for (String server : Arrays.asList(currentServer, "lobby-1", "survival-1", "creative-1", "prison-1")) {
                            if (server.toLowerCase().startsWith(args[2].toLowerCase())) {
                                completions.add(server);
                            }
                        }
                    }
                }
            }
        }

        return completions;
    }

    /**
     * Commande de test pour simuler l'ouverture depuis un plugin externe
     * Cette commande teste la robustesse du système contre les bugs d'interaction
     */
    private boolean handleTestExternalCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est uniquement pour les joueurs");
            return true;
        }

        if (!player.hasPermission("krakenlevels.admin")) {
            notificationService.sendMessage(player, "commands.no-permission");
            return true;
        }

        try {
            player.sendMessage("§e[TEST] Simulation d'ouverture depuis un plugin externe...");
            
            // Simuler l'ouverture depuis un autre plugin avec la méthode publique
            // Ceci teste le système de nettoyage et de prévention des bugs
            Main.openLevelsGUI(player, 0);
            
            player.sendMessage("§a[TEST] Menu ouvert avec succès ! Ferme le menu et teste les interactions avec les coffres/fours.");
            player.sendMessage("§7[TEST] Cette commande simule l'ouverture depuis un autre plugin.");
            
        } catch (Exception e) {
            player.sendMessage("§c[TEST] Erreur lors du test: " + e.getMessage());
            Main.getInstance().getLogger().warning("Erreur dans la commande de test externe: " + e.getMessage());
        }

        return true;
    }

    /**
     * Commande de test pour vérifier la persistance de la progression des missions
     * Permet de tester que la progression des missions est bien sauvegardée
     */
    private boolean handleTestProgressCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est uniquement pour les joueurs");
            return true;
        }

        if (!player.hasPermission("krakenlevels.admin")) {
            notificationService.sendMessage(player, "commands.no-permission");
            return true;
        }

        try {
            String playerUuid = player.getUniqueId().toString();

            if (args.length == 0) {
                // Afficher l'aide
                player.sendMessage("§e========== Test Progression Missions ==========");
                player.sendMessage("§f/levels testprogress info §7- Affiche la progression actuelle");
                player.sendMessage("§f/levels testprogress add <niveau> <montant> §7- Ajoute de la progression");
                player.sendMessage("§f/levels testprogress reset <niveau> §7- Remet à zéro la progression");
                player.sendMessage("§f/levels testprogress save §7- Force la sauvegarde");
                player.sendMessage("§f/levels testprogress reload §7- Recharge les données");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "info" -> {
                    player.sendMessage("§e========== Progression Missions ==========");
                    
                    // Récupérer les données joueur
                    var playerDataOpt = playerService.getPlayerData(playerUuid);
                    if (playerDataOpt.isEmpty()) {
                        player.sendMessage("§cAucune donnée trouvée pour votre profil");
                        return true;
                    }

                    var playerData = playerDataOpt.get();
                    player.sendMessage("§aNiveau actuel: §f" + playerData.getLevel());
                    player.sendMessage("§aProgression des missions:");

                    if (playerData.getMissionProgress().isEmpty()) {
                        player.sendMessage("§7  Aucune mission en cours");
                    } else {
                        for (var entry : playerData.getMissionProgress().entrySet()) {
                            String mission = entry.getKey();
                            int progress = entry.getValue();
                            player.sendMessage("§7  " + mission + ": §f" + progress);
                        }
                    }
                }

                case "add" -> {
                    if (args.length < 3) {
                        player.sendMessage("§cUsage: /levels testprogress add <niveau> <montant>");
                        return true;
                    }

                    try {
                        int level = Integer.parseInt(args[1]);
                        int amount = Integer.parseInt(args[2]);

                        // Utiliser l'API des missions pour ajouter la progression
                        missionService.updateMissionProgress(playerUuid, String.valueOf(level), amount);
                        
                        player.sendMessage("§a" + amount + " progression ajoutée à la mission niveau " + level);
                        player.sendMessage("§7Progression actuelle: " + missionService.getMissionProgress(playerUuid, String.valueOf(level)));

                    } catch (NumberFormatException e) {
                        player.sendMessage("§cLe niveau et le montant doivent être des nombres");
                    }
                }

                case "reset" -> {
                    if (args.length < 2) {
                        player.sendMessage("§cUsage: /levels testprogress reset <niveau>");
                        return true;
                    }

                    try {
                        int level = Integer.parseInt(args[1]);
                        String missionKey = "mission_" + level;

                        var playerDataOpt = playerService.getPlayerData(playerUuid);
                        if (playerDataOpt.isEmpty()) {
                            player.sendMessage("§cAucune donnée trouvée pour votre profil");
                            return true;
                        }

                        var playerData = playerDataOpt.get();
                        playerData.getMissionProgress().remove(missionKey);
                        playerService.savePlayerData(playerUuid, playerData);

                        player.sendMessage("§aProgression de la mission niveau " + level + " remise à zéro");

                    } catch (NumberFormatException e) {
                        player.sendMessage("§cLe niveau doit être un nombre");
                    }
                }

                case "save" -> {
                    var playerDataOpt = playerService.getPlayerData(playerUuid);
                    if (playerDataOpt.isEmpty()) {
                        player.sendMessage("§cAucune donnée trouvée pour votre profil");
                        return true;
                    }

                    var playerData = playerDataOpt.get();
                    playerService.savePlayerData(playerUuid, playerData);
                    player.sendMessage("§aDonnées sauvegardées avec succès");
                }

                case "reload" -> {
                    // Vider le cache pour forcer le rechargement depuis la base
                    String cacheKey = "player_" + playerUuid;
                    cacheService.remove(cacheKey);
                    
                    player.sendMessage("§aDonnées rechargées depuis la base de données");
                    
                    // Afficher les données rechargées
                    var playerDataOpt = playerService.getPlayerData(playerUuid);
                    if (playerDataOpt.isPresent()) {
                        var playerData = playerDataOpt.get();
                        player.sendMessage("§7Progression rechargée: " + playerData.getMissionProgress().size() + " missions");
                    }
                }

                default -> {
                    player.sendMessage("§cAction inconnue. Utilisez 'info', 'add', 'reset', 'save' ou 'reload'");
                }
            }

        } catch (Exception e) {
            player.sendMessage("§c[TEST] Erreur lors du test de progression: " + e.getMessage());
            Main.getInstance().getLogger().warning("Erreur dans la commande de test de progression: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Commande de test pour la synchronisation inter-serveur
     */
    private boolean handleTestSyncCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est uniquement pour les joueurs");
            return true;
        }

        if (!player.hasPermission("krakenlevels.admin")) {
            notificationService.sendMessage(player, "commands.no-permission");
            return true;
        }

        try {
            String playerUuid = player.getUniqueId().toString();

            if (args.length == 0) {
                // Afficher l'aide
                player.sendMessage("§e========== Test Synchronisation ==========");
                player.sendMessage("§f/levels testsync info §7- Affiche les infos de synchronisation");
                player.sendMessage("§f/levels testsync force §7- Force une synchronisation");
                player.sendMessage("§f/levels testsync status §7- Statut du service de synchronisation");
                player.sendMessage("§f/levels testsync clear §7- Vide le cache et force une sync complète");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "info" -> {
                    player.sendMessage("§e========== Infos Synchronisation ==========");
                    
                    // Récupérer les données joueur
                    var playerDataOpt = playerService.getPlayerData(playerUuid);
                    if (playerDataOpt.isEmpty()) {
                        player.sendMessage("§cAucune donnée trouvée pour votre profil");
                        return true;
                    }

                    var playerData = playerDataOpt.get();
                    player.sendMessage("§aNiveau actuel: §f" + playerData.getLevel());
                    player.sendMessage("§aProgression des missions:");
                    if (playerData.getMissionProgress().isEmpty()) {
                        player.sendMessage("§7  Aucune mission en cours");
                    } else {
                        for (var entry : playerData.getMissionProgress().entrySet()) {
                            String mission = entry.getKey();
                            int progress = entry.getValue();
                            player.sendMessage("§7  " + mission + ": §f" + progress);
                        }
                    }
                    
                    // Informations sur le serveur
                    String serverName = configService.getConfig().getString("server.name", "unknown");
                    player.sendMessage("§aServeur actuel: §f" + serverName);
                }

                case "force" -> {
                    player.sendMessage("§eForçage de la synchronisation...");
                    
                    // Forcer la synchronisation
                    var syncService = Main.getInstance().getSynchronizationService();
                    if (syncService != null) {
                        syncService.forceSync(playerUuid).thenAccept(success -> {
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                                if (success) {
                                    player.sendMessage("§aSynchronisation forcée réussie !");
                                    
                                    // Afficher les données après synchronisation
                                    var newPlayerDataOpt = playerService.getPlayerData(playerUuid);
                                    if (newPlayerDataOpt.isPresent()) {
                                        var newPlayerData = newPlayerDataOpt.get();
                                        player.sendMessage("§aNiveau après sync: §f" + newPlayerData.getLevel());
                                    }
                                } else {
                                    player.sendMessage("§cÉchec de la synchronisation forcée");
                                }
                            });
                        });
                    } else {
                        player.sendMessage("§cService de synchronisation non disponible");
                    }
                }

                case "status" -> {
                    player.sendMessage("§e========== Statut Synchronisation ==========");
                    
                    // Vérifier si la synchronisation est activée
                    boolean syncEnabled = configService.getConfig().getBoolean("synchronization.enabled", false);
                    player.sendMessage("§aSynchronisation: " + (syncEnabled ? "§aActivée" : "§cDésactivée"));
                    
                    if (syncEnabled) {
                        boolean autoJoin = configService.getConfig().getBoolean("synchronization.auto-sync.on-join", true);
                        boolean autoQuit = configService.getConfig().getBoolean("synchronization.auto-sync.on-quit", true);
                        player.sendMessage("§aAuto-sync connexion: " + (autoJoin ? "§aOui" : "§cNon"));
                        player.sendMessage("§aAuto-sync déconnexion: " + (autoQuit ? "§aOui" : "§cNon"));
                        
                        String serverName = configService.getConfig().getString("server.name", "unknown");
                        player.sendMessage("§aNom du serveur: §f" + serverName);
                    }
                }

                case "clear" -> {
                    player.sendMessage("§eVidage du cache et synchronisation complète...");
                    
                    // Vider le cache pour ce joueur
                    var cacheService = Main.getInstance().getCacheService();
                    if (cacheService != null) {
                        cacheService.invalidatePlayer(playerUuid);
                        player.sendMessage("§aCache vidé pour votre joueur");
                    }
                    
                    // Forcer une synchronisation complète
                    var syncService = Main.getInstance().getSynchronizationService();
                    if (syncService != null) {
                        syncService.forceSync(playerUuid).thenAccept(success -> {
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                                if (success) {
                                    player.sendMessage("§aSynchronisation complète réussie !");
                                    
                                    // Afficher les données après synchronisation
                                    var newPlayerDataOpt = playerService.getPlayerData(playerUuid);
                                    if (newPlayerDataOpt.isPresent()) {
                                        var newPlayerData = newPlayerDataOpt.get();
                                        player.sendMessage("§aDonnées rechargées - Niveau: §f" + newPlayerData.getLevel());
                                        player.sendMessage("§aMissions: §f" + newPlayerData.getMissionProgress().size());
                                    }
                                } else {
                                    player.sendMessage("§cÉchec de la synchronisation complète");
                                }
                            });
                        });
                    } else {
                        player.sendMessage("§cService de synchronisation non disponible");
                    }
                }

                default -> {
                    player.sendMessage("§cAction inconnue. Utilisez 'info', 'force', 'status' ou 'clear'");
                }
            }

        } catch (Exception e) {
            player.sendMessage("§c[TEST] Erreur lors du test de synchronisation: " + e.getMessage());
            Main.getInstance().getLogger().warning("Erreur dans la commande de test de synchronisation: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Commande pour voir les niveaux d'un joueur par serveur
     * Usage: /levels player <pseudo> [serveur]
     */
    private boolean handlePlayerCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("krakenlevels.admin")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c========== Niveaux par Serveur ==========");
            sender.sendMessage("§cUsage: §f/levels player <pseudo> [serveur]");
            sender.sendMessage("§eExemples:");
            sender.sendMessage("§f  /levels player Loyfael §7- Voir les niveaux sur tous les serveurs");
            sender.sendMessage("§f  /levels player Loyfael lobby-1 §7- Voir les niveaux sur un serveur spécifique");
            return true;
        }

        String targetPlayerName = args[1];
        String specificServer = args.length >= 3 ? args[2] : null;

        // Obtenir l'UUID du joueur
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage("§cJoueur §f" + targetPlayerName + " §cn'est pas connecté ou n'existe pas");
            return true;
        }

        String playerUuid = targetPlayer.getUniqueId().toString();

        sender.sendMessage("§e========== Niveaux de " + targetPlayerName + " ==========");

        try {
            // Obtenir les données locales
            var localPlayerDataOpt = playerService.getPlayerData(playerUuid);
            if (localPlayerDataOpt.isPresent()) {
                var localData = localPlayerDataOpt.get();
                var configService = Main.getInstance().getConfigurationService();
                String currentServer = configService.getConfig().getString("server.name", "inconnu");
                
                sender.sendMessage("§a📍 Serveur actuel (" + currentServer + "):");
                sender.sendMessage("§f  ├─ Niveau: §a" + localData.getLevel());
                sender.sendMessage("§f  ├─ Dernière connexion: §7" + new java.util.Date(localData.getLastSeen()));
                sender.sendMessage("§f  └─ Missions: §e" + localData.getMissionProgress().size() + " en cours");
            } else {
                sender.sendMessage("§c❌ Aucune donnée locale trouvée");
            }

            // Obtenir les données MongoDB pour comparaison
            var syncService = Main.getInstance().getSynchronizationService();
            if (syncService != null) {
                sender.sendMessage("§b🔄 Récupération des données de synchronisation...");
                
                // Utilisation asynchrone pour éviter de bloquer
                CompletableFuture.supplyAsync(() -> {
                    try {
                        // Accéder aux données MongoDB directement
                        var databaseService = Main.getInstance().getDatabaseService();
                        if (databaseService != null) {
                            Object mongoData = databaseService.getData("player_" + playerUuid);
                            return mongoData;
                        }
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                }).thenAccept(mongoData -> {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        if (mongoData != null) {
                            sender.sendMessage("§b📊 Données MongoDB:");
                            sender.sendMessage("§f  └─ Données trouvées dans la base");
                            // TODO: Parser et afficher les détails selon le serveur
                        } else {
                            sender.sendMessage("§c❌ Aucune donnée MongoDB trouvée");
                        }
                        
                        sender.sendMessage("§e==========================================");
                        if (specificServer == null) {
                            sender.sendMessage("§7Astuce: Utilisez §f/levels player " + targetPlayerName + " <serveur> §7pour voir un serveur spécifique");
                        }
                    });
                });
            } else {
                sender.sendMessage("§c❌ Service de synchronisation non disponible");
            }

        } catch (Exception e) {
            sender.sendMessage("§cErreur lors de la récupération des données: " + e.getMessage());
            Main.getInstance().getLogger().warning("Erreur dans la commande player: " + e.getMessage());
        }

        return true;
    }
}
