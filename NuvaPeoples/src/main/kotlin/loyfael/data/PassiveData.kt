package loyfael.data

/**
 * Enumeration representing the different types of passives
 */
enum class PassiveType {
    // Permanent passives - Bastorgnes
    HEMATOMES_PERSISTANTS,  // Bonus damage with clubs
    RESISTANCE_ENDURCIE,    // Damage reduction
    
    // Permanent passives - Tartinuits  
    SANTE_FLORISSANTE,      // Regeneration if hunger full
    
    // Permanent passives - Sylvounets
    FORCE_SYLVESTRE,        // Bonus damage bow/crossbow
    AGILITE_FORESTIERE,     // Speed in forest biomes
    
    // Permanent passives - Grosuki
    
    // Permanent passives - Bricobrak
    OUTILS_BRICOLEURS,      // Reduced durability loss
    RESSORT_BRICOLE,        // Permanent Jump Boost
    
    // Permanent passives - Miraziens
    PLUME_LEGERE,           // Reduced fall damage
    PRECISION_AERIENNE,     // Chance weakness on arrows
    
    // Passives with cooldown
    PAIN_NOURRICIER,        // Absorption when eating bread (Tartinuits)
    SERENITE_LUNAIRE,       // Resistance if <25% HP (Grosuki) 
    ESPRIT_APAISE,          // Regeneration when sneaking (Grosuki)
    
    // Additional passives - Miraziens
    VISION_DIMENSIONNELLE,  // Dimensional vision
    
    // Additional passives - Bastorgnes
    BERSERKER_SANGUINAIRE;  // Berserker mode
    
    /**
     * Gets the display name for this passive type
     */
    fun getDisplayName(): String {
        return when (this) {
            HEMATOMES_PERSISTANTS -> "Hématomes persistants"
            RESISTANCE_ENDURCIE -> "Résistance endurcie"
            SANTE_FLORISSANTE -> "Santé florissante"
            FORCE_SYLVESTRE -> "Force sylvestre"
            AGILITE_FORESTIERE -> "Agilité forestière"
            OUTILS_BRICOLEURS -> "Outils bricoleurs"
            RESSORT_BRICOLE -> "Ressort bricolé"
            PLUME_LEGERE -> "Plume légère"
            PRECISION_AERIENNE -> "Précision aérienne"
            PAIN_NOURRICIER -> "Pain nourricier"
            SERENITE_LUNAIRE -> "Sérénité lunaire"
            ESPRIT_APAISE -> "Esprit apaisé"
            VISION_DIMENSIONNELLE -> "Vision dimensionnelle"
            BERSERKER_SANGUINAIRE -> "Berserker sanguinaire"
        }
    }
}

/**
 * Data class representing an active passive on a player
 */
data class ActivePassive(
    val type: PassiveType,
    val player: java.util.UUID,
    var lastTrigger: Long = 0L,
    var cooldownDuration: Int = 0,
    var active: Boolean = true
) {
    fun isOnCooldown(): Boolean = System.currentTimeMillis() - lastTrigger < (cooldownDuration * 1000L)
    
    fun startCooldown(duration: Int) {
        lastTrigger = System.currentTimeMillis()
        cooldownDuration = duration
    }
    
    fun getRemainingCooldown(): Int {
        val elapsedTime = (System.currentTimeMillis() - lastTrigger) / 1000
        return maxOf(0, cooldownDuration - elapsedTime.toInt())
    }
}

/**
 * Data class representing player data
 */
data class PlayerData(
    val uuid: java.util.UUID,
    var classe: Classe = Classe.AME_ERRANTE,
    var selectionDate: Long = System.currentTimeMillis(),
    val activePassives: MutableMap<PassiveType, ActivePassive> = mutableMapOf(),
    
    // Temporary data for certain passives
    var sneakingTime: Long = 0L,
    var lastBiome: String = "",
    var lastPosY: Double = 0.0
) {
    fun addPassive(type: PassiveType): ActivePassive {
        val passive = ActivePassive(type, uuid)
        activePassives[type] = passive
        return passive
    }
    
    fun removePassive(type: PassiveType) {
        activePassives.remove(type)
    }
    
    fun getPassive(type: PassiveType): ActivePassive? = activePassives[type]
    
    fun hasPassive(type: PassiveType): Boolean = activePassives.containsKey(type)
    
    fun changeClasse(newClasse: Classe) {
        classe = newClasse
        selectionDate = System.currentTimeMillis()
        // Clear old passives
        activePassives.clear()
        sneakingTime = 0L
        lastBiome = ""
    }
}
