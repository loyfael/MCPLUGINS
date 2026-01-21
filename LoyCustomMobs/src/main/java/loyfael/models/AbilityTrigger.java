package loyfael.models;

/**
 * Defines when mob abilities can be triggered
 */
public enum AbilityTrigger {
    ON_SPAWN("On Spawn"),
    ON_ATTACK("On Attack"),
    ON_DAMAGED("On Damaged"),
    ON_DEATH("On Death"),
    PERIODIC("Periodic"),
    ON_LOW_HEALTH("On Low Health"),
    ON_TARGET_ACQUIRED("On Target Acquired"),
    ON_BLOCK_BREAK("On Block Break"),
    PASSIVE("Passive");

    private final String displayName;

    AbilityTrigger(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
