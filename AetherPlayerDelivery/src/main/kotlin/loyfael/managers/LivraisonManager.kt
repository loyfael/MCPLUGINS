package loyfael.managers

import kotlinx.coroutines.*
import loyfael.AetherPlayerDelivery
import loyfael.entities.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.*
import java.util.logging.Level
import kotlin.coroutines.CoroutineContext

/**
 * Gestionnaire des livraisons
 * Gère l'acceptation, préparation, validation des livraisons avec vérification stricte des inventaires
 */
class LivraisonManager(
    private val plugin: AetherPlayerDelivery,
    private val databaseManager: DatabaseManager,
    private val economieManager: EconomieManager,
    private val notificationManager: NotificationManager
) : CoroutineScope {
    
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    
    private val config = plugin.config
    
    /**
     * Un livreur accepte une commande
     */
    suspend fun accepterCommande(player: Player, commandeId: Long): LivraisonResult = withContext(Dispatchers.IO) {
        
        try {
            val commande = databaseManager.getCommande(commandeId)
                ?: return@withContext LivraisonResult.NOT_FOUND("Commande introuvable")
            
            // Vérifications
            if (commande.uuidClient == player.uniqueId) {
                return@withContext LivraisonResult.INVALID_ACTION("Vous ne pouvez pas livrer votre propre commande")
            }
            
            if (commande.statut != StatutCommande.EN_ATTENTE) {
                return@withContext LivraisonResult.INVALID_STATE("Cette commande n'est plus disponible (statut: ${commande.statut})")
            }
            
            if (commande.isExpired()) {
                return@withContext LivraisonResult.EXPIRED("Cette commande a expiré")
            }
            
            // Vérifier la réputation du livreur
            @Suppress("UNUSED_VARIABLE")
            val reputationMin = config.getDouble("reputation.reputation-minimum", 0.0)
            // TODO: Récupérer la réputation du joueur depuis la base
            
            // Calculer le délai de récupération
            val delaiRecuperation = LocalDateTime.now().plusDays(config.getInt("livraison.delai_recuperation", 3).toLong())
            
            // Créer la livraison
            val livraison = Livraison(
                idCommande = commandeId,
                uuidLivreur = player.uniqueId,
                nomLivreur = player.name,
                statut = StatutCommande.ACCEPTEE,
                acceptedAt = LocalDateTime.now(),
                delaiRecuperation = delaiRecuperation
            )
            
            // Sauvegarder en base
            val success = databaseManager.createLivraison(livraison)
            if (!success) {
                return@withContext LivraisonResult.DATABASE_ERROR("Erreur lors de la sauvegarde")
            }
            
            // Historique
            databaseManager.addHistoriqueEvenement(
                HistoriqueEvenement(
                    typeEvenement = TypeEvenement.COMMANDE_ACCEPTEE,
                    uuidJoueur = player.uniqueId,
                    nomJoueur = player.name,
                    idCommande = commandeId,
                    details = "Commande acceptée par le livreur ${player.name}"
                )
            )
            
            // Notifications
            notificationManager.notifyLivraisonAcceptee(player.uniqueId, commande.nomClient, commandeId)
            notificationManager.notifyCommandeAcceptee(commande.uuidClient, player.name, commandeId)
            
            LivraisonResult.SUCCESS("Commande acceptée ! Vous avez ${commande.getDaysRemaining()} jours pour la livrer.")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur lors de l'acceptation de commande #$commandeId par ${player.name}", e)
            LivraisonResult.ERROR("Erreur technique lors de l'acceptation")
        }
    }
    
    /**
     * Un livreur dépose les items requis (simulation du dépôt)
     */
    suspend fun deposerItems(player: Player, commandeId: Long): LivraisonResult = withContext(Dispatchers.IO) {
        
        try {
            val commande = databaseManager.getCommande(commandeId)
                ?: return@withContext LivraisonResult.NOT_FOUND("Commande introuvable")
            
            // Vérifier que c'est le bon livreur
            val livraisons = databaseManager.getLivraisonsLivreur(player.uniqueId)
            val livraison = livraisons.find { it.first.id == commandeId }?.second
                ?: return@withContext LivraisonResult.PERMISSION_DENIED("Cette livraison ne vous appartient pas")
            
            if (commande.statut != StatutCommande.ACCEPTEE) {
                return@withContext LivraisonResult.INVALID_STATE("Cette commande ne peut pas être livrée (statut: ${commande.statut})")
            }
            
            // Vérifier si la deadline de livraison n'est pas dépassée
            if (commande.isExpired()) {
                // Annuler automatiquement et rembourser
                return@withContext annulerLivraisonExpiree(commande, livraison)
            }
            
            // Vérifier que le joueur a les items requis dans son inventaire
            val inventoryCheck = verifyInventoryItems(player, commande)
            if (!inventoryCheck.hasEnoughItems) {
                return@withContext LivraisonResult.INSUFFICIENT_ITEMS(inventoryCheck.missingItemsMessage)
            }
            
            // Retirer les items de l'inventaire du livreur
            val itemsRemoved = removeItemsFromInventory(player, commande)
            if (!itemsRemoved) {
                return@withContext LivraisonResult.ERROR("Erreur lors du retrait des items de votre inventaire")
            }
            
            // Mettre à jour le statut de la commande
            databaseManager.updateStatutCommande(commandeId, StatutCommande.PRETE)
            
            // Historique
            databaseManager.addHistoriqueEvenement(
                HistoriqueEvenement(
                    typeEvenement = TypeEvenement.COMMANDE_LIVREE,
                    uuidJoueur = player.uniqueId,
                    nomJoueur = player.name,
                    idCommande = commandeId,
                    details = "Items déposés, commande prête à être récupérée"
                )
            )
            
            // Payer le livreur
            val paymentResult = economieManager.processPayment(
                commande.uuidClient,
                player.uniqueId,
                commande.prixTotal + commande.bonus,
                commandeId
            )
            
            if (paymentResult is PaymentResult.SUCCESS) {
                notificationManager.notifyPaiementRecu(
                    player.uniqueId,
                    economieManager.formatMoney(commande.prixTotal + commande.bonus),
                    commandeId
                )
                
                databaseManager.addHistoriqueEvenement(
                    HistoriqueEvenement(
                        typeEvenement = TypeEvenement.PAIEMENT_EFFECTUE,
                        uuidJoueur = player.uniqueId,
                        nomJoueur = player.name,
                        idCommande = commandeId,
                        details = "Paiement reçu pour la livraison",
                        montant = commande.prixTotal + commande.bonus
                    )
                )
            } else {
                plugin.logger.severe("Erreur paiement livreur commande #$commandeId: ${(paymentResult as PaymentResult.ERROR).message}")
            }
            
            // Notifier le client que sa commande est prête
            notificationManager.notifyCommandePrete(commande.uuidClient, commandeId)
            
            LivraisonResult.SUCCESS("Livraison effectuée ! Vous avez été payé ${economieManager.formatMoney(commande.prixTotal + commande.bonus)}")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur lors du dépôt d'items pour commande #$commandeId", e)
            LivraisonResult.ERROR("Erreur technique lors du dépôt")
        }
    }
    
    /**
     * Un client récupère sa commande livrée
     */
    suspend fun recupererCommande(player: Player, commandeId: Long): LivraisonResult = withContext(Dispatchers.IO) {
        
        try {
            val commande = databaseManager.getCommande(commandeId)
                ?: return@withContext LivraisonResult.NOT_FOUND("Commande introuvable")
            
            // Vérifier que c'est bien le client
            if (commande.uuidClient != player.uniqueId) {
                return@withContext LivraisonResult.PERMISSION_DENIED("Cette commande ne vous appartient pas")
            }
            
            if (commande.statut != StatutCommande.PRETE) {
                return@withContext LivraisonResult.INVALID_STATE("Cette commande n'est pas prête à être récupérée (statut: ${commande.statut})")
            }
            
            // Vérifier l'espace dans l'inventaire du client
            if (!hasInventorySpace(player, commande)) {
                return@withContext LivraisonResult.INSUFFICIENT_SPACE("Vous n'avez pas assez d'espace dans votre inventaire")
            }
            
            // Ajouter les items à l'inventaire du client
            val itemsAdded = addItemsToInventory(player, commande)
            if (!itemsAdded) {
                return@withContext LivraisonResult.ERROR("Erreur lors de l'ajout des items à votre inventaire")
            }
            
            // Mettre à jour le statut
            databaseManager.updateStatutCommande(commandeId, StatutCommande.LIVREE)
            
            // Historique
            databaseManager.addHistoriqueEvenement(
                HistoriqueEvenement(
                    typeEvenement = TypeEvenement.COMMANDE_RECUPEREE,
                    uuidJoueur = player.uniqueId,
                    nomJoueur = player.name,
                    idCommande = commandeId,
                    details = "Commande récupérée par le client"
                )
            )
            
            // Notification
            notificationManager.notifyCommandeLivree(player.uniqueId, commandeId)
            
            LivraisonResult.SUCCESS("Commande récupérée ! ${commande.quantite} ${commande.nomItem} ajoutés à votre inventaire.")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur lors de la récupération de commande #$commandeId", e)
            LivraisonResult.ERROR("Erreur technique lors de la récupération")
        }
    }
    
    /**
     * Récupère les livraisons d'un livreur
     */
    suspend fun getLivraisonsLivreur(playerUuid: UUID): List<Pair<Commande, Livraison>> = withContext(Dispatchers.IO) {
        return@withContext databaseManager.getLivraisonsLivreur(playerUuid)
    }
    
    /**
     * Traite les livraisons expirées (non récupérées)
     */
    suspend fun processLivraisonsExpirees(): Int = withContext(Dispatchers.IO) {
        
        try {
            val livraisonsExpirees = databaseManager.getLivraisonsExpirees()
            var processed = 0
            
            for ((commande, _) in livraisonsExpirees) {
                try {
                    // Calculer le remboursement partiel
                    val pourcentageRemboursement = config.getInt("livraison.remboursement_expire", 50)
                    
                    // Mettre à jour le statut
                    databaseManager.updateStatutCommande(commande.id, StatutCommande.EXPIREE)
                    
                    // Remboursement partiel
                    val refundResult = economieManager.processPartialRefund(
                        commande.uuidClient,
                        commande.prixTotal + commande.bonus,
                        pourcentageRemboursement,
                        commande.id
                    )
                    
                    // Historique
                    val refundAmount = (commande.prixTotal + commande.bonus) * pourcentageRemboursement / 100.0
                    databaseManager.addHistoriqueEvenement(
                        HistoriqueEvenement(
                            typeEvenement = TypeEvenement.COMMANDE_EXPIREE,
                            uuidJoueur = commande.uuidClient,
                            nomJoueur = commande.nomClient,
                            idCommande = commande.id,
                            details = "Commande expirée (non récupérée) - remboursement partiel $pourcentageRemboursement%",
                            montant = refundAmount
                        )
                    )
                    
                    // Notifications
                    if (refundResult is RefundResult.SUCCESS) {
                        notificationManager.notifyRemboursement(
                            commande.uuidClient,
                            economieManager.formatMoney(refundAmount),
                            commande.id
                        )
                    }
                    
                    processed++
                    
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Erreur lors du traitement de la livraison expirée #${commande.id}", e)
                }
            }
            
            if (processed > 0) {
                plugin.logger.info("§7[LivraisonManager] $processed livraisons expirées traitées")
            }
            
            processed
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur lors du traitement des livraisons expirées", e)
            0
        }
    }
    
    // === MÉTHODES UTILITAIRES ===
    
    /**
     * Vérifie si un joueur a les items requis dans son inventaire
     */
    private fun verifyInventoryItems(player: Player, commande: Commande): InventoryCheck {
        val inventory = player.inventory
        var totalItems = 0
        
        // Compter tous les items du type requis
        for (item in inventory.contents) {
            if (item != null && item.type == commande.material) {
                totalItems += item.amount
            }
        }
        
        return if (totalItems >= commande.quantite) {
            InventoryCheck(true, "")
        } else {
            val missing = commande.quantite - totalItems
            InventoryCheck(false, "Il vous manque $missing ${commande.nomItem} dans votre inventaire (vous avez $totalItems/${commande.quantite})")
        }
    }
    
    /**
     * Retire les items requis de l'inventaire du joueur
     */
    private fun removeItemsFromInventory(player: Player, commande: Commande): Boolean {
        val inventory = player.inventory
        var toRemove = commande.quantite
        
        for (slot in inventory.contents.indices) {
            val item = inventory.getItem(slot)
            if (item != null && item.type == commande.material && toRemove > 0) {
                val itemCount = item.amount
                if (itemCount <= toRemove) {
                    // Retirer tout l'item
                    inventory.setItem(slot, null)
                    toRemove -= itemCount
                } else {
                    // Retirer partiellement
                    item.amount = itemCount - toRemove
                    toRemove = 0
                }
            }
        }
        
        return toRemove == 0
    }
    
    /**
     * Vérifie si le joueur a assez d'espace dans son inventaire
     */
    private fun hasInventorySpace(player: Player, commande: Commande): Boolean {
        val inventory = player.inventory
        val maxStackSize = commande.material.maxStackSize
        var remainingItems = commande.quantite
        
        // Vérifier d'abord les slots avec le même item
        for (item in inventory.contents) {
            if (item != null && item.type == commande.material) {
                val spaceInStack = maxStackSize - item.amount
                remainingItems -= spaceInStack.coerceAtLeast(0)
                if (remainingItems <= 0) return true
            }
        }
        
        // Vérifier les slots vides
        val emptySlots = inventory.contents.count { it == null }
        val fullStacksNeeded = (remainingItems + maxStackSize - 1) / maxStackSize
        
        return emptySlots >= fullStacksNeeded
    }
    
    /**
     * Ajoute les items à l'inventaire du client
     */
    private fun addItemsToInventory(player: Player, commande: Commande): Boolean {
        val itemStack = ItemStack(commande.material, commande.quantite)
        val leftOver = player.inventory.addItem(itemStack)
        return leftOver.isEmpty()
    }
    
    /**
     * Annule une livraison expirée
     */
    private suspend fun annulerLivraisonExpiree(commande: Commande, livraison: Livraison): LivraisonResult {
        // Mettre à jour le statut
        databaseManager.updateStatutCommande(commande.id, StatutCommande.EXPIREE)
        
        // Remboursement intégral au client
        economieManager.processRefund(
            commande.uuidClient,
            commande.prixTotal + commande.bonus,
            commande.id,
            "Livraison expirée"
        )
        
        // Notifications
        notificationManager.notifyPlayer(
            commande.uuidClient,
            "Votre commande #${commande.id} a expiré et vous avez été remboursé intégralement.",
            NotificationType.WARNING
        )
        
        notificationManager.notifyPlayer(
            livraison.uuidLivreur,
            "Votre livraison #${commande.id} a expiré. Vous ne serez pas payé.",
            NotificationType.ERROR
        )
        
        return LivraisonResult.EXPIRED("Cette livraison a dépassé le délai autorisé et a été annulée")
    }
    
    /**
     * Ferme le LivraisonManager
     */
    fun shutdown() {
        job.cancel()
        plugin.logger.info("§cLivraisonManager fermé")
    }
}

/**
 * Résultat d'une opération de livraison
 */
sealed class LivraisonResult {
    data class SUCCESS(val message: String) : LivraisonResult()
    data class NOT_FOUND(val message: String) : LivraisonResult()
    data class INVALID_ACTION(val message: String) : LivraisonResult()
    data class INVALID_STATE(val message: String) : LivraisonResult()
    data class PERMISSION_DENIED(val message: String) : LivraisonResult()
    data class INSUFFICIENT_ITEMS(val message: String) : LivraisonResult()
    data class INSUFFICIENT_SPACE(val message: String) : LivraisonResult()
    data class EXPIRED(val message: String) : LivraisonResult()
    data class DATABASE_ERROR(val message: String) : LivraisonResult()
    data class ERROR(val message: String) : LivraisonResult()
}

/**
 * Résultat de vérification d'inventaire
 */
private data class InventoryCheck(
    val hasEnoughItems: Boolean,
    val missingItemsMessage: String
)
