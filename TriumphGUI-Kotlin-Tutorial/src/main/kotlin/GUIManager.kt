package loyfael

import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * Gestionnaire d'interfaces graphiques avec Triumph-GUI
 *
 * Cette classe montre les meilleures pratiques pour utiliser Triumph-GUI
 * avec Kotlin sur PaperMC 1.21.8, en particulier sur les serveurs Leaf.
 *
 * Configuration testée et fonctionnelle ✅
 *
 * @author VotrePseudo
 * @since 1.0.0
 */
class GUIManager(private val plugin: JavaPlugin) {

    companion object {
        // Constantes pour les tailles de GUI courantes
        const val GUI_ROWS_SMALL = 3    // 27 slots
        const val GUI_ROWS_MEDIUM = 4   // 36 slots
        const val GUI_ROWS_LARGE = 6    // 54 slots
    }

    /**
     * Serializer Legacy pour convertir les codes couleurs (§) en Components Adventure
     * Compatible avec tous les serveurs Paper/Leaf
     */
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    /**
     * Convertit un texte avec codes couleurs legacy en Component Adventure
     *
     * IMPORTANT: Désactive l'italique par défaut pour un rendu propre
     *
     * @param text Le texte avec codes couleurs (ex: "§6Hello World")
     * @return Component Adventure formaté
     */
    private fun toComponent(text: String): Component {
        return legacySerializer.deserialize(text)
            .decoration(TextDecoration.ITALIC, false) // Crucial pour éviter l'italique
    }

    /**
     * Convertit une liste de textes en Components Adventure
     * Utilisé principalement pour les lores d'items
     */
    private fun toComponentList(texts: List<String>): List<Component> {
        return texts.map { toComponent(it) }
    }

    /**
     * Crée un ItemStack avec nom et lore formatés en Adventure Components
     *
     * @param material Le matériau de l'item
     * @param name Le nom affiché (avec codes couleurs supportés)
     * @param lore La liste des lignes de description (optionnel)
     * @return ItemStack prêt à être utilisé dans un GUI
     */
    private fun createItemStack(
        material: Material,
        name: String,
        lore: List<String> = emptyList()
    ): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta

        // Application du nom avec Adventure Components
        meta.displayName(toComponent(name))

        // Application du lore si présent
        if (lore.isNotEmpty()) {
            meta.lore(toComponentList(lore))
        }

        item.itemMeta = meta
        return item
    }

    /**
     * Crée une bordure décorative pour embellir les GUIs
     *
     * @param gui Le GUI à décorer
     * @param rows Le nombre de lignes du GUI
     * @param material Le matériau pour la bordure (défaut: vitre noire)
     */
    private fun createDecorativeBorder(
        gui: Gui,
        rows: Int,
        material: Material = Material.BLACK_STAINED_GLASS_PANE
    ) {
        val borderItem = createItemStack(material, "§8")
        val borderGuiItem = GuiItem(borderItem)

        // Première et dernière rangée complètes
        for (slot in 0 until 9) {
            gui.setItem(slot, borderGuiItem)
            gui.setItem((rows - 1) * 9 + slot, borderGuiItem)
        }

        // Bordures gauche et droite pour les rangées intermédiaires
        for (row in 1 until rows - 1) {
            gui.setItem(row * 9, borderGuiItem)     // Colonne gauche
            gui.setItem(row * 9 + 8, borderGuiItem) // Colonne droite
        }
    }

    // ========================================
    // EXEMPLES DE GUIS
    // ========================================

    /**
     * Menu principal - Exemple de base
     *
     * Montre les concepts fondamentaux :
     * - Création d'un GUI
     * - Ajout d'items cliquables
     * - Gestion des événements de clic
     */
    fun openMainMenu(player: Player) {
        // Création du GUI avec titre coloré
        val title = "§6§l⚡ Menu Principal ⚡"

        val gui = Gui.gui()
            .title(toComponent(title))
            .rows(GUI_ROWS_SMALL)
            .disableAllInteractions() // Empêche de prendre les items
            .create()

        // Item d'information
        val infoItem = createItemStack(
            Material.ENCHANTED_BOOK,
            "§b§l📖 Informations",
            listOf(
                "§7",
                "§7Bienvenue dans ce menu",
                "§7d'exemple utilisant Triumph-GUI !",
                "§7",
                "§e▶ §6Clic pour plus d'infos"
            )
        )

        gui.setItem(11, GuiItem(infoItem) { event ->
            val clickedPlayer = event.whoClicked as Player
            clickedPlayer.sendMessage("§a✓ Triumph-GUI fonctionne parfaitement !")
            clickedPlayer.sendMessage("§7Configuration testée sur Paper/Leaf 1.21.8")
        })

        // Item de navigation vers un sous-menu
        val navigationItem = createItemStack(
            Material.COMPASS,
            "§d§l🧭 Navigation",
            listOf(
                "§7",
                "§7Accédez aux autres menus",
                "§7et fonctionnalités",
                "§7",
                "§e▶ §6Clic pour naviguer"
            )
        )

        gui.setItem(13, GuiItem(navigationItem) {
            openNavigationMenu(player)
        })

        // Item de fermeture
        val closeItem = createItemStack(
            Material.BARRIER,
            "§c§l❌ Fermer",
            listOf(
                "§7",
                "§7Ferme cette interface",
                "§7",
                "§e▶ §6Clic pour fermer"
            )
        )

        gui.setItem(15, GuiItem(closeItem) {
            player.closeInventory()
            player.sendMessage("§7Interface fermée !")
        })

        // Ouverture du GUI
        gui.open(player)
    }

    /**
     * Menu de navigation - Exemple avec bordure décorative
     */
    fun openNavigationMenu(player: Player) {
        val gui = Gui.gui()
            .title(toComponent("§8§l⚒ Navigation ⚒"))
            .rows(GUI_ROWS_MEDIUM)
            .disableAllInteractions()
            .create()

        // Ajout de la bordure décorative
        createDecorativeBorder(gui, GUI_ROWS_MEDIUM)

        // Section Joueur
        val playerItem = createItemStack(
            Material.PLAYER_HEAD,
            "§a§l👤 Profil Joueur",
            listOf(
                "§7",
                "§7Gérez votre profil",
                "§7et vos paramètres personnels",
                "§7",
                "§a• §7Statistiques",
                "§a• §7Préférences",
                "§a• §7Historique",
                "§7",
                "§e▶ §6Clic pour ouvrir"
            )
        )
        gui.setItem(11, GuiItem(playerItem) {
            player.sendMessage("§a🎭 Section Profil (à implémenter)")
            openMainMenu(player) // Retour pour l'exemple
        })

        // Section Utilitaires
        val utilsItem = createItemStack(
            Material.REDSTONE,
            "§c§l🔧 Utilitaires",
            listOf(
                "§7",
                "§7Outils et fonctionnalités",
                "§7utiles pour votre expérience",
                "§7",
                "§c• §7Calculatrice",
                "§c• §7Convertisseur",
                "§c• §7Aide en ligne",
                "§7",
                "§e▶ §6Clic pour ouvrir"
            )
        )
        gui.setItem(13, GuiItem(utilsItem) {
            openUtilitiesMenu(player)
        })

        // Section Admin (conditionnelle)
        if (player.hasPermission("exemple.admin")) {
            val adminItem = createItemStack(
                Material.COMMAND_BLOCK,
                "§c§l⚙ Administration",
                listOf(
                    "§7",
                    "§7Panneau d'administration",
                    "§7réservé aux modérateurs",
                    "§7",
                    "§c⚠ §7Accès restreint",
                    "§7",
                    "§e▶ §6Clic pour ouvrir"
                )
            )
            gui.setItem(15, GuiItem(adminItem) {
                player.sendMessage("§c⚙ Panneau Admin (à implémenter)")
            })
        }

        // Bouton retour
        val backItem = createItemStack(
            Material.ARROW,
            "§7← Retour",
            listOf("§7Retourner au menu principal")
        )
        gui.setItem(27, GuiItem(backItem) {
            openMainMenu(player)
        })

        gui.open(player)
    }

    /**
     * Menu utilitaires - Exemple avec gestion avancée des clics
     */
    fun openUtilitiesMenu(player: Player) {
        val gui = Gui.gui()
            .title(toComponent("§c§l🔧 Utilitaires"))
            .rows(GUI_ROWS_LARGE)
            .disableAllInteractions()
            .create()

        createDecorativeBorder(gui, GUI_ROWS_LARGE, Material.RED_STAINED_GLASS_PANE)

        // Calculatrice
        val calculatorItem = createItemStack(
            Material.REDSTONE_TORCH,
            "§6§l🧮 Calculatrice",
            listOf(
                "§7",
                "§7Effectuez des calculs",
                "§7mathématiques simples",
                "§7",
                "§e▶ §6Clic gauche : Addition",
                "§e▶ §6Clic droit : Multiplication"
            )
        )

        gui.setItem(19, GuiItem(calculatorItem) { event ->
            when {
                event.click.isLeftClick -> {
                    player.sendMessage("§6🧮 Mode Addition activé !")
                    player.sendMessage("§7Tapez deux nombres dans le chat")
                }
                event.click.isRightClick -> {
                    player.sendMessage("§6🧮 Mode Multiplication activé !")
                    player.sendMessage("§7Tapez deux nombres dans le chat")
                }
                else -> {
                    player.sendMessage("§7Utilisez clic gauche ou droit")
                }
            }
        })

        // Informations serveur
        val serverInfoItem = createItemStack(
            Material.BEACON,
            "§b§l📊 Infos Serveur",
            listOf(
                "§7",
                "§7Informations en temps réel",
                "§7sur le serveur",
                "§7",
                "§b• §7Joueurs connectés: §e${plugin.server.onlinePlayers.size}",
                "§b• §7Version: §e${plugin.server.version}",
                "§b• §7TPS: §e20.0", // Exemple statique
                "§7",
                "§e▶ §6Clic pour actualiser"
            )
        )

        gui.setItem(21, GuiItem(serverInfoItem) {
            player.sendMessage("§b📊 Informations mises à jour !")
            openUtilitiesMenu(player) // Recharge le menu pour actualiser
        })

        // Générateur de nombres aléatoires
        val randomItem = createItemStack(
            Material.ENDER_PEARL,
            "§5§l🎲 Générateur Aléatoire",
            listOf(
                "§7",
                "§7Génère des nombres",
                "§7aléatoirement",
                "§7",
                "§5• §7Plage: 1-100",
                "§7",
                "§e▶ §6Clic pour générer"
            )
        )

        gui.setItem(23, GuiItem(randomItem) {
            val randomNumber = (1..100).random()
            player.sendMessage("§5🎲 Nombre généré: §e$randomNumber")

            // Effet sonore (optionnel)
            player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
        })

        // Bouton retour
        val backItem = createItemStack(Material.ARROW, "§7← Retour")
        gui.setItem(45, GuiItem(backItem) {
            openNavigationMenu(player)
        })

        // Bouton fermer
        val closeItem = createItemStack(Material.BARRIER, "§c❌ Fermer")
        gui.setItem(49, GuiItem(closeItem) {
            player.closeInventory()
        })

        gui.open(player)
    }

    /**
     * Menu avec pagination - Exemple avancé
     *
     * @param player Le joueur
     * @param items Liste d'éléments à afficher
     * @param page Page actuelle (commence à 0)
     */
    fun openPaginatedMenu(player: Player, items: List<String>, page: Int = 0) {
        val itemsPerPage = 21 // 7x3 slots centraux
        val maxPage = maxOf(0, (items.size - 1) / itemsPerPage)
        val currentPage = page.coerceIn(0, maxPage)

        val gui = Gui.gui()
            .title(toComponent("§9§l📋 Liste (${currentPage + 1}/${maxPage + 1})"))
            .rows(GUI_ROWS_LARGE)
            .disableAllInteractions()
            .create()

        createDecorativeBorder(gui, GUI_ROWS_LARGE, Material.BLUE_STAINED_GLASS_PANE)

        // Affichage des items de la page actuelle
        val startIndex = currentPage * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, items.size)

        var slot = 10 // Position initiale (ligne 2, colonne 2)
        for (i in startIndex until endIndex) {
            val item = createItemStack(
                Material.PAPER,
                "§f${items[i]}",
                listOf(
                    "§7",
                    "§7Élément #${i + 1}",
                    "§7",
                    "§e▶ §6Clic pour sélectionner"
                )
            )

            gui.setItem(slot, GuiItem(item) {
                player.sendMessage("§a✓ Sélectionné: §f${items[i]}")
                player.closeInventory()
            })

            slot++
            // Passage à la ligne suivante (évite les bordures)
            if (slot % 9 == 8) slot += 2
        }

        // Navigation - Page précédente
        if (currentPage > 0) {
            val prevItem = createItemStack(
                Material.ARROW,
                "§7← Page Précédente",
                listOf(
                    "§7",
                    "§7Page §e${currentPage}§7/§e${maxPage + 1}",
                    "§7",
                    "§e▶ §6Clic pour revenir"
                )
            )
            gui.setItem(45, GuiItem(prevItem) {
                openPaginatedMenu(player, items, currentPage - 1)
            })
        }

        // Navigation - Page suivante
        if (currentPage < maxPage) {
            val nextItem = createItemStack(
                Material.ARROW,
                "§7Page Suivante →",
                listOf(
                    "§7",
                    "§7Page §e${currentPage + 2}§7/§e${maxPage + 1}",
                    "§7",
                    "§e▶ §6Clic pour continuer"
                )
            )
            gui.setItem(53, GuiItem(nextItem) {
                openPaginatedMenu(player, items, currentPage + 1)
            })
        }

        // Informations sur la page
        val infoItem = createItemStack(
            Material.BOOK,
            "§6§lInformations",
            listOf(
                "§7",
                "§7Page: §e${currentPage + 1}§7/§e${maxPage + 1}",
                "§7Total: §e${items.size} §7éléments",
                "§7Par page: §e$itemsPerPage",
                "§7"
            )
        )
        gui.setItem(49, GuiItem(infoItem))

        gui.open(player)
    }

    /**
     * Menu de démonstration pour tester toutes les fonctionnalités
     * Appelez cette méthode depuis votre commande principale
     */
    fun openDemoMenu(player: Player) {
        player.sendMessage("§a✓ Ouverture du menu de démonstration...")
        player.sendMessage("§7Configuration Triumph-GUI + Kotlin testée sur Paper/Leaf 1.21.8")

        openMainMenu(player)
    }

    /**
     * Exemple de menu avec données dynamiques
     */
    fun openDynamicListExample(player: Player) {
        // Génération d'une liste d'exemple
        val dynamicItems = mutableListOf<String>()

        // Ajout des joueurs connectés
        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            dynamicItems.add("Joueur: ${onlinePlayer.name}")
        }

        // Ajout d'éléments générés
        for (i in 1..50) {
            dynamicItems.add("Élément généré #$i")
        }

        // Ouverture du menu paginé
        openPaginatedMenu(player, dynamicItems)
    }
}