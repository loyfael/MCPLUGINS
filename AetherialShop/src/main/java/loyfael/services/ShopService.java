package loyfael.services;

import loyfael.interfaces.IEconomyService;
import loyfael.interfaces.IItemService;
import loyfael.interfaces.IShopRepository;
import loyfael.interfaces.IShopService;
import loyfael.model.ShopItem;
import loyfael.utils.TransactionManager;
import loyfael.utils.UXHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ShopService implements IShopService {

    private final JavaPlugin plugin;
    private final IItemService itemService;
    private final IShopRepository shopRepository;
    private final IEconomyService economyService;
    private List<ShopItem> currentItems;
    private boolean schedulerRunning = false;

    public ShopService(JavaPlugin plugin, IItemService itemService,
                      IShopRepository shopRepository, IEconomyService economyService) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.shopRepository = shopRepository;
        this.economyService = economyService;

        initializeShop();
    }

    private void initializeShop() {
        if (shopRepository.hasValidData() && isStillSameRotationDay()) {
            currentItems = shopRepository.loadShopData();
            plugin.getLogger().info("Données du shop chargées depuis la sauvegarde (même journée de rotation)");
        } else {
            plugin.getLogger().info("Nouvelle journée de rotation détectée, génération de nouveaux items...");
            rotateItems(false); // Ne pas envoyer de webhook au redémarrage
        }
    }
    
    /**
     * Vérifie si on est encore dans la même "journée de rotation"
     * Une journée de rotation commence à l'heure configurée (ex: 12h00)
     */
    private boolean isStillSameRotationDay() {
        try {
            String lastRotationDate = shopRepository.getLastRotationDate();
            if (lastRotationDate == null) {
                return false;
            }
            
            String currentRotationDay = getCurrentRotationDay();
            boolean isSameDay = currentRotationDay.equals(lastRotationDate);
            
            plugin.getLogger().info("Vérification journée rotation - Actuelle: " + currentRotationDay + 
                                   ", Dernière: " + lastRotationDate + ", Identique: " + isSameDay);
            
            return isSameDay;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la vérification de la journée de rotation: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Calcule l'identifiant de la journée de rotation actuelle
     * Format: YYYY-MM-DD (mais peut être le jour précédent si on n'a pas encore atteint l'heure de rotation)
     */
    private String getCurrentRotationDay() {
        int rotationHour = plugin.getConfig().getInt("rotation.hour", 12);
        int rotationMinute = plugin.getConfig().getInt("rotation.minute", 0);
        
        LocalDateTime now = LocalDateTime.now();
        LocalTime rotationTime = LocalTime.of(rotationHour, rotationMinute);
        
        // Si l'heure actuelle est avant l'heure de rotation, on est encore dans la journée précédente
        if (now.toLocalTime().isBefore(rotationTime)) {
            return now.minusDays(1).toLocalDate().toString();
        } else {
            return now.toLocalDate().toString();
        }
    }

    @Override
    public void rotateItems() {
        rotateItems(true); // Par défaut, envoyer le webhook
    }
    
    /**
     * Effectue la rotation des items
     * @param sendWebhook Si true, envoie le webhook Discord (pour les vraies rotations)
     */
    public void rotateItems(boolean sendWebhook) {
        int itemsPerDay = plugin.getConfig().getInt("rotation.items_per_day", 9);
        String rotationDay = getCurrentRotationDay();
        
        currentItems = itemService.generateDailyItems(itemsPerDay);
        shopRepository.saveShopData(currentItems, rotationDay);
        
        // Sauvegarder en base de données SQLite
        if (loyfael.Main.getInstance().getDatabase() != null) {
            loyfael.Main.getInstance().getDatabase().saveRotation(rotationDay, currentItems);
        }
        
        // Envoyer notification Discord SEULEMENT si demandé (pas lors des redémarrages)
        if (sendWebhook && 
            loyfael.Main.getInstance().getDiscordService() != null && 
            loyfael.Main.getInstance().getDiscordService().isEnabled() &&
            plugin.getConfig().getBoolean("discord.send_on_rotation", true)) {
            
            loyfael.Main.getInstance().getDiscordService().sendRotationEmbed(currentItems);
            plugin.getLogger().info("Webhook Discord envoyé pour la nouvelle rotation");
        } else if (!sendWebhook) {
            plugin.getLogger().info("Webhook Discord ignoré (redémarrage serveur)");
        }
        
        plugin.getLogger().info("Rotation du shop effectuée! " + currentItems.size() + " items disponibles.");
    }

    @Override
    public List<ShopItem> getCurrentItems() {
        return currentItems;
    }

    @Override
    public boolean processBuy(Player player, ShopItem item, int quantity) {
        // Validation des paramètres d'entrée
        if (player == null || item == null || quantity <= 0) {
            if (player != null) {
                UXHelper.sendErrorMessage(player, "Paramètres de transaction invalides!");
            }
            plugin.getLogger().warning("Tentative d'achat avec paramètres invalides: player=" + 
                (player != null ? player.getName() : "null") + ", item=" + 
                (item != null ? item.getName() : "null") + ", quantity=" + quantity);
            return false;
        }
        
        // Protection contre les quantités excessives
        if (quantity > 10000) {
            UXHelper.sendErrorMessage(player, "Quantité trop importante! Maximum: 10000");
            return false;
        }
        
        String itemId = item.getMaterial().name() + "_" + (item.getName() != null ? item.getName() : "DEFAULT");
        
        // Transaction atomique avec protection contre la concurrence
        final boolean[] success = {false};
        TransactionManager.executeTransaction(player.getUniqueId(), itemId, () -> {
            try {
                // Re-vérifier les conditions dans la zone critique
                if (!item.canBuy(quantity)) {
                    UXHelper.sendErrorMessage(player, "Stock insuffisant!");
                    return;
                }
                
                double totalCost = (double) item.getCurrentPrice() * quantity;
                
                // Protection contre les débordements arithmétiques
                if (totalCost < 0 || totalCost > Double.MAX_VALUE / 2) {
                    UXHelper.sendErrorMessage(player, "Montant de transaction invalide!");
                    plugin.getLogger().severe("Débordement arithmétique détecté pour " + player.getName() + 
                        ": prix=" + item.getCurrentPrice() + ", quantité=" + quantity);
                    return;
                }
                
                if (!economyService.hasEnoughMoney(player, totalCost)) {
                    UXHelper.sendErrorMessage(player, "Fonds insuffisants! Coût: " + UXHelper.formatMoney(totalCost));
                    return;
                }
                
                if (!hasInventorySpace(player, item, quantity)) {
                    UXHelper.sendErrorMessage(player, "Inventaire plein!");
                    return;
                }
                
                // Transaction économique avec vérification de rollback
                if (!economyService.withdrawMoney(player, totalCost)) {
                    UXHelper.sendErrorMessage(player, "Erreur lors du retrait des fonds!");
                    plugin.getLogger().severe("Échec du retrait économique pour " + player.getName() + 
                        ": montant=" + totalCost);
                    return;
                }
                
                // Tentative de donner les items avec rollback en cas d'échec
                try {
                    item.buy(quantity);
                    giveItemsToPlayer(player, item, quantity);
                    shopRepository.saveShopData(currentItems, LocalDate.now().toString());
                    
                    // Logger la transaction en base de données
                    if (loyfael.Main.getInstance().getDatabase() != null) {
                        loyfael.Main.getInstance().getDatabase().logTransaction(
                            player.getName(), player.getUniqueId().toString(), item, 
                            quantity, item.getCurrentPrice(), totalCost, "BUY"
                        );
                    }
                    
                    success[0] = true;
                } catch (Exception e) {
                    // Rollback de la transaction économique
                    economyService.depositMoney(player, totalCost);
                    item.sell(quantity); // Restaurer le stock
                    UXHelper.sendErrorMessage(player, "Erreur lors de la livraison des items!");
                    plugin.getLogger().severe("Erreur lors de la transaction d'achat pour " + player.getName() + 
                        ": " + e.getMessage());
                }
                
            } catch (Exception e) {
                UXHelper.sendErrorMessage(player, "Erreur interne lors de la transaction!");
                plugin.getLogger().severe("Erreur critique dans processBuy: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        return success[0];
    }
    
    @Override
    public boolean processSell(Player player, ShopItem item, int quantity) {
        // Validation des paramètres d'entrée
        if (player == null || item == null || quantity <= 0) {
            if (player != null) {
                UXHelper.sendErrorMessage(player, "Paramètres de transaction invalides!");
            }
            plugin.getLogger().warning("Tentative de vente avec paramètres invalides: player=" + 
                (player != null ? player.getName() : "null") + ", item=" + 
                (item != null ? item.getName() : "null") + ", quantity=" + quantity);
            return false;
        }
        
        // Protection contre les quantités excessives
        if (quantity > 10000) {
            UXHelper.sendErrorMessage(player, "Quantité trop importante! Maximum: 10000");
            return false;
        }
        
        String itemId = item.getMaterial().name() + "_" + (item.getName() != null ? item.getName() : "DEFAULT");
        
        // Transaction atomique avec protection contre la concurrence
        final boolean[] success = {false};
        TransactionManager.executeTransaction(player.getUniqueId(), itemId, () -> {
            try {
                int playerHas = countPlayerItems(player, item);
                if (playerHas < quantity) {
                    UXHelper.sendErrorMessage(player, "Vous n'avez pas assez d'items!");
                    return;
                }
                
                double earnings = (double) item.getCurrentPrice() * quantity * 0.8;
                
                // Protection contre les débordements arithmétiques
                if (earnings < 0 || earnings > Double.MAX_VALUE / 2) {
                    UXHelper.sendErrorMessage(player, "Montant de transaction invalide!");
                    plugin.getLogger().severe("Débordement arithmétique détecté lors de la vente pour " + player.getName());
                    return;
                }
                
                // Sauvegarder l'état initial pour rollback
                int originalStock = item.getCurrentStock();
                
                try {
                    removeItemsFromPlayer(player, item, quantity);
                    economyService.depositMoney(player, earnings);
                    item.sell(quantity);
                    shopRepository.saveShopData(currentItems, LocalDate.now().toString());
                    
                    // Logger la transaction en base de données
                    if (loyfael.Main.getInstance().getDatabase() != null) {
                        loyfael.Main.getInstance().getDatabase().logTransaction(
                            player.getName(), player.getUniqueId().toString(), item, 
                            quantity, item.getCurrentPrice() * 0.8, earnings, "SELL"
                        );
                    }
                    
                    success[0] = true;
                } catch (Exception e) {
                    // Rollback en cas d'erreur
                    item.setCurrentStock(originalStock);
                    UXHelper.sendErrorMessage(player, "Erreur lors de la transaction de vente!");
                    plugin.getLogger().severe("Erreur lors de la transaction de vente pour " + player.getName() + 
                        ": " + e.getMessage());
                }
                
            } catch (Exception e) {
                UXHelper.sendErrorMessage(player, "Erreur interne lors de la transaction!");
                plugin.getLogger().severe("Erreur critique dans processSell: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        return success[0];
    }

    @Override
    public void startRotationScheduler() {
        if (schedulerRunning) {
            plugin.getLogger().info("Scheduler de rotation déjà en cours, ignoré");
            return;
        }
        
        schedulerRunning = true;
        int hour = plugin.getConfig().getInt("rotation.hour", 4);
        int minute = plugin.getConfig().getInt("rotation.minute", 0);

        new BukkitRunnable() {
            @Override
            public void run() {
                scheduleNextRotation(hour, minute);
            }
        }.runTask(plugin);
    }

    private void scheduleNextRotation(int hour, int minute) {
        LocalTime now = LocalTime.now();
        LocalTime targetTime = LocalTime.of(hour, minute);
        LocalDateTime nextRotation = LocalDateTime.now().with(targetTime);

        if (now.isAfter(targetTime)) {
            nextRotation = nextRotation.plusDays(1);
        }

        long delayTicks = ChronoUnit.SECONDS.between(LocalDateTime.now(), nextRotation) * 20;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            rotateItems();
            startRotationScheduler();
        }, Math.max(1, delayTicks));
    }

    private boolean hasInventorySpace(Player player, ShopItem item, int quantity) {
        int freeSlots = getFreeInventorySlots(player);
        int slotsNeeded = (quantity * item.getAmount() + 63) / 64;
        return freeSlots >= slotsNeeded;
    }

    private int getFreeInventorySlots(Player player) {
        int free = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                free++;
            }
        }
        return free;
    }

    private void giveItemsToPlayer(Player player, ShopItem item, int quantity) {
        for (int i = 0; i < quantity; i++) {
            player.getInventory().addItem(item.toItemStack());
        }
    }

    private int countPlayerItems(Player player, ShopItem shopItem) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == shopItem.getMaterial()) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItemsFromPlayer(Player player, ShopItem shopItem, int quantity) {
        int remaining = quantity;

        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (remaining <= 0) break;

            if (item != null && item.getType() == shopItem.getMaterial()) {
                int itemAmount = item.getAmount();

                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }

        // Mettre à jour l'inventaire
        player.updateInventory();
    }
}
