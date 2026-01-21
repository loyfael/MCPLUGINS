package loyfael.interfaces;

import loyfael.model.ShopItem;
import org.bukkit.entity.Player;

import java.util.List;

public interface IShopService {
    void rotateItems();
    void rotateItems(boolean sendWebhook);
    List<ShopItem> getCurrentItems();
    boolean processBuy(Player player, ShopItem item, int quantity);
    boolean processSell(Player player, ShopItem item, int quantity);
    void startRotationScheduler();
}
