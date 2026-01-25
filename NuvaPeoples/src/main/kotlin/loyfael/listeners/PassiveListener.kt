package loyfael.listeners

import loyfael.ClassePlugin
import loyfael.data.PassiveType
import loyfael.manager.ClasseManager
import loyfael.manager.PassiveManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent

/**
 * Minecraft events handler for passives
 */
class PassiveListener(private val plugin: ClassePlugin) : Listener {
    
    private val classeManager: ClasseManager by lazy { plugin.classeManager }
    private val passiveManager: PassiveManager by lazy { plugin.passiveManager }
    
    /**
     * Handle player join events
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Load player data with delay to avoid timing issues
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            classeManager.loadPlayer(player)
        }, 20L) // 1 second delay
    }
    
    /**
     * Handle player quit events
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        classeManager.unloadPlayer(event.player)
    }
    
    /**
     * Handle item consumption events (Nourishing Bread)
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        val item = event.item
        
        // Nourishing Bread (Tartinuits)
        if (item.type == Material.BREAD) {
            val playerData = classeManager.getPlayerData(player) ?: return
            
            if (playerData.hasPassive(PassiveType.PAIN_NOURRICIER)) {
                // Trigger passive with delay so the item is consumed
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    passiveManager.handleNourishingBread(player)
                }, 1L)
            }
        }
    }
    
    /**
     * Handle entity damage events
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return
        
        when (event.cause) {
            EntityDamageEvent.DamageCause.FALL -> {
                // Light Feather (Miraziens) - Fall damage reduction
                val playerData = classeManager.getPlayerData(victim) ?: return
                
                if (playerData.hasPassive(PassiveType.PLUME_LEGERE)) {
                    if (plugin.config.getBoolean("passifs.plume_legere.enabled", true)) {
                        val reduction = plugin.config.getDouble("passifs.plume_legere.reduction_degats_chute", 0.50)
                        val newDamage = event.damage * (1.0 - reduction)
                        event.damage = maxOf(0.0, newDamage)
                    }
                }
            }
            else -> {
                // Other damage types - Apply general reductions
                val newDamage = passiveManager.calculateDamageReduction(victim, event.damage)
                if (newDamage != event.damage) {
                    event.damage = newDamage
                }
                
                // Check lunar serenity after taking damage
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    passiveManager.handleLunarSerenity(victim)
                }, 1L)
            }
        }
    }
    
    /**
     * Handle entity damage by entity events (combat)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player
        val attacker = event.damager as? Player
        
        // Handle damage dealt by player
        if (attacker != null) {
            val weapon = attacker.inventory.itemInMainHand.type
            val newDamage = passiveManager.calculateDamageWithPassives(attacker, event.damage, weapon)
            
            if (newDamage != event.damage) {
                event.damage = newDamage
            }
        }
        
        // Handle damage received by player
        if (victim != null) {
            val newDamage = passiveManager.calculateDamageReduction(victim, event.damage)
            if (newDamage != event.damage) {
                event.damage = newDamage
            }
            
            // Trigger lunar serenity after damage
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                passiveManager.handleLunarSerenity(victim)
            }, 1L)
        }
    }
    
    /**
     * Handle projectile hit events
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) 
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity as? Projectile ?: return
        val shooter = projectile.shooter as? Player ?: return
        val hitEntity = event.hitEntity as? Player ?: return
        
        val playerData = classeManager.getPlayerData(shooter) ?: return
        
        // Aerial Precision (Miraziens) - Weakness effect on arrows
        if (playerData.hasPassive(PassiveType.PRECISION_AERIENNE)) {
            when (projectile.type.name) {
                "ARROW", "SPECTRAL_ARROW" -> {
                    if (passiveManager.handleAerialPrecision(hitEntity)) {
                        // Optional visual effect or sound
                        hitEntity.world.strikeLightningEffect(hitEntity.location)
                    }
                }
            }
        }
    }
    
    /**
     * Handle player sneak toggle events
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        val player = event.player
        val isSneaking = event.isSneaking
        
        val playerData = classeManager.getPlayerData(player) ?: return
        
        // Peaceful Spirit (Grosuki) - Regeneration when sneaking
        if (playerData.hasPassive(PassiveType.ESPRIT_APAISE)) {
            passiveManager.handlePeacefulSpirit(player, isSneaking)
        }
    }
}
