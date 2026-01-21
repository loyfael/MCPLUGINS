package loyfael.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import loyfael.Main;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Material translation manager
 * Loads French names from materials.yml
 */
public class MaterialTranslator {

    private static MaterialTranslator instance;
    private final Map<String, String> materialNames;
    private final Main plugin;

    private MaterialTranslator() {
        this.plugin = Main.getInstance();
        this.materialNames = new HashMap<>();
        loadMaterialNames();
    }

    /**
     * Gets the singleton instance
     */
    public static MaterialTranslator getInstance() {
        try {
            if (instance == null) instance = new MaterialTranslator();
            return instance;
        } catch (Exception e) {
            System.err.println("Critical error creating MaterialTranslator instance: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads material names from materials.yml
     */
    private void loadMaterialNames() {
        try {
            File materialsFile = new File(plugin.getDataFolder(), "materials.yml");

            // Create the file if it doesn't exist
            if (!materialsFile.exists()) {
                try {
                    createDefaultMaterialsFile(materialsFile);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to create default materials.yml: " + e.getMessage());
                    Utils.sendConsoleLog("&cCritical error: Could not create materials.yml");
                    return;
                }
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(materialsFile);

            // Load all materials into the map
            try {
                for (String key : config.getKeys(false)) {
                    String frenchName = config.getString(key);
                    if (frenchName != null && !frenchName.trim().isEmpty()) {
                        materialNames.put(key.toLowerCase(), frenchName);
                    } else {
                        plugin.getLogger().warning("Empty or null material name for key: " + key);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error processing material entries: " + e.getMessage());
                Utils.sendConsoleLog("&cFailed to process some material entries");
            }

            Utils.sendConsoleLog("&a" + materialNames.size() + " material names loaded from materials.yml");

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading materials.yml", e);
            Utils.sendConsoleLog("&cError loading materials.yml: " + e.getMessage());

            // Initialize with empty map to prevent null pointer exceptions
            if (materialNames.isEmpty()) {
                plugin.getLogger().warning("MaterialTranslator running with empty material names map");
                Utils.sendConsoleLog("&eWarning: MaterialTranslator running without loaded materials");
            }
        }
    }

    /**
     * Creates the default materials.yml file with all materials
     */
    private void createDefaultMaterialsFile(File materialsFile) {
        try {
            // Create parent folder if necessary
            if (!materialsFile.getParentFile().exists()) {
                if (!materialsFile.getParentFile().mkdirs()) {
                    throw new IOException("Could not create parent directories for materials.yml");
                }
            }

            FileConfiguration config = new YamlConfiguration();

            // Add file header
            config.options().header("French material names configuration\nFormat: minecraft_id: \"Name in French\"");

            // Basic blocks
            try {
                config.set("stone", "Pierre");
                config.set("granite", "Granit");
                config.set("diorite", "Diorite");
                config.set("andesite", "Andésite");
                config.set("cobblestone", "Pierre taillée");
                config.set("mossy_cobblestone", "Pierre taillée moussue");
                config.set("smooth_stone", "Pierre lisse");
                config.set("stone_bricks", "Briques de pierre");
                config.set("cracked_stone_bricks", "Briques de pierre fissurées");
                config.set("mossy_stone_bricks", "Briques de pierre moussues");
                config.set("chiseled_stone_bricks", "Briques de pierre sculptées");

                // Ores
                config.set("coal_ore", "Minerai de charbon");
                config.set("iron_ore", "Minerai de fer");
                config.set("gold_ore", "Minerai d'or");
                config.set("diamond_ore", "Minerai de diamant");
                config.set("emerald_ore", "Minerai d'émeraude");
                config.set("lapis_ore", "Minerai de lapis-lazuli");
                config.set("redstone_ore", "Minerai de redstone");
                config.set("copper_ore", "Minerai de cuivre");
                config.set("netherite_scrap", "Débris de netherite");
                config.set("nether_quartz_ore", "Minerai de quartz du Nether");

                // Wood and planks
                config.set("oak_log", "Bois de chêne");
                config.set("oak_wood", "Bois de chêne");
                config.set("birch_log", "Bois de bouleau");
                config.set("birch_wood", "Bois de bouleau");
                config.set("spruce_log", "Bois d'épicéa");
                config.set("spruce_wood", "Bois d'épicéa");
                config.set("jungle_log", "Bois d'acajou");
                config.set("jungle_wood", "Bois d'acajou");
                config.set("dark_oak_log", "Bois de chêne noir");
                config.set("dark_oak_wood", "Bois de chêne noir");
                config.set("acacia_log", "Bois d'acacia");
                config.set("acacia_wood", "Bois d'acacia");
                config.set("mangrove_log", "Bois de palétuvier");
                config.set("mangrove_wood", "Bois de palétuvier");
                config.set("cherry_log", "Bois de cerisier");
                config.set("cherry_wood", "Bois de cerisier");
                config.set("bamboo_block", "Bloc de bambou");

                config.set("oak_planks", "Planches de chêne");
                config.set("birch_planks", "Planches de bouleau");
                config.set("spruce_planks", "Planches d'épicéa");
                config.set("jungle_planks", "Planches d'acajou");
                config.set("dark_oak_planks", "Planches de chêne noir");
                config.set("acacia_planks", "Planches d'acacia");
                config.set("mangrove_planks", "Planches de palétuvier");
                config.set("cherry_planks", "Planches de cerisier");
                config.set("bamboo_planks", "Planches de bambou");

                // Wooden tools
                config.set("wooden_sword", "Épée en bois");
                config.set("wooden_pickaxe", "Pioche en bois");
                config.set("wooden_axe", "Hache en bois");
                config.set("wooden_shovel", "Pelle en bois");
                config.set("wooden_hoe", "Houe en bois");

                // Stone tools
                config.set("stone_sword", "Épée en pierre");
                config.set("stone_pickaxe", "Pioche en pierre");
                config.set("stone_axe", "Hache en pierre");
                config.set("stone_shovel", "Pelle en pierre");
                config.set("stone_hoe", "Houe en pierre");

                // Iron tools
                config.set("iron_sword", "Épée en fer");
                config.set("iron_pickaxe", "Pioche en fer");
                config.set("iron_axe", "Hache en fer");
                config.set("iron_shovel", "Pelle en fer");
                config.set("iron_hoe", "Houe en fer");

                // Gold tools
                config.set("golden_sword", "Épée en or");
                config.set("golden_pickaxe", "Pioche en or");
                config.set("golden_axe", "Hache en or");
                config.set("golden_shovel", "Pelle en or");
                config.set("golden_hoe", "Houe en or");

                // Diamond tools
                config.set("diamond_sword", "Épée en diamant");
                config.set("diamond_pickaxe", "Pioche en diamant");
                config.set("diamond_axe", "Hache en diamant");
                config.set("diamond_shovel", "Pelle en diamant");
                config.set("diamond_hoe", "Houe en diamant");

                // Netherite tools
                config.set("netherite_sword", "Épée en netherite");
                config.set("netherite_pickaxe", "Pioche en netherite");
                config.set("netherite_axe", "Hache en netherite");
                config.set("netherite_shovel", "Pelle en netherite");
                config.set("netherite_hoe", "Houe en netherite");

                // Leather armor
                config.set("leather_helmet", "Casque en cuir");
                config.set("leather_chestplate", "Plastron en cuir");
                config.set("leather_leggings", "Jambières en cuir");
                config.set("leather_boots", "Bottes en cuir");

                // Iron armor
                config.set("iron_helmet", "Casque en fer");
                config.set("iron_chestplate", "Plastron en fer");
                config.set("iron_leggings", "Jambières en fer");
                config.set("iron_boots", "Bottes en fer");

                // Gold armor
                config.set("golden_helmet", "Casque en or");
                config.set("golden_chestplate", "Plastron en or");
                config.set("golden_leggings", "Jambières en or");
                config.set("golden_boots", "Bottes en or");

                // Diamond armor
                config.set("diamond_helmet", "Casque en diamant");
                config.set("diamond_chestplate", "Plastron en diamant");
                config.set("diamond_leggings", "Jambières en diamant");
                config.set("diamond_boots", "Bottes en diamant");

                // Netherite armor
                config.set("netherite_helmet", "Casque en netherite");
                config.set("netherite_chestplate", "Plastron en netherite");
                config.set("netherite_leggings", "Jambières en netherite");
                config.set("netherite_boots", "Bottes en netherite");

                // Other tools
                config.set("bow", "Arc");
                config.set("crossbow", "Arbalète");
                config.set("trident", "Trident");
                config.set("fishing_rod", "Canne à pêche");
                config.set("flint_and_steel", "Briquet");
                config.set("shears", "Cisailles");
                config.set("shield", "Bouclier");

                // Food
                config.set("bread", "Pain");
                config.set("apple", "Pomme");
                config.set("golden_apple", "Pomme en or");
                config.set("enchanted_golden_apple", "Pomme en or enchantée");
                config.set("carrot", "Carotte");
                config.set("golden_carrot", "Carotte en or");
                config.set("potato", "Pomme de terre");
                config.set("baked_potato", "Pomme de terre cuite");
                config.set("beef", "Bœuf cru");
                config.set("cooked_beef", "Steak");
                config.set("porkchop", "Porc cru");
                config.set("cooked_porkchop", "Côtelette de porc cuite");
                config.set("chicken", "Poulet cru");
                config.set("cooked_chicken", "Poulet cuit");
                config.set("cod", "Morue crue");
                config.set("cooked_cod", "Morue cuite");
                config.set("salmon", "Saumon cru");
                config.set("cooked_salmon", "Saumon cuit");

                // Precious materials
                config.set("diamond", "Diamant");
                config.set("emerald", "Émeraude");
                config.set("gold_ingot", "Lingot d'or");
                config.set("iron_ingot", "Lingot de fer");
                config.set("copper_ingot", "Lingot de cuivre");
                config.set("netherite_ingot", "Lingot de netherite");
                config.set("coal", "Charbon");
                config.set("charcoal", "Charbon de bois");
                config.set("redstone", "Poudre de redstone");
                config.set("lapis_lazuli", "Lapis-lazuli");
                config.set("quartz", "Quartz du Nether");

                // Nether blocks
                config.set("netherrack", "Netherrack");
                config.set("nether_bricks", "Briques du Nether");
                config.set("red_nether_bricks", "Briques rouges du Nether");
                config.set("glowstone", "Pierre lumineuse");
                config.set("soul_sand", "Sable des âmes");
                config.set("soul_soil", "Terre des âmes");
                config.set("basalt", "Basalte");
                config.set("blackstone", "Roche noire");

                // End blocks
                config.set("end_stone", "Pierre de l'End");
                config.set("end_stone_bricks", "Briques de pierre de l'End");
                config.set("purpur_block", "Bloc de purpur");
                config.set("purpur_pillar", "Pilier de purpur");
                config.set("chorus_fruit", "Fruit de chorus");
                config.set("popped_chorus_fruit", "Fruit de chorus éclaté");

                // Other common blocks
                config.set("dirt", "Terre");
                config.set("grass_block", "Bloc d'herbe");
                config.set("sand", "Sable");
                config.set("gravel", "Gravier");
                config.set("glass", "Verre");
                config.set("obsidian", "Obsidienne");
                config.set("bedrock", "Bedrock");
                config.set("water", "Eau");
                config.set("lava", "Lave");
                config.set("ice", "Glace");
                config.set("snow", "Neige");
                config.set("clay", "Argile");
                config.set("brick", "Brique");
                config.set("tnt", "TNT");
                config.set("bookshelf", "Bibliothèque");
                config.set("crafting_table", "Table de craft");
                config.set("furnace", "Fourneau");
                config.set("chest", "Coffre");
                config.set("ender_chest", "Coffre de l'Ender");

                // Plants and agriculture
                config.set("wheat", "Blé");
                config.set("wheat_seeds", "Graines de blé");
                config.set("beetroot", "Betterave");
                config.set("beetroot_seeds", "Graines de betterave");
                config.set("melon", "Pastèque");
                config.set("pumpkin", "Citrouille");
                config.set("sugar_cane", "Canne à sucre");
                config.set("bamboo", "Bambou");
                config.set("kelp", "Varech");
                config.set("seagrass", "Herbes marines");

                // Flowers and decoration
                config.set("poppy", "Coquelicot");
                config.set("dandelion", "Pissenlit");
                config.set("blue_orchid", "Orchidée bleue");
                config.set("allium", "Allium");
                config.set("azure_bluet", "Houstonie bleue");
                config.set("red_tulip", "Tulipe rouge");
                config.set("orange_tulip", "Tulipe orange");
                config.set("white_tulip", "Tulipe blanche");
                config.set("pink_tulip", "Tulipe rose");
                config.set("oxeye_daisy", "Marguerite");
                config.set("cornflower", "Bleuet");
                config.set("lily_of_the_valley", "Muguet");
                config.set("sunflower", "Tournesol");
                config.set("lilac", "Lilas");
                config.set("rose_bush", "Rosier");
                config.set("peony", "Pivoine");

                // Entities/Mobs
                config.set("zombie", "Zombie");
                config.set("skeleton", "Squelette");
                config.set("spider", "Araignée");
                config.set("creeper", "Creeper");
                config.set("enderman", "Enderman");
                config.set("witch", "Sorcière");
                config.set("blaze", "Blaze");
                config.set("ghast", "Ghast");
                config.set("piglin", "Piglin");
                config.set("hoglin", "Hoglin");
                config.set("cow", "Vache");
                config.set("pig", "Cochon");
                config.set("sheep", "Mouton");
                config.set("chicken_entity", "Poulet");
                config.set("horse", "Cheval");
                config.set("wolf", "Loup");
                config.set("cat", "Chat");
                config.set("ocelot", "Ocelot");
                config.set("villager", "Villageois");
                config.set("iron_golem", "Golem de fer");
                config.set("ender_dragon", "Dragon de l'End");
                config.set("wither", "Wither");

            } catch (Exception e) {
                plugin.getLogger().severe("Error setting default material values: " + e.getMessage());
                throw new RuntimeException("Failed to set default materials", e);
            }

            // Save the file
            try {
                config.save(materialsFile);
                Utils.sendConsoleLog("&amaterials.yml file created with " + config.getKeys(false).size() + " entries");
            } catch (IOException e) {
                throw new IOException("Failed to save materials.yml: " + e.getMessage());
            }

        } catch (Exception e) {
            Utils.sendConsoleLog("&cError while creating materials.yml: " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "Error creating materials.yml", e);
            throw new RuntimeException("Could not create default materials file", e);
        }
    }

    /**
     * Obtains the French name of a material
     * @param material The English name of the material (ex: "stone", "diamond_sword")
     * @return The French name or a formatted version if not found
     */
    public String getMaterialName(String material) {
        try {
            if (material == null || material.trim().isEmpty()) {
                plugin.getLogger().warning("getMaterialName called with null or empty material");
                return "Unknown";
            }

            String key = material.toLowerCase().replace("minecraft:", "");

            // Search in the loaded materials map
            String frenchName = materialNames.get(key);
            if (frenchName != null && !frenchName.trim().isEmpty()) {
                return frenchName;
            }

            // If not found, auto-format
            return formatMaterialName(material);

        } catch (Exception e) {
            plugin.getLogger().warning("Error getting material name for '" + material + "': " + e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Automatically formats a material name
     * @param material The name of the material to format
     * @return The formatted name
     */
    private String formatMaterialName(String material) {
        try {
            if (material == null || material.trim().isEmpty()) {
                return "Inconnu";
            }

            // Clean the name
            String formatted = material.toLowerCase()
                .replace("minecraft:", "")
                .replace("_", " ");

            // Capitalize each word
            String[] words = formatted.split(" ");
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < words.length; i++) {
                if (i > 0) result.append(" ");
                if (!words[i].isEmpty()) {
                    result.append(Character.toUpperCase(words[i].charAt(0)));
                    if (words[i].length() > 1) result.append(words[i].substring(1));
                }
            }

            return result.toString();

        } catch (Exception e) {
            plugin.getLogger().warning("Error formatting material name '" + material + "': " + e.getMessage());
            return "Matériau Inconnu";
        }
    }

    /**
     * Reloads materials from the file
     */
    public void reload() {
        try {
            materialNames.clear();
            loadMaterialNames();
            Utils.sendConsoleLog("&aMaterialTranslator reloaded successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading MaterialTranslator: " + e.getMessage());
            Utils.sendConsoleLog("&cFailed to reload MaterialTranslator: " + e.getMessage());
        }
    }

    /**
     * Gets the number of loaded materials
     */
    public int getLoadedMaterialsCount() {
        try {
            return materialNames.size();
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting loaded materials count: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Checks if a material is known
     */
    public boolean hasMaterial(String material) {
        try {
            if (material == null) return false;
            return materialNames.containsKey(material.toLowerCase().replace("minecraft:", ""));
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking if material exists '" + material + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds or updates a material
     */
    public void addMaterial(String material, String frenchName) {
        try {
            if (material != null && frenchName != null && !material.trim().isEmpty() && !frenchName.trim().isEmpty()) {
                materialNames.put(material.toLowerCase().replace("minecraft:", ""), frenchName);
            } else {
                plugin.getLogger().warning("Attempted to add invalid material: " + material + " -> " + frenchName);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error adding material '" + material + "': " + e.getMessage());
        }
    }

    /**
     * Gets statistics about materials
     */
    public String getStats() {
        try {
            return "&aMaterialTranslator: &f" + materialNames.size() + " materials loaded";
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting MaterialTranslator stats: " + e.getMessage());
            return "&cMaterialTranslator: &fError getting stats";
        }
    }
}
