package loyfael.core.services;

import loyfael.api.interfaces.IMissionService;
import loyfael.api.interfaces.IPlayerService;
import loyfael.api.interfaces.INotificationService;
import loyfael.api.interfaces.ILevelsConfigService;
import loyfael.Main;
import loyfael.utils.Utils;
import loyfael.utils.RewardExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de missions centralisé utilisant totalement levels.yml
 * Principe de responsabilité unique : gestion des missions uniquement
 * Principe ouvert/fermé : extensible pour nouveaux types de missions
 */
public class MissionService implements IMissionService {

    private final IPlayerService playerService;
    private final INotificationService notificationService;
    private final ILevelsConfigService levelsConfigService;
    private final Map<String, List<Mission>> playerMissions = new ConcurrentHashMap<>();

    // Cache pour optimiser les performances - ne traite les événements que si nécessaire
    private volatile boolean hasActiveBlockBreakMissions = false;
    private volatile boolean hasActiveBlockPlaceMissions = false;
    private volatile boolean hasActiveKillMissions = false;
    private volatile boolean hasActiveFishMissions = false;

    // Gestion de la sauvegarde différée
    private final Set<String> modifiedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> playerActionCounts = new ConcurrentHashMap<>();

    public MissionService(IPlayerService playerService, INotificationService notificationService,
                         ILevelsConfigService levelsConfigService) {
        this.playerService = playerService;
        this.notificationService = notificationService;
        this.levelsConfigService = levelsConfigService;
        loadMissionsFromConfig();
        updateActiveMissionsCache();
    }

    /**
     * Met à jour le cache des missions actives pour optimiser les performances
     * Ne traite les événements QUE si au moins un joueur connecté a une mission active de ce type
     */
    private void updateActiveMissionsCache() {
        hasActiveBlockBreakMissions = false;
        hasActiveBlockPlaceMissions = false;
        hasActiveKillMissions = false;
        hasActiveFishMissions = false;

        // Parcourir tous les joueurs connectés pour voir qui a des missions actives
        for (Player onlinePlayer : Main.getInstance().getServer().getOnlinePlayers()) {
            String playerUuid = onlinePlayer.getUniqueId().toString();
            int currentLevel = playerService.getPlayerLevel(playerUuid);
            int nextLevel = currentLevel + 1;

            // Vérifier si ce joueur a une mission active pour le prochain niveau
            LevelsConfigService.LevelConfig nextLevelConfig = levelsConfigService.getLevelConfig(nextLevel);

            if (nextLevelConfig != null && nextLevelConfig.getType().isMission()) {
                String missionType = nextLevelConfig.getType().getName().toLowerCase();

                switch (missionType) {
                    case "blockbreak":
                        hasActiveBlockBreakMissions = true;
                        break;
                    case "blockplace":
                        hasActiveBlockPlaceMissions = true;
                        break;
                    case "kills":
                        hasActiveKillMissions = true;
                        break;
                    case "fish":
                        hasActiveFishMissions = true;
                        break;
                }

                // Optimisation : si tous les types sont actifs, pas besoin de continuer
                if (hasActiveBlockBreakMissions && hasActiveBlockPlaceMissions &&
                    hasActiveKillMissions && hasActiveFishMissions) {
                    break;
                }
            }
        }
    }

    /**
     * Charge toutes les missions depuis levels.yml
     */
    private void loadMissionsFromConfig() {
        // Les missions sont chargées à la volée via levelsConfigService
    }

    @Override
    public void processEvent(Player player, Event event) {
        if (player == null) return;

        String playerUuid = player.getUniqueId().toString();

        // Obtenir le niveau actuel du joueur
        int currentLevel = playerService.getPlayerLevel(playerUuid);
        int targetLevel = currentLevel + 1; // Mission pour atteindre le niveau suivant

        // Vérifier si il y a une mission pour atteindre le niveau cible
        LevelsConfigService.LevelConfig targetLevelConfig = levelsConfigService.getLevelConfig(targetLevel);

        if (targetLevelConfig == null || targetLevelConfig.getType().getName().equalsIgnoreCase("currency")) {
            return; // Pas de mission pour ce niveau (c'est de l'économie)
        }

        // Traiter l'événement pour progresser vers le niveau cible
        processMissionEvent(player, targetLevelConfig, event);
    }

    /**
     * Traite un événement pour une mission spécifique
     */
    private void processMissionEvent(Player player, LevelsConfigService.LevelConfig levelConfig, Event event) {
        LevelsConfigService.LevelType missionType = levelConfig.getType();
        String playerUuid = player.getUniqueId().toString();

        boolean progressMade = false;

        switch (missionType.getName().toLowerCase()) {
            case "blockbreak":
                if (event instanceof BlockBreakEvent) {
                    BlockBreakEvent breakEvent = (BlockBreakEvent) event;
                    // Vérifier que c'est bien le bon joueur qui casse le bloc
                    if (breakEvent.getPlayer().equals(player)) {
                        Material targetMaterial = Material.valueOf(missionType.getMaterial().toUpperCase());
                        Material brokenMaterial = breakEvent.getBlock().getType();
                        
                        // Vérifier si le matériau cassé correspond ou est équivalent (minerais deepslate)
                        if (brokenMaterial == targetMaterial || isEquivalentOre(targetMaterial, brokenMaterial)) {
                            // Vérifier la maturité seulement si c'est une culture
                            if (isCropMature(breakEvent.getBlock(), brokenMaterial)) {
                                progressMade = addMissionProgress(playerUuid, levelConfig.getLevelNumber(), 1);
                            }
                        }
                    }
                }
                break;

            case "blockplace":
                if (event instanceof BlockPlaceEvent) {
                    BlockPlaceEvent placeEvent = (BlockPlaceEvent) event;
                    // Vérifier que c'est bien le bon joueur qui place le bloc
                    if (placeEvent.getPlayer().equals(player)) {
                        Material targetMaterial = Material.valueOf(missionType.getMaterial().toUpperCase());
                        if (placeEvent.getBlock().getType() == targetMaterial) {
                            progressMade = addMissionProgress(playerUuid, levelConfig.getLevelNumber(), 1);
                        }
                    }
                }
                break;

            case "kills":
                if (event instanceof EntityDeathEvent) {
                    EntityDeathEvent deathEvent = (EntityDeathEvent) event;

                    // Vérifier que c'est bien le joueur qui a tué l'entité
                    if (deathEvent.getEntity().getKiller() != null &&
                        deathEvent.getEntity().getKiller().equals(player) &&
                        deathEvent.getEntity().getType().name().toLowerCase().contains(missionType.getMob().toLowerCase())) {

                        progressMade = addMissionProgress(playerUuid, levelConfig.getLevelNumber(), 1);
                    }
                }
                break;

            case "fish":
                if (event instanceof PlayerFishEvent) {
                    PlayerFishEvent fishEvent = (PlayerFishEvent) event;
                    if (fishEvent.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                        progressMade = addMissionProgress(playerUuid, levelConfig.getLevelNumber(), 1);
                    }
                }
                break;
        }

        if (progressMade) {
            checkMissionCompletion(player, levelConfig);
        }
    }

    /**
     * Ajoute du progrès à une mission et retourne true si du progrès a été fait
     */
    private boolean addMissionProgress(String playerUuid, int level, int amount) {
        String missionKey = "mission_" + level;
        IPlayerService.PlayerData playerData = playerService.getPlayerData(playerUuid).orElse(null);
        if (playerData == null) return false;

        int currentProgress = playerData.getMissionProgress().getOrDefault(missionKey, 0);
        int newProgress = currentProgress + amount;

        // Sauvegarder le progrès selon la stratégie configurée
        playerData.getMissionProgress().put(missionKey, newProgress);
        
        // Vérifier la stratégie de sauvegarde depuis la config
        if (shouldSaveImmediately(playerUuid)) {
            playerService.savePlayerData(playerUuid, playerData);
        } else {
            // Marquer les données comme modifiées pour sauvegarde différée
            markPlayerDataAsModified(playerUuid);
        }

        return amount > 0;
    }

    /**
     * Vérifie si une mission est complétée et déclenche les récompenses
     */
    private void checkMissionCompletion(Player player, LevelsConfigService.LevelConfig levelConfig) {
        String playerUuid = player.getUniqueId().toString();
        String missionKey = "mission_" + levelConfig.getLevelNumber();

        IPlayerService.PlayerData playerData = playerService.getPlayerData(playerUuid).orElse(null);
        if (playerData == null) return;

        int currentProgress = playerData.getMissionProgress().getOrDefault(missionKey, 0);
        int requiredAmount = levelConfig.getType().getAmount();

        if (currentProgress >= requiredAmount) {
            completeMission(player, levelConfig);
        } else {
            // Notifier du progrès en utilisant le système de messages
            notificationService.sendActionBar(player, "missions.progress", currentProgress, requiredAmount);
        }
    }

    /**
     * Complète une mission et accorde les récompenses
     */
    private void completeMission(Player player, LevelsConfigService.LevelConfig levelConfig) {
        String playerUuid = player.getUniqueId().toString();

        // Récupérer le niveau actuel du joueur
        int currentLevel = playerService.getPlayerLevel(playerUuid);
        int targetLevel = levelConfig.getLevelNumber(); // Le niveau que cette mission débloque

        // Vérifier que le joueur n'a pas déjà ce niveau ou un niveau supérieur
        if (currentLevel >= targetLevel) {
            return; // Joueur a déjà ce niveau ou plus
        }

        // Mettre à jour le niveau du joueur au niveau cible de cette mission
        playerService.setPlayerLevel(playerUuid, targetLevel);

        // Exécuter les commandes de récompense et messages
        RewardExecutor.executeCommands(player, levelConfig.getRewards().getCommands());
        RewardExecutor.executeCommands(player, levelConfig.getRewards().getRewardsMessage());

        // Broadcast si configuré
        if (levelConfig.getRewards().isBroadcast()) {
            RewardExecutor.broadcastMessages(player, levelConfig.getRewards().getBroadcastMessage());
        }

        // Nettoyer le progrès de mission
        String missionKey = "mission_" + levelConfig.getLevelNumber();
        IPlayerService.PlayerData playerData = playerService.getPlayerData(playerUuid).orElse(null);
        if (playerData != null) {
            playerData.getMissionProgress().remove(missionKey);
            playerService.savePlayerData(playerUuid, playerData);
        }

        // Rafraîchir le cache car le joueur a changé de niveau
        refreshMissionsCache();
    }

    // Méthodes publiques pour l'optimisation des performances
    public boolean hasActiveBlockBreakMissions() {
        return hasActiveBlockBreakMissions;
    }

    public boolean hasActiveBlockPlaceMissions() {
        return hasActiveBlockPlaceMissions;
    }

    public boolean hasActiveKillMissions() {
        return hasActiveKillMissions;
    }

    public boolean hasActiveFishMissions() {
        return hasActiveFishMissions;
    }

    @Override
    public List<Mission> getPlayerMissions(String playerUuid) {
        return playerMissions.getOrDefault(playerUuid, new ArrayList<>());
    }

    @Override
    public void assignMission(String playerUuid, Mission mission) {
        playerMissions.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(mission);
    }

    @Override
    public boolean isMissionCompleted(String playerUuid, String missionId) {
        String missionKey = "mission_" + missionId;
        IPlayerService.PlayerData playerData = playerService.getPlayerData(playerUuid).orElse(null);
        if (playerData == null) return false;

        int currentProgress = playerData.getMissionProgress().getOrDefault(missionKey, 0);

        // Récupérer la configuration du niveau pour connaître l'objectif
        try {
            int levelNumber = Integer.parseInt(missionId);
            LevelsConfigService.LevelConfig levelConfig = levelsConfigService.getLevelConfig(levelNumber);
            if (levelConfig != null && levelConfig.getType().isMission()) {
                return currentProgress >= levelConfig.getType().getAmount();
            }
        } catch (NumberFormatException e) {
            // Ignore
        }

        return false;
    }

    @Override
    public void updateMissionProgress(String playerUuid, String missionId, int progress) {
        try {
            int levelNumber = Integer.parseInt(missionId);
            boolean progressMade = addMissionProgress(playerUuid, levelNumber, progress);

            if (progressMade) {
                // Récupérer le joueur pour vérifier la complétion
                Player player = Main.getInstance().getServer().getPlayer(java.util.UUID.fromString(playerUuid));
                if (player != null) {
                    LevelsConfigService.LevelConfig levelConfig = levelsConfigService.getLevelConfig(levelNumber);
                    if (levelConfig != null) {
                        checkMissionCompletion(player, levelConfig);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            Utils.sendConsoleLog("&cErreur lors de la mise à jour de la progression de mission: " + e.getMessage());
        }
    }

    @Override
    public int getMissionProgress(String playerUuid, String missionId) {
        String missionKey = "mission_" + missionId;
        IPlayerService.PlayerData playerData = playerService.getPlayerData(playerUuid).orElse(null);
        if (playerData == null) return 0;

        return playerData.getMissionProgress().getOrDefault(missionKey, 0);
    }

    @Override
    public void rewardPlayer(Player player, Mission mission) {
        // Cette méthode est appelée par les autres services si nécessaire
        // Pour notre implémentation, les récompenses sont gérées dans completeMission
        // Suppression du log de récompense accordée
    }

    @Override
    public List<Mission> getAvailableMissions() {
        List<Mission> missions = new ArrayList<>();

        // Créer des missions depuis la configuration des niveaux
        for (LevelsConfigService.LevelConfig levelConfig : levelsConfigService.getMissionLevels()) {
            Map<String, Object> rewards = new java.util.HashMap<>();
            rewards.put("commands", levelConfig.getRewards().getCommands());
            rewards.put("messages", levelConfig.getRewards().getRewardsMessage());

            Mission mission = new Mission(
                String.valueOf(levelConfig.getLevelNumber()),
                levelConfig.getName().replace("&", "§"),
                levelConfig.getType().getName(),
                levelConfig.getType().getAmount(),
                rewards
            );
            missions.add(mission);
        }

        return missions;
    }

    @Override
    public boolean forceCompleteLevel(Player player, int levelNumber) {
        if (player == null) {
            return false;
        }

        LevelsConfigService.LevelConfig levelConfig = levelsConfigService.getLevelConfig(levelNumber);
        if (levelConfig == null) {
            return false;
        }

        String playerUuid = player.getUniqueId().toString();
        int currentLevel = playerService.getPlayerLevel(playerUuid);
        if (currentLevel >= levelNumber) {
            return false;
        }

        completeMission(player, levelConfig);
        return true;
    }

    /**
     * Met à jour le cache quand un joueur se connecte/déconnecte ou change de niveau
     */
    public void refreshMissionsCache() {
        updateActiveMissionsCache();
    }

    /**
     * Vérifie si une culture est mature (au stade final de croissance)
     * @param block Le bloc à vérifier
     * @param targetMaterial Le matériau cible de la mission
     * @return true si la culture est mature ou si ce n'est pas une culture, false sinon
     */
    private boolean isCropMature(Block block, Material targetMaterial) {
        // Si le matériau cible n'est pas une culture, accepter automatiquement
        if (!isCropBlock(targetMaterial)) {
            return true;
        }

        // Vérifier si c'est une culture avec l'interface Ageable
        if (block.getBlockData() instanceof Ageable) {
            Ageable crop = (Ageable) block.getBlockData();
            // Vérifier si l'âge actuel est égal à l'âge maximum
            return crop.getAge() == crop.getMaximumAge();
        }

        return true; // Par défaut, accepter si on ne peut pas déterminer
    }

    /**
     * Vérifie si un matériau est une culture
     * @param material Le matériau à vérifier
     * @return true si c'est une culture, false sinon
     */
    private boolean isCropBlock(Material material) {
        switch (material) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case NETHER_WART:
            case COCOA:
            case SWEET_BERRY_BUSH:
                return true;
            default:
                return false;
        }
    }

    /**
     * Détermine si les données doivent être sauvegardées immédiatement
     */
    private boolean shouldSaveImmediately(String playerUuid) {
        // Récupérer la stratégie depuis la configuration
        var configService = Main.getInstance().getConfigurationService();
        if (configService == null) return true; // Sauvegarde immédiate par défaut
        
        String saveMode = configService.getConfig().getString("system.save-strategy.mode", "immediate");
        
        if ("immediate".equals(saveMode)) {
            return true;
        }
        
        // Mode différé : vérifier si on a atteint le seuil d'actions
        int maxActions = configService.getConfig().getInt("system.save-strategy.max-actions-before-save", 50);
        int currentActions = playerActionCounts.getOrDefault(playerUuid, 0);
        
        return currentActions >= maxActions;
    }

    /**
     * Marque un joueur comme ayant des données modifiées
     */
    private void markPlayerDataAsModified(String playerUuid) {
        modifiedPlayers.add(playerUuid);
        playerActionCounts.merge(playerUuid, 1, Integer::sum);
        
        // Si on a atteint le seuil, sauvegarder quand même
        if (shouldSaveImmediately(playerUuid)) {
            savePlayerDataIfModified(playerUuid);
        }
    }

    /**
     * Sauvegarde les données d'un joueur si elles ont été modifiées
     */
    public void savePlayerDataIfModified(String playerUuid) {
        if (modifiedPlayers.contains(playerUuid)) {
            var playerDataOpt = playerService.getPlayerData(playerUuid);
            if (playerDataOpt.isPresent()) {
                playerService.savePlayerData(playerUuid, playerDataOpt.get());
                modifiedPlayers.remove(playerUuid);
                playerActionCounts.remove(playerUuid);
            }
        }
    }

    /**
     * Sauvegarde toutes les données modifiées (appelé à la déconnexion)
     */
    public void saveAllModifiedData() {
        for (String playerUuid : new HashSet<>(modifiedPlayers)) {
            savePlayerDataIfModified(playerUuid);
        }
    }

    /**
     * Nettoie les données d'un joueur déconnecté
     */
    public void cleanupPlayerData(String playerUuid) {
        // Sauvegarder les données modifiées avant de nettoyer
        savePlayerDataIfModified(playerUuid);
        // Nettoyer les références
        modifiedPlayers.remove(playerUuid);
        playerActionCounts.remove(playerUuid);
    }

    /**
     * Vérifie si deux minerais sont équivalents (ex: iron_ore et deepslate_iron_ore)
     */
    private boolean isEquivalentOre(Material targetMaterial, Material brokenMaterial) {
        // Map des équivalences minerai normal <-> minerai deepslate
        String targetName = targetMaterial.name();
        String brokenName = brokenMaterial.name();
        
        // Si la mission demande un minerai normal, accepter aussi la version deepslate
        if (brokenName.equals("DEEPSLATE_" + targetName)) {
            return true;
        }
        
        // Si la mission demande un minerai deepslate, accepter aussi la version normale
        if (targetName.startsWith("DEEPSLATE_") && brokenName.equals(targetName.substring(10))) {
            return true;
        }
        
        return false;
    }
}
