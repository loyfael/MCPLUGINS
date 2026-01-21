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
import java.util.Optional;

/**
 * GUI handler for the levels interface
 * Single responsibility principle: only handles levels interface management
 */
public class LevelsGuiHandler implements IGuiHandler {

    private final IPlayerService playerService;
    private final INotificationService notificationService;

    public LevelsGuiHandler(IPlayerService playerService, INotificationService notificationService) {
        this.playerService = playerService;
        this.notificationService = notificationService;
    }

    @Override
    public Inventory createInventory(Player player, Map<String, Object> parameters) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§6§lNiveaux - " + player.getName());

        // Récupérer les données du joueur
        String playerUuid = player.getUniqueId().toString();
        Optional<IPlayerService.PlayerData> playerDataOpt = playerService.getPlayerData(playerUuid);

        if (playerDataOpt.isEmpty()) {
            // Créer un nouveau profil si nécessaire
            playerService.createPlayer(playerUuid, player.getName());
            playerDataOpt = playerService.getPlayerData(playerUuid);
        }

        IPlayerService.PlayerData playerData = playerDataOpt.get();

        // Remplir l'inventaire
        fillLevelsInventory(inventory, playerData);

        return inventory;
    }

    @Override
    public void handleClick(Player player, ItemStack clickedItem, int slot) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Gérer les clics sur les niveaux
        String playerUuid = player.getUniqueId().toString();
        int currentLevel = playerService.getPlayerLevel(playerUuid);
        int targetLevel = slot + 1; // Le slot correspond au niveau - 1

        if (targetLevel <= currentLevel) {
            // Niveau déjà atteint - afficher les récompenses
            player.sendMessage("§a✓ Niveau " + targetLevel + " déjà atteint !");
        } else if (targetLevel == currentLevel + 1) {
            // Prochain niveau - permettre l'upgrade ou afficher les requis
            player.sendMessage("§e→ Niveau " + targetLevel + " - Prochain objectif");
        } else {
            // Niveau trop élevé
            player.sendMessage("§cNiveau " + targetLevel + " non accessible pour le moment");
        }
    }

    @Override
    public void updateContent(Player player, Inventory inventory) {
        // Mettre à jour l'inventaire avec les nouveaux niveaux
        String playerUuid = player.getUniqueId().toString();
        int currentLevel = playerService.getPlayerLevel(playerUuid);

        // Remplir avec les niveaux mis à jour
        for (int level = 1; level <= getMaxLevel() && level <= inventory.getSize(); level++) {
            ItemStack levelItem = createLevelItem(level, currentLevel, playerUuid);
            inventory.setItem(level - 1, levelItem);
        }
    }

    /**
     * Remplit l'inventaire avec les informations de niveaux
     */
    private void fillLevelsInventory(Inventory inventory, IPlayerService.PlayerData playerData) {
        // Header - Info du joueur
        ItemStack playerInfo = createPlayerInfoItem(playerData);
        inventory.setItem(4, playerInfo);

        // Bordures décoratives
        ItemStack border = createBorderItem();
        for (int i = 0; i < 9; i++) {
            if (i != 4) inventory.setItem(i, border);
        }
        for (int i = 45; i < 54; i++) {
            if (i != 48 && i != 49 && i != 50) inventory.setItem(i, border);
        }

        // Niveaux (slots 9-44, soit 36 niveaux par page)
        int currentLevel = playerData.getLevel();
        int startLevel = 1; // TODO: Calculer selon la page courante

        for (int i = 0; i < 36; i++) {
            int level = startLevel + i;
            int slot = 9 + i;

            ItemStack levelItem = createLevelItem(level, currentLevel, playerData);
            inventory.setItem(slot, levelItem);
        }

        // Navigation
        inventory.setItem(48, createNavigationItem("§7« Page précédente", Material.ARROW));
        inventory.setItem(49, createNavigationItem("§c§lFermer", Material.BARRIER));
        inventory.setItem(50, createNavigationItem("§7Page suivante »", Material.ARROW));
    }

    /**
     * Crée l'item d'information du joueur
     */
    private ItemStack createPlayerInfoItem(IPlayerService.PlayerData playerData) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6§l" + playerData.getName());

            List<String> lore = Arrays.asList(
                "§7Niveau actuel: §e" + playerData.getLevel(),
                "§7Dernière connexion: §f" + formatLastSeen(playerData.getLastSeen()),
                "",
                "§7Cliquez pour voir les statistiques détaillées"
            );
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Récupère le nombre maximum de niveaux
     */
    private int getMaxLevel() {
        // TODO: Récupérer depuis le service LevelsConfigService
        // Pour l'instant, retourner une valeur fixe
        return 50;
    }

    /**
     * Crée un item de niveau avec les données du joueur
     */
    private ItemStack createLevelItem(int level, int currentLevel, String playerUuid) {
        // Utiliser la méthode existante avec les données du joueur
        Optional<IPlayerService.PlayerData> playerDataOpt = playerService.getPlayerData(playerUuid);
        if (playerDataOpt.isEmpty()) {
            return new ItemStack(Material.BARRIER);
        }

        return createLevelItem(level, currentLevel, playerDataOpt.get());
    }

    /**
     * Crée un item de niveau avec le nouveau format demandé
     */
    private ItemStack createLevelItem(int level, int currentLevel, IPlayerService.PlayerData playerData) {
        Material material;
        String statusText;
        String statusColor;
        List<String> lore = new ArrayList<>();

        // Déterminer le statut du niveau
        if (level <= currentLevel) {
            // Niveau déjà obtenu
            material = Material.LIME_STAINED_GLASS;
            statusText = "Débloqué";
            statusColor = "§a";

            lore.add("§7Statut: " + statusColor + "✓ " + statusText);
            lore.add("§7Tâche: " + getTaskDescription(level));
            lore.add("§7Récompense: §aVoir les détails dans le menu");

        } else if (level == currentLevel + 1) {
            // Niveau suivant (en cours)
            material = Material.YELLOW_STAINED_GLASS;
            statusText = "En cours";
            statusColor = "§e";

            lore.add("§7Statut: " + statusColor + "⚡ " + statusText);
            lore.add("§7Tâche: " + getTaskDescription(level));
            lore.add("§7Récompense: §eVoir les détails dans le menu");

        } else {
            // Niveaux futurs (cachés comme demandé)
            material = Material.RED_STAINED_GLASS;
            statusText = "Verrouillé";
            statusColor = "§c";

            lore.add("§7Statut: " + statusColor + "✗ " + statusText);
            lore.add("§7Tâche: §8???");
            lore.add("§7Récompense: §8???");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§f§lNiveau " + level + " §8(" + statusText + ")");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Récupère la description de la tâche pour un niveau donné
     */
    private String getTaskDescription(int level) {
        // TODO: Récupérer depuis le fichier de configuration levels.yml
        // Pour l'instant, exemples temporaires
        return switch (level) {
            case 1 -> "§eCasser 50 blocs de pierre";
            case 2 -> "§eTuer 20 zombies";
            case 3 -> "§ePêcher 30 poissons";
            case 4 -> "§eDonner 1000◎ au serveur";
            case 5 -> "§eCasser 100 blocs de fer";
            default -> "§eComplèter une mission spéciale";
        };
    }


    /**
     * Crée un item de bordure décorative
     */
    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
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
     * Gère le clic sur un niveau spécifique
     */
    private void handleLevelClick(Player player, int slot, ItemStack item) {
        int level = (slot - 9) + 1; // Calcul du niveau basé sur le slot
        String playerUuid = player.getUniqueId().toString();
        int currentLevel = playerService.getPlayerLevel(playerUuid);

        if (level <= currentLevel) {
            notificationService.sendMessage(player, "gui.level.already-unlocked", level);
        } else {
            notificationService.sendMessage(player, "gui.level.locked", level, (level - currentLevel));
        }
    }

    /**
     * Affiche les statistiques détaillées du joueur
     */
    private void showPlayerStats(Player player) {
        String playerUuid = player.getUniqueId().toString();
        Optional<IPlayerService.PlayerData> playerDataOpt = playerService.getPlayerData(playerUuid);

        if (playerDataOpt.isPresent()) {
            IPlayerService.PlayerData data = playerDataOpt.get();
            notificationService.sendMessage(player, "gui.stats.header", data.getName());
            notificationService.sendMessage(player, "gui.stats.level", data.getLevel());
            notificationService.sendMessage(player, "gui.stats.last-seen", formatLastSeen(data.getLastSeen()));
        }
    }

    /**
     * Formate la date de dernière connexion
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
