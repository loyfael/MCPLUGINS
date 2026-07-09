package loyfael.litefish.minigame;

import loyfael.litefish.LiteFish;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * VOID FISHING MINI-GAME - SÉPARÉ ET AUTONOME
 * 
 * Système complètement indépendant du FishingMiniGame normal
 * pour éviter les conflits et l'instabilité
 */
public class VoidFishingMiniGame implements Listener {
    
    private final LiteFish plugin;
    private final Map<UUID, VoidSession> activeSessions;
    
    public VoidFishingMiniGame(LiteFish plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Démarre une session de pêche void - SYSTÈME SÉPARÉ
     */
    public void startVoidMiniGame(Player player, String dropKey, String displayName, 
                                  int experience, int rarity, 
                                  Location location, FishHook hook) {
        // Nettoie toute session existante
        stopSession(player.getUniqueId());
        
        // Crée une nouvelle session void autonome
    VoidSession session = new VoidSession(player, dropKey, displayName, 
                        experience, rarity, location, hook);
        activeSessions.put(player.getUniqueId(), session);
        
        // Démarre la session
        session.start();
    }
    
    /**
     * Arrête une session void avec cleanup complet
     */
    public void stopSession(UUID playerId) {
        VoidSession session = activeSessions.remove(playerId);
        if (session != null) {
            // Cleanup complet de la session
            session.performCompleteCleanup();
        }
    }
    
    /**
     * Vérifie si un joueur a une session void active
     */
    public boolean hasActiveSession(Player player) {
        VoidSession session = activeSessions.get(player.getUniqueId());
        return session != null && session.isActive();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        VoidSession session = activeSessions.get(player.getUniqueId());
        
        if (session != null && session.isActive()) {
            // Ne gérer que les clics droits comme le mini-jeu lave/eau
            if (event.getAction().name().contains("RIGHT_CLICK")) {
                session.onPlayerClick();
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        VoidSession session = activeSessions.get(player.getUniqueId());
        
        if (session != null && session.isActive()) {
            // Le joueur change d'item en main pendant le mini-jeu - annule la session
            stopSession(player.getUniqueId());
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopSession(event.getPlayer().getUniqueId());
    }
    
    /**
     * Session de pêche void - SYSTÈME BARRE DE PROGRESSION
     */
    private class VoidSession {
        private final Player player;
        private final String dropKey;
        private final String displayName;
        private final int experience;
        private final Location location;
        private final FishHook fishHook;
        
        private BukkitRunnable gameTask;
        private boolean gameActive = false;
        
        // Nouveau système de barre de progression
        private int progressBar;        // Current progress (0-100)
        private int targetProgress;     // Target to reach (usually 100)
        private long lastClickTime = 0;
        private int clicksNeeded;       // Total clicks needed to win
        private int clicksMade = 0;     // Clicks made so far
        private int gameTime = 0;       // Time elapsed in ticks
        
    public VoidSession(Player player, String dropKey, String displayName, 
              int experience, int rarity,
                          Location location, FishHook fishHook) {
            this.player = player;
            this.dropKey = dropKey;
            this.displayName = displayName;
            this.experience = experience;
            this.location = location;
            this.fishHook = fishHook;
            
            // Initialize progress bar values (void creatures are more challenging)
            this.progressBar = 0;
            this.targetProgress = 100;
            this.clicksNeeded = calculateClicksNeeded(rarity);
        }
        
        private int calculateClicksNeeded(int rarity) {
            // Void creatures need more clicks (they're mystical and strong)
            if (rarity >= 80) return 20;     // Very rare void entity = 20 clicks
            if (rarity >= 60) return 16;     // Rare void entity = 16 clicks  
            if (rarity >= 40) return 14;     // Uncommon void entity = 14 clicks
            return 12;                       // Common void entity = 12 clicks
        }
        
        public void start() {
            gameActive = true;
            
            // Effets visuels d'entrée (thème void en violet)
            location.getWorld().spawnParticle(Particle.WITCH, location, 10, 0.6, 0.4, 0.6, 0.01);
            location.getWorld().spawnParticle(Particle.CLOUD, location, 12, 0.8, 0.5, 0.8, 0.02);
            player.playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.2f);
            
            // Boucle identique au mini-jeu lave/eau: tick chaque tick
            gameTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!gameActive || !player.isOnline()) {
                        onGameTimeout();
                        cancel();
                        return;
                    }
                    gameTime++;
                    
                    // Met à jour la barre chaque tick (avec décroissance après 0.8s)
                    updateProgressBar();
                    
                    // Met à jour l'affichage toutes les 5 ticks (ACTION BAR uniquement pour éviter le clignotement de Title)
                    if (gameTime % 5 == 0) {
                        updateProgressDisplay();
                    }
                    
                    // Condition de victoire
                    if (progressBar >= targetProgress) {
                        onGameSuccess();
                        cancel();
                        return;
                    }
                    
                    // Condition d'échec (timeout 15s ou inactivité > 3s après 1er clic)
                    if (gameTime > 300 || (System.currentTimeMillis() - lastClickTime > 3000 && clicksMade > 0)) {
                        onGameFailure();
                        cancel();
                    }
                }
            };
            gameTask.runTaskTimer(plugin, 0L, 1L);
            
            // Titre d'entrée (thème void) - affiché UNE SEULE FOIS, le reste passe par l'action bar
            player.sendTitle("§5🎣 §lÇA MORD !", "§dClique rapidement !", 10, 200, 10);
            lastClickTime = System.currentTimeMillis();
        }
        
        public void stop() {
            gameActive = false;
            if (gameTask != null && !gameTask.isCancelled()) {
                gameTask.cancel();
            }
            // NE PAS effacer le titre ici - laissons les méthodes de succès/échec gérer ça
        }
        
        public boolean isActive() {
            return gameActive;
        }
        
        private void updateProgressBar() {
            // Décroissance douce après 0.8s d'inactivité (comme lava/eau)
            long since = System.currentTimeMillis() - lastClickTime;
            if (since > 800 && clicksMade > 0 && progressBar > 0) {
                progressBar = Math.max(0, progressBar - 1);
            }
            
            // Particules ambiantes (thème violet)
            if (Math.random() < 0.05) {
                location.getWorld().spawnParticle(Particle.WITCH, location, 1, 0.3, 0.2, 0.3, 0.01);
            }
        }
        
        public void onPlayerClick() {
            if (!gameActive) return;
            
            clicksMade++;
            lastClickTime = System.currentTimeMillis();
            
            // Gain de progression identique au lava/eau
            int progressIncrease = (targetProgress / clicksNeeded) + 2;
            progressBar = Math.min(targetProgress, progressBar + progressIncrease);
            
            // Son feedback (léger pitch up)
            float pitch = 1.0f + ((float)progressBar / (float)targetProgress);
            player.playSound(location, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.6f, pitch);
            
            // Particules violettes
            location.getWorld().spawnParticle(Particle.WITCH, location, 2, 0.2, 0.2, 0.2, 0.01);
            
            if (progressBar >= targetProgress) {
                onGameSuccess();
                return;
            }
        }
        
        private void updateProgressDisplay() {
            // Barre de progression en ACTION BAR (évite le clignotement de Title)
            StringBuilder bar = new StringBuilder();
            int filled = (progressBar * 20 / targetProgress);
            for (int i = 0; i < 20; i++) {
                if (i < filled) {
                    bar.append("§5█"); // Rempli violet
                } else {
                    bar.append("§8█"); // Fond gris
                }
            }
            String message = String.format("§dProgression: %s §7[%d/%d] §8Clics: §d%d§7/§d%d",
                    bar.toString(), progressBar, targetProgress, clicksMade, clicksNeeded);
            player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
            );
        }
        
        private void onGameSuccess() {
            gameActive = false;
            stop();
            
            // Titre de succès (même modèle que lava, en violet)
            String title = "§5§l" + (displayName == null || displayName.isEmpty() ? "PRISE" : displayName);
            String subtitle = "§a§lCAPTURÉ AVEC SUCCÈS !";
            player.sendTitle(title, subtitle, 10, 40, 10);
            
            // Sons et particules
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.5f);
            location.getWorld().spawnParticle(Particle.WITCH, location, 12, 0.6, 0.4, 0.6, 0.02);
            location.getWorld().spawnParticle(Particle.ENCHANT, location, 8, 0.5, 0.4, 0.5, 0.1);
            
            // Récompenses
            giveVoidRewards();
            
            // Cleanup
            performCompleteCleanup();
        }

        private void onGameFailure() {
            gameActive = false;
            stop();
            player.sendTitle("§c§lÉCHOUÉ !", "§7L'entité s'est échappée...", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.5f, 0.8f);
            activeSessions.remove(player.getUniqueId());
        }

        private void onGameTimeout() {
            onGameFailure();
        }
        
        /**
         * CLEANUP COMPLET - Nettoie tout l'état du void fishing
         */
        private void performCompleteCleanup() {
            try {
                // 1. Supprime la session de la map
                activeSessions.remove(player.getUniqueId());
                
                // 2. Arrête la tâche de jeu
                stop();
                
                // 3. Nettoie les particules dans un rayon autour de la location
                clearParticlesInArea();
                
                // 4. Nettoie le poisson virtuel s'il existe dans VoidFishing
                cleanupVirtualFishFromVoidFishing();
                
                // 5. Efface le titre après un délai
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendTitle("", "", 0, 1, 0);
                    }
                }, 80L); // 4 secondes après
                
                // 6. RÉCUPÈRE L'HAMEÇON AVEC DÉLAI (SÉPARÉ)
                scheduleHookRetrieval();
                
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du cleanup void fishing: " + e.getMessage());
            }
        }
        
        /**
         * Programme la récupération de l'hameçon APRÈS un délai
         */
        private void scheduleHookRetrieval() {
            // DÉLAI IMPORTANT : Laisse le temps au joueur de voir l'hameçon après le mini-game
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                returnFishingRodToPlayer();
            }, 40L); // 2 secondes après la fin du mini-game
        }
        
        /**
         * Récupère l'hameçon dans l'inventaire du joueur (appelée APRÈS délai)
         */
        private void returnFishingRodToPlayer() {
            if (fishHook != null && !fishHook.isDead()) {
                try {
                    // Retire l'hameçon du monde (exécuté APRÈS le délai de scheduleHookRetrieval)
                    fishHook.remove();
                    
                    // Remet automatiquement la canne à pêche en mode "non-lancée"
                    if (player.isOnline()) {
                        ItemStack mainHand = player.getInventory().getItemInMainHand();
                        if (mainHand.getType().name().contains("FISHING_ROD")) {
                            // Force la mise à jour de l'état de la canne
                            player.updateInventory();
                        }
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur lors de la récupération de l'hameçon: " + e.getMessage());
                }
            }
        }
        
        /**
         * Nettoie les particules dans la zone de pêche
         */
        private void clearParticlesInArea() {
            try {
                // Note: Minecraft ne permet pas de supprimer directement les particules déjà spawned
                // Mais on peut créer un "nettoyage visuel" avec des particules blanches
                location.getWorld().spawnParticle(Particle.SWEEP_ATTACK, location, 1, 0.1, 0.1, 0.1, 0);
                
                // Optionnel: on peut jouer un son de "nettoyage"
                player.playSound(location, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 0.3f, 2.0f);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du nettoyage des particules: " + e.getMessage());
            }
        }
        
        /**
         * Nettoie le poisson virtuel du système VoidFishing
         */
        private void cleanupVirtualFishFromVoidFishing() {
            try {
                // Récupère le VoidFishing associé à cet hameçon et nettoie le poisson virtuel
                if (fishHook != null) {
                    loyfael.litefish.mechanics.VoidFishing voidFishing = 
                        loyfael.litefish.mechanics.VoidFishing.getVoidFishing(fishHook);
                    
                    if (voidFishing != null) {
                        // Arrête le VoidFishing (qui nettoiera le poisson virtuel)
                        voidFishing.stop();
                        // Supprime de la map statique
                        loyfael.litefish.mechanics.VoidFishing.bobberList.remove(fishHook);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du nettoyage du poisson virtuel: " + e.getMessage());
            }
        }
        
        private void giveVoidRewards() {
            // Système de récompenses VOID spécifique
            // Récupère le FishDrop depuis la clé
            Optional<loyfael.litefish.models.FishDrop> dropOpt = plugin.getDropManager().getDrop(dropKey);
            ItemStack voidItem;
            
            if (dropOpt.isPresent()) {
                // Utilise le système existant si le drop existe
                voidItem = plugin.getDropManager().createDropItem(dropOpt.get());
            } else {
                // Fallback : crée un item basique avec les propriétés fournies
                voidItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COD);
                org.bukkit.inventory.meta.ItemMeta meta = voidItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§5Void " + displayName);
                    voidItem.setItemMeta(meta);
                }
            }
            
            // Bonus void (plus élevés car plus difficile)
            ItemStack rod = player.getInventory().getItemInMainHand();
            double rodPower = plugin.getNexoHook().getFishingRodPower(rod) * 1.5; // 50% bonus void
            
            ItemStack bait = player.getInventory().getItemInOffHand();
            double baitLuck = plugin.getNexoHook().getBaitLuckBonus(bait) * 1.3; // 30% bonus void
            
            int finalExp = (int) (experience * rodPower * baitLuck);
            
            // Donne l'objet
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(voidItem);
            } else {
                location.getWorld().dropItemNaturally(location, voidItem);
            }
            
            // Donne l'expérience
            if (plugin.getConfigManager().getConfig().getBoolean("fishing.vanilla-exp", true)) {
                player.giveExp(finalExp);
            }
            
            // Enregistre les statistiques
            plugin.getPlayerDataManager().addFishCaught(player, dropKey, 1);
        }
    }
    
    /**
     * Nettoyage
     */
    public void shutdown() {
        for (VoidSession session : activeSessions.values()) {
            session.stop();
        }
        activeSessions.clear();
        HandlerList.unregisterAll(this);
    }
}