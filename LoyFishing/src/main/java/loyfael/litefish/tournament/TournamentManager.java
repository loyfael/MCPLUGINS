package loyfael.litefish.tournament;

import loyfael.litefish.LiteFish;
import loyfael.litefish.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages fishing tournaments
 */
public class TournamentManager {
    
    private final LiteFish plugin;
    private final List<Tournament> tournaments;
    private Tournament activeTournament;
    
    public TournamentManager(LiteFish plugin) {
        this.plugin = plugin;
        this.tournaments = new ArrayList<>();
        
        // Start periodic announcements
        startAnnouncementTask();
    }
    
    /**
     * Create a new tournament
     */
    public Tournament createTournament(String name, long durationMinutes) {
        Tournament tournament = new Tournament(name, durationMinutes);
        tournaments.add(tournament);
        
        // Announce tournament creation
        Bukkit.broadcastMessage(MessageUtils.colorize("&6[Tournament] &eNew tournament created: &f" + name));
        Bukkit.broadcastMessage(MessageUtils.colorize("&6[Tournament] &eDuration: &f" + durationMinutes + " minutes"));
        Bukkit.broadcastMessage(MessageUtils.colorize("&6[Tournament] &eType &f/lfish tournament join &eto participate!"));
        
        return tournament;
    }
    
    /**
     * Start a tournament
     */
    public boolean startTournament(Tournament tournament) {
        if (activeTournament != null) {
            return false; // Another tournament is already active
        }
        
        if (tournament.getParticipantCount() < 2) {
            return false; // Need at least 2 participants
        }
        
        activeTournament = tournament;
        tournament.start();
        
        // Schedule tournament end
        new BukkitRunnable() {
            @Override
            public void run() {
                endActiveTournament();
            }
        }.runTaskLater(plugin, tournament.getTimeRemaining() / 50); // Convert milliseconds to ticks
        
        return true;
    }
    
    /**
     * End the active tournament
     */
    public void endActiveTournament() {
        if (activeTournament != null) {
            activeTournament.end();
            tournaments.remove(activeTournament);
            activeTournament = null;
        }
    }
    
    /**
     * Get the active tournament
     */
    public Optional<Tournament> getActiveTournament() {
        return Optional.ofNullable(activeTournament);
    }
    
    /**
     * Get all tournaments
     */
    public List<Tournament> getAllTournaments() {
        return new ArrayList<>(tournaments);
    }
    
    /**
     * Get tournaments that are waiting for participants
     */
    public List<Tournament> getWaitingTournaments() {
        return tournaments.stream()
                .filter(t -> t.getState() == Tournament.TournamentState.WAITING)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Add a player to a tournament
     */
    public boolean joinTournament(Player player, Tournament tournament) {
        if (tournament.getState() != Tournament.TournamentState.WAITING) {
            MessageUtils.sendMessage(player, "&cThis tournament is not accepting participants!");
            return false;
        }
        
        if (tournament.isParticipant(player)) {
            MessageUtils.sendMessage(player, "&cYou are already in this tournament!");
            return false;
        }
        
        tournament.addParticipant(player);
        return true;
    }
    
    /**
     * Remove a player from a tournament
     */
    public boolean leaveTournament(Player player, Tournament tournament) {
        if (!tournament.isParticipant(player)) {
            MessageUtils.sendMessage(player, "&cYou are not in this tournament!");
            return false;
        }
        
        tournament.removeParticipant(player);
        return true;
    }
    
    /**
     * Handle fish caught during tournament
     */
    public void onFishCaught(Player player, String fishType) {
        if (activeTournament != null && activeTournament.isParticipant(player)) {
            activeTournament.addFishCaught(player, fishType);
        }
    }
    
    /**
     * Start periodic tournament announcements
     */
    private void startAnnouncementTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeTournament != null) {
                    long timeRemaining = activeTournament.getTimeRemaining();
                    
                    if (timeRemaining > 0) {
                        long minutesLeft = timeRemaining / (1000 * 60);
                        
                        // Announce at specific intervals
                        if (minutesLeft == 10 || minutesLeft == 5 || minutesLeft == 1) {
                            Bukkit.broadcastMessage(MessageUtils.colorize(
                                "&6[Tournament] &e" + minutesLeft + " minute(s) remaining in '" + 
                                activeTournament.getName() + "'!"));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1200L); // Run every minute
    }
    
    /**
     * Create a quick tournament for testing
     */
    public Tournament createQuickTournament() {
        return createTournament("Quick Tournament", 10); // 10 minutes
    }
}
