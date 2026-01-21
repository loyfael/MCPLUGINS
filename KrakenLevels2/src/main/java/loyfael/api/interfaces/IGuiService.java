package loyfael.api.interfaces;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

/**
 * Interface pour les services d'interface utilisateur
 * Principe de responsabilité unique - séparation de la logique GUI
 */
public interface IGuiService {

    /**
     * Crée et ouvre une GUI pour un joueur
     */
    void openGui(Player player, String guiType, Map<String, Object> parameters);

    void updateGui(Player player);

    /**
     * Met à jour une GUI existante
     */
    void updateGui(Player player, String guiType);

    /**
     * Ferme toutes les GUIs d'un joueur
     */
    void closeGui(Player player);

    /**
     * Gère l'interaction avec un item dans une GUI
     */
    void handleGuiClick(Player player, ItemStack item, int slot);

    /**
     * Crée un ItemStack avec les propriétés spécifiées
     */
    ItemStack createGuiItem(String materialName, String displayName, String... lore);

    /**
     * Vérifie si un joueur a une GUI ouverte
     */
    boolean hasOpenGui(Player player);

    /**
     * Récupère le type de GUI actuellement ouvert pour un joueur
     */
    String getCurrentGuiType(Player player);

    /**
     * Enregistre un gestionnaire de GUI personnalisé
     */
    void registerGuiHandler(String guiType, IGuiHandler handler);

    /**
     * Enregistre un gestionnaire de GUI (alias pour compatibilité)
     */
    void registerHandler(String guiType, IGuiHandler handler);

    /**
     * Gère le clic dans un inventaire
     */
    void handleInventoryClick(Player player, int slot, ItemStack clickedItem);

    /**
     * Vérifie si un joueur a une GUI ouverte
     */
    boolean isGuiOpen(Player player);

    /**
     * Récupère le type de GUI actif pour un joueur
     */
    String getActiveGuiType(Player player);
}
