package loyfael.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Modèle représentant un shop selon les spécifications du cahier des charges
 */
public class Shop {

    private final String id;
    private final UUID ownerUUID;
    private final String ownerName;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final ShopType type;
    private final ShopItem item;
    private double price;
    private int stock;
    private final Instant createdAt;
    private boolean active;

    public Shop(@NotNull String id, @NotNull UUID ownerUUID, @NotNull String ownerName,
                @NotNull Location location, @NotNull ShopType type, @NotNull ShopItem item,
                double price, int stock) {
        this.id = id;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.world = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
        this.type = type;
        this.item = item;
        this.price = price;
        this.stock = stock;
        this.createdAt = Instant.now();
        this.active = true;
    }

    // Constructeur depuis Document MongoDB
    public Shop(@NotNull Document document) {
        this.id = document.getString("_id");
        this.ownerUUID = UUID.fromString(document.getString("ownerUUID"));
        this.ownerName = document.getString("ownerName");
        this.world = document.getString("world");
        this.x = document.getInteger("x");
        this.y = document.getInteger("y");
        this.z = document.getInteger("z");

        // CORRECTION: Gestion des types obsolètes (BUY) en les convertissant en SELL
        String typeString = document.getString("type");
        ShopType assignedType;
        if ("BUY".equals(typeString)) {
            // Convertir les anciens shops BUY en SELL par défaut
            assignedType = ShopType.SELL;
            // TODO: Log this conversion for monitoring
        } else {
            try {
                assignedType = ShopType.valueOf(typeString);
            } catch (IllegalArgumentException e) {
                // Type inconnu, par défaut SELL
                assignedType = ShopType.SELL;
            }
        }
        this.type = assignedType;

        this.item = new ShopItem(document.get("item", Document.class));
        this.price = document.getDouble("price");
        this.stock = document.getInteger("stock");

        // FIX: Gestion correcte de la conversion Date -> Instant
        Object createdAtObj = document.get("createdAt");
        if (createdAtObj instanceof java.util.Date date) {
            this.createdAt = date.toInstant();
        } else if (createdAtObj instanceof Instant instant) {
            this.createdAt = instant;
        } else {
            // Valeur par défaut si le champ n'existe pas ou est corrompu
            this.createdAt = Instant.now();
        }

        this.active = document.getBoolean("active", true);
    }

    /**
     * Convertit le shop en Document MongoDB
     */
    public Document toDocument() {
        return new Document()
                .append("_id", id)
                .append("ownerUUID", ownerUUID.toString())
                .append("ownerName", ownerName)
                .append("world", world)
                .append("x", x)
                .append("y", y)
                .append("z", z)
                .append("type", type.name())
                .append("item", item.toDocument())
                .append("price", price)
                .append("stock", stock)
                .append("createdAt", createdAt)
                .append("active", active);
    }

    /**
     * Obtient la location du shop
     */
    @Nullable
    public Location getLocation() {
        var world = org.bukkit.Bukkit.getWorld(this.world);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    /**
     * Vérifie si le shop correspond à une location donnée
     */
    public boolean isAtLocation(@NotNull Location location) {
        return location.getWorld().getName().equals(world)
            && location.getBlockX() == x
            && location.getBlockY() == y
            && location.getBlockZ() == z;
    }

    /**
     * Obtient la location du shop (alias pour getLocation)
     */
    @Nullable
    public Location getChestLocation() {
        return getLocation();
    }

    // Getters
    public String getId() { return id; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public String getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public ShopType getType() { return type; }
    public ShopItem getItem() { return item; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }

    // Setters
    public void setPrice(double price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }
    public void setActive(boolean active) { this.active = active; }

    public enum ShopType {
        SELL("Vente");
        // BUY("Achat"); // Point d'entrée conservé pour usage futur - actuellement désactivé

        private final String displayName;

        ShopType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }

        // Point d'entrée pour réactiver le type ACHAT dans le futur
        public static ShopType[] getAvailableTypes() {
            return new ShopType[]{SELL}; // Pour réactiver ACHAT, ajouter BUY dans ce tableau
        }

        // Méthode utilitaire pour vérifier si un type est disponible
        public boolean isAvailable() {
            return this == SELL; // Pour réactiver ACHAT : return this == SELL || this == BUY;
        }
    }
}
