package loyfael.litefish.models;

import java.util.List;
import java.util.Random;

/**
 * Represents a group of drops that are randomly selected from
 */
public class GroupDrop {
    
    private final String id;
    private final String name;
    private final List<String> fishKeys;
    private final double chance;
    private final Random random;
    
    public GroupDrop(String id, String name, List<String> fishKeys, double chance) {
        this.id = id;
        this.name = name;
        this.fishKeys = fishKeys;
        this.chance = chance;
        this.random = new Random();
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public List<String> getFishKeys() {
        return fishKeys;
    }
    
    public double getChance() {
        return chance;
    }
    
    /**
     * Get a random fish from this group
     */
    public String getRandomFish() {
        if (fishKeys.isEmpty()) {
            return null;
        }
        
        return fishKeys.get(random.nextInt(fishKeys.size()));
    }
    
    /**
     * Check if this group contains a specific fish
     */
    public boolean containsFish(String fishKey) {
        return fishKeys.contains(fishKey);
    }
    
    /**
     * Add a fish to this group
     */
    public void addFish(String fishKey) {
        if (!fishKeys.contains(fishKey)) {
            fishKeys.add(fishKey);
        }
    }
    
    /**
     * Remove a fish from this group
     */
    public void removeFish(String fishKey) {
        fishKeys.remove(fishKey);
    }
}
