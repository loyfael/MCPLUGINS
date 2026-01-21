package loyfael

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Exemple de listener pour les événements de connexion/déconnexion
 * 
 * Pour l'utiliser, décommentez l'enregistrement dans MyPlugin.kt :
 * server.pluginManager.registerEvents(PlayerConnectionListener(this), this)
 */
class PlayerConnectionListener(
    private val plugin: MyPlugin,
    private val databaseManager: DatabaseManager
) : Listener {
    
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Exemple : sauvegarder le joueur en base de données
        try {
            savePlayerToDatabase(player.uniqueId.toString(), player.name)
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.logger.info("Joueur ${player.name} enregistré en base de données")
            }
            
        } catch (e: Exception) {
            plugin.logger.warning("Erreur lors de l'enregistrement du joueur ${player.name} : ${e.message}")
        }
        
        // Message de bienvenue personnalisé (optionnel)
        val configManager = plugin.getConfigManager()
        if (player.hasPlayedBefore()) {
            // Joueur connu
            if (configManager.hasKey("messages.welcome-back")) {
                val message = configManager.getMessage("messages.welcome-back")
                    .replace("{player}", player.name)
                player.sendMessage(message)
            }
        } else {
            // Nouveau joueur
            if (configManager.hasKey("messages.welcome-new")) {
                val message = configManager.getMessage("messages.welcome-new")
                    .replace("{player}", player.name)
                player.sendMessage(message)
            }
        }
    }
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // Exemple : mettre à jour la dernière connexion
        try {
            updatePlayerLastSeen(player.uniqueId.toString())
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.logger.info("Dernière connexion mise à jour pour ${player.name}")
            }
            
        } catch (e: Exception) {
            plugin.logger.warning("Erreur lors de la mise à jour pour ${player.name} : ${e.message}")
        }
    }
    
    /**
     * Sauvegarde un joueur en base de données
     */
    private fun savePlayerToDatabase(uuid: String, username: String) {
        val sql = """
            INSERT INTO users (uuid, username, created_at, updated_at) 
            VALUES (?, ?, ${getCurrentTimestamp()}, ${getCurrentTimestamp()}) 
            ON DUPLICATE KEY UPDATE 
            username = VALUES(username), 
            updated_at = VALUES(updated_at)
        """.trimIndent()
        
        databaseManager.executeUpdate(sql, uuid, username)
    }
    
    /**
     * Met à jour la dernière connexion d'un joueur
     */
    private fun updatePlayerLastSeen(uuid: String) {
        val sql = "UPDATE users SET updated_at = ${getCurrentTimestamp()} WHERE uuid = ?"
        databaseManager.executeUpdate(sql, uuid)
    }
    
    /**
     * Obtient la valeur timestamp actuelle selon le type de base de données
     */
    private fun getCurrentTimestamp(): String {
        val configManager = plugin.getConfigManager()
        return when (configManager.getDatabaseType()) {
            "mysql" -> "NOW()"
            "sqlite" -> "strftime('%s', 'now')"
            else -> "NOW()"
        }
    }
}