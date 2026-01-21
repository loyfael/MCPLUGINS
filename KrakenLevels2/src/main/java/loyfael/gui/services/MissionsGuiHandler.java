package loyfael.gui.services;

import loyfael.api.interfaces.IGuiHandler;
import loyfael.api.interfaces.IPlayerService;
import loyfael.api.interfaces.INotificationService;
import loyfael.api.interfaces.IMissionService;
import loyfael.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * GUI handler for the missions interface
 * Single responsibility principle: only handles missions interface management
 */
public class MissionsGuiHandler implements IGuiHandler {

    private final IPlayerService playerService;
    private final INotificationService notificationService;

    public MissionsGuiHandler(IPlayerService playerService, INotificationService notificationService) {
        this.playerService = playerService;
        this.notificationService = notificationService;
    }

    @Override
    public Inventory createInventory(Player player, Map<String, Object> parameters) {
        try {
            if (player == null) {
                throw new IllegalArgumentException("Player cannot be null");
            }

            Inventory inventory = Bukkit.createInventory(null, 45, "§5§lMissions - " + player.getName());

            // Get mission service with null check
            IMissionService missionService = Main.getInstance().getMissionService();
            if (missionService == null) {
                player.sendMessage("§cErreur: Service de missions non disponible");
                return inventory; // Return empty inventory
            }

            String playerUuid = player.getUniqueId().toString();

            // Get player missions with error handling
            List<IMissionService.Mission> playerMissions;
            List<IMissionService.Mission> availableMissions;

            try {
                playerMissions = missionService.getPlayerMissions(playerUuid);
                availableMissions = missionService.getAvailableMissions();

                if (playerMissions == null) playerMissions = new ArrayList<>();
                if (availableMissions == null) availableMissions = new ArrayList<>();

            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Error getting missions for player " + player.getName() + ": " + e.getMessage());
                playerMissions = new ArrayList<>();
                availableMissions = new ArrayList<>();
            }

            // Fill inventory with error handling
            try {
                fillMissionsInventory(inventory, playerMissions, availableMissions);
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Error filling missions inventory for " + player.getName() + ": " + e.getMessage());
                // Continue with empty inventory rather than crashing
            }

            return inventory;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Critical error creating missions inventory: " + e.getMessage());
            e.printStackTrace();

            // Return a basic error inventory
            try {
                Inventory errorInventory = Bukkit.createInventory(null, 9, "§c§lErreur - Missions");
                // Add error indicator item
                ItemStack errorItem = new ItemStack(Material.BARRIER);
                ItemMeta meta = errorItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§c§lErreur");
                    meta.setLore(Arrays.asList("§7Une erreur s'est produite", "§7lors du chargement des missions"));
                    errorItem.setItemMeta(meta);
                }
                errorInventory.setItem(4, errorItem);
                return errorInventory;
            } catch (Exception fallbackError) {
                Main.getInstance().getLogger().severe("Failed to create error inventory: " + fallbackError.getMessage());
                return null;
            }
        }
    }

    @Override
    public void handleClick(Player player, ItemStack clickedItem, int slot) {
        try {
            if (player == null) {
                Main.getInstance().getLogger().warning("handleClick called with null player");
                return;
            }

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return; // Valid case, no action needed
            }

            // Handle mission clicks with bounds checking
            if (slot >= 9 && slot < 45) {
                try {
                    int missionIndex = slot - 9;
                    IMissionService missionService = Main.getInstance().getMissionService();

                    if (missionService == null) {
                        player.sendMessage("§cErreur: Service de missions non disponible");
                        return;
                    }

                    List<IMissionService.Mission> availableMissions = missionService.getAvailableMissions();
                    if (availableMissions == null || missionIndex >= availableMissions.size()) {
                        player.sendMessage("§cErreur: Mission non trouvée");
                        return;
                    }

                    IMissionService.Mission mission = availableMissions.get(missionIndex);
                    if (mission != null) {
                        handleMissionClick(player, mission);
                    }

                } catch (IndexOutOfBoundsException e) {
                    Main.getInstance().getLogger().warning("Mission index out of bounds for player " + player.getName() + ": " + e.getMessage());
                    player.sendMessage("§cErreur: Mission non accessible");
                } catch (Exception e) {
                    Main.getInstance().getLogger().warning("Error handling mission click for " + player.getName() + ": " + e.getMessage());
                    player.sendMessage("§cErreur lors de l'interaction avec la mission");
                }
            }

            // Navigation with error handling
            if (slot == 49) { // Close button
                try {
                    player.closeInventory();
                } catch (Exception e) {
                    Main.getInstance().getLogger().warning("Error closing inventory for " + player.getName() + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Critical error in handleClick for missions GUI: " + e.getMessage());
            e.printStackTrace();

            try {
                player.sendMessage("§cErreur critique dans l'interface des missions");
                player.closeInventory();
            } catch (Exception fallbackError) {
                Main.getInstance().getLogger().severe("Failed to send error message to player: " + fallbackError.getMessage());
            }
        }
    }

    @Override
    public void updateContent(Player player, Inventory inventory) {
        try {
            if (player == null || inventory == null) {
                Main.getInstance().getLogger().warning("updateContent called with null parameter");
                return;
            }

            // Get services with null checks
            String playerUuid = player.getUniqueId().toString();
            IMissionService missionService = Main.getInstance().getMissionService();

            if (missionService == null) {
                Main.getInstance().getLogger().warning("Mission service not available for update");
                return;
            }

            List<IMissionService.Mission> availableMissions;
            List<IMissionService.Mission> playerMissions;

            try {
                availableMissions = missionService.getAvailableMissions();
                playerMissions = missionService.getPlayerMissions(playerUuid);

                if (availableMissions == null) availableMissions = new ArrayList<>();
                if (playerMissions == null) playerMissions = new ArrayList<>();

            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Error getting missions during update: " + e.getMessage());
                return;
            }

            // Clear and update inventory
            try {
                inventory.clear();

                // Fill with updated missions
                for (int i = 0; i < Math.min(availableMissions.size(), 36); i++) {
                    try {
                        IMissionService.Mission mission = availableMissions.get(i);
                        if (mission == null) continue;

                        // Check if this mission is active for the player
                        boolean isActive = playerMissions.stream()
                            .anyMatch(pm -> pm != null && pm.getId() != null && pm.getId().equals(mission.getId()));

                        ItemStack missionItem = createMissionItem(mission, isActive);
                        if (missionItem != null) {
                            inventory.setItem(9 + i, missionItem);
                        }
                    } catch (Exception e) {
                        Main.getInstance().getLogger().warning("Error creating mission item " + i + ": " + e.getMessage());
                        // Continue with other missions
                    }
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Error updating mission inventory content: " + e.getMessage());
            }

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Critical error updating missions content: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Remplit l'inventaire avec les missions
     */
    private void fillMissionsInventory(Inventory inventory, List<IMissionService.Mission> playerMissions,
                                     List<IMissionService.Mission> availableMissions) {

        // Nettoyer l'inventaire
        inventory.clear();

        // Header - Info des missions
        ItemStack missionInfo = createMissionInfoItem(playerMissions);
        inventory.setItem(4, missionInfo);

        // Bordures décoratives
        ItemStack border = createBorderItem();
        for (int i = 0; i < 9; i++) {
            if (i != 4) inventory.setItem(i, border);
        }
        for (int i = 36; i < 45; i++) {
            if (i != 39 && i != 40) inventory.setItem(i, border);
        }

        // Missions actives du joueur (slots 9-35, soit 27 missions max)
        int slot = 9;

        // Missions en cours
        for (IMissionService.Mission mission : playerMissions) {
            if (slot >= 36) break;
            inventory.setItem(slot++, createMissionItem(mission, true));
        }

        // Remplir avec des missions disponibles si nécessaire
        for (IMissionService.Mission mission : availableMissions) {
            if (slot >= 36) break;

            // Vérifier si la mission n'est pas déjà active
            boolean isActive = playerMissions.stream()
                .anyMatch(pm -> pm.getType().equals(mission.getType()));

            if (!isActive) {
                inventory.setItem(slot++, createMissionItem(mission, false));
            }
        }

        // Navigation
        inventory.setItem(39, createNavigationItem("§e§lActualiser", Material.LIME_DYE));
        inventory.setItem(40, createNavigationItem("§c§lFermer", Material.BARRIER));
    }

    /**
     * Crée l'item d'information des missions
     */
    private ItemStack createMissionInfoItem(List<IMissionService.Mission> playerMissions) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§5§lMes Missions");

            long completedMissions = playerMissions.stream().mapToLong(m -> m.isCompleted() ? 1 : 0).sum();
            long activeMissions = playerMissions.size() - completedMissions;

            List<String> lore = Arrays.asList(
                "§7Missions actives: §e" + activeMissions,
                "§7Missions complétées: §a" + completedMissions,
                "§7Total: §f" + playerMissions.size(),
                "",
                "§7Complétez des missions pour gagner",
                "§7de l'expérience et des récompenses !"
            );
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Crée un item de mission
     */
    private ItemStack createMissionItem(IMissionService.Mission mission, boolean isActive) {
        Material material = getMissionMaterial(mission.getType());
        String displayName;
        List<String> lore = new ArrayList<>();

        if (isActive) {
            if (mission.isCompleted()) {
                displayName = "§a§l" + mission.getDescription() + " §8(Complétée)";
                lore.add("§7Statut: §aComplétée ✓");
            } else {
                displayName = "§e§l" + mission.getDescription();
                lore.add("§7Statut: §eEn cours...");
                lore.add("§7Progrès: §f" + mission.getCurrentProgress() + "§8/§f" + mission.getTargetAmount());

                // Barre de progression
                int percentage = (mission.getCurrentProgress() * 100) / mission.getTargetAmount();
                lore.add("§7[§a" + "█".repeat(Math.min(10, percentage / 10)) +
                        "§8" + "█".repeat(10 - Math.min(10, percentage / 10)) + "§7] §f" + percentage + "%");
            }
        } else {
            displayName = "§7" + mission.getDescription() + " §8(Disponible)";
            lore.add("§7Statut: §7Disponible");
            lore.add("§7Objectif: §f" + mission.getTargetAmount());
        }

        lore.add("");
        lore.add("§7Récompenses:");
        mission.getRewards().forEach((key, value) -> {
            switch (key) {
                case "experience":
                    lore.add("§8• §bExpérience: §f" + value + " XP");
                    break;
                case "levels":
                    lore.add("§8• §eNiveaux: §f+" + value);
                    break;
                default:
                    lore.add("§8• §f" + key + ": §f" + value);
                    break;
            }
        });

        if (isActive && mission.isCompleted()) {
            lore.add("");
            lore.add("§a§lCliquez pour récupérer les récompenses !");
        } else if (!isActive) {
            lore.add("");
            lore.add("§7§lCliquez pour démarrer cette mission");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);

            // Effet d'enchantement pour les missions complétées
            if (isActive && mission.isCompleted()) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Détermine le matériau approprié selon le type de mission
     */
    private Material getMissionMaterial(String missionType) {
        return switch (missionType) {
            case "BREAK" -> Material.DIAMOND_PICKAXE;
            case "PLACE" -> Material.BRICKS; // Correction: BUILDING_BLOCKS n'existe pas
            case "FISHING" -> Material.FISHING_ROD;
            case "KILL" -> Material.DIAMOND_SWORD;
            default -> Material.BOOK;
        };
    }

    /**
     * Crée un item de bordure
     */
    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Crée un item de navigation
     */
    private ItemStack createNavigationItem(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Gère le clic sur une mission
     */
    private void handleMissionClick(Player player, IMissionService.Mission mission) {
        // TODO: Implémenter la logique de récupération des récompenses
        // et de démarrage des nouvelles missions
        notificationService.sendMessage(player, "gui.missions.click-handled");
    }

    /**
     * Affiche les statistiques des missions
     */
    private void showMissionStats(Player player) {
        IMissionService missionService = Main.getInstance().getMissionService();
        String playerUuid = player.getUniqueId().toString();
        List<IMissionService.Mission> missions = missionService.getPlayerMissions(playerUuid);

        long completed = missions.stream().mapToLong(m -> m.isCompleted() ? 1 : 0).sum();
        long active = missions.size() - completed;

        notificationService.sendMessage(player, "gui.missions.stats.header");
        notificationService.sendMessage(player, "gui.missions.stats.active", active);
        notificationService.sendMessage(player, "gui.missions.stats.completed", completed);
        notificationService.sendMessage(player, "gui.missions.stats.total", missions.size());
    }
}
