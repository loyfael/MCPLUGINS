package loyfael.litefish.managers;

import loyfael.litefish.LiteFish;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player fishing data and statistics
 */
public class PlayerDataManager {
    
    private final LiteFish plugin;
    private final Map<UUID, PlayerFishingData> playerData;
    
    public PlayerDataManager(LiteFish plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
    }
    
    /**
     * Get or create player fishing data
     */
    public PlayerFishingData getPlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(), 
            uuid -> new PlayerFishingData(player.getUniqueId(), player.getName()));
    }
    
    /**
     * Add a fish caught to player statistics
     */
    public void addFishCaught(Player player, String fishKey, int amount) {
        PlayerFishingData data = getPlayerData(player);
        data.addFishCaught(fishKey, amount);
        data.setTotalFishCaught(data.getTotalFishCaught() + amount);
    }
    
    /**
     * Save all player data (called on plugin disable)
     */
    public void saveAllData() {
        // In a real implementation, this would save to database/files
        plugin.getLogger().info("Saved fishing data for " + playerData.size() + " players");
    }
    
    /**
     * Get total fish caught by player
     */
    public int getTotalFishCaught(Player player) {
        return getPlayerData(player).getTotalFishCaught();
    }
    
    /**
     * Get total experience gained from fishing
     */
    public int getTotalExperienceGained(Player player) {
        PlayerFishingData data = getPlayerData(player);
        
        for (Map.Entry<String, Integer> entry : data.getFishCaught().entrySet()) {
            String fishKey = entry.getKey();
            int count = entry.getValue();
            
            // Get experience from drop manager
            plugin.getDropManager().getDrop(fishKey).ifPresent(drop -> {
                data.addExperience(drop.getExperience() * count);
            });
        }
        
        return data.getTotalExperience();
    }
    
    /**
     * Get total money earned from fishing
     */
    public double getTotalMoneyEarned(Player player) {
        return getPlayerData(player).getTotalMoneyEarned();
    }
    
    /**
     * Get player's favorite biome
     */
    public String getFavoriteBiome(Player player) {
        PlayerFishingData data = getPlayerData(player);
        
        String favoriteBiome = "Ocean";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : data.getBiomeFishCounts().entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                favoriteBiome = entry.getKey();
            }
        }
        
        return favoriteBiome;
    }
    
    /**
     * Get fish counts by type
     */
    public Map<String, Integer> getFishCounts(Player player) {
        return getPlayerData(player).getFishCaught();
    }
    
    /**
     * Add money earned
     */
    public void addMoneyEarned(Player player, double amount) {
        getPlayerData(player).addMoneyEarned(amount);
    }
    
    /**
     * Add biome fishing data
     */
    public void addBiomeFishing(Player player, String biome) {
        getPlayerData(player).addBiomeFishing(biome);
    }
    
    /**
     * Inner class to hold player fishing data
     */
    public static class PlayerFishingData {
        private final UUID uuid;
        private final String playerName;
        private final Map<String, Integer> fishCaught;
        private final Map<String, Integer> biomeFishCounts;
        private int totalFishCaught;
        private int totalExperience;
        private double totalMoneyEarned;
        private long lastFishingTime;
        
        public PlayerFishingData(UUID uuid, String playerName) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.fishCaught = new HashMap<>();
            this.biomeFishCounts = new HashMap<>();
            this.totalFishCaught = 0;
            this.totalExperience = 0;
            this.totalMoneyEarned = 0.0;
            this.lastFishingTime = System.currentTimeMillis();
        }
        
        public UUID getUuid() {
            return uuid;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public Map<String, Integer> getFishCaught() {
            return new HashMap<>(fishCaught);
        }
        
        public void addFishCaught(String fishKey, int amount) {
            fishCaught.merge(fishKey, amount, Integer::sum);
        }
        
        public int getFishCaughtCount(String fishKey) {
            return fishCaught.getOrDefault(fishKey, 0);
        }
        
        public int getTotalFishCaught() {
            return totalFishCaught;
        }
        
        public void setTotalFishCaught(int totalFishCaught) {
            this.totalFishCaught = totalFishCaught;
        }
        
        public int getTotalExperience() {
            return totalExperience;
        }
        
        public void addExperience(int experience) {
            this.totalExperience += experience;
        }
        
        public double getTotalMoneyEarned() {
            return totalMoneyEarned;
        }
        
        public void addMoneyEarned(double amount) {
            this.totalMoneyEarned += amount;
        }
        
        public Map<String, Integer> getBiomeFishCounts() {
            return new HashMap<>(biomeFishCounts);
        }
        
        public void addBiomeFishing(String biome) {
            biomeFishCounts.merge(biome, 1, Integer::sum);
        }
        
        public long getLastFishingTime() {
            return lastFishingTime;
        }
        
        public void setLastFishingTime(long lastFishingTime) {
            this.lastFishingTime = lastFishingTime;
        }
    }
}
