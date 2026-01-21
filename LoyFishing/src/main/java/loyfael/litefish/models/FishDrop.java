package loyfael.litefish.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a fishing drop configuration
 */
public class FishDrop {
    
    private final String key;
    private String displayName;
    private Material material;
    private double chance;
    private int experience;
    private double price;
    private List<String> biomes;
    private String nexoItem;
    private Map<Enchantment, Integer> enchantments;
    private boolean randomEnchantments;
    
    public FishDrop(String key, String displayName, Material material, double chance, 
                    int experience, double price, List<String> biomes, String nexoItem) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.chance = chance;
        this.experience = experience;
        this.price = price;
        this.biomes = biomes;
        this.nexoItem = nexoItem;
        this.enchantments = new HashMap<>();
        this.randomEnchantments = false;
    }
    
    public FishDrop(String key, String displayName, Material material, double chance, 
                    int experience, double price, List<String> biomes, String nexoItem,
                    Map<Enchantment, Integer> enchantments, boolean randomEnchantments) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.chance = chance;
        this.experience = experience;
        this.price = price;
        this.biomes = biomes;
        this.nexoItem = nexoItem;
        this.enchantments = enchantments != null ? enchantments : new HashMap<>();
        this.randomEnchantments = randomEnchantments;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public void setMaterial(Material material) {
        this.material = material;
    }
    
    public double getChance() {
        return chance;
    }
    
    public void setChance(double chance) {
        this.chance = Math.max(0.0, chance);
    }
    
    public int getExperience() {
        return experience;
    }
    
    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = Math.max(0.0, price);
    }
    
    public List<String> getBiomes() {
        return biomes;
    }
    
    public void setBiomes(List<String> biomes) {
        this.biomes = biomes;
    }
    
    public String getNexoItem() {
        return nexoItem;
    }
    
    public void setNexoItem(String nexoItem) {
        this.nexoItem = nexoItem;
    }
    
    public boolean isNexoItem() {
        return nexoItem != null && !nexoItem.isEmpty();
    }
    
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }
    
    public void setEnchantments(Map<Enchantment, Integer> enchantments) {
        this.enchantments = enchantments;
    }
    
    public boolean isRandomEnchantments() {
        return randomEnchantments;
    }
    
    public void setRandomEnchantments(boolean randomEnchantments) {
        this.randomEnchantments = randomEnchantments;
    }
    
    public boolean hasEnchantments() {
        return enchantments != null && !enchantments.isEmpty();
    }
    
    @Override
    public String toString() {
        return "FishDrop{" +
                "key='" + key + '\'' +
                ", displayName='" + displayName + '\'' +
                ", material=" + material +
                ", chance=" + chance +
                ", experience=" + experience +
                ", price=" + price +
                ", biomes=" + biomes +
                ", nexoItem='" + nexoItem + '\'' +
                '}';
    }
}
