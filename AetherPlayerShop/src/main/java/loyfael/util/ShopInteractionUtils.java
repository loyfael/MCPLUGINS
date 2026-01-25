package loyfael.util;

import loyfael.model.Shop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Utilitaires pour les interactions avec les shops
 */
public class ShopInteractionUtils {

    /**
     * Vérifie si une pancarte est une pancarte de shop
     */
    public static boolean isShopSign(@NotNull String[] lines) {
        if (lines.length < 1) return false;
        String firstLine = lines[0].toLowerCase();
        return firstLine.contains("[achat]") || firstLine.contains("[vente]") ||
               firstLine.contains("[buy]") || firstLine.contains("[sell]") ||
               firstLine.contains("[shop]");
    }

    /**
     * Trouve une pancarte de shop adjacente à un coffre
     */
    @Nullable
    public static Block findAdjacentShopSign(@NotNull Block chestBlock) {
        // Recherche dans les 6 directions adjacentes
        Block[] adjacents = {
            chestBlock.getRelative(1, 0, 0), chestBlock.getRelative(-1, 0, 0),
            chestBlock.getRelative(0, 1, 0), chestBlock.getRelative(0, -1, 0),
            chestBlock.getRelative(0, 0, 1), chestBlock.getRelative(0, 0, -1)
        };

        for (Block adjacent : adjacents) {
            if (adjacent.getState() instanceof Sign sign) {
                if (isShopSign(getSignLines(sign))) {
                    return adjacent;
                }
            }
        }

        return null;
    }

    /**
     * Trouve un coffre adjacent à une pancarte de shop
     */
    @Nullable
    public static Block findAdjacentChest(@NotNull Block signBlock) {
        // Recherche dans les 6 directions adjacentes
        Block[] adjacents = {
            signBlock.getRelative(1, 0, 0), signBlock.getRelative(-1, 0, 0),
            signBlock.getRelative(0, 1, 0), signBlock.getRelative(0, -1, 0),
            signBlock.getRelative(0, 0, 1), signBlock.getRelative(0, 0, -1)
        };

        for (Block adjacent : adjacents) {
            if (adjacent.getType().name().contains("CHEST")) {
                return adjacent;
            }
        }

        return null;
    }

    /**
     * Obtient les lignes d'une pancarte en tant que tableau de String
     */
    @NotNull
    public static String[] getSignLines(@NotNull Sign sign) {
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            Component line = sign.getSide(org.bukkit.block.sign.Side.FRONT).line(i);
            lines[i] = line != null ? PlainTextComponentSerializer.plainText().serialize(line) : "";
        }
        return lines;
    }

    /**
     * Met à jour l'affichage d'une pancarte de shop selon le stock disponible
     * Affiche une croix rouge si pas de stock, l'item normal sinon
     */
    public static void updateShopSignDisplay(@NotNull Shop shop, @NotNull Block signBlock, @Nullable Inventory chestInventory) {
        if (!(signBlock.getState() instanceof Sign sign)) return;

        // Calculer le stock réel dans le coffre si disponible
        int actualStock = 0;
        if (chestInventory != null) {
            actualStock = countItemsInInventory(chestInventory, shop.getItem().toItemStack());
        } else {
            actualStock = shop.getStock(); // Fallback sur le stock en DB
        }

        // Déterminer l'affichage selon le stock
        if (actualStock <= 0) {
            // Pas de stock - afficher une croix rouge
            updateSignToOutOfStock(sign, shop);
        } else {
            // Stock disponible - afficher normalement
            updateSignToInStock(sign, shop, actualStock);
        }

        // Appliquer les changements
        sign.update(true);
    }

    /**
     * Met à jour la pancarte pour indiquer "rupture de stock" avec une croix rouge
     */
    private static void updateSignToOutOfStock(@NotNull Sign sign, @NotNull Shop shop) {
        org.bukkit.block.sign.SignSide frontSide = sign.getSide(org.bukkit.block.sign.Side.FRONT);

        // Ligne 1: Type de shop avec indication de rupture
        String shopType = shop.getType() == Shop.ShopType.SELL ? "[VENTE]" : "[ACHAT]";
        frontSide.line(0, Component.text(shopType, NamedTextColor.DARK_RED, TextDecoration.BOLD));

        // Ligne 2: Croix rouge + "RUPTURE"
        frontSide.line(1, Component.text("✗ RUPTURE", NamedTextColor.RED, TextDecoration.BOLD));

        // Ligne 3: Item name (grisé)
        frontSide.line(2, Component.text(shop.getItem().getDisplayName(), NamedTextColor.GRAY));

        // Ligne 4: Propriétaire
        frontSide.line(3, Component.text(shop.getOwnerName(), NamedTextColor.DARK_GRAY));
    }

    /**
     * Met à jour la pancarte pour l'affichage normal avec stock
     */
    private static void updateSignToInStock(@NotNull Sign sign, @NotNull Shop shop, int stock) {
        org.bukkit.block.sign.SignSide frontSide = sign.getSide(org.bukkit.block.sign.Side.FRONT);

        // Ligne 1: Type de shop
        String shopType = shop.getType() == Shop.ShopType.SELL ? "[VENTE]" : "[ACHAT]";
        NamedTextColor typeColor = shop.getType() == Shop.ShopType.SELL ? NamedTextColor.GREEN : NamedTextColor.BLUE;
        frontSide.line(0, Component.text(shopType, typeColor, TextDecoration.BOLD));

        // Ligne 2: Prix
        String priceText = String.format("%.2f◎", shop.getPrice());
        frontSide.line(1, Component.text(priceText, NamedTextColor.GOLD, TextDecoration.BOLD));

        // Ligne 3: Item name
        frontSide.line(2, Component.text(shop.getItem().getDisplayName(), NamedTextColor.WHITE));

        // Ligne 4: Stock et propriétaire
        String stockInfo = "Stock: " + stock;
        frontSide.line(3, Component.text(stockInfo, NamedTextColor.YELLOW));

        /*frontSide.line(3, Component.text(stockInfo, NamedTextColor.YELLOW).append(Component.text(" | " + shop.getOwnerName(), NamedTextColor.GRAY)));*/
    }

    /**
     * Compte les items d'un type donné dans un inventaire
     */
    public static int countItemsInInventory(@NotNull Inventory inventory, @NotNull ItemStack targetItem) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.isSimilar(targetItem)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Vérifie si un joueur peut effectuer une transaction sur un shop
     */
    public static boolean canPerformTransaction(@NotNull Player player, @NotNull Shop shop, boolean isBuyAction) {
        // Vérifier si c'est le propriétaire
        if (shop.getOwnerUUID().equals(player.getUniqueId())) {
            return false; // Les propriétaires ne peuvent pas acheter/vendre à leur propre shop
        }

        // Vérifier si le shop est actif
        if (!shop.isActive()) {
            player.sendMessage("§cCe shop est actuellement inactif.");
            return false;
        }

        // Vérifier le type d'action par rapport au type de shop
        if (shop.getType() == Shop.ShopType.SELL && !isBuyAction) {
            player.sendMessage("§cCeci est un shop de vente. Faites clic gauche pour acheter.");
            return false;
        }

        // Plus de type BUY - tous les shops sont maintenant SELL uniquement
        // if (shop.getType() == Shop.ShopType.BUY && isBuyAction) {
        //     player.sendMessage("§cCeci est un shop d'achat. Faites clic droit pour vendre.");
        //     return false;
        // }

        // Tous les shops sont de type SELL maintenant
        return true;
    }

    /**
     * Vérifie si le joueur a assez d'espace dans son inventaire pour un item
     */
    public static boolean hasInventorySpace(@NotNull Player player, @NotNull ItemStack item) {
        int emptySlots = 0;
        int partialStacks = 0;

        for (ItemStack slot : player.getInventory().getContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                emptySlots++;
            } else if (slot.isSimilar(item) && slot.getAmount() < slot.getMaxStackSize()) {
                partialStacks += slot.getMaxStackSize() - slot.getAmount();
            }
        }

        // Calculer si on peut stocker au moins un item
        return emptySlots > 0 || partialStacks > 0;
    }

    /**
     * Crée un item stack représentant un shop sans stock (croix rouge)
     */
    @NotNull
    public static ItemStack createOutOfStockDisplayItem(@NotNull Shop shop) {
        ItemStack outOfStockItem = new ItemStack(Material.BARRIER);
        var meta = outOfStockItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("✗ RUPTURE DE STOCK", NamedTextColor.RED, TextDecoration.BOLD));
            meta.lore(List.of(
                Component.empty(),
                Component.text("Item: " + shop.getItem().getDisplayName(), NamedTextColor.GRAY),
                Component.text("Prix: " + String.format("%.2f", shop.getPrice()) + "◎", NamedTextColor.GRAY),
                Component.text("Propriétaire: " + shop.getOwnerName(), NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Ce shop n'a plus de stock disponible", NamedTextColor.RED),
                Component.text("Revenez plus tard !", NamedTextColor.YELLOW)
            ));
            outOfStockItem.setItemMeta(meta);
        }
        return outOfStockItem;
    }
}
