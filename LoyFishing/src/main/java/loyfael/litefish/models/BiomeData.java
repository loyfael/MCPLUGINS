package loyfael.litefish.models;

import org.bukkit.block.Biome;

/**
 * Represents biome-specific fishing data
 */
public class BiomeData {
    
    private final Biome biome;
    private String name;
    private String color;
    private int monsterChance;
    
    public BiomeData(Biome biome, String name, String color, int monsterChance) {
        this.biome = biome;
        this.name = name;
        this.color = color;
        this.monsterChance = monsterChance;
    }
    
    public Biome getBiome() {
        return biome;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public int getMonsterChance() {
        return monsterChance;
    }
    
    public void setMonsterChance(int monsterChance) {
        this.monsterChance = Math.max(0, Math.min(100, monsterChance));
    }
    
    @Override
    public String toString() {
        return "BiomeData{" +
                "biome=" + biome +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", monsterChance=" + monsterChance +
                '}';
    }
}
