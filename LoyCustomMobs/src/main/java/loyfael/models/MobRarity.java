package loyfael.models;

/**
 * Represents different rarity levels for custom mobs
 */
public enum MobRarity {
    COMMON("Commun", 1, 0.7f, 1, 2, 1, 1.5, 1.25, 1.10),
    UNCOMMON("Peu commun", 2, 0.5f, 2, 3, 2, 1.75, 1.35, 1.20),
    RARE("Rare", 3, 0.3f, 3, 4, 3, 2.0, 1.55, 1.30),
    EPIC("Épique", 5, 0.15f, 4, 5, 4, 2.5, 1.85, 1.40),
    LEGENDARY("Légendaire", 8, 0.05f, 5, 6, 5, 3.0, 2.15, 1.50),
    MYTHIC("Mythique", 12, 0.01f, 6, 7, 6, 3.5, 2.35, 1.55);

    private final String displayName;
    private final int defaultLives;
    private final float spawnChance;
    private final int minAbilities;
    private final int maxAbilities;
    private final int tier;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double speedMultiplier;

    MobRarity(String displayName, int defaultLives, float spawnChance,
              int minAbilities, int maxAbilities,
              int tier, double healthMultiplier, double damageMultiplier, double speedMultiplier) {
        this.displayName = displayName;
        this.defaultLives = defaultLives;
        this.spawnChance = spawnChance;
        this.minAbilities = minAbilities;
        this.maxAbilities = maxAbilities;
        this.tier = tier;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.speedMultiplier = speedMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultLives() {
        return defaultLives;
    }

    public float getSpawnChance() {
        return spawnChance;
    }

    public int getMinAbilities() { return minAbilities; }

    public int getMaxAbilities() { return maxAbilities; }

    public int getTier() {
        return tier;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public double getXpMultiplier() {
        return 1.0 + 0.25 * (tier - 1);
    }

    public double computeLootMultiplier(double randomFactor) {
        return Math.max(1.0, tier * 0.5 + randomFactor);
    }

    public double getLowHealthThreshold() {
        return tier >= 4 ? 0.30 : 0.25;
    }

    /**
     * Get a random rarity based on spawn chances
     */
    public static MobRarity getRandomRarity() {
        double random = Math.random();
        double cumulativeChance = 0.0;

        for (MobRarity rarity : values()) {
            cumulativeChance += rarity.spawnChance;
            if (random <= cumulativeChance) {
                return rarity;
            }
        }

        return COMMON; // fallback
    }
}
