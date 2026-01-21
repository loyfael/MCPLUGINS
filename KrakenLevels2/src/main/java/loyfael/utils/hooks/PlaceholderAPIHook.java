package loyfael.utils.hooks;

import loyfael.Main;
import loyfael.api.interfaces.IPlayerService;
import loyfael.api.interfaces.ILevelsConfigService;
import loyfael.api.interfaces.IMissionService;
import loyfael.core.services.LevelsConfigService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Optional;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final Main plugin;
    private final IPlayerService playerService;
    private final ILevelsConfigService levelsConfigService;
    private final IMissionService missionService;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public PlaceholderAPIHook(Main plugin) {
        this.plugin = plugin;
        this.playerService = plugin.getPlayerService();
        this.levelsConfigService = plugin.getLevelsConfigService();
        this.missionService = plugin.getMissionService();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "krakenlevels";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Loyfael";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        String playerUuid = player.getUniqueId().toString();
        Optional<IPlayerService.PlayerData> playerDataOpt = playerService.getPlayerData(playerUuid);

        if (playerDataOpt.isEmpty()) {
            return "";
        }

        IPlayerService.PlayerData playerData = playerDataOpt.get();
        int currentLevel = playerData.getLevel();
        int nextLevel = currentLevel + 1;
        int maxLevel = levelsConfigService.getMaxLevel();

        switch (params.toLowerCase()) {
            // Niveaux de base
            case "level":
                return String.valueOf(currentLevel);

            case "nextlevel":
                if (nextLevel > maxLevel) {
                    return String.valueOf(maxLevel);
                }
                return String.valueOf(nextLevel);

            // Coûts (anciens noms pour compatibilité)
            case "cost":
            case "cost_currentlevel":
                return String.valueOf(getCostForLevel(currentLevel));

            case "nextcost":
            case "cost_nextlevel":
                return String.valueOf(getCostForLevel(nextLevel));

            case "cost_formatted":
            case "cost_currentlevel_format":
                double currentCost = getCostForLevel(currentLevel);
                return formatter.format(currentCost);

            case "nextcost_formatted":
            case "cost_nextlevel_format":
                double nextCost = getCostForLevel(nextLevel);
                return formatter.format(nextCost);

            // Niveau maximum
            case "isonmaxlevel":
                return currentLevel >= maxLevel ? "yes" : "no";

            case "getmaxlevel":
                return String.valueOf(maxLevel);

            // Type de tâche
            case "tasktype":
                LevelsConfigService.LevelConfig nextLevelConfig = levelsConfigService.getLevelConfig(nextLevel);
                if (nextLevelConfig != null && nextLevelConfig.getType() != null) {
                    return getTaskTypeDisplay(nextLevelConfig.getType());
                }
                return "Aucune tâche";

            // Progression des missions
            case "progress":
                return getProgressDisplay(playerData, nextLevel);

            case "currentprogress":
                return String.valueOf(getCurrentProgress(playerData, nextLevel));

            case "isonmission":
                return hasActiveMission(playerData, currentLevel) ? "yes" : "no";

            // Affichage des niveaux (configuration)
            case "displaylevel":
                return getDisplayLevel(currentLevel);

            case "displaynextlevel":
                return getDisplayLevel(nextLevel);

            // Économie (pour compatibilité)
            case "balance":
                Economy economy = plugin.getEconomy();
                if (economy != null) {
                    double balance = economy.getBalance(player);
                    return formatter.format(balance);
                }
                return "0";

            default:
                return null;
        }
    }

    /**
     * Gets the cost for a specific level
     */
    private double getCostForLevel(int level) {
        LevelsConfigService.LevelConfig levelConfig = levelsConfigService.getLevelConfig(level);
        if (levelConfig != null && levelConfig.getType() != null) {
            return levelConfig.getType().getCost();
        }
        return 0.0;
    }

    /**
     * Gets the display name for the task type
     */
    private String getTaskTypeDisplay(LevelsConfigService.LevelType levelType) {
        if (levelType == null) return "No task";
        
        switch (levelType.getName().toLowerCase()) {
            case "currency":
                return "Economy";
            case "blockbreak":
                return "Break blocks";
            case "blockplace":
                return "Place blocks";
            case "kills":
                return "Kill mobs";
            case "fish":
                return "Fish";
            default:
                return levelType.getName();
        }
    }

    /**
     * Gets the progress display
     */
    private String getProgressDisplay(IPlayerService.PlayerData playerData, int level) {
        int currentProgress = getCurrentProgress(playerData, level);
        int requiredProgress = getRequiredProgress(level);
        return currentProgress + "/" + requiredProgress;
    }

    /**
     * Gets the current progress for a level
     */
    private int getCurrentProgress(IPlayerService.PlayerData playerData, int level) {
        String missionKey = "mission_" + level;
        return playerData.getMissionProgress().getOrDefault(missionKey, 0);
    }

    /**
     * Gets the required progress for a level
     */
    private int getRequiredProgress(int level) {
        LevelsConfigService.LevelConfig levelConfig = levelsConfigService.getLevelConfig(level);
        if (levelConfig != null && levelConfig.getType() != null) {
            return levelConfig.getType().getAmount();
        }
        return 0;
    }

    /**
     * Checks if the player has an active mission
     */
    private boolean hasActiveMission(IPlayerService.PlayerData playerData, int currentLevel) {
        int nextLevel = currentLevel + 1;
        LevelsConfigService.LevelConfig levelConfig = levelsConfigService.getLevelConfig(nextLevel);
        
        if (levelConfig == null || levelConfig.getType() == null) {
            return false;
        }

        // Economy levels are not active missions
        if ("currency".equalsIgnoreCase(levelConfig.getType().getName())) {
            return false;
        }

        // Check if there is some progress for this level
        String missionKey = "mission_" + nextLevel;
        int progress = playerData.getMissionProgress().getOrDefault(missionKey, 0);
        int required = levelConfig.getType().getAmount();
        
        return progress < required; // Mission active if not completed
    }

    /**
     * Gets the configured display for a level
     */
    private String getDisplayLevel(int level) {
        LevelsConfigService.LevelConfig levelConfig = levelsConfigService.getLevelConfig(level);
        if (levelConfig != null) {
            // Use configured level name or a default format
            String displayName = levelConfig.getName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName.replace("&", "§");
            }
        }
        // Default format
        return "Level " + level;
    }
}
