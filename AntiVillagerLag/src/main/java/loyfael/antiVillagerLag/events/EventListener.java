package loyfael.antiVillagerLag.events;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.util.List;
import java.util.ArrayList;
import loyfael.antiVillagerLag.AntiVillagerLag;
import loyfael.antiVillagerLag.utils.VillagerUtilities;
import loyfael.antiVillagerLag.utils.VillagerCache;

public class EventListener implements Listener {

    AntiVillagerLag plugin;


    public EventListener(AntiVillagerLag plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) return;
        Villager villager = (Villager) event.getRightClicked();

        // Setup new Villagers
        if (!VillagerUtilities.hasMarker(villager, plugin)) {
            VillagerUtilities.setAiCooldown(villager, plugin, 0L);
            VillagerUtilities.setLevelCooldown(villager, plugin, 0L);
            VillagerUtilities.setLastRestock(villager, plugin);
            VillagerUtilities.setMarker(villager, plugin, true);
        }

        // SIMPLE CHECK: Is it on an emerald block?
        boolean block_result = BlockAI.call(villager, plugin, player);

        if (block_result) {
            // ON EMERALD BLOCK → OPTIMIZE AND ALLOW TRADING
            if (VillagerUtilities.getMarker(villager, plugin)) {
                VillagerUtilities.setMarker(villager, plugin, false);
                villager.setAware(false);
                villager.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS,
                    Integer.MAX_VALUE,
                    255,
                    false,
                    false
                ));
                player.sendMessage(VillagerUtilities.colorcodes.cm("&a✅ Villageois stabilisé ! Vous pouvez commercer. Les objets sont réinitialisés à &l6h00 et 19h00 &a(heure Minecraft), et votre villageois doit avoir bougé au &lmoins une fois&l."));
            }
            // CONTINUE NORMALLY - The villager can be clicked
        } else {
            // NOT ON EMERALD BLOCK → BLOCK COMPLETELY
            if (!VillagerUtilities.getMarker(villager, plugin)) {
                VillagerUtilities.setMarker(villager, plugin, true);
                villager.setAware(true);
                villager.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
            }
            player.sendMessage(VillagerUtilities.colorcodes.cm("&4❌ &cPlacez ce villageois sur un &a&lbloc d'émeraude puis cliquez &cpour commercer."));
            event.setCancelled(true);
            return;
        }

        // SIMPLIFIED REST OF CODE (only for villagers on emerald block)
        long currentTime = System.currentTimeMillis() / 1000;
        long vilLevelCooldown = VillagerUtilities.getLevelCooldown(villager, plugin);

        // If the villager is leveling up, block temporarily
        if (vilLevelCooldown > currentTime) {
            String message = plugin.getConfig().getString("messages.cooldown-levelup-message");
            long level_sec = vilLevelCooldown - currentTime;
            message = message.replaceAll("%avlseconds%", Long.toString(level_sec));
            event.getPlayer().sendMessage(VillagerUtilities.colorcodes.cm(message));
            villager.shakeHead();
            event.setCancelled(true);
            return;
        }

        // COMPLETE REMOVAL of manual restock
        // Restock is now handled automatically server-side

        // LAZY RESTOCK: Check and restock automatically if needed
        // Only when a player interacts - ultra-optimized!
        if (!VillagerUtilities.getMarker(villager, plugin)) {
            checkAndRestockIfNeeded(villager);
        }
        
        // TOUJOURS vérifier et supprimer les trades avec Mending (pour test)
        removeMendingTradesComplete(villager);
    }

    /**
     * Checks and restocks a villager only if necessary (optimized lazy approach)
     * Improved performance with cache and player feedback
     */
    private void checkAndRestockIfNeeded(org.bukkit.entity.Villager villager) {
        try {
            long worldTick = villager.getWorld().getFullTime();
            long vilTick = VillagerUtilities.getLastRestock(villager, plugin);

            // Cache to avoid repeated calculations
            Long nextRestockTick = getNextRestockTick(worldTick);
            if (nextRestockTick == null) return; // No restock scheduled

            // Restock only if necessary
            if (worldTick >= nextRestockTick && vilTick < nextRestockTick) {
                VillagerUtilities.restock(villager);
                VillagerUtilities.setLastRestock(villager, plugin);

                // Discrete visual feedback
                villager.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    villager.getLocation().add(0, 2, 0),
                    3, 0.3, 0.3, 0.3, 0
                );
            }
        } catch (Exception e) {
            // Ignore errors silently
        }
    }

    // Cache for next restock to avoid recalculations
    private static long cachedWorldTick = -1;
    private static Long cachedNextRestock = null;

    /**
     * Calculates the next restock tick with caching
     * Avoids repeated calculations for better performance
     */
    private Long getNextRestockTick(long worldTick) {
        // Use cache if data is recent (less than 100 ticks = 5 seconds)
        if (cachedWorldTick >= 0 && Math.abs(worldTick - cachedWorldTick) < 100) {
            return cachedNextRestock;
        }

        long currentDayTick = worldTick % 24000;
        long beginningOfDayTick = worldTick - currentDayTick;

        Long nextRestock = null;
        for (long restockTime : VillagerUtilities.restock_times) {
            long todayRestock = beginningOfDayTick + restockTime;

            if (worldTick < todayRestock) {
                nextRestock = (nextRestock == null) ? todayRestock : Math.min(nextRestock, todayRestock);
            }
        }

        // If no restock today, take the first one tomorrow
        if (nextRestock == null && !VillagerUtilities.restock_times.isEmpty()) {
            nextRestock = beginningOfDayTick + 24000 + VillagerUtilities.restock_times.get(0);
        }

        // Cache it
        cachedWorldTick = worldTick;
        cachedNextRestock = nextRestock;

        return nextRestock;
    }

        /**
     * Vérifie si un ItemStack contient l'enchantement Mending
     * Gère correctement les livres enchantés avec EnchantmentStorageMeta
     */
    private boolean hasMendingEnchantment(ItemStack item) {
        if (item == null) return false;
        
        // Vérifier les enchantements normaux (sur les outils, armes, armures)
        if (item.getEnchantments().containsKey(Enchantment.MENDING)) {
            return true;
        }
        
        // Vérifier les enchantements stockés (sur les livres enchantés)
        if (item.hasItemMeta() && item.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            return meta.hasStoredEnchant(Enchantment.MENDING);
        }
        
        return false;
    }

    /**
     * Supprime TOUS les trades contenant du Mending de manière ultra-efficace
     * Inspiré directement du plugin TradeManager
     */
    private void removeMendingTradesComplete(Villager villager) {
        try {
            // Vérifier si la prévention du Mending est activée
            if (!plugin.getConfig().getBoolean("toggleableoptions.prevent-mending-trades", true)) {
                plugin.getLogger().info("DEBUG: Prévention du Mending désactivée dans la config");
                return;
            }
            
            // Obtenir les trades actuels dans une nouvelle liste
            List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
            
            // Compteur pour debug
            int originalSize = recipes.size();
            plugin.getLogger().info("DEBUG: Villageois a " + originalSize + " trades au total");
            
            // Vérifier chaque trade pour debug
            for (int i = 0; i < recipes.size(); i++) {
                MerchantRecipe recipe = recipes.get(i);
                ItemStack result = recipe.getResult();
                plugin.getLogger().info("DEBUG: Trade " + i + " - Résultat: " + 
                    (result != null ? result.getType() : "null") + 
                    " - Mending détecté: " + hasMendingEnchantment(result));
            }
            
            // Utiliser removeIf pour une suppression ultra-efficace (comme TradeManager)
            recipes.removeIf(recipe -> {
                // Vérifier le résultat du trade
                ItemStack result = recipe.getResult();
                if (hasMendingEnchantment(result)) {
                    plugin.getLogger().info("DEBUG: Suppression d'un trade avec Mending: " + result.getType());
                    return true; // Supprimer ce trade
                }
                
                // Vérifier tous les ingrédients
                for (ItemStack ingredient : recipe.getIngredients()) {
                    if (hasMendingEnchantment(ingredient)) {
                        plugin.getLogger().info("DEBUG: Suppression d'un trade avec ingrédient Mending: " + ingredient.getType());
                        return true; // Supprimer ce trade
                    }
                }
                
                return false; // Garder ce trade
            });
            
            // Appliquer les changements si des trades ont été supprimés
            if (recipes.size() != originalSize) {
                villager.setRecipes(recipes);
                plugin.getLogger().info("DEBUG: Supprimé " + (originalSize - recipes.size()) + 
                                      " trade(s) avec Mending du villageois à " + villager.getLocation());
            } else {
                plugin.getLogger().info("DEBUG: Aucun trade avec Mending trouvé à supprimer");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("DEBUG: Erreur lors de la suppression du Mending: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Supprime le Mending de TOUS les villageois du serveur
     * Utilise la même approche que TradeManager pour une suppression globale
     */
    public void removeAllMendingFromServer() {
        if (!plugin.getConfig().getBoolean("toggleableoptions.prevent-mending-trades", true)) {
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                int totalRemoved = 0;
                int villagersProcessed = 0;
                
                for (World world : Bukkit.getWorlds()) {
                    // Éviter les mondes désactivés si configuré
                    if (plugin.getConfig().getStringList("disabled-worlds").contains(world.getName())) {
                        continue;
                    }
                    
                    // Traiter tous les villageois du monde
                    for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
                        int originalSize = recipes.size();
                        
                        // Suppression ultra-efficace avec removeIf (méthode TradeManager)
                        recipes.removeIf(recipe -> {
                            ItemStack result = recipe.getResult();
                            if (result != null && result.getEnchantments().containsKey(Enchantment.MENDING)) {
                                return true;
                            }
                            
                            for (ItemStack ingredient : recipe.getIngredients()) {
                                if (ingredient != null && ingredient.getEnchantments().containsKey(Enchantment.MENDING)) {
                                    return true;
                                }
                            }
                            
                            return false;
                        });
                        
                        if (recipes.size() != originalSize) {
                            villager.setRecipes(recipes);
                            totalRemoved += (originalSize - recipes.size());
                        }
                        
                        villagersProcessed++;
                    }
                    
                    // Traiter aussi les marchands ambulants
                    for (WanderingTrader trader : world.getEntitiesByClass(WanderingTrader.class)) {
                        List<MerchantRecipe> recipes = new ArrayList<>(trader.getRecipes());
                        int originalSize = recipes.size();
                        
                        recipes.removeIf(recipe -> {
                            ItemStack result = recipe.getResult();
                            if (result != null && result.getEnchantments().containsKey(Enchantment.MENDING)) {
                                return true;
                            }
                            
                            for (ItemStack ingredient : recipe.getIngredients()) {
                                if (ingredient != null && ingredient.getEnchantments().containsKey(Enchantment.MENDING)) {
                                    return true;
                                }
                            }
                            
                            return false;
                        });
                        
                        if (recipes.size() != originalSize) {
                            trader.setRecipes(recipes);
                            totalRemoved += (originalSize - recipes.size());
                        }
                    }
                }
                
                // Log final
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Nettoyage global terminé : " + totalRemoved + 
                                          " trades avec Mending supprimés de " + villagersProcessed + " villageois");
                }
            });
        });
    }

    // Optimized events with priority and fast filtering
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void inventoryMove(InventoryClickEvent event) {
        // Quick checks first
        if (!(event.getInventory().getHolder() instanceof Villager)) return;
        if (!plugin.getConfig().getBoolean("toggleableoptions.preventtrading")) return;

        Villager vil = (Villager) event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();

        // REAL-TIME SECURITY: Check villager position on every trading click
        boolean block_result = BlockAI.call(vil, plugin, player);
        if (!block_result) { // FALSE = the villager is NOT on an emerald block (not optimized)
            // The villager is not optimized, block the trade
            VillagerUtilities.setMarker(vil, plugin, true);
            vil.setAware(true);
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(VillagerUtilities.colorcodes.cm("&c🚫 Le villageois doit être sur un bloc d'émeraude pour commercer !"));
            return;
        }

        // Additional check with marker - only optimized villagers (marker=false) can trade
        if (VillagerUtilities.hasMarker(vil, plugin) && VillagerUtilities.getMarker(vil, plugin)) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(VillagerUtilities.colorcodes.cm("&c🚫 Ce villageois doit être stabilisé pour commercer. Placez-le sur un bloc d'émeraude."));
            return;
        }

        // Vérifier si le trade contient du Mending
        if (plugin.getConfig().getBoolean("toggleableoptions.prevent-mending-trades", true) &&
            event.getCurrentItem() != null && 
            event.getCurrentItem().getEnchantments().containsKey(Enchantment.MENDING)) {
            event.setCancelled(true);
            player.sendMessage(VillagerUtilities.colorcodes.cm("&c🚫 Vous ne pouvez pas échanger d'objets avec raccommodage !"));
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void villagerTradeClick(TradeSelectEvent event) {
        if (!(event.getInventory().getHolder() instanceof Villager)) return;
        if (!plugin.getConfig().getBoolean("toggleableoptions.preventtrading")) return;

        Villager vil = (Villager) event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();

        // REAL-TIME SECURITY: Check villager position on every trade selection
        boolean block_result = BlockAI.call(vil, plugin, player);
        if (!block_result) { // FALSE = the villager is NOT on an emerald block (not optimized)
            // The villager is not optimized, block the trade
            VillagerUtilities.setMarker(vil, plugin, true);
            vil.setAware(true);
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(VillagerUtilities.colorcodes.cm("&c⚡ Les vents perturbent les échanges ! Émeraude requise."));
            return;
        }

        // Additional check with marker - only optimized villagers (marker=false) can trade
        if (VillagerUtilities.hasMarker(vil, plugin) && VillagerUtilities.getMarker(vil, plugin)) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(VillagerUtilities.colorcodes.cm("&c⚡ Ce marcheur n'est pas optimisé dans Nuvalis !"));
            return;
        }

        // Vérifier si le trade sélectionné contient du Mending
        if (plugin.getConfig().getBoolean("toggleableoptions.prevent-mending-trades", true)) {
            MerchantRecipe selectedRecipe = event.getMerchant().getRecipe(event.getIndex());
            if (selectedRecipe != null) {
                ItemStack result = selectedRecipe.getResult();
                if (result != null && result.getEnchantments().containsKey(Enchantment.MENDING)) {
                    event.setCancelled(true);
                    player.sendMessage(VillagerUtilities.colorcodes.cm("&c🚫 Ce villageois ne peut pas vendre d'objets avec Raccommodage !"));
                    return;
                }
                
                // Vérifier aussi les ingrédients
                for (ItemStack ingredient : selectedRecipe.getIngredients()) {
                    if (ingredient != null && ingredient.getEnchantments().containsKey(Enchantment.MENDING)) {
                        event.setCancelled(true);
                        player.sendMessage(VillagerUtilities.colorcodes.cm("&c🚫 Ce villageois ne peut pas vendre d'objets avec Raccommodage !"));
                        return;
                    }
                }
            }
        }
    }

    // Critical optimization - use cache instead of PersistentData
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCancelVillagerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Villager && event.getDamager() instanceof Zombie)) return;

        Villager vil = (Villager) event.getEntity();
        VillagerCache.VillagerData data = VillagerCache.getVillagerData(vil, plugin);

        // Protect only optimized villagers (AI disabled)
        if (data != null && !data.aiState) {
            event.setCancelled(true);
        }
    }

    // Event to handle Villager updating
    @EventHandler
    public void afterTrade(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();
        if(player.hasPermission("avl.disable"))
            return;
        // check if inventory belongs to a Villager Trade Screen
        if (event.getInventory().getHolder() == null) return;
        if (event.getInventory().getHolder() instanceof WanderingTrader) return;
        if(event.getInventory().getType() != InventoryType.MERCHANT) return;

        Villager vil = (Villager) event.getInventory().getHolder();
        // make sure the villager is disabled
        if (!VillagerUtilities.hasMarker(vil, plugin)) return;
        if (VillagerUtilities.getMarker(vil, plugin)) return;

        // handle leveling
        VillagerLevelManager.call(vil, plugin, player);
    }

}
