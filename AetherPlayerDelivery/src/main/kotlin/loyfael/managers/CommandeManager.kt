package loyfael.managers

import kotlinx.coroutines.*
import loyfael.AetherPlayerDelivery
import loyfael.entities.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.util.*
import java.util.logging.Level
import kotlin.coroutines.CoroutineContext

/**
 * Gestionnaire des commandes - Logique métier principale
 * Gère la création, validation, annulation et suivi des commandes
 */
class CommandeManager(
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
     * Crée une nouvelle commande après toutes les vérifications
     */
    suspend fun createCommande(
        player: Player,
        material: Material,
        nomItem: String,
        quantite: Int,
        prixTotal: Double,
        delaiJours: Int,
        description: String? = null,
        bonus: Double = 0.0
    ): CommandeResult = withContext(Dispatchers.IO) {
        
        try {
            // Vérifications préliminaires
            val validationResult = validateCommande(player.uniqueId, material, quantite, prixTotal, delaiJours)
            if (validationResult !is CommandeValidation.VALID) {
                return@withContext CommandeResult.VALIDATION_ERROR(validationResult.message)
            }
            
            // Vérifier le solde du joueur
            if (!economieManager.hasEnoughMoney(player.uniqueId, prixTotal + bonus)) {
                return@withContext CommandeResult.INSUFFICIENT_FUNDS(
                    "Solde insuffisant : ${economieManager.formatMoney(economieManager.getBalance(player.uniqueId))} < ${economieManager.formatMoney(prixTotal + bonus)}"
                )
            }
            
            // Retirer l'argent immédiatement
            val withdrawResult = economieManager.withdrawMoney(
                player.uniqueId, 
                prixTotal + bonus, 
                "Création commande $quantite $nomItem"
            )
            
            if (withdrawResult !is EconomyResult.SUCCESS) {
                val errorMessage = when (withdrawResult) {
                    is EconomyResult.ERROR -> withdrawResult.message
                    is EconomyResult.INSUFFICIENT_FUNDS -> withdrawResult.message
                    else -> "Erreur de paiement inconnue"
                }
                return@withContext CommandeResult.PAYMENT_ERROR("Erreur lors du paiement : $errorMessage")
            }
            
            // Créer la commande
            val deadline = LocalDateTime.now().plusDays(delaiJours.toLong())
            val commande = Commande(
                uuidClient = player.uniqueId,
                nomClient = player.name,
                material = material,
                nomItem = nomItem,
                quantite = quantite,
                prixTotal = prixTotal,
                statut = StatutCommande.EN_ATTENTE,
                createdAt = LocalDateTime.now(),
                deadline = deadline,
                description = description,
                bonus = bonus
            )
            
            // Sauvegarder en base de données
            val commandeId = databaseManager.createCommande(commande)
            
            // Ajouter à l'historique
            databaseManager.addHistoriqueEvenement(
                HistoriqueEvenement(
                    typeEvenement = TypeEvenement.COMMANDE_CREEE,
                    uuidJoueur = player.uniqueId,
                    nomJoueur = player.name,
                    idCommande = commandeId,
                    details = "Commande créée : $quantite $nomItem pour ${economieManager.formatMoney(prixTotal)}",
                    montant = prixTotal + bonus
                )
            )
            
            // Notification au client
            notificationManager.notifyPlayer(
                player.uniqueId,
                config.getString("messages.client.order-created", "")!!
                    .replace("%id%", commandeId.toString())
                    .replace("%price%", economieManager.formatMoney(prixTotal + bonus))
                    .replace("%days%", delaiJours.toString()),
                NotificationType.SUCCESS
            )
            
            // Notification aux livreurs potentiels
            notificationManager.broadcastToLivreurs(
                "§7[AetherDelivery] §aNouvelle commande disponible : $quantite $nomItem pour ${economieManager.formatMoney(prixTotal)}",
                player.uniqueId // Exclure le créateur
            )
            
            CommandeResult.SUCCESS(commandeId, "Commande créée avec succès !")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur lors de la création de commande pour ${player.name}", e)
            CommandeResult.ERROR("Erreur technique lors de la création de la commande")
        }
    }
    
    /**
     * Valide tous les paramètres d'une commande
     */
    private suspend fun validateCommande(
        playerUuid: UUID,
        material: Material,
        quantite: Int,
        prixTotal: Double,
        delaiJours: Int
    ): CommandeValidation = withContext(Dispatchers.IO) {
        
        // Vérifier si l'item est autorisé
        val itemsAutorises = config.getStringList("items_autorises")
        if (!itemsAutorises.contains(material.name)) {
            return@withContext CommandeValidation.INVALID("Cet item n'est pas autorisé pour les commandes")
        }
        
        // Vérifier la quantité maximale
        val quantiteMax = config.getInt("livraison.quantite_max", 2304)
        if (quantite > quantiteMax) {
            return@withContext CommandeValidation.INVALID("Quantité maximale autorisée : $quantiteMax items")
        }
        
        if (quantite <= 0) {
            return@withContext CommandeValidation.INVALID("La quantité doit être supérieure à 0")
        }
        
        // Vérifier le prix
        if (prixTotal <= 0.0) {
            return@withContext CommandeValidation.INVALID("Le prix doit être supérieur à 0")
        }
        
        // Vérifier le délai
        val delaiMin = config.getInt("livraison.delai_minimum", 1)
        val delaiMax = config.getInt("livraison.delai_maximum", 7)
        
        if (delaiJours < delaiMin || delaiJours > delaiMax) {
            return@withContext CommandeValidation.INVALID("Le délai doit être entre $delaiMin et $delaiMax jours")
        }
        
        // Vérifier le nombre maximum de commandes par joueur
        val commandesActuelles = databaseManager.getCommandesClient(playerUuid)
            .count { it.statut in listOf(StatutCommande.EN_ATTENTE, StatutCommande.ACCEPTEE, StatutCommande.PRETE) }
        
        val maxCommandes = config.getInt("livraison.max_commandes_par_joueur", 2)
        if (commandesActuelles >= maxCommandes) {
            return@withContext CommandeValidation.INVALID("Vous avez atteint le nombre maximum de commandes simultanées ($maxCommandes)")
        }
        
        CommandeValidation.VALID("Validation réussie")
    }
    
    /**
     * Annule une commande (seulement si EN_ATTENTE)
     */
    suspend fun cancelCommande(player: Player, commandeId: Long): CommandeResult = withContext(Dispatchers.IO) {
        
        try {
            val commande = databaseManager.getCommande(commandeId)
                ?: return@withContext CommandeResult.NOT_FOUND("Commande introuvable")
            
            // Vérifier que c'est bien le client
            if (commande.uuidClient != player.uniqueId) {
                return@withContext CommandeResult.PERMISSION_DENIED("Cette commande ne vous appartient pas")
            }
            
            // Vérifier que la commande peut être annulée
            if (commande.statut != StatutCommande.EN_ATTENTE) {
                return@withContext CommandeResult.INVALID_STATE("Cette commande ne peut plus être annulée (statut: ${commande.statut})")
            }
            
            // Mettre à jour le statut
            databaseManager.updateStatutCommande(commandeId, StatutCommande.ANNULEE_CLIENT)
            
            // Rembourser intégralement
            val refundResult = economieManager.processRefund(
                player.uniqueId,
                commande.prixTotal + commande.bonus,
                commandeId,
                "Annulation volontaire"
            )
            
            if (refundResult is RefundResult.ERROR) {
                plugin.logger.severe("Erreur remboursement commande #$commandeId : ${refundResult.message}")
                // On continue quand même pour la cohérence des données
            }
            
            // Historique
            databaseManager.addHistoriqueEvenement(
                HistoriqueEvenement(
                    typeEvenement = TypeEvenement.COMMANDE_ANNULEE,
                    uuidJoueur = player.uniqueId,
                    nomJoueur = player.name,
                    idCommande = commandeId,
                    details = "Commande annulée par le client",
                    montant = commande.prixTotal + commande.bonus
                )
            )
            
            // Notification
            notificationManager.notifyPlayer(
                player.uniqueId,
                config.getString("messages.client.order-cancelled", "")!!
                    .replace("%id%", commandeId.toString()),
                NotificationType.INFO
            )
            
            CommandeResult.SUCCESS(commandeId, "Commande annulée et remboursement effectué")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur lors de l'annulation de commande #$commandeId", e)
            CommandeResult.ERROR("Erreur technique lors de l'annulation")
        }
    }
    
    /**
     * Récupère les commandes d'un client
     */
    suspend fun getCommandesClient(playerUuid: UUID): List<Commande> = withContext(Dispatchers.IO) {
        return@withContext databaseManager.getCommandesClient(playerUuid)
    }
    
    /**
     * Récupère une commande par son ID
     */
    suspend fun getCommande(commandeId: Long): Commande? = withContext(Dispatchers.IO) {
        return@withContext databaseManager.getCommande(commandeId)
    }
    
    /**
     * Récupère toutes les commandes en attente (pour les livreurs)
     */
    suspend fun getCommandesDisponibles(): List<Commande> = withContext(Dispatchers.IO) {
        return@withContext databaseManager.getCommandesEnAttente()
    }
    
    /**
     * Traite les commandes expirées (appelé périodiquement)
     */
    suspend fun processCommandesExpirees(): Int = withContext(Dispatchers.IO) {
        
        try {
            val commandesExpirees = databaseManager.getCommandesExpirees()
            var processed = 0
            
            for (commande in commandesExpirees) {
                try {
                    // Mettre à jour le statut
                    databaseManager.updateStatutCommande(commande.id, StatutCommande.EXPIREE)
                    
                    // Remboursement intégral
                    economieManager.processRefund(
                        commande.uuidClient,
                        commande.prixTotal + commande.bonus,
                        commande.id,
                        "Expiration automatique"
                    )
                    
                    // Historique
                    databaseManager.addHistoriqueEvenement(
                        HistoriqueEvenement(
                            typeEvenement = TypeEvenement.COMMANDE_EXPIREE,
                            uuidJoueur = commande.uuidClient,
                            nomJoueur = commande.nomClient,
                            idCommande = commande.id,
                            details = "Commande expirée automatiquement - remboursement intégral",
                            montant = commande.prixTotal + commande.bonus
                        )
                    )
                    
                    // Notification au client (s'il est en ligne)
                    notificationManager.notifyPlayer(
                        commande.uuidClient,
                        config.getString("messages.client.order-expired", "")!!
                            .replace("%id%", commande.id.toString())
                            .replace("%amount%", economieManager.formatMoney(commande.prixTotal + commande.bonus)),
                        NotificationType.WARNING
                    )
                    
                    processed++
                    
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Erreur lors du traitement de la commande expirée #${commande.id}", e)
                }
            }
            
            if (processed > 0) {
                plugin.logger.info("§7[CommandeManager] $processed commandes expirées traitées")
            }
            
            processed
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur lors du traitement des commandes expirées", e)
            0
        }
    }
    
    /**
     * Recherche des items par nom (français et anglais)
     */
    fun searchItems(query: String): List<Pair<Material, String>> {
        val itemsAutorises = config.getStringList("items_autorises")
        val results = mutableListOf<Pair<Material, String>>()
        
        val queryLower = query.lowercase()
        
        for (materialName in itemsAutorises) {
            try {
                val material = Material.valueOf(materialName)
                val frenchName = getFrenchItemName(material)
                val englishName = material.name.lowercase().replace("_", " ")
                
                // Recherche dans le nom français et anglais
                if (frenchName.lowercase().contains(queryLower) || 
                    englishName.contains(queryLower) || 
                    materialName.lowercase().contains(queryLower)) {
                    results.add(Pair(material, frenchName))
                }
            } catch (e: IllegalArgumentException) {
                // Material invalide, on ignore
            }
        }
        
        return results.sortedBy { it.second }
    }
    
    /**
     * Obtient le nom français d'un item (simplified mapping)
     */
    private fun getFrenchItemName(material: Material): String {
        return when (material) {
            Material.STONE -> "Pierre"
            Material.COBBLESTONE -> "Pierre Taillée"
            Material.DIRT -> "Terre"
            Material.SAND -> "Sable"
            Material.GRAVEL -> "Gravier"
            Material.COAL -> "Charbon"
            Material.IRON_ORE -> "Minerai de Fer"
            Material.GOLD_ORE -> "Minerai d'Or"
            Material.DIAMOND -> "Diamant"
            Material.EMERALD -> "Émeraude"
            Material.REDSTONE -> "Redstone"
            Material.LAPIS_LAZULI -> "Lapis-Lazuli"
            Material.COPPER_ORE -> "Minerai de Cuivre"
            Material.NETHERITE_SCRAP -> "Débris de Netherite"
            Material.OAK_LOG -> "Bûche de Chêne"
            Material.BIRCH_LOG -> "Bûche de Bouleau"
            Material.SPRUCE_LOG -> "Bûche de Sapin"
            Material.JUNGLE_LOG -> "Bûche de Jungle"
            Material.ACACIA_LOG -> "Bûche d'Acacia"
            Material.DARK_OAK_LOG -> "Bûche de Chêne Noir"
            Material.MANGROVE_LOG -> "Bûche de Mangrove"
            Material.CHERRY_LOG -> "Bûche de Cerisier"
            Material.BAMBOO_BLOCK -> "Bloc de Bambou"
            Material.WHEAT -> "Blé"
            Material.CARROT -> "Carotte"
            Material.POTATO -> "Pomme de Terre"
            Material.BEETROOT -> "Betterave"
            Material.SWEET_BERRIES -> "Baies Sucrées"
            Material.GLOW_BERRIES -> "Baies Lumineuses"
            Material.APPLE -> "Pomme"
            Material.MELON_SLICE -> "Tranche de Pastèque"
            Material.PUMPKIN -> "Citrouille"
            Material.SUGAR_CANE -> "Canne à Sucre"
            Material.KELP -> "Varech"
            Material.DRIED_KELP -> "Varech Séché"
            Material.NETHER_WART -> "Verrues du Nether"
            Material.CHORUS_FRUIT -> "Fruit de Chorus"
            else -> material.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }
    
    /**
     * Ferme le CommandeManager
     */
    fun shutdown() {
        job.cancel()
        plugin.logger.info("§cCommandeManager fermé")
    }
}

/**
 * Résultat de validation d'une commande
 */
sealed class CommandeValidation(val message: String) {
    class VALID(message: String) : CommandeValidation(message)
    class INVALID(message: String) : CommandeValidation(message)
}

/**
 * Résultat d'une opération sur une commande
 */
sealed class CommandeResult {
    data class SUCCESS(val commandeId: Long, val message: String) : CommandeResult()
    data class VALIDATION_ERROR(val message: String) : CommandeResult()
    data class INSUFFICIENT_FUNDS(val message: String) : CommandeResult()
    data class PAYMENT_ERROR(val message: String) : CommandeResult()
    data class NOT_FOUND(val message: String) : CommandeResult()
    data class PERMISSION_DENIED(val message: String) : CommandeResult()
    data class INVALID_STATE(val message: String) : CommandeResult()
    data class ERROR(val message: String) : CommandeResult()
}

/**
 * Type de notification
 */
enum class NotificationType {
    SUCCESS, INFO, WARNING, ERROR
}
