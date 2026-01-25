package loyfael.manager

import loyfael.Main
import java.sql.Types
import loyfael.model.Shop
import loyfael.model.ShopItem
import loyfael.model.Transaction
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Gestionnaire principal des shops - Implémentation MySQL asynchrone avec cache
 */
class ShopManager(private val plugin: Main) {

    /**
     * Crée un nouveau shop de manière asynchrone avec diagnostic amélioré
     */
    fun createShop(
        owner: Player,
        location: Location,
        type: Shop.ShopType,
        item: ShopItem,
        price: Double,
        stock: Int,
        teleportPolicy: Shop.TeleportPolicy
    ): CompletableFuture<Boolean> {
        return plugin.mySqlManager.supplyAsync<Boolean> {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    plugin.logger.info(
                        "[DEBUG] Création shop - Joueur: ${owner.name}, " +
                                "Monde: ${location.world.name}, " +
                                "Position: ${location.blockX},${location.blockY},${location.blockZ}, " +
                                "Type: $type, Prix: $price, Stock: $stock"
                    )

                    // Vérification de la limite par joueur
                    val playerShopCount = getPlayerShopCount(owner.uniqueId).join()
                    val maxShops = plugin.configManager.maxShopsPerPlayer
                    if (playerShopCount >= maxShops.toLong() && !owner.hasPermission("aetherplayershop.bypasslimit")) {
                        return@supplyAsync false
                    }

                    // Vérification qu'aucun shop n'existe déjà à cette location
                    conn.prepareStatement(
                        "SELECT id FROM shops WHERE world=? AND x=? AND y=? AND z=? LIMIT 1"
                    ).use { ps ->
                        ps.setString(1, location.world.name)
                        ps.setInt(2, location.blockX)
                        ps.setInt(3, location.blockY)
                        ps.setInt(4, location.blockZ)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) return@supplyAsync false
                        }
                    }

                    val shopId = UUID.randomUUID().toString()
                    val itemData = serializeItemStack(item.toItemStack().apply { amount = 1 })

                    conn.prepareStatement(
                        """INSERT INTO shops (id, owner_uuid, owner_name, world, x, y, z, type, price, stock,
                        teleport_policy, item_material, item_data, created_at, active) 
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,1)"""
                    ).use { ps ->
                        ps.setString(1, shopId)
                        ps.setString(2, owner.uniqueId.toString())
                        ps.setString(3, owner.name)
                        ps.setString(4, location.world.name)
                        ps.setInt(5, location.blockX)
                        ps.setInt(6, location.blockY)
                        ps.setInt(7, location.blockZ)
                        ps.setString(8, type.name)
                        ps.setDouble(9, price)
                        ps.setInt(10, stock)
                        ps.setString(11, teleportPolicy.name)
                        ps.setString(12, item.material.name)
                        if (itemData != null) {
                            ps.setBytes(13, itemData)
                        } else {
                            ps.setNull(13, java.sql.Types.BLOB)
                        }
                        ps.executeUpdate()
                    }

                    val shop = Shop(shopId, owner.uniqueId, owner.name, location, type, item, price, stock)
                    plugin.cacheManager.cacheShop(shop)
                    plugin.logger.info("[SUCCESS] Shop créé avec succès - ID: $shopId")
                    true
                }
            } catch (e: Exception) {
                plugin.logger.severe("[ERROR] Erreur création shop (SQL): ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Calcule le prix moyen d'un item sur une période donnée (en jours)
     */
    fun getAveragePriceForItem(item: ShopItem, lastNDays: Int): CompletableFuture<Double?> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        "SELECT AVG(unit_price) FROM transactions WHERE item_key=? AND created_at >= ?"
                    ).use { ps ->
                        val key = buildItemKey(item)
                        val since = Instant.now().minus(Duration.ofDays(lastNDays.toLong()))
                        ps.setString(1, key)
                        ps.setTimestamp(2, java.sql.Timestamp.from(since))
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                val avg = rs.getDouble(1)
                                if (!rs.wasNull()) return@supplyAsync avg
                            }
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL getAveragePriceForItem: ${e.message}")
                null
            }
        }
    }

    /**
     * Récupère un shop par ID avec cache prioritaire
     */
    fun getShop(shopId: String): CompletableFuture<Shop?> {
        val cachedShop = plugin.cacheManager.getCachedShop(shopId)
        if (cachedShop != null) return CompletableFuture.completedFuture(cachedShop)

        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement("SELECT * FROM shops WHERE id=? LIMIT 1").use { ps ->
                        ps.setString(1, shopId)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                val shop = mapShop(rs)
                                plugin.cacheManager.cacheShop(shop)
                                shop
                            } else null
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL getShop: ${e.message}")
                null
            }
        }
    }

    /**
     * Récupère un shop à une location donnée
     */
    fun getShopAtLocation(location: Location): CompletableFuture<Shop?> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        "SELECT * FROM shops WHERE world=? AND x=? AND y=? AND z=? AND active=1 LIMIT 1"
                    ).use { ps ->
                        ps.setString(1, location.world.name)
                        ps.setInt(2, location.blockX)
                        ps.setInt(3, location.blockY)
                        ps.setInt(4, location.blockZ)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) mapShop(rs) else null
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL getShopAtLocation: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Récupère un shop à une location ou dans les positions adjacentes
     */
    fun getShopAtLocationWithAdjacent(location: Location): CompletableFuture<Shop?> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    // Exact
                    conn.prepareStatement(
                        "SELECT * FROM shops WHERE world=? AND x=? AND y=? AND z=? AND active=1 LIMIT 1"
                    ).use { ps ->
                        ps.setString(1, location.world.name)
                        ps.setInt(2, location.blockX)
                        ps.setInt(3, location.blockY)
                        ps.setInt(4, location.blockZ)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) return@supplyAsync mapShop(rs)
                        }
                    }

                    // Adjacent 6 directions
                    val directions = arrayOf(
                        intArrayOf(1, 0, 0), intArrayOf(-1, 0, 0),
                        intArrayOf(0, 1, 0), intArrayOf(0, -1, 0),
                        intArrayOf(0, 0, 1), intArrayOf(0, 0, -1)
                    )

                    for (offset in directions) {
                        conn.prepareStatement(
                            "SELECT * FROM shops WHERE world=? AND x=? AND y=? AND z=? AND active=1 LIMIT 1"
                        ).use { ps ->
                            ps.setString(1, location.world.name)
                            ps.setInt(2, location.blockX + offset[0])
                            ps.setInt(3, location.blockY + offset[1])
                            ps.setInt(4, location.blockZ + offset[2])
                            ps.executeQuery().use { rs ->
                                if (rs.next()) return@supplyAsync mapShop(rs)
                            }
                        }
                    }
                    null
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL getShopAtLocationWithAdjacent: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Nettoie les shops fantômes à une position donnée
     */
    fun cleanupGhostShopsAtLocation(location: Location): CompletableFuture<Int> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.logger.info(
                    "[GHOST-CLEANUP] Vérification des shops fantômes à la position: " +
                            "${location.world.name} ${location.blockX},${location.blockY},${location.blockZ}"
                )

                var cleanedCount = 0
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        """SELECT id, x, y, z, owner_name FROM shops 
                        WHERE world=? AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ?"""
                    ).use { ps ->
                        ps.setString(1, location.world.name)
                        ps.setInt(2, location.blockX - 3)
                        ps.setInt(3, location.blockX + 3)
                        ps.setInt(4, location.blockY - 3)
                        ps.setInt(5, location.blockY + 3)
                        ps.setInt(6, location.blockZ - 3)
                        ps.setInt(7, location.blockZ + 3)

                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                val shopId = rs.getString("id")
                                val shopX = rs.getInt("x")
                                val shopY = rs.getInt("y")
                                val shopZ = rs.getInt("z")
                                val ownerName = rs.getString("owner_name")

                                val shopLocation = Location(location.world, shopX.toDouble(), shopY.toDouble(), shopZ.toDouble())
                                val chestBlock = shopLocation.block
                                val hasChest = chestBlock.type == Material.CHEST || chestBlock.type == Material.TRAPPED_CHEST

                                var hasSign = false
                                if (hasChest) {
                                    val adjacents = arrayOf(
                                        chestBlock.getRelative(1, 0, 0),
                                        chestBlock.getRelative(-1, 0, 0),
                                        chestBlock.getRelative(0, 1, 0),
                                        chestBlock.getRelative(0, -1, 0),
                                        chestBlock.getRelative(0, 0, 1),
                                        chestBlock.getRelative(0, 0, -1)
                                    )

                                    for (adjacent in adjacents) {
                                        if (adjacent.state is Sign) {
                                            val sign = adjacent.state as Sign
                                            val lines = getSignLines(sign)
                                            if (isShopSign(lines, ownerName)) {
                                                hasSign = true
                                                break
                                            }
                                        }
                                    }
                                }

                                if (!hasChest && !hasSign) {
                                    plugin.logger.warning(
                                        "[GHOST-CLEANUP] Shop fantôme détecté - ID: $shopId, " +
                                                "Position: $shopX,$shopY,$shopZ, Propriétaire: $ownerName - Suppression..."
                                    )

                                    conn.prepareStatement("DELETE FROM shops WHERE id=?").use { del ->
                                        del.setString(1, shopId)
                                        del.executeUpdate()
                                    }
                                    plugin.cacheManager.removeCachedShop(shopId)
                                    plugin.logger.info("[GHOST-CLEANUP] Shop fantôme supprimé: $shopId")
                                    cleanedCount++
                                } else {
                                    plugin.logger.info(
                                        "[GHOST-CLEANUP] Shop valide trouvé - ID: $shopId, " +
                                                "Coffre: $hasChest, Panneau: $hasSign"
                                    )
                                }
                            }
                        }
                    }
                }
                plugin.logger.info("[GHOST-CLEANUP] Nettoyage terminé - $cleanedCount shops fantômes supprimés")
                cleanedCount
            } catch (e: Exception) {
                plugin.logger.severe("[ERROR] Erreur lors du nettoyage des shops fantômes: ${e.message}")
                e.printStackTrace()
                0
            }
        }
    }

    /**
     * Méthodes utilitaires pour la détection de shops fantômes
     */
    private fun getSignLines(sign: Sign): Array<String> {
        val lines = sign.getSide(org.bukkit.block.sign.Side.FRONT).lines()
        val result = Array(4) { "" }

        for (i in 0 until minOf(4, lines.size)) {
            result[i] = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lines[i])
        }

        return result
    }

    private fun isShopSign(lines: Array<String>, expectedOwner: String): Boolean {
        if (lines.size < 4) return false

        val firstLine = lines[0].lowercase()
        val lastLine = lines[3]

        val isShopType = firstLine.contains("[shop]") ||
                firstLine.contains("[buy]") ||
                firstLine.contains("[sell]") ||
                firstLine.contains("[achat]") ||
                firstLine.contains("[vente]")

        val isCorrectOwner = lastLine == expectedOwner

        return isShopType && isCorrectOwner
    }

    /**
     * Recherche de shops avec filtres avancés
     */
    fun searchShops(filter: ShopSearchFilter): CompletableFuture<List<Shop>> {
        return plugin.mySqlManager.supplyAsync {
            val sql = StringBuilder("SELECT * FROM shops WHERE active=1")
            val params = mutableListOf<Any>()

            filter.material?.let {
                sql.append(" AND item_material=?")
                params.add(it.name)
            }
            filter.minPrice?.let {
                sql.append(" AND price>=?")
                params.add(it)
            }
            filter.maxPrice?.let {
                sql.append(" AND price<=?")
                params.add(it)
            }
            filter.ownerUUID?.let {
                sql.append(" AND owner_uuid=?")
                params.add(it.toString())
            }
            filter.shopType?.let {
                sql.append(" AND type=?")
                params.add(it.name)
            }

            sql.append(" ORDER BY ")
            sql.append(
                when (filter.sortBy) {
                    ShopSearchFilter.SortBy.PRICE_ASC -> "price ASC"
                    ShopSearchFilter.SortBy.PRICE_DESC -> "price DESC"
                    ShopSearchFilter.SortBy.CREATED_ASC -> "created_at ASC"
                    ShopSearchFilter.SortBy.CREATED_DESC -> "created_at DESC"
                    ShopSearchFilter.SortBy.OWNER -> "owner_name ASC"
                }
            )
            sql.append(" LIMIT ? OFFSET ?")
            params.add(filter.limit)
            params.add(filter.skip)

            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement(sql.toString()).use { ps ->
                        params.forEachIndexed { index, param ->
                            ps.setObject(index + 1, param)
                        }
                        ps.executeQuery().use { rs ->
                            val list = mutableListOf<Shop>()
                            while (rs.next()) {
                                list.add(mapShop(rs))
                            }
                            // Cache results lightly
                            list.forEach { plugin.cacheManager.cacheShop(it) }
                            list
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL searchShops: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Met à jour un shop existant
     */
    fun updateShop(shop: Shop): CompletableFuture<Boolean> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        "UPDATE shops SET price=?, stock=?, active=?, teleport_policy=? WHERE id=?"
                    ).use { ps ->
                        ps.setDouble(1, shop.price)
                        ps.setInt(2, shop.stock)
                        ps.setBoolean(3, shop.active)
                        ps.setString(4, shop.teleportPolicy.name)
                        ps.setString(5, shop.id)
                        val updated = ps.executeUpdate()
                        if (updated > 0) plugin.cacheManager.updateCachedShop(shop)
                        updated > 0
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL updateShop: ${e.message}")
                false
            }
        }
    }

    /**
     * Met à jour le prix d'un shop
     */
    fun updateShopPrice(shopId: String, newPrice: Double): CompletableFuture<Boolean> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement("UPDATE shops SET price=? WHERE id=?").use { ps ->
                        ps.setDouble(1, newPrice)
                        ps.setString(2, shopId)
                        val updated = ps.executeUpdate()
                        if (updated > 0) plugin.cacheManager.removeCachedShop(shopId)
                        updated > 0
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL updateShopPrice: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Met à jour le stock d'un shop
     */
    fun updateShopStock(shopId: String, newStock: Int): CompletableFuture<Boolean> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement("UPDATE shops SET stock=? WHERE id=?").use { ps ->
                        ps.setInt(1, newStock)
                        ps.setString(2, shopId)
                        val updated = ps.executeUpdate()
                        if (updated > 0) plugin.cacheManager.removeCachedShop(shopId)
                        updated > 0
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL updateShopStock: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    fun updateShopItemData(shopId: String, item: ShopItem): CompletableFuture<Boolean> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement("UPDATE shops SET item_material=?, item_data=? WHERE id=?").use { ps ->
                        ps.setString(1, item.material.name)
                        val data = serializeItemStack(item.toItemStack())
                        if (data != null) {
                            ps.setBytes(2, data)
                        } else {
                            ps.setNull(2, Types.BLOB)
                        }
                        ps.setString(3, shopId)
                        val updated = ps.executeUpdate()
                        if (updated > 0) plugin.cacheManager.removeCachedShop(shopId)
                        updated > 0
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL updateShopItemData: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Supprime un shop
     */
    fun deleteShop(shopId: String): CompletableFuture<Boolean> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement("DELETE FROM shops WHERE id=?").use { ps ->
                        ps.setString(1, shopId)
                        val deleted = ps.executeUpdate()
                        if (deleted > 0) plugin.cacheManager.removeCachedShop(shopId)
                        deleted > 0
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL deleteShop: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Supprime un shop à une location donnée
     */
    fun deleteShopAtLocation(location: Location): CompletableFuture<Boolean> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        "DELETE FROM shops WHERE world=? AND x=? AND y=? AND z=?"
                    ).use { ps ->
                        ps.setString(1, location.world.name)
                        ps.setInt(2, location.blockX)
                        ps.setInt(3, location.blockY)
                        ps.setInt(4, location.blockZ)
                        val deleted = ps.executeUpdate()
                        if (deleted > 0) plugin.cacheManager.clearCache()
                        deleted > 0
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL deleteShopAtLocation: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Force la suppression d'un shop (pour les admins)
     */
    fun forceDeleteShopAtLocation(location: Location): CompletableFuture<Boolean> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        """DELETE FROM shops WHERE world=? AND x BETWEEN ? AND ? 
                        AND y BETWEEN ? AND ? AND z BETWEEN ? AND ?"""
                    ).use { ps ->
                        ps.setString(1, location.world.name)
                        ps.setInt(2, location.blockX - 1)
                        ps.setInt(3, location.blockX + 1)
                        ps.setInt(4, location.blockY - 1)
                        ps.setInt(5, location.blockY + 1)
                        ps.setInt(6, location.blockZ - 1)
                        ps.setInt(7, location.blockZ + 1)
                        val deleted = ps.executeUpdate()
                        plugin.cacheManager.clearCache()
                        deleted > 0
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL forceDeleteShopAtLocation: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Enregistre une transaction
     */
    fun recordTransaction(transaction: Transaction): CompletableFuture<Boolean> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        """INSERT INTO transactions (buyer_uuid, seller_uuid, shop_id, item_key, unit_price, quantity, created_at) 
                        VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)"""
                    ).use { ps ->
                        ps.setString(1, transaction.buyerUUID.toString())
                        ps.setString(2, transaction.sellerUUID.toString())
                        ps.setString(3, transaction.shopId)
                        ps.setString(4, buildItemKey(transaction.item))
                        ps.setDouble(5, transaction.price)
                        ps.setInt(6, transaction.quantity)
                        ps.executeUpdate()
                        true
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL recordTransaction: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Compte le nombre de shops d'un joueur
     */
    fun getPlayerShopCount(playerUUID: UUID): CompletableFuture<Long> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        "SELECT COUNT(*) FROM shops WHERE owner_uuid=? AND active=1"
                    ).use { ps ->
                        ps.setString(1, playerUUID.toString())
                        ps.executeQuery().use { rs ->
                            if (rs.next()) rs.getLong(1) else 0L
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL getPlayerShopCount: ${e.message}")
                e.printStackTrace()
                0L
            }
        }
    }

    /**
     * Obtient tous les shops d'un joueur
     */
    fun getPlayerShops(playerUUID: UUID): CompletableFuture<List<Shop>> {
        return plugin.mySqlManager.supplyAsync {
            try {
                plugin.mySqlManager.getConnection().use { conn ->
                    conn.prepareStatement(
                        "SELECT * FROM shops WHERE owner_uuid=? AND active=1 ORDER BY created_at DESC"
                    ).use { ps ->
                        ps.setString(1, playerUUID.toString())
                        val list = mutableListOf<Shop>()
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                list.add(mapShop(rs))
                            }
                        }
                        list
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erreur SQL getPlayerShops: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Classe de filtrage pour les recherches avancées
     */
    class ShopSearchFilter(
        var material: Material? = null,
        var minPrice: Double? = null,
        var maxPrice: Double? = null,
        var ownerUUID: UUID? = null,
        var shopType: Shop.ShopType? = null,
        var sortBy: SortBy = SortBy.CREATED_DESC,
        var limit: Int = 45,
        var skip: Int = 0
    ) {
        enum class SortBy {
            PRICE_ASC, PRICE_DESC, CREATED_ASC, CREATED_DESC, OWNER
        }
    }

    // =====================
    // Mapping & serialization helpers
    // =====================
    private fun mapShop(rs: java.sql.ResultSet): Shop {
        val id = rs.getString("id")
        val ownerUUID = UUID.fromString(rs.getString("owner_uuid"))
        val ownerName = rs.getString("owner_name")
        val worldName = rs.getString("world")
        val x = rs.getInt("x")
        val y = rs.getInt("y")
        val z = rs.getInt("z")
        val type = Shop.ShopType.valueOf(rs.getString("type"))

        // Build item from stored data (material + optional serialized meta)
        val matName = rs.getString("item_material")
        val material = Material.matchMaterial(matName) ?: Material.AIR
        val itemData = rs.getBytes("item_data")
        val deserializedStack = itemData?.let { deserializeItemStack(it) }
        val baseStack = (deserializedStack ?: ItemStack(material, 1)).apply { amount = 1 }
        val item = ShopItem(baseStack)

        val price = rs.getDouble("price")
        val stock = rs.getInt("stock")

        val world = Bukkit.getWorld(worldName)
        if (world == null) {
            plugin.logger.fine("[Catalog] Monde $worldName non chargé pour le shop $id - utilisation des coordonnées enregistrées seulement")
        }

        val shop = if (world != null) {
            val loc = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
            Shop(id, ownerUUID, ownerName, loc, type, item, price, stock)
        } else {
            Shop(
                id = id,
                ownerUUID = ownerUUID,
                ownerName = ownerName,
                world = worldName,
                x = x,
                y = y,
                z = z,
                type = type,
                item = item,
                price = price,
                stock = stock
            )
        }
        shop.active = rs.getBoolean("active")

        try {
            shop.teleportPolicy = Shop.TeleportPolicy.valueOf(rs.getString("teleport_policy"))
        } catch (ignored: Exception) {
        }

        return shop
    }

    private fun buildItemKey(item: ShopItem): String {
        val base = item.material.name
        val cmd = item.customModelData
        return if (cmd > 0) "$base|cmd:$cmd" else base
    }

    private fun serializeItemStack(itemStack: ItemStack): ByteArray? = try {
        itemStack.clone().apply { amount = 1 }.serializeAsBytes()
    } catch (e: Exception) {
        plugin.logger.warning("[WARN] Impossible de sérialiser l'item du shop: ${e.message}")
        null
    }

    private fun deserializeItemStack(data: ByteArray): ItemStack? = try {
        ItemStack.deserializeBytes(data)
    } catch (e: Exception) {
        plugin.logger.warning("[WARN] Impossible de désérialiser l'item du shop: ${e.message}")
        null
    }
}
