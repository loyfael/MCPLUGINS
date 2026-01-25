package loyfael.listener;

import loyfael.Main;
import loyfael.model.Shop;
import loyfael.util.ShopInteractionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Gestionnaire simplifié des interactions avec les shops physiques
 * Les fonctionnalités complexes ont été déplacées vers des classes spécialisées
 */
public class ShopInteractionListener implements Listener {

    private final Main plugin;

    public ShopInteractionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();

        // Interaction avec une pancarte de shop
        if (block.getState() instanceof Sign sign) {
            String[] lines = ShopInteractionUtils.getSignLines(sign);
            if (ShopInteractionUtils.isShopSign(lines)) {
                Player player = event.getPlayer();

                // Éviter l'interaction du propriétaire en shift (réservé à la config)
                if (lines[3].equals(player.getName()) && player.isSneaking()) {
                    return; // Géré par ShopCreationListener
                }

                event.setCancelled(true);
                handleShopInteraction(player, block, event.getAction());
            }
        }

        // Interaction avec un coffre de shop
        else if (block.getType().name().contains("CHEST")) {
            handleChestInteraction(event);
        }
    }

    private void handleShopInteraction(@NotNull Player player, @NotNull Block signBlock,
                                     @NotNull org.bukkit.event.block.Action action) {
        // Logs de débogage
        plugin.getLogger().info("[DEBUG] Interaction avec shop - Joueur: " + player.getName() +
            ", Position: " + signBlock.getLocation().getWorld().getName() + " " +
            signBlock.getX() + "," + signBlock.getY() + "," + signBlock.getZ() +
            ", Action: " + action);

        plugin.getShopManager().getShopAtLocationWithAdjacent(signBlock.getLocation())
            .thenAccept(shop -> {
                if (shop == null) {
                    plugin.getLogger().warning("[ERROR] Shop introuvable à la position: " +
                        signBlock.getLocation().getWorld().getName() + " " +
                        signBlock.getX() + "," + signBlock.getY() + "," + signBlock.getZ());
                    player.sendMessage("§cShop introuvable ou inactif. Contactez un administrateur si le problème persiste.");
                    return;
                }

                plugin.getLogger().info("[DEBUG] Shop trouvé - ID: " + shop.getId() +
                    ", Type: " + shop.getType() + ", Propriétaire: " + shop.getOwnerName() +
                    ", Stock: " + shop.getStock() + ", Prix: " + shop.getPrice());

                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        boolean isBuyAction = action == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;
                        boolean isOwner = shop.getOwnerUUID().equals(player.getUniqueId());

                        // Gestion spéciale pour les propriétaires
                        if (isOwner) {
                            if (isBuyAction) {
                                // Clic gauche du propriétaire = Détruire le shop
                                plugin.getShopOwnerManager().handleOwnerDestroyShop(player, shop, signBlock);
                            } else {
                                // Clic droit du propriétaire = Modifier le shop
                                plugin.getShopOwnerManager().handleOwnerModifyShop(player, shop);
                            }
                            return;
                        }

                        // Vérifications préliminaires pour les non-propriétaires
                        if (!performBasicChecks(player, shop, isBuyAction)) {
                            return;
                        }

                        // Déléguer aux gestionnaires spécialisés
                        if (shop.getType() == Shop.ShopType.SELL && isBuyAction) {
                            // Obtenir le coffre associé pour vérifier le stock réel
                            org.bukkit.block.Block chestBlock = ShopInteractionUtils.findAdjacentChest(signBlock);
                            if (chestBlock != null && chestBlock.getState() instanceof org.bukkit.block.Chest chest) {
                                org.bukkit.inventory.Inventory chestInventory = chest.getInventory();
                                int actualStock = ShopInteractionUtils.countItemsInInventory(chestInventory, shop.getItem().toItemStack());

                                // Vérifier s'il y a du stock
                                if (actualStock <= 0) {
                                    player.sendMessage("§c✗ Ce shop n'a plus de stock disponible !");
                                    player.sendMessage("§7Revenez plus tard quand le propriétaire aura réapprovisionné.");

                                    // Mettre à jour l'affichage de la pancarte pour montrer la rupture de stock
                                    ShopInteractionUtils.updateShopSignDisplay(shop, signBlock, chestInventory);
                                    return;
                                }

                                // Il y a du stock - ouvrir le menu d'achat
                                plugin.getPurchaseMenuGUI().openPurchaseMenu(player, shop, chestInventory, actualStock);

                                // Mettre à jour l'affichage de la pancarte avec le stock correct
                                ShopInteractionUtils.updateShopSignDisplay(shop, signBlock, chestInventory);
                            } else {
                                player.sendMessage("§cCoffre du shop introuvable ! Contactez le propriétaire.");
                            }

                        } else {
                            // Seul SELL existe maintenant - toujours clic gauche attendu
                            player.sendMessage("§cUtilisez clic gauche pour acheter dans ce shop.");
                            plugin.getLogger().info("[DEBUG] Mauvaise action - Attendu: clic gauche" +
                                ", Reçu: " + (isBuyAction ? "clic gauche" : "clic droit"));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("[ERROR] Erreur lors du traitement de l'interaction shop: " + e.getMessage());
                        e.printStackTrace();
                        player.sendMessage("§cUne erreur est survenue lors de l'interaction avec le shop. Erreur: " + e.getMessage());
                    }
                });
            })
            .exceptionally(throwable -> {
                plugin.getLogger().severe("[ERROR] Erreur async lors de la récupération du shop: " + throwable.getMessage());
                throwable.printStackTrace();
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§cErreur de base de données lors de l'accès au shop. Contactez un administrateur.");
                });
                return null;
            });
    }

    /**
     * Effectue les vérifications de base avant toute transaction
     */
    private boolean performBasicChecks(@NotNull Player player, @NotNull Shop shop, boolean isBuyAction) {
        // Vérification des permissions
        String permission = isBuyAction ? "aetherplayershop.buy" : "aetherplayershop.sell";
        if (!player.hasPermission(permission)) {
            String action = isBuyAction ? "acheter" : "vendre";
            player.sendMessage("§cVous n'avez pas la permission de " + action + " dans les shops.");
            return false;
        }

        // Vérification du stock pour les achats
        if (isBuyAction && shop.getStock() <= 0) {
            plugin.getLogger().info("[DEBUG] Achat refusé - Stock épuisé");
            player.sendMessage("§cCe shop n'a plus de stock disponible.");
            return false;
        }

        // Vérification de l'économie
        if (plugin.getEconomy() == null) {
            plugin.getLogger().severe("[ERROR] Plugin économique Vault non disponible !");
            player.sendMessage("§cSystème économique non disponible.");
            return false;
        }

        // Vérification de l'argent pour les achats
        if (isBuyAction) {
            double playerBalance = plugin.getEconomy().getBalance(player);
            double price = shop.getPrice();

            if (playerBalance < price) {
                plugin.getLogger().info("[DEBUG] Achat refusé - Argent insuffisant");
                player.sendMessage("§cVous n'avez pas assez d'argent. Prix: §e" +
                    String.format("%.2f", price) + "§c◎, Votre argent: §e" +
                    String.format("%.2f", playerBalance) + "§c◎");
                return false;
            }

            // Vérification de l'espace inventaire
            if (!ShopInteractionUtils.hasInventorySpace(player, shop.getItem().toItemStack())) {
                plugin.getLogger().info("[DEBUG] Achat refusé - Inventaire plein");
                player.sendMessage("§cVous n'avez pas assez de place dans votre inventaire.");
                return false;
            }
        }

        return true;
    }

    private void handleChestInteraction(PlayerInteractEvent event) {
        // Empêcher l'ouverture directe des coffres de shop par des non-propriétaires
        Block chestBlock = event.getClickedBlock();
        Player player = event.getPlayer();

        // Recherche d'une pancarte de shop adjacente
        Block shopSign = ShopInteractionUtils.findAdjacentShopSign(chestBlock);
        if (shopSign != null) {
            Sign sign = (Sign) shopSign.getState();
            String[] lines = ShopInteractionUtils.getSignLines(sign);

            // Si ce n'est pas le propriétaire ou un admin
            if (!lines[3].equals(player.getName()) && !player.hasPermission("aetherplayershop.admin")) {
                event.setCancelled(true);
                player.sendMessage("§cVous ne pouvez pas accéder directement au coffre du shop.");
            }
        }
    }
}
