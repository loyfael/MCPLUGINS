package loyfael.api.interfaces;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

/**
 * Interface pour les gestionnaires de GUI
 * Principe de responsabilité unique - gestion spécialisée des interactions GUI
 */
public interface IGuiHandler {

    /**
     * Crée l'inventaire pour cette GUI
     */
    Inventory createInventory(Player player, Map<String, Object> parameters);

    /**
     * Gère le clic sur un item dans la GUI
     */
    void handleClick(Player player, ItemStack clickedItem, int slot);

    /**
     * Met à jour le contenu de l'inventaire
     */
    void updateContent(Player player, Inventory inventory);
}
