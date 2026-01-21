package loyfael.core.services;

import loyfael.Main;
import loyfael.api.interfaces.ILevelsConfigService;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Service pour lire et gérer la configuration des niveaux depuis levels.yml
 */
public class LevelsConfigService implements ILevelsConfigService {

    private final Map<Integer, LevelConfig> levels = new HashMap<>();
    private YamlConfiguration levelsConfig;

    public LevelsConfigService() {
        loadLevelsConfig();
    }

    /**
     * Charge la configuration des niveaux depuis levels.yml
     */
    private void loadLevelsConfig() {
        try {
            File levelsFile = new File(Main.getInstance().getDataFolder(), "levels.yml");
            if (!levelsFile.exists()) {
                Main.getInstance().saveResource("levels.yml", false);
                Main.getInstance().getLogger().info("Fichier levels.yml créé depuis les ressources par défaut");
            }

            levelsConfig = YamlConfiguration.loadConfiguration(levelsFile);
            ConfigurationSection levelsSection = levelsConfig.getConfigurationSection("levels");

            if (levelsSection == null) {
                Main.getInstance().getLogger().severe("Section 'levels' introuvable dans levels.yml !");
                return;
            }

            if (levelsSection != null) {
                for (String levelKey : levelsSection.getKeys(false)) {
                    try {
                        int levelNumber = Integer.parseInt(levelKey);
                        ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);

                        if (levelSection != null) {
                            LevelConfig levelConfig = parseLevelConfig(levelNumber, levelSection);
                            if (levelConfig != null) {
                                levels.put(levelNumber, levelConfig);
                                // Log supprimé pour éviter le spam - sera remplacé par un résumé
                            } else {
                                Main.getInstance().getLogger().warning("Impossible de parser le niveau " + levelNumber);
                            }
                        } else {
                            Main.getInstance().getLogger().warning("Section null pour le niveau " + levelKey);
                        }
                    } catch (NumberFormatException e) {
                        Main.getInstance().getLogger().warning("Clé de niveau invalide: " + levelKey + " (doit être un nombre)");
                    }
                }
            }

            // Log récapitulatif du chargement
            int totalLevels = levels.size();
            int currencyLevels = (int) levels.values().stream().filter(config -> config.getType().isCurrency()).count();
            int missionLevels = (int) levels.values().stream().filter(config -> config.getType().isMission()).count();
            
            Main.getInstance().getLogger().info("§a" + totalLevels + " niveaux chargés depuis levels.yml");
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur lors du chargement de levels.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parse la configuration d'un niveau depuis sa section YAML
     */
    private LevelConfig parseLevelConfig(int levelNumber, ConfigurationSection section) {
        String name = section.getString("name", "Niveau " + levelNumber);
        String description = section.getString("description", "Description du niveau " + levelNumber);

        Material material;
        try {
            material = Material.valueOf(section.getString("material", "GRASS_BLOCK").toUpperCase());
        } catch (IllegalArgumentException e) {
            Main.getInstance().getLogger().warning("Matériau invalide pour le niveau " + levelNumber + ": " + section.getString("material"));
            material = Material.GRASS_BLOCK;
        }

        boolean enchanted = section.getBoolean("enchanted", false);

        // Parse le type de niveau
        LevelType levelType = parseLevelType(section.getConfigurationSection("type"));

        // Parse les récompenses
        RewardConfig rewards = parseRewards(section.getConfigurationSection("rewards"));

        return new LevelConfig(levelNumber, name, material, enchanted, description, levelType, rewards);
    }

    /**
     * Parse les récompenses d'un niveau
     */
    private RewardConfig parseRewards(ConfigurationSection rewardsSection) {
        if (rewardsSection == null) {
            return new RewardConfig(new ArrayList<>(), new ArrayList<>(), false, new ArrayList<>());
        }

        List<String> commands = rewardsSection.getStringList("commands");
        List<String> rewardsMessage = rewardsSection.getStringList("rewards-message");
        boolean broadcast = rewardsSection.getBoolean("broadcast", false);
        List<String> broadcastMessage = rewardsSection.getStringList("broadcast-message");

        return new RewardConfig(commands, rewardsMessage, broadcast, broadcastMessage);
    }

    /**
     * Parse le type de niveau (currency, blockbreak, blockplace, kills, fish)
     */
    private LevelType parseLevelType(ConfigurationSection typeSection) {
        if (typeSection == null) {
            return new LevelType("currency", 0, null, null, 0);
        }

        String typeName = typeSection.getString("name", "currency");
        double cost = typeSection.getDouble("cost", 0);
        String material = typeSection.getString("material");
        String mob = typeSection.getString("mob");
        int amount = typeSection.getInt("amount", 0);

        return new LevelType(typeName, cost, material, mob, amount);
    }

    /**
     * Obtient la configuration d'un niveau spécifique
     */
    public LevelConfig getLevelConfig(int level) {
        return levels.get(level);
    }

    /**
     * Obtient toutes les configurations de niveau
     */
    public Map<Integer, LevelConfig> getAllLevels() {
        return new HashMap<>(levels);
    }

    /**
     * Obtient le niveau maximum défini dans la configuration
     */
    public int getMaxLevel() {
        return levels.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /**
     * Vérifie si un niveau existe dans la configuration
     */
    public boolean levelExists(int level) {
        return levels.containsKey(level);
    }

    /**
     * Obtient le nombre total de niveaux configurés
     */
    public int getTotalLevels() {
        return levels.size();
    }

    /**
     * Obtient tous les niveaux triés par numéro
     */
    public List<LevelConfig> getSortedLevels() {
        return levels.values().stream()
                .sorted(Comparator.comparing(LevelConfig::getLevelNumber))
                .toList();
    }

    /**
     * Recharge la configuration des niveaux
     */
    public void reload() {
        levels.clear();
        loadLevelsConfig();
    }

    /**
     * Obtient les niveaux par type
     */
    public List<LevelConfig> getLevelsByType(String typeName) {
        return levels.values().stream()
                .filter(config -> config.getType().getName().equalsIgnoreCase(typeName))
                .sorted(Comparator.comparing(LevelConfig::getLevelNumber))
                .toList();
    }

    /**
     * Obtient les niveaux de type currency
     */
    public List<LevelConfig> getCurrencyLevels() {
        return getLevelsByType("currency");
    }

    /**
     * Obtient les niveaux de type missions
     */
    public List<LevelConfig> getMissionLevels() {
        return getLevelsByType("missions");
    }

    /**
     * Configuration d'un niveau chargée depuis levels.yml
     */
    public static class LevelConfig {
        private final int levelNumber;
        private final String name;
        private final Material material;
        private final boolean enchanted;
        private final String description;
        private final LevelType type;
        private final RewardConfig rewards;

        public LevelConfig(int levelNumber, String name, Material material, boolean enchanted,
                          String description, LevelType type, RewardConfig rewards) {
            this.levelNumber = levelNumber;
            this.name = name;
            this.material = material;
            this.enchanted = enchanted;
            this.description = description;
            this.type = type;
            this.rewards = rewards;
        }

        // Getters
        public int getLevelNumber() { return levelNumber; }
        public String getName() { return name; }
        public Material getMaterial() { return material; }
        public boolean isEnchanted() { return enchanted; }
        public String getDescription() { return description; }
        public LevelType getType() { return type; }
        public RewardConfig getRewards() { return rewards; }
    }

    /**
     * Configuration des récompenses d'un niveau
     */
    public static class RewardConfig {
        private final List<String> commands;
        private final List<String> rewardsMessage;
        private final boolean broadcast;
        private final List<String> broadcastMessage;

        public RewardConfig(List<String> commands, List<String> rewardsMessage, boolean broadcast, List<String> broadcastMessage) {
            this.commands = commands != null ? commands : new ArrayList<>();
            this.rewardsMessage = rewardsMessage != null ? rewardsMessage : new ArrayList<>();
            this.broadcast = broadcast;
            this.broadcastMessage = broadcastMessage != null ? broadcastMessage : new ArrayList<>();
        }

        // Getters
        public List<String> getCommands() { return commands; }
        public List<String> getRewardsMessage() { return rewardsMessage; }
        public boolean isBroadcast() { return broadcast; }
        public List<String> getBroadcastMessage() { return broadcastMessage; }
    }

    /**
     * Types de niveau supportés
     */
    public static class LevelType {
        private final String name;
        private final double cost;
        private final String material;
        private final String mob;
        private final int amount;

        public LevelType(String name, double cost, String material, String mob, int amount) {
            this.name = name;
            this.cost = cost;
            this.material = material;
            this.mob = mob;
            this.amount = amount;
        }

        // Getters
        public String getName() { return name; }
        public double getCost() { return cost; }
        public String getMaterial() { return material; }
        public String getMob() { return mob; }
        public int getAmount() { return amount; }

        public boolean isCurrency() { return "currency".equalsIgnoreCase(name); }
        public boolean isMission() {
            // Reconnaître tous les types de mission valides
            return "kills".equalsIgnoreCase(name) ||
                   "blockbreak".equalsIgnoreCase(name) ||
                   "blockplace".equalsIgnoreCase(name) ||
                   "fish".equalsIgnoreCase(name);
        }
    }
}
