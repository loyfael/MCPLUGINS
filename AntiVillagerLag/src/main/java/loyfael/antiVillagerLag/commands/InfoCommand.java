package loyfael.antiVillagerLag.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import loyfael.antiVillagerLag.AntiVillagerLag;
import loyfael.antiVillagerLag.utils.VillagerUtilities;

public class InfoCommand implements CommandExecutor {

    private final AntiVillagerLag plugin;

    public InfoCommand(AntiVillagerLag plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Display plugin information and usage guide
        player.sendMessage(VillagerUtilities.colorcodes.cm("&a&l=== AntiVillagerLag - Usage Guide ==="));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&e🎯 &7How to optimize villagers:"));

        if (plugin.getConfig().getBoolean("toggleableoptions.userenaming")) {
            player.sendMessage(VillagerUtilities.colorcodes.cm("&a  ✓ &7Name with nametag: &e'Optimize'"));
        }
        if (plugin.getConfig().getBoolean("toggleableoptions.useblocks")) {
            player.sendMessage(VillagerUtilities.colorcodes.cm("&a  ✓ &7Place on block: &eDiamond/Emerald Block"));
        }
        if (plugin.getConfig().getBoolean("toggleableoptions.useworkstations")) {
            player.sendMessage(VillagerUtilities.colorcodes.cm("&a  ✓ &7Near a workstation (radius " +
                plugin.getConfig().getInt("toggleableoptions.workstationcheckradius") + " blocks)"));
        }

        player.sendMessage("");
        player.sendMessage(VillagerUtilities.colorcodes.cm("&6💡 &7Villager states:"));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&a  [OPTIMIZED] &7- AI disabled, ready for trading"));
        player.sendMessage(VillagerUtilities.colorcodes.cm("&e  [ACTIVE] &7- AI enabled, can move/breed"));

        player.sendMessage("");
        player.sendMessage(VillagerUtilities.colorcodes.cm("&c⏰ &7Cooldown: &e" +
            (plugin.getConfig().getLong("ai-toggle-cooldown") / 60) + " minutes"));

        if (player.hasPermission("avl.commands")) {
            player.sendMessage("");
            player.sendMessage(VillagerUtilities.colorcodes.cm("&b🔧 &7Available commands:"));
            player.sendMessage(VillagerUtilities.colorcodes.cm("&b  /avloptimize <radius> &7- Optimize all villagers"));
            player.sendMessage(VillagerUtilities.colorcodes.cm("&b  /avlstatus &7- Plugin statistics"));
        }

        return true;
    }
}
