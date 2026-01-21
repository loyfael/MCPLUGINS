package loyfael.interfaces;

import loyfael.model.ShopItem;
import org.bukkit.entity.Player;

import java.util.List;

public interface IShopRepository {
    void saveShopData(List<ShopItem> items, String date);
    List<ShopItem> loadShopData();
    String getLastRotationDate();
    boolean hasValidData();
}
