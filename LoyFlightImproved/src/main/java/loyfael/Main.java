package loyfael;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LoyFlight - Boost élytre avec clic droit, consommation de faim
 * Un clic = un boost avec plume, double clic = super boost
 */
public class Main extends JavaPlugin implements Listener {

  // Dernière fois qu'un joueur a eu un boost
  private final Map<UUID, Long> lastBoostTime = new ConcurrentHashMap<>();
  // Dernière fois qu'un joueur a fait un clic (pour détecter le double clic)
  private final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();

  // Paramètres
  private static final int HUNGER_COST = 1;
  private static final int SUPER_HUNGER_COST = 4;
  private static final long COOLDOWN_MS = 200; // 0.2 seconde
  private static final long DOUBLE_CLICK_WINDOW = 500; // 0.5 seconde pour double clic
  private static final double BOOST_MULTIPLIER = 1.2; // Force du boost normal
  private static final double SUPER_BOOST_MULTIPLIER = 2.5; // Force du super boost

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
    getLogger().info("LoyFlightImproved enabled.");
  }

  @Override
  public void onDisable() {
    lastBoostTime.clear();
    lastClickTime.clear();
  }

  /**
   * Empêche l'utilisation des fireworks en vol
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onFireworkUse(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    ItemStack item = event.getItem();
    if (item != null && item.getType() == Material.FIREWORK_ROCKET) {
      // Vérifier si le joueur vole avec des élytres
      if (isFlying(player)) {
        event.setCancelled(true);
        player.sendMessage("§8[§c!§8] §cLes fusées ne fonctionnent pas en plein vol. Veuillez utiliser une plume.");
      }
    }
  }

  /**
   * Détecte le clic droit avec plume (boost normal ou super boost)
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    // Vérifier que le joueur a une plume en main
    ItemStack mainHand = player.getInventory().getItemInMainHand();
    if (mainHand.getType() != Material.FEATHER) {
      return;
    }

    if (!canBoost(player)) {
      return;
    }

    UUID uuid = player.getUniqueId();
    long now = System.currentTimeMillis();

    // Vérifier le cooldown
    Long lastBoost = lastBoostTime.get(uuid);
    if (lastBoost != null && (now - lastBoost) < COOLDOWN_MS) {
      return;
    }

    // Vérifier si c'est un double clic (dans les 0.5 secondes)
    Long lastClick = lastClickTime.get(uuid);
    boolean isDoubleClick = lastClick != null && (now - lastClick) <= DOUBLE_CLICK_WINDOW;

    if (isDoubleClick) {
      // SUPER BOOST !
      if (player.getFoodLevel() < SUPER_HUNGER_COST) {
        player.sendMessage("§cVotre barre de faim est trop faible pour le super boost (§4§l4 points requis§c)");
        return;
      }

      // Appliquer le super boost
      player.setFoodLevel(player.getFoodLevel() - SUPER_HUNGER_COST);
      applySuperBoost(player);
      lastBoostTime.put(uuid, now);
      lastClickTime.remove(uuid); // Reset le double clic

      // Son épique pour le super boost - vitesse et puissance !
      player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 0.8f);
      player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1.2f);
      player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.7f);
      player.sendMessage("§6⚡ SUPER BOOST ! ⚡");

    } else {
      // Boost normal
      if (player.getFoodLevel() < HUNGER_COST) {
        player.sendMessage("§cVotre barre de faim est trop faible");
        return;
      }

      // Appliquer le boost normal
      player.setFoodLevel(player.getFoodLevel() - HUNGER_COST);
      applyDirectBoost(player);
      lastBoostTime.put(uuid, now);
      lastClickTime.put(uuid, now); // Marquer pour le potentiel double clic

      // Son d'aile d'enderdragon
      player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
    }
  }

  /**
   * Empêche la consommation de nourriture ou de potions en vol
   */
  @EventHandler
  public void onConsume(PlayerItemConsumeEvent event) {
    Player player = event.getPlayer();

    if (player.isGliding()) {
      event.setCancelled(true);
      player.sendMessage("§cTu ne peux pas manger ni boire de potion en plein vol. Atterris avant !");
    }
  }

  /**
   * Vérifie si le joueur vole avec des élytres
   */
  private boolean isFlying(Player player) {
    ItemStack chest = player.getInventory().getChestplate();
    return !player.isOnGround() && chest != null && chest.getType() == Material.ELYTRA;
  }

  /**
   * Vérifie si le joueur peut utiliser le boost
   */
  private boolean canBoost(Player player) {
    return player.getGameMode() == GameMode.SURVIVAL && isFlying(player);
  }

  /**
   * Applique un boost normal
   */
  private void applyDirectBoost(Player player) {
    Vector direction = player.getLocation().getDirection().normalize();
    Vector currentVelocity = player.getVelocity();
    Vector boostVector = direction.multiply(BOOST_MULTIPLIER);
    Vector newVelocity = currentVelocity.add(boostVector);
    player.setVelocity(newVelocity);
  }

  /**
   * Applique un super boost
   */
  private void applySuperBoost(Player player) {
    Vector direction = player.getLocation().getDirection().normalize();
    Vector currentVelocity = player.getVelocity();
    Vector superBoostVector = direction.multiply(SUPER_BOOST_MULTIPLIER);
    Vector newVelocity = currentVelocity.add(superBoostVector);
    player.setVelocity(newVelocity);
  }
}
