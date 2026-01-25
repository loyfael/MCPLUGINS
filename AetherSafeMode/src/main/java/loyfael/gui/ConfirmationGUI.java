package loyfael.gui;

import loyfael.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface de sélection de mode avec système de cooldown
 */
public class ConfirmationGUI implements Listener {

    private final Main plugin;
    private final Player player;
    private Inventory inventory;

    // Système de cooldown global (configurable)
    private static final ConcurrentHashMap<UUID, Long> lastModeChange = new ConcurrentHashMap<>();

    public ConfirmationGUI(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        createInventory();
    }

    /**
     * Crée l'interface graphique avec les 2 modes
     */
    private void createInventory() {
        String title = plugin.getConfigManager().getGuiTitle();
        inventory = Bukkit.createInventory(null, 27, title); // 3 lignes = 27 slots

        // Remplir avec du verre gris
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        boolean currentMode = plugin.getSafeModeManager().isSafeMode(player);

        // Mode Sécurisé (slot 11 - ligne 2, colonne 3)
        inventory.setItem(11, createModeItem(true, currentMode));

        // Mode Combat (slot 15 - ligne 2, colonne 7)
        inventory.setItem(15, createModeItem(false, currentMode));
    }

    /**
     * Crée un item de mode avec cooldown
     */
    private ItemStack createModeItem(boolean safeMode, boolean currentMode) {
        Material material = safeMode ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        String name = safeMode ? "&a&lMODE SÉCURISÉ" : "&c&lMODE DANGER";

        List<String> lore = new ArrayList<>();

        if (currentMode == safeMode) {
            // Mode actuel
            lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("");
            lore.add("&f&lTON MODE ACTUEL");
            lore.add("");

            if (safeMode) {
                lore.add("&a✅ &7Tu es actuellement protégé");
                lore.add("&a✅ &7Tes objets sont sauvegardés à la mort");
                lore.add("&a✅ &7Le PvP est désactivé");
            } else {
                lore.add("&4⚠ &c&lATTENTION, &4&lDANGER &c&l!");
                lore.add("&4⚠ &cUtilise ce mode avec prudence !");
                lore.add("&cEn cas de mort, &4&lAUCUN REMBOURSEMENT");
                lore.add("&cne sera effectué. Tu es devient responsable");
                lore.add("&cde tes actions !");
                lore.add("");
                lore.add("- &7Le PvP est activé");
                lore.add("- &7Tu perds tes objets si tu meurs");
                lore.add("- &7Tu peux attaquer d'autres joueurs \nen mode danger");
            }

            // Afficher quand le joueur pourra changer
            long lastChange = lastModeChange.getOrDefault(player.getUniqueId(), 0L);
            long timeSinceChange = System.currentTimeMillis() - lastChange;
            long timeRemaining = plugin.getConfigManager().getCooldownTime() - timeSinceChange;

            lore.add("");
            lore.add("&f&lCHANGEMENT DE MODE :");
            if (timeRemaining > 0) {
                lore.add("&7Tu pourras changer dans : &f" + formatTime(timeRemaining));
                lore.add("");
                lore.add("&8▪ &7Délai de sécurité : 24 heures");
                lore.add("&8▪ &7Évite les changements abusifs");
            } else {
                lore.add("&a✅ &7Changement possible maintenant");
                lore.add("");
                lore.add("&8▪ &7Tu peux choisir l'autre mode");
            }

        } else {
            // L'autre mode (disponible ou en cooldown)
            lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("");

            if (safeMode) {
                lore.add("&a✅ &7PvP &aDÉSACTIVÉ");
                lore.add("&a✅ &7Aucune perte d'objets");
                lore.add("");
                lore.add("&f&lLIMITATIONS :");
                lore.add("&8▪ &7Impossible d'attaquer d'autres joueurs");
                lore.add("&8▪ &7Pas de combat PvP possible");
            } else {
                lore.add("&4⚠ &7PvP &cACTIVÉ");
                lore.add("&4⚠ &7Pertes des objets &cACTIVÉES");
                lore.add("");
                lore.add("&4&lIMPORTANT &7- &c&lTA RESPONSABILITÉ :");
                lore.add("&7En activant ce mode, tu acceptes toutes");
                lore.add("&7En cas de mort, §4§lAUCUN REMBOURSEMENT");
                lore.add("&7ne sera effectué. Tu es deviens responsable");
                lore.add("&cde §ctoutes tes actions !");
                lore.add("&7Utilise ce mode avec prudence !");
            }

            // Vérifier si le joueur peut changer
            long lastChange = lastModeChange.getOrDefault(player.getUniqueId(), 0L);
            long timeSinceChange = System.currentTimeMillis() - lastChange;
            long timeRemaining = plugin.getConfigManager().getCooldownTime() - timeSinceChange;

            lore.add("");
            lore.add("&f&lDISPONIBILITÉ :");
            if (timeRemaining > 0) {
                lore.add("&c❌ &7Temporairement indisponible");
                lore.add("&7Attendre encore : &f" + formatTime(timeRemaining));
                lore.add("");
                lore.add("&8▪ &7Reviens plus tard !");
            } else {
                lore.add("&a✅ &7Tu peux changer de mode.");
                lore.add("");
                lore.add("&a&l➤ &7Clique pour activer ce mode");
            }
        }

        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return createItem(material, name, lore);
    }

    /**
     * Formate le temps restant en heures, minutes, secondes
     */
    private String formatTime(long timeInMillis) {
        long seconds = timeInMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    /**
     * Crée un ItemStack avec nom et lore
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
            }
            if (lore != null) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Ouvre l'interface pour le joueur
     */
    public void open() {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !event.getWhoClicked().equals(player)) {
            return;
        }

        if (!event.getInventory().equals(inventory)) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        boolean currentMode = plugin.getSafeModeManager().isSafeMode(player);

        if (slot == 11) { // Mode Sécurisé
            if (currentMode) {
                player.sendMessage("§7Vous êtes déjà en Mode Sécurisé !");
                return;
            }
            attemptModeChange(true);
        } else if (slot == 15) { // Mode Combat
            if (!currentMode) {
                player.sendMessage("§7Vous êtes déjà en Mode Combat !");
                return;
            }
            attemptModeChange(false);
        }
    }

    /**
     * Tente de changer de mode en vérifiant le cooldown
     */
    private void attemptModeChange(boolean targetMode) {
        UUID uuid = player.getUniqueId();
        long lastChange = lastModeChange.getOrDefault(uuid, 0L);
        long timeSinceChange = System.currentTimeMillis() - lastChange;
        long timeRemaining = plugin.getConfigManager().getCooldownTime() - timeSinceChange;

        if (timeRemaining > 0) {
            player.closeInventory();
            player.sendMessage("§c⏰ Vous devez attendre encore " + formatTime(timeRemaining) + " avant de changer de mode !");
            return;
        }

        // Effectuer le changement
        player.closeInventory();
        plugin.getSafeModeManager().switchMode(player, targetMode);

        // Enregistrer le timestamp du changement
        lastModeChange.put(uuid, System.currentTimeMillis());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player) || !event.getPlayer().equals(player)) {
            return;
        }

        if (!event.getInventory().equals(inventory)) {
            return;
        }

        // Ne pas afficher de message d'annulation, juste fermer silencieusement
    }
}
