package loyfael.managers

import kotlinx.coroutines.*
import loyfael.AetherPlayerDelivery
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Gestionnaire des notifications multi-canaux
 * Gère l'envoi de messages via chat, action bar, titres et sons
 */
class NotificationManager(private val plugin: AetherPlayerDelivery) : CoroutineScope {
    
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    
    private val config = plugin.config
    
    /**
     * Notifie un joueur spécifique
     */
    fun notifyPlayer(
        playerUuid: UUID,
        message: String,
        type: NotificationType,
        channels: Set<NotificationChannel> = getEnabledChannels()
    ) {
        launch {
            val player = Bukkit.getPlayer(playerUuid)
            if (player == null || !player.isOnline) return@launch
            
            val coloredMessage = colorizeMessage(message, type)
            
            // Chat message
            if (channels.contains(NotificationChannel.CHAT)) {
                player.sendMessage(coloredMessage)
            }
            
            // Action bar
            if (channels.contains(NotificationChannel.ACTION_BAR)) {
                player.sendActionBar(Component.text(coloredMessage.removeColorCodes()))
            }
            
            // Title
            if (channels.contains(NotificationChannel.TITLE)) {
                val title = Title.title(
                    Component.text("AetherDelivery").color(getTypeColor(type)),
                    Component.text(message.removeColorCodes()),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                )
                player.showTitle(title)
            }
            
            // Sound
            if (channels.contains(NotificationChannel.SOUND)) {
                val sound = getSoundForType(type)
                player.playSound(player.location, sound, 1.0f, 1.0f)
            }
        }
    }
    
    /**
     * Notifie tous les joueurs en ligne avec une permission spécifique
     */
    fun broadcastToPermission(
        message: String,
        permission: String,
        type: NotificationType = NotificationType.INFO,
        excludePlayer: UUID? = null
    ) {
        launch {
            Bukkit.getOnlinePlayers()
                .filter { it.hasPermission(permission) }
                .filter { excludePlayer == null || it.uniqueId != excludePlayer }
                .forEach { player ->
                    notifyPlayer(player.uniqueId, message, type)
                }
        }
    }
    
    /**
     * Broadcast aux livreurs potentiels
     */
    fun broadcastToLivreurs(
        message: String,
        excludePlayer: UUID? = null
    ) {
        broadcastToPermission(message, "aetherdelivery.livreur", NotificationType.INFO, excludePlayer)
    }
    
    /**
     * Notifie un joueur d'une nouvelle commande acceptée
     */
    fun notifyCommandeAcceptee(clientUuid: UUID, livreurName: String, commandeId: Long) {
        val message = config.getString("messages.client.order-accepted", "")!!
            .replace("%id%", commandeId.toString())
            .replace("%player%", livreurName)
        
        notifyPlayer(clientUuid, message, NotificationType.SUCCESS)
    }
    
    /**
     * Notifie un joueur que sa commande est prête
     */
    fun notifyCommandePrete(clientUuid: UUID, commandeId: Long) {
        val message = config.getString("messages.client.order-ready", "")!!
            .replace("%id%", commandeId.toString())
        
        notifyPlayer(
            clientUuid, 
            message, 
            NotificationType.SUCCESS,
            setOf(NotificationChannel.CHAT, NotificationChannel.ACTION_BAR, NotificationChannel.SOUND)
        )
    }
    
    /**
     * Notifie un joueur que sa commande a été livrée
     */
    fun notifyCommandeLivree(clientUuid: UUID, commandeId: Long) {
        val message = config.getString("messages.client.order-delivered", "")!!
            .replace("%id%", commandeId.toString())
        
        notifyPlayer(clientUuid, message, NotificationType.SUCCESS)
    }
    
    /**
     * Notifie un livreur qu'il a accepté une commande
     */
    fun notifyLivraisonAcceptee(livreurUuid: UUID, clientName: String, commandeId: Long) {
        val message = config.getString("messages.livreur.order-accepted", "")!!
            .replace("%id%", commandeId.toString())
            .replace("%player%", clientName)
        
        notifyPlayer(livreurUuid, message, NotificationType.SUCCESS)
    }
    
    /**
     * Notifie un livreur qu'il a été payé
     */
    fun notifyPaiementRecu(livreurUuid: UUID, amount: String, commandeId: Long) {
        val message = config.getString("messages.livreur.payment-received", "")!!
            .replace("%amount%", amount)
            .replace("%id%", commandeId.toString())
        
        notifyPlayer(livreurUuid, message, NotificationType.SUCCESS)
    }
    
    /**
     * Notifie un remboursement
     */
    fun notifyRemboursement(clientUuid: UUID, amount: String, commandeId: Long) {
        val message = config.getString("messages.client.refund-received", "")!!
            .replace("%amount%", amount)
            .replace("%id%", commandeId.toString())
        
        notifyPlayer(clientUuid, message, NotificationType.INFO)
    }
    
    /**
     * Notifie une erreur
     */
    fun notifyError(playerUuid: UUID, errorKey: String, vararg replacements: Pair<String, String>) {
        var message = config.getString("messages.error.$errorKey", "§cErreur inconnue")!!
        
        for ((placeholder, value) in replacements) {
            message = message.replace("%$placeholder%", value)
        }
        
        notifyPlayer(playerUuid, message, NotificationType.ERROR)
    }
    
    /**
     * Rappel périodique pour les commandes/livraisons en attente
     */
    fun sendPeriodicReminders() {
        launch {
            // Cette méthode sera appelée périodiquement par le SchedulerManager
            // Pour notifier des rappels aux joueurs
            
            // Exemple : rappeler aux clients que leurs commandes sont prêtes
            // Exemple : rappeler aux livreurs leurs livraisons en cours
        }
    }
    
    // === MÉTHODES UTILITAIRES ===
    
    /**
     * Obtient les canaux de notification activés depuis la config
     */
    private fun getEnabledChannels(): Set<NotificationChannel> {
        val channels = mutableSetOf<NotificationChannel>()
        
        if (config.getBoolean("notifications.channels.chat", true)) {
            channels.add(NotificationChannel.CHAT)
        }
        if (config.getBoolean("notifications.channels.action-bar", true)) {
            channels.add(NotificationChannel.ACTION_BAR)
        }
        if (config.getBoolean("notifications.channels.title", false)) {
            channels.add(NotificationChannel.TITLE)
        }
        if (config.getBoolean("notifications.channels.sound", true)) {
            channels.add(NotificationChannel.SOUND)
        }
        
        return channels
    }
    
    /**
     * Colorise un message selon son type
     */
    private fun colorizeMessage(message: String, type: NotificationType): String {
        val prefix = config.getString("general.prefix", "&8[&6AetherDelivery&8] &r")!!
        val typeColor = when (type) {
            NotificationType.SUCCESS -> "&a"
            NotificationType.INFO -> "&7"
            NotificationType.WARNING -> "&e"
            NotificationType.ERROR -> "&c"
        }
        
        return "$prefix$typeColor$message".colorize()
    }
    
    /**
     * Obtient la couleur Adventure pour un type de notification
     */
    private fun getTypeColor(type: NotificationType): NamedTextColor {
        return when (type) {
            NotificationType.SUCCESS -> NamedTextColor.GREEN
            NotificationType.INFO -> NamedTextColor.GRAY
            NotificationType.WARNING -> NamedTextColor.YELLOW
            NotificationType.ERROR -> NamedTextColor.RED
        }
    }
    
    /**
     * Obtient le son approprié pour un type de notification
     */
    private fun getSoundForType(type: NotificationType): Sound {
        val configSound = when (type) {
            NotificationType.SUCCESS -> config.getString("notifications.sounds.order-delivered", "ENTITY_VILLAGER_YES")
            NotificationType.INFO -> config.getString("notifications.sounds.order-created", "ENTITY_EXPERIENCE_ORB_PICKUP")
            NotificationType.WARNING -> config.getString("notifications.sounds.order-cancelled", "ENTITY_VILLAGER_NO")
            NotificationType.ERROR -> config.getString("notifications.sounds.error", "ENTITY_ENDERMAN_TELEPORT")
        }
        
        return try {
            @Suppress("DEPRECATION")
            Sound.valueOf(configSound!!)
        } catch (e: IllegalArgumentException) {
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP
        }
    }
    
    /**
     * Ferme le NotificationManager
     */
    fun shutdown() {
        job.cancel()
        plugin.logger.info("§cNotificationManager fermé")
    }
}

/**
 * Canaux de notification disponibles
 */
enum class NotificationChannel {
    CHAT, ACTION_BAR, TITLE, SOUND
}

/**
 * Extension pour coloriser les messages Bukkit
 */
private fun String.colorize(): String {
    return this.replace("&", "§")
}

/**
 * Extension pour retirer les codes couleur
 */
private fun String.removeColorCodes(): String {
    return this.replace("&[0-9a-fk-or]".toRegex(), "").replace("§[0-9a-fk-or]".toRegex(), "")
}
