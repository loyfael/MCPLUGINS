package loyfael.litefish.seasons;

import org.bukkit.Bukkit;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages seasonal fishing bonuses and special drops
 */
public class SeasonManager {
    
    private final Map<Season, SeasonData> seasonBonuses;
    
    public SeasonManager() {
        this.seasonBonuses = new HashMap<>();
        
        initializeSeasons();
    }
    
    private void initializeSeasons() {
        // Spring (March, April, May)
        seasonBonuses.put(Season.SPRING, new SeasonData(
            "Spring", 1.2, 1.1, "Fish are more active in spring!"
        ));
        
        // Summer (June, July, August) 
        seasonBonuses.put(Season.SUMMER, new SeasonData(
            "Summer", 1.0, 1.3, "Hot summer fishing!"
        ));
        
        // Autumn (September, October, November)
        seasonBonuses.put(Season.AUTUMN, new SeasonData(
            "Autumn", 1.4, 1.1, "Autumn brings rare fish!"
        ));
        
        // Winter (December, January, February)
        seasonBonuses.put(Season.WINTER, new SeasonData(
            "Winter", 1.1, 0.9, "Cold winter fishing is challenging but rewarding!"
        ));
    }
    
    /**
     * Get the current season based on real date
     */
    public Season getCurrentSeason() {
        LocalDate now = LocalDate.now();
        Month month = now.getMonth();
        
        switch (month) {
            case MARCH:
            case APRIL:
            case MAY:
                return Season.SPRING;
                
            case JUNE:
            case JULY:
            case AUGUST:
                return Season.SUMMER;
                
            case SEPTEMBER:
            case OCTOBER:
            case NOVEMBER:
                return Season.AUTUMN;
                
            case DECEMBER:
            case JANUARY:
            case FEBRUARY:
                return Season.WINTER;
                
            default:
                return Season.SPRING;
        }
    }
    
    /**
     * Get seasonal data for current season
     */
    public SeasonData getCurrentSeasonData() {
        return seasonBonuses.get(getCurrentSeason());
    }
    
    /**
     * Get experience multiplier for current season
     */
    public double getSeasonalExpMultiplier() {
        return getCurrentSeasonData().getExpMultiplier();
    }
    
    /**
     * Get drop rate multiplier for current season
     */
    public double getSeasonalDropMultiplier() {
        return getCurrentSeasonData().getDropMultiplier();
    }
    
    /**
     * Announce season change
     */
    public void announceSeasonChange() {
        SeasonData seasonData = getCurrentSeasonData();
        Bukkit.broadcastMessage("§6[LiteFish] §eNow in " + seasonData.getName() + " season!");
        Bukkit.broadcastMessage("§6[LiteFish] §e" + seasonData.getDescription());
        Bukkit.broadcastMessage("§6[LiteFish] §eExp: §a" + (int)((seasonData.getExpMultiplier() - 1) * 100) + "% §eDrop Rate: §a" + (int)((seasonData.getDropMultiplier() - 1) * 100) + "%");
    }
    
    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }
    
    public static class SeasonData {
        private final String name;
        private final double expMultiplier;
        private final double dropMultiplier;
        private final String description;
        
        public SeasonData(String name, double expMultiplier, double dropMultiplier, String description) {
            this.name = name;
            this.expMultiplier = expMultiplier;
            this.dropMultiplier = dropMultiplier;
            this.description = description;
        }
        
        public String getName() { return name; }
        public double getExpMultiplier() { return expMultiplier; }
        public double getDropMultiplier() { return dropMultiplier; }
        public String getDescription() { return description; }
    }
}
