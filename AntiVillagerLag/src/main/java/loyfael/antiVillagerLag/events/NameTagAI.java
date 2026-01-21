package loyfael.antiVillagerLag.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import loyfael.antiVillagerLag.AntiVillagerLag;
import loyfael.antiVillagerLag.utils.VillagerUtilities;

public class NameTagAI {

    public static boolean call(Villager villager, AntiVillagerLag plugin, Player player) {
        if (!plugin.getConfig().getBoolean("toggleableoptions.userenaming")) return false;

        //  Ensure item is a nametag
        ItemStack nametag = player.getInventory().getItemInMainHand();
        if (!nametag.getType().equals(Material.NAME_TAG)) {
            String name = villager.getCustomName();
            if (name != null) {
                name = name.toLowerCase().replaceAll("(?i)[§&][0-9A-FK-ORXLo]", "");
                return VillagerUtilities.disabling_names.contains(name);
            }
            return false;
        }

        ItemMeta itemMeta = nametag.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) {
            String villagerName = villager.getCustomName();
            if (villagerName != null) {
                villagerName = villagerName.replaceAll("(?i)[§&][0-9A-FK-ORXLo]", "");
                return VillagerUtilities.disabling_names.contains(villagerName.toLowerCase());
            }
            return false;
        }

        //  Should the villager be disabled?
        String itemName = itemMeta.getDisplayName();
        if (itemName != null) {
            itemName = itemName.replaceAll("(?i)[§&][0-9A-FK-ORXLo]", "");
            return VillagerUtilities.disabling_names.contains(itemName.toLowerCase());
        }

        return false;
    }
}