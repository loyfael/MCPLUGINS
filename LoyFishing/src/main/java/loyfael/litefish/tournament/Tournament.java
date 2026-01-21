package loyfael.litefish.tournament;

import loyfael.litefish.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents a fishing tournament
 */
public class Tournament {
    
    private final String name;
    private final long duration; // in minutes
    private final Map<UUID, Integer> participants;
    private TournamentState state;
    private long startTime;
    private long endTime;
    
    public Tournament(String name, long durationMinutes) {
        this.name = name;
        this.duration = durationMinutes;
        this.participants = new HashMap<>();
        this.state = TournamentState.WAITING;
    }
    
    public void start() {
        if (state != TournamentState.WAITING) {
            return;
        }
        
        this.state = TournamentState.ACTIVE;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + (duration * 60 * 1000);
        
        // Notify all participants
        for (UUID playerId : participants.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendTitle("§6§lTournament Started!", "§e" + name, 10, 70, 20);
                player.sendMessage("§6[Tournament] §eThe tournament '" + name + "' has started!");
            }
        }
    }
    
    public void end() {
        if (state != TournamentState.ACTIVE) {
            return;
        }
        
        this.state = TournamentState.FINISHED;
        
        // Calculate winners
        List<Map.Entry<UUID, Integer>> sortedParticipants = new ArrayList<>(participants.entrySet());
        sortedParticipants.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        
        // Announce results
        for (UUID playerId : participants.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendTitle("§6§lTournament Ended!", "§eCheck your results", 10, 70, 20);
                announceResults(player, sortedParticipants);
            }
        }
    }
    
    private void announceResults(Player player, List<Map.Entry<UUID, Integer>> results) {
        player.sendMessage("§6§l=== Tournament Results ===");
        
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            UUID winnerId = results.get(i).getKey();
            int score = results.get(i).getValue();
            Player winner = Bukkit.getPlayer(winnerId);
            String winnerName = winner != null ? winner.getName() : "Unknown";
            
            String position = "";
            switch (i) {
                case 0: position = "§6🥇 1st Place"; break;
                case 1: position = "§7🥈 2nd Place"; break;
                case 2: position = "§c🥉 3rd Place"; break;
            }
            
            player.sendMessage(position + "§f: " + winnerName + " §e(" + score + " fish)");
        }
        
        // Show player's position
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getKey().equals(player.getUniqueId())) {
                player.sendMessage("§aYour position: §e#" + (i + 1) + " §7(" + results.get(i).getValue() + " fish)");
                break;
            }
        }
    }
    
    public void addParticipant(Player player) {
        if (state == TournamentState.WAITING) {
            participants.put(player.getUniqueId(), 0);
            player.sendMessage("§6[Tournament] §aYou joined the tournament '" + name + "'!");
        }
    }
    
    public void removeParticipant(Player player) {
        if (participants.remove(player.getUniqueId()) != null) {
            player.sendMessage("§6[Tournament] §cYou left the tournament '" + name + "'!");
        }
    }
    
    public void addFishCaught(Player player, String fishType) {
        if (state != TournamentState.ACTIVE) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        if (participants.containsKey(playerId)) {
            participants.merge(playerId, 1, Integer::sum);
            
            // Show progress
            int currentScore = participants.get(playerId);
            MessageUtils.sendMessage(player, "§6Tournament Score: §e" + currentScore + " fish");
        }
    }
    
    public boolean isActive() {
        return state == TournamentState.ACTIVE;
    }
    
    public boolean isParticipant(Player player) {
        return participants.containsKey(player.getUniqueId());
    }
    
    public long getTimeRemaining() {
        if (state != TournamentState.ACTIVE) {
            return 0;
        }
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    public int getParticipantCount() {
        return participants.size();
    }
    
    public String getName() {
        return name;
    }
    
    public TournamentState getState() {
        return state;
    }
    
    public enum TournamentState {
        WAITING,
        ACTIVE,
        FINISHED
    }
}
