package loyfael.entities

import org.bukkit.Material
import java.time.LocalDateTime
import java.util.*

/**
 * Enum représentant les différents statuts d'une commande
 */
enum class StatutCommande {
    EN_ATTENTE,      // Commande créée, en attente d'un livreur
    ACCEPTEE,        // Commande acceptée par un livreur
    PRETE,           // Items déposés, commande prête à être récupérée
    LIVREE,          // Commande récupérée par le client
    ANNULEE_CLIENT,  // Annulée par le client
    ANNULEE_LIVREUR, // Annulée par le livreur
    EXPIREE,         // Non récupérée dans les temps
    ERREUR_TECHNIQUE // Erreur système
}

/**
 * Enum représentant les types d'événements dans l'historique
 */
enum class TypeEvenement {
    COMMANDE_CREEE,
    COMMANDE_ACCEPTEE,
    COMMANDE_LIVREE,
    COMMANDE_RECUPEREE,
    COMMANDE_ANNULEE,
    COMMANDE_EXPIREE,
    REMBOURSEMENT_EFFECTUE,
    PAIEMENT_EFFECTUE
}

/**
 * Data class représentant une commande de livraison
 */
data class Commande(
    val id: Long = 0,
    val uuidClient: UUID,
    val nomClient: String,
    val material: Material,
    val nomItem: String, // Nom affiché (peut être en français)
    val quantite: Int,
    val prixTotal: Double,
    val statut: StatutCommande,
    val createdAt: LocalDateTime,
    val deadline: LocalDateTime, // Date limite pour l'acceptation
    val description: String? = null,
    val bonus: Double = 0.0 // Bonus supplémentaire proposé par le client
) {
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(deadline) && statut == StatutCommande.EN_ATTENTE
    }
    
    fun getDaysRemaining(): Long {
        val now = LocalDateTime.now()
        return if (now.isBefore(deadline)) {
            java.time.Duration.between(now, deadline).toDays()
        } else {
            0
        }
    }
}

/**
 * Data class représentant une livraison (acceptation d'une commande)
 */
data class Livraison(
    val idCommande: Long,
    val uuidLivreur: UUID,
    val nomLivreur: String,
    val statut: StatutCommande, // Même enum que Commande pour cohérence
    val acceptedAt: LocalDateTime,
    val deliveredAt: LocalDateTime? = null,
    val delaiRecuperation: LocalDateTime, // Quand le client doit récupérer
    val itemsDeposes: Boolean = false, // Les items ont été déposés
    val paiementEffectue: Boolean = false // Le livreur a été payé
) {
    fun isRecuperationExpired(): Boolean {
        return LocalDateTime.now().isAfter(delaiRecuperation) && 
               statut == StatutCommande.PRETE
    }
    
    fun getRecuperationDaysRemaining(): Long {
        val now = LocalDateTime.now()
        return if (now.isBefore(delaiRecuperation)) {
            java.time.Duration.between(now, delaiRecuperation).toDays()
        } else {
            0
        }
    }
}

/**
 * Data class pour l'historique des événements
 */
data class HistoriqueEvenement(
    val id: Long = 0,
    val typeEvenement: TypeEvenement,
    val uuidJoueur: UUID,
    val nomJoueur: String,
    val idCommande: Long?,
    val details: String,
    val montant: Double? = null, // Pour les paiements/remboursements
    val date: LocalDateTime = LocalDateTime.now()
)

/**
 * Data class représentant la réputation d'un joueur
 */
data class ReputationJoueur(
    val uuid: UUID,
    val nom: String,
    val reputationLivreur: Double = 1.0, // Entre 0.0 et 2.0
    val totalLivraisonsEffectuees: Int = 0,
    val totalLivraisonsEchouees: Int = 0,
    val totalCommandesPassees: Int = 0,
    val lastUpdate: LocalDateTime = LocalDateTime.now()
) {
    fun getTauxReussite(): Double {
        val total = totalLivraisonsEffectuees + totalLivraisonsEchouees
        return if (total > 0) {
            totalLivraisonsEffectuees.toDouble() / total.toDouble()
        } else {
            1.0
        }
    }
    
    fun canAcceptDelivery(reputationMinimum: Double): Boolean {
        return reputationLivreur >= reputationMinimum
    }
}

/**
 * Data class pour les statistiques du plugin
 */
data class StatistiquesPlugin(
    val totalCommandes: Long,
    val commandesActives: Long,
    val commandesLivrees: Long,
    val commandesAnnulees: Long,
    val volumeEconomique: Double, // Volume total d'argent échangé
    val joueurActifs: Int
)
