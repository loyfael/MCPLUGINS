package loyfael.antiVillagerLag.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import loyfael.antiVillagerLag.AntiVillagerLag;
import loyfael.antiVillagerLag.utils.VillagerUtilities;

public class StatusCommand implements CommandExecutor {

    AntiVillagerLag plugin;

    public StatusCommand(AntiVillagerLag plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!command.getName().equalsIgnoreCase("avlstatus")) return true;

        // Make sure it's a player
        if (!(commandSender instanceof Player)) return false;
        Player player = (Player) commandSender;

        // Check if they have permission
        if(!player.hasPermission("avl.status")) {
            player.sendMessage(VillagerUtilities.colorcodes.cm(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        // Default radius or specified radius
        int radius;
        try{
            radius = (strings != null && strings.length > 0) ? Integer.parseInt(strings[0]) : plugin.getConfig().getInt("RadiusDefault");
        } catch (NumberFormatException e) {
            player.sendMessage(VillagerUtilities.colorcodes.cm(plugin.getConfig().getString("messages.radius-invalid")));
            return true;
        }

        boolean canSearchRadius = radius <= plugin.getConfig().getInt("RadiusLimit");
        if(!canSearchRadius){
            player.sendMessage(VillagerUtilities.colorcodes.cm(plugin.getConfig().getString("messages.radius-limit")).replace("%avlradiuslimit%", plugin.getConfig().getString("RadiusLimit")));
            return true;
        }

        // Count villagers
        int totalVillagers = 0;
        int optimizedVillagers = 0;
        int activeVillagers = 0;
        int unmanagedVillagers = 0;

        // Search for nearby villagers
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Villager) {
                Villager villager = (Villager) entity;
                totalVillagers++;

                if (!VillagerUtilities.hasMarker(villager, plugin)) {
                    unmanagedVillagers++;
                } else if (!VillagerUtilities.getMarker(villager, plugin)) {
                    optimizedVillagers++;
                } else {
                    activeVillagers++;
                }
            }
        }

        // Send status report
        player.sendMessage(VillagerUtilities.colorcodes.cm("&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&6           📊 AntiVillagerLag Status Report"));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&e🔍 Search Radius: &7" + radius + " blocks"));
        player.sendMessage(VillagerUtilities.colorcodes.cm(""));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&f👥 Total Villagers Found: &b" + totalVillagers));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&a✅ Optimized (Frozen): &2" + optimizedVillagers));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&e⚡ Active (Mobile): &6" + activeVillagers));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&7❓ Unmanaged: &8" + unmanagedVillagers));
        player.sendMessage(VillagerUtilities.colorcodes.cm(""));

        if (totalVillagers > 0) {
            int optimizationRate = (optimizedVillagers * 100) / totalVillagers;
            int estimatedPerformanceGain = Math.min(90, optimizedVillagers * 2);

            player.sendMessage(VillagerUtilities.colorcodes.cm("&f📈 Optimization Rate: &b" + optimizationRate + "%"));
            player.sendMessage(VillagerUtilities.colorcodes.cm("&f🚀 Estimated Performance Gain: &a~" + estimatedPerformanceGain + "%"));

            if (optimizationRate < 70) {
                player.sendMessage(VillagerUtilities.colorcodes.cm("&c⚠ Consider optimizing more villagers for better performance!"));
            } else if (optimizationRate >= 70 && optimizationRate < 90) {
                player.sendMessage(VillagerUtilities.colorcodes.cm("&e👍 Good optimization level!"));
            } else {
                player.sendMessage(VillagerUtilities.colorcodes.cm("&a🏆 Excellent optimization! Great job!"));
            }
        }

        player.sendMessage(VillagerUtilities.colorcodes.cm(""));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&7💡 Use &f/avloptimize " + radius + "&7 to optimize all villagers"));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        return true;
    }
}
