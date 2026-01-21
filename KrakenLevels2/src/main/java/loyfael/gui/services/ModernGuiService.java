package loyfael.gui.services;

import loyfael.api.interfaces.IGuiService;
import loyfael.api.interfaces.IGuiHandler;
import loyfael.api.interfaces.IPlayerService;
import loyfael.api.interfaces.INotificationService;
import loyfael.api.interfaces.ILevelsConfigService;
import loyfael.core.services.LevelsConfigService;
import loyfael.Main;
import loyfael.utils.RewardExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service GUI moderne utilisant totalement levels.yml
 * Responsabilité unique : gestion des interfaces graphiques
 * Principe ouvert/fermé : extensible pour nouveaux types de GUI
 */
@SuppressWarnings("deprecation")
public class ModernGuiService implements IGuiService {

    private final IPlayerService playerService;
    private final INotificationService notificationService;
    private final ILevelsConfigService levelsConfigService;
    private final Map<String, IGuiHandler> guiHandlers = new ConcurrentHashMap<>();
    private final Map<Player, String> activeGuis = new ConcurrentHashMap<>();

    public ModernGuiService(IPlayerService playerService, INotificationService notificationService,
                           ILevelsConfigService levelsConfigService) {
        this.playerService = playerService;
        this.notificationService = notificationService;
        this.levelsConfigService = levelsConfigService;
        registerDefaultHandlers();
    }

        @Override
    public void openGui(Player player, String guiType, Map<String, Object> parameters) {
        if (player == null || guiType == null) return;

        // Nettoyer d'abord toute GUI existante pour éviter les conflits
        if (activeGuis.containsKey(player)) {
            cleanupPlayerGui(player);
        }

        IGuiHandler handler = guiHandlers.get(guiType);
        if (handler == null) {
            notificationService.sendMessage(player, "gui.type-not-found", guiType);
            return;
        }

        try {
            // S'assurer que l'opération s'exécute sur le thread principal
            Runnable openGuiTask = () -> {
                try {
                    Inventory inventory = handler.createInventory(player, parameters);
                    player.openInventory(inventory);
                    activeGuis.put(player, guiType);
                } catch (Exception e) {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    Main.getInstance().getLogger().warning("Erreur lors de l'ouverture de la GUI " + guiType + " pour " + player.getName() + ": " + errorMessage);
                    notificationService.sendMessage(player, "gui.error", errorMessage);
                    
                    // Nettoyer en cas d'erreur
                    cleanupPlayerGui(player);
                }
            };

            // Exécuter sur le thread principal si on n'y est pas déjà
            if (Bukkit.isPrimaryThread()) {
                openGuiTask.run();
            } else {
                Bukkit.getScheduler().runTask(Main.getInstance(), openGuiTask);
            }

        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            Main.getInstance().getLogger().warning("Erreur critique lors de l'ouverture de la GUI " + guiType + " pour " + player.getName() + ": " + errorMessage);
            notificationService.sendMessage(player, "gui.error", errorMessage);
            cleanupPlayerGui(player);
        }
    }

    /**
     * Crée l'inventaire principal des niveaux basé sur levels.yml avec pagination
     */
    public Inventory createLevelsInventory(Player player) {
        return createLevelsInventory(player, 0); // Page 0 par défaut
    }

    /**
     * Crée l'inventaire des niveaux avec pagination
     */
    public Inventory createLevelsInventory(Player player, int page) {
        int totalLevels = levelsConfigService.getTotalLevels();
        int levelsPerPage = 28; // 4 lignes de 7 items (laisse place pour navigation)
        int totalPages = (totalLevels - 1) / levelsPerPage + 1;

        // Limiter la page
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inventory = Bukkit.createInventory(null, 54,
            "§8§lMissions §0- §7Page " + (page + 1) + "/" + totalPages);

        String playerUuid = player.getUniqueId().toString();
        int currentPlayerLevel = playerService.getPlayerLevel(playerUuid);

        // Remplir avec les bordures décoratives
        fillBorders(inventory);

        // Calculer les niveaux à afficher sur cette page
        List<LevelsConfigService.LevelConfig> sortedLevels = levelsConfigService.getSortedLevels();
        int startIndex = page * levelsPerPage;
        int endIndex = Math.min(startIndex + levelsPerPage, sortedLevels.size());

        // Remplir les niveaux (slots 10-16, 19-25, 28-34, 37-43)
        int[] availableSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
                               28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex && slotIndex < availableSlots.length; i++, slotIndex++) {
            LevelsConfigService.LevelConfig levelConfig = sortedLevels.get(i);
            ItemStack levelItem = createLevelItem(levelConfig, currentPlayerLevel, playerUuid);
            inventory.setItem(availableSlots[slotIndex], levelItem);
        }

        // Ajouter les boutons de navigation
        addNavigationButtons(inventory, page, totalPages);

        return inventory;
    }

    /**
     * Remplit les bordures de l'inventaire
     */
    private void fillBorders(Inventory inventory) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }

        // Bordures haut et bas
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }

        // Bordures côtés
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }

    /**
     * Ajoute les boutons de navigation
     */
    private void addNavigationButtons(Inventory inventory, int currentPage, int totalPages) {
        // Bouton page précédente
        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§a§l← Page Précédente");
                prevMeta.setLore(Arrays.asList("§7Cliquez pour aller à la page " + currentPage));
                prevButton.setItemMeta(prevMeta);
            }
            inventory.setItem(48, prevButton);
        }

        // Bouton page suivante
        if (currentPage < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§a§lPage Suivante →");
                nextMeta.setLore(Arrays.asList("§7Cliquez pour aller à la page " + (currentPage + 2)));
                nextButton.setItemMeta(nextMeta);
            }
            inventory.setItem(50, nextButton);
        }

        // Info page actuelle - CORRECTION: Ne pas utiliser inventory.getViewers()
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageInfoMeta = pageInfo.getItemMeta();
        if (pageInfoMeta != null) {
            pageInfoMeta.setDisplayName("§6§lPage " + (currentPage + 1) + " / " + totalPages);
            pageInfoMeta.setLore(Arrays.asList(
                "§7Total des niveaux : §e" + levelsConfigService.getTotalLevels()
            ));
            pageInfo.setItemMeta(pageInfoMeta);
        }
        inventory.setItem(49, pageInfo);
    }

    /**
     * Crée un item représentant un niveau avec le nouveau design
     */
    private ItemStack createLevelItem(LevelsConfigService.LevelConfig levelConfig, int currentPlayerLevel, String playerUuid) {
        Material material;
        boolean enchanted = false;
        String status;
        String statusColor;

        // Déterminer le statut et l'apparence
        if (levelConfig.getLevelNumber() <= currentPlayerLevel) {
            // Niveau débloqué
            material = levelConfig.getMaterial();
            status = "débloqué";
            statusColor = "§a";
        } else if (levelConfig.getLevelNumber() == currentPlayerLevel + 1) {
            // Niveau en cours (prochain niveau)
            material = levelConfig.getMaterial();
            enchanted = levelConfig.isEnchanted();
            status = "en cours";
            statusColor = "§e";
        } else {
            // Niveau verrouillé
            material = Material.BARRIER;
            status = "verrouillé";
            statusColor = "§c";
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Titre avec statut
            String displayName;
            if (levelConfig.getLevelNumber() > currentPlayerLevel + 1) {
                displayName = "§7Niveau " + levelConfig.getLevelNumber() + " - §8???";
            } else {
                displayName = levelConfig.getName().replace("&", "§") + " §8(" + statusColor + status + "§8)";
            }
            meta.setDisplayName(displayName);

            // Lore selon le nouveau format
            List<String> lore = new ArrayList<>();

            if (levelConfig.getLevelNumber() > currentPlayerLevel + 1) {
                // Niveau verrouillé - informations cachées
                lore.add("§8Tâche: §8???");
                lore.add("§8Récompense: §8???");
                lore.add("");
                lore.add(statusColor + "✗ " + levelConfig.getLevelNumber() + " verrouillé");
            } else {
                // Description
                lore.add(levelConfig.getDescription().replace("&", "§"));
                lore.add("");

                // Tâche selon le type
                String task = getTaskDescription(levelConfig.getType());
                lore.add("§7Tâche: " + task);

                // Afficher la progression si c'est le niveau en cours et que ce n'est pas de l'économie
                if (levelConfig.getLevelNumber() == currentPlayerLevel + 1 && !levelConfig.getType().getName().equalsIgnoreCase("currency")) {
                    String missionKey = "mission_" + levelConfig.getLevelNumber();
                    IPlayerService.PlayerData playerData = playerService.getPlayerData(playerUuid).orElse(null);
                    int progress = playerData != null ? playerData.getMissionProgress().getOrDefault(missionKey, 0) : 0;
                    int required = levelConfig.getType().getAmount();
                    lore.add("§7Progression: §e" + progress + "§7/§e" + required);
                }

                lore.add("");

                // Action disponible
                if (levelConfig.getLevelNumber() <= currentPlayerLevel) {
                    lore.add("§a✓ Niveau complété !");
                } else {
                    lore.add("§e▶ Clic pour débloquer !");
                }
            }

            meta.setLore(lore);

            // Enchantement si nécessaire
            if (enchanted) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Obtient la description de la tâche selon le type
     */
    private String getTaskDescription(LevelsConfigService.LevelType type) {
        return switch (type.getName().toLowerCase()) {
            case "currency" -> "§ePayer §f" + (int) type.getCost() + "◎";
            case "blockbreak" -> "§eCasser §f" + type.getAmount() + " " + formatMaterialName(type.getMaterial());
            case "blockplace" -> "§ePoser §f" + type.getAmount() + " " + formatMaterialName(type.getMaterial());
            case "kills" -> "§eTuer §f" + type.getAmount() + " " + formatMobName(type.getMob());
            case "fish" -> "§ePêcher §f" + type.getAmount() + " poissons";
            default -> "§eTâche inconnue";
        };
    }

    /**
     * Formate le nom d'un matériau pour l'affichage
     */
    private String formatMaterialName(String material) {
        if (material == null) return "blocs";
        
        // Utiliser MaterialTranslator pour obtenir le nom français
        return loyfael.utils.MaterialTranslator.getInstance().getMaterialName(material);
    }

    /**
     * Formate le nom d'un mob pour l'affichage
     */
    private String formatMobName(String mob) {
        if (mob == null) return "mobs";
        return switch (mob.toLowerCase()) {
            case "zombie" -> "zombies";
            case "skeleton" -> "squelettes";
            case "spider" -> "araignées";
            case "creeper" -> "creepers";
            case "enderman" -> "endermen";
            case "wither_skeleton" -> "squelettes wither";
            default -> mob.toLowerCase().replace("_", " ");
        };
    }

    @Override
    public void handleInventoryClick(Player player, int slot, ItemStack clickedItem) {
        if (player == null || clickedItem == null) return;

        String guiType = activeGuis.get(player);
        if (guiType == null || !"levels".equals(guiType)) return;

        // Récupérer la page actuelle depuis le titre de l'inventaire
        int currentPage = getCurrentPageFromTitle(player.getOpenInventory().getTitle());

        // Gérer les boutons de navigation
        if (slot == 48) { // Bouton page précédente
            if (currentPage > 0) {
                openLevelsGui(player, currentPage - 1);
            }
            return;
        } else if (slot == 50) { // Bouton page suivante
            int totalPages = (levelsConfigService.getTotalLevels() - 1) / 28 + 1;
            if (currentPage < totalPages - 1) {
                openLevelsGui(player, currentPage + 1);
            }
            return;
        } else if (slot == 49) { // Bouton info - ne rien faire
            return;
        }

        // Vérifier si c'est un slot de bordure
        if (isBorderSlot(slot)) return;

        // Gérer les clics sur les niveaux
        handleLevelClick(player, slot, currentPage);
    }

    /**
     * Gère les clics sur les items de niveau
     */
    private void handleLevelClick(Player player, int slot, int currentPage) {
        // Calculer quel niveau correspond à ce slot
        int[] availableSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
                               28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

        int slotIndex = -1;
        for (int i = 0; i < availableSlots.length; i++) {
            if (availableSlots[i] == slot) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex == -1) return;

        int levelsPerPage = 28;
        int levelIndex = currentPage * levelsPerPage + slotIndex;
        List<LevelsConfigService.LevelConfig> sortedLevels = levelsConfigService.getSortedLevels();

        if (levelIndex >= sortedLevels.size()) return;

        LevelsConfigService.LevelConfig levelConfig = sortedLevels.get(levelIndex);
        String playerUuid = player.getUniqueId().toString();
        int currentPlayerLevel = playerService.getPlayerLevel(playerUuid);

        // Vérifier si le joueur peut interagir avec ce niveau
        if (levelConfig.getLevelNumber() > currentPlayerLevel + 1) {
            // Niveau verrouillé - ne rien faire
            player.sendMessage("§c✗ Ce niveau est verrouillé !");
            return;
        }

        if (levelConfig.getLevelNumber() <= currentPlayerLevel) {
            // Niveau déjà complété
            player.sendMessage("§a✓ Vous avez déjà complété ce niveau !");
            return;
        }

        // C'est le prochain niveau - commencer ou continuer la mission
        if (levelConfig.getType().getName().equalsIgnoreCase("currency")) {
            // Vérifier si le joueur a assez d'argent pour débloquer directement
            attemptLevelUnlock(player, levelConfig);
        } else {
            // Mission - afficher les informations et commencer
            startMission(player, levelConfig);
        }
    }

    /**
     * Tente de débloquer un niveau basé sur l'économie
     */
    private void attemptLevelUnlock(Player player, LevelsConfigService.LevelConfig levelConfig) {
        String playerUuid = player.getUniqueId().toString();
        double requiredCost = levelConfig.getType().getCost();

        // Vérifier si le joueur a assez d'argent (intégration Vault)
        if (Main.getInstance().getEconomy() != null) {
            double balance = Main.getInstance().getEconomy().getBalance(player);

            if (balance >= requiredCost) {
                // Débiter l'argent
                Main.getInstance().getEconomy().withdrawPlayer(player, requiredCost);

                // Débloquer le niveau
                playerService.setPlayerLevel(playerUuid, levelConfig.getLevelNumber());

                // Exécuter les récompenses (SEULEMENT le message formaté)
                executeRewards(player, levelConfig);


                Main.getInstance().getLogger().info("Joueur " + player.getName() + " a débloqué le niveau " + levelConfig.getLevelNumber() + " pour " + requiredCost + "◎");
            } else {
                double needed = requiredCost - balance;
                player.sendMessage("§c❌ Fonds insuffisants !");
                player.sendMessage("§7Requis: §e" + (int)requiredCost + "◎");
                player.sendMessage("§7Votre solde: §c" + (int)balance + "◎");
                player.sendMessage("§7Il vous manque: §c" + (int)needed + "◎");
            }
        } else {
            player.sendMessage("§c❌ Système économique indisponible !");
            Main.getInstance().getLogger().warning("Tentative d'achat de niveau sans système économique configuré !");
        }

        player.closeInventory();
    }

    /**
     * Démarre une mission
     */
    private void startMission(Player player, LevelsConfigService.LevelConfig levelConfig) {
        String playerUuid = player.getUniqueId().toString();

        player.sendMessage("§a▶ Mission démarrée : " + levelConfig.getName().replace("&", "§"));
        player.sendMessage("§7Tâche : " + getTaskDescription(levelConfig.getType()));

        // Initialiser le progrès de mission à 0 si pas encore commencé
        String missionKey = "mission_" + levelConfig.getLevelNumber();
        IPlayerService.PlayerData playerData = playerService.getPlayerData(playerUuid).orElse(null);
        if (playerData != null) {
            if (!playerData.getMissionProgress().containsKey(missionKey)) {
                playerData.getMissionProgress().put(missionKey, 0);
                playerService.savePlayerData(playerUuid, playerData);
            }

            int currentProgress = playerData.getMissionProgress().get(missionKey);
            int required = levelConfig.getType().getAmount();

            player.sendMessage("§7Progression actuelle: §e" + currentProgress + "§7/§a" + required);

            if (currentProgress >= required) {
                player.sendMessage("§a🎉 Mission déjà terminée ! Réclamation des récompenses...");
                completeMission(player, levelConfig);
                return;
            }
        }

        // Donner des instructions spécifiques selon le type de mission
        switch (levelConfig.getType().getName().toLowerCase()) {
            case "blockbreak":
                player.sendMessage("§7💡 Cassez des blocs de §e" + formatMaterialName(levelConfig.getType().getMaterial()) + " §7pour progresser !");
                break;
            case "blockplace":
                player.sendMessage("§7💡 Placez des blocs de §e" + formatMaterialName(levelConfig.getType().getMaterial()) + " §7pour progresser !");
                break;
            case "kills":
                player.sendMessage("§7💡 Tuez des §e" + formatMobName(levelConfig.getType().getMob()) + " §7pour progresser !");
                break;
            case "fish":
                player.sendMessage("§7💡 Pêchez des poissons pour progresser !");
                break;
        }

        player.closeInventory();
        // Log supprimé pour réduire le spam de logs
    }

    /**
     * Complète une mission et accorde les récompenses
     */
    private void completeMission(Player player, LevelsConfigService.LevelConfig levelConfig) {
        String playerUuid = player.getUniqueId().toString();

        // Mettre ���� jour le niveau du joueur
        playerService.setPlayerLevel(playerUuid, levelConfig.getLevelNumber());

        // Exécuter les récompenses
        executeRewards(player, levelConfig);

        // Nettoyer le progrès de mission
        String missionKey = "mission_" + levelConfig.getLevelNumber();
        IPlayerService.PlayerData playerData = playerService.getPlayerData(playerUuid).orElse(null);
        if (playerData != null) {
            playerData.getMissionProgress().remove(missionKey);
            playerService.savePlayerData(playerUuid, playerData);
        }

        player.sendMessage("§a🏆 Mission accomplie ! Niveau " + levelConfig.getLevelNumber() + " atteint !");
        Main.getInstance().getLogger().info("Mission complétée pour " + player.getName() + " - Niveau " + levelConfig.getLevelNumber());
    }

    /**
     * Exécute les récompenses d'un niveau
     */
    private void executeRewards(Player player, LevelsConfigService.LevelConfig levelConfig) {
        RewardExecutor.executeCommands(player, levelConfig.getRewards().getCommands());
        RewardExecutor.executeCommands(player, levelConfig.getRewards().getRewardsMessage());

        if (levelConfig.getRewards().isBroadcast()) {
            RewardExecutor.broadcastMessages(player, levelConfig.getRewards().getBroadcastMessage());
        }
    }


    @Override
    public boolean isGuiOpen(Player player) {
        return activeGuis.containsKey(player);
    }

    @Override
    public String getActiveGuiType(Player player) {
        return activeGuis.get(player);
    }

    @Override
    public void updateGui(Player player) {
        String guiType = activeGuis.get(player);
        if (guiType != null && "levels".equals(guiType)) {
            // Récupérer la page actuelle et rouvrir la GUI
            int currentPage = getCurrentPageFromTitle(player.getOpenInventory().getTitle());
            openLevelsGui(player, currentPage);
        }
    }

    @Override
    public void updateGui(Player player, String guiType) {
        if ("levels".equals(guiType) && activeGuis.containsKey(player)) {
            int currentPage = getCurrentPageFromTitle(player.getOpenInventory().getTitle());
            openLevelsGui(player, currentPage);
        }
    }

    @Override
    public void closeGui(Player player) {
        cleanupPlayerGui(player);
        player.closeInventory();
    }

    /**
     * Nettoie explicitement les données GUI d'un joueur
     * Méthode publique pour permettre un nettoyage externe
     */
    public void cleanupPlayerGui(Player player) {
        activeGuis.remove(player);
        
        // Forcer la synchronisation de l'inventaire
        if (player.isOnline()) {
            player.updateInventory();
        }
    }

    @Override
    public void handleGuiClick(Player player, ItemStack item, int slot) {
        handleInventoryClick(player, slot, item);
    }

    @Override
    public ItemStack createGuiItem(String materialName, String displayName, String... lore) {
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName.replace("&", "§"));
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line.replace("&", "§"));
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean hasOpenGui(Player player) {
        return activeGuis.containsKey(player);
    }

    @Override
    public String getCurrentGuiType(Player player) {
        return activeGuis.get(player);
    }

    @Override
    public void registerGuiHandler(String guiType, IGuiHandler handler) {
        guiHandlers.put(guiType, handler);
    }

    @Override
    public void registerHandler(String guiType, IGuiHandler handler) {
        guiHandlers.put(guiType, handler);
    }

    /**
     * Nettoie les données lors de la déconnexion d'un joueur
     */
    public void onPlayerDisconnect(Player player) {
        activeGuis.remove(player);
    }

    /**
     * Enregistre les handlers par défaut
     */
    private void registerDefaultHandlers() {
        // Handler pour la GUI des niveaux
        registerHandler("levels", new IGuiHandler() {
            @Override
            public Inventory createInventory(Player player, Map<String, Object> parameters) {
                int page = parameters != null && parameters.containsKey("page") ?
                    (Integer) parameters.get("page") : 0;
                return createLevelsInventory(player, page);
            }

            @Override
            public void handleClick(Player player, ItemStack clickedItem, int slot) {
                handleInventoryClick(player, slot, clickedItem);
            }

            @Override
            public void updateContent(Player player, Inventory inventory) {
                // Mettre à jour le contenu de l'inventaire des niveaux
                int currentPage = getCurrentPageFromTitle(inventory.getViewers().size() > 0 ?
                    player.getOpenInventory().getTitle() : "§8§lMissions §7- §8Page 1/1");
                Inventory newInventory = createLevelsInventory(player, currentPage);

                // Copier le contenu du nouvel inventaire dans l'inventaire existant
                for (int i = 0; i < Math.min(inventory.getSize(), newInventory.getSize()); i++) {
                    inventory.setItem(i, newInventory.getItem(i));
                }
            }
        });
    }

    /**
     * Ouvre la GUI des niveaux à une page spécifique
     */
    public void openLevelsGui(Player player, int page) {
        Map<String, Object> parameters = new ConcurrentHashMap<>();
        parameters.put("page", page);
        openGui(player, "levels", parameters);
    }

    /**
     * Extrait le numéro de page depuis le titre de l'inventaire
     */
    private int getCurrentPageFromTitle(String title) {
        if (title == null || !title.contains("Page ")) return 0;

        try {
            // Extraire "Page X/Y" du titre
            String[] parts = title.split("Page ");
            if (parts.length > 1) {
                String pagePart = parts[1].split("/")[0].trim();
                return Integer.parseInt(pagePart) - 1; // Convertir en index 0-based
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Impossible d'extraire la page du titre: " + title);
        }

        return 0;
    }

    /**
     * Vérifie si un slot fait partie des bordures
     */
    private boolean isBorderSlot(int slot) {
        // Bordures haut et bas
        if (slot < 9 || slot >= 45) return true;

        // Bordures côtés
        if (slot % 9 == 0 || slot % 9 == 8) return true;

        return false;
    }
}
