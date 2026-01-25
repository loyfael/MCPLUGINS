package loyfael.gui;

import loyfael.Main;
import loyfael.manager.ShopManager;
import loyfael.model.Shop;
import loyfael.model.ShopItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * GUI principal du menu central global - Système avancé inspiré des hôtels des ventes
 * Performances garanties selon le cahier des charges
 */
public class ShopMenuGUI implements CommandExecutor, Listener, InventoryHolder {

    private final Main plugin;
    private final Map<UUID, MenuSession> activeSessions;
    private final Map<Material, List<Shop>> groupedShops;

    public ShopMenuGUI(Main plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.groupedShops = new ConcurrentHashMap<>();

        // Écouter les événements GUI
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Rafraîchissement périodique des données
        startPeriodicRefresh();
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

        openMainMenu(player);
        return true;
    }

    /**
     * Ouvre le menu principal avec tous les shops groupés par type d'item
     */
    public void openMainMenu(@NotNull Player player) {
        plugin.getShopManager().searchShops(new ShopManager.ShopSearchFilter())
            .thenAccept(shops -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    MenuSession session = new MenuSession(player, shops);
                    activeSessions.put(player.getUniqueId(), session);
                    displayMainMenu(session);
                });
            });
    }

    /**
     * Affiche le menu principal avec pagination dynamique
     */
    private void displayMainMenu(@NotNull MenuSession session) {
        String title = plugin.getConfigManager().getMenuTitle();
        Inventory inventory = Bukkit.createInventory(this, 54, Component.text(title));

        // Groupement par matériau
        Map<Material, List<Shop>> grouped = session.groupShopsByMaterial();
        List<Material> materials = new ArrayList<>(grouped.keySet());

        int itemsPerPage = plugin.getConfigManager().getItemsPerPage();
        int startIndex = session.currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, materials.size());

        // Affichage des items groupés
        for (int i = startIndex; i < endIndex; i++) {
            Material material = materials.get(i);
            List<Shop> shopsForMaterial = grouped.get(material);

            ItemStack displayItem = createMaterialDisplayItem(material, shopsForMaterial);
            inventory.setItem(i - startIndex, displayItem);
        }

        // Boutons de navigation et filtres
        addNavigationButtons(inventory, session, materials.size());
        addFilterButtons(inventory, session);

        session.player.openInventory(inventory);
    }

    /**
     * Crée l'item d'affichage pour un matériau avec statistiques
     */
    private ItemStack createMaterialDisplayItem(@NotNull Material material, @NotNull List<Shop> shops) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Statistiques des shops pour ce matériau
            OptionalDouble avgPrice = shops.stream()
                .mapToDouble(Shop::getPrice)
                .average();

            Shop cheapestShop = shops.stream()
                .min(Comparator.comparingDouble(Shop::getPrice))
                .orElse(null);

            int totalStock = shops.stream()
                .mapToInt(Shop::getStock)
                .sum();

            // Utilisation de l'API Adventure moderne pour displayName
            meta.displayName(Component.text("§6§l" + formatMaterialName(material)));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Shops disponibles: §e" + shops.size()));
            lore.add(Component.text("§7Stock total: §e" + totalStock));

            if (avgPrice.isPresent()) {
                lore.add(Component.text("§7Prix moyen: §e" + String.format("%.2f", avgPrice.getAsDouble()) + "§6◎"));
            }

            if (cheapestShop != null) {
                lore.add(Component.text("§7Prix le plus bas: §e" + String.format("%.2f", cheapestShop.getPrice()) + "§6◎"));
                lore.add(Component.text("§7Vendeur: §e" + cheapestShop.getOwnerName()));
            }

            lore.add(Component.empty());
            lore.add(Component.text("§a▶ Clic gauche: Voir les shops"));
            lore.add(Component.text("§b▶ Clic droit: Filtrer par ce matériau"));

            // Utilisation de l'API Adventure moderne pour lore
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Affiche la liste détaillée des shops pour un matériau
     */
    public void openMaterialShops(@NotNull Player player, @NotNull Material material) {
        ShopManager.ShopSearchFilter filter = new ShopManager.ShopSearchFilter();
        filter.material = material;
        filter.sortBy = ShopManager.ShopSearchFilter.SortBy.PRICE_ASC;

        plugin.getShopManager().searchShops(filter)
            .thenAccept(shops -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    displayShopList(player, shops, material.name());
                });
            });
    }

    /**
     * Affiche une liste paginée de shops spécifiques
     */
    private void displayShopList(@NotNull Player player, @NotNull List<Shop> shops, @NotNull String title) {
        Inventory inventory = Bukkit.createInventory(this, 54,
            Component.text("§6§lShops - " + formatMaterialName(Material.valueOf(title))));

        MenuSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            session = new MenuSession(player, shops);
            activeSessions.put(player.getUniqueId(), session);
        }

        int itemsPerPage = 45; // 9x5 pour les shops
        int startIndex = session.detailPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, shops.size());

        // Affichage des shops individuels
        for (int i = startIndex; i < endIndex; i++) {
            Shop shop = shops.get(i);
            ItemStack displayItem = createShopDisplayItem(shop);
            inventory.setItem(i - startIndex, displayItem);
        }

        // Navigation pour la vue détaillée
        addDetailNavigationButtons(inventory, session, shops.size());

        player.openInventory(inventory);
    }

    /**
     * Crée l'item d'affichage pour un shop individuel
     */
    private ItemStack createShopDisplayItem(@NotNull Shop shop) {
        ItemStack item = shop.getItem().toDisplayItemStack(shop.getItem().getAmount());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Utilisation de l'API Adventure moderne pour displayName
            meta.displayName(Component.text("§6§l" + shop.getItem().getDisplayName()));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Type: §e" + shop.getType().getDisplayName()));
            lore.add(Component.text("§7Prix unitaire: §e" + String.format("%.2f", shop.getPrice()) + "§6◎"));
            lore.add(Component.text("§7Stock: §e" + shop.getStock()));
            lore.add(Component.text("§7Vendeur: §e" + shop.getOwnerName()));
            lore.add(Component.text("§7Monde: §e" + shop.getWorld()));
            lore.add(Component.text("§7Position: §e" + shop.getX() + ", " + shop.getY() + ", " + shop.getZ()));
            lore.add(Component.empty());

            if (plugin.getConfigManager().isTeleportEnabled()) {
                lore.add(Component.text("§a▶ Clic gauche: Se téléporter au shop"));
            }
            lore.add(Component.text("§b▶ Maj + Clic: Plus d'informations"));

            // Utilisation de l'API Adventure moderne pour lore
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() != this) return;

        event.setCancelled(true);

        MenuSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        handleMenuClick(player, session, event);
    }

    private void handleMenuClick(@NotNull Player player, @NotNull MenuSession session, @NotNull InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack item = event.getCurrentItem();

        // Navigation
        if (item.getType() == Material.ARROW) {
            handleNavigation(player, session, item);
            return;
        }

        // Filtres
        if (item.getType() == Material.HOPPER) {
            openFilterMenu(player, session);
            return;
        }

        // Téléportation vers shop
        if (event.isLeftClick() && plugin.getConfigManager().isTeleportEnabled()) {
            handleShopTeleport(player, session, slot);
        }

        // Affichage détaillé
        else if (event.isRightClick()) {
            handleDetailView(player, session, item.getType());
        }
    }

    private void handleShopTeleport(@NotNull Player player, @NotNull MenuSession session, int slot) {
        Shop targetShop = session.getShopAtSlot(slot);
        if (targetShop == null) return;

        org.bukkit.Location location = targetShop.getLocation();
        if (location == null) {
            player.sendMessage("§cImpossible de se téléporter au shop: monde introuvable.");
            return;
        }

        player.closeInventory();
        int delay = plugin.getConfigManager().getTeleportDelay();

        if (delay > 0) {
            player.sendMessage("§6Téléportation dans " + delay + " secondes...");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(location.add(0.5, 1, 0.5));
                player.sendMessage("§aVous avez été téléporté au shop de §e" + targetShop.getOwnerName());
            }, delay * 20L);
        } else {
            player.teleport(location.add(0.5, 1, 0.5));
            player.sendMessage("§aVous avez été téléporté au shop de §e" + targetShop.getOwnerName());
        }
    }

    private void addNavigationButtons(@NotNull Inventory inventory, @NotNull MenuSession session, int totalItems) {
        int itemsPerPage = plugin.getConfigManager().getItemsPerPage();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        // Page précédente
        if (session.currentPage > 0) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta meta = prevItem.getItemMeta();
            // Utilisation de l'API Adventure moderne
            meta.displayName(Component.text("§a◀ Page précédente"));
            prevItem.setItemMeta(meta);
            inventory.setItem(48, prevItem);
        }

        // Page suivante
        if (session.currentPage < totalPages - 1) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta meta = nextItem.getItemMeta();
            // Utilisation de l'API Adventure moderne
            meta.displayName(Component.text("§aPage suivante ▶"));
            nextItem.setItemMeta(meta);
            inventory.setItem(50, nextItem);
        }

        // Info pagination
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta meta = infoItem.getItemMeta();
        // Utilisation de l'API Adventure moderne
        meta.displayName(Component.text("§6Page " + (session.currentPage + 1) + "/" + totalPages));
        infoItem.setItemMeta(meta);
        inventory.setItem(49, infoItem);
    }

    private void addFilterButtons(@NotNull Inventory inventory, @NotNull MenuSession session) {
        // Bouton filtres
        ItemStack filterItem = new ItemStack(Material.HOPPER);
        ItemMeta meta = filterItem.getItemMeta();
        // Utilisation de l'API Adventure moderne
        meta.displayName(Component.text("§6§lFiltres avancés"));
        List<Component> lore = Arrays.asList(
            Component.text("§7Cliquez pour ouvrir"),
            Component.text("§7les options de filtrage")
        );
        meta.lore(lore);
        filterItem.setItemMeta(meta);
        inventory.setItem(53, filterItem);
    }

    private void addDetailNavigationButtons(@NotNull Inventory inventory, @NotNull MenuSession session, int totalShops) {
        // Similaire aux boutons de navigation principaux mais pour la vue détaillée
        addNavigationButtons(inventory, session, totalShops);

        // Bouton retour
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = backItem.getItemMeta();
        // Utilisation de l'API Adventure moderne
        meta.displayName(Component.text("§c◀ Retour au menu principal"));
        backItem.setItemMeta(meta);
        inventory.setItem(45, backItem);
    }

    private void handleNavigation(@NotNull Player player, @NotNull MenuSession session, @NotNull ItemStack item) {
        // Récupération du displayName avec l'API Adventure moderne
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(item.getItemMeta().displayName());

        if (displayName.contains("précédente")) {
            session.currentPage = Math.max(0, session.currentPage - 1);
            displayMainMenu(session);
        } else if (displayName.contains("suivante")) {
            session.currentPage++;
            displayMainMenu(session);
        }
    }

    private void handleDetailView(@NotNull Player player, @NotNull MenuSession session, @NotNull Material material) {
        openMaterialShops(player, material);
    }

    private void openFilterMenu(@NotNull Player player, @NotNull MenuSession session) {
        // TODO: Implémenter le menu de filtrage avancé
        player.sendMessage("§6Menu de filtrage en développement...");
    }

    private void startPeriodicRefresh() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Rafraîchissement du cache des shops populaires
            plugin.getCacheManager().getMostPopularShops(100);
        }, 0L, 20L * 60L); // Toutes les minutes
    }

    private String formatMaterialName(@NotNull Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Bukkit.createInventory(this, 54, Component.text("AetherPlayerShop"));
    }

    /**
     * Session de menu pour un joueur
     */
    private static class MenuSession {
        final Player player;
        final List<Shop> allShops;
        int currentPage = 0;
        int detailPage = 0;
        ShopManager.ShopSearchFilter currentFilter = new ShopManager.ShopSearchFilter();

        MenuSession(@NotNull Player player, @NotNull List<Shop> shops) {
            this.player = player;
            this.allShops = new ArrayList<>(shops);
        }

        Map<Material, List<Shop>> groupShopsByMaterial() {
            return allShops.stream()
                .collect(Collectors.groupingBy(shop -> shop.getItem().getMaterial()));
        }

        @Nullable
        Shop getShopAtSlot(int slot) {
            if (slot >= 0 && slot < allShops.size()) {
                return allShops.get(slot);
            }
            return null;
        }
    }
}
