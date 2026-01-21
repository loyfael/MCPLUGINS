package loyfael.antiVillagerLag.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import loyfael.antiVillagerLag.AntiVillagerLag;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerUtilities {

    private static final String MARKER_KEY = "Marker";
    private static final String AI_COOLDOWN_KEY = "cooldown";
    private static final String LEVEL_COOLDOWN_KEY = "levelCooldown";
    private static final String LAST_RESTOCK_KEY = "time";

    private static volatile NamespacedKey markerKey;
    private static volatile NamespacedKey aiCooldownKey;
    private static volatile NamespacedKey levelCooldownKey;
    private static volatile NamespacedKey lastRestockKey;

    public static final ColorCode colorcodes = new ColorCode();

    public static final Set<String> disabling_names = ConcurrentHashMap.newKeySet();
    public static final EnumSet<Material> standingon_blocks = EnumSet.noneOf(Material.class);
    public static final EnumSet<Material> disabling_blocks = EnumSet.noneOf(Material.class);
    public static final EnumSet<Material> enabling_blocks = EnumSet.noneOf(Material.class);
    public static final EnumSet<Material> workstation_blocks = EnumSet.noneOf(Material.class);
    public static final List<Long> restock_times = new ArrayList<>();

    public static synchronized void initializeKeys(AntiVillagerLag plugin) {
        if (markerKey == null) {
            markerKey = new NamespacedKey(plugin, MARKER_KEY);
            aiCooldownKey = new NamespacedKey(plugin, AI_COOLDOWN_KEY);
            levelCooldownKey = new NamespacedKey(plugin, LEVEL_COOLDOWN_KEY);
            lastRestockKey = new NamespacedKey(plugin, LAST_RESTOCK_KEY);
        }
    }

    public static void initializeNewVillager(Villager villager, AntiVillagerLag plugin, long currentTime) {
        PersistentDataContainer container = villager.getPersistentDataContainer();

        container.set(markerKey, PersistentDataType.BOOLEAN, true);
        container.set(aiCooldownKey, PersistentDataType.LONG, 0L);
        container.set(levelCooldownKey, PersistentDataType.LONG, 0L);
        container.set(lastRestockKey, PersistentDataType.LONG, villager.getWorld().getFullTime());
    }

    public static boolean hasMarker(Villager v, AntiVillagerLag plugin) {
        return v.getPersistentDataContainer().has(markerKey, PersistentDataType.BOOLEAN);
    }

    public static boolean getMarker(Villager v, AntiVillagerLag plugin) {
        Boolean result = v.getPersistentDataContainer().get(markerKey, PersistentDataType.BOOLEAN);
        return result != null ? result : false;
    }

    public static void setMarker(Villager v, AntiVillagerLag plugin, boolean val) {
        v.getPersistentDataContainer().set(markerKey, PersistentDataType.BOOLEAN, val);
    }

    public static long getAiCooldown(Villager v, AntiVillagerLag plugin) {
        Long result = v.getPersistentDataContainer().get(aiCooldownKey, PersistentDataType.LONG);
        return result != null ? result : 0L;
    }

    public static long getLevelCooldown(Villager v, AntiVillagerLag plugin) {
        Long result = v.getPersistentDataContainer().get(levelCooldownKey, PersistentDataType.LONG);
        return result != null ? result : 0L;
    }

    public static long getLastRestock(Villager v, AntiVillagerLag plugin) {
        Long result = v.getPersistentDataContainer().get(lastRestockKey, PersistentDataType.LONG);
        return result != null ? result : 0L;
    }

    public static void setAiCooldown(Villager v, AntiVillagerLag plugin, long cooldown) {
        v.getPersistentDataContainer().set(aiCooldownKey, PersistentDataType.LONG,
            (System.currentTimeMillis() / 1000) + cooldown);
    }

    public static void setLevelCooldown(Villager v, AntiVillagerLag plugin, long cooldown) {
        v.getPersistentDataContainer().set(levelCooldownKey, PersistentDataType.LONG,
            (System.currentTimeMillis() / 1000) + cooldown);
    }

    public static void setLastRestock(Villager v, AntiVillagerLag plugin) {
        v.getPersistentDataContainer().set(lastRestockKey, PersistentDataType.LONG,
            v.getWorld().getFullTime());
    }

    public static void updateNameTags(AntiVillagerLag plugin) {
        if (!plugin.getConfig().getBoolean("toggleableoptions.userenaming")) return;

        disabling_names.clear();
        List<String> names = plugin.getConfig().getStringList("NamesThatDisable");
        for (String name : names) {
            disabling_names.add(name.toLowerCase());
        }
    }

    public static void updateStandingOnBlocks(AntiVillagerLag plugin) {
        if (!plugin.getConfig().getBoolean("toggleableoptions.useblocks")) return;

        // Charger les blocs qui DÉSACTIVENT les villageois
        disabling_blocks.clear();
        List<String> disablingBlockNames = plugin.getConfig().getStringList("BlocksThatDisable");
        for (String blockName : disablingBlockNames) {
            Material block = Material.getMaterial(blockName.toUpperCase());
            if (block != null) {
                disabling_blocks.add(block);
            }
        }

        // Charger les blocs qui ACTIVENT les villageois
        enabling_blocks.clear();
        List<String> enablingBlockNames = plugin.getConfig().getStringList("BlocksThatEnable");
        for (String blockName : enablingBlockNames) {
            Material block = Material.getMaterial(blockName.toUpperCase());
            if (block != null) {
                enabling_blocks.add(block);
            }
        }

        // Maintenir la compatibilité avec l'ancienne liste (sera supprimée dans une version future)
        standingon_blocks.clear();
        standingon_blocks.addAll(disabling_blocks);
    }

    public static void updateWorkstationBlocks(AntiVillagerLag plugin) {
        if (!plugin.getConfig().getBoolean("toggleableoptions.useworkstations")) return;

        workstation_blocks.clear();
        List<String> blockNames = plugin.getConfig().getStringList("WorkstationsThatDisable");
        for (String blockName : blockNames) {
            Material block = Material.getMaterial(blockName.toUpperCase());
            if (block != null) {
                workstation_blocks.add(block);
            }
        }
    }

    public static void updateRestockTimes(AntiVillagerLag plugin) {
        restock_times.clear();
        restock_times.addAll(plugin.getConfig().getLongList("RestockTimes.times"));
    }

    public static void CleanseTheVillagers(Villager v, AntiVillagerLag plugin) {
        if (!hasMarker(v, plugin)) return;

        v.setAware(true);
        PersistentDataContainer container = v.getPersistentDataContainer();

        container.remove(markerKey);
        container.remove(aiCooldownKey);
        container.remove(levelCooldownKey);
        container.remove(lastRestockKey);

        VillagerCache.removeVillager(v.getUniqueId());
    }

    public static void restock(Villager v) {
        List<MerchantRecipe> recipes = v.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            recipes.get(i).setUses(0);
        }
    }
}
