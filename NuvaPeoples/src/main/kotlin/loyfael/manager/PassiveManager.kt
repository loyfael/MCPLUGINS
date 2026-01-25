package loyfael.manager

import loyfael.ClassePlugin
import loyfael.data.Classe
import loyfael.data.PlayerData
import loyfael.data.PassiveType
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager for passives and their effects
 */
class PassiveManager(private val plugin: ClassePlugin) {
    
    private val classeManager: ClasseManager by lazy { plugin.classeManager }
    
    // Tracking for temporary passives
    private val sneakingPlayers = ConcurrentHashMap<UUID, Long>()
    private val lastBiomes = ConcurrentHashMap<UUID, String>()
    
    // Periodic tasks
    private var permanentPassivesTask: BukkitRunnable? = null
    
    /**
     * Initializes the passive manager
     */
    fun initialize() {
        startPermanentPassivesTask()
        plugin.logger.info("PassiveManager initialized")
    }
    
    /**
     * Applies permanent effects for a player
     */
    fun applyPermanentEffects(player: Player) {
        val playerData = classeManager.getPlayerData(player) ?: return
        
        // Permanent Jump Boost for Bricobrak
        if (playerData.hasPassive(PassiveType.RESSORT_BRICOLE)) {
            if (plugin.config.getBoolean("passifs.ressort_bricole.enabled", true)) {
                val level = plugin.config.getInt("passifs.ressort_bricole.niveau_jump_boost", 1)
                player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, Int.MAX_VALUE, level - 1, false, false))
            }
        }
    }
    
    /**
     * Cleans all effects from a player
     */
    fun cleanPlayerEffects(player: Player) {
        // Remove permanent potion effects from the plugin
        player.removePotionEffect(PotionEffectType.JUMP_BOOST)
        player.removePotionEffect(PotionEffectType.SPEED)
        player.removePotionEffect(PotionEffectType.REGENERATION)
        player.removePotionEffect(PotionEffectType.RESISTANCE)
        player.removePotionEffect(PotionEffectType.ABSORPTION)
        player.removePotionEffect(PotionEffectType.WEAKNESS)
        resetPlayerScale(player)
        
        // Clean temporary data
        sneakingPlayers.remove(player.uniqueId)
        lastBiomes.remove(player.uniqueId)
    }

    /**
     * Sets the player's visual scale if supported; falls back to dispatching the attribute command.
     */
    fun setPlayerScale(player: Player, scale: Double) {
        // Use the minecraft:scale attribute command via console (works on 1.21.x)
        val cmd = "attribute ${player.name} minecraft:scale base set $scale"
        plugin.server.dispatchCommand(plugin.server.consoleSender, cmd)
    }

    /**
     * Resets the player's scale to default 1.0.
     */
    fun resetPlayerScale(player: Player) {
        val cmd = "attribute ${player.name} minecraft:scale base set 1.0"
        plugin.server.dispatchCommand(plugin.server.consoleSender, cmd)
    }
    
    /**
     * Calculates damage with passive modifiers
     */
    fun calculateDamageWithPassives(attacker: Player?, damage: Double, weapon: Material?): Double {
        if (attacker == null) return damage
        
        val playerData = classeManager.getPlayerData(attacker) ?: return damage
        var newDamage = damage
        
        // Heavy Strike (Bastorgnes) - Club damage bonus
        if (playerData.hasPassive(PassiveType.HEMATOMES_PERSISTANTS)) {
            if (plugin.config.getBoolean("passifs.frappe_lourde.enabled", true)) {
                if (weapon != null && isBluntWeapon(weapon)) {
                    val bonus = plugin.config.getDouble("passifs.frappe_lourde.bonus_degats", 0.02)
                    newDamage += damage * bonus
                }
            }
        }
        
        // Forest Shooting (Sylvounets) - Bow/crossbow damage bonus
        if (playerData.hasPassive(PassiveType.FORCE_SYLVESTRE)) {
            if (plugin.config.getBoolean("passifs.tir_sylvestre.enabled", true)) {
                if (weapon != null && (weapon == Material.BOW || weapon == Material.CROSSBOW)) {
                    val bonus = plugin.config.getDouble("passifs.tir_sylvestre.bonus_degats", 0.02)
                    newDamage += damage * bonus
                }
            }
        }
        
        return newDamage
    }
    
    /**
     * Calculates damage reduction for victim
     */
    fun calculateDamageReduction(victim: Player, damage: Double): Double {
        val playerData = classeManager.getPlayerData(victim) ?: return damage
        var newDamage = damage
        
        // Iron Skin (Bastorgnes) - Damage reduction
        if (playerData.hasPassive(PassiveType.RESISTANCE_ENDURCIE)) {
            if (plugin.config.getBoolean("passifs.peau_fer.enabled", true)) {
                val reduction = plugin.config.getDouble("passifs.peau_fer.reduction_degats", 0.05)
                newDamage -= damage * reduction
            }
        }
        
        // Light Feather (Miraziens) - Fall damage reduction
        if (playerData.hasPassive(PassiveType.PLUME_LEGERE)) {
            if (plugin.config.getBoolean("passifs.plume_legere.enabled", true)) {
                // This reduction will be applied in EntityDamageEvent for fall damage
                val reduction = plugin.config.getDouble("passifs.plume_legere.reduction_degats_chute", 0.50)
                if (damage > 0) { // If it's fall damage (to be checked in event handler)
                    newDamage -= damage * reduction
                }
            }
        }
        
        return maxOf(0.0, newDamage)
    }
    
    /**
     * Handles Nourishing Bread passive (Tartinuits)
     */
    fun handleNourishingBread(player: Player): Boolean {
        val playerData = classeManager.getPlayerData(player) ?: return false
        
        if (!playerData.hasPassive(PassiveType.PAIN_NOURRICIER)) return false
        if (!plugin.config.getBoolean("passifs.pain_nourricier.enabled", true)) return false
        
        val passive = playerData.getPassive(PassiveType.PAIN_NOURRICIER) ?: return false
        
        // Check cooldown
        val cooldown = plugin.config.getInt("passifs.pain_nourricier.cooldown", 20)
        if (passive.isOnCooldown()) {
            val remainingTime = passive.getRemainingCooldown()
            val message = plugin.config.getString("messages.cooldown_actif", "") ?: ""
            val formattedMessage = message
                .replace("{temps}", remainingTime.toString())
                .replace("&", "§")
            val prefix = plugin.config.getString("messages.prefix", "") ?: ""
            player.sendMessage(prefix.replace("&", "§") + formattedMessage)
            return false
        }
        
        // Trigger passive
        val hearts = plugin.config.getInt("passifs.pain_nourricier.absorption_coeurs", 2)
        val duration = plugin.config.getInt("passifs.pain_nourricier.duree_absorption", 10)
        
        player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, duration * 20, hearts - 1, false, true))
        
        passive.startCooldown(cooldown)
        
        val message = plugin.config.getString("messages.pain_absorption", "") ?: ""
        val formattedMessage = message.replace("&", "§")
        val prefix = plugin.config.getString("messages.prefix", "") ?: ""
        player.sendMessage(prefix.replace("&", "§") + formattedMessage)
        
        return true
    }
    
    /**
     * Handles Lunar Serenity passive (Grosuki)
     */
    fun handleLunarSerenity(player: Player): Boolean {
        val playerData = classeManager.getPlayerData(player) ?: return false
        
        if (!playerData.hasPassive(PassiveType.SERENITE_LUNAIRE)) return false
        if (!plugin.config.getBoolean("passifs.serenite_lunaire.enabled", true)) return false
        
        val passive = playerData.getPassive(PassiveType.SERENITE_LUNAIRE) ?: return false
        
        // Check health threshold
        val threshold = plugin.config.getDouble("passifs.serenite_lunaire.seuil_vie", 0.25)
        val maxHealth = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        val currentHealth = player.health / maxHealth
        
        if (currentHealth >= threshold) return false
        
        // Check cooldown
        val cooldown = plugin.config.getInt("passifs.serenite_lunaire.cooldown", 60)
        if (passive.isOnCooldown()) return false
        
        // Trigger passive
        val level = plugin.config.getInt("passifs.serenite_lunaire.niveau_resistance", 1)
        val duration = plugin.config.getInt("passifs.serenite_lunaire.duree", 5)
        
        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, duration * 20, level - 1, false, true))
        
        passive.startCooldown(cooldown)
        
        val message = plugin.config.getString("messages.resistance_active", "") ?: ""
        val formattedMessage = message.replace("&", "§")
        val prefix = plugin.config.getString("messages.prefix", "") ?: ""
        player.sendMessage(prefix.replace("&", "§") + formattedMessage)
        
        return true
    }
    
    /**
     * Handles Peaceful Spirit passive (Grosuki)
     */
    fun handlePeacefulSpirit(player: Player, isSneaking: Boolean) {
        val playerData = classeManager.getPlayerData(player) ?: return
        
        if (!playerData.hasPassive(PassiveType.ESPRIT_APAISE)) return
        if (!plugin.config.getBoolean("passifs.esprit_apaise.enabled", true)) return
        
        val uuid = player.uniqueId
        val now = System.currentTimeMillis()
        
        if (isSneaking) {
            // Start or continue counter
            if (!sneakingPlayers.containsKey(uuid)) {
                sneakingPlayers[uuid] = now
                playerData.sneakingTime = now
            }
        } else {
            // Stop counter and check if threshold is reached
            val startTime = sneakingPlayers.remove(uuid)
            if (startTime != null) {
                val sneakingDuration = (now - startTime) / 1000.0
                val requiredThreshold = plugin.config.getInt("passifs.esprit_apaise.temps_accroupi", 3)
                
                if (sneakingDuration >= requiredThreshold) {
                    triggerPeacefulSpirit(player)
                }
            }
        }
    }
    
    /**
     * Triggers Peaceful Spirit effect
     */
    private fun triggerPeacefulSpirit(player: Player) {
        val playerData = classeManager.getPlayerData(player) ?: return
        val passive = playerData.getPassive(PassiveType.ESPRIT_APAISE) ?: return
        
        val cooldown = plugin.config.getInt("passifs.esprit_apaise.cooldown", 45)
        if (passive.isOnCooldown()) return
        
        val level = plugin.config.getInt("passifs.esprit_apaise.niveau_regeneration", 1)
        val duration = plugin.config.getInt("passifs.esprit_apaise.duree_regeneration", 5)
        
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, duration * 20, level - 1, false, true))
        
        passive.startCooldown(cooldown)
        
        val message = plugin.config.getString("messages.regeneration_active", "") ?: ""
        val formattedMessage = message.replace("&", "§")
        val prefix = plugin.config.getString("messages.prefix", "") ?: ""
        player.sendMessage(prefix.replace("&", "§") + formattedMessage)
    }
    
    /**
     * Handles durability reduction (Bricobrak)
     */
    fun handleDurabilityReduction(player: Player): Boolean {
        val playerData = classeManager.getPlayerData(player) ?: return false
        
        if (!playerData.hasPassive(PassiveType.OUTILS_BRICOLEURS)) return false
        if (!plugin.config.getBoolean("passifs.outils_bricoleurs.enabled", true)) return false
        
        // Reduction will be applied in tool usage event handler
        return true
    }
    
    /**
     * Handles Aerial Precision (Miraziens)
     */
    fun handleAerialPrecision(target: Player): Boolean {
        if (!plugin.config.getBoolean("passifs.precision_aerienne.enabled", true)) return false
        
        val chance = plugin.config.getDouble("passifs.precision_aerienne.chance_faiblesse", 0.10)
        if (Math.random() > chance) return false
        
        val duration = plugin.config.getInt("passifs.precision_aerienne.duree_faiblesse", 3)
        val level = plugin.config.getInt("passifs.precision_aerienne.niveau_faiblesse", 1)
        
        target.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, duration * 20, level - 1, false, true))
        
        return true
    }
    
    /**
     * Starts the task that handles permanent passives
     */
    private fun startPermanentPassivesTask() {
        permanentPassivesTask?.cancel()
        
        permanentPassivesTask = object : BukkitRunnable() {
            override fun run() {
                plugin.server.onlinePlayers.forEach { player ->
                    handlePermanentPassives(player)
                }
            }
        }
        
        // Run every 2 seconds (40 ticks)
        permanentPassivesTask?.runTaskTimer(plugin, 40L, 40L)
    }
    
    /**
     * Handles permanent passives that require periodic checking
     */
    private fun handlePermanentPassives(player: Player) {
        val playerData = classeManager.getPlayerData(player) ?: return
        
        // Flourishing Health (Tartinuits)
        if (playerData.hasPassive(PassiveType.SANTE_FLORISSANTE)) {
            if (plugin.config.getBoolean("passifs.sante_florissante.enabled", true)) {
                if (player.foodLevel >= 20) {
                    val level = plugin.config.getInt("passifs.sante_florissante.niveau_regeneration", 1)
                    if (!player.hasPotionEffect(PotionEffectType.REGENERATION)) {
                        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 60, level - 1, false, false))
                    }
                }
            }
        }
        
        // Forest Agility (Sylvounets)
        if (playerData.hasPassive(PassiveType.AGILITE_FORESTIERE)) {
            if (plugin.config.getBoolean("passifs.agilite_forestiere.enabled", true)) {
                val biome = player.location.block.biome.key.key.lowercase()
                val biomeType = player.location.block.biome
                val forestBiomes = plugin.config.getStringList("passifs.agilite_forestiere.biomes_forestiers")
                    .map { it.lowercase() }
                
                if (plugin.config.getBoolean("debug", false)) {
                    plugin.logger.info("Biome actuel pour ${player.name}: key='$biome', type='$biomeType'")
                    plugin.logger.info("Biomes forestiers configurés: $forestBiomes")
                }
                
                val isInForest = forestBiomes.contains(biome) || 
                                 forestBiomes.any { biome.contains(it) || it.contains(biome) }
                
                if (isInForest) {
                    if (lastBiomes[player.uniqueId] != biome) {
                        lastBiomes[player.uniqueId] = biome
                        val speedBonus = plugin.config.getDouble("passifs.agilite_forestiere.bonus_vitesse", 0.20)
                        // Convertir 0.20 (20%) en niveau d'effet Minecraft (Speed I = niveau 0, Speed II = niveau 1)
                        val level = (speedBonus * 5).toInt() - 1 // 0.20 * 5 = 1, donc Speed II (niveau 1)
                        val finalLevel = maxOf(0, level) // S'assurer que le niveau est au moins 0
                        
                        // Utiliser la nouvelle API PaperMC
                        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, finalLevel, false, false))
                        
                        if (plugin.config.getBoolean("debug", false)) {
                            plugin.logger.info("Effet de vitesse appliqué à ${player.name}, niveau: $finalLevel (Speed ${finalLevel + 1})")
                        }
                    }
                } else {
                    val lastBiome = lastBiomes[player.uniqueId]
                    if (lastBiome != null && forestBiomes.contains(lastBiome)) {
                        player.removePotionEffect(PotionEffectType.SPEED)
                        lastBiomes.remove(player.uniqueId)
                        
                        if (plugin.config.getBoolean("debug", false)) {
                            plugin.logger.info("Effet de vitesse retiré pour ${player.name}")
                        }
                    }
                }
            }
        }
        
        // Check lunar serenity if low health
        if (playerData.hasPassive(PassiveType.SERENITE_LUNAIRE)) {
            handleLunarSerenity(player)
        }
    }
    
    /**
     * Checks if a weapon is considered blunt (club)
     */
    private fun isBluntWeapon(material: Material): Boolean {
        return when (material) {
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, 
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.MACE -> true // Mace added in 1.21
            else -> false
        }
    }
    
    /**
     * Stops the passive manager
     */
    fun stop() {
        permanentPassivesTask?.cancel()
        sneakingPlayers.clear()
        lastBiomes.clear()
        plugin.logger.info("PassiveManager stopped")
    }
    
    /**
     * Shuts down the passive manager (alias for stop)
     */
    fun shutdown() {
        stop()
    }
}
