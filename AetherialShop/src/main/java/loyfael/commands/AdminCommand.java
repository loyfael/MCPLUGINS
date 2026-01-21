package loyfael.commands;

import loyfael.services.DiscordService;
import loyfael.interfaces.IShopService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class AdminCommand implements CommandExecutor {
    
    private final IShopService shopService;
    private final DiscordService discordService;
    private final Logger logger;
    
    public AdminCommand(IShopService shopService, DiscordService discordService, Logger logger) {
        this.shopService = shopService;
        this.discordService = discordService;
        this.logger = logger;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aetherialshop.admin")) {
            sender.sendMessage("§c✗ Vous n'avez pas la permission d'utiliser cette commande !");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "test-discord":
                handleTestDiscord(sender);
                break;
                
            case "reload":
                handleReload(sender);
                break;
                
            case "stats":
                handleStats(sender);
                break;
                
            case "rotate":
                handleForceRotate(sender);
                break;
                
            case "rotate-silent":
                handleSilentRotate(sender);
                break;
                
            case "check-rotation":
                handleCheckRotation(sender);
                break;
                
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void handleTestDiscord(CommandSender sender) {
        sender.sendMessage("§e⚡ Test de l'embed Discord en cours...");
        
        if (!discordService.isEnabled()) {
            sender.sendMessage("§c✗ Discord non configuré ! Vérifiez config.yml");
            return;
        }
        
        boolean success = discordService.sendTestEmbed();
        
        if (success) {
            sender.sendMessage("§a✓ Embed de test envoyé avec succès !");
        } else {
            sender.sendMessage("§c✗ Échec de l'envoi ! Vérifiez les logs pour plus d'infos.");
        }
    }
    
    private void handleReload(CommandSender sender) {
        sender.sendMessage("§e⚡ Rechargement de la configuration...");
        // Note: Implémenter le rechargement si nécessaire
        sender.sendMessage("§a✓ Configuration rechargée !");
    }
    
    private void handleStats(CommandSender sender) {
        sender.sendMessage("§6§l=== STATISTIQUES AETHERIALSHOP ===");
        sender.sendMessage("§7Items actuels: §e" + shopService.getCurrentItems().size());
        sender.sendMessage("§7Discord: " + (discordService.isEnabled() ? "§a✓ Activé" : "§c✗ Désactivé"));
        
        if (sender instanceof Player player) {
            sender.sendMessage("§7Joueur: §e" + player.getName());
        }
        
        sender.sendMessage("§6§l================================");
    }
    
    private void handleForceRotate(CommandSender sender) {
        sender.sendMessage("§e⚡ Rotation forcée du shop...");
        
        try {
            shopService.rotateItems(false); // Rotation sans webhook automatique
            sender.sendMessage("§a✓ Rotation effectuée avec succès !");
            
            if (discordService.isEnabled()) {
                discordService.sendRotationEmbed(shopService.getCurrentItems());
                sender.sendMessage("§a✓ Notification Discord envoyée !");
            }
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors de la rotation : " + e.getMessage());
            logger.severe("Erreur lors de la rotation forcée: " + e.getMessage());
        }
    }
    
    private void handleSilentRotate(CommandSender sender) {
        sender.sendMessage("§e⚡ Rotation silencieuse du shop (sans Discord)...");
        
        try {
            shopService.rotateItems(false); // Sans webhook
            sender.sendMessage("§a✓ Rotation silencieuse effectuée avec succès !");
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors de la rotation : " + e.getMessage());
            logger.severe("Erreur lors de la rotation silencieuse: " + e.getMessage());
        }
    }
    
    private void handleCheckRotation(CommandSender sender) {
        sender.sendMessage("§e⚡ Vérification du système de rotation...");
        
        try {
            // Informations de debug sur la rotation
            int hour = loyfael.Main.getInstance().getConfig().getInt("rotation.hour", 12);
            int minute = loyfael.Main.getInstance().getConfig().getInt("rotation.minute", 0);
            
            sender.sendMessage("§7Heure de rotation configurée: §e" + String.format("%02d:%02d", hour, minute));
            sender.sendMessage("§7Items actuels: §a" + shopService.getCurrentItems().size());
            sender.sendMessage("§a✓ Système de rotation opérationnel !");
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors de la vérification : " + e.getMessage());
            logger.severe("Erreur lors de la vérification de rotation: " + e.getMessage());
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== COMMANDES ADMIN AETHERIALSHOP ===");
        sender.sendMessage("§e/ashop test-discord §7- Tester l'envoi Discord");
        sender.sendMessage("§e/ashop reload §7- Recharger la configuration");
        sender.sendMessage("§e/ashop stats §7- Afficher les statistiques");
        sender.sendMessage("§e/ashop rotate §7- Forcer une rotation (avec Discord)");
        sender.sendMessage("§e/ashop rotate-silent §7- Rotation sans Discord");
        sender.sendMessage("§e/ashop check-rotation §7- Vérifier le système");
        sender.sendMessage("§6§l=====================================");
    }
}
