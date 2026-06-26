package loyfael;

import loyfael.interfaces.IEconomyService;
import loyfael.interfaces.IShopService;
import loyfael.model.ShopItem;
import loyfael.utils.UXHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ShopGUI implements Listener {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();
    private static final String MAIN_TITLE_PLAIN = "Bazaar de Zéphyline Bricorne";
    private static final String BUY_PREFIX_PLAIN = "🛒 Acheter - ";
    private static final String SELL_PREFIX_PLAIN = "💰 Vendre - ";

    private final IShopService shopService;
    private final IEconomyService economyService;
    private final Set<UUID> activeTransactions = new HashSet<>();

  public ShopGUI(IShopService shopService, IEconomyService economyService) {
        this.shopService = shopService;
        this.economyService = economyService;
    }

    public void openShop(Player player) {
        UXHelper.playShopOpenSound(player);
        Inventory inv = Bukkit.createInventory(null, 45, LEGACY_SERIALIZER.deserialize("§8Bazaar de Zéphyline Bricorne"));

        // Ligne décorative du haut
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§8"));
        }

        // Ligne décorative du bas
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§8"));
        }

        // Items dans la zone centrale (lignes 1-3)
        List<ShopItem> items = shopService.getCurrentItems();
        for (int i = 0; i < Math.min(items.size(), 27); i++) {
            ShopItem shopItem = items.get(i);
            ItemStack displayItem = createEnhancedDisplayItem(shopItem, player);
            inv.setItem(i + 9, displayItem); // +9 pour skip la ligne décorative
        }

        // Bouton d'info en bas à droite
        inv.setItem(44, createInfoButton(player));

        player.openInventory(inv);
    }

    private ItemStack createEnhancedDisplayItem(ShopItem shopItem, Player player) {
        ItemStack displayItem = shopItem.toItemStack();
        ItemMeta meta = displayItem.getItemMeta();

        if (meta != null) {
            List<String> lore = new ArrayList<>();

            // Lore originale de l'item
            if (shopItem.getLore() != null) {
                lore.addAll(shopItem.getLore());
                lore.add("");
            }

            // Informations de prix (prix fixe pour la journée)
            lore.add("§e💰 §7Prix: §e" + UXHelper.formatMoney(shopItem.getCurrentPrice()));

            // Stock avec couleur dynamique
            String stockColor = UXHelper.getStockColorCode(shopItem.getCurrentStock(), shopItem.getMaxStock());
            lore.add("§7📦 Stock: " + stockColor + shopItem.getCurrentStock() + "§7/" + shopItem.getMaxStock());

            // Calcul de ce que le joueur peut acheter
            double balance = economyService.getBalance(player);
            int maxAffordable = (int) (balance / shopItem.getCurrentPrice());
            int playerCanBuy = Math.min(maxAffordable, shopItem.getCurrentStock());

            if (playerCanBuy > 0) {
                lore.add("§7💳 Achat possible: §a" + playerCanBuy + "x");
            } else {
                lore.add("§7💳 §cFonds insuffisants");
            }

            // Calcul de ce que le joueur peut vendre
            int playerHas = countItemInInventory(player, shopItem);
            if (playerHas > 0) {
                double sellPrice = shopItem.getCurrentPrice() * 0.8;
                lore.add("§7🏷️ Vous possédez: §b" + playerHas + "x");
                lore.add("§7💸 Prix de vente: §6" + UXHelper.formatMoney(sellPrice));
            }

            lore.add("");
            lore.add("§7§lCLIC GAUCHE: §aVENDRE §0§l");
            lore.add("§7§lCLIC DROIT: §aACHETER §0§l");

            meta.lore(toLegacyComponents(lore));
            displayItem.setItemMeta(meta);
        }

        return displayItem;
    }

    private ItemStack createInfoButton(Player player) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY_SERIALIZER.deserialize("§6📊 §lInformations"));
            List<String> lore = List.of(
                    "§b§l§oZéphyline §7§ote salue d’un clin d’œil ;)",
                    "§7Chaque matin à 4h00, son étal s’anime de", 
                    "§7nouvelles trouvailles célestes. Tu peux vendre", 
                    "§7ce que tu récoltes ou acheter ce dont tu as besoin.", 
                    "§7Mais prends garde ! Les stocks sont limités et",
                    "§7les prix ne restent jamais figés. Plus un objet",
                    "§7est demandé, plus sa valeur grimpe. Moins il intéresse,",
                    "§7plus il devient abordable.",
                    "",
                    "§7Fait vite, car les meilleures affaires ne restent pas longtemps !",
                    "  ",
                    "§7Ton solde: §e" + UXHelper.formatMoney(economyService.getBalance(player)),
                    "§7Items disponibles: §a" + shopService.getCurrentItems().size()
            );
            meta.lore(toLegacyComponents(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDecorativeItem(Material material, String color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY_SERIALIZER.deserialize(color));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openBuyMenu(Player player, ShopItem item) {
        UXHelper.playClickSound(player);
        Inventory inv = Bukkit.createInventory(null, 27, LEGACY_SERIALIZER.deserialize("§a🛒 Acheter - " + item.getName()));

        // Ligne décorative
        for (int i = 18; i < 27; i++) {
            inv.setItem(i, createDecorativeItem(Material.GREEN_STAINED_GLASS_PANE, "§8"));
        }

        // Boutons de quantité avec prix calculé
        inv.setItem(10, createEnhancedQuantityItem(Material.LIME_CONCRETE, "§a1x", 1, item, true));
        inv.setItem(11, createEnhancedQuantityItem(Material.LIME_CONCRETE, "§a5x", 5, item, true));
        inv.setItem(12, createEnhancedQuantityItem(Material.LIME_CONCRETE, "§a10x", 10, item, true));
        inv.setItem(13, createEnhancedQuantityItem(Material.LIME_CONCRETE, "§a32x", 32, item, true));
        inv.setItem(14, createEnhancedQuantityItem(Material.LIME_CONCRETE, "§a64x", 64, item, true));

        // Bouton MAX calculé
        int maxAffordable = (int) (economyService.getBalance(player) / item.getCurrentPrice());
        int maxBuyable = Math.min(maxAffordable, item.getCurrentStock());
        int freeSlots = getFreeInventorySlots(player);
        int maxBySpace = freeSlots * 64 / item.getAmount();
        int finalMax = Math.min(maxBuyable, maxBySpace);

        if (finalMax > 0) {
            inv.setItem(16, createEnhancedQuantityItem(Material.EMERALD_BLOCK, "§aMAX (" + finalMax + "x)", finalMax, item, true));
        } else {
            inv.setItem(16, createDisabledItem(Material.BARRIER, "§cImpossible", "§7Fonds insuffisants ou stock vide"));
        }

        inv.setItem(22, createQuantityItem(Material.BARRIER, "§cRetour", -1));

        player.openInventory(inv);
    }

    public void openSellMenu(Player player, ShopItem item) {
        UXHelper.playClickSound(player);
        Inventory inv = Bukkit.createInventory(null, 27, LEGACY_SERIALIZER.deserialize("§c💰 Vendre - " + item.getName()));

        // Ligne décorative
        for (int i = 18; i < 27; i++) {
            inv.setItem(i, createDecorativeItem(Material.RED_STAINED_GLASS_PANE, "§8"));
        }

        // Boutons de quantité avec prix de vente calculé
        inv.setItem(10, createEnhancedQuantityItem(Material.RED_CONCRETE, "§c1x", 1, item, false));
        inv.setItem(11, createEnhancedQuantityItem(Material.RED_CONCRETE, "§c5x", 5, item, false));
        inv.setItem(12, createEnhancedQuantityItem(Material.RED_CONCRETE, "§c10x", 10, item, false));
        inv.setItem(13, createEnhancedQuantityItem(Material.RED_CONCRETE, "§c32x", 32, item, false));
        inv.setItem(14, createEnhancedQuantityItem(Material.RED_CONCRETE, "§c64x", 64, item, false));

        // Bouton TOUT
        int playerHas = countItemInInventory(player, item);
        if (playerHas > 0) {
            inv.setItem(16, createEnhancedQuantityItem(Material.REDSTONE_BLOCK, "§cTOUT (" + playerHas + "x)", playerHas, item, false));
        } else {
            inv.setItem(16, createDisabledItem(Material.BARRIER, "§cAucun item", "§7Vous ne possédez pas cet item"));
        }

        inv.setItem(22, createQuantityItem(Material.BARRIER, "§cRetour", -1));

        player.openInventory(inv);
    }

    private ItemStack createEnhancedQuantityItem(Material material, String name, int amount, ShopItem shopItem, boolean isBuying) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY_SERIALIZER.deserialize(name));
            List<String> lore = new ArrayList<>();
            lore.add("§7Quantité: §e" + amount);

            if (isBuying) {
                double totalCost = shopItem.getCurrentPrice() * amount;
                lore.add("§7Coût total: §6" + UXHelper.formatMoney(totalCost));
                if (amount <= shopItem.getCurrentStock()) {
                    lore.add("§a✓ Stock disponible");
                } else {
                    lore.add("§c✗ Stock insuffisant");
                }
            } else {
                double totalEarnings = shopItem.getCurrentPrice() * amount * 0.8;
                lore.add("§7Gain total: §6" + UXHelper.formatMoney(totalEarnings));
            }

            meta.lore(toLegacyComponents(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDisabledItem(Material material, String name, String reason) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY_SERIALIZER.deserialize(name));
            meta.lore(toLegacyComponents(List.of(reason)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createQuantityItem(Material material, String name, int amount) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY_SERIALIZER.deserialize(name));
            if (amount > 0) {
                meta.lore(toLegacyComponents(List.of("§7Quantité: §e" + amount)));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private int getFreeInventorySlots(Player player) {
        int free = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                free++;
            }
        }
        return free;
    }

    private int countItemInInventory(Player player, ShopItem shopItem) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == shopItem.getMaterial()) {
                // Se baser uniquement sur le type de matériau vanille
                // Peu importe le nom personnalisé ou la lore de l'item du shop
                count += item.getAmount();
            }
        }
        return count;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = getPlainViewTitle(event.getView());

        // Vérification de sécurité - joueur toujours connecté
        if (!player.isOnline()) {
            event.setCancelled(true);
            return;
        }

        // Sécuriser TOUS les menus du shop
        if (title.contains(MAIN_TITLE_PLAIN) ||
            title.startsWith(BUY_PREFIX_PLAIN) ||
            title.startsWith(SELL_PREFIX_PLAIN)) {

            // Bloquer TOUS les types de clics dangereux qui permettent de voler des items
            if (event.getClick().isShiftClick() ||
                event.getClick() == ClickType.DOUBLE_CLICK ||
                event.getClick() == ClickType.CREATIVE ||
                event.getClick() == ClickType.WINDOW_BORDER_LEFT ||
                event.getClick() == ClickType.WINDOW_BORDER_RIGHT ||
                event.getClick() == ClickType.DROP ||
                event.getClick() == ClickType.CONTROL_DROP ||
                event.getClick() == ClickType.NUMBER_KEY ||
                event.getClick() == ClickType.SWAP_OFFHAND ||
                event.getClick() == ClickType.MIDDLE) {
                event.setCancelled(true);
                return; // Ignorer complètement ces types de clics
            }

            // Empêcher TOUTE interaction avec l'inventaire du joueur depuis le menu
            if (event.getClickedInventory() == player.getInventory()) {
                event.setCancelled(true);
                return; // Bloquer les interactions avec l'inventaire du joueur
            }

            // Pour les clics LEFT et RIGHT légitimes dans les menus d'achat/vente,
            // annuler l'événement pour empêcher de prendre l'item
            if (title.startsWith(BUY_PREFIX_PLAIN) || title.startsWith(SELL_PREFIX_PLAIN)) {
                if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
                    event.setCancelled(true);
                    // Continue vers la logique du shop ci-dessous
                } else {
                    // Tous les autres types de clics sont bloqués
                    event.setCancelled(true);
                    return;
                }
            } else if (title.contains(MAIN_TITLE_PLAIN)) {
                // Pour le menu principal, annuler l'événement pour empêcher de prendre les items
                // mais laisser passer TOUS les clics LEFT et RIGHT vers la logique
                if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
                    event.setCancelled(true);
                    // Continue vers la logique du shop ci-dessous
                } else {
                    // Tous les autres types de clics sont bloqués
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (title.contains(MAIN_TITLE_PLAIN)) {
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().name().contains("GLASS_PANE")) return;

            int slot = event.getSlot();
            if (slot < 9 || slot >= 36) return; // Zones décoratives

            int itemIndex = slot - 9; // Ajuster pour la ligne décorative
            List<ShopItem> items = shopService.getCurrentItems();

            // Protection contre les index hors limites
            if (itemIndex < 0 || itemIndex >= items.size()) {
                UXHelper.sendErrorMessage(player, "Item non disponible!");
                player.closeInventory();
                return;
            }

            ShopItem item = items.get(itemIndex);

            // Vérification que l'item existe toujours
            if (item == null) {
                UXHelper.sendErrorMessage(player, "Cet item n'est plus disponible!");
                player.closeInventory();
                return;
            }

            try {
                if (event.getClick() == ClickType.LEFT) {
                    openSellMenu(player, item);
                } else if (event.getClick() == ClickType.RIGHT) {
                    openBuyMenu(player, item);
                }
            } catch (Exception e) {
                UXHelper.sendErrorMessage(player, "Erreur lors de l'ouverture du menu!");
                player.closeInventory();
                Main.getInstance().getLogger().severe("Erreur dans onInventoryClick: " + e.getMessage());
            }
        } else if (title.startsWith(BUY_PREFIX_PLAIN)) {
            handleBuyClick(player, event);
        } else if (title.startsWith(SELL_PREFIX_PLAIN)) {
            handleSellClick(player, event);
        }
    }

    private void handleBuyClick(Player player, InventoryClickEvent event) {
        try {
            if (event.getCurrentItem() == null) return;

            ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta == null) return;

            // Extraire correctement le nom de l'item depuis le titre
            String title = getPlainViewTitle(event.getView());
            String itemName = extractItemNameFromTitle(title, BUY_PREFIX_PLAIN);
            ShopItem shopItem = findItemByName(itemName);

            if (shopItem == null) {
                UXHelper.sendErrorMessage(player, "Cet item n'est plus disponible!");
                player.closeInventory();
                return;
            }

            if (event.getSlot() == 22) { // Bouton retour
                openShop(player);
                return;
            }

            int quantity = getQuantityFromLore(meta);
            if (quantity <= 0) return;

            // Protection contre les clics multiples rapides
            if (activeTransactions.contains(player.getUniqueId())) {
                UXHelper.sendErrorMessage(player, "Transaction en cours, veuillez patienter...");
                return;
            }

            activeTransactions.add(player.getUniqueId());

            try {
                if (shopService.processBuy(player, shopItem, quantity)) {
                    UXHelper.sendTransactionMessage(player, "ACHAT", shopItem.getName(), quantity, shopItem.getCurrentPrice() * quantity);
                    player.closeInventory();
                }
            } finally {
                activeTransactions.remove(player.getUniqueId());
            }
        } catch (Exception e) {
            UXHelper.sendErrorMessage(player, "Erreur lors de la transaction!");
            player.closeInventory();
            Main.getInstance().getLogger().severe("Erreur dans handleBuyClick pour " + player.getName() + ": " + e.getMessage());
        }
    }

    private void handleSellClick(Player player, InventoryClickEvent event) {
        try {
            if (event.getCurrentItem() == null) return;

            ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta == null) return;

            // Extraire correctement le nom de l'item depuis le titre
            String title = getPlainViewTitle(event.getView());
            String itemName = extractItemNameFromTitle(title, SELL_PREFIX_PLAIN);
            ShopItem shopItem = findItemByName(itemName);

            if (shopItem == null) {
                UXHelper.sendErrorMessage(player, "Cet item n'est plus disponible!");
                player.closeInventory();
                return;
            }

            if (event.getSlot() == 22) { // Bouton retour
                openShop(player);
                return;
            }

            int quantity = getQuantityFromLore(meta);
            if (quantity <= 0) return;

            // Protection contre les clics multiples rapides
            if (activeTransactions.contains(player.getUniqueId())) {
                UXHelper.sendErrorMessage(player, "Transaction en cours, veuillez patienter...");
                return;
            }

            activeTransactions.add(player.getUniqueId());

            try {
                if (shopService.processSell(player, shopItem, quantity)) {
                    double earnings = shopItem.getCurrentPrice() * quantity * 0.8;
                    UXHelper.sendTransactionMessage(player, "VENTE", shopItem.getName(), quantity, earnings);
                    player.closeInventory();
                }
            } finally {
                activeTransactions.remove(player.getUniqueId());
            }
        } catch (Exception e) {
            UXHelper.sendErrorMessage(player, "Erreur lors de la transaction!");
            player.closeInventory();
            Main.getInstance().getLogger().severe("Erreur dans handleSellClick pour " + player.getName() + ": " + e.getMessage());
        }
    }

    // Méthode pour extraire correctement le nom de l'item depuis le titre
    private String extractItemNameFromTitle(String title, String prefix) {
        // Supprimer tous les codes de couleur du titre
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "");

        // Trouver la position du préfixe et extraire le nom après
        int prefixIndex = cleanTitle.indexOf(prefix);
        if (prefixIndex != -1) {
            return cleanTitle.substring(prefixIndex + prefix.length()).trim();
        }

        // Fallback: essayer avec les anciens calculs
        if (title.contains("Acheter")) {
            return title.substring(title.indexOf("Acheter - ") + 10);
        } else if (title.contains("Vendre")) {
            return title.substring(title.indexOf("Vendre - ") + 9);
        }

        return "";
    }

    private ShopItem findItemByName(String name) {
        return shopService.getCurrentItems().stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElse(null);
    }

    private int getQuantityFromLore(ItemMeta meta) {
        List<Component> lore = meta.lore();
        if (lore == null) return 0;

        for (Component component : lore) {
            String line = LEGACY_SERIALIZER.serialize(component);
            if (line.startsWith("§7Quantité: §e")) {
                try {
                    return Integer.parseInt(line.substring(14));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private List<Component> toLegacyComponents(List<String> lines) {
        return lines.stream()
                .map(LEGACY_SERIALIZER::deserialize)
                .collect(Collectors.toList());
    }

    // Protection contre le glisser-déposer (drag) dans les menus du shop
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = getPlainViewTitle(event.getView());

        // Bloquer TOUS les drags dans les menus du shop
        if (title.contains(MAIN_TITLE_PLAIN) ||
            title.startsWith(BUY_PREFIX_PLAIN) ||
            title.startsWith(SELL_PREFIX_PLAIN)) {

            event.setCancelled(true);
        }
    }

    // Protection supplémentaire contre la fermeture d'inventaire avec items sur le curseur
    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = getPlainViewTitle(event.getView());

        // Vérifier si c'est un menu du shop
        if (title.contains(MAIN_TITLE_PLAIN) ||
            title.startsWith(BUY_PREFIX_PLAIN) ||
            title.startsWith(SELL_PREFIX_PLAIN)) {

            // Si le joueur a un item sur son curseur, le supprimer pour éviter le vol
            ItemStack cursorItem = player.getItemOnCursor();
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                player.setItemOnCursor(null);
            }
        }
    }

    private String getPlainViewTitle(org.bukkit.inventory.InventoryView view) {
        return PLAIN_TEXT_SERIALIZER.serialize(view.title());
    }
}
