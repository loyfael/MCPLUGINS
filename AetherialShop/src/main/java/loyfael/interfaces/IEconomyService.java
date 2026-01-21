package loyfael.interfaces;

import loyfael.model.ShopItem;
import org.bukkit.entity.Player;

public interface IEconomyService {
    boolean hasEnoughMoney(Player player, double amount);
    boolean withdrawMoney(Player player, double amount);
    void depositMoney(Player player, double amount);
    double getBalance(Player player);
}
