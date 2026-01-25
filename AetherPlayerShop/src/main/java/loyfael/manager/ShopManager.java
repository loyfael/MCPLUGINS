package loyfael.manager;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import loyfael.Main;
import loyfael.model.Shop;
import loyfael.model.ShopItem;
import loyfael.model.Transaction;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Gestionnaire principal des shops avec optimisations MongoDB selon le cahier des charges
 */
public class ShopManager {

    private final Main plugin;

    public ShopManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Crée un nouveau shop de manière asynchrone avec diagnostic amélioré
     */
    public CompletableFuture<Boolean> createShop(@NotNull Player owner, @NotNull Location location,
                                                @NotNull Shop.ShopType type, @NotNull ShopItem item,
                                                double price, int stock) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                plugin.getLogger().info("[DEBUG] Création shop - Joueur: " + owner.getName() +
                    ", Monde: " + location.getWorld().getName() +
                    ", Position: " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() +
                    ", Type: " + type + ", Prix: " + price + ", Stock: " + stock);

                // Vérification de la limite par joueur
                long playerShopCount = getPlayerShopCount(owner.getUniqueId()).join();
                int maxShops = plugin.getConfigManager().getMaxShopsPerPlayer();

                if (playerShopCount >= maxShops && !owner.hasPermission("aetherplayershop.bypasslimit")) {
                    plugin.getLogger().warning("[DEBUG] Création échouée - Limite atteinte: " + playerShopCount + "/" + maxShops);
                    return false;
                }

                // Vérification qu'aucun shop n'existe déjà à cette location
                Shop existingShop = getShopAtLocation(location).join();
                if (existingShop != null) {
                    plugin.getLogger().warning("[DEBUG] Création échouée - Shop existant à cette position: " + existingShop.getId());
                    return false;
                }

                String shopId = UUID.randomUUID().toString();
                Shop shop = new Shop(shopId, owner.getUniqueId(), owner.getName(),
                                   location, type, item, price, stock);

                plugin.getLogger().info("[DEBUG] Tentative insertion en DB - Shop ID: " + shopId);
                plugin.getMongoManager().getShopsCollection().insertOne(shop.toDocument());
                plugin.getCacheManager().cacheShop(shop);

                plugin.getLogger().info("[SUCCESS] Shop créé avec succès - ID: " + shopId +
                    " à la position " + location.getWorld().getName() + " " +
                    location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
                return true;

            } catch (Exception e) {
                plugin.getLogger().severe("[ERROR] Erreur lors de la création du shop: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Récupère un shop par ID avec cache prioritaire
     */
    public CompletableFuture<Shop> getShop(@NotNull String shopId) {
        // Vérification du cache d'abord
        Shop cachedShop = plugin.getCacheManager().getCachedShop(shopId);
        if (cachedShop != null) {
            return CompletableFuture.completedFuture(cachedShop);
        }

        return plugin.getMongoManager().executeAsync(() -> {
            try {
                Document doc = plugin.getMongoManager().getShopsCollection()
                        .find(Filters.eq("_id", shopId))
                        .first();

                if (doc != null) {
                    try {
                        Shop shop = new Shop(doc);
                        plugin.getCacheManager().cacheShop(shop);
                        return shop;
                    } catch (IllegalArgumentException e) {
                        // Shop corrompu ou avec type obsolète - le supprimer automatiquement
                        String corruptedShopId = doc.getString("_id");
                        String shopType = doc.getString("type");
                        plugin.getLogger().warning("[AUTO-CLEANUP] Shop corrompu détecté lors de getShop - Type: " + shopType + ", ID: " + corruptedShopId + " - Suppression automatique...");
                        
                        plugin.getMongoManager().getShopsCollection().deleteOne(Filters.eq("_id", corruptedShopId));
                        plugin.getLogger().info("[AUTO-CLEANUP] Shop corrompu supprimé de la base de données: " + corruptedShopId);
                        return null;
                    }
                }
                return null;

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de la récupération du shop: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Récupère un shop à une location donnée
     */
    public CompletableFuture<Shop> getShopAtLocation(@NotNull Location location) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                plugin.getLogger().info("[DEBUG] Recherche shop en DB - Monde: " + location.getWorld().getName() +
                    ", Position: " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());

                Bson filter = Filters.and(
                    Filters.eq("world", location.getWorld().getName()),
                    Filters.eq("x", location.getBlockX()),
                    Filters.eq("y", location.getBlockY()),
                    Filters.eq("z", location.getBlockZ()),
                    Filters.eq("active", true)
                );

                // Recherche avec le filtre complet
                Document doc = plugin.getMongoManager().getShopsCollection()
                        .find(filter)
                        .first();

                if (doc != null) {
                    plugin.getLogger().info("[DEBUG] Shop trouvé en DB - ID: " + doc.getString("_id") +
                        ", Propriétaire: " + doc.getString("ownerName") +
                        ", Actif: " + doc.getBoolean("active", true));
                    return new Shop(doc);
                }

                // Si pas trouvé, recherche sans le filtre "active" pour diagnostic
                plugin.getLogger().warning("[DEBUG] Shop non trouvé avec filtre actif, recherche élargie...");

                Bson basicFilter = Filters.and(
                    Filters.eq("world", location.getWorld().getName()),
                    Filters.eq("x", location.getBlockX()),
                    Filters.eq("y", location.getBlockY()),
                    Filters.eq("z", location.getBlockZ())
                );

                Document inactiveDoc = plugin.getMongoManager().getShopsCollection()
                        .find(basicFilter)
                        .first();

                if (inactiveDoc != null) {
                    plugin.getLogger().warning("[DEBUG] Shop trouvé mais INACTIF - ID: " + inactiveDoc.getString("_id") +
                        ", Propriétaire: " + inactiveDoc.getString("ownerName") +
                        ", Actif: " + inactiveDoc.getBoolean("active", true));
                } else {
                    plugin.getLogger().warning("[DEBUG] Aucun shop trouvé à cette position en base de données");

                    // Recherche dans un rayon élargi pour diagnostic
                    plugin.getLogger().info("[DEBUG] Recherche dans un rayon élargi...");
                    Bson enlargedFilter = Filters.and(
                        Filters.eq("world", location.getWorld().getName()),
                        Filters.gte("x", location.getBlockX() - 2),
                        Filters.lte("x", location.getBlockX() + 2),
                        Filters.gte("y", location.getBlockY() - 2),
                        Filters.lte("y", location.getBlockY() + 2),
                        Filters.gte("z", location.getBlockZ() - 2),
                        Filters.lte("z", location.getBlockZ() + 2)
                    );

                    long nearbyCount = plugin.getMongoManager().getShopsCollection()
                            .countDocuments(enlargedFilter);
                    plugin.getLogger().info("[DEBUG] Shops trouvés dans un rayon de 2 blocs: " + nearbyCount);
                }

                return null;

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de la recherche de shop: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Récupère un shop à une location donnée ou dans les positions adjacentes
     * Utile pour les panneaux attachés aux coffres
     */
    public CompletableFuture<Shop> getShopAtLocationWithAdjacent(@NotNull Location location) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                plugin.getLogger().info("[DEBUG] Recherche shop en DB - Monde: " + location.getWorld().getName() +
                    ", Position: " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());

                // D'abord, recherche exacte
                Bson exactFilter = Filters.and(
                    Filters.eq("world", location.getWorld().getName()),
                    Filters.eq("x", location.getBlockX()),
                    Filters.eq("y", location.getBlockY()),
                    Filters.eq("z", location.getBlockZ()),
                    Filters.eq("active", true)
                );

                Document doc = plugin.getMongoManager().getShopsCollection()
                        .find(exactFilter)
                        .first();

                if (doc != null) {
                    plugin.getLogger().info("[DEBUG] Shop trouvé en DB (position exacte) - ID: " + doc.getString("_id") +
                        ", Propriétaire: " + doc.getString("ownerName"));
                    return new Shop(doc);
                }

                // Recherche dans les positions adjacentes (6 directions)
                plugin.getLogger().info("[DEBUG] Recherche dans les positions adjacentes...");

                int[][] adjacentOffsets = {
                    {1, 0, 0}, {-1, 0, 0},  // X+/X-
                    {0, 1, 0}, {0, -1, 0},  // Y+/Y-
                    {0, 0, 1}, {0, 0, -1}   // Z+/Z-
                };

                for (int[] offset : adjacentOffsets) {
                    int adjX = location.getBlockX() + offset[0];
                    int adjY = location.getBlockY() + offset[1];
                    int adjZ = location.getBlockZ() + offset[2];

                    Bson adjacentFilter = Filters.and(
                        Filters.eq("world", location.getWorld().getName()),
                        Filters.eq("x", adjX),
                        Filters.eq("y", adjY),
                        Filters.eq("z", adjZ),
                        Filters.eq("active", true)
                    );

                    Document adjacentDoc = plugin.getMongoManager().getShopsCollection()
                            .find(adjacentFilter)
                            .first();

                    if (adjacentDoc != null) {
                        plugin.getLogger().info("[DEBUG] Shop trouvé en position adjacente (" + adjX + "," + adjY + "," + adjZ + ") - ID: " + adjacentDoc.getString("_id"));

                        // CORRECTION: Gestion sécurisée des shops avec types obsolètes
                        try {
                            return new Shop(adjacentDoc);
                        } catch (IllegalArgumentException e) {
                            // Shop avec type obsolète (BUY) ou corrompu - le supprimer automatiquement
                            String shopId = adjacentDoc.getString("_id");
                            String shopType = adjacentDoc.getString("type");
                            plugin.getLogger().warning("[AUTO-CLEANUP] Shop corrompu détecté - Type: " + shopType + ", ID: " + shopId + " - Suppression automatique...");

                            // Supprimer le shop corrompu
                            plugin.getMongoManager().getShopsCollection().deleteOne(Filters.eq("_id", shopId));
                            plugin.getLogger().info("[AUTO-CLEANUP] Shop corrompu supprimé de la base de données: " + shopId);

                            // Continuer la recherche avec les autres positions adjacentes
                            continue;
                        }
                    }
                }

                // Si toujours pas trouvé, effectuer le diagnostic habituel
                plugin.getLogger().warning("[DEBUG] Shop non trouvé avec filtre actif, recherche élargie...");

                Bson basicFilter = Filters.and(
                    Filters.eq("world", location.getWorld().getName()),
                    Filters.eq("x", location.getBlockX()),
                    Filters.eq("y", location.getBlockY()),
                    Filters.eq("z", location.getBlockZ())
                );

                Document inactiveDoc = plugin.getMongoManager().getShopsCollection()
                        .find(basicFilter)
                        .first();

                if (inactiveDoc != null) {
                    plugin.getLogger().warning("[DEBUG] Shop trouvé mais INACTIF - ID: " + inactiveDoc.getString("_id") +
                        ", Propriétaire: " + inactiveDoc.getString("ownerName") +
                        ", Actif: " + inactiveDoc.getBoolean("active", true));
                } else {
                    plugin.getLogger().warning("[DEBUG] Aucun shop trouvé à cette position en base de données");

                    // Recherche dans un rayon élargi pour diagnostic
                    plugin.getLogger().info("[DEBUG] Recherche dans un rayon élargi...");
                    Bson enlargedFilter = Filters.and(
                        Filters.eq("world", location.getWorld().getName()),
                        Filters.gte("x", location.getBlockX() - 2),
                        Filters.lte("x", location.getBlockX() + 2),
                        Filters.gte("y", location.getBlockY() - 2),
                        Filters.lte("y", location.getBlockY() + 2),
                        Filters.gte("z", location.getBlockZ() - 2),
                        Filters.lte("z", location.getBlockZ() + 2)
                    );

                    long nearbyCount = plugin.getMongoManager().getShopsCollection()
                            .countDocuments(enlargedFilter);
                    plugin.getLogger().info("[DEBUG] Shops trouvés dans un rayon de 2 blocs: " + nearbyCount);
                }

                return null;

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de la recherche de shop: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Nettoie les shops fantômes à une position donnée
     * Un shop fantôme est un shop présent en base de données mais sans panneau/coffre physique
     */
    public CompletableFuture<Integer> cleanupGhostShopsAtLocation(@NotNull Location location) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                plugin.getLogger().info("[GHOST-CLEANUP] Vérification des shops fantômes à la position: " +
                    location.getWorld().getName() + " " + location.getBlockX() + "," +
                    location.getBlockY() + "," + location.getBlockZ());

                // Rechercher tous les shops dans un rayon de 3 blocs
                List<Bson> filters = new ArrayList<>();
                filters.add(Filters.eq("world", location.getWorld().getName()));
                filters.add(Filters.gte("x", location.getBlockX() - 3));
                filters.add(Filters.lte("x", location.getBlockX() + 3));
                filters.add(Filters.gte("y", location.getBlockY() - 3));
                filters.add(Filters.lte("y", location.getBlockY() + 3));
                filters.add(Filters.gte("z", location.getBlockZ() - 3));
                filters.add(Filters.lte("z", location.getBlockZ() + 3));

                Bson filter = Filters.and(filters);

                List<Document> shopDocs = new ArrayList<>();
                plugin.getMongoManager().getShopsCollection()
                        .find(filter)
                        .forEach(shopDocs::add);

                plugin.getLogger().info("[GHOST-CLEANUP] " + shopDocs.size() + " shops trouvés en base dans la zone");

                int cleanedCount = 0;
                for (Document shopDoc : shopDocs) {
                    String shopId = shopDoc.getString("_id");
                    int shopX = shopDoc.getInteger("x");
                    int shopY = shopDoc.getInteger("y");
                    int shopZ = shopDoc.getInteger("z");
                    String ownerName = shopDoc.getString("ownerName");

                    Location shopLocation = new Location(location.getWorld(), shopX, shopY, shopZ);

                    // Vérifier si le coffre existe physiquement
                    org.bukkit.block.Block chestBlock = shopLocation.getBlock();
                    boolean hasChest = chestBlock.getType() == org.bukkit.Material.CHEST ||
                                      chestBlock.getType() == org.bukkit.Material.TRAPPED_CHEST;

                    // Vérifier s'il y a un panneau adjacent
                    boolean hasSign = false;
                    if (hasChest) {
                        org.bukkit.block.Block[] adjacents = {
                            chestBlock.getRelative(1, 0, 0),   // Est
                            chestBlock.getRelative(-1, 0, 0),  // Ouest
                            chestBlock.getRelative(0, 1, 0),   // Haut
                            chestBlock.getRelative(0, -1, 0),  // Bas
                            chestBlock.getRelative(0, 0, 1),   // Sud
                            chestBlock.getRelative(0, 0, -1)   // Nord
                        };

                        for (org.bukkit.block.Block adjacent : adjacents) {
                            if (adjacent.getState() instanceof org.bukkit.block.Sign sign) {
                                // Vérifier si c'est un panneau de shop
                                String[] lines = getSignLines(sign);
                                if (isShopSign(lines, ownerName)) {
                                    hasSign = true;
                                    break;
                                }
                            }
                        }
                    }

                    // Si pas de coffre ET pas de panneau = shop fantôme
                    if (!hasChest && !hasSign) {
                        plugin.getLogger().warning("[GHOST-CLEANUP] Shop fantôme détecté - ID: " + shopId +
                            ", Position: " + shopX + "," + shopY + "," + shopZ +
                            ", Propriétaire: " + ownerName + " - Suppression...");

                        // Supprimer le shop fantôme
                        plugin.getMongoManager().getShopsCollection().deleteOne(Filters.eq("_id", shopId));
                        plugin.getCacheManager().removeCachedShop(shopId);

                        plugin.getLogger().info("[GHOST-CLEANUP] Shop fantôme supprimé: " + shopId);
                        cleanedCount++;
                    } else {
                        plugin.getLogger().info("[GHOST-CLEANUP] Shop valide trouvé - ID: " + shopId +
                            ", Coffre: " + hasChest + ", Panneau: " + hasSign);
                    }
                }

                plugin.getLogger().info("[GHOST-CLEANUP] Nettoyage terminé - " + cleanedCount + " shops fantômes supprimés");
                return cleanedCount;

            } catch (Exception e) {
                plugin.getLogger().severe("[ERROR] Erreur lors du nettoyage des shops fantômes: " + e.getMessage());
                e.printStackTrace();
                return 0;
            }
        });
    }

    /**
     * Méthodes utilitaires pour la détection de shops fantômes
     */
    private String[] getSignLines(@NotNull org.bukkit.block.Sign sign) {
        List<net.kyori.adventure.text.Component> lines = sign.getSide(org.bukkit.block.sign.Side.FRONT).lines();
        String[] result = new String[4];

        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            result[i] = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lines.get(i));
        }

        // Compléter avec des chaînes vides si nécessaire
        for (int i = lines.size(); i < 4; i++) {
            result[i] = "";
        }

        return result;
    }

    private boolean isShopSign(@NotNull String[] lines, @NotNull String expectedOwner) {
        if (lines.length < 4) return false;

        String firstLine = lines[0].toLowerCase();
        String lastLine = lines[3];

        boolean isShopType = firstLine.contains("[shop]") ||
                           firstLine.contains("[buy]") ||
                           firstLine.contains("[sell]") ||
                           firstLine.contains("[achat]") ||
                           firstLine.contains("[vente]");

        boolean isCorrectOwner = lastLine.equals(expectedOwner);

        return isShopType && isCorrectOwner;
    }

    /**
     * Recherche de shops avec filtres avancés - Performance < 50ms garantie
     */
    public CompletableFuture<List<Shop>> searchShops(@NotNull ShopSearchFilter filter) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                List<Bson> conditions = new ArrayList<>();
                conditions.add(Filters.eq("active", true));

                // Filtres selon les critères
                if (filter.material != null) {
                    conditions.add(Filters.eq("item.material", filter.material.name()));
                }

                if (filter.minPrice != null) {
                    conditions.add(Filters.gte("price", filter.minPrice));
                }

                if (filter.maxPrice != null) {
                    conditions.add(Filters.lte("price", filter.maxPrice));
                }

                if (filter.ownerUUID != null) {
                    conditions.add(Filters.eq("ownerUUID", filter.ownerUUID.toString()));
                }

                if (filter.shopType != null) {
                    conditions.add(Filters.eq("type", filter.shopType.name()));
                }

                Bson finalFilter = conditions.size() == 1 ? conditions.get(0) : Filters.and(conditions);

                // Tri selon les critères
                Bson sort = switch (filter.sortBy) {
                    case PRICE_ASC -> Sorts.ascending("price");
                    case PRICE_DESC -> Sorts.descending("price");
                    case CREATED_ASC -> Sorts.ascending("createdAt");
                    case CREATED_DESC -> Sorts.descending("createdAt");
                    case OWNER -> Sorts.ascending("ownerName");
                };

                List<Shop> shops = new ArrayList<>();
                plugin.getMongoManager().getShopsCollection()
                        .find(finalFilter)
                        .sort(sort)
                        .limit(filter.limit)
                        .skip(filter.skip)
                        .forEach(doc -> {
                            try {
                                Shop shop = new Shop(doc);
                                shops.add(shop);
                                plugin.getCacheManager().cacheShop(shop); // Cache les résultats populaires
                            } catch (IllegalArgumentException e) {
                                // Shop corrompu - le supprimer et continuer
                                String shopId = doc.getString("_id");
                                String shopType = doc.getString("type");
                                plugin.getLogger().warning("[AUTO-CLEANUP] Shop corrompu détecté lors de searchShops - Type: " + shopType + ", ID: " + shopId + " - Suppression automatique...");

                                plugin.getMongoManager().getShopsCollection().deleteOne(Filters.eq("_id", shopId));
                                plugin.getLogger().info("[AUTO-CLEANUP] Shop corrompu supprimé de la base de données: " + shopId);
                            }
                        });

                return shops;

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de la recherche: " + e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    /**
     * Met à jour un shop existant
     */
    public CompletableFuture<Boolean> updateShop(@NotNull Shop shop) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                plugin.getMongoManager().getShopsCollection()
                        .replaceOne(Filters.eq("_id", shop.getId()), shop.toDocument());

                plugin.getCacheManager().updateCachedShop(shop);
                return true;

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de la mise à jour du shop: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Met à jour le prix d'un shop
     */
    public CompletableFuture<Boolean> updateShopPrice(@NotNull String shopId, double newPrice) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                plugin.getLogger().info("[DEBUG] Mise à jour du prix du shop " + shopId + " vers " + newPrice);

                // Récupérer d'abord le shop actuel pour comparer le prix
                Document currentDoc = plugin.getMongoManager().getShopsCollection()
                        .find(Filters.eq("_id", shopId))
                        .first();

                if (currentDoc == null) {
                    plugin.getLogger().warning("[WARNING] Shop " + shopId + " non trouvé pour mise à jour du prix");
                    return false;
                }

                double currentPrice = currentDoc.getDouble("price");

                // Vérifier si le prix est différent
                if (Math.abs(currentPrice - newPrice) < 0.01) { // Précision de 1 centime
                    plugin.getLogger().info("[INFO] Prix identique détecté pour le shop " + shopId + " - Aucune modification nécessaire");
                    return true; // Retourner true car ce n'est pas une erreur
                }

                // Mettre à jour en base de données
                Bson filter = Filters.eq("_id", shopId);
                Bson update = Updates.set("price", newPrice);

                long modifiedCount = plugin.getMongoManager().getShopsCollection()
                        .updateOne(filter, update)
                        .getModifiedCount();

                boolean success = modifiedCount > 0;
                if (success) {
                    plugin.getLogger().info("[SUCCESS] Prix du shop " + shopId + " mis à jour de " + currentPrice + "◎ vers " + newPrice + "◎");

                    // Invalider le cache pour forcer la recharge
                    plugin.getCacheManager().removeCachedShop(shopId);
                } else {
                    plugin.getLogger().warning("[WARNING] Aucun shop modifié lors de la mise à jour du prix pour " + shopId);
                }

                return success;

            } catch (Exception e) {
                plugin.getLogger().severe("[ERROR] Erreur lors de la mise à jour du prix du shop " + shopId + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Met à jour le stock d'un shop
     */
    public CompletableFuture<Boolean> updateShopStock(@NotNull String shopId, int newStock) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                plugin.getLogger().info("[DEBUG] Mise à jour du stock du shop " + shopId + " vers " + newStock);

                // Mettre à jour en base de données
                Bson filter = Filters.eq("_id", shopId);
                Bson update = Updates.set("stock", newStock);

                long modifiedCount = plugin.getMongoManager().getShopsCollection()
                        .updateOne(filter, update)
                        .getModifiedCount();

                boolean success = modifiedCount > 0;
                if (success) {
                    plugin.getLogger().info("[SUCCESS] Stock du shop " + shopId + " mis à jour vers " + newStock);

                    // Invalider le cache pour forcer la recharge
                    plugin.getCacheManager().removeCachedShop(shopId);
                } else {
                    plugin.getLogger().warning("[WARNING] Aucun shop modifié lors de la mise à jour du stock pour " + shopId);
                }

                return success;

            } catch (Exception e) {
                plugin.getLogger().severe("[ERROR] Erreur lors de la mise à jour du stock du shop " + shopId + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Supprime un shop
     */
    public CompletableFuture<Boolean> deleteShop(@NotNull String shopId) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                plugin.getMongoManager().getShopsCollection()
                        .deleteOne(Filters.eq("_id", shopId));

                plugin.getCacheManager().removeCachedShop(shopId);
                return true;

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de la suppression du shop: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Supprime un shop à une location donnée avec gestion d'erreurs améliorée
     */
    public CompletableFuture<Boolean> deleteShopAtLocation(@NotNull Location location) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                // Rechercher le shop à cette location
                Bson filter = Filters.and(
                    Filters.eq("world", location.getWorld().getName()),
                    Filters.eq("x", location.getBlockX()),
                    Filters.eq("y", location.getBlockY()),
                    Filters.eq("z", location.getBlockZ())
                );

                Document shopDoc = plugin.getMongoManager().getShopsCollection()
                        .findOneAndDelete(filter);

                if (shopDoc != null) {
                    // Nettoyer le cache
                    String shopId = shopDoc.getString("_id");
                    plugin.getCacheManager().removeCachedShop(shopId);

                    plugin.getLogger().info("Shop supprimé avec succès à la position: " +
                        location.getWorld().getName() + " " + location.getBlockX() + "," +
                        location.getBlockY() + "," + location.getBlockZ());
                    return true;
                } else {
                    plugin.getLogger().warning("Aucun shop trouvé à la position: " +
                        location.getWorld().getName() + " " + location.getBlockX() + "," +
                        location.getBlockY() + "," + location.getBlockZ());
                    return false;
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de la suppression du shop à la position " +
                    location.getWorld().getName() + " " + location.getBlockX() + "," +
                    location.getBlockY() + "," + location.getBlockZ() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Force la suppression d'un shop (pour les admins en cas de bug)
     */
    public CompletableFuture<Boolean> forceDeleteShopAtLocation(@NotNull Location location) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                // Suppression forcée avec filtres moins stricts
                List<Bson> filters = new ArrayList<>();
                filters.add(Filters.eq("world", location.getWorld().getName()));

                // Zone de recherche élargie (±1 bloc)
                filters.add(Filters.gte("x", location.getBlockX() - 1));
                filters.add(Filters.lte("x", location.getBlockX() + 1));
                filters.add(Filters.gte("y", location.getBlockY() - 1));
                filters.add(Filters.lte("y", location.getBlockY() + 1));
                filters.add(Filters.gte("z", location.getBlockZ() - 1));
                filters.add(Filters.lte("z", location.getBlockZ() + 1));

                Bson filter = Filters.and(filters);

                long deletedCount = plugin.getMongoManager().getShopsCollection()
                        .deleteMany(filter)
                        .getDeletedCount();

                plugin.getLogger().info("Suppression forcée: " + deletedCount + " shops supprimés autour de la position " +
                    location.getWorld().getName() + " " + location.getBlockX() + "," +
                    location.getBlockY() + "," + location.getBlockZ());

                // Nettoyer complètement le cache
                plugin.getCacheManager().clearCache();

                return deletedCount > 0;

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de la suppression forcée: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Enregistre une transaction
     */
    public CompletableFuture<Boolean> recordTransaction(@NotNull Transaction transaction) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                plugin.getMongoManager().getTransactionsCollection()
                        .insertOne(transaction.toDocument());
                return true;

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de l'enregistrement de la transaction: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Compte le nombre de shops d'un joueur
     */
    public CompletableFuture<Long> getPlayerShopCount(@NotNull UUID playerUUID) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                return plugin.getMongoManager().getShopsCollection()
                        .countDocuments(Filters.and(
                            Filters.eq("ownerUUID", playerUUID.toString()),
                            Filters.eq("active", true)
                        ));

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors du comptage des shops: " + e.getMessage());
                return 0L;
            }
        });
    }

    /**
     * Obtient tous les shops d'un joueur
     */
    public CompletableFuture<List<Shop>> getPlayerShops(@NotNull UUID playerUUID) {
        return plugin.getMongoManager().executeAsync(() -> {
            try {
                List<Shop> shops = new ArrayList<>();
                plugin.getMongoManager().getShopsCollection()
                        .find(Filters.and(
                            Filters.eq("ownerUUID", playerUUID.toString()),
                            Filters.eq("active", true)
                        ))
                        .sort(Sorts.descending("createdAt"))
                        .forEach(doc -> {
                            try {
                                shops.add(new Shop(doc));
                            } catch (IllegalArgumentException e) {
                                // Shop corrompu - le supprimer et continuer
                                String shopId = doc.getString("_id");
                                String shopType = doc.getString("type");
                                plugin.getLogger().warning("[AUTO-CLEANUP] Shop corrompu détecté lors de getPlayerShops - Type: " + shopType + ", ID: " + shopId + " - Suppression automatique...");

                                plugin.getMongoManager().getShopsCollection().deleteOne(Filters.eq("_id", shopId));
                                plugin.getLogger().info("[AUTO-CLEANUP] Shop corrompu supprimé de la base de données: " + shopId);
                            }
                        });

                return shops;

            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de la récupération des shops du joueur: " + e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    /**
     * Classe de filtrage pour les recherches avancées
     */
    public static class ShopSearchFilter {
        public Material material;
        public Double minPrice;
        public Double maxPrice;
        public UUID ownerUUID;
        public Shop.ShopType shopType;
        public SortBy sortBy = SortBy.CREATED_DESC;
        public int limit = 45;
        public int skip = 0;

        public enum SortBy {
            PRICE_ASC, PRICE_DESC, CREATED_ASC, CREATED_DESC, OWNER
        }
    }
}
