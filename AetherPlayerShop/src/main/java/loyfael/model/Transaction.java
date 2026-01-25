package loyfael.model;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Modèle représentant une transaction selon le cahier des charges
 */
public class Transaction {

    private final String id;
    private final String shopId;
    private final UUID buyerUUID;
    private final UUID sellerUUID;
    private final TransactionType type;
    private final ShopItem item;
    private final double price;
    private final int quantity;
    private final Instant date;

    public Transaction(@NotNull String shopId, @NotNull UUID buyerUUID, @NotNull UUID sellerUUID,
                      @NotNull TransactionType type, @NotNull ShopItem item,
                      double price, int quantity) {
        this.id = UUID.randomUUID().toString();
        this.shopId = shopId;
        this.buyerUUID = buyerUUID;
        this.sellerUUID = sellerUUID;
        this.type = type;
        this.item = item;
        this.price = price;
        this.quantity = quantity;
        this.date = Instant.now();
    }

    public Transaction(@NotNull Document document) {
        this.id = document.getString("_id");
        this.shopId = document.getString("shopId");
        this.buyerUUID = UUID.fromString(document.getString("buyerUUID"));
        this.sellerUUID = UUID.fromString(document.getString("sellerUUID"));
        this.type = TransactionType.valueOf(document.getString("type"));
        this.item = new ShopItem(document.get("item", Document.class));
        this.price = document.getDouble("price");
        this.quantity = document.getInteger("quantity");
        this.date = document.get("date", Instant.class);
    }

    /**
     * Convertit la transaction en Document MongoDB
     */
    public Document toDocument() {
        return new Document()
                .append("_id", id)
                .append("shopId", shopId)
                .append("buyerUUID", buyerUUID.toString())
                .append("sellerUUID", sellerUUID.toString())
                .append("type", type.name())
                .append("item", item.toDocument())
                .append("price", price)
                .append("quantity", quantity)
                .append("date", date);
    }

    // Getters
    public String getId() { return id; }
    public String getShopId() { return shopId; }
    public UUID getBuyerUUID() { return buyerUUID; }
    public UUID getSellerUUID() { return sellerUUID; }
    public TransactionType getType() { return type; }
    public ShopItem getItem() { return item; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public Instant getDate() { return date; }

    public enum TransactionType {
        BUY("Achat"),
        SELL("Vente");

        private final String displayName;

        TransactionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }
}
