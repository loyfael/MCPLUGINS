package loyfael.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import loyfael.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SafeMode command executor - Simplified for direct mode switching
 * /safemode = Toggle mode directly (main usage)
 * /safemode status = Show current mode
 * /safemode reload = Reload config (admin only)
 */
public class SafeModeCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public SafeModeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
            return true;
        }

        // /safemode without arguments = Direct toggle (main usage)
        if (args.length == 0) {
            if (!player.hasPermission("aethersafemode.toggle")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            // Launch the toggle process (shows GUI)
            plugin.getSafeModeManager().initiateToggle(player);
            return true;
        }

        // Handle sub-commands
        switch (args[0].toLowerCase()) {
            case "status":
            case "info":
                showStatus(player);
                break;

            case "reload":
                if (!player.hasPermission("aethersafemode.reload")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                reloadConfig(player);
                break;

            default:
                showHelp(player);
                break;
        }

        return true;
    }

    /**
     * Affiche le statut actuel du joueur avec design amélioré
     */
    private void showStatus(Player player) {
        boolean isSafe = plugin.getSafeModeManager().isSafeMode(player);

        // En-tête élégant
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("               §7§lSTATUT SAFEMODE                    ");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");

        if (isSafe) {
            player.sendMessage("§8│  §7Mode actuel : §a§l✓ MODE SÉCURISÉ");
            player.sendMessage("§8│");
            player.sendMessage("§8│  §a✓ §7PvP §aDÉSACTIVÉ§7)");
            player.sendMessage("§8│  §a✓ §7Vos objets seront préservés en mourant");
            player.sendMessage("§8│  §a✓ §7Vous ne pouvez plus être attaqué");
            player.sendMessage("§8│  §a✓ §7Vous ne pouvez plus attaquer d'autres joueurs");
        } else {
            player.sendMessage("§8│  §7Mode actuel : §c§l⚠ MODE DANGER");
            player.sendMessage("§8│");
            player.sendMessage("§8│  §c- §7PvP §c§lACTIVÉ§7");
            player.sendMessage("§8│  §c- §7Perte des objets §c§lACTIVÉE§r");
            player.sendMessage("§8│");
            player.sendMessage("§8│  §c⚠ §c§lIMPORTANT - À LIRE §c⚠");
            player.sendMessage("§8│  §7En cas de mort, §4§lAUCUN REMBOURSEMENT");
            player.sendMessage("§8│  §7ne sera effectué. Tu es deviens responsable");
            player.sendMessage("§8│  §cde §ctoutes tes actions !");
        }

        player.sendMessage("");
        player.sendMessage("§8│  §aVous pouvez changer de mode à tout moment avec");
        player.sendMessage("§8│  §a§2/safemode§a une fois votre temps de recharge");
        player.sendMessage("§8│  §aterminé.");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    /**
     * Recharge la configuration
     */
    private void reloadConfig(Player player) {
        plugin.getConfigManager().loadConfig();
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§a✓ §7Configuration rechargée avec succès !");
    }

    /**
     * Affiche l'aide avec design amélioré
     */
    private void showHelp(Player player) {
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§8│                   §7§lSAFE MODE                       §8│");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
        player.sendMessage("§8│  §7Commandes disponibles :");
        player.sendMessage("§8│  §a▪ §f/safemode §8- §7Changer de mode de jeu");
        player.sendMessage("§8│  §a▪ §f/safemode status §8- §7Voir votre mode actuel");

        if (player.hasPermission("aethersafemode.reload")) {
            player.sendMessage("§8│  §a▪ §f/safemode reload §8- §7Recharger la config");
        }

        player.sendMessage("");
        player.sendMessage("§8│  §7Modes de jeu :");
        player.sendMessage("§8│  §a▪ §aMODE SÉCURISÉ §8- §7Protection totale");
        player.sendMessage("§8│  §c▪ §cMODE DANGER §8- §7PvP activé, risques élevés");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("status", "info");

            if (sender.hasPermission("aethersafemode.reload")) {
                subcommands = new ArrayList<>(subcommands);
                subcommands.add("reload");
            }

            String input = args[0].toLowerCase();
            for (String subcommand : subcommands) {
                if (subcommand.startsWith(input)) {
                    completions.add(subcommand);
                }
            }
        }

        return completions;
    }
}
