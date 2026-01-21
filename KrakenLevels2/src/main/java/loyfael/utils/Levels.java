package loyfael.utils;

import java.util.List;
import java.util.ArrayList;

/**
 * Classe représentant un niveau dans le système KrakenLevels
 * Contient toutes les informations d'un niveau (nom, matériau, récompenses, etc.)
 */
public final class Levels {

    private String name;
    private String material;
    private List<String> lore;
    private int level;
    private boolean enchanted;
    private String type;
    private List<String> rewards; // Ancienne compatibilité
    private List<String> commands; // Nouvelles commandes séparées
    private List<String> rewardsMessage; // Nouveaux messages de récompense séparés
    private int target; // Objectif à atteindre pour ce niveau

    /**
     * Constructeur par défaut
     */
    public Levels() {
        this.name = "";
        this.material = "STONE";
        this.lore = new ArrayList<>();
        this.level = 1;
        this.enchanted = false;
        this.type = "BREAK";
        this.rewards = new ArrayList<>();
        this.commands = new ArrayList<>();
        this.rewardsMessage = new ArrayList<>();
        this.target = 100;
    }

    /**
     * Constructeur avec paramètres essentiels
     */
    public Levels(String name, String material, int level, String type) {
        this();
        this.name = name;
        this.material = material;
        this.level = level;
        this.type = type;
    }

    // Getters
    public String getName() {
        return name != null ? name : "";
    }

    public String getMaterial() {
        return material != null ? material : "STONE";
    }

    public List<String> getLore() {
        return lore != null ? lore : new ArrayList<>();
    }

    public int getLevel() {
        return level;
    }

    public boolean isEnchanted() {
        return enchanted;
    }

    public String getType() {
        return type != null ? type : "BREAK";
    }

    public List<String> getRewards() {
        return rewards != null ? rewards : new ArrayList<>();
    }

    public int getTarget() {
        return target;
    }

    /**
     * Gets the commands to execute (new structure)
     */
    public List<String> getCommands() {
        try {
            if (commands != null && !commands.isEmpty()) {
                return new ArrayList<>(commands);
            }
            // Fallback to old structure for compatibility
            return getRewards();
        } catch (Exception e) {
            System.err.println("Error getting commands for level " + level + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Sets the commands to execute
     */
    public void setCommands(List<String> commands) {
        try {
            this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error setting commands for level " + level + ": " + e.getMessage());
            this.commands = new ArrayList<>();
        }
    }

    /**
     * Gets the reward messages (new structure)
     */
    public List<String> getRewardsMessage() {
        try {
            return rewardsMessage != null ? new ArrayList<>(rewardsMessage) : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error getting reward messages for level " + level + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Sets the reward messages
     */
    public void setRewardsMessage(List<String> rewardsMessage) {
        try {
            this.rewardsMessage = rewardsMessage != null ? new ArrayList<>(rewardsMessage) : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error setting reward messages for level " + level + ": " + e.getMessage());
            this.rewardsMessage = new ArrayList<>();
        }
    }

    // Setters with validation and error handling
    public void setName(String name) {
        try {
            this.name = name != null ? name : "";
        } catch (Exception e) {
            System.err.println("Error setting name for level " + level + ": " + e.getMessage());
            this.name = "Unknown Level";
        }
    }

    public void setMaterial(String material) {
        try {
            this.material = material != null ? material : "STONE";
        } catch (Exception e) {
            System.err.println("Error setting material for level " + level + ": " + e.getMessage());
            this.material = "STONE";
        }
    }

    public void setLore(List<String> lore) {
        this.lore = lore != null ? lore : new ArrayList<>();
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level); // Minimum niveau 1
    }

    public void setEnchanted(boolean enchanted) {
        this.enchanted = enchanted;
    }

    public void setType(String type) {
        this.type = type != null ? type.toUpperCase() : "BREAK";
    }

    public void setRewards(List<String> rewards) {
        this.rewards = rewards != null ? rewards : new ArrayList<>();
    }

    public void setTarget(int target) {
        this.target = Math.max(1, target); // Minimum 1
    }

    // ====================================
    // NOUVELLES MÉTHODES POUR LES PRÉREQUIS
    // ====================================

    private String typeName; // Type du niveau (currency, missions, etc.)
    private int cost; // Coût en argent
    private String missionType; // Type de mission (blockbreak, fishing, etc.)
    private String missionMaterial; // Matériau pour la mission
    private int amount; // Quantité requise pour la mission

    /**
     * Obtient le nom du type de niveau
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Définit le nom du type de niveau
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Obtient le coût en argent du niveau
     */
    public int getCost() {
        return cost;
    }

    /**
     * Définit le coût en argent du niveau
     */
    public void setCost(int cost) {
        this.cost = Math.max(0, cost);
    }

    /**
     * Obtient le type de mission
     */
    public String getMissionType() {
        return missionType;
    }

    /**
     * Définit le type de mission
     */
    public void setMissionType(String missionType) {
        this.missionType = missionType;
    }

    /**
     * Obtient le matériau de la mission
     */
    public String getMissionMaterial() {
        return missionMaterial;
    }

    /**
     * Définit le matériau de la mission
     */
    public void setMissionMaterial(String missionMaterial) {
        this.missionMaterial = missionMaterial;
    }

    /**
     * Obtient la quantité requise pour la mission
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Définit la quantité requise pour la mission
     */
    public void setAmount(int amount) {
        this.amount = Math.max(1, amount);
    }

    // ====================================
    // MÉTHODES UTILITAIRES
    // ====================================

    /**
     * Vérifie si ce niveau est un niveau d'économie
     */
    public boolean isCurrencyLevel() {
        return "currency".equalsIgnoreCase(typeName);
    }

    /**
     * Vérifie si ce niveau est un niveau de mission
     */
    public boolean isMissionLevel() {
        return "missions".equalsIgnoreCase(typeName);
    }

    /**
     * Obtient une description du prérequis pour ce niveau
     */
    public String getRequirementDescription() {
        if (isCurrencyLevel() && cost > 0) {
            return "Coût: " + cost + "◎";
        } else if (isMissionLevel()) {
            return "Mission: " + missionType + " " + missionMaterial + " (" + amount + ")";
        }
        return "Aucun prérequis";
    }

    /**
     * Clone ce niveau
     */
    public Levels clone() {
        Levels cloned = new Levels();
        cloned.setName(this.name);
        cloned.setMaterial(this.material);
        cloned.setLore(new ArrayList<>(this.getLore()));
        cloned.setLevel(this.level);
        cloned.setEnchanted(this.enchanted);
        cloned.setType(this.type);
        cloned.setRewards(new ArrayList<>(this.getRewards()));
        cloned.setTarget(this.target);
        cloned.setTypeName(this.typeName);
        cloned.setCost(this.cost);
        cloned.setMissionType(this.missionType);
        cloned.setMissionMaterial(this.missionMaterial);
        cloned.setAmount(this.amount);
        cloned.setCommands(new ArrayList<>(this.getCommands()));
        cloned.setRewardsMessage(new ArrayList<>(this.getRewardsMessage()));
        return cloned;
    }

    /**
     * Représentation textuelle du niveau pour le debug
     */
    @Override
    public String toString() {
        return "Level{" +
                "level=" + level +
                ", name='" + name + '\'' +
                ", type='" + typeName + '\'' +
                ", cost=" + cost +
                ", missionType='" + missionType + '\'' +
                ", material='" + missionMaterial + '\'' +
                ", amount=" + amount +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Levels other = (Levels) obj;
        return level == other.level &&
               name.equals(other.name) &&
               type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, level, type);
    }
}
