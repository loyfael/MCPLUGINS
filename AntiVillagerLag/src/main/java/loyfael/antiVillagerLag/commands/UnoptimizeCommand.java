package loyfael.antiVillagerLag.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import loyfael.antiVillagerLag.AntiVillagerLag;
import loyfael.antiVillagerLag.utils.VillagerUtilities;

public class UnoptimizeCommand implements CommandExecutor {

    AntiVillagerLag plugin;

    public UnoptimizeCommand(AntiVillagerLag plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!command.getName().equalsIgnoreCase("avlunoptimize")) return true;
        //  Make sure it's a player
        if (!(commandSender instanceof Player)) return false;
        Player player = (Player) commandSender;

        //  Check if they have permission
        if(!player.hasPermission("avl.unoptimize")) {
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
        player.sendMessage(VillagerUtilities.colorcodes.cm(plugin.getConfig().getString("messages.bulk-unoptimize-start")));

        // Counter for unoptimized villagers
        int unoptimizedCount = 0;

        // Search for nearby villagers
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Villager) {
                Villager villager = (Villager) entity;
                //  Setup new Villagers
                if (!VillagerUtilities.hasMarker(villager, plugin)) {
                    VillagerUtilities.setAiCooldown(villager, plugin, 0);
                    VillagerUtilities.setLevelCooldown(villager, plugin, 0);
                    VillagerUtilities.setLastRestock(villager, plugin);
                    VillagerUtilities.setMarker(villager, plugin, true);
                }

                // Only unoptimize if currently optimized
                if (!VillagerUtilities.getMarker(villager, plugin)) {
                    //  Rename villager
                    villager.setCustomName("");
                    //  Update the marker and AI
                    VillagerUtilities.setMarker(villager, plugin, true);
                    villager.setAware(true);
                    unoptimizedCount++;
                }
            }
        }

        // Send completion message with statistics
        String completeMsg = plugin.getConfig().getString("messages.bulk-unoptimize-complete");
        completeMsg = completeMsg.replace("%avlcount%", String.valueOf(unoptimizedCount));
        player.sendMessage(VillagerUtilities.colorcodes.cm(completeMsg));

        // Show breeding reminder if villagers were activated
        if (unoptimizedCount > 0) {
            player.sendMessage(VillagerUtilities.colorcodes.cm(plugin.getConfig().getString("messages.villager-breeding-reminder")));
        }
        return true;
    }
}
