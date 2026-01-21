package loyfael.data

import java.util.*

/**
 * Classe de données représentant un joueur
 * 
 * Cette classe d'exemple montre comment structurer vos données.
 * Adaptez-la selon vos besoins spécifiques.
 */
data class PlayerData(
    val uuid: UUID,
    val username: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    
    /**
     * Constructeur alternatif pour UUID sous forme de String
     */
    constructor(
        uuidString: String,
        username: String,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
        isActive: Boolean = true
    ) : this(UUID.fromString(uuidString), username, createdAt, updatedAt, isActive)
    
    /**
     * Vérifie si le joueur s'est connecté récemment
     * @param hoursThreshold Seuil en heures
     */
    fun hasLoggedInRecently(hoursThreshold: Int = 24): Boolean {
        val thresholdMillis = hoursThreshold * 60 * 60 * 1000L
        return System.currentTimeMillis() - updatedAt <= thresholdMillis
    }
    
    /**
     * Obtient l'âge du compte en jours
     */
    fun getAccountAgeInDays(): Long {
        return (System.currentTimeMillis() - createdAt) / (1000 * 60 * 60 * 24)
    }
    
    /**
     * Copie avec une nouvelle date de mise à jour
     */
    fun withUpdatedTimestamp(): PlayerData {
        return copy(updatedAt = System.currentTimeMillis())
    }
    
    /**
     * Représentation en chaîne de caractères
     */
    override fun toString(): String {
        return "PlayerData(uuid=$uuid, username='$username', active=$isActive)"
    }
}