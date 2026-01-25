package loyfael.listener;

import loyfael.Main;
import loyfael.model.Shop;
import loyfael.model.ShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestionnaire de création de shops via clic droit avec panneau en main
 * UX complètement repensée selon les nouvelles spécifications
 */
public class ShopCreationListener implements Listener {

    private final Main plugin;
    // Stockage temporaire des configurations en cours
    private final Map<UUID, ShopConfiguration> pendingConfigurations = new HashMap<>();

    public ShopCreationListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Nouveau système : Clic droit avec un panneau en main sur un coffre
     * NOUVEAU : Distingue entre joueur accroupi (panneau normal) et debout (création shop)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Vérifier si le joueur fait clic droit
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // === CRÉATION DE SHOP ===
        // Vérifier si le joueur a un panneau en main et clique sur un coffre
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (isSignMaterial(itemInHand.getType()) && isChestBlock(clickedBlock)) {

            // NOUVELLE LOGIQUE : Vérifier si le joueur est accroupi
            if (player.isSneaking()) {
                // Joueur accroupi = Poser un panneau normal
                // Ne pas annuler l'événement pour permettre le placement normal du panneau
                plugin.getLogger().info("[DEBUG] Joueur " + player.getName() + " accroupi - Placement normal du panneau autorisé");
                return; // Laisser Minecraft gérer le placement normal
            } else {
                // Joueur debout = Créer un shop
                plugin.getLogger().info("[DEBUG] Joueur " + player.getName() + " debout - Déclenchement création shop");
                handleShopCreation(event, player, clickedBlock);
                return;
            }
        }

        // === ÉDITION DE SHOP ===
        // Vérifier si le joueur clique sur un panneau de shop existant
        if (clickedBlock.getState() instanceof Sign sign) {
            String[] lines = getSignLines(sign);
            if (isShopSign(lines)) {
                handleShopEdit(event, player, sign);
                return;
            }
        }
    }

    /**
     * Gestion de la création d'un nouveau shop
     */
    private void handleShopCreation(@NotNull PlayerInteractEvent event, @NotNull Player player, @NotNull Block chestBlock) {
        event.setCancelled(true);

        if (!player.hasPermission("aetherplayershop.create")) {
            player.sendMessage("§cVous n'avez pas la permission de créer des shops.");
            return;
        }

        Chest chest = (Chest) chestBlock.getState();

        // Vérification que le coffre est plein d'UN SEUL type d'item
        ShopItemValidation validation = validateChestContents(chest);
        if (!validation.isValid()) {
            player.sendMessage("§c" + validation.getErrorMessage());
            return;
        }

        // Vérification de la limite de shops
        plugin.getShopManager().getPlayerShopCount(player.getUniqueId())
            .thenAccept(count -> {
                int maxShops = plugin.getConfigManager().getMaxShopsPerPlayer();
                if (count >= maxShops && !player.hasPermission("aetherplayershop.bypasslimit")) {
                    player.sendMessage("§cVous avez atteint la limite de " + maxShops + " shops.");
                    return;
                }

                // Créer la configuration temporaire
                ShopConfiguration config = new ShopConfiguration(
                    player.getUniqueId(),
                    null, // Pas encore de panneau placé
                    chestBlock.getLocation(),
                    validation.getShopItem(),
                    validation.getTotalQuantity()
                );

                pendingConfigurations.put(player.getUniqueId(), config);

                // Ouvrir le menu de configuration
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    openShopConfigurationMenu(player, config);
                });
            });
    }

    /**
     * Gestion de l'édition d'un shop existant
     */
    private void handleShopEdit(@NotNull PlayerInteractEvent event, @NotNull Player player, @NotNull Sign sign) {
        event.setCancelled(true);

        String[] lines = getSignLines(sign);
        String ownerName = lines[3];

        // Vérifier que c'est le propriétaire ou un admin
        if (!ownerName.equals(player.getName()) && !player.hasPermission("aetherplayershop.admin")) {
            player.sendMessage("§cVous ne pouvez modifier que vos propres shops.");
            return;
        }

        // Récupérer le shop depuis la base de données
        plugin.getShopManager().getShopAtLocation(sign.getLocation())
            .thenAccept(shop -> {
                if (shop == null) {
                    player.sendMessage("§cShop introuvable en base de données.");
                    return;
                }

                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    // Créer une configuration d'édition
                    ShopConfiguration config = new ShopConfiguration(
                        player.getUniqueId(),
                        sign.getLocation(),
                        shop.getChestLocation(),
                        shop.getItem(),
                        shop.getStock()
                    );
                    config.setShopType(shop.getType());
                    config.setPrice(shop.getPrice());

                    pendingConfigurations.put(player.getUniqueId(), config);
                    openShopConfigurationMenu(player, config);
                });
            });
    }

    /**
     * Suppression automatique du shop quand on casse le panneau
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSignBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign sign)) return;

        String[] lines = getSignLines(sign);
        if (!isShopSign(lines)) return;

        Player player = event.getPlayer();
        String ownerName = lines[3];

        // Vérifier que c'est le propriétaire ou un admin
        if (!ownerName.equals(player.getName()) && !player.hasPermission("aetherplayershop.admin")) {
            event.setCancelled(true);
            player.sendMessage("§cVous ne pouvez détruire que vos propres shops.");
            return;
        }

        // Supprimer le shop de la base de données
        plugin.getShopManager().deleteShopAtLocation(block.getLocation())
            .thenAccept(success -> {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage("§aShop supprimé avec succès !");
                    } else {
                        player.sendMessage("§6Shop supprimé (non trouvé en base).");
                    }
                });
            });
    }

    /**
     * Menu de configuration REDESIGNÉ - 6 lignes, UX intuitive
     */
    private void openShopConfigurationMenu(@NotNull Player player, @NotNull ShopConfiguration config) {
        org.bukkit.inventory.Inventory menu = org.bukkit.Bukkit.createInventory(
            null, 54, Component.text("§6§l⚡ Configuration du Shop ⚡")
        );

        // === LIGNE 1: TITRE ET INFO ===
        // Titre décoratif (slots 0-8)
        for (int i = 0; i <= 8; i++) {
            if (i == 4) {
                // Item central - Info du shop
                ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
                ItemMeta infoMeta = infoItem.getItemMeta();
                if (infoMeta != null) {
                    infoMeta.displayName(Component.text("§6§l✨ Votre Shop ✨"));
                    infoMeta.lore(List.of(
                        Component.text("§7Configurez votre boutique"),
                        Component.text("§7avec ce menu intuitif"),
                        Component.empty(),
                        Component.text("§eItem: §f" + config.getShopItem().getDisplayName()),
                        Component.text("§eQuantité: §f" + config.getTotalQuantity())
                    ));
                    infoItem.setItemMeta(infoMeta);
                }
                menu.setItem(4, infoItem);
            } else {
                menu.setItem(i, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));
            }
        }

        // === LIGNE 2: ITEM À VENDRE/ACHETER ===
        // Bordures élégantes
        menu.setItem(9, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));
        menu.setItem(17, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));

        // Item principal (slot 13)
        ItemStack displayItem = config.getShopItem().toDisplayItemStack(1);
        ItemMeta displayMeta = displayItem.getItemMeta();
        if (displayMeta != null) {
            displayMeta.displayName(Component.text("§6§l» " + config.getShopItem().getDisplayName() + " §6§l«"));
            displayMeta.lore(List.of(
                Component.text("§7Ceci est l'item de votre shop"),
                Component.empty(),
                Component.text("§eQuantité disponible: §a" + config.getTotalQuantity()),
                Component.text("§eType: §f" + formatMaterialName(config.getShopItem().getMaterial()))
            ));
            displayItem.setItemMeta(displayMeta);
        }
        menu.setItem(13, displayItem);

        // === LIGNE 3: TYPE DE SHOP SIMPLIFIÉ (VENTE UNIQUEMENT) ===
        // Bordures
        menu.setItem(18, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));
        menu.setItem(26, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));

        // VENTE UNIQUEMENT (slot 22) - Centré puisque c'est le seul mode disponible
        ItemStack sellItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta sellMeta = sellItem.getItemMeta();
        if (sellMeta != null) {
            sellMeta.displayName(Component.text("§a§l✅ BOUTIQUE DE VENTE"));
            sellMeta.lore(List.of(
                Component.text("§7Vous vendez vos items"),
                Component.text("§7aux autres joueurs"),
                Component.empty(),
                Component.text("§7💡 Les joueurs vous achètent vos items"),
                Component.text("§7💰 Vous recevez l'argent automatiquement"),
                Component.empty(),
                Component.text("§a§l>>> MODE SÉLECTIONNÉ <<<")
            ));
            sellItem.setItemMeta(sellMeta);
        }
        menu.setItem(22, sellItem);

        // Point d'entrée futur : slots 21 et 23 restent disponibles pour d'autres types
        // if (Shop.ShopType.BUY.isAvailable()) { ... } // Code pour réactiver ACHAT plus tard

        // === LIGNE 4: CONTRÔLES DE PRIX SYMÉTRIQUES - 3 DE CHAQUE ===
        // Bordures
        menu.setItem(27, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));
        menu.setItem(35, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));

        // DÉCRÉMENTER - 3 boutons centrés (slots 28, 29, 30)
        createPriceButton(menu, 28, Material.IRON_BLOCK, "§c§l🔻 -1000◎", -1000.0,
            List.of("§7Diminuer le prix de 1000◎", "§8Grosse réduction"));
        createPriceButton(menu, 29, Material.IRON_INGOT, "§c§l🔻 -100◎", -100.0,
            List.of("§7Diminuer le prix de 100◎", "§8Réduction moyenne"));
        createPriceButton(menu, 30, Material.IRON_NUGGET, "§c§l🔻 -1◎", -1.0,
            List.of("§7Diminuer le prix de 1◎", "§8Ajustement précis"));

        // INCRÉMENTER - 3 boutons symétriques (slots 32, 33, 34)
        createPriceButton(menu, 32, Material.GOLD_NUGGET, "§a§l🔺 +1◎", 1.0,
            List.of("§7Augmenter le prix de 1◎", "§8Ajustement précis"));
        createPriceButton(menu, 33, Material.GOLD_INGOT, "§a§l🔺 +100◎", 100.0,
            List.of("§7Augmenter le prix de 100◎", "§8Augmentation moyenne"));
        createPriceButton(menu, 34, Material.GOLD_BLOCK, "§a§l🔺 +1000◎", 1000.0,
            List.of("§7Augmenter le prix de 1000◎", "§8Grosse augmentation"));

        // Slot 31 reste vide pour la symétrie visuelle
        menu.setItem(31, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));

        // === LIGNE 5: PRIX ACTUEL AVEC NOUVEAU DESIGN ===
        // Bordures
        menu.setItem(36, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));
        menu.setItem(44, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));

        // Prix actuel avec YELLOW_BUNDLE - PLUS INTUITIF
        ItemStack priceItem = new ItemStack(Material.YELLOW_BUNDLE); // Plus joli que GOLD_INGOT
        ItemMeta priceMeta = priceItem.getItemMeta();
        if (priceMeta != null) {
            priceMeta.displayName(Component.text("§e§l💰 PRIX ACTUEL: " + String.format("%.2f", config.getPrice()) + "◎ 💰"));
            priceMeta.lore(List.of(
                Component.text("§7Prix unitaire par item"),
                Component.empty(),
                Component.text("§6⚡ Utilisez les pépites/lingots/blocs"),
                Component.text("§6⚡ FER pour diminuer (-) "),
                Component.text("§6⚡ OR pour augmenter (+)"),
                Component.empty(),
                Component.text("§8💡 Design intuitif et logique")
            ));
            priceItem.setItemMeta(priceMeta);
        }
        menu.setItem(40, priceItem);

        // Contrôle rapide de réinitialisation
        ItemStack resetItem = new ItemStack(Material.BARRIER);
        ItemMeta resetMeta = resetItem.getItemMeta();
        if (resetMeta != null) {
            resetMeta.displayName(Component.text("§c§l🔄 RESET"));
            resetMeta.lore(List.of(
                Component.text("§7Remettre le prix à 1.00◎"),
                Component.empty(),
                Component.text("§eUtile pour recommencer")
            ));
            resetItem.setItemMeta(resetMeta);
        }
        menu.setItem(43, resetItem);

        // === LIGNE 6: ACTIONS FINALES ===
        // Bordures et décoration
        menu.setItem(45, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));
        menu.setItem(46, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));
        menu.setItem(52, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));
        menu.setItem(53, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, "§r"));

        // ANNULER (slot 48) - Design amélioré
        ItemStack cancelItem = new ItemStack(Material.RED_TERRACOTTA);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("§c§l❌ ANNULER & FERMER"));
            cancelMeta.lore(List.of(
                Component.text("§7Fermer sans sauvegarder"),
                Component.text("§cAucune modification ne sera appliquée"),
                Component.empty(),
                Component.text("§8Le shop ne sera pas créé")
            ));
            cancelItem.setItemMeta(cancelMeta);
        }
        menu.setItem(48, cancelItem);

        // VALIDER (slot 50) - Design amélioré
        ItemStack validateItem = new ItemStack(Material.LIME_TERRACOTTA);
        ItemMeta validateMeta = validateItem.getItemMeta();
        if (validateMeta != null) {
            validateMeta.displayName(Component.text("§a§l✅ CRÉER LE SHOP"));
            validateMeta.lore(List.of(
                Component.text("§7Finaliser la création du shop"),
                Component.empty(),
                Component.text("§a📊 Résumé:"),
                Component.text("§fType: §e" + (config.getShopType() == Shop.ShopType.SELL ? "VENTE" : "ACHAT")),
                Component.text("§fPrix: §e" + String.format("%.2f", config.getPrice()) + "◎"),
                Component.text("§fItem: §e" + config.getShopItem().getDisplayName()),
                Component.text("§fQuantité: §e" + config.getTotalQuantity()),
                Component.empty(),
                Component.text("§a§lCliquez pour confirmer!")
            ));
            validateItem.setItemMeta(validateMeta);
        }
        menu.setItem(50, validateItem);

        player.openInventory(menu);
    }

    /**
     * Crée un item décoratif
     */
    private ItemStack createDecorativeItem(@NotNull Material material, @NotNull String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void createPriceButton(@NotNull org.bukkit.inventory.Inventory menu, int slot,
                                  @NotNull Material material, @NotNull String name, double change,
                                  @NotNull List<String> extraLore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            List<Component> lore = new ArrayList<>();
            for (String line : extraLore) {
                lore.add(Component.text(line));
            }
            lore.add(Component.empty());
            lore.add(Component.text("§8Clic: modifier le prix"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        menu.setItem(slot, item);
    }

    private boolean isShopSign(@NotNull String[] lines) {
        if (lines.length < 3) return false;

        String firstLine = lines[0].toLowerCase();
        return firstLine.contains("[shop]") ||
               firstLine.contains("[buy]") ||
               firstLine.contains("[sell]") ||
               firstLine.contains("[achat]") ||
               firstLine.contains("[vente]");
    }

    private Shop.ShopType parseShopType(@NotNull String line) {
        String lower = line.toLowerCase();
        // Plus de type BUY - tous les shops sont maintenant SELL uniquement
        // if (lower.contains("buy") || lower.contains("achat")) {
        //     return Shop.ShopType.BUY;
        // } else
        if (lower.contains("sell") || lower.contains("vente")) {
            return Shop.ShopType.SELL;
        } else if (lower.contains("[shop]")) {
            return Shop.ShopType.SELL; // Par défaut
        }
        // Tous les types non reconnus deviennent SELL maintenant
        return Shop.ShopType.SELL;
    }

    private double parsePrice(@NotNull String line) {
        try {
            // Extraction du prix depuis des formats comme "10◎", "price: 10", "10.5", etc.
            String cleaned = line.replaceAll("[^0-9.,]", "").replace(",", ".");
            if (cleaned.isEmpty()) {
                throw new IllegalArgumentException("Aucun prix trouvé");
            }

            double price = Double.parseDouble(cleaned);
            if (price <= 0) {
                throw new IllegalArgumentException("Le prix doit être positif");
            }
            return price;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prix invalide: " + line);
        }
    }

    private int parseQuantity(@NotNull String line) {
        try {
            String cleaned = line.replaceAll("[^0-9]", "");
            if (cleaned.isEmpty()) {
                return 1; // Quantité par défaut
            }

            int quantity = Integer.parseInt(cleaned);
            if (quantity <= 0) {
                throw new IllegalArgumentException("La quantité doit être positive");
            }
            return Math.min(quantity, 64); // Limite à 64

        } catch (NumberFormatException e) {
            return 1; // Valeur par défaut en cas d'erreur
        }
    }

    private Block findAdjacentChest(@NotNull Block signBlock) {
        // Vérification des 6 directions adjacentes
        Block[] adjacents = {
            signBlock.getRelative(1, 0, 0),   // Est
            signBlock.getRelative(-1, 0, 0),  // Ouest
            signBlock.getRelative(0, 1, 0),   // Haut
            signBlock.getRelative(0, -1, 0),  // Bas
            signBlock.getRelative(0, 0, 1),   // Sud
            signBlock.getRelative(0, 0, -1)   // Nord
        };

        for (Block adjacent : adjacents) {
            if (adjacent.getType() == Material.CHEST || adjacent.getType() == Material.TRAPPED_CHEST) {
                return adjacent;
            }
        }

        return null;
    }

    private ItemStack findShopItemInChest(@NotNull Chest chest) {
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return item.clone();
            }
        }
        return null;
    }

    private int getInitialStock(@NotNull Chest chest, @NotNull ItemStack shopItem, @NotNull Shop.ShopType shopType) {
        // Maintenant que seul SELL existe, on compte toujours les items dans le coffre
        int stock = 0;
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item != null && item.isSimilar(shopItem)) {
                stock += item.getAmount();
            }
        }
        return stock;
    }

    private void formatShopSign(@NotNull SignChangeEvent event, @NotNull Shop.ShopType shopType,
                              double price, int quantity, @NotNull ShopItem item) {
        // Formatage automatique de la pancarte avec l'API Adventure moderne
        setSignLine(event, 0, "§1[§6" + shopType.getDisplayName() + "§1]");
        setSignLine(event, 1, "§2" + String.format("%.2f", price) + "§6◎ §7x" + quantity);
        setSignLine(event, 2, "§9" + truncateItemName(item.getDisplayName(), 15));
        setSignLine(event, 3, "§8" + event.getPlayer().getName());
    }

    private String truncateItemName(@NotNull String name, int maxLength) {
        if (name.length() <= maxLength) {
            return name;
        }
        return name.substring(0, maxLength - 3) + "...";
    }

    /**
     * Récupère les lignes d'une pancarte avec l'API moderne Adventure
     */
    private String[] getSignLines(@NotNull Sign sign) {
        // Utilisation de getSide().lines() au lieu de lines() déprécié
        List<Component> lines = sign.getSide(org.bukkit.block.sign.Side.FRONT).lines();
        String[] result = new String[4];

        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            result[i] = PlainTextComponentSerializer.plainText().serialize(lines.get(i));
        }

        // Compléter avec des chaînes vides si nécessaire
        for (int i = lines.size(); i < 4; i++) {
            result[i] = "";
        }

        return result;
    }

    /**
     * Récupère les lignes d'un événement SignChangeEvent avec l'API moderne Adventure
     */
    private String[] getSignChangeLines(@NotNull SignChangeEvent event) {
        // Pour SignChangeEvent, utilisation directe de lines() qui retourne les nouvelles lignes
        String[] result = new String[4];

        for (int i = 0; i < 4; i++) {
            Component line = event.line(i);
            result[i] = line != null ? PlainTextComponentSerializer.plainText().serialize(line) : "";
        }

        return result;
    }

    /**
     * Définit une ligne de pancarte avec l'API moderne Adventure
     */
    private void setSignLine(@NotNull SignChangeEvent event, int line, @NotNull String text) {
        event.line(line, Component.text(text));
    }


    /**
     * Valide le contenu d'un coffre pour la création de shop
     */
    private ShopItemValidation validateChestContents(@NotNull Chest chest) {
        return ShopItemValidation.validate(chest);
    }

    /**
     * Gestionnaire de clics dans le menu de configuration
     */
    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(Component.text("§6§l⚡ Configuration du Shop ⚡"))) return;

        event.setCancelled(true);

        ShopConfiguration config = pendingConfigurations.get(player.getUniqueId());
        if (config == null) return;

        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        // Gestion des différents boutons avec les NOUVEAUX slots
        switch (slot) {
            // Plus de choix entre VENTE/ACHAT - seulement VENTE maintenant
            // case 21 -> { ... } // Supprimé car plus de choix de type

            // Contrôles de prix - NOUVEAUX slots réorganisés
            case 28 -> { // -1000◎
                config.adjustPrice(-1000.0);
                refreshConfigMenu(player, config);
            }
            case 29 -> { // -100◎
                config.adjustPrice(-100.0);
                refreshConfigMenu(player, config);
            }
            case 30 -> { // -1◎
                config.adjustPrice(-1.0);
                refreshConfigMenu(player, config);
            }
            case 32 -> { // +1◎
                config.adjustPrice(1.0);
                refreshConfigMenu(player, config);
            }
            case 33 -> { // +100◎
                config.adjustPrice(100.0);
                refreshConfigMenu(player, config);
            }
            case 34 -> { // +1000◎
                config.adjustPrice(1000.0);
                refreshConfigMenu(player, config);
            }
            case 43 -> { // RESET
                config.setPrice(1.0);
                refreshConfigMenu(player, config);
            }
            case 50 -> { // VALIDER - Créer le shop
                finalizeShopCreation(player, config);
            }
            case 48 -> { // ANNULER
                cancelShopCreation(player);
            }
        }
    }

    /**
     * Rafraîchit le menu de configuration
     */
    private void refreshConfigMenu(@NotNull Player player, @NotNull ShopConfiguration config) {
        openShopConfigurationMenu(player, config);
    }

    /**
     * Finalise la création du shop - VERSION CORRIGÉE
     */
    private void finalizeShopCreation(@NotNull Player player, @NotNull ShopConfiguration config) {
        player.closeInventory();

        // Calculer le stock initial
        int initialStock = config.getShopType() == Shop.ShopType.SELL ? config.getTotalQuantity() : 0;

        // Créer le shop en base de données
        plugin.getShopManager().createShop(
            player,
            config.getChestLocation(),
            config.getShopType(),
            config.getShopItem(),
            config.getPrice(),
            initialStock
        ).thenAccept(success -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    // PLACER LE PANNEAU automatiquement
                    placePanelAutomatically(config, player);

                    player.sendMessage("§a§lShop créé avec succès !");
                    player.sendMessage("§7Type: §e" + (config.getShopType() == Shop.ShopType.SELL ? "VENTE" : "ACHAT"));
                    player.sendMessage("§7Prix: §e" + String.format("%.2f", config.getPrice()) + "◎");
                    player.sendMessage("§7Item: §e" + config.getShopItem().getDisplayName());

                    // Retirer un panneau de l'inventaire du joueur
                    removeSignFromInventory(player);

                    // Effets visuels
                    if (plugin.getConfigManager().isSoundEnabled()) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                    if (plugin.getConfigManager().isParticleEnabled()) {
                        player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                            config.getChestLocation().add(0.5, 1, 0.5), 10);
                    }
                } else {
                    player.sendMessage("§c§lErreur lors de la création du shop !");
                }

                // Nettoyer la configuration temporaire
                pendingConfigurations.remove(player.getUniqueId());
            });
        });
    }

    /**
     * Place automatiquement le panneau sur la face du coffre face au joueur
     */
    private void placePanelAutomatically(@NotNull ShopConfiguration config, @NotNull Player player) {
        Block chestBlock = config.getChestLocation().getBlock();

        // Calculer la direction du joueur par rapport au coffre
        org.bukkit.Location playerLoc = player.getLocation();
        org.bukkit.Location chestLoc = chestBlock.getLocation().add(0.5, 0.5, 0.5);

        // Calculer le vecteur de direction
        double deltaX = playerLoc.getX() - chestLoc.getX();
        double deltaZ = playerLoc.getZ() - chestLoc.getZ();

        // Déterminer la face la plus proche du joueur
        Block targetBlock;
        if (Math.abs(deltaX) > Math.abs(deltaZ)) {
            // Le joueur est plus à l'est ou à l'ouest
            if (deltaX > 0) {
                targetBlock = chestBlock.getRelative(1, 0, 0); // Face est
            } else {
                targetBlock = chestBlock.getRelative(-1, 0, 0); // Face ouest
            }
        } else {
            // Le joueur est plus au nord ou au sud
            if (deltaZ > 0) {
                targetBlock = chestBlock.getRelative(0, 0, 1); // Face sud
            } else {
                targetBlock = chestBlock.getRelative(0, 0, -1); // Face nord
            }
        }

        // Vérifier si la position est libre
        if (targetBlock.getType() == Material.AIR || targetBlock.getType().isAir() || targetBlock.isEmpty()) {
            // Placer le panneau mural ORIENTÉ VERS LE JOUEUR
            if (Math.abs(deltaX) > Math.abs(deltaZ)) {
                if (deltaX > 0) {
                    targetBlock.setType(Material.OAK_WALL_SIGN);
                    if (targetBlock.getBlockData() instanceof org.bukkit.block.data.type.WallSign wallSign) {
                        wallSign.setFacing(org.bukkit.block.BlockFace.EAST); // Face vers le joueur (est)
                        targetBlock.setBlockData(wallSign);
                    }
                } else {
                    targetBlock.setType(Material.OAK_WALL_SIGN);
                    if (targetBlock.getBlockData() instanceof org.bukkit.block.data.type.WallSign wallSign) {
                        wallSign.setFacing(org.bukkit.block.BlockFace.WEST); // Face vers le joueur (ouest)
                        targetBlock.setBlockData(wallSign);
                    }
                }
            } else {
                if (deltaZ > 0) {
                    targetBlock.setType(Material.OAK_WALL_SIGN);
                    if (targetBlock.getBlockData() instanceof org.bukkit.block.data.type.WallSign wallSign) {
                        wallSign.setFacing(org.bukkit.block.BlockFace.SOUTH); // Face vers le joueur (sud)
                        targetBlock.setBlockData(wallSign);
                    }
                } else {
                    targetBlock.setType(Material.OAK_WALL_SIGN);
                    if (targetBlock.getBlockData() instanceof org.bukkit.block.data.type.WallSign wallSign) {
                        wallSign.setFacing(org.bukkit.block.BlockFace.NORTH); // Face vers le joueur (nord)
                        targetBlock.setBlockData(wallSign);
                    }
                }
            }

            if (targetBlock.getState() instanceof Sign sign) {
                // Mettre à jour la location du panneau dans la config
                config.setSignLocation(targetBlock.getLocation());

                // Formater le panneau
                updateSignWithShopInfo(config, player);
                player.sendMessage("§aPanneau placé automatiquement sur la face du coffre face à vous !");
                return;
            }
        }

        // Si la position préférée n'est pas libre, essayer les autres faces
        Block[] otherFaces = {
            chestBlock.getRelative(1, 0, 0),   // Est
            chestBlock.getRelative(-1, 0, 0),  // Ouest
            chestBlock.getRelative(0, 0, 1),   // Sud
            chestBlock.getRelative(0, 0, -1),  // Nord
            chestBlock.getRelative(0, 1, 0)    // Dessus en dernier recours
        };

        for (Block face : otherFaces) {
            if (face.getType() == Material.AIR || face.getType().isAir() || face.isEmpty()) {
                // Placer un panneau mural ou normal selon la position
                if (face.getY() == chestBlock.getY()) { // Même niveau = panneau mural
                    face.setType(Material.OAK_WALL_SIGN);

                    // Déterminer la direction du panneau mural
                    org.bukkit.block.BlockFace facing = org.bukkit.block.BlockFace.SOUTH; // Par défaut
                    if (face.getX() > chestBlock.getX()) facing = org.bukkit.block.BlockFace.WEST;
                    else if (face.getX() < chestBlock.getX()) facing = org.bukkit.block.BlockFace.EAST;
                    else if (face.getZ() > chestBlock.getZ()) facing = org.bukkit.block.BlockFace.NORTH;
                    else if (face.getZ() < chestBlock.getZ()) facing = org.bukkit.block.BlockFace.SOUTH;

                    if (face.getBlockData() instanceof org.bukkit.block.data.type.WallSign wallSign) {
                        wallSign.setFacing(facing);
                        face.setBlockData(wallSign);
                    }
                } else { // Au-dessus = panneau normal
                    face.setType(Material.OAK_SIGN);
                }

                if (face.getState() instanceof Sign sign) {
                    config.setSignLocation(face.getLocation());
                    updateSignWithShopInfo(config, player);
                    player.sendMessage("§6Panneau placé automatiquement à côté du coffre !");
                    return;
                }
            }
        }

        // Si aucune position trouvée, informer le joueur
        player.sendMessage("§6Impossible de placer automatiquement le panneau. Placez-le manuellement à côté du coffre.");
    }

    /**
     * Retire un panneau de l'inventaire du joueur
     */
    private void removeSignFromInventory(@NotNull Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (isSignMaterial(itemInHand.getType())) {
            if (itemInHand.getAmount() > 1) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
        }
    }

    /**
     * Méthode pour ajouter la méthode setSignLocation à ShopConfiguration
     */
    // Note: Cette méthode devra être ajoutée à la classe ShopConfiguration

    /**
     * Annule la création du shop - VERSION CORRIGÉE
     */
    private void cancelShopCreation(@NotNull Player player) {
        pendingConfigurations.remove(player.getUniqueId());
        player.closeInventory();
        player.sendMessage("§6Création du shop annulée.");
    }

    /**
     * Vérifie si un matériau est un panneau
     */
    private boolean isSignMaterial(@NotNull Material material) {
        return material == Material.OAK_SIGN || material == Material.BIRCH_SIGN ||
               material == Material.SPRUCE_SIGN || material == Material.JUNGLE_SIGN ||
               material == Material.ACACIA_SIGN || material == Material.DARK_OAK_SIGN ||
               material == Material.CRIMSON_SIGN || material == Material.WARPED_SIGN ||
               material == Material.MANGROVE_SIGN || material == Material.CHERRY_SIGN ||
               material == Material.BAMBOO_SIGN;
    }

    /**
     * Vérifie si un bloc est un coffre
     */
    private boolean isChestBlock(@NotNull Block block) {
        return block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST;
    }

    /**
     * Nettoie les configurations expirées (évite les fuites mémoire)
     */
    private void cleanupExpiredConfigurations() {
        // TODO: Implémenter un système de timeout pour nettoyer les configs abandonnées
        // Peut être appelé périodiquement
    }

    /**
     * Met à jour la pancarte avec les informations du shop
     */
    private void updateSignWithShopInfo(@NotNull ShopConfiguration config, @NotNull Player player) {
        Block signBlock = config.getSignLocation().getBlock();
        if (!(signBlock.getState() instanceof Sign sign)) return;

        // Format selon vos spécifications
        String shopTypeDisplay;
        if (config.getShopType() == Shop.ShopType.SELL) {
            shopTypeDisplay = "§c[VENTE]";
        } else {
            shopTypeDisplay = "§a[ACHAT]";
        }

        // Ligne 1: Type avec couleur
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(0, Component.text(shopTypeDisplay));

        // Ligne 2: Prix formaté
        String priceText = "§e" + String.format("%.2f", config.getPrice()) + "§6◎";
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(1, Component.text(priceText));

        // Ligne 3: Type d'item (avec nom custom si applicable)
        String itemDisplay = formatItemDisplayName(config.getShopItem());
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(2, Component.text(itemDisplay));

        // Ligne 4: Pseudo du joueur
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(3, Component.text("§8" + player.getName()));

        sign.update();
    }

    /**
     * Formate le nom d'affichage de l'item pour la pancarte
     */
    private String formatItemDisplayName(@NotNull ShopItem shopItem) {
        String materialName = formatMaterialName(shopItem.getMaterial());

        // Si l'item a un nom custom, on l'affiche en violet
        if (shopItem.getCustomName() != null && !shopItem.getCustomName().isEmpty()) {
            return "§3" + materialName + " §5" + truncateText(shopItem.getCustomName(), 8);
        } else {
            return "§3" + materialName;
        }
    }

    /**
     * Formate le nom du matériau
     */
    private String formatMaterialName(@NotNull Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        // Capitaliser chaque mot
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase())
                      .append(word.substring(1));
            }
        }
        return truncateText(result.toString(), 12);
    }

    /**
     * Tronque un texte à une longueur maximale
     */
    private String truncateText(@NotNull String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 2) + "..";
    }
}
