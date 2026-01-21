package loyfael.antiVillagerLag.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import loyfael.antiVillagerLag.AntiVillagerLag;

public class BlockAI {
    public static boolean call(Villager villager, AntiVillagerLag plugin, Player player) {
        if (!plugin.getConfig().getBoolean("toggleableoptions.useblocks")) return false; // Pas de contrôle par bloc

        Location loc = villager.getLocation();
        Material blockBelow = villager.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ()).getType();

        // LOGIQUE CORRIGÉE : Même effet que le nom "bonk"
        // Si le villageois est sur un bloc d'émeraude, il doit être optimisé (comme "bonk")
        if (blockBelow == Material.EMERALD_BLOCK) {
            return true; // TRUE = Le villageois doit être OPTIMISÉ (même effet que "bonk")
        }

        // Tous les autres blocs = villageois normal (non-optimisé)
        return false; // FALSE = Le villageois reste normal
    }
}
