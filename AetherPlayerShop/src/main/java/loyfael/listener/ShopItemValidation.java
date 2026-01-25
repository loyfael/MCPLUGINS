package loyfael.listener;

import loyfael.model.ShopItem;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classe de validation du contenu d'un coffre pour la création de shop
 */
public class ShopItemValidation {

    private final boolean valid;
    private final String errorMessage;
    private final ShopItem shopItem;
    private final int totalQuantity;

    private ShopItemValidation(boolean valid, @Nullable String errorMessage,
                              @Nullable ShopItem shopItem, int totalQuantity) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.shopItem = shopItem;
        this.totalQuantity = totalQuantity;
    }

    /**
     * Valide le contenu d'un coffre selon les nouvelles règles
     */
    public static ShopItemValidation validate(@NotNull Chest chest) {
        ItemStack[] contents = chest.getInventory().getContents();

        // Vérifier que le coffre n'est pas vide
        boolean hasItems = false;
        for (ItemStack item : contents) {
            if (item != null && item.getAmount() > 0) {
                hasItems = true;
                break;
            }
        }

        if (!hasItems) {
            return new ShopItemValidation(false, "Le coffre doit contenir des items pour créer un shop.", null, 0);
        }

        // Trouver le premier item non-null comme référence
        ItemStack referenceItem = null;
        for (ItemStack item : contents) {
            if (item != null && item.getAmount() > 0) {
                referenceItem = item;
                break;
            }
        }

        if (referenceItem == null) {
            return new ShopItemValidation(false, "Erreur lors de la lecture du coffre.", null, 0);
        }

        // Vérifier que TOUS les items sont identiques
        int totalQuantity = 0;
        for (ItemStack item : contents) {
            if (item != null && item.getAmount() > 0) {
                if (!item.isSimilar(referenceItem)) {
                    return new ShopItemValidation(false,
                        "Le coffre doit contenir UN SEUL type d'item. " +
                        "Retirez les autres items différents.", null, 0);
                }
                totalQuantity += item.getAmount();
            }
        }

        // Vérifier qu'il y a assez d'items (minimum 1 stack complet recommandé)
        if (totalQuantity < 64) {
            // Warning mais pas d'erreur bloquante
            // On peut créer un shop même avec moins d'items
        }

        // Créer le ShopItem à partir de l'item de référence
        ShopItem shopItem = new ShopItem(referenceItem);

        return new ShopItemValidation(true, null, shopItem, totalQuantity);
    }

    // Getters
    public boolean isValid() { return valid; }
    public String getErrorMessage() { return errorMessage != null ? errorMessage : ""; }
    public ShopItem getShopItem() { return shopItem; }
    public int getTotalQuantity() { return totalQuantity; }
}
