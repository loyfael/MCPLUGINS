package loyfael.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class ShopItem {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final Material material;
    private final String name;
    private final List<String> lore;
    private final int minPrice;
    private final int maxPrice;
    private final int amount;
    private final int maxStock;
    private int currentStock;
    private int currentPrice;
    private final String nbt;

    public ShopItem(Material material, String name, List<String> lore,
                   int minPrice, int maxPrice, int amount, int maxStock, String nbt) {
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.amount = amount;
        this.maxStock = maxStock;
        this.currentStock = maxStock;
        this.currentPrice = generateRandomDailyPrice();
        this.nbt = nbt;
    }

    private int generateRandomDailyPrice() {
        // Prix fixe aléatoire entre min et max pour la journée
        return minPrice + (int)(Math.random() * (maxPrice - minPrice + 1));
    }

    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.displayName(LEGACY_SERIALIZER.deserialize(name));
            }
            if (lore != null) {
                List<Component> loreComponents = lore.stream()
                        .map(LEGACY_SERIALIZER::deserialize)
                        .collect(Collectors.toList());
                meta.lore(loreComponents);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void updatePrice() {
        // Prix fixes - ne plus changer selon le stock
        // Le prix reste le même toute la journée
    }

    public boolean canBuy(int quantity) {
        return currentStock >= quantity;
    }

    public void buy(int quantity) {
        if (canBuy(quantity)) {
            currentStock -= quantity;
            // Ne plus appeler updatePrice() - prix fixes
        }
    }

    public void sell(int quantity) {
        currentStock += quantity;
        if (currentStock > maxStock) {
            currentStock = maxStock;
        }
        // Ne plus appeler updatePrice() - prix fixes
    }

    public Material getMaterial() { return material; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public int getMinPrice() { return minPrice; }
    public int getMaxPrice() { return maxPrice; }
    public int getAmount() { return amount; }
    public int getMaxStock() { return maxStock; }
    public int getCurrentStock() { return currentStock; }
    public int getCurrentPrice() { return currentPrice; }
    public String getNbt() { return nbt; }

    public void setCurrentStock(int currentStock) {
        this.currentStock = Math.max(0, Math.min(currentStock, maxStock));
    }

    public void setCurrentPrice(int currentPrice) {
        this.currentPrice = Math.max(minPrice, Math.min(currentPrice, maxPrice));
    }

    public void generateRandomDailyStock() {
        int minDailyStock = 51;
        int maxDailyStock = Math.max(minDailyStock, maxStock);
        this.currentStock = minDailyStock + (int)(Math.random() * (maxDailyStock - minDailyStock + 1));
        // Générer un nouveau prix fixe pour la journée
        this.currentPrice = generateRandomDailyPrice();
    }
}
