package loyfael.litefish.minigame;

import loyfael.litefish.LiteFish;
import loyfael.litefish.models.FishDrop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Progress Bar Fishing Mini-game system
 * Player must rapidly right-click to fill a progress bar
 */
public class FishingMiniGame implements Listener {
    
    private final LiteFish plugin;
    private final Map<UUID, ProgressBarFishingSession> activeSessions;
    
    // Static storage for pending catches
    private static final Map<UUID, PendingCatch> pendingCatches = new HashMap<>();
    
    // Inner class to store pending catch data
    public static class PendingCatch {
        final ItemStack item;
        final int experience;
        
        PendingCatch(ItemStack item, int experience) {
            this.item = item;
            this.experience = experience;
        }
        
        public ItemStack getItem() {
            return item;
        }
        
        public int getExperience() {
            return experience;
        }
    }
    
    public static void storePendingCatch(Player player, ItemStack item, int experience) {
        pendingCatches.put(player.getUniqueId(), new PendingCatch(item, experience));
    }
    
    public static PendingCatch getPendingCatch(Player player) {
        return pendingCatches.remove(player.getUniqueId());
    }
    
    public static boolean hasPendingCatch(Player player) {
        return pendingCatches.containsKey(player.getUniqueId());
    }
    
    public FishingMiniGame(LiteFish plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void startMiniGame(Player player, FishDrop drop, Location location, boolean isLavaFishing, FishHook fishHook) {
        UUID playerId = player.getUniqueId();
        
        // Stop any existing session
        stopMiniGame(playerId);
        
        // Create new progress bar fishing session
        ProgressBarFishingSession session = new ProgressBarFishingSession(player, drop, location, isLavaFishing, fishHook);
        activeSessions.put(playerId, session);
        
        // Start the fishing game
        session.start();
    }
    
    public boolean hasActiveSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }
    
    public void stopMiniGame(UUID playerId) {
        ProgressBarFishingSession session = activeSessions.remove(playerId);
        if (session != null) {
            session.stop();
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        ProgressBarFishingSession session = activeSessions.get(playerId);
        if (session != null && session.isActive()) {
            // Only handle right clicks
            if (event.getAction().name().contains("RIGHT_CLICK")) {
                session.onPlayerClick();
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopMiniGame(event.getPlayer().getUniqueId());
    }
    
    public void disable() {
        HandlerList.unregisterAll(this);
        for (ProgressBarFishingSession session : activeSessions.values()) {
            session.stop();
        }
        activeSessions.clear();
    }
    
    /**
     * Progress Bar Fishing Session - Fill the bar with right clicks
     */
    private class ProgressBarFishingSession {
        private final Player player;
        private final FishDrop drop;
        private final Location location;
        private final boolean isLavaFishing;
        private final FishHook fishHook;
        
        private BukkitRunnable gameTask;
        private int progressBar;        // Current progress (0-100)
        private int targetProgress;     // Target to reach (usually 100)
        private boolean gameActive = false;
        private long lastClickTime = 0;
        private int clicksNeeded;       // Total clicks needed to win
        private int clicksMade = 0;     // Clicks made so far
        private int gameTime = 0;       // Time elapsed in ticks
        
        public ProgressBarFishingSession(Player player, FishDrop drop, Location location, boolean isLavaFishing, FishHook fishHook) {
            this.player = player;
            this.drop = drop;
            this.location = location;
            this.isLavaFishing = isLavaFishing;
            this.fishHook = fishHook;
            
            // Initialize progress bar values
            this.progressBar = 0;
            this.targetProgress = 100;
            this.clicksNeeded = calculateClicksNeeded(drop);
        }
        
        /**
         * Calculate clicks needed based on fish rarity
         * More rare fish require more clicks
         */
        private int calculateClicksNeeded(FishDrop drop) {
            // Calculate clicks needed based on rarity (more rare = more clicks)
            if (drop.getChance() < 5) return 40;      // Very rare fish = 40 clicks
            if (drop.getChance() < 15) return 30;     // Rare fish = 30 clicks
            if (drop.getChance() < 50) return 20;     // Uncommon fish = 20 clicks
            return 10;                                 // Common fish = 10 clicks
        }
        
        public boolean isActive() {
            return gameActive;
        }
        
        public void start() {
            gameActive = true;
            
            // Start the progress bar mini-game
            gameTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!gameActive || !player.isOnline()) {
                        onGameTimeout();
                        cancel();
                        return;
                    }
                    
                    gameTime++;
                    
                    // Update progress bar every tick
                    updateProgressBar();
                    
                    // Update display every 5 ticks
                    if (gameTime % 5 == 0) {
                        updateProgressDisplay();
                    }
                    
                    // Check win condition
                    if (progressBar >= targetProgress) {
                        onGameSuccess();
                        cancel();
                        return;
                    }
                    
                    // Check lose condition (timeout after 15 seconds or no clicks in 3 seconds)
                    if (gameTime > 300 || (System.currentTimeMillis() - lastClickTime > 3000 && clicksMade > 0)) { 
                        onGameFailure();
                        cancel();
                        return;
                    }
                }
            };
            
            gameTask.runTaskTimer(plugin, 0L, 1L);
            
            // Initial display and instructions
            player.sendTitle("§3🎣 §lÇA MORD !", 
                             "§bClique rapidement!", 10, 200, 10);
            lastClickTime = System.currentTimeMillis();
        }
        
        private void updateProgressBar() {
            // Decay progress over time (makes it challenging)
            long timeSinceLastClick = System.currentTimeMillis() - lastClickTime;
            if (timeSinceLastClick > 800 && clicksMade > 0) { // After 0.8 seconds, start decaying
                if (progressBar > 0) {
                    progressBar = Math.max(0, progressBar - 1); // Slow decay
                }
            }
        }
        
        private void updateProgressDisplay() {
            // Create progress bar visualization
            StringBuilder barDisplay = new StringBuilder();
            int barLength = 20;
            int filledBars = (progressBar * barLength) / targetProgress;
            
            barDisplay.append("§a");
            for (int i = 0; i < filledBars; i++) {
                barDisplay.append("█");
            }
            
            barDisplay.append("§7");
            for (int i = filledBars; i < barLength; i++) {
                barDisplay.append("█");
            }
            
            // Show in action bar with stats
            String message = String.format("§6Progression: %s §6[%d/%d] §eClics: %d/%d", 
                                          barDisplay.toString(), progressBar, targetProgress, clicksMade, clicksNeeded);
            
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                                       net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
        }
        
        public void onPlayerClick() {
            if (!gameActive) return;
            
            clicksMade++;
            lastClickTime = System.currentTimeMillis();
            
            // Increase progress based on click
            int progressIncrease = (targetProgress / clicksNeeded) + 2; // Slightly more than needed for forgiveness
            progressBar = Math.min(targetProgress, progressBar + progressIncrease);
            
            // Sound feedback with pitch variation
            float pitch = 1.0f + ((float)progressBar / (float)targetProgress);
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.5f, pitch);
            
            // Particle effect at fishing location
            if (isLavaFishing) {
                location.getWorld().spawnParticle(org.bukkit.Particle.LAVA, location, 2, 0.1, 0.1, 0.1, 0.01);
            } else {
                location.getWorld().spawnParticle(org.bukkit.Particle.SPLASH, location, 3, 0.2, 0.1, 0.2, 0.02);
            }
        }
        
        private void onGameSuccess() {
            gameActive = false;
            stop();
            
            // Victory title
            String successTitle = isLavaFishing ? "§c§l" + drop.getDisplayName() : "§a§l" + drop.getDisplayName();
            String successSubtitle = "§a§lCAPTURÉ AVEC SUCCÈS !";
            player.sendTitle(successTitle, successSubtitle, 10, 40, 10);
            
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.5f);
            
            // Give rewards
            giveRewards();
        }
        
        private void onGameFailure() {
            gameActive = false;
            stop();
            
            // Failure title
            player.sendTitle("§c§lÉCHOUÉ !", "§7Le poisson s'est échappé...", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.5f, 0.8f);
            
            // Clean up session
            activeSessions.remove(player.getUniqueId());
        }
        
        private void onGameTimeout() {
            onGameFailure(); // Same behavior as failure
        }
        
        private void giveRewards() {
            // Create the actual item that was caught
            ItemStack dropItem = plugin.getDropManager().createDropItem(drop);
            
            // Calculate experience with bonuses
            ItemStack rod = player.getInventory().getItemInMainHand();
            double rodPower = plugin.getNexoHook().getFishingRodPower(rod);
            ItemStack bait = player.getInventory().getItemInOffHand();
            double baitLuck = plugin.getNexoHook().getBaitLuckBonus(bait);
            
            if (isLavaFishing) {
                rodPower *= 1.5; // Lava fishing bonus
                baitLuck *= 1.2;
            }
            
            int finalExp = (int) (drop.getExperience() * rodPower * baitLuck * 1.2); // 20% mini-game bonus
            
            // Store the pending catch for vanilla system to pick up
            storePendingCatch(player, dropItem, finalExp);
            
            // Record statistics
            plugin.getPlayerDataManager().addFishCaught(player, drop.getKey(), 1);
            
            // Clean up session
            activeSessions.remove(player.getUniqueId());
        }
        
        public void stop() {
            gameActive = false;
            if (gameTask != null && !gameTask.isCancelled()) {
                gameTask.cancel();
                gameTask = null;
            }
            
            // Clear any titles and action bar
            player.sendTitle("", "", 0, 1, 0);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                                       net.md_5.bungee.api.chat.TextComponent.fromLegacyText(""));
        }
    }
}
