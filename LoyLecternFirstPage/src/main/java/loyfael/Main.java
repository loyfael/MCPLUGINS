package loyfael;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
/*
 * This plugin allows players to open a written book from a lectern by right-clicking it.
 * It checks if the clicked block is a lectern and if it contains a written book.
 * If so, it opens the book for the player.
 */
public class Main extends JavaPlugin implements Listener {

  private String worldName;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    worldName = getConfig().getString("world-name", "tuto");

    getServer().getPluginManager().registerEvents(this, this);

    if (worldName == null || worldName.trim().isEmpty()) {
      getLogger().info("LoyLecternFirstPage activated for all worlds.");
    } else {
      getLogger().info("LoyLecternFirstPage activated for the world: " + worldName);
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Block clickedBlock = event.getClickedBlock();
    Player player = event.getPlayer();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    if (worldName != null && !worldName.trim().isEmpty() && !player.getWorld().getName().equals(worldName)) {
      return;
    }

    // Verify if the clicked block is a lectern
    // and if it contains a written book
    // If so, open the book for the player
    // and cancel the event to prevent default behavior
    if (clickedBlock != null && clickedBlock.getType() == Material.LECTERN) {
      BlockState state = clickedBlock.getState();
      if (state instanceof Lectern lectern) {
        ItemStack book = lectern.getInventory().getItem(0);
        if (book != null && book.getType() == Material.WRITTEN_BOOK) {
          event.setCancelled(true);

          new BukkitRunnable() {
            @Override
            public void run() {
              player.openBook(book);
            }
          }.runTaskLater(this, 1L);
        }
      }
    }
  }

  // This event is triggered when a player closes an inventory
  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    Player player = (Player) event.getPlayer();

    // verify if the player is in the correct world
    if (worldName != null && !worldName.trim().isEmpty() && !player.getWorld().getName().equals(worldName)) {
      return;
    }

    // Verify if the closed inventory is a lectern
    if (event.getInventory().getType() == InventoryType.LECTERN) {
      // Find the lectern block from the inventory
      if (event.getInventory().getLocation() != null) {
        Block lecternBlock = event.getInventory().getLocation().getBlock();
        if (lecternBlock.getType() == Material.LECTERN) {
          BlockState state = lecternBlock.getState();
          if (state instanceof Lectern lectern) {
            // Verify if the lectern contains a written book
            lectern.setPage(0); // Reset the lectern to the first page
            lectern.update();
          }
        }
      }
    }
  }
}