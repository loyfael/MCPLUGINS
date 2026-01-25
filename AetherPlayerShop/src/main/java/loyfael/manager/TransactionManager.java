package loyfael.manager;

import loyfael.Main;
import loyfael.model.Shop;
import loyfael.model.ShopItem;
import loyfael.model.Transaction;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Gestionnaire des transactions d'achat et de vente
 */
public class TransactionManager {

    private final Main plugin;

    public TransactionManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Traite une transaction d'achat (ouvre le menu d'achat)
     */
    public void handleBuyTransaction(@NotNull Player player, @NotNull Shop shop) {
        try {
            plugin.getLogger().info("[DEBUG] Traitement transaction d'achat - Joueur: " + player.getName() +
                ", Shop: " + shop.getId());

            double price = shop.getPrice();
            ItemStack item = shop.getItem().toItemStack();

            // Vérifier le stock physique dans le coffre
            Location chestLocation = new Location(
                org.bukkit.Bukkit.getWorld(shop.getWorld()),
                shop.getX(), shop.getY(), shop.getZ()
            );

            Block chestBlock = chestLocation.getBlock();
            if (!chestBlock.getType().name().contains("CHEST")) {
                plugin.getLogger().severe("[ERROR] Coffre introuvable à la position du shop " + shop.getId());
                player.sendMessage("§cErreur: coffre du shop introuvable.");
                return;
            }

            org.bukkit.block.Chest chest = (org.bukkit.block.Chest) chestBlock.getState();
            Inventory chestInventory = chest.getInventory();

            // Synchroniser le stock réel avec la base de données
            int realStock = calculateRealStock(chestInventory, item);

            // Mettre à jour le stock en base si différent
            if (realStock != shop.getStock()) {
                plugin.getLogger().info("[DEBUG] Synchronisation du stock - DB: " + shop.getStock() +
                    ", Coffre: " + realStock + " -> Mise à jour");
                shop.setStock(realStock);
                plugin.getShopManager().updateShop(shop);
            }

            // Vérifier s'il y a l'item dans le coffre
            if (realStock <= 0 || !chestInventory.containsAtLeast(item, 1)) {
                plugin.getLogger().info("[DEBUG] Achat refusé - Item non disponible dans le coffre (Stock réel: " +
                    realStock + ")");
                player.sendMessage("§c✖ Le coffre est vide !");
                player.sendMessage("§7Le propriétaire doit remettre du stock pour que vous puissiez acheter.");

                // Mettre à jour le stock à 0
                shop.setStock(0);
                plugin.getShopManager().updateShop(shop);
                return;
            }

            // Ouvrir le menu d'achat
            plugin.getPurchaseMenuGUI().openPurchaseMenu(player, shop, chestInventory, realStock);

        } catch (Exception e) {
            plugin.getLogger().severe("[ERROR] Erreur critique lors de handleBuyTransaction: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cErreur critique lors de l'achat. Contactez un administrateur.");
        }
    }

    /**
     * Traite une transaction de vente
     */
    public void handleSellTransaction(@NotNull Player player, @NotNull Shop shop) {
        try {
            plugin.getLogger().info("[DEBUG] Tentative de vente - Joueur: " + player.getName() +
                ", Shop: " + shop.getId());

            // Vérifications similaires mais pour la vente
            if (!player.hasPermission("aetherplayershop.sell")) {
                player.sendMessage("§cVous n'avez pas la permission de vendre dans les shops.");
                return;
            }

            if (shop.getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage("§cVous ne pouvez pas vendre dans votre propre shop.");
                return;
            }

            // Vérifier que le joueur a l'item requis
            ItemStack requiredItem = shop.getItem().toItemStack();
            if (!hasMatchingItem(player, requiredItem)) {
                player.sendMessage("§cVous n'avez pas l'item requis: §e" + shop.getItem().getDisplayName());
                return;
            }

            // Vérifier que le propriétaire du shop a assez d'argent
            double price = shop.getPrice();
            UUID ownerUUID = shop.getOwnerUUID();
            double ownerBalance = plugin.getEconomy().getBalance(org.bukkit.Bukkit.getOfflinePlayer(ownerUUID));

            plugin.getLogger().info("[DEBUG] Vérification économique vente - Balance propriétaire: " + ownerBalance +
                ", Prix à payer: " + price);

            if (ownerBalance < price) {
                player.sendMessage("§cLe propriétaire du shop n'a pas assez d'argent pour acheter vos items.");
                return;
            }

            // Effectuer la transaction de vente
            performSellTransaction(player, shop);

        } catch (Exception e) {
            plugin.getLogger().severe("[ERROR] Erreur lors de handleSellTransaction: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cErreur lors de la vente: " + e.getMessage());
        }
    }

    /**
     * Exécute la transaction de vente
     */
    private void performSellTransaction(@NotNull Player player, @NotNull Shop shop) {
        try {
            plugin.getLogger().info("[DEBUG] Exécution de la transaction de vente - Joueur: " + player.getName() +
                ", Shop: " + shop.getId());

            double price = shop.getPrice();
            ItemStack item = shop.getItem().toItemStack();

            // Retirer l'item de l'inventaire du joueur
            player.getInventory().removeItem(item);

            // Créditer l'argent au joueur
            EconomyResponse response = plugin.getEconomy().depositPlayer(player, price);
            if (!response.transactionSuccess()) {
                player.sendMessage("§cErreur lors du crédit de l'argent: " + response.errorMessage);
                plugin.getLogger().severe("[ERROR] Échec crédit argent - Joueur: " + player.getName() +
                    ", Montant: " + price + ", Erreur: " + response.errorMessage);
                return;
            }

            // Mettre à jour le stock du shop
            shop.setStock(shop.getStock() + 1);
            plugin.getShopManager().updateShop(shop)
                .thenAccept(success -> {
                    if (!success) {
                        plugin.getLogger().severe("[ERROR] Échec mise à jour stock pour shop " + shop.getId());
                        // Rollback de la transaction
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getEconomy().withdrawPlayer(player, price);
                            player.getInventory().addItem(item);
                            player.sendMessage("§cErreur lors de la mise à jour du shop. Transaction annulée.");
                        });
                        return;
                    }

                    plugin.getLogger().info("[SUCCESS] Transaction de vente réussie - Joueur: " + player.getName() +
                        ", Shop: " + shop.getId() + ", Montant: " + price);
                });

            // Messages de confirmation
            player.sendMessage("§aVente effectuée ! Item: §e" + shop.getItem().getDisplayName() +
                "§a, Prix: §e" + String.format("%.2f", price) + "◎");

            // Enregistrer la transaction
            Transaction transaction = new Transaction(
                shop.getId(),
                player.getUniqueId(),
                shop.getOwnerUUID(),
                Transaction.TransactionType.SELL,
                shop.getItem(),
                price,
                1
            );
            plugin.getShopManager().recordTransaction(transaction);

        } catch (Exception e) {
            plugin.getLogger().severe("[ERROR] Erreur critique lors de performSellTransaction: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cErreur critique lors de la vente. Contactez un administrateur.");
        }
    }

    /**
     * Calcule le stock réel dans le coffre
     */
    public int calculateRealStock(@NotNull Inventory chestInventory, @NotNull ItemStack targetItem) {
        int realStock = 0;
        for (ItemStack invItem : chestInventory.getContents()) {
            if (invItem != null && invItem.isSimilar(targetItem)) {
                realStock += invItem.getAmount();
            }
        }
        return realStock;
    }

    /**
     * Vérifie si le joueur a un item correspondant
     */
    private boolean hasMatchingItem(@NotNull Player player, @NotNull ItemStack item) {
        for (ItemStack inventoryItem : player.getInventory().getContents()) {
            if (inventoryItem != null && inventoryItem.isSimilar(item)) {
                return true;
            }
        }
        return false;
    }
}
