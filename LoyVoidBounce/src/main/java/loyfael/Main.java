package loyfael;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener {
  private final Set<LivingEntity> currentlyProcessing = new HashSet<>();
  private final Set<Player> messageAlreadySent = new HashSet<>();

  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
    getLogger().info("LoyVoidBounce activé avec succès !");
  }

  @Override
  public void onDisable() {
    currentlyProcessing.clear();
    messageAlreadySent.clear();
    getLogger().info("LoyVoidBounce désactivé proprement.");
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onEntityFall(EntityDamageEvent event) {
    try {
      if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
      if (!(event.getEntity() instanceof LivingEntity entity)) return;
      if (entity.isDead()) return;

      if (currentlyProcessing.contains(entity)) return;

      Location loc = entity.getLocation();
      World world = loc.getWorld();
      if (world == null || loc.getY() >= 0) return;

      if (entity instanceof Player player) {
        if (!player.isOnline() || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
          return;
        }
      }

      currentlyProcessing.add(entity);

      boolean shouldBounce = Math.random() < 0.6;

      if (shouldBounce) {
        applyBounce(entity, world, loc);
        event.setCancelled(true);
        Bukkit.getScheduler().runTaskLater(this, () -> currentlyProcessing.remove(entity), 100L);
      } else {
        if (entity instanceof Player player && player.isGliding()) {
          player.sendMessage(Component.text("Tu es bloqué dans le vide…", NamedTextColor.RED));
          player.sendMessage(Component.text("Enlève tes élytres pour avoir une chance de remonter, ou utilise ta plume si tu as encore de la faim.", NamedTextColor.GRAY));
        }

        Bukkit.getScheduler().runTaskLater(this, () -> currentlyProcessing.remove(entity), 60L); // 3 secondes
      }

    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Erreur dans le gestionnaire de void", e);
      if (event.getEntity() instanceof LivingEntity entity) {
        currentlyProcessing.remove(entity);
      }
    }
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();

    if (!player.isOnline() || player.isDead()) return;
    if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;

    Location loc = player.getLocation();
    if (loc.getY() > -63) return;

    if (player.isGliding() && !messageAlreadySent.contains(player)) {
//      player.sendMessage(Component.text("Tu es bloqué dans le vide…", NamedTextColor.RED));
//      player.sendMessage(Component.text("Enlève tes élytres pour avoir une chance de remonter, ou utilise ta plume si tu as encore de la faim.", NamedTextColor.GRAY));
      messageAlreadySent.add(player);

      // Nettoyage du message après 5 secondes
      Bukkit.getScheduler().runTaskLater(this, () -> messageAlreadySent.remove(player), 100L);
    }
  }

  private void applyBounce(LivingEntity entity, World world, Location loc) {
    try {
      if (entity instanceof Player player) {
        boolean hadElytraActive = player.isGliding();
        if (hadElytraActive) {
          player.setGliding(false);
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 100, false, false), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 400, 0, false, false), true);
        player.sendMessage(Component.text("Tu as pris un courant ascendant !", NamedTextColor.AQUA));

        if (hadElytraActive) {
          Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline() && !player.isDead() &&
                    player.getInventory().getChestplate() != null &&
                    player.getInventory().getChestplate().getType() == Material.ELYTRA) {
              player.setGliding(true);
            }
          }, 65L);
        }

      } else {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 3, false, false), true);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 400, 0, false, false), true);
      }

      world.playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);
      world.spawnParticle(Particle.END_ROD, loc, 25, 0.8, 1.2, 0.8, 0.15);
      world.spawnParticle(Particle.PORTAL, loc, 15, 0.5, 0.5, 0.5, 0.1);

    } catch (Exception e) {
      getLogger().log(Level.WARNING, "Erreur lors du rebond pour: " + entity.getType(), e);
    }
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    currentlyProcessing.remove(event.getPlayer());
    messageAlreadySent.remove(event.getPlayer());
  }

  @EventHandler
  public void onEntityDeath(EntityDeathEvent event) {
    currentlyProcessing.remove(event.getEntity());
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    currentlyProcessing.remove(event.getPlayer());
    messageAlreadySent.remove(event.getPlayer());
  }

  @EventHandler
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    currentlyProcessing.remove(event.getPlayer());
    messageAlreadySent.remove(event.getPlayer());
  }
}
