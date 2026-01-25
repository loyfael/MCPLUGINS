package loyfael.manager

import loyfael.ClassePlugin
import loyfael.data.Classe
import loyfael.data.PlayerData
import loyfael.data.PassiveType
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Central manager for player classes
 */
class ClasseManager(private val plugin: ClassePlugin) {
    
    private val databaseManager: DatabaseManager = plugin.databaseManager
    private val passiveManager: PassiveManager by lazy { plugin.passiveManager }
    
    // Cache of connected players data
    private val connectedPlayers = ConcurrentHashMap<UUID, PlayerData>()
    
    // Automatic save task
    private var saveTask: BukkitRunnable? = null
    
    /**
     * Initializes the class manager
     */
    fun initialize() {
        // Start automatic save every 5 minutes
        startAutomaticSave()
        
        plugin.logger.info("ClasseManager initialized")
    }
    
    /**
     * Loads player data on connection
     */
    fun loadPlayer(player: Player) {
        val uuid = player.uniqueId
        
        if (plugin.config.getBoolean("debug", false)) {
            plugin.logger.info("Loading data for ${player.name}")
        }
        
        databaseManager.loadPlayer(uuid).thenAccept { playerData ->
            if (playerData != null) {
                // Add to cache
                connectedPlayers[uuid] = playerData
                
                // Apply passives corresponding to the class
                plugin.server.scheduler.runTask(plugin) { _ ->
                    applyClassPassives(player, playerData.classe)
                }
                
            } else {
                plugin.logger.warning("Unable to load data for ${player.name}")
            }
        }.exceptionally { throwable ->
            plugin.logger.log(Level.SEVERE, "Error loading ${player.name}", throwable)
            null
        }
    }
    
    /**
     * Unloads player data on disconnection
     */
    fun unloadPlayer(player: Player) {
        val uuid = player.uniqueId
        val playerData = connectedPlayers[uuid]
        
        if (playerData != null) {
            // Save immediately
            databaseManager.savePlayer(playerData).thenAccept { success ->
                if (!success) {
                    plugin.logger.warning("Failed to save ${player.name}")
                }
            }
            
            // Remove from cache
            connectedPlayers.remove(uuid)
            
            // Clean effects
            passiveManager.cleanPlayerEffects(player)
        }
    }
    
    /**
     * Changes a player's class
     */
    fun changeClasse(player: Player, newClasse: Classe): Boolean {
        val uuid = player.uniqueId
        val playerData = connectedPlayers[uuid] ?: return false
        
        val oldClasse = playerData.classe
        
        // Guard: prevent switching directly from a non-Âme Errante to another non-Âme Errante
        if (newClasse != Classe.AME_ERRANTE && oldClasse != Classe.AME_ERRANTE) {
            // Feedback to player
            plugin.server.scheduler.runTask(plugin, Runnable {
                val prefix = plugin.config.getString("messages.prefix", "")?.replace("&", "§") ?: ""
                val msg = "§cVous devez d'abord réinitialiser votre classe vers §8Âme errante§c avant d'en choisir une nouvelle."
                player.sendMessage(prefix + msg)
            })
            return false
        }
        
        // Remove old passives
        if (oldClasse != Classe.AME_ERRANTE) {
            removeClassPassives(player)
        }
        
        // Change the class
        playerData.changeClasse(newClasse)
        
        // Apply new passives
        if (newClasse != Classe.AME_ERRANTE) {
            applyClassPassives(player, newClasse)
        }
        
        // Save asynchronously
        databaseManager.savePlayer(playerData).thenAccept { success ->
            if (success) {
                // Confirmation message
                val messageKey = if (newClasse == Classe.AME_ERRANTE) "classe_oubliee" else "classe_choisie"
                val message = plugin.config.getString("messages.$messageKey", "") ?: ""
                val formattedMessage = message
                    .replace("{classe}", newClasse.getFullDisplayName())
                    .replace("&", "§")
                
                plugin.server.scheduler.runTask(plugin) { _ ->
                    val prefix = plugin.config.getString("messages.prefix", "") ?: ""
                    player.sendMessage(prefix.replace("&", "§") + formattedMessage)
                }
                
                if (plugin.config.getBoolean("debug", false)) {
                    plugin.logger.info("${player.name} changed class: ${oldClasse.displayName} → ${newClasse.displayName}")
                }
            } else {
                plugin.server.scheduler.runTask(plugin) { _ ->
                    player.sendMessage("§cErreur lors de la sauvegarde de votre classe !")
                }
            }
        }
        
        return true
    }
    
    /**
     * Gets a player's current class
     */
    fun getClasse(player: Player): Classe? {
        return connectedPlayers[player.uniqueId]?.classe
    }
    
    /**
     * Gets player data
     */
    fun getPlayerData(player: Player): PlayerData? {
        return connectedPlayers[player.uniqueId]
    }
    
    /**
     * Gets player data by UUID
     */
    fun getPlayerData(uuid: UUID): PlayerData? {
        return connectedPlayers[uuid]
    }
    
    /**
     * Checks if a player has a specific class
     */
    fun hasClasse(player: Player, classe: Classe): Boolean {
        return getClasse(player) == classe
    }
    
    /**
     * Applies all passives of a class
     */
    private fun applyClassPassives(player: Player, classe: Classe) {
        val playerData = connectedPlayers[player.uniqueId] ?: return
        
        when (classe) {
            Classe.BASTORGNES -> {
                playerData.addPassive(PassiveType.HEMATOMES_PERSISTANTS)
                playerData.addPassive(PassiveType.RESISTANCE_ENDURCIE)
            }
            Classe.TARTINUITS -> {
                playerData.addPassive(PassiveType.PAIN_NOURRICIER)
                playerData.addPassive(PassiveType.SANTE_FLORISSANTE)
                // Apply smaller size for Tartinuits
                passiveManager.setPlayerScale(player, 0.8)
            }
            Classe.SYLVOUNETS -> {
                playerData.addPassive(PassiveType.FORCE_SYLVESTRE)
                playerData.addPassive(PassiveType.AGILITE_FORESTIERE)
            }
            Classe.GROSUKI -> {
                playerData.addPassive(PassiveType.SERENITE_LUNAIRE)
                playerData.addPassive(PassiveType.ESPRIT_APAISE)
            }
            Classe.BRICOBRAK -> {
                playerData.addPassive(PassiveType.OUTILS_BRICOLEURS)
                playerData.addPassive(PassiveType.RESSORT_BRICOLE)
            }
            Classe.MIRAZIENS -> {
                playerData.addPassive(PassiveType.PLUME_LEGERE)
                playerData.addPassive(PassiveType.PRECISION_AERIENNE)
            }
            Classe.AME_ERRANTE -> {
                // No passives
            }
        }
        
        // Apply permanent effects via PassiveManager
        passiveManager.applyPermanentEffects(player)
    }
    
    /**
     * Removes all passives of a class
     */
    private fun removeClassPassives(player: Player) {
        val playerData = connectedPlayers[player.uniqueId] ?: return
        
        // Clear all passives
        playerData.activePassives.clear()
        
        // Clean effects via PassiveManager
        passiveManager.cleanPlayerEffects(player)
        // Ensure scale returns to normal on class removal
        passiveManager.resetPlayerScale(player)
    }
    
    /**
     * Gets all players with a specific class
     */
    fun getPlayersWithClasse(classe: Classe): List<Player> {
        return connectedPlayers.values
            .filter { it.classe == classe }
            .mapNotNull { plugin.server.getPlayer(it.uuid) }
    }
    
    /**
     * Gets the number of players per class
     */
    fun getClassStatistics(): Map<Classe, Int> {
        val stats = mutableMapOf<Classe, Int>()
        connectedPlayers.values.forEach { playerData ->
            stats[playerData.classe] = stats.getOrDefault(playerData.classe, 0) + 1
        }
        return stats
    }
    
    /**
     * Gets the statistics for classes (for command usage)
     */
    fun getClasseStatistics(): List<Pair<Classe, Int>> {
        return getClassStatistics().toList()
    }
    
    /**
     * Gets all online players with any class
     */
    fun getOnlinePlayersWithClass(): List<Player> {
        return plugin.server.onlinePlayers.filter { player ->
            connectedPlayers.containsKey(player.uniqueId)
        }
    }
    
    /**
     * Forces save of all connected players
     */
    fun saveAll(): Int {
        var counter = 0
        connectedPlayers.values.forEach { playerData ->
            databaseManager.savePlayer(playerData).thenAccept { success ->
                if (success) counter++
            }
        }
        return counter
    }
    
    /**
     * Starts the automatic save task
     */
    private fun startAutomaticSave() {
        saveTask?.cancel()
        
        saveTask = object : BukkitRunnable() {
            override fun run() {
                if (connectedPlayers.isNotEmpty()) {
                    val saved = saveAll()
                    
                    if (plugin.config.getBoolean("debug", false)) {
                        plugin.logger.info("Automatic save: $saved player(s)")
                    }
                }
            }
        }
        
        // Save every 5 minutes (6000 ticks)
        saveTask?.runTaskTimerAsynchronously(plugin, 6000L, 6000L)
    }
    
    /**
     * Stops the manager and saves everything
     */
    fun stop() {
        // Stop save task
        saveTask?.cancel()
        
        // Save all connected players
        val nbSaves = saveAll()
        plugin.logger.info("$nbSaves player(s) saved before shutdown")
        
        // Clear cache
        connectedPlayers.clear()
    }
    
    /**
     * Reloads configuration and synchronizes all players
     */
    fun reload() {
        plugin.server.onlinePlayers.forEach { player ->
            val playerData = connectedPlayers[player.uniqueId]
            if (playerData != null) {
                // Reapply passives with new configuration
                removeClassPassives(player)
                applyClassPassives(player, playerData.classe)
            }
        }
        
        plugin.logger.info("ClasseManager reloaded")
    }
}
