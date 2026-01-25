package loyfael.gui

import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import kotlinx.coroutines.launch
import loyfael.AetherPlayerDelivery
import loyfael.data.CommandeEnCours
import loyfael.data.EtapeCreation
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestionnaire des interfaces graphiques avec Triumph-GUI
 */
class GUIManager(private val plugin: AetherPlayerDelivery) {

    private val legacySerializer = LegacyComponentSerializer.legacySection()

    // Cache des commandes en cours de création par joueur
    private val commandesEnCours = ConcurrentHashMap<UUID, CommandeEnCours>()

    // === Utils ===
    private fun toComponent(text: String): Component =
        legacySerializer.deserialize(text).decoration(TextDecoration.ITALIC, false)

    private fun toComponentList(lines: List<String>): List<Component> = lines.map { toComponent(it) }

    private fun createItemStack(material: Material, name: String, lore: List<String> = emptyList()): ItemStack {
        val stack = ItemStack(material)
        val meta = stack.itemMeta
        meta.displayName(toComponent(name))
        if (lore.isNotEmpty()) meta.lore(toComponentList(lore))
        stack.itemMeta = meta
        return stack
    }

    private fun creerBordureDecorative(gui: Gui, rows: Int) {
        val pane = GuiItem(createItemStack(Material.BLACK_STAINED_GLASS_PANE, "§8"))
        for (i in 0 until 9) {
            gui.setItem(i, pane)
            gui.setItem((rows - 1) * 9 + i, pane)
        }
        for (r in 1 until rows - 1) {
            gui.setItem(r * 9, pane)
            gui.setItem(r * 9 + 8, pane)
        }
    }

    // === Menu principal ===
    fun ouvrirGUILivraisons(player: Player) {
        val gui = Gui.gui()
            .title(toComponent("§8Livraisons"))
            .rows(6)
            .disableAllInteractions()
            .create()

        creerBordureDecorative(gui, 6)

        gui.setItem(20, GuiItem(createItemStack(
            Material.WRITABLE_BOOK,
            "§a§l📝 Passer une commande",
            listOf("§7Créez une nouvelle demande de livraison", "§e▶ §6Clic pour commencer")
        )) { ouvrirGUICommande(player) })

        gui.setItem(22, GuiItem(createItemStack(
            Material.COMPASS,
            "§b§l📋 Mes commandes",
            listOf("§7Voir toutes vos commandes en cours", "§e▶ §6Clic pour consulter")
        )) { ouvrirGUIMesCommandes(player) })

        gui.setItem(24, GuiItem(createItemStack(
            Material.CHEST_MINECART,
            "§d§l🚚 Livraisons disponibles",
            listOf("§7Voir les commandes à livrer", "§e▶ §6Clic pour voir")
        )) { ouvrirGUILivraisonsDisponibles(player) })

        gui.setItem(49, GuiItem(createItemStack(Material.BARRIER, "§c§l❌ Fermer")) { player.closeInventory() })

        gui.open(player)
    }

    // === Création de commande: étape 1 (choix) ===
    fun ouvrirGUICommande(player: Player) {
        commandesEnCours.computeIfAbsent(player.uniqueId) { CommandeEnCours(it) }.apply { step = EtapeCreation.SELECTION_ITEM }

        val gui = Gui.gui()
            .title(toComponent("§a📝 §8§lNouvelle Commande"))
            .rows(6)
            .disableAllInteractions()
            .create()

        creerBordureDecorative(gui, 6)

        gui.setItem(13, GuiItem(createItemStack(
            Material.NAME_TAG,
            "§e§l🔍 Rechercher un item",
            listOf("§7Par nom FR/EN", "§7Ex: diamant, stone, fer", "§e▶ §6Clic pour rechercher")
        )) { ouvrirGUIRechercheItem(player) })

        gui.setItem(20, GuiItem(createItemStack(Material.COBBLESTONE, "§6§l🏗 Blocs")) { ouvrirGUICategorieItems(player, "blocs") })
        gui.setItem(22, GuiItem(createItemStack(Material.IRON_INGOT, "§b§l⛏ Ressources")) { ouvrirGUICategorieItems(player, "ressources") })
        gui.setItem(24, GuiItem(createItemStack(Material.OAK_LOG, "§2§l🌿 Naturel")) { ouvrirGUICategorieItems(player, "naturel") })

        gui.setItem(45, GuiItem(createItemStack(Material.ARROW, "§7← Retour")) { ouvrirGUILivraisons(player) })
        gui.setItem(49, GuiItem(createItemStack(Material.BARRIER, "§c§l❌ Fermer")) { player.closeInventory() })

        gui.open(player)
    }

    fun ouvrirGUIRechercheItem(player: Player) {
        // Version simple: afficher quelques items autorisés populaires
        val gui = Gui.gui()
            .title(toComponent("§e§l🔍 Items Populaires"))
            .rows(6)
            .disableAllInteractions()
            .create()

        creerBordureDecorative(gui, 6)

        val populaires = listOf(
            Material.DIAMOND, Material.IRON_INGOT, Material.GOLD_INGOT,
            Material.EMERALD, Material.STONE, Material.COBBLESTONE,
            Material.OAK_LOG, Material.WHEAT, Material.COAL, Material.REDSTONE
        )

        var slot = 10
        for (mat in populaires) {
            val name = mat.name.lowercase().replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            val item = createItemStack(mat, "§f$name", listOf("§a▶ §6Clic pour sélectionner"))
            gui.setItem(slot, GuiItem(item) { selectionnerItem(player, mat) })
            slot++
            if (slot % 9 == 8) slot += 2
        }

        gui.setItem(45, GuiItem(createItemStack(Material.ARROW, "§7← Retour")) { ouvrirGUICommande(player) })
        gui.open(player)
    }

    fun ouvrirGUICategorieItems(player: Player, categorie: String) {
        val gui = Gui.gui()
            .title(toComponent(when (categorie) {
                "blocs" -> "§6§l🏗 Blocs de construction"
                "ressources" -> "§b§l⛏ Ressources"
                "naturel" -> "§2§l🌿 Items naturels"
                else -> "§7§l📦 Items"
            }))
            .rows(6)
            .disableAllInteractions()
            .create()

        creerBordureDecorative(gui, 6)

        val items = when (categorie) {
            "blocs" -> listOf(
                Material.STONE, Material.COBBLESTONE, Material.STONE_BRICKS,
                Material.OAK_PLANKS, Material.BRICKS, Material.SANDSTONE,
                Material.SMOOTH_STONE, Material.POLISHED_GRANITE, Material.TERRACOTTA,
                Material.WHITE_CONCRETE, Material.BLACK_CONCRETE
            )
            "ressources" -> listOf(
                Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT,
                Material.IRON_INGOT, Material.COPPER_INGOT, Material.NETHERITE_INGOT,
                Material.COAL, Material.REDSTONE, Material.LAPIS_LAZULI, Material.QUARTZ
            )
            "naturel" -> listOf(
                Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
                Material.WHEAT, Material.CARROT, Material.POTATO, Material.SUGAR_CANE,
                Material.PUMPKIN, Material.MELON, Material.APPLE
            )
            else -> emptyList()
        }

        var slot = 10
        for (mat in items) {
            val name = mat.name.lowercase().replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            gui.setItem(slot, GuiItem(createItemStack(mat, "§f$name")) { selectionnerItem(player, mat) })
            slot++
            if (slot % 9 == 8) slot += 2
        }

        gui.setItem(45, GuiItem(createItemStack(Material.ARROW, "§7← Retour")) { ouvrirGUICommande(player) })
        gui.open(player)
    }

    private fun selectionnerItem(player: Player, material: Material) {
        if (!isItemAutorise(material)) {
            player.sendMessage("§c❌ Cet item n'est pas autorisé pour les livraisons (équipements interdits)")
            return
        }

        val commande = commandesEnCours.computeIfAbsent(player.uniqueId) { CommandeEnCours(it) }
        commande.material = material
        commande.step = EtapeCreation.CONFIGURATION_QUANTITE
        ouvrirGUIQuantite(player)
    }

    fun ouvrirGUIQuantite(player: Player) {
        val commande = commandesEnCours[player.uniqueId] ?: return
        val material = commande.material ?: return

        val gui = Gui.gui()
            .title(toComponent("§b§l📊 Quantité - ${material.name}"))
            .rows(5)
            .disableAllInteractions()
            .create()

        creerBordureDecorative(gui, 5)

        gui.setItem(13, GuiItem(createItemStack(material, "§a§lItem sélectionné", listOf(
            "§7Type: §e${material.name}", "§7Quantité actuelle: §b${commande.quantity}"
        ))))

        gui.setItem(19, GuiItem(createItemStack(Material.RED_CONCRETE, "§c-10")) { commande.quantity = maxOf(1, commande.quantity - 10); ouvrirGUIQuantite(player) })
        gui.setItem(20, GuiItem(createItemStack(Material.ORANGE_CONCRETE, "§e-1")) { commande.quantity = maxOf(1, commande.quantity - 1); ouvrirGUIQuantite(player) })
        gui.setItem(24, GuiItem(createItemStack(Material.LIME_CONCRETE, "§a+1")) { commande.quantity = minOf(2304, commande.quantity + 1); ouvrirGUIQuantite(player) })
        gui.setItem(25, GuiItem(createItemStack(Material.GREEN_CONCRETE, "§2+10")) { commande.quantity = minOf(2304, commande.quantity + 10); ouvrirGUIQuantite(player) })

        gui.setItem(28, GuiItem(createItemStack(Material.CHEST, "§661 Stacks (64)")) { commande.quantity = 64; ouvrirGUIQuantite(player) })
        gui.setItem(30, GuiItem(createItemStack(Material.BARREL, "§610 Stacks (640)")) { commande.quantity = 640; ouvrirGUIQuantite(player) })
        gui.setItem(32, GuiItem(createItemStack(Material.SHULKER_BOX, "§61 Inventaire (2304)")) { commande.quantity = 2304; ouvrirGUIQuantite(player) })

        gui.setItem(36, GuiItem(createItemStack(Material.ARROW, "§7← Retour")) { commande.step = EtapeCreation.SELECTION_ITEM; ouvrirGUICommande(player) })
        gui.setItem(44, GuiItem(createItemStack(Material.EMERALD, "§a§lContinuer →")) { commande.step = EtapeCreation.CONFIGURATION_PRIX; ouvrirGUIPrix(player) })

        gui.open(player)
    }

    fun ouvrirGUIPrix(player: Player) {
        val commande = commandesEnCours[player.uniqueId] ?: return
        val material = commande.material ?: return

        val gui = Gui.gui()
            .title(toComponent("§6§l💰 Prix - ${commande.quantity}x ${material.name}"))
            .rows(5)
            .disableAllInteractions()
            .create()

        creerBordureDecorative(gui, 5)

        gui.setItem(13, GuiItem(createItemStack(material, "§e§lRésumé", listOf(
            "§7Quantité: §b${commande.quantity}", "§7Prix actuel: §6${"%.2f".format(commande.price)}$"
        ))))

        gui.setItem(19, GuiItem(createItemStack(Material.RED_CONCRETE, "§c-100$")) { commande.price = maxOf(0.0, commande.price - 100.0); ouvrirGUIPrix(player) })
        gui.setItem(20, GuiItem(createItemStack(Material.ORANGE_CONCRETE, "§e-10$")) { commande.price = maxOf(0.0, commande.price - 10.0); ouvrirGUIPrix(player) })
        gui.setItem(21, GuiItem(createItemStack(Material.YELLOW_CONCRETE, "§e-1$")) { commande.price = maxOf(0.0, commande.price - 1.0); ouvrirGUIPrix(player) })
        gui.setItem(23, GuiItem(createItemStack(Material.LIME_CONCRETE, "§a+1$")) { commande.price += 1.0; ouvrirGUIPrix(player) })
        gui.setItem(24, GuiItem(createItemStack(Material.GREEN_CONCRETE, "§2+10$")) { commande.price += 10.0; ouvrirGUIPrix(player) })
        gui.setItem(25, GuiItem(createItemStack(Material.EMERALD_BLOCK, "§2+100$")) { commande.price += 100.0; ouvrirGUIPrix(player) })

        gui.setItem(36, GuiItem(createItemStack(Material.ARROW, "§7← Retour")) { commande.step = EtapeCreation.CONFIGURATION_QUANTITE; ouvrirGUIQuantite(player) })
        gui.setItem(44, GuiItem(createItemStack(Material.EMERALD, "§a§lContinuer →")) {
            if (commande.price <= 0.0) { player.sendMessage("§c❌ Le prix doit être > 0"); return@GuiItem }
            commande.step = EtapeCreation.CONFIGURATION_DELAI
            ouvrirGUIDelai(player)
        })

        gui.open(player)
    }

    fun ouvrirGUIDelai(player: Player) {
        val commande = commandesEnCours[player.uniqueId] ?: return
        val material = commande.material ?: return

        val gui = Gui.gui()
            .title(toComponent("§d§l⏰ Délai"))
            .rows(4)
            .disableAllInteractions()
            .create()

        creerBordureDecorative(gui, 4)

        gui.setItem(13, GuiItem(createItemStack(material, "§e§lRésumé", listOf(
            "§7${commande.quantity}x ${material.name}", "§7Prix: §6${"%.2f".format(commande.price)}$"
        ))))

        gui.setItem(19, GuiItem(createItemStack(Material.CLOCK, "§c§l1 jour")) { commande.deadline = LocalDateTime.now().plusDays(1); ouvrirGUIConfirmation(player) })
        gui.setItem(21, GuiItem(createItemStack(Material.CLOCK, "§6§l3 jours")) { commande.deadline = LocalDateTime.now().plusDays(3); ouvrirGUIConfirmation(player) })
        gui.setItem(23, GuiItem(createItemStack(Material.CLOCK, "§a§l7 jours")) { commande.deadline = LocalDateTime.now().plusDays(7); ouvrirGUIConfirmation(player) })

        gui.setItem(27, GuiItem(createItemStack(Material.ARROW, "§7← Retour")) { commande.step = EtapeCreation.CONFIGURATION_PRIX; ouvrirGUIPrix(player) })
        gui.open(player)
    }

    fun ouvrirGUIConfirmation(player: Player) {
        val commande = commandesEnCours[player.uniqueId] ?: return
        val material = commande.material ?: return

        val gui = Gui.gui()
            .title(toComponent("§a§l✅ Confirmation"))
            .rows(5)
            .disableAllInteractions()
            .create()

        creerBordureDecorative(gui, 5)

        val jours = Duration.between(LocalDateTime.now(), commande.deadline).toDays().coerceAtLeast(1)
        gui.setItem(13, GuiItem(createItemStack(material, "§e§lCommande finale", listOf(
            "§7Item: §e${material.name}",
            "§7Quantité: §b${commande.quantity}",
            "§7Prix total: §6${"%.2f".format(commande.price)}$",
            "§7Délai: §d${jours}j"
        ))))

        gui.setItem(30, GuiItem(createItemStack(Material.EMERALD_BLOCK, "§a§l✅ Confirmer & Payer")) {
            // Déclenche la création via CommandeManager en tâche IO
            val qty = commande.quantity
            val price = commande.price
            val mat = material
            val delai = jours.toInt().coerceIn(1, 7)

            plugin.commandeManager.launch {
                val result = plugin.commandeManager.createCommande(
                    player = player,
                    material = mat,
                    nomItem = mat.name,
                    quantite = qty,
                    prixTotal = price,
                    delaiJours = delai
                )

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    when (result) {
                        is loyfael.managers.CommandeResult.SUCCESS -> {
                            commandesEnCours.remove(player.uniqueId)
                            player.sendMessage("§aCommande créée (#${result.commandeId})")
                            ouvrirGUILivraisons(player)
                        }
                        is loyfael.managers.CommandeResult.VALIDATION_ERROR -> player.sendMessage("§c${result.message}")
                        is loyfael.managers.CommandeResult.INSUFFICIENT_FUNDS -> player.sendMessage("§c${result.message}")
                        is loyfael.managers.CommandeResult.PAYMENT_ERROR -> player.sendMessage("§c${result.message}")
                        else -> player.sendMessage("§cErreur lors de la création de la commande")
                    }
                })
            }
        })

        gui.setItem(32, GuiItem(createItemStack(Material.REDSTONE_BLOCK, "§c§l❌ Annuler")) {
            commandesEnCours.remove(player.uniqueId)
            ouvrirGUILivraisons(player)
        })

        gui.setItem(36, GuiItem(createItemStack(Material.BOOK, "§e§l📝 Modifier")) {
            commandesEnCours[player.uniqueId]?.step = EtapeCreation.SELECTION_ITEM
            ouvrirGUICommande(player)
        })

        gui.open(player)
    }

    // === Mes commandes (placeholder minimal pour compiler) ===
    fun ouvrirGUIMesCommandes(player: Player) {
        val gui = Gui.gui()
            .title(toComponent("§b§l📋 Mes Commandes"))
            .rows(6)
            .disableAllInteractions()
            .create()

        creerBordureDecorative(gui, 6)
        gui.setItem(45, GuiItem(createItemStack(Material.ARROW, "§7← Retour")) { ouvrirGUILivraisons(player) })
        gui.setItem(49, GuiItem(createItemStack(Material.BARRIER, "§c§l❌ Fermer")) { player.closeInventory() })
        gui.open(player)
    }

    // === Livraisons disponibles (placeholder minimal pour compiler) ===
    fun ouvrirGUILivraisonsDisponibles(player: Player) {
        val gui = Gui.gui()
            .title(toComponent("§d🚚 §8§lLivraisons Disponibles"))
            .rows(6)
            .disableAllInteractions()
            .create()

        creerBordureDecorative(gui, 6)
        gui.setItem(45, GuiItem(createItemStack(Material.ARROW, "§7← Retour")) { ouvrirGUILivraisons(player) })
        gui.setItem(49, GuiItem(createItemStack(Material.BARRIER, "§c§l❌ Fermer")) { player.closeInventory() })
        gui.open(player)
    }

    // Règle: équipements interdits
    private fun isItemAutorise(material: Material): Boolean {
        val interdits = setOf(
            // Armes
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT,
            // Armures
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            // Outils
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
        )
        return !interdits.contains(material)
    }
}