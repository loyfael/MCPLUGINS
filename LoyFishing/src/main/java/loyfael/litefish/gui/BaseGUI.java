package loyfael.litefish.gui;

import loyfael.litefish.LiteFish;
import loyfael.litefish.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Base class for all GUI interfaces
 */
public abstract class BaseGUI implements InventoryHolder {
    
    protected final LiteFish plugin;
    protected final Player player;
    protected final Inventory inventory;
    protected final String title;
    protected final int size;
    
    public BaseGUI(LiteFish plugin, Player player, String title, int size) {
        this.plugin = plugin;
        this.player = player;
        this.title = MessageUtils.colorize(title);
        this.size = size;
        this.inventory = Bukkit.createInventory(this, size, this.title);
        
        setupGUI();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Abstract method to setup the GUI contents
     */
    protected abstract void setupGUI();
    
    /**
     * Handle clicks in the GUI
     */
    public abstract void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick);
    
    /**
     * Open the GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
        playSound(Sound.BLOCK_CHEST_OPEN);
    }
    
    /**
     * Close the GUI
     */
    public void close() {
        player.closeInventory();
    }
    
    /**
     * Refresh the GUI contents
     */
    public void refresh() {
        inventory.clear();
        setupGUI();
    }
    
    /**
     * Play a sound to the player
     */
    protected void playSound(Sound sound) {
        playSound(sound, 1.0f, 1.0f);
    }
    
    protected void playSound(Sound sound, float volume, float pitch) {
        if (plugin.getConfigManager().getConfig().getBoolean("general.sound-effects", true)) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
    
    /**
     * Create a GUI item with name and lore
     */
    protected ItemStack createGuiItem(Material material, String name, String... lore) {
        return createGuiItem(material, 1, name, lore);
    }
    
    protected ItemStack createGuiItem(Material material, int amount, String name, String... lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(MessageUtils.colorize(name));
            
            if (lore.length > 0) {
                List<String> loreList = Arrays.asList(lore);
                loreList.replaceAll(MessageUtils::colorize);
                meta.setLore(loreList);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create a filler item (glass pane)
     */
    protected ItemStack createFillerItem() {
        return createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }
    
    /**
     * Fill empty slots with filler items
     */
    protected void fillEmpty() {
        ItemStack filler = createFillerItem();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
    
    /**
     * Create navigation buttons
     */
    protected ItemStack createBackButton() {
        return createGuiItem(Material.ARROW, "&c&l← Back", "&7Click to go back");
    }
    
    protected ItemStack createNextButton() {
        return createGuiItem(Material.ARROW, "&a&lNext →", "&7Click to go to next page");
    }
    
    protected ItemStack createPreviousButton() {
        return createGuiItem(Material.ARROW, "&a&l← Previous", "&7Click to go to previous page");
    }
    
    protected ItemStack createCloseButton() {
        return createGuiItem(Material.BARRIER, "&c&lClose", "&7Click to close this menu");
    }
    
    /**
     * Send a message to the player
     */
    protected void sendMessage(String message) {
        MessageUtils.sendMessage(player, message);
    }
    
    /**
     * Check if player has permission
     */
    protected boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }
}
