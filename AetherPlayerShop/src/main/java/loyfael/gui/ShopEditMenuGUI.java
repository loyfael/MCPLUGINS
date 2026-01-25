package loyfael.gui;

import loyfael.Main;
import loyfael.model.Shop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
 * Menu graphique d'édition des shops - Interface UX épurée et intuitive
 * Remplace le système de conversation par chat
 */
public class ShopEditMenuGUI implements Listener {

    private final Main plugin;
    private static Map<UUID, EditSession> editSessions;

    public ShopEditMenuGUI(Main plugin) {
        this.plugin = plugin;
        if (editSessions == null) {
            editSessions = new HashMap<>();
        }
    }

    /**
     * Ouvre le menu d'édition pour un shop donné
     */
    public void openEditMenu(@NotNull Player player, @NotNull Shop shop) {
        plugin.getLogger().info("[DEBUG] Ouverture du menu d'édition - Joueur: " + player.getName() +
            ", Shop: " + shop.getId());

        // Vérifier que le joueur est bien propriétaire
        if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§cVous n'êtes pas le propriétaire de ce shop !");
            return;
        }

        // Créer un inventaire de menu EN 5 LIGNES (45 slots) avec entourage décoratif
        Inventory menu = org.bukkit.Bukkit.createInventory(null, 45,
            Component.text("⚙ Édition - ", NamedTextColor.GOLD)
                .append(Component.text(shop.getItem().getDisplayName(), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));

        // Initialiser la session d'édition
        EditSession session = new EditSession(shop, shop.getPrice());
        editSessions.put(player.getUniqueId(), session);

        // Remplir le menu
        setupEditMenu(menu, player, session);

        // Ouvrir le menu
        player.openInventory(menu);
    }

    /**
     * Configure le menu d'édition épuré et intuitive
     */
    private void setupEditMenu(@NotNull Inventory menu, @NotNull Player player, @NotNull EditSession session) {
        Shop shop = session.shop;
        double currentPrice = session.tempPrice;
        double originalPrice = shop.getPrice();
        boolean priceChanged = Math.abs(currentPrice - originalPrice) > 0.01;

        // Effacer le menu
        menu.clear();

        // === ENTOURAGE DÉCORATIF EN VITRE - SIMPLIFIÉ ===
        // Ligne du haut (0-8)
        for (int i = 0; i <= 8; i++) {
            menu.setItem(i, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        }

        // Ligne du bas (36-44)
        for (int i = 36; i <= 44; i++) {
            menu.setItem(i, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        }

        // Côtés gauche et droit
        menu.setItem(9, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        menu.setItem(17, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        menu.setItem(18, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        menu.setItem(26, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        menu.setItem(27, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        menu.setItem(35, createDecorativeGlass(Material.LIGHT_BLUE_STAINED_GLASS_PANE));

        // === LIGNE 2 (10-16): INFORMATIONS CENTRALES ===

        // Article du shop (slot 13) - Centre
        ItemStack shopItem = shop.getItem().toItemStack();
        var shopMeta = shopItem.getItemMeta();
        if (shopMeta != null) {
            shopMeta.displayName(Component.text("🏪 " + shop.getItem().getDisplayName(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

            List<Component> shopLore = new ArrayList<>();
            shopLore.add(Component.empty());
            shopLore.add(Component.text("═══ MODIFICATION DU PRIX ═══", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
            shopLore.add(Component.empty());
            shopLore.add(Component.text("📊 Stock: " + shop.getStock(), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
            shopLore.add(Component.text("💰 Prix actuel: " + String.format("%.2f", originalPrice) + "$", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

            if (priceChanged) {
                shopLore.add(Component.text("💲 Nouveau prix: " + String.format("%.2f", currentPrice) + "$",
                    currentPrice > originalPrice ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            }

            shopLore.add(Component.empty());
            shopLore.add(Component.text("👑 " + shop.getOwnerName(), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));

            shopMeta.lore(shopLore);
            shopItem.setItemMeta(shopMeta);
        }
        menu.setItem(13, shopItem);

        // === LIGNE 3 (19-25): CONTRÔLES DE PRIX INTUITIFS AVEC LOGIQUE OR/FER ===

        // Contrôles de diminution avec gradation fer (moins précieux)
        createPriceButton(menu, 19, Material.IRON_BLOCK, "-- -10◎", -10.0, "Diminue de 10◎");
        createPriceButton(menu, 20, Material.IRON_INGOT, "- -1◎", -1.0, "Diminue de 1◎");
        createPriceButton(menu, 21, Material.IRON_NUGGET, "-0.1◎", -0.1, "Diminue de 0.1◎");

        // RESET au centre avec un item symbolique (slot 22)
        ItemStack resetBtn = new ItemStack(Material.RECOVERY_COMPASS);
        var resetMeta = resetBtn.getItemMeta();
        if (resetMeta != null) {
            resetMeta.displayName(Component.text("🔄 RESET", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
            resetMeta.lore(List.of(
                Component.empty(),
                Component.text("Remet le prix original", NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text(String.format("%.2f◎", originalPrice), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)
            ));
            resetBtn.setItemMeta(resetMeta);
        }
        menu.setItem(22, resetBtn);

        // Contrôles d'augmentation avec gradation or (précieux/richesse)
        createPriceButton(menu, 23, Material.GOLD_NUGGET, "+0.1◎", 0.1, "Augmente de 0.1◎");
        createPriceButton(menu, 24, Material.GOLD_INGOT, "+ +1◎", 1.0, "Augmente de 1◎");
        createPriceButton(menu, 25, Material.GOLD_BLOCK, "++ +10◎", 10.0, "Augmente de 10◎");

        // === LIGNE 4 (28-34): CONTRÔLES AVANCÉS AVEC ITEMS SYMBOLIQUES ===

        createPriceButton(menu, 29, Material.FIRE_CHARGE, "-100◎", -100.0, "Diminue de 100◎");
        createPriceButton(menu, 30, Material.SHEARS, "÷ 2", 0, "Divise le prix par 2");
        createPriceButton(menu, 32, Material.NETHER_STAR, "+100◎", 100.0, "Augmente de 100◎");

        // === LIGNE 5 (37-43): ACTIONS FINALES SIMPLIFIÉES ===

        // ANNULER (slot 39)
        ItemStack cancelBtn = new ItemStack(Material.BARRIER);
        var cancelMeta = cancelBtn.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("❌ Annuler", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
            cancelMeta.lore(List.of(
                Component.empty(),
                Component.text("Fermer sans sauvegarder", NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            ));
            cancelBtn.setItemMeta(cancelMeta);
        }
        menu.setItem(39, cancelBtn);

        // SAUVEGARDER (slot 40) - LE BOUTON PRINCIPAL
        Material saveMaterial = (priceChanged && currentPrice > 0) ? Material.EMERALD_BLOCK : Material.GRAY_CONCRETE;
        ItemStack saveBtn = new ItemStack(saveMaterial);
        var saveMeta = saveBtn.getItemMeta();
        if (saveMeta != null) {
            if (priceChanged && currentPrice > 0) {
                saveMeta.displayName(Component.text("💾 SAUVEGARDER", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
                List<Component> saveLore = new ArrayList<>();
                saveLore.add(Component.empty());
                saveLore.add(Component.text("Nouveau prix: " + String.format("%.2f", currentPrice) + "$",
                    NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                saveLore.add(Component.empty());
                saveLore.add(Component.text("👆 CLIQUER POUR CONFIRMER", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
                saveMeta.lore(saveLore);
            } else {
                saveMaterial = Material.GRAY_CONCRETE;
                saveMeta.displayName(Component.text("⚪ Aucune modification", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
                saveMeta.lore(List.of(
                    Component.empty(),
                    Component.text("Modifiez le prix pour sauvegarder", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                ));
            }
            saveBtn.setItemMeta(saveMeta);
        }
        menu.setItem(40, saveBtn);

        // SUPPRIMER (slot 41) - Plus discret
        ItemStack deleteBtn = new ItemStack(session.pendingDeletion ? Material.TNT : Material.REDSTONE);
        var deleteMeta = deleteBtn.getItemMeta();
        if (deleteMeta != null) {
            if (session.pendingDeletion) {
                deleteMeta.displayName(Component.text("💥 CONFIRMER SUPPRESSION", NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false));
                deleteMeta.lore(List.of(
                    Component.empty(),
                    Component.text("⚠ CLIQUEZ À NOUVEAU POUR CONFIRMER", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.text("Le shop sera définitivement supprimé", NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)
                ));
            } else {
                deleteMeta.displayName(Component.text("🗑 Supprimer", NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false));
                deleteMeta.lore(List.of(
                    Component.empty(),
                    Component.text("Supprimer définitivement ce shop", NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.text("Stock: " + shop.getStock(), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                ));
            }
            deleteBtn.setItemMeta(deleteMeta);
        }
        menu.setItem(41, deleteBtn);
    }

    /**
     * Crée un bouton de modification du prix
     */
    private void createPriceButton(Inventory menu, int slot, Material material, String name, double priceChange, String description) {
        ItemStack button = new ItemStack(material);
        var meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text(description, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
            if (priceChange > 0) {
                lore.add(Component.text("+" + String.format("%.2f", priceChange) + " $", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text(String.format("%.2f", priceChange) + " $", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("👆 CLIQUEZ !", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            button.setItemMeta(meta);
        }
        menu.setItem(slot, button);
    }

    /**
     * Crée du verre décoratif coloré
     */
    private ItemStack createDecorativeGlass(Material material) {
        ItemStack glass = new ItemStack(material);
        var meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(""));
            glass.setItemMeta(meta);
        }
        return glass;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleString = title.toString();
        if (!titleString.contains("Édition")) return;

        event.setCancelled(true);

        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null || session.isExpired()) {
            player.sendMessage("§c⏰ Session d'édition expirée. Rouvrez le menu d'édition !");
            player.closeInventory();
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        int slot = event.getSlot();

        // === CONTRÔLES DE PRIX AVEC NOUVEAUX ITEMS ===

        // Ligne 3 : Contrôles principaux avec gradation fer/or (19-25)
        if (slot == 19 && clickedItem.getType() == Material.IRON_BLOCK) {
            session.adjustPrice(-10.0);
            setupEditMenu(event.getInventory(), player, session);
            player.sendMessage("§e📉 -10◎ (Prix: " + String.format("%.2f", session.tempPrice) + "◎)");
        } else if (slot == 20 && clickedItem.getType() == Material.IRON_INGOT) {
            session.adjustPrice(-1.0);
            setupEditMenu(event.getInventory(), player, session);
            player.sendMessage("§e📉 -1◎ (Prix: " + String.format("%.2f", session.tempPrice) + "◎)");
        } else if (slot == 21 && clickedItem.getType() == Material.IRON_NUGGET) {
            session.adjustPrice(-0.1);
            setupEditMenu(event.getInventory(), player, session);
            player.sendMessage("§e📉 -0.1◎ (Prix: " + String.format("%.2f", session.tempPrice) + "◎)");
        }
        // RESET avec Recovery Compass (slot 22)
        else if (slot == 22 && clickedItem.getType() == Material.RECOVERY_COMPASS) {
            session.tempPrice = session.shop.getPrice();
            setupEditMenu(event.getInventory(), player, session);
            player.sendMessage("§e🔄 Prix restauré: " + String.format("%.2f", session.tempPrice) + "◎");
        }
        else if (slot == 23 && clickedItem.getType() == Material.GOLD_NUGGET) {
            session.adjustPrice(0.1);
            setupEditMenu(event.getInventory(), player, session);
            player.sendMessage("§e📈 +0.1◎ (Prix: " + String.format("%.2f", session.tempPrice) + "◎)");
        } else if (slot == 24 && clickedItem.getType() == Material.GOLD_INGOT) {
            session.adjustPrice(1.0);
            setupEditMenu(event.getInventory(), player, session);
            player.sendMessage("§e📈 +1◎ (Prix: " + String.format("%.2f", session.tempPrice) + "◎)");
        } else if (slot == 25 && clickedItem.getType() == Material.GOLD_BLOCK) {
            session.adjustPrice(10.0);
            setupEditMenu(event.getInventory(), player, session);
            player.sendMessage("§e📈 +10◎ (Prix: " + String.format("%.2f", session.tempPrice) + "◎)");
        }

        // Ligne 4 : Contrôles supplémentaires avec items symboliques (29, 30, 32)
        else if (slot == 29 && clickedItem.getType() == Material.FIRE_CHARGE) {
            session.adjustPrice(-100.0);
            setupEditMenu(event.getInventory(), player, session);
            player.sendMessage("§e📉 -100◎ (Prix: " + String.format("%.2f", session.tempPrice) + "◎)");
        } else if (slot == 30 && clickedItem.getType() == Material.SHEARS) {
            session.tempPrice = Math.max(0.01, session.tempPrice / 2);
            setupEditMenu(event.getInventory(), player, session);
            player.sendMessage("§e💰 Prix divisé par 2 (Prix: " + String.format("%.2f", session.tempPrice) + "◎)");
        } else if (slot == 32 && clickedItem.getType() == Material.NETHER_STAR) {
            session.adjustPrice(100.0);
            setupEditMenu(event.getInventory(), player, session);
            player.sendMessage("§e📈 +100◎ (Prix: " + String.format("%.2f", session.tempPrice) + "◎)");
        }

        // === ACTIONS FINALES (39-41) ===

        // ANNULER (slot 39)
        else if (slot == 39 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            editSessions.remove(player.getUniqueId());
            player.sendMessage("§c❌ Édition annulée");
        }
        // SAUVEGARDER (slot 40)
        else if (slot == 40 && clickedItem.getType() == Material.EMERALD_BLOCK) {
            player.sendMessage("§a💾 Sauvegarde en cours...");
            saveShopChanges(player, session);
        }
        // SUPPRIMER (slot 41)
        else if (slot == 41) {
            if (session.pendingDeletion && clickedItem.getType() == Material.TNT) {
                // Confirmation de suppression
                executeShopDeletion(player, session);
            } else if (!session.pendingDeletion && clickedItem.getType() == Material.REDSTONE) {
                // Première demande de suppression
                confirmShopDeletion(player, session);
            }
        }
    }

    /**
     * Sauvegarde les modifications du shop
     */
    private void saveShopChanges(@NotNull Player player, @NotNull EditSession session) {
        Shop shop = session.shop;
        double oldPrice = shop.getPrice();
        double newPrice = session.tempPrice;

        if (newPrice <= 0) {
            player.sendMessage("§cErreur: Le prix doit être supérieur à 0 !");
            return;
        }

        if (Math.abs(oldPrice - newPrice) < 0.01) {
            player.sendMessage("§eAucune modification à appliquer - Prix identique !");
            return;
        }

        // Utiliser la méthode updateShopPrice au lieu de créer un nouvel objet
        plugin.getShopManager().updateShopPrice(shop.getId(), newPrice)
            .thenAccept(success -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    // Mettre à jour le prix dans l'objet shop local
                    shop.setPrice(newPrice);

                    // ✨ MISE À JOUR AUTOMATIQUE DE LA PANCARTE ✨
                    updateShopSignDisplay(shop, player);

                    player.sendMessage("§a§l✓ Shop modifié avec succès !");
                    player.sendMessage("§7Ancien prix: §c" + String.format("%.2f", oldPrice) + "$");
                    player.sendMessage("§7Nouveau prix: §a" + String.format("%.2f", newPrice) + "$");
                    player.sendMessage("§a✨ Pancarte mise à jour automatiquement !");

                    player.closeInventory();
                    editSessions.remove(player.getUniqueId());

                    plugin.getLogger().info("[SUCCESS] Shop " + shop.getId() + " modifié par " + player.getName() +
                        " - Prix: " + oldPrice + "$ → " + newPrice + "$ - Pancarte mise à jour");
                } else {
                    player.sendMessage("§cErreur lors de la sauvegarde des modifications !");
                    plugin.getLogger().severe("[ERROR] Impossible de sauvegarder les modifications du shop " + shop.getId());
                }
            }));
    }

    /**
     * Met à jour automatiquement l'affichage de la pancarte après modification
     */
    private void updateShopSignDisplay(@NotNull Shop shop, @NotNull Player player) {
        try {
            // Construire la location du shop
            org.bukkit.World world = plugin.getServer().getWorld(shop.getLocation().getWorld().getName());
            if (world == null) {
                plugin.getLogger().warning("[WARNING] Monde introuvable pour le shop " + shop.getId());
                return;
            }

            org.bukkit.Location shopLocation = new org.bukkit.Location(
                world,
                shop.getLocation().getX(),
                shop.getLocation().getY(),
                shop.getLocation().getZ()
            );

            // Trouver la pancarte à cette position ou adjacente
            org.bukkit.block.Block signBlock = shopLocation.getBlock();
            if (!(signBlock.getState() instanceof org.bukkit.block.Sign)) {
                // Chercher dans les blocs adjacents
                signBlock = findAdjacentShopSign(shopLocation.getBlock());
            }

            if (signBlock != null && signBlock.getState() instanceof org.bukkit.block.Sign) {
                // Trouver le coffre associé pour obtenir le stock réel
                org.bukkit.block.Block chestBlock = findAdjacentChest(signBlock);
                org.bukkit.inventory.Inventory chestInventory = null;

                if (chestBlock != null && chestBlock.getState() instanceof org.bukkit.block.Chest chest) {
                    chestInventory = chest.getInventory();
                }

                // Mettre à jour l'affichage de la pancarte
                updateSignText(shop, signBlock, chestInventory);

                plugin.getLogger().info("[SUCCESS] Pancarte du shop " + shop.getId() + " mise à jour automatiquement");
            } else {
                plugin.getLogger().warning("[WARNING] Pancarte introuvable pour le shop " + shop.getId());
                player.sendMessage("§e⚠ Pancarte non trouvée pour la mise à jour automatique");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[ERROR] Erreur lors de la mise à jour de la pancarte: " + e.getMessage());
            player.sendMessage("§c✗ Erreur lors de la mise à jour de la pancarte");
        }
    }

    /**
     * Trouve une pancarte adjacente au bloc donné
     */
    private org.bukkit.block.Block findAdjacentShopSign(org.bukkit.block.Block block) {
        org.bukkit.block.BlockFace[] faces = {
            org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH,
            org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST,
            org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN
        };

        for (org.bukkit.block.BlockFace face : faces) {
            org.bukkit.block.Block adjacent = block.getRelative(face);
            if (adjacent.getState() instanceof org.bukkit.block.Sign) {
                return adjacent;
            }
        }
        return null;
    }

    /**
     * Trouve un coffre adjacent au bloc donné
     */
    private org.bukkit.block.Block findAdjacentChest(org.bukkit.block.Block block) {
        org.bukkit.block.BlockFace[] faces = {
            org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH,
            org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST,
            org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN
        };

        for (org.bukkit.block.BlockFace face : faces) {
            org.bukkit.block.Block adjacent = block.getRelative(face);
            if (adjacent.getState() instanceof org.bukkit.block.Chest) {
                return adjacent;
            }
        }
        return null;
    }

    /**
     * Met à jour le texte de la pancarte avec les nouvelles informations
     */
    private void updateSignText(@NotNull Shop shop, @NotNull org.bukkit.block.Block signBlock,
                               org.bukkit.inventory.Inventory chestInventory) {
        if (!(signBlock.getState() instanceof org.bukkit.block.Sign sign)) return;

        try {
            // Calculer le stock réel depuis le coffre
            int realStock = 0;
            if (chestInventory != null) {
                ItemStack targetItem = shop.getItem().toItemStack();
                for (ItemStack item : chestInventory.getContents()) {
                    if (item != null && item.isSimilar(targetItem)) {
                        realStock += item.getAmount();
                    }
                }
            }

            // Mettre à jour les lignes de la pancarte selon le format standard
            String shopType = shop.getType() == Shop.ShopType.SELL ? "[VENTE]" : "[ACHAT]";
            NamedTextColor typeColor = shop.getType() == Shop.ShopType.SELL ? NamedTextColor.RED : NamedTextColor.GREEN;

            List<Component> lines = Arrays.asList(
                Component.text(shopType, typeColor, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.text(String.format("%.2f", shop.getPrice()), NamedTextColor.YELLOW)
                    .append(Component.text("◎", NamedTextColor.GOLD))
                    .decoration(TextDecoration.ITALIC, false),
                Component.text(shop.getItem().getDisplayName(), NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false),
                Component.text("Stock: " + realStock, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            );

            // Appliquer toutes les lignes en une seule fois avec la nouvelle API
            var signSide = sign.getSide(org.bukkit.block.sign.Side.FRONT);
            for (int i = 0; i < lines.size() && i < 4; i++) {
                signSide.line(i, lines.get(i));
            }

            // Sauvegarder les changements
            sign.update();

        } catch (Exception e) {
            plugin.getLogger().severe("[ERROR] Impossible de mettre à jour la pancarte: " + e.getMessage());
        }
    }

    /**
     * Exécute la suppression définitive du shop
     */
    private void executeShopDeletion(@NotNull Player player, @NotNull EditSession session) {
        Shop shop = session.shop;

        plugin.getShopManager().deleteShop(shop.getId())
            .thenAccept(success -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    // Supprimer la pancarte physiquement
                    try {
                        org.bukkit.World world = plugin.getServer().getWorld(shop.getLocation().getWorld().getName());
                        if (world != null) {
                            org.bukkit.Location shopLocation = new org.bukkit.Location(
                                world,
                                shop.getLocation().getX(),
                                shop.getLocation().getY(),
                                shop.getLocation().getZ()
                            );

                            org.bukkit.block.Block signBlock = shopLocation.getBlock();
                            if (!(signBlock.getState() instanceof org.bukkit.block.Sign)) {
                                signBlock = findAdjacentShopSign(shopLocation.getBlock());
                            }

                            if (signBlock != null && signBlock.getState() instanceof org.bukkit.block.Sign) {
                                signBlock.setType(org.bukkit.Material.AIR);
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("[WARNING] Erreur lors de la suppression physique de la pancarte: " + e.getMessage());
                    }

                    player.sendMessage("§c§l✓ Shop supprimé définitivement !");
                    player.sendMessage("§7Vous pouvez récupérer les items restants dans le coffre");

                    player.closeInventory();
                    editSessions.remove(player.getUniqueId());

                    plugin.getLogger().info("[SUCCESS] Shop " + shop.getId() + " supprimé par " + player.getName());
                } else {
                    player.sendMessage("§cErreur lors de la suppression du shop !");
                    plugin.getLogger().severe("[ERROR] Impossible de supprimer le shop " + shop.getId());
                }
            }));
    }

    /**
     * Confirme la suppression du shop
     */
    private void confirmShopDeletion(@NotNull Player player, @NotNull EditSession session) {
        Shop shop = session.shop;

        player.sendMessage("§c§l⚠ CONFIRMATION DE SUPPRESSION ⚠");
        player.sendMessage("§eÊtes-vous vraiment sûr de vouloir supprimer ce shop ?");
        player.sendMessage("§7- Article: §e" + shop.getItem().getDisplayName());
        player.sendMessage("§7- Stock restant: §e" + shop.getStock());
        player.sendMessage("§7- Prix: §e" + String.format("%.2f", shop.getPrice()) + "$");
        player.sendMessage("§c§lCliquez à nouveau sur TNT pour CONFIRMER la suppression");
        player.sendMessage("§a§lOu cliquez sur ANNULER pour revenir en arrière");

        // Marquer la session comme en attente de confirmation
        session.pendingDeletion = true;
        session.deletionConfirmationTime = System.currentTimeMillis();

        // Rafraîchir le menu pour changer l'apparence du bouton TNT
        setupEditMenu(player.getOpenInventory().getTopInventory(), player, session);

        // Programmer l'expiration de la confirmation (10 secondes)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            EditSession currentSession = editSessions.get(player.getUniqueId());
            if (currentSession != null && currentSession.pendingDeletion) {
                currentSession.pendingDeletion = false;
                if (player.getOpenInventory().getTopInventory().getSize() == 45) {
                    setupEditMenu(player.getOpenInventory().getTopInventory(), player, currentSession);
                    player.sendMessage("§a✓ Délai de confirmation expiré - Suppression annulée");
                }
            }
        }, 200L); // 10 secondes
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleString = title.toString();
        if (titleString.contains("Édition")) {
            // Nettoyer la session après un délai
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                EditSession session = editSessions.get(player.getUniqueId());
                if (session != null && session.isExpired()) {
                    editSessions.remove(player.getUniqueId());
                }
            }, 60L);
        }
    }

    /**
     * Session d'édition d'un shop
     */
    public static class EditSession {
        public final Shop shop;
        public double tempPrice;
        public boolean pendingDeletion = false;
        public long deletionConfirmationTime = 0;
        private final long creationTime;

        public EditSession(Shop shop, double initialPrice) {
            this.shop = shop;
            this.tempPrice = initialPrice;
            this.creationTime = System.currentTimeMillis();
        }

        public void adjustPrice(double adjustment) {
            tempPrice = Math.max(0.01, tempPrice + adjustment);
            // Limiter à un maximum raisonnable
            if (tempPrice > 1000000) {
                tempPrice = 1000000;
            }
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - creationTime > 300000; // 5 minutes
        }

        public boolean isDeletionConfirmationExpired() {
            return pendingDeletion && (System.currentTimeMillis() - deletionConfirmationTime > 10000); // 10 secondes
        }
    }
}
