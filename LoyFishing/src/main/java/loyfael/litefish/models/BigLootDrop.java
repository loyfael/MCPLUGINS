package loyfael.litefish.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Represents a big loot drop - rare special rewards
 */
public class BigLootDrop {
    
    private final String id;
    private final String displayName;
    private final Material material;
    private final double chance;
    private final int experience;
    private final double price;
    private final List<String> lore;
    private final boolean isNexoItem;
    private final String nexoItemId;
    
    public BigLootDrop(String id, String displayName, Material material, double chance, 
                       int experience, double price, List<String> lore, 
                       boolean isNexoItem, String nexoItemId) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.chance = chance;
        this.experience = experience;
        this.price = price;
        this.lore = lore;
        this.isNexoItem = isNexoItem;
        this.nexoItemId = nexoItemId;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public double getChance() {
        return chance;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public double getPrice() {
        return price;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public boolean isNexoItem() {
        return isNexoItem;
    }
    
    public String getNexoItemId() {
        return nexoItemId;
    }
    
    public ItemStack createItem() {
        ItemStack item = new ItemStack(material);
        // TODO: Implement proper item creation with lore and custom properties
        return item;
    }
}
