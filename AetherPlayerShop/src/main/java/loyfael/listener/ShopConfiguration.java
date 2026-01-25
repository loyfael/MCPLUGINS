package loyfael.listener;

import loyfael.model.Shop;
import loyfael.model.ShopItem;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Configuration temporaire d'un shop en cours de création
 */
public class ShopConfiguration {

    private final UUID playerUUID;
    private Location signLocation;
    private final Location chestLocation;
    private final ShopItem shopItem;
    private final int totalQuantity;

    private Shop.ShopType shopType = Shop.ShopType.SELL; // Par défaut = vente
    private double price = 1.0; // Prix par défaut

    public ShopConfiguration(@NotNull UUID playerUUID, @NotNull Location signLocation,
                           @NotNull Location chestLocation, @NotNull ShopItem shopItem,
                           int totalQuantity) {
        this.playerUUID = playerUUID;
        this.signLocation = signLocation;
        this.chestLocation = chestLocation;
        this.shopItem = shopItem;
        this.totalQuantity = totalQuantity;
    }

    // Getters
    public UUID getPlayerUUID() { return playerUUID; }
    public Location getSignLocation() { return signLocation; }
    public Location getChestLocation() { return chestLocation; }
    public ShopItem getShopItem() { return shopItem; }
    public int getTotalQuantity() { return totalQuantity; }
    public Shop.ShopType getShopType() { return shopType; }
    public double getPrice() { return price; }

    // Setters
    public void setShopType(@NotNull Shop.ShopType shopType) { this.shopType = shopType; }
    public void setPrice(double price) { this.price = Math.max(0.01, price); } // Min 1 centime
    public void setSignLocation(@NotNull Location signLocation) { this.signLocation = signLocation; }

    public void adjustPrice(double change) {
        setPrice(this.price + change);
    }
}
