package loyfael.manager

import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import dev.triumphteam.gui.components.GuiType
import loyfael.ClassePlugin
import loyfael.data.Classe
import loyfael.data.PassiveType
import loyfael.data.PlayerData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * GUI Manager with Triumph GUI
 */
class GUIManager(private val plugin: ClassePlugin, private val classeManager: ClasseManager) {
    
    private val legacySerializer = LegacyComponentSerializer.legacySection()
    
    private fun toComponent(text: String): Component {
        return legacySerializer.deserialize(text)
            .decoration(TextDecoration.ITALIC, false) // Disable italic explicitly
    }
    
    private fun toComponentList(texts: List<String>): List<Component> {
        return texts.map { toComponent(it) }
    }
    
    private fun createItemStack(material: Material, name: String, lore: List<String> = emptyList()): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        // Use proper Component with italic disabled
        meta.displayName(toComponent(name))
        if (lore.isNotEmpty()) {
            meta.lore(toComponentList(lore))
        }
        item.itemMeta = meta
        return item
    }
    
    /**
     * Open class selection GUI
     */
    
    /**
     * Open main class selection GUI
     */
    fun ouvrirGUISelection(player: Player) {
        // Check permission before opening GUI
        if (!player.hasPermission("nuva.classe.choisir")) {
            player.sendMessage("§cVous n'avez pas la permission de choisir une classe !")
            return
        }
        
        val title = "§7⌘ §8§lPeuples de Célesbourg §7⌘"
        
        val gui = Gui.gui()
            .title(toComponent(title))
            .rows(6)
            .disableAllInteractions()
            .create()
        
        // Decoration border - Dark glass panes (6 rows)
        creerBordureDecorative(gui, 6)
        
        // Add class items
        ajouterItemsClasses(gui, player)
        
        // Close item with fantasy design
        val itemFermer = createItemStack(Material.BARRIER, "§8§l⊗ §cFermer le Grimoire §8§l⊗", listOf(
            "§7",
            "§7Referme ce tome des peuples",
            "§7et retourne à ton aventure...",
            "§7",
            "§e▶ §6§lClic gauche pour fermer"
        ))
        gui.setItem(49, GuiItem(itemFermer) {
            player.closeInventory()
        })
        
        // Info item
        val classeActuelle = classeManager.getClasse(player) ?: Classe.AME_ERRANTE
        val itemInfo = createItemStack(Material.ENCHANTED_BOOK, "§d§l✦ Ton Peuple Actuel ✦", listOf(
            "§7",
            "§7Tu appartiens actuellement au peuple :",
            "${classeActuelle.getCouleur()}§l${classeActuelle.getSingularName()}",
            "§7",
            "§8${classeActuelle.emoji} ${classeActuelle.description}",
            "§7",
            "§e▶ §6§lClic droit pour plus d'infos"
        ))
        gui.setItem(45, GuiItem(itemInfo) { event ->
            if (event.click.isRightClick) {
                ouvrirGUIInfo(player)
            }
        })
        
        gui.open(player)
    }
    
    /**
     * Open class change confirmation GUI
     */
    fun ouvrirGUIConfirmation(player: Player, nouvelleClasse: Classe) {
        val classeActuelle = classeManager.getClasse(player) ?: Classe.AME_ERRANTE
        
        val title = "§4§l⚔ §cRituel de Changement §4§l⚔"
        val gui = Gui.gui()
            .title(toComponent(title))
            .rows(5)
            .disableAllInteractions()
            .create()
        
        // Decorative border for 5 rows
        creerBordureDecorative(gui, 5)
        
        // Current class item (slot 19)
        val itemActuel = createItemStack(
            getMaterialForClasse(classeActuelle), 
            "§7§l⚜ Peuple Actuel ⚜",
            listOf(
                "§7",
                "${classeActuelle.getCouleur()}§l${classeActuelle.getSingularName()}",
                "§8${classeActuelle.emoji} ${classeActuelle.description}",
                "§7",
                "§7Tu quittes ce peuple pour toujours.. ou pas !",
                "§7"
            )
        )
        gui.setItem(19, GuiItem(itemActuel))
        
        // Mystical arrow (slot 22)
        val itemFleche = createItemStack(Material.SPECTRAL_ARROW, "§6§l➤ §eTransformation §6§l➤", listOf(
            "§7",
            "§7Le rituel de changement va",
            "§7transformer ton essence..",
            "§7",
            "§6§nEs-tu prêt(e) ?",
            "§7"
        ))
        gui.setItem(22, GuiItem(itemFleche))
        
        // New class item (slot 25)
        val itemNouveau = createItemStack(
            getMaterialForClasse(nouvelleClasse), 
            "§a§l✦ Nouveau Destin ✦",
            listOf(
                "§7",
                "${nouvelleClasse.getCouleur()}§l${nouvelleClasse.getSingularName()}",
                "§8${nouvelleClasse.emoji} ${nouvelleClasse.description}",
                "§7",
                "§a§lPassifs de ce peuple :",
                *nouvelleClasse.passives.map { "§f● §7$it" }.toTypedArray(),
                "§7"
            )
        )
        gui.setItem(25, GuiItem(itemNouveau))
        
        // Confirm (slot 37 - Emerald)
        val itemConfirmer = createItemStack(Material.EMERALD, "§a§l✓ §2Accomplir le Rituel", listOf(
            "§7",
            "§7Confirme la transformation vers",
            "${nouvelleClasse.getCouleur()}§l${nouvelleClasse.getSingularName()}",
            "§7",
            "§c⚠ §4§lCette action est irréversible !",
            "§7",
            "§e▶ §6§lClic gauche pour confirmer"
        ))
        
        gui.setItem(37, GuiItem(itemConfirmer) {
            player.closeInventory()
            
            if (classeManager.changeClasse(player, nouvelleClasse)) {
                // Confirmation message is sent by ClasseManager
            } else {
                player.sendMessage("§c§l✗ §cLe rituel a échoué ! Une erreur mystique est survenue...")
            }
        })
        
        // Cancel (slot 39 - Redstone)
        val itemAnnuler = createItemStack(Material.REDSTONE, "§c§l✗ §4Annuler le Rituel", listOf(
            "§7",
            "§7Abandonne le rituel et conserve",
            "§7ton peuple actuel",
            "§7",
            "§e▶ §6§lClic gauche pour revenir"
        ))
        
        gui.setItem(39, GuiItem(itemAnnuler) {
            player.closeInventory()
            // Only allow returning to selection if player has permission
            if (player.hasPermission("nuva.classe.choisir")) {
                ouvrirGUISelection(player)
            }
        })
        
        gui.open(player)
    }
    
    /**
     * Open detailed information GUI
     */
    fun ouvrirGUIInfo(player: Player) {
        val classeActuelle = classeManager.getClasse(player) ?: Classe.AME_ERRANTE
        val playerData = classeManager.getPlayerData(player)
        
        val title = "§5§l✦ ${classeActuelle.getSingularName()} ✦"
        val gui = Gui.gui()
            .title(toComponent(title))
            .rows(6)
            .disableAllInteractions()
            .create()
        
        // Decorative border for 6 rows
        creerBordureDecorative(gui, 6)
        
        // Main class item with details
        val itemClasse = creerItemClasseDetaille(classeActuelle, playerData)
        gui.setItem(22, GuiItem(itemClasse))
        
        // Passive statistics if applicable
        if (classeActuelle != Classe.AME_ERRANTE && playerData != null) {
            ajouterStatistiquesPassives(gui, playerData)
        }
        
        // Lore/Story item
        val itemLore = createItemStack(Material.WRITABLE_BOOK, "§6§l✒ Légendes du Peuple ✒", listOf(
            "§7",
            "§f${classeActuelle.description}",
            "§7",
            "§7Chaque peuple possède ses propres",
            "§7traditions et mystères anciens...",
            "§7",
            "§8✦ Histoire millénaire ✦"
        ))
        gui.setItem(31, GuiItem(itemLore))
        
        // Back button with fantasy design - but only if player has permission to choose
        if (player.hasPermission("nuva.classe.choisir")) {
            val itemRetour = createItemStack(Material.SPECTRAL_ARROW, "§e§l← Retourner au Grimoire", listOf(
                "§7",
                "§7Revenir au tome des peuples",
                "§7pour explorer d'autres destinées",
                "§7",
                "§e▶ §6§lClic gauche pour revenir"
            ))
            gui.setItem(49, GuiItem(itemRetour) {
                ouvrirGUISelection(player)
            })
        } else {
            // Alternative button for players without permission - just close
            val itemFermer = createItemStack(Material.BARRIER, "§c§l✗ Fermer", listOf(
                "§7",
                "§7Fermer cette interface",
                "§7",
                "§e▶ §6§lClic gauche pour fermer"
            ))
            gui.setItem(49, GuiItem(itemFermer) {
                player.closeInventory()
            })
        }
        
        gui.open(player)
    }
    
    /**
     * Add class items to GUI
     */
    private fun ajouterItemsClasses(gui: Gui, player: Player) {
        val classeActuelle = classeManager.getClasse(player)
        
        // Fixed positions for 6x9 GUI in a nice pattern
        val positions = mapOf(
            Classe.BASTORGNES to 19,
            Classe.TARTINUITS to 20,
            Classe.SYLVOUNETS to 21,
            Classe.GROSUKI to 24,
            Classe.BRICOBRAK to 25,
            Classe.MIRAZIENS to 26
        )
        
        Classe.values().filter { it != Classe.AME_ERRANTE }.forEach { classe ->
            val position = positions[classe] ?: return@forEach
            val estClasseActuelle = classeActuelle == classe
            val item = creerItemClasse(classe, estClasseActuelle)
            
            gui.setItem(position, GuiItem(item) { event ->
                val clickedPlayer = event.whoClicked as Player
                
                if (estClasseActuelle) {
                    // Open info GUI for current class
                    ouvrirGUIInfo(clickedPlayer)
                } else {
                    // Open confirmation for class change
                    ouvrirGUIConfirmation(clickedPlayer, classe)
                }
            })
        }
    }
    
    /**
     * Create decorative border
     */
    private fun creerBordureDecorative(gui: Gui, rows: Int) {
        val borderItem = createItemStack(Material.BLACK_STAINED_GLASS_PANE, "§8", emptyList())
        val lastRowStart = (rows - 1) * 9
        
        // Top and bottom rows
        for (i in 0..8) {
            gui.setItem(i, GuiItem(borderItem))
            gui.setItem(lastRowStart + i, GuiItem(borderItem))
        }
        
        // Side columns (skip first and last rows)
        for (i in 1 until rows - 1) {
            gui.setItem(i * 9, GuiItem(borderItem))
            gui.setItem(i * 9 + 8, GuiItem(borderItem))
        }
        
        // Corner decorations
        val cornerItem = createItemStack(Material.PURPLE_STAINED_GLASS_PANE, "§5§l✦", emptyList())
        gui.setItem(0, GuiItem(cornerItem))
        gui.setItem(8, GuiItem(cornerItem))
        gui.setItem(lastRowStart, GuiItem(cornerItem))
        gui.setItem(lastRowStart + 8, GuiItem(cornerItem))
    }
    
    /**
     * Create class item
     */
    private fun creerItemClasse(classe: Classe, estClasseActuelle: Boolean): ItemStack {
        val material = getMaterialForClasse(classe)
        
        val nom = if (estClasseActuelle) {
            "§a§l✓ ${classe.getCouleur()}§l${classe.getSingularName()}"
        } else {
            "${classe.getCouleur()}§l${classe.getSingularName()}"
        }
        
        val lore = mutableListOf<String>()
        lore.add("§7")
        
        // Status indicator
        if (estClasseActuelle) {
            lore.add("§a§l⬥ TON PEUPLE ACTUEL ⬥")
            lore.add("§7")
            lore.add("§7Tu appartiens à ce noble peuple.")
            lore.add("§e▶ §6§lClic droit pour plus de détails")
        } else {
            lore.add("§7Un peuple aux traditions anciennes...")
            lore.add("§e▶ §6§lClic gauche pour rejoindre")
        }
        
        lore.add("§7")
        lore.add("§8${classe.emoji} ${classe.description}")
        lore.add("§7")
        
        // Passives with fantasy styling
        if (classe.passives.isNotEmpty()) {
            lore.add("§6§l⚔ Dons Ancestraux ⚔")
            classe.passives.forEach { passif ->
                lore.add("§f● §7$passif")
            }
            lore.add("§7")
        }
        
        // Add mystical footer
        lore.add("§8§l◇ ◆ ◇ ◆ ◇")
        
        return createItemStack(material, nom, lore)
    }
    
    /**
     * Create detailed class item for info GUI
     */
    private fun creerItemClasseDetaille(classe: Classe, playerData: PlayerData?): ItemStack {
        val material = getMaterialForClasse(classe)
        
        val nom = "${classe.getCouleur()}§l✦ ${classe.getSingularName()} ✦"
        val lore = mutableListOf<String>()
        
        lore.add("§7")
        lore.add("§8${classe.emoji} ${classe.name}")
        lore.add("§7")
        
        // Epic description
        lore.add("§6§l═══ Légende du Peuple ═══")
        lore.add("§f${classe.description}")
        lore.add("§7")
        
        // Passives with enhanced styling
        if (classe.passives.isNotEmpty()) {
            lore.add("§d§l⚔ Pouvoirs Hérités ⚔")
            classe.passives.forEachIndexed { index, passif ->
                val symbol = if (index == 0) "◆" else "◇"
                lore.add("§5$symbol §7$passif")
            }
            lore.add("§7")
        }
        
        // Player data info with mystical styling
        if (playerData != null) {
            val date = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(playerData.selectionDate))
            lore.add("§3§l⌘ Rituel d'Appartenance ⌘")
            lore.add("§7Accompli le: §b$date")
            lore.add("§7")
        }
        
        // Mystical footer
        lore.add("§8§l◇ ◆ ◇ ◆ ◇ ◆ ◇")
        
        return createItemStack(material, nom, lore)
    }
    
    /**
     * Add passive statistics to info GUI - Simplified and clearer layout
     */
    private fun ajouterStatistiquesPassives(gui: Gui, playerData: PlayerData) {
        // Single comprehensive passive status item - left side
        val itemStats = createItemStack(Material.CLOCK, "§6§l⏰ États des Pouvoirs ⏰", creerLoreCooldowns(playerData))
        gui.setItem(20, GuiItem(itemStats))
        
        // Passive abilities summary - right side
        val itemPassives = creerItemResumePouvoirss(playerData)
        gui.setItem(24, GuiItem(itemPassives))
    }
    
    /**
     * Create passive abilities summary item
     */
    private fun creerItemResumePouvoirss(playerData: PlayerData): ItemStack {
        val lore = mutableListOf<String>()
        
        lore.add("§7")
        
        if (playerData.activePassives.isEmpty()) {
            lore.add("§8Aucun pouvoir ancestral actif")
            lore.add("§7")
            return createItemStack(Material.PAPER, "§7§lPouvoirs Inactifs", lore)
        }
        
        lore.add("§d§l⚔ Arsenal Mystique ⚔")
        lore.add("§7")
        
        // Group passives by type for clarity
        val permanentPassives = mutableListOf<PassiveType>()
        val cooldownPassives = mutableListOf<PassiveType>()
        
        playerData.activePassives.forEach { (passiveType, _) ->
            if (isPassivePermanent(passiveType)) {
                permanentPassives.add(passiveType)
            } else {
                cooldownPassives.add(passiveType)
            }
        }
        
        // Display permanent passives
        if (permanentPassives.isNotEmpty()) {
            lore.add("§a§l◆ Effets Permanents:")
            permanentPassives.forEach { passiveType ->
                val description = getPassiveShortDescription(passiveType)
                lore.add("§2● §7${passiveType.getDisplayName()}")
                lore.add("  §8$description")
            }
            lore.add("§7")
        }
        
        // Display cooldown passives
        if (cooldownPassives.isNotEmpty()) {
            lore.add("§6§l◆ Capacités Activables:")
            cooldownPassives.forEach { passiveType ->
                val description = getPassiveShortDescription(passiveType)
                lore.add("§e● §7${passiveType.getDisplayName()}")
                lore.add("  §8$description")
            }
            lore.add("§7")
        }
        
        lore.add("§8§nHéritage de ton peuple")
        
        return createItemStack(Material.ENCHANTED_BOOK, "§5§l⚡ Pouvoirs Hérités ⚡", lore)
    }
    
    /**
     * Check if a passive is permanent (no cooldown)
     */
    private fun isPassivePermanent(passiveType: PassiveType): Boolean {
        return when (passiveType) {
            PassiveType.HEMATOMES_PERSISTANTS,
            PassiveType.RESISTANCE_ENDURCIE,
            PassiveType.SANTE_FLORISSANTE,
            PassiveType.FORCE_SYLVESTRE,
            PassiveType.AGILITE_FORESTIERE,
            PassiveType.OUTILS_BRICOLEURS,
            PassiveType.RESSORT_BRICOLE,
            PassiveType.PLUME_LEGERE,
            PassiveType.PRECISION_AERIENNE,
            PassiveType.VISION_DIMENSIONNELLE,
            PassiveType.BERSERKER_SANGUINAIRE -> true
            else -> false
        }
    }
    
    /**
     * Get short description for passive type
     */
    private fun getPassiveShortDescription(passiveType: PassiveType): String {
        return when (passiveType) {
            PassiveType.HEMATOMES_PERSISTANTS -> "Inflige 2% de dégâts en plus avec les massues"
            PassiveType.RESISTANCE_ENDURCIE -> "Réduit tous les dégâts reçus de 5%"
            PassiveType.SANTE_FLORISSANTE -> "Régénère automatiquement la vie quand la faim est pleine"
            PassiveType.FORCE_SYLVESTRE -> "Inflige 2% de dégâts en plus avec arc et arbalète"
            PassiveType.AGILITE_FORESTIERE -> "Se déplace 20% plus vite dans les biomes de forêt"
            PassiveType.OUTILS_BRICOLEURS -> "Les outils perdent 20% moins de durabilité"
            PassiveType.RESSORT_BRICOLE -> "Saute plus haut en permanence (Jump Boost I)"
            PassiveType.PLUME_LEGERE -> "Réduit les dégâts de chute de 50%"
            PassiveType.PRECISION_AERIENNE -> "10% de chance d'infliger Faiblesse I (3s) avec les flèches"
            PassiveType.PAIN_NOURRICIER -> "Manger du pain donne 2 cœurs d'absorption (10s, CD: 20s)"
            PassiveType.SERENITE_LUNAIRE -> "Obtient Résistance I (5s) quand la vie < 25% (CD: 60s)"
            PassiveType.ESPRIT_APAISE -> "S'accroupir 3s déclenche Régénération II (5s, CD: 45s)"
            PassiveType.VISION_DIMENSIONNELLE -> "⚠ Passif non implémenté"
            PassiveType.BERSERKER_SANGUINAIRE -> "⚠ Passif non implémenté"
        }
    }

    /**
     * Create cooldown lore for passives
     */
    private fun creerLoreCooldowns(playerData: PlayerData): List<String> {
        val lore = mutableListOf<String>()
        
        lore.add("§7")
        
        // Count cooldown passives
        val cooldownPassives = playerData.activePassives.filter { (passiveType, _) -> 
            !isPassivePermanent(passiveType) 
        }
        
        if (cooldownPassives.isEmpty()) {
            lore.add("§8Aucun pouvoir en récupération")
            lore.add("§7")
            lore.add("§7Tous tes pouvoirs ancestraux")
            lore.add("§7sont des effets permanents.")
            lore.add("§7")
            return lore
        }
        
        lore.add("§d§l⏰ Récupération Active ⏰")
        lore.add("§7")
        
        var hasActiveCooldowns = false
        
        cooldownPassives.forEach { (passiveType, activePassive) ->
            val remaining = activePassive.getRemainingCooldown()
            if (remaining > 0) {
                hasActiveCooldowns = true
                lore.add("§c● §7${passiveType.getDisplayName()}")
                lore.add("  §8Prêt dans §c${remaining}s")
            }
        }
        
        if (!hasActiveCooldowns) {
            lore.add("§a§l✓ Tous les pouvoirs disponibles !")
            lore.add("§7")
            lore.add("§7Tes capacités spéciales sont")
            lore.add("§7prêtes à être déclenchées.")
        }
        
        lore.add("§7")
        lore.add("§8Les pouvoirs se rechargent automatiquement")
        
        return lore
    }
    
    /**
     * Create passive item for detailed view - Simplified without duplicates
     */
    private fun creerItemPassive(passiveType: PassiveType): ItemStack {
        val (material, name, description) = when (passiveType) {
            // Bastorgnes
            PassiveType.HEMATOMES_PERSISTANTS -> 
                Triple(Material.MACE, "§c§lFrappe Lourde", "Bonus dégâts avec massues")
            PassiveType.RESISTANCE_ENDURCIE -> 
                Triple(Material.IRON_CHESTPLATE, "§8§lPeau de Fer", "Réduction des dégâts subis")
            
            // Tartinuits
            PassiveType.PAIN_NOURRICIER -> 
                Triple(Material.BREAD, "§6§lBénédiction Nourricière", "Absorption au pain")
            PassiveType.SANTE_FLORISSANTE -> 
                Triple(Material.GOLDEN_APPLE, "§2§lSanté Florissante", "Régénération si faim pleine")
                
            // Sylvounets
            PassiveType.FORCE_SYLVESTRE -> 
                Triple(Material.BOW, "§a§lTir Sylvestre", "Bonus dégâts arc et arbalète")
            PassiveType.AGILITE_FORESTIERE -> 
                Triple(Material.LEATHER_BOOTS, "§2§lAgilité Forestière", "Vitesse accrue en forêt")
                
            // Grosuki
            PassiveType.SERENITE_LUNAIRE -> 
                Triple(Material.GHAST_TEAR, "§5§lSérénité Lunaire", "Résistance si vie faible")
            PassiveType.ESPRIT_APAISE -> 
                Triple(Material.GLISTERING_MELON_SLICE, "§d§lEsprit Apaisé", "Régénération en s'accroupissant")
                
            // Bricobrak
            PassiveType.OUTILS_BRICOLEURS -> 
                Triple(Material.IRON_PICKAXE, "§3§lOutils Bricolés", "Durabilité réduite")
            PassiveType.RESSORT_BRICOLE -> 
                Triple(Material.SLIME_BLOCK, "§a§lRessort Bricolé", "Saut permanent")
                
            // Miraziens  
            PassiveType.PLUME_LEGERE -> 
                Triple(Material.FEATHER, "§f§lPlume Légère", "Chutes amorties")
            PassiveType.PRECISION_AERIENNE -> 
                Triple(Material.BOW, "§e§lPrécision Aérienne", "Flèches affaiblissantes")
                
            // Additional passives
            PassiveType.VISION_DIMENSIONNELLE -> 
                Triple(Material.ENDER_EYE, "§d§lVision Dimensionnelle", "Perceptions étendues")
            PassiveType.BERSERKER_SANGUINAIRE -> 
                Triple(Material.REDSTONE, "§4§lBerserker Sanguinaire", "Rage au combat")
        }
        
        val statusText = if (isPassivePermanent(passiveType)) {
            "§a⚡ Effet permanent"
        } else {
            "§6⏰ Capacité activable"
        }
        
        return createItemStack(material, name, listOf(
            "§7",
            "§8✦ $description ✦",
            "§7",
            statusText,
            "§7",
            "§5◆ Héritage ancestral"
        ))
    }
    
    /**
     * Get material for class representation with enhanced fantasy materials
     */
    private fun getMaterialForClasse(classe: Classe): Material {
        return when (classe) {
            Classe.BASTORGNES -> Material.NETHERITE_SWORD
            Classe.TARTINUITS -> Material.GOLDEN_APPLE
            Classe.SYLVOUNETS -> Material.BOW
            Classe.GROSUKI -> Material.TOTEM_OF_UNDYING
            Classe.BRICOBRAK -> Material.NETHERITE_PICKAXE
            Classe.MIRAZIENS -> Material.ELYTRA
            Classe.AME_ERRANTE -> Material.SOUL_TORCH
        }
    }
}
