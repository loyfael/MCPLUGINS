package loyfael.models;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.UUID;

/**
 * Represents a custom mob with special abilities and properties
 */
public class CustomMob {
    private final UUID id;
    private final LivingEntity entity;
    private final MobRarity rarity;
    private final List<MobAbility> abilities;
    private int lives;
    private String effectName;
    private boolean isActive;
    private String displayName;
    private String epithet;
    private String deathQuip;
    private String familyTag;

    public CustomMob(LivingEntity entity, MobRarity rarity, List<MobAbility> abilities) {
        this.entity = entity;
        this.id = entity.getUniqueId();
        this.rarity = rarity;
        this.abilities = abilities;
        this.lives = rarity.getDefaultLives();
        this.isActive = true;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobRarity getRarity() {
        return rarity;
    }

    public List<MobAbility> getAbilities() {
        return abilities;
    }

    public int getLives() {
        return lives;
    }

    public String getEffectName() {
        return effectName;
    }

    public boolean isActive() {
        return isActive;
    }

    // Setters
    public void setLives(int lives) {
        this.lives = lives;
    }

    public void setEffectName(String effectName) {
        this.effectName = effectName;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEpithet() {
        return epithet;
    }

    public void setEpithet(String epithet) {
        this.epithet = epithet;
    }

    public String getDeathQuip() {
        return deathQuip;
    }

    public void setDeathQuip(String deathQuip) {
        this.deathQuip = deathQuip;
    }

    public String getFamilyTag() {
        return familyTag;
    }

    public void setFamilyTag(String familyTag) {
        this.familyTag = familyTag;
    }

    public void decrementLives() {
        this.lives = Math.max(0, this.lives - 1);
    }

    public boolean isDead() {
        return !isActive || entity.isDead() || lives <= 0;
    }

    @Override
    public String toString() {
        return "CustomMob{" +
                "type=" + entity.getType().name() +
                ", rarity=" + rarity +
                ", abilities=" + abilities.size() +
                ", lives=" + lives +
        ", name=" + displayName +
                "}";
    }
}
