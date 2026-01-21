package loyfael.interfaces;

import loyfael.model.ShopItem;

import java.util.List;

public interface IItemService {
    List<ShopItem> loadAvailableItems();
    List<ShopItem> generateDailyItems(int count);
    ShopItem createShopItem(String material, String name, List<String> lore,
                           int minPrice, int maxPrice, int amount, int stock, String nbt);
}
