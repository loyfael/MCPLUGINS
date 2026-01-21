package loyfael.antiVillagerLag.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import loyfael.antiVillagerLag.AntiVillagerLag;
import loyfael.antiVillagerLag.utils.VillagerUtilities;

public class OptimizeCommand implements CommandExecutor {

    AntiVillagerLag plugin;

    public OptimizeCommand(AntiVillagerLag plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!command.getName().equalsIgnoreCase("avloptimize")) return true;
        //  Make sure it's a player
        if (!(commandSender instanceof Player)) return false;
        Player player = (Player) commandSender;

        //  Check if they have permission
        if(!player.hasPermission("avl.optimize")) {
            player.sendMessage(VillagerUtilities.colorcodes.cm(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        //searchable radius based on first argument specified, if null defaults to config
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
        player.sendMessage(VillagerUtilities.colorcodes.cm(plugin.getConfig().getString("messages.searching-radius")).replace("%avlradius%", String.valueOf(radius)));
        player.sendMessage(VillagerUtilities.colorcodes.cm(plugin.getConfig().getString("messages.bulk-optimize-start")));

        // Counter for optimized villagers
        int optimizedCount = 0;

        // Search for nearby villagers
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Villager) {
                Villager villager = (Villager) entity;
                //  Setup new Villagers
                if (!VillagerUtilities.hasMarker(villager, plugin)) {
                    VillagerUtilities.setAiCooldown(villager, plugin, 0);
                    VillagerUtilities.setLevelCooldown(villager, plugin, 0);
                    VillagerUtilities.setLastRestock(villager, plugin);
                    VillagerUtilities.setMarker(villager, plugin, true);
                }

                // Only optimize if not already optimized
                if (VillagerUtilities.getMarker(villager, plugin)) {
                    //  Rename villager - correction pour Set au lieu de List
                    if (!VillagerUtilities.disabling_names.isEmpty()) {
                        String firstName = VillagerUtilities.disabling_names.iterator().next();
                        villager.setCustomName(firstName);
                    }
                    //  Update the marker and AI
                    VillagerUtilities.setMarker(villager, plugin, false);
                    villager.setAware(false);
                    optimizedCount++;
                }
            }
        }

        // Send completion message with statistics
        String completeMsg = plugin.getConfig().getString("messages.bulk-optimize-complete");
        completeMsg = completeMsg.replace("%avlcount%", String.valueOf(optimizedCount));
        player.sendMessage(VillagerUtilities.colorcodes.cm(completeMsg));

        // Calculate and show performance impact
        if (optimizedCount > 0) {
            int estimatedPerformanceGain = Math.min(90, optimizedCount * 2); // Rough estimate: 2% per villager, max 90%
            String performanceMsg = plugin.getConfig().getString("messages.performance-impact");
            performanceMsg = performanceMsg.replace("%avlpercentage%", String.valueOf(estimatedPerformanceGain));
            player.sendMessage(VillagerUtilities.colorcodes.cm(performanceMsg));
            player.sendMessage(VillagerUtilities.colorcodes.cm(plugin.getConfig().getString("messages.villager-zombie-protected")));
        }
        return true;
    }
}
