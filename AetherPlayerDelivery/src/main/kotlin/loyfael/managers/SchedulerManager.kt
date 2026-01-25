package loyfael.managers

import kotlinx.coroutines.*
import loyfael.AetherPlayerDelivery
import org.bukkit.Bukkit
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import kotlin.coroutines.CoroutineContext

/**
 * Gestionnaire des tâches planifiées et périodiques
 * Gère les délais, expirations automatiques avec coroutines Kotlin
 */
class SchedulerManager(
    private val plugin: AetherPlayerDelivery,
    private val commandeManager: CommandeManager,
    private val livraisonManager: LivraisonManager,
    private val notificationManager: NotificationManager
) : CoroutineScope {
    
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    
    private val config = plugin.config
    private var isRunning = false
    
    // Jobs pour les différentes tâches
    private var expirationCheckJob: Job? = null
    private var reminderJob: Job? = null
    private var cleanupJob: Job? = null
    
    /**
     * Démarre toutes les tâches périodiques
     */
    fun startScheduledTasks() {
        if (isRunning) return
        
        isRunning = true
        plugin.logger.info("§aSchedulerManager - Démarrage des tâches périodiques...")
        
        // Tâche de vérification des expirations (toutes les 5 minutes)
        startExpirationCheck()
        
        // Tâche de rappels (toutes les heures)
        startReminderTask()
        
        // Tâche de nettoyage (tous les jours à 3h du matin)
        startCleanupTask()
        
        plugin.logger.info("§aToutes les tâches périodiques sont démarrées !")
    }
    
    /**
     * Démarre la vérification des expirations
     */
    private fun startExpirationCheck() {
        expirationCheckJob = launch {
            while (isActive) {
                try {
                    if (config.getString("mode") == "principal") {
                        // Traiter les commandes expirées
                        val commandesExpireesTraitees = commandeManager.processCommandesExpirees()
                        
                        // Traiter les livraisons expirées
                        val livraisonsExpireesTraitees = livraisonManager.processLivraisonsExpirees()
                        
                        if (config.getBoolean("general.debug", false) && (commandesExpireesTraitees > 0 || livraisonsExpireesTraitees > 0)) {
                            plugin.logger.info("§7[SchedulerManager] Expirations traitées - Commandes: $commandesExpireesTraitees, Livraisons: $livraisonsExpireesTraitees")
                        }
                    }
                    
                    // Attendre 5 minutes avant la prochaine vérification
                    delay(TimeUnit.MINUTES.toMillis(5))
                    
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Erreur dans la tâche de vérification des expirations", e)
                    delay(TimeUnit.MINUTES.toMillis(1)) // Attendre 1 minute avant de réessayer
                }
            }
        }
    }
    
    /**
     * Démarre la tâche de rappels
     */
    private fun startReminderTask() {
        reminderJob = launch {
            while (isActive) {
                try {
                    // Envoyer des rappels périodiques
                    notificationManager.sendPeriodicReminders()
                    
                    // Attendre 1 heure avant le prochain cycle
                    delay(TimeUnit.HOURS.toMillis(1))
                    
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Erreur dans la tâche de rappels", e)
                    delay(TimeUnit.MINUTES.toMillis(5)) // Attendre 5 minutes avant de réessayer
                }
            }
        }
    }
    
    /**
     * Démarre la tâche de nettoyage quotidien
     */
    private fun startCleanupTask() {
        cleanupJob = launch {
            while (isActive) {
                try {
                    // Calculer le délai jusqu'à 3h du matin
                    val delayUntil3AM = calculateDelayUntil3AM()
                    delay(delayUntil3AM)
                    
                    if (config.getString("mode") == "principal") {
                        performDailyCleanup()
                    }
                    
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Erreur dans la tâche de nettoyage", e)
                    delay(TimeUnit.HOURS.toMillis(1)) // Attendre 1 heure avant de réessayer
                }
            }
        }
    }
    
    /**
     * Effectue le nettoyage quotidien de la base de données
     */
    private suspend fun performDailyCleanup() = withContext(Dispatchers.IO) {
        try {
            plugin.logger.info("§7[SchedulerManager] Début du nettoyage quotidien...")
            
            // Ici on pourrait ajouter :
            // - Nettoyage de l'historique ancien (> 30 jours)
            // - Optimisation des tables MySQL
            // - Archivage des commandes anciennes
            // - Calcul des statistiques quotidiennes
            
            plugin.logger.info("§7[SchedulerManager] Nettoyage quotidien terminé")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erreur lors du nettoyage quotidien", e)
        }
    }
    
    /**
     * Calcule le délai en millisecondes jusqu'à 3h du matin
     */
    private fun calculateDelayUntil3AM(): Long {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        
        // Définir l'heure à 3h00
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 3)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        var targetTime = calendar.timeInMillis
        
        // Si on a dépassé 3h aujourd'hui, programmer pour demain
        if (targetTime <= now) {
            targetTime += TimeUnit.DAYS.toMillis(1)
        }
        
        return targetTime - now
    }
    
    /**
     * Programme une tâche unique avec délai
     */
    fun scheduleTask(delayMillis: Long, task: suspend () -> Unit): Job {
        return launch {
            delay(delayMillis)
            try {
                task()
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Erreur dans une tâche programmée", e)
            }
        }
    }
    
    /**
     * Programme une tâche répétitive
     */
    fun scheduleRepeatingTask(initialDelayMillis: Long, intervalMillis: Long, task: suspend () -> Unit): Job {
        return launch {
            delay(initialDelayMillis)
            while (isActive) {
                try {
                    task()
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Erreur dans une tâche répétitive", e)
                }
                delay(intervalMillis)
            }
        }
    }
    
    /**
     * Exécute une tâche de manière asynchrone
     */
    fun runAsync(task: suspend () -> Unit): Job {
        return launch(Dispatchers.IO) {
            try {
                task()
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Erreur dans une tâche asynchrone", e)
            }
        }
    }
    
    /**
     * Exécute une tâche sur le thread principal (sync)
     */
    fun runSync(task: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            task()
        } else {
            Bukkit.getScheduler().runTask(plugin, Runnable { task() })
        }
    }
    
    /**
     * Vérifie si une tâche périodique spécifique est en cours d'exécution
     */
    fun isTaskRunning(taskType: TaskType): Boolean {
        return when (taskType) {
            TaskType.EXPIRATION_CHECK -> expirationCheckJob?.isActive == true
            TaskType.REMINDERS -> reminderJob?.isActive == true
            TaskType.CLEANUP -> cleanupJob?.isActive == true
        }
    }
    
    /**
     * Redémarre une tâche spécifique
     */
    fun restartTask(taskType: TaskType) {
        when (taskType) {
            TaskType.EXPIRATION_CHECK -> {
                expirationCheckJob?.cancel()
                startExpirationCheck()
            }
            TaskType.REMINDERS -> {
                reminderJob?.cancel()
                startReminderTask()
            }
            TaskType.CLEANUP -> {
                cleanupJob?.cancel()
                startCleanupTask()
            }
        }
        
        plugin.logger.info("§7[SchedulerManager] Tâche $taskType redémarrée")
    }
    
    /**
     * Obtient les statistiques des tâches
     */
    fun getTaskStats(): TaskStats {
        return TaskStats(
            expirationCheckRunning = expirationCheckJob?.isActive == true,
            remindersRunning = reminderJob?.isActive == true,
            cleanupRunning = cleanupJob?.isActive == true,
            totalCoroutines = coroutineContext[Job]?.children?.count() ?: 0,
            isSchedulerRunning = isRunning
        )
    }
    
    /**
     * Programme une vérification immédiate des expirations (pour les tests ou commandes admin)
     */
    fun forceExpirationCheck(): Job {
        return runAsync {
            plugin.logger.info("§7[SchedulerManager] Vérification forcée des expirations...")
            val commandesTraitees = commandeManager.processCommandesExpirees()
            val livraisonsTraitees = livraisonManager.processLivraisonsExpirees()
            plugin.logger.info("§7[SchedulerManager] Vérification forcée terminée - Commandes: $commandesTraitees, Livraisons: $livraisonsTraitees")
        }
    }
    
    /**
     * Arrête toutes les tâches périodiques
     */
    fun stopScheduledTasks() {
        if (!isRunning) return
        
        plugin.logger.info("§c[SchedulerManager] Arrêt des tâches périodiques...")
        
        expirationCheckJob?.cancel()
        reminderJob?.cancel()
        cleanupJob?.cancel()
        
        isRunning = false
        plugin.logger.info("§cToutes les tâches périodiques ont été arrêtées")
    }
    
    /**
     * Ferme le SchedulerManager
     */
    fun shutdown() {
        stopScheduledTasks()
        job.cancel()
        plugin.logger.info("§cSchedulerManager fermé")
    }
}

/**
 * Types de tâches périodiques
 */
enum class TaskType {
    EXPIRATION_CHECK,
    REMINDERS,
    CLEANUP
}

/**
 * Statistiques des tâches
 */
data class TaskStats(
    val expirationCheckRunning: Boolean,
    val remindersRunning: Boolean,
    val cleanupRunning: Boolean,
    val totalCoroutines: Int,
    val isSchedulerRunning: Boolean
)
