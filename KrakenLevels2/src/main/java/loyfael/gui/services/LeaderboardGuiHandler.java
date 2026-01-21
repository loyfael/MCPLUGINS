package loyfael.gui.services;

import loyfael.api.interfaces.IGuiHandler;
import loyfael.api.interfaces.IPlayerService;
import loyfael.api.interfaces.INotificationService;
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
 * GUI handler for the leaderboard interface
 * Single responsibility principle: only handles leaderboard interface management
 */
public class LeaderboardGuiHandler implements IGuiHandler {

    private final IPlayerService playerService;
    private final INotificationService notificationService;
    private List<IPlayerService.PlayerData> topPlayers; // Missing variable addition

    public LeaderboardGuiHandler(IPlayerService playerService, INotificationService notificationService) {
        this.playerService = playerService;
        this.notificationService = notificationService;
        this.topPlayers = new ArrayList<>(); // Initialization
    }

    @Override
    public Inventory createInventory(Player player, Map<String, Object> parameters) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§b§lClassement - Top Joueurs");

        // Retrieve top 45 players
        List<IPlayerService.PlayerData> topPlayers = playerService.getTopPlayers(45);

        fillLeaderboardInventory(inventory, topPlayers, player);

        return inventory;
    }

    @Override
    public void handleClick(Player player, ItemStack clickedItem, int slot) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Logic for handling clicks on the leaderboard
        // For example, display more details about the clicked player

        if (slot >= 0 && slot < topPlayers.size()) {
            IPlayerService.PlayerData playerData = topPlayers.get(slot);
            player.sendMessage("§6Détails de " + playerData.getName() + ":");
            player.sendMessage("§7Niveau: §e" + playerData.getLevel());
            player.sendMessage("§7Boutons: §e" + playerData.getButtonAmount());
        }
    }

    @Override
    public void updateContent(Player player, Inventory inventory) {
        // Reload leaderboard data
        topPlayers = playerService.getTopPlayers(45); // Max 5 inventory rows

        inventory.clear();

        for (int i = 0; i < topPlayers.size() && i < inventory.getSize(); i++) {
            IPlayerService.PlayerData playerData = topPlayers.get(i);

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = playerHead.getItemMeta();

            if (meta != null) {
                meta.setDisplayName("§6#" + (i + 1) + " §e" + playerData.getName());
                meta.setLore(Arrays.asList(
                    "§7Niveau: §e" + playerData.getLevel(),
                    "§7Boutons: §e" + playerData.getButtonAmount(),
                    "",
                    "§eCliquez pour plus de détails"
                ));
                playerHead.setItemMeta(meta);
            }

            inventory.setItem(i, playerHead);
        }
    }

    /**
     * Fills the inventory with the leaderboard
     */
    private void fillLeaderboardInventory(Inventory inventory, List<IPlayerService.PlayerData> topPlayers, Player viewer) {
        // Clear the inventory
        inventory.clear();

        // Header - Leaderboard info
        ItemStack leaderboardInfo = createLeaderboardInfoItem(topPlayers.size());
        inventory.setItem(4, leaderboardInfo);

        // Decorative borders
        ItemStack border = createBorderItem();
        for (int i = 0; i < 9; i++) {
            if (i != 4) inventory.setItem(i, border);
        }
        for (int i = 45; i < 54; i++) {
            if (i != 48 && i != 49) inventory.setItem(i, border);
        }

        // Ranked players (slots 9-44, i.e. max 36 players per page)
        for (int i = 0; i < Math.min(36, topPlayers.size()); i++) {
            IPlayerService.PlayerData playerData = topPlayers.get(i);
            int rank = i + 1;
            int slot = 9 + i;

            boolean isViewer = playerData.getUuid().equals(viewer.getUniqueId().toString());
            ItemStack playerItem = createPlayerRankItem(playerData, rank, isViewer);
            inventory.setItem(slot, playerItem);
        }

        // Navigation
        inventory.setItem(48, createNavigationItem("§e§lActualiser", Material.LIME_DYE));
        inventory.setItem(49, createNavigationItem("§c§lFermer", Material.BARRIER));
    }

    /**
     * Creates the leaderboard info item
     */
    private ItemStack createLeaderboardInfoItem(int totalPlayers) {
        ItemStack item = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§b§lClassement Global");

            List<String> lore = Arrays.asList(
                "§7Joueurs classés: §e" + totalPlayers,
                "§7Mise à jour: §fEn temps réel",
                "",
                "§7Classement basé sur:",
                "§8• §7Niveau atteint",
                "§8• §7Progression générale",
                "",
                "§7§oLes meilleurs joueurs sont",
                "§7§orécompensés régulièrement !"
            );
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates an item representing a player in the leaderboard
     */
    private ItemStack createPlayerRankItem(IPlayerService.PlayerData playerData, int rank, boolean isViewer) {
        Material material = getRankMaterial(rank);
        String displayName;

        if (isViewer) {
            displayName = "§e§l#" + rank + " " + playerData.getName() + " §7(Vous)";
        } else {
            displayName = getRankColor(rank) + "§l#" + rank + " " + playerData.getName();
        }

        List<String> lore = Arrays.asList(
            "§7Position: " + getRankColor(rank) + "#" + rank,
            "§7Niveau: §e" + playerData.getLevel(),
            "§7Boutons: §a" + playerData.getButtonAmount(),
            "§7Dernière connexion: §f" + formatLastSeen(playerData.getLastSeen()),
            "",
            getRankDescription(rank),
            "",
            "§7Cliquez pour voir le profil détaillé"
        );

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);

            // Special effect for top 3
            if (rank <= 3) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            // Special effect if it's the viewing player
            if (isViewer) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Determines the material based on the rank
     */
    private Material getRankMaterial(int rank) {
        return switch (rank) {
            case 1 -> Material.GOLDEN_HELMET; // 1st place
            case 2 -> Material.IRON_HELMET;   // 2nd place
            case 3 -> Material.LEATHER_HELMET; // 3rd place
            default -> {
                if (rank <= 10) yield Material.PLAYER_HEAD; // Top 10
                yield Material.SKELETON_SKULL; // Others
            }
        };
    }

    /**
     * Determines the color based on the rank
     */
    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "§6"; // Gold
            case 2 -> "§7"; // Silver
            case 3 -> "§c"; // Bronze
            default -> {
                if (rank <= 10) yield "§e"; // Yellow for top 10
                yield "§f"; // White for others
            }
        };
    }

    /**
     * Special description based on the rank
     */
    private String getRankDescription(int rank) {
        return switch (rank) {
            case 1 -> "§6§l★ CHAMPION LÉGENDAIRE ★";
            case 2 -> "§7§l✦ MAÎTRE SUPRÊME ✦";
            case 3 -> "§c§l▲ EXPERT CONFIRMÉ ▲";
            default -> {
                if (rank <= 10) yield "§e§l◆ TOP 10 ◆";
                if (rank <= 50) yield "§a§l♦ JOUEUR EXPÉRIMENTÉ ♦";
                yield "§f§l• AVENTURIER •";
            }
        };
    }

    /**
     * Creates a border item
     */
    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates a navigation item
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
     * Handles the click on a player in the leaderboard
     */
    private void handlePlayerClick(Player viewer, int slot, ItemStack item) {
        // Calculate the rank based on the slot
        int rank = (slot - 9) + 1;
        notificationService.sendMessage(viewer, "gui.leaderboard.player-info", rank);
    }

    /**
     * Displays the leaderboard information
     */
    private void showLeaderboardInfo(Player player) {
        List<IPlayerService.PlayerData> topPlayers = playerService.getTopPlayers(50);

        notificationService.sendMessage(player, "gui.leaderboard.info.header");
        notificationService.sendMessage(player, "gui.leaderboard.info.total-players", topPlayers.size());

        if (!topPlayers.isEmpty()) {
            IPlayerService.PlayerData topPlayer = topPlayers.get(0);
            notificationService.sendMessage(player, "gui.leaderboard.info.top-player",
                topPlayer.getName(), topPlayer.getLevel());
        }

        // Find the player's position
        String playerUuid = player.getUniqueId().toString();
        for (int i = 0; i < topPlayers.size(); i++) {
            if (topPlayers.get(i).getUuid().equals(playerUuid)) {
                notificationService.sendMessage(player, "gui.leaderboard.info.your-rank", (i + 1));
                break;
            }
        }
    }

    /**
     * Formats the last seen date
     */
    private String formatLastSeen(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (60 * 1000);
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " jour(s)";
        } else if (hours > 0) {
            return hours + " heure(s)";
        } else if (minutes > 0) {
            return minutes + " minute(s)";
        } else {
            return "maintenant";
        }
    }
}
