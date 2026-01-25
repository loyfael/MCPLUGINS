package loyfael.managers

import kotlinx.coroutines.*
import loyfael.AetherPlayerDelivery
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.RegisteredServiceProvider
import java.util.*
import java.util.logging.Level
import kotlin.coroutines.CoroutineContext

/**
 * Gestionnaire de l'économie via Vault
 * Gère tous les paiements, remboursements et vérifications de solde
 */
class EconomieManager(private val plugin: AetherPlayerDelivery) : CoroutineScope {
    
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    
    private var economy: Economy? = null
    
    /**
     * Initialise l'intégration avec Vault
     */
    fun initialize(): Boolean {
        if (!setupEconomy()) {
            plugin.logger.severe("§cVault Economy n'a pas pu être initialisé ! Le plugin ne peut pas fonctionner.")
            return false
        }
        
        plugin.logger.info("§aEconomieManager initialisé avec succès !")
        plugin.logger.info("§7Système économique : ${economy?.name}")
        return true
    }
    
    /**
     * Configure l'économie avec Vault
     */
    private fun setupEconomy(): Boolean {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            plugin.logger.warning("§eVault n'est pas installé !")
            return false
        }
        
        val rsp: RegisteredServiceProvider<Economy>? = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            plugin.logger.warning("§eAucun plugin d'économie compatible avec Vault trouvé !")
            return false
        }
        
        economy = rsp.provider
        return economy != null
    }
    
    /**
     * Vérifie si un joueur a suffisamment d'argent
     */
    suspend fun hasEnoughMoney(playerUuid: UUID, amount: Double): Boolean = withContext(Dispatchers.IO) {
        val offlinePlayer = Bukkit.getOfflinePlayer(playerUuid)
        economy?.has(offlinePlayer, amount) ?: false
    }
    
    /**
     * Récupère le solde d'un joueur
     */
    suspend fun getBalance(playerUuid: UUID): Double = withContext(Dispatchers.IO) {
        val offlinePlayer = Bukkit.getOfflinePlayer(playerUuid)
        economy?.getBalance(offlinePlayer) ?: 0.0
    }
    
    /**
     * Retire de l'argent du compte d'un joueur (pour créer une commande)
     */
    suspend fun withdrawMoney(playerUuid: UUID, amount: Double, reason: String): EconomyResult = withContext(Dispatchers.IO) {
        try {
            val offlinePlayer = Bukkit.getOfflinePlayer(playerUuid)
            val econ = economy ?: return@withContext EconomyResult.ERROR("Système économique non disponible")
            
            if (!econ.has(offlinePlayer, amount)) {
                return@withContext EconomyResult.INSUFFICIENT_FUNDS("Solde insuffisant : ${econ.getBalance(offlinePlayer)}$ < $amount$")
            }
            
            val response = econ.withdrawPlayer(offlinePlayer, amount)
            if (response.transactionSuccess()) {
                plugin.logger.info("§7[Économie] Retrait de $amount$ pour ${offlinePlayer.name} - Raison: $reason")
                EconomyResult.SUCCESS("$amount$ retiré avec succès", response.balance)
            } else {
                EconomyResult.ERROR("Échec du retrait : ${response.errorMessage}")
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Erreur lors du retrait d'argent pour $playerUuid", e)
            EconomyResult.ERROR("Erreur technique lors du retrait")
        }
    }
    
    /**
     * Ajoute de l'argent au compte d'un joueur (remboursement ou paiement)
     */
    suspend fun depositMoney(playerUuid: UUID, amount: Double, reason: String): EconomyResult = withContext(Dispatchers.IO) {
        try {
            val offlinePlayer = Bukkit.getOfflinePlayer(playerUuid)
            val econ = economy ?: return@withContext EconomyResult.ERROR("Système économique non disponible")
            
            val response = econ.depositPlayer(offlinePlayer, amount)
            if (response.transactionSuccess()) {
                plugin.logger.info("§7[Économie] Dépôt de $amount$ pour ${offlinePlayer.name} - Raison: $reason")
                EconomyResult.SUCCESS("$amount$ ajouté avec succès", response.balance)
            } else {
                EconomyResult.ERROR("Échec du dépôt : ${response.errorMessage}")
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Erreur lors du dépôt d'argent pour $playerUuid", e)
            EconomyResult.ERROR("Erreur technique lors du dépôt")
        }
    }
    
    /**
     * Effectue un paiement de client vers livreur
     */
    suspend fun processPayment(@Suppress("UNUSED_PARAMETER") clientUuid: UUID, livreurUuid: UUID, amount: Double, commandeId: Long): PaymentResult = withContext(Dispatchers.IO) {
        try {
            // Pas besoin de retirer au client car c'est déjà fait à la création de la commande
            // On paie directement le livreur
            val depositResult = depositMoney(livreurUuid, amount, "Paiement livraison commande #$commandeId")
            
            when (depositResult) {
                is EconomyResult.SUCCESS -> {
                    PaymentResult.SUCCESS("Paiement de $amount$ effectué avec succès", amount)
                }
                is EconomyResult.ERROR -> {
                    PaymentResult.ERROR("Erreur lors du paiement au livreur : ${depositResult.message}")
                }
                is EconomyResult.INSUFFICIENT_FUNDS -> {
                    // Ne devrait jamais arriver pour un dépôt
                    PaymentResult.ERROR("Erreur inattendue lors du paiement")
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur critique lors du paiement commande #$commandeId", e)
            PaymentResult.ERROR("Erreur technique lors du paiement")
        }
    }
    
    /**
     * Effectue un remboursement total au client
     */
    suspend fun processRefund(clientUuid: UUID, amount: Double, commandeId: Long, reason: String): RefundResult = withContext(Dispatchers.IO) {
        try {
            val depositResult = depositMoney(clientUuid, amount, "Remboursement commande #$commandeId - $reason")
            
            when (depositResult) {
                is EconomyResult.SUCCESS -> {
                    RefundResult.SUCCESS("Remboursement de $amount$ effectué avec succès", amount)
                }
                is EconomyResult.ERROR -> {
                    RefundResult.ERROR("Erreur lors du remboursement : ${depositResult.message}")
                }
                is EconomyResult.INSUFFICIENT_FUNDS -> {
                    // Ne devrait jamais arriver pour un dépôt
                    RefundResult.ERROR("Erreur inattendue lors du remboursement")
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur critique lors du remboursement commande #$commandeId", e)
            RefundResult.ERROR("Erreur technique lors du remboursement")
        }
    }
    
    /**
     * Effectue un remboursement partiel (commande non récupérée)
     */
    suspend fun processPartialRefund(clientUuid: UUID, originalAmount: Double, refundPercentage: Int, commandeId: Long): RefundResult = withContext(Dispatchers.IO) {
        val refundAmount = (originalAmount * refundPercentage) / 100.0
        return@withContext processRefund(clientUuid, refundAmount, commandeId, "Remboursement partiel ($refundPercentage%)")
    }
    
    /**
     * Formate un montant d'argent pour l'affichage
     */
    fun formatMoney(amount: Double): String {
        return economy?.format(amount) ?: "${amount}$"
    }
    
    /**
     * Récupère le nom de la devise
     */
    fun getCurrencyName(): String {
        return economy?.currencyNamePlural() ?: "dollars"
    }
    
    /**
     * Vérifie si l'économie est disponible
     */
    fun isEconomyAvailable(): Boolean {
        return economy != null
    }
    
    /**
     * Ferme le gestionnaire d'économie
     */
    fun shutdown() {
        job.cancel()
        plugin.logger.info("§cEconomieManager fermé")
    }
}

/**
 * Résultat d'une opération économique simple
 */
sealed class EconomyResult {
    data class SUCCESS(val message: String, val newBalance: Double) : EconomyResult()
    data class ERROR(val message: String) : EconomyResult()
    data class INSUFFICIENT_FUNDS(val message: String) : EconomyResult()
}

/**
 * Résultat d'un paiement
 */
sealed class PaymentResult {
    data class SUCCESS(val message: String, val amount: Double) : PaymentResult()
    data class ERROR(val message: String) : PaymentResult()
}

/**
 * Résultat d'un remboursement
 */
sealed class RefundResult {
    data class SUCCESS(val message: String, val amount: Double) : RefundResult()
    data class ERROR(val message: String) : RefundResult()
}
