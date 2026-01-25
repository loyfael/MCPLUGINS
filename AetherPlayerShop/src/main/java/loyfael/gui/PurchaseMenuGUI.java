package loyfael.gui;

import loyfael.Main;
import loyfael.model.Shop;
import loyfael.model.Transaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Gestionnaire du menu d'achat avec système d'incrémentation/décrémentation
 * Inspiré du menu de création de shop
 */
public class PurchaseMenuGUI implements Listener {

    private final Main plugin;
    private static Map<UUID, PurchaseSession> purchaseSessions;

    public PurchaseMenuGUI(Main plugin) {
        this.plugin = plugin;
        if (purchaseSessions == null) {
            purchaseSessions = new HashMap<>();
        }
    }

    /**
     * Ouvre le menu d'achat avec système d'incrémentation/décrémentation
     */
    public void openPurchaseMenu(@NotNull Player player, @NotNull Shop shop,
                                @NotNull Inventory chestInventory, int availableStock) {
        plugin.getLogger().info("[DEBUG] Ouverture du menu d'achat - Joueur: " + player.getName() +
            ", Stock: " + availableStock);

        // Créer un inventaire de menu EN 5 LIGNES (45 slots) avec entourage décoratif
        Inventory menu = org.bukkit.Bukkit.createInventory(null, 45,
            Component.text("🛒 Boutique - ", NamedTextColor.GOLD)
                .append(Component.text(shop.getItem().getDisplayName(), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));

        // Initialiser la session avec quantité de base à 1
        PurchaseSession session = new PurchaseSession(shop, chestInventory, availableStock, 1);
        purchaseSessions.put(player.getUniqueId(), session);

        // Remplir le menu
        setupPurchaseMenu(menu, player, session);

        // Ouvrir le menu
        player.openInventory(menu);
    }

    /**
     * Configure le menu d'achat épuré et intuitif en 5 lignes avec entourage décoratif
     */
    private void setupPurchaseMenu(@NotNull Inventory menu, @NotNull Player player, @NotNull PurchaseSession session) {
        Shop shop = session.shop;
        double unitPrice = shop.getPrice();
        double playerBalance = plugin.getEconomy().getBalance(player);
        int currentQuantity = session.selectedQuantity;
        double totalPrice = unitPrice * currentQuantity;
        boolean canAfford = playerBalance >= totalPrice;
        boolean hasStock = currentQuantity <= session.availableStock;

        // Effacer le menu
        menu.clear();

        // === ENTOURAGE DÉCORATIF EN VITRE ===
        // Ligne du haut (0-8)
        for (int i = 0; i <= 8; i++) {
            menu.setItem(i, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE, ""));
        }

        // Ligne du bas (36-44)
        for (int i = 36; i <= 44; i++) {
            menu.setItem(i, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE, ""));
        }

        // Côtés gauche et droit
        menu.setItem(9, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE, ""));
        menu.setItem(17, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE, ""));
        menu.setItem(18, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE, ""));
        menu.setItem(26, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE, ""));
        menu.setItem(27, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE, ""));
        menu.setItem(35, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE, ""));

        // === LIGNE 2 (10-16): CONTRÔLES DE QUANTITÉ ÉPURÉS ===

        // -10 (slot 11)
        createQuantityButton(menu, 11, Material.REDSTONE_BLOCK, "-- -10", -10,
            currentQuantity >= 10, "Enlève 10 articles");

        // -1 (slot 12)
        createQuantityButton(menu, 12, Material.REDSTONE, "- -1", -1,
            currentQuantity > 1, "Enlève 1 article");

        // RESET au centre (slot 13)
        ItemStack resetBtn = new ItemStack(Material.LAVA_BUCKET);
        var resetMeta = resetBtn.getItemMeta();
        if (resetMeta != null) {
            resetMeta.displayName(Component.text("🔄 RESET", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
            resetMeta.lore(List.of(
                Component.empty(),
                Component.text("Remet la quantité à 1", NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Cliquez pour réinitialiser !", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)
            ));
            resetBtn.setItemMeta(resetMeta);
        }
        menu.setItem(13, resetBtn);

        // +1 (slot 14)
        createQuantityButton(menu, 14, Material.EMERALD, "+ +1", 1,
            currentQuantity < session.availableStock, "Ajoute 1 article");

        // +10 (slot 15)
        createQuantityButton(menu, 15, Material.EMERALD_BLOCK, "++ +10", 10,
            currentQuantity + 10 <= session.availableStock, "Ajoute 10 articles");

        // === LIGNE 3 (19-25): INFORMATIONS PRINCIPALES ===

        // Informations sur l'argent (slot 20)
        ItemStack moneyDisplay = new ItemStack(Material.GOLD_INGOT);
        var moneyMeta = moneyDisplay.getItemMeta();
        if (moneyMeta != null) {
            moneyMeta.displayName(Component.text("💰 Votre Argent", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
            List<Component> moneyLore = new ArrayList<>();
            moneyLore.add(Component.empty());
            moneyLore.add(Component.text("Solde: " + String.format("%.2f", playerBalance) + " $", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
            if (canAfford) {
                moneyLore.add(Component.text("✅ Assez d'argent", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
                moneyLore.add(Component.text("Reste: " + String.format("%.2f", playerBalance - totalPrice) + " $", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            } else {
                moneyLore.add(Component.text("❌ Argent insuffisant", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
                moneyLore.add(Component.text("Manque: " + String.format("%.2f", totalPrice - playerBalance) + " $", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            }
            moneyMeta.lore(moneyLore);
            moneyDisplay.setItemMeta(moneyMeta);
        }
        menu.setItem(20, moneyDisplay);

        // ARTICLE PRINCIPAL (slot 22) - Un seul bloc au centre
        ItemStack mainItem = shop.getItem().toItemStack();
        mainItem.setAmount(Math.min(currentQuantity, 64));
        var mainMeta = mainItem.getItemMeta();
        if (mainMeta != null) {
            mainMeta.displayName(Component.text("🎯 " + shop.getItem().getDisplayName(), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

            List<Component> mainLore = new ArrayList<>();
            mainLore.add(Component.empty());
            mainLore.add(Component.text("═══ VOTRE COMMANDE ═══", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
            mainLore.add(Component.empty());

            mainLore.add(Component.text("📊 Quantité: " + currentQuantity, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
            mainLore.add(Component.text("💲 Prix unitaire: " + String.format("%.2f", unitPrice) + " $", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
            mainLore.add(Component.text("💰 PRIX TOTAL: " + String.format("%.2f", totalPrice) + " $",
                canAfford ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
            mainLore.add(Component.empty());

            // État simple et clair
            if (!canAfford) {
                mainLore.add(Component.text("💸 ARGENT INSUFFISANT", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            } else if (!hasStock) {
                mainLore.add(Component.text("📦 STOCK INSUFFISANT", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            } else {
                mainLore.add(Component.text("✅ PRÊT À ACHETER !", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            }

            mainMeta.lore(mainLore);
            mainItem.setItemMeta(mainMeta);
        }
        menu.setItem(22, mainItem);

        // Informations sur le stock (slot 24)
        Material stockMaterial = session.availableStock > 10 ? Material.CHEST :
                                session.availableStock > 0 ? Material.ENDER_CHEST : Material.BARRIER;
        ItemStack stockDisplay = new ItemStack(stockMaterial);
        var stockMeta = stockDisplay.getItemMeta();
        if (stockMeta != null) {
            stockMeta.displayName(Component.text("📦 Stock: " + session.availableStock, NamedTextColor.BLUE)
                .decoration(TextDecoration.ITALIC, false));
            List<Component> stockLore = new ArrayList<>();
            stockLore.add(Component.empty());
            stockLore.add(Component.text("Stock disponible: " + session.availableStock, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
            stockLore.add(Component.text("Propriétaire: " + shop.getOwnerName(), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
            stockMeta.lore(stockLore);
            stockDisplay.setItemMeta(stockMeta);
        }
        menu.setItem(24, stockDisplay);

        // === LIGNE 4 (28-34): ACTIONS FINALES ===

        // ANNULER (slot 30)
        ItemStack cancelBtn = new ItemStack(Material.BARRIER);
        var cancelMeta = cancelBtn.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("❌ ANNULER", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
            cancelMeta.lore(List.of(
                Component.empty(),
                Component.text("Fermer sans acheter", NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Cliquez pour sortir", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)
            ));
            cancelBtn.setItemMeta(cancelMeta);
        }
        menu.setItem(30, cancelBtn);

        // ACHETER (slot 31) - LE BOUTON PRINCIPAL
        Material confirmMaterial = canAfford && hasStock ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        ItemStack confirmBtn = new ItemStack(confirmMaterial);
        var confirmMeta = confirmBtn.getItemMeta();
        if (confirmMeta != null) {
            if (canAfford && hasStock) {
                confirmMeta.displayName(Component.text("💳 ACHETER", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
                List<Component> confirmLore = new ArrayList<>();
                confirmLore.add(Component.empty());
                confirmLore.add(Component.text("• " + currentQuantity + "x " + shop.getItem().getDisplayName(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
                confirmLore.add(Component.text("• Total: " + String.format("%.2f", totalPrice) + " $", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
                confirmLore.add(Component.empty());
                confirmLore.add(Component.text("👆 CLIQUER POUR ACHETER !", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
                confirmMeta.lore(confirmLore);
            } else if (!hasStock) {
                confirmMeta.displayName(Component.text("❌ STOCK INSUFFISANT", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
                confirmMeta.lore(List.of(
                    Component.empty(),
                    Component.text("Réduisez la quantité !", NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.text("Stock: " + session.availableStock + " / Demandé: " + currentQuantity, NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
                ));
            } else {
                confirmMeta.displayName(Component.text("❌ ARGENT INSUFFISANT", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
                confirmMeta.lore(List.of(
                    Component.empty(),
                    Component.text("Vous avez besoin de plus d'argent !", NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.text("Manque: " + String.format("%.2f", totalPrice - playerBalance) + " $", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
                ));
            }
            confirmBtn.setItemMeta(confirmMeta);
        }
        menu.setItem(31, confirmBtn);

        // MAX (slot 32) - Bouton pour acheter le maximum possible
        ItemStack maxBtn = new ItemStack(Material.PURPLE_CONCRETE);
        var maxMeta = maxBtn.getItemMeta();
        if (maxMeta != null) {
            int maxAffordable = (int) Math.floor(playerBalance / unitPrice);
            int actualMax = Math.min(maxAffordable, session.availableStock);

            maxMeta.displayName(Component.text("📈 MAX", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
            List<Component> maxLore = new ArrayList<>();
            maxLore.add(Component.empty());
            maxLore.add(Component.text("Maximum possible: " + actualMax, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
            maxLore.add(Component.text("Coût: " + String.format("%.2f", actualMax * unitPrice) + " $", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
            maxLore.add(Component.empty());
            maxLore.add(Component.text("Cliquez pour définir au maximum !", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
            maxMeta.lore(maxLore);
            maxBtn.setItemMeta(maxMeta);
        }
        menu.setItem(32, maxBtn);
    }

    /**
     * Crée un bouton de contrôle de quantité magnifique
     */
    private void createQuantityButton(Inventory menu, int slot, Material material, String name, int increment, boolean enabled, String description) {
        if (!enabled) {
            material = Material.GRAY_CONCRETE;
        }

        ItemStack button = new ItemStack(material);
        var meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, enabled ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            if (enabled) {
                lore.add(Component.text(description, NamedTextColor.WHITE));
                lore.add(Component.empty());
                lore.add(Component.text("👆 CLIQUEZ !", NamedTextColor.YELLOW, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("Non disponible", NamedTextColor.DARK_GRAY));
            }
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        menu.setItem(slot, button);
    }

    /**
     * Crée du verre décoratif coloré
     */
    private ItemStack createDecorativeGlass(Material material, String name) {
        ItemStack glass = new ItemStack(material);
        var meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            glass.setItemMeta(meta);
        }
        return glass;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleString = title.toString();
        if (!titleString.contains("Boutique")) return;

        event.setCancelled(true);

        PurchaseSession session = purchaseSessions.get(player.getUniqueId());
        if (session == null || session.isExpired()) {
            player.sendMessage("§c⏰ Session d'achat expirée. Cliquez à nouveau sur le panneau pour recommencer !");
            player.closeInventory();
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        int slot = event.getSlot();

        // === CONTRÔLES DE QUANTITÉ ÉPURÉS (ligne 2: 10-16) ===

        // -10 (slot 11)
        if (slot == 11 && clickedItem.getType() == Material.REDSTONE_BLOCK) {
            session.adjustQuantity(-10);
            setupPurchaseMenu(event.getInventory(), player, session);
            player.sendMessage("§e📉 -10 articles (Total: " + session.selectedQuantity + ")");
        }
        // -1 (slot 12)
        else if (slot == 12 && clickedItem.getType() == Material.REDSTONE) {
            session.adjustQuantity(-1);
            setupPurchaseMenu(event.getInventory(), player, session);
            player.sendMessage("§e📉 -1 article (Total: " + session.selectedQuantity + ")");
        }
        // RESET (slot 13)
        else if (slot == 13 && clickedItem.getType() == Material.LAVA_BUCKET) {
            session.selectedQuantity = 1;
            setupPurchaseMenu(event.getInventory(), player, session);
            player.sendMessage("§e🔄 Quantité remise à 1");
        }
        // +1 (slot 14)
        else if (slot == 14 && clickedItem.getType() == Material.EMERALD) {
            session.adjustQuantity(1);
            setupPurchaseMenu(event.getInventory(), player, session);
            player.sendMessage("§e📈 +1 article (Total: " + session.selectedQuantity + ")");
        }
        // +10 (slot 15)
        else if (slot == 15 && clickedItem.getType() == Material.EMERALD_BLOCK) {
            session.adjustQuantity(10);
            setupPurchaseMenu(event.getInventory(), player, session);
            player.sendMessage("§e📈 +10 articles (Total: " + session.selectedQuantity + ")");
        }

        // === ACTIONS FINALES (ligne 4: 28-34) ===

        // ANNULER (slot 30)
        else if (slot == 30 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            purchaseSessions.remove(player.getUniqueId());
            player.sendMessage("§c❌ Achat annulé");
        }
        // ACHETER (slot 31) - LE BOUTON PRINCIPAL
        else if (slot == 31 && clickedItem.getType() == Material.EMERALD_BLOCK) {
            player.sendMessage("§a💳 Traitement de votre achat en cours...");
            executePurchase(player, session);
        }
        // MAX (slot 32)
        else if (slot == 32 && clickedItem.getType() == Material.CRYING_OBSIDIAN) {
            double playerBalance = plugin.getEconomy().getBalance(player);
            double unitPrice = session.shop.getPrice();
            int maxAffordable = (int) Math.floor(playerBalance / unitPrice);
            int actualMax = Math.min(maxAffordable, session.availableStock);

            if (actualMax > 0) {
                session.selectedQuantity = actualMax;
                setupPurchaseMenu(event.getInventory(), player, session);
                player.sendMessage("§e📈 Quantité au MAXIMUM: " + actualMax + " articles");
                player.sendMessage("§7💰 Coût total: " + String.format("%.2f", actualMax * unitPrice) + "$");
            } else {
                player.sendMessage("§c❌ Impossible d'acheter même 1 article !");
                player.sendMessage("§7💸 Vous avez besoin de " + String.format("%.2f", unitPrice) + "$ minimum");
            }
        }

        // === ÉLÉMENTS INFORMATIFS INTERACTIFS (ligne 3: 19-25) ===

        else if (slot == 20) {
            // Clic sur l'affichage de l'argent
            double balance = plugin.getEconomy().getBalance(player);
            player.sendMessage("§e💰 Solde détaillé:");
            player.sendMessage("§7├─ Argent actuel: §e" + String.format("%.2f", balance) + "$");

            double totalCost = session.selectedQuantity * session.shop.getPrice();
            if (balance >= totalCost) {
                player.sendMessage("§7├─ Après cet achat: §a" + String.format("%.2f", balance - totalCost) + "$");
                player.sendMessage("§7└─ §a✅ Vous pouvez acheter !");
            } else {
                player.sendMessage("§7├─ Coût de cet achat: §c" + String.format("%.2f", totalCost) + "$");
                player.sendMessage("§7└─ §c❌ Argent insuffisant !");
            }
        } else if (slot == 22) {
            // Clic sur l'article principal
            double totalPrice = session.shop.getPrice() * session.selectedQuantity;
            player.sendMessage("§e🎯 Résumé de votre commande:");
            player.sendMessage("§7├─ Article: §e" + session.shop.getItem().getDisplayName());
            player.sendMessage("§7├─ Quantité: §e" + session.selectedQuantity + " unités");
            player.sendMessage("§7├─ Prix unitaire: §e" + String.format("%.2f", session.shop.getPrice()) + "$");
            player.sendMessage("§7└─ §e💰 Prix total: " + String.format("%.2f", totalPrice) + "$");
        } else if (slot == 24) {
            // Clic sur le stock
            player.sendMessage("§e📦 Informations sur le stock:");
            player.sendMessage("§7├─ Stock disponible: §e" + session.availableStock + " unités");
            player.sendMessage("§7├─ Propriétaire: §e" + session.shop.getOwnerName());
            player.sendMessage("§7└─ §e" + (session.availableStock > 10 ? "Stock excellent !" : session.availableStock > 5 ? "Stock correct" : "Stock limité"));
        }
    }

    /**
     * Exécute l'achat de la quantité sélectionnée
     */
    private void executePurchase(@NotNull Player player, @NotNull PurchaseSession session) {
        try {
            Shop shop = session.shop;
            Inventory chestInventory = session.chestInventory;
            int quantity = session.selectedQuantity;

            double totalPrice = shop.getPrice() * quantity;

            // Vérifications finales
            if (plugin.getEconomy().getBalance(player) < totalPrice) {
                player.sendMessage("§cVous n'avez pas assez d'argent pour cet achat !");
                return;
            }

            int actualStock = countItemsInInventory(chestInventory, shop.getItem().toItemStack());
            if (quantity > actualStock) {
                player.sendMessage("§cStock insuffisant ! Stock disponible: " + actualStock);
                return;
            }

            // Retirer les items du coffre
            if (!removeItemsFromInventory(chestInventory, shop.getItem().toItemStack(), quantity)) {
                player.sendMessage("§cErreur lors du retrait des items du stock !");
                return;
            }

            // Effectuer le paiement
            EconomyResponse economyResponse = plugin.getEconomy().withdrawPlayer(player, totalPrice);
            if (!economyResponse.transactionSuccess()) {
                player.sendMessage("§cErreur lors du paiement !");
                addItemsToInventory(chestInventory, shop.getItem().toItemStack(), quantity);
                return;
            }

            // Donner les items au joueur
            ItemStack itemToGive = shop.getItem().toItemStack();
            giveItemsToPlayer(player, itemToGive, quantity);

            // Payer le propriétaire du shop
            org.bukkit.OfflinePlayer shopOwner = org.bukkit.Bukkit.getOfflinePlayer(shop.getOwnerUUID());
            if (plugin.getEconomy().hasAccount(shopOwner)) {
                plugin.getEconomy().depositPlayer(shopOwner, totalPrice);
            }

            // Enregistrer la transaction
            try {
                Transaction transaction = new Transaction(
                    shop.getId(),
                    player.getUniqueId(),
                    shop.getOwnerUUID(),
                    Transaction.TransactionType.BUY,
                    shop.getItem(),
                    totalPrice,
                    quantity
                );
                plugin.getLogger().info("[DEBUG] Transaction créée: " + transaction.getId());
            } catch (Exception transactionError) {
                plugin.getLogger().warning("Impossible de créer la transaction: " + transactionError.getMessage());
            }

            // Message de confirmation
            player.sendMessage("§a§l✓ Achat effectué avec succès !");
            player.sendMessage("§7Quantité: §e" + quantity);
            player.sendMessage("§7Prix total: §e" + String.format("%.2f", totalPrice) + "◎");
            player.sendMessage("§7Argent restant: §e" + String.format("%.2f", plugin.getEconomy().getBalance(player)) + "◎");

            // Fermer le menu et nettoyer
            player.closeInventory();
            purchaseSessions.remove(player.getUniqueId());

        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de l'achat: " + e.getMessage());
            player.sendMessage("§cUne erreur est survenue lors de l'achat !");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleString = title.toString();
        if (titleString.contains("Achat")) {
            // Nettoyer la session après un délai
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                PurchaseSession session = purchaseSessions.get(player.getUniqueId());
                if (session != null && session.isExpired()) {
                    purchaseSessions.remove(player.getUniqueId());
                }
            }, 60L);
        }
    }

    /**
     * Compte le nombre d'items dans l'inventaire
     */
    private int countItemsInInventory(Inventory inventory, ItemStack targetItem) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.isSimilar(targetItem)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Retire des items de l'inventaire
     */
    private boolean removeItemsFromInventory(Inventory inventory, ItemStack targetItem, int quantity) {
        int remaining = quantity;
        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.isSimilar(targetItem)) {
                int available = item.getAmount();
                if (available <= remaining) {
                    inventory.setItem(i, null);
                    remaining -= available;
                } else {
                    item.setAmount(available - remaining);
                    remaining = 0;
                }
            }
        }
        return remaining == 0;
    }

    /**
     * Ajoute des items à l'inventaire
     */
    private void addItemsToInventory(Inventory inventory, ItemStack targetItem, int quantity) {
        int remaining = quantity;
        ItemStack itemToAdd = targetItem.clone();

        // Essayer d'abord de remplir les stacks existants
        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.isSimilar(targetItem)) {
                int maxStack = item.getMaxStackSize();
                int current = item.getAmount();
                int canAdd = Math.min(remaining, maxStack - current);
                if (canAdd > 0) {
                    item.setAmount(current + canAdd);
                    remaining -= canAdd;
                }
            }
        }

        // Puis créer de nouveaux stacks
        while (remaining > 0) {
            int stackSize = Math.min(remaining, itemToAdd.getMaxStackSize());
            itemToAdd.setAmount(stackSize);
            inventory.addItem(itemToAdd.clone());
            remaining -= stackSize;
        }
    }

    /**
     * Donne des items au joueur
     */
    private void giveItemsToPlayer(Player player, ItemStack item, int quantity) {
        int remaining = quantity;
        ItemStack itemToGive = item.clone();

        while (remaining > 0) {
            int stackSize = Math.min(remaining, item.getMaxStackSize());
            itemToGive.setAmount(stackSize);

            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(itemToGive.clone());
            if (!leftOver.isEmpty()) {
                for (ItemStack leftOverItem : leftOver.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftOverItem);
                }
                player.sendMessage("§6Inventaire plein ! Les items ont été jetés au sol.");
            }
            remaining -= stackSize;
        }
    }

    /**
     * Session d'achat avec quantité sélectionnée
     */
    public static class PurchaseSession {
        public final Shop shop;
        public final Inventory chestInventory;
        public final int availableStock;
        public int selectedQuantity;
        private final long creationTime;

        public PurchaseSession(Shop shop, Inventory chestInventory, int availableStock, int initialQuantity) {
            this.shop = shop;
            this.chestInventory = chestInventory;
            this.availableStock = availableStock;
            this.selectedQuantity = initialQuantity;
            this.creationTime = System.currentTimeMillis();
        }

        public void adjustQuantity(int adjustment) {
            selectedQuantity = Math.max(1, Math.min(selectedQuantity + adjustment, availableStock));
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - creationTime > 300000; // 5 minutes
        }
    }
}
