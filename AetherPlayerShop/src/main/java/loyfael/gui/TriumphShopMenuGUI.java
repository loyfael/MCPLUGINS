package loyfael.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import loyfael.Main;
import loyfael.manager.ShopManager;
import loyfael.model.Shop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI principal modernisé avec TriumphGUI - Menu central global selon le cahier des charges
 * Interface inspirée des hôtels des ventes avec performances optimales
 */
public class TriumphShopMenuGUI implements CommandExecutor {

    private final Main plugin;

    public TriumphShopMenuGUI(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                           @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
            return true;
        }

        if (!player.hasPermission("aetherplayershop.menu")) {
            player.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        openMainShopMenu(player);
        return true;
    }

    /**
     * Ouvre le menu principal avec TriumphGUI - Interface moderne et performante
     */
    public void openMainShopMenu(@NotNull Player player) {
        // Récupération asynchrone des shops pour éviter les lags
        plugin.getShopManager().searchShops(new ShopManager.ShopSearchFilter())
            .thenAccept(shops -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    createMainMenu(player, shops);
                });
            });
    }

    /**
     * Crée le menu principal avec groupement par matériau
     */
    private void createMainMenu(@NotNull Player player, @NotNull List<Shop> shops) {
        // Groupement des shops par matériau pour l'affichage optimisé
        Map<Material, List<Shop>> groupedShops = shops.stream()
            .filter(Shop::isActive)
            .collect(Collectors.groupingBy(shop -> shop.getItem().getMaterial()));

        // Interface paginée moderne avec TriumphGUI
        PaginatedGui gui = Gui.paginated()
            .title(Component.text("✦ Aether Player Shop ✦")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true))
            .rows(6)
            .pageSize(28) // 4 lignes d'items (7x4)
            .create();

        // Ajout des items groupés par matériau
        for (Map.Entry<Material, List<Shop>> entry : groupedShops.entrySet()) {
            Material material = entry.getKey();
            List<Shop> materialShops = entry.getValue();

            GuiItem materialItem = createMaterialGuiItem(material, materialShops, player);
            gui.addItem(materialItem);
        }

        // Boutons de navigation et actions
        setupNavigationButtons(gui, player, groupedShops.size());
        setupActionButtons(gui, player);

        // Ouverture avec effets
        gui.open(player);
        playOpenSound(player);
    }

    /**
     * Crée un GuiItem pour représenter un matériau avec ses statistiques
     */
    private GuiItem createMaterialGuiItem(@NotNull Material material,
                                        @NotNull List<Shop> shops,
                                        @NotNull Player player) {
        // Calculs statistiques
        OptionalDouble avgPrice = shops.stream().mapToDouble(Shop::getPrice).average();
        Optional<Shop> cheapestShop = shops.stream()
            .min(Comparator.comparingDouble(Shop::getPrice));
        int totalStock = shops.stream().mapToInt(Shop::getStock).sum();

        // Construction de l'item avec métadonnées riches
        List<Component> loreComponents = new ArrayList<>();
        loreComponents.add(Component.text("§7Shops disponibles: §e" + shops.size()));
        loreComponents.add(Component.text("§7Stock total: §e" + totalStock));
        loreComponents.add(Component.empty());

        // Ajout des statistiques de prix
        if (avgPrice.isPresent()) {
            loreComponents.add(Component.text("§7Prix moyen: §e" +
                String.format("%.2f", avgPrice.getAsDouble()) + "§6◎"));
        }

        if (cheapestShop.isPresent()) {
            Shop cheapest = cheapestShop.get();
            loreComponents.add(Component.text("§7Prix le plus bas: §e" +
                String.format("%.2f", cheapest.getPrice()) + "§6◎"));
            loreComponents.add(Component.text("§7Vendeur: §e" + cheapest.getOwnerName()));
        }

        loreComponents.add(Component.empty());
        loreComponents.add(Component.text("§a▶ Clic gauche: Voir les shops", NamedTextColor.GREEN));
        loreComponents.add(Component.text("§b▶ Clic droit: Filtrer par ce matériau", NamedTextColor.AQUA));
        loreComponents.add(Component.text("§e▶ Shift + Clic: Informations détaillées", NamedTextColor.YELLOW));

        ItemStack item = ItemBuilder.from(material)
            .name(Component.text("§6§l" + formatMaterialName(material)))
            .lore(loreComponents)
            .build();

        return ItemBuilder.from(item)
            .asGuiItem(event -> {
                event.setCancelled(true);

                if (event.isShiftClick()) {
                    showMaterialDetails(player, material, shops);
                } else if (event.isRightClick()) {
                    openFilteredView(player, material);
                } else {
                    openMaterialShops(player, material);
                }

                playClickSound(player);
            });
    }

    /**
     * Ouvre la vue détaillée d'un matériau avec liste des shops
     */
    private void openMaterialShops(@NotNull Player player, @NotNull Material material) {
        ShopManager.ShopSearchFilter filter = new ShopManager.ShopSearchFilter();
        filter.material = material;
        filter.sortBy = ShopManager.ShopSearchFilter.SortBy.PRICE_ASC;

        plugin.getShopManager().searchShops(filter)
            .thenAccept(shops -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    createMaterialShopsMenu(player, material, shops);
                });
            });
    }

    /**
     * Crée le menu détaillé des shops pour un matériau spécifique
     */
    private void createMaterialShopsMenu(@NotNull Player player, @NotNull Material material,
                                       @NotNull List<Shop> shops) {
        PaginatedGui gui = Gui.paginated()
            .title(Component.text("Shops - " + formatMaterialName(material))
                .color(NamedTextColor.GOLD))
            .rows(6)
            .pageSize(28)
            .create();

        // Ajout des shops individuels
        for (Shop shop : shops) {
            if (!shop.isActive()) continue;

            GuiItem shopItem = createShopGuiItem(shop, player);
            gui.addItem(shopItem);
        }

        // Navigation et bouton retour
        setupNavigationButtons(gui, player, shops.size());
        setupBackButton(gui, player);

        gui.open(player);
        playOpenSound(player);
    }

    /**
     * Crée un GuiItem pour un shop individuel
     */
    private GuiItem createShopGuiItem(@NotNull Shop shop, @NotNull Player player) {
        ItemStack displayItem = shop.getItem().toDisplayItemStack(shop.getItem().getAmount());

        List<Component> loreComponents = Arrays.asList(
            Component.text("§7Type: §e" + shop.getType().getDisplayName()),
            Component.text("§7Prix unitaire: §e" + String.format("%.2f", shop.getPrice()) + "§6◎"),
            Component.text("§7Stock: §e" + shop.getStock()),
            Component.text("§7Vendeur: §e" + shop.getOwnerName()),
            Component.empty(),
            Component.text("§7Localisation:"),
            Component.text("§8• Monde: §7" + shop.getWorld()),
            Component.text("§8• Position: §7" + shop.getX() + ", " + shop.getY() + ", " + shop.getZ()),
            Component.empty(),
            Component.text("§a▶ Clic gauche: Se téléporter au shop", NamedTextColor.GREEN),
            Component.text("§b▶ Clic droit: Plus d'informations", NamedTextColor.AQUA)
        );

        ItemStack item = ItemBuilder.from(displayItem)
            .name(Component.text("§6§l" + shop.getItem().getDisplayName()))
            .lore(loreComponents)
            .build();

        return ItemBuilder.from(item)
            .asGuiItem(event -> {
                event.setCancelled(true);

                if (event.isRightClick()) {
                    showShopDetails(player, shop);
                } else {
                    handleShopTeleport(player, shop);
                }

                playClickSound(player);
            });
    }

    /**
     * Gère la téléportation vers un shop
     */
    private void handleShopTeleport(@NotNull Player player, @NotNull Shop shop) {
        if (!plugin.getConfigManager().isTeleportEnabled()) {
            player.sendMessage("§cLa téléportation est désactivée.");
            return;
        }

        org.bukkit.Location location = shop.getLocation();
        if (location == null) {
            player.sendMessage("§cImpossible de se téléporter au shop: monde introuvable.");
            return;
        }

        player.closeInventory();
        int delay = plugin.getConfigManager().getTeleportDelay();

        if (delay > 0) {
            player.sendMessage("§6Téléportation dans " + delay + " secondes...");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                teleportPlayer(player, location, shop);
            }, delay * 20L);
        } else {
            teleportPlayer(player, location, shop);
        }
    }

    /**
     * Téléporte le joueur avec effets
     */
    private void teleportPlayer(@NotNull Player player, @NotNull org.bukkit.Location location,
                              @NotNull Shop shop) {
        location.add(0.5, 1, 0.5); // Centrage et élévation
        player.teleport(location);

        // Messages et effets
        player.sendMessage("§aVous avez été téléporté au shop de §e" + shop.getOwnerName());

        if (plugin.getConfigManager().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }

        if (plugin.getConfigManager().isParticleEnabled()) {
            player.spawnParticle(org.bukkit.Particle.PORTAL,
                player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    /**
     * Configure les boutons de navigation modernes
     */
    private void setupNavigationButtons(@NotNull PaginatedGui gui, @NotNull Player player, int totalItems) {
        // Page précédente
        gui.setItem(5, 3, ItemBuilder.from(Material.ARROW)
            .name(Component.text("§a◀ Page précédente", NamedTextColor.GREEN))
            .asGuiItem(event -> {
                event.setCancelled(true);
                gui.previous();
                playClickSound(player);
            }));

        // Informations de page (corrigé sans getItems())
        gui.setItem(5, 4, ItemBuilder.from(Material.BOOK)
            .name(Component.text("§6Informations", NamedTextColor.GOLD))
            .lore(Arrays.asList(
                Component.text("§7Page actuelle: §e" + (gui.getCurrentPageNum() + 1)),
                Component.text("§7Navigation: §eUtilisez les flèches")
            ))
            .asGuiItem(event -> event.setCancelled(true)));

        // Page suivante
        gui.setItem(5, 5, ItemBuilder.from(Material.ARROW)
            .name(Component.text("§aPage suivante ▶", NamedTextColor.GREEN))
            .asGuiItem(event -> {
                event.setCancelled(true);
                gui.next();
                playClickSound(player);
            }));
    }

    /**
     * Configure les boutons d'action supplémentaires
     */
    private void setupActionButtons(@NotNull PaginatedGui gui, @NotNull Player player) {
        // Bouton de rafraîchissement
        gui.setItem(5, 1, ItemBuilder.from(Material.COMPASS)
            .name(Component.text("§bActualiser", NamedTextColor.AQUA))
            .lore(Component.text("§7Cliquez pour actualiser la liste"))
            .asGuiItem(event -> {
                event.setCancelled(true);
                gui.close(player);
                openMainShopMenu(player);
            }));

        // Filtres avancés
        gui.setItem(5, 7, ItemBuilder.from(Material.HOPPER)
            .name(Component.text("§6§lFiltres avancés", NamedTextColor.GOLD, TextDecoration.BOLD))
            .lore(Arrays.asList(
                Component.text("§7Filtrer par:"),
                Component.text("§8• Prix"),
                Component.text("§8• Vendeur"),
                Component.text("§8• Type d'item")
            ))
            .asGuiItem(event -> {
                event.setCancelled(true);
                openAdvancedFilters(player);
            }));
    }

    /**
     * Configure le bouton retour
     */
    private void setupBackButton(@NotNull PaginatedGui gui, @NotNull Player player) {
        gui.setItem(5, 1, ItemBuilder.from(Material.BARRIER)
            .name(Component.text("§c◀ Retour au menu principal", NamedTextColor.RED))
            .asGuiItem(event -> {
                event.setCancelled(true);
                gui.close(player);
                openMainShopMenu(player);
            }));
    }

    // Méthodes utilitaires et stubs pour les fonctionnalités avancées
    private void showMaterialDetails(@NotNull Player player, @NotNull Material material,
                                   @NotNull List<Shop> shops) {
        player.sendMessage("§6=== Détails pour " + formatMaterialName(material) + " ===");
        player.sendMessage("§7Shops disponibles: §e" + shops.size());
        player.sendMessage("§7Stock total: §e" + shops.stream().mapToInt(Shop::getStock).sum());

        OptionalDouble avgPrice = shops.stream().mapToDouble(Shop::getPrice).average();
        if (avgPrice.isPresent()) {
            player.sendMessage("§7Prix moyen: §e" + String.format("%.2f", avgPrice.getAsDouble()) + "§6◎");
        }
    }

    private void openFilteredView(@NotNull Player player, @NotNull Material material) {
        player.sendMessage("§6Vue filtrée pour " + formatMaterialName(material) + " (à développer)");
    }

    private void showShopDetails(@NotNull Player player, @NotNull Shop shop) {
        player.sendMessage("§6=== Détails du shop ===");
        player.sendMessage("§7Propriétaire: §e" + shop.getOwnerName());
        player.sendMessage("§7Type: §e" + shop.getType().getDisplayName());
        player.sendMessage("§7Item: §e" + shop.getItem().getDisplayName());
        player.sendMessage("§7Prix: §e" + String.format("%.2f", shop.getPrice()) + "§6◎");
        player.sendMessage("§7Stock: §e" + shop.getStock());
    }

    private void openAdvancedFilters(@NotNull Player player) {
        player.sendMessage("§6Filtres avancés (à développer)");
    }

    // Effets sonores et visuels
    private void playOpenSound(@NotNull Player player) {
        if (plugin.getConfigManager().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
        }
    }

    private void playClickSound(@NotNull Player player) {
        if (plugin.getConfigManager().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        }
    }

    private String formatMaterialName(@NotNull Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
