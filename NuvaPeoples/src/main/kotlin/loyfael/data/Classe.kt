package loyfael.data

/**
 * Enumeration representing the different available classes/peoples
 */
enum class Classe(
    val displayName: String,
    val emoji: String,
    val description: String,
    val passives: List<String>
) {
    BASTORGNES(
        "Bastorgnes", 
        "⚒",
        "Orqs guerriers, solides au combat",
        listOf("Frappe lourde", "Peau de fer")
    ),
    
    TARTINUITS(
        "Tartinuits",
        "🥖", 
        "Hobbits paisibles, experts en survie",
        listOf("Pain nourricier", "Santé florissante")
    ),
    
    SYLVOUNETS(
        "Sylvounets",
        "🌿",
        "Elfes agiles, maîtres de l'arc",
        listOf("Tir sylvestre", "Agilité forestière")
    ),
    
    GROSUKI(
        "Grosuki", 
        "🦊",
        "Hommes panda/kitsunes méditatifs",
        listOf("Sérénité lunaire", "Esprit apaisé")
    ),
    
    BRICOBRAK(
        "Bricobrak",
        "💥", 
        "Humains inventifs et bricoleurs",
        listOf("Outils bricoleurs", "Ressort bricolé")
    ),
    
    MIRAZIENS(
        "Miraziens",
        "🐦",
        "Hommes oiseaux, snipers aériens", 
        listOf("Plume légère", "Précision aérienne")
    ),
    
    AME_ERRANTE(
        "Âme errante",
        "👻",
        "Classe par défaut, sans bonus",
        emptyList()
    );
    
    fun getFullDisplayName(): String = "$emoji $displayName"
    
    /**
     * Get singular form of class name
     */
    fun getSingularName(): String {
        return when (this) {
            BASTORGNES -> "Bastorgne"
            TARTINUITS -> "Tartinuit" 
            SYLVOUNETS -> "Sylvounet"
            GROSUKI -> "Grosuki" // Already singular
            BRICOBRAK -> "Bricobrak" // Already singular
            MIRAZIENS -> "Mirazien"
            AME_ERRANTE -> "Âme errante" // Already singular
        }
    }
    
    /**
     * Get the color code for the class
     */
    fun getCouleur(): String {
        return when (this) {
            BASTORGNES -> "§c" // Rouge
            TARTINUITS -> "§6" // Orange/Or
            SYLVOUNETS -> "§a" // Vert
            MIRAZIENS -> "§e" // Jaune
            BRICOBRAK -> "§3" // Cyan foncé
            GROSUKI -> "§5" // Violet
            AME_ERRANTE -> "§7" // Gris
        }
    }
    
    companion object {
        fun fromString(name: String): Classe? {
            return values().find { 
                it.name.equals(name, ignoreCase = true) || 
                it.displayName.equals(name, ignoreCase = true) 
            }
        }
        
        fun getAllClasses(): List<Classe> = values().toList()
        
        fun getSelectableClasses(): List<Classe> = values().filter { it != AME_ERRANTE }
    }
}
