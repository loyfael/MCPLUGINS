package loyfael;

import loyfael.interfaces.IShopService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ShopCommand implements CommandExecutor {

    private final IShopService shopService;
    private final Logger logger;

    public ShopCommand(IShopService shopService) {
        this.shopService = shopService;
        this.logger = Main.getInstance().getLogger();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            // Validation de base
            if (sender == null || command == null) {
                logger.severe("onCommand appelé avec des paramètres null!");
                return true;
            }

            // Gestion des arguments : /dailyshop [joueur]
            if (args.length == 0) {
                // /dailyshop - ouvrir pour soi-même (seulement si c'est un joueur)
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c✗ La console doit spécifier un joueur: /dailyshop <joueur>");
                    return true;
                }
                return handleOpenShop(sender);
                
            } else if (args.length == 1) {
                // /dailyshop <joueur> - ouvrir pour quelqu'un d'autre
                String targetName = args[0];
                return handleOpenShopForOther(sender, targetName);
                
            } else {
                // Trop d'arguments
                sender.sendMessage("§c✗ Usage: /dailyshop [joueur]");
                return true;
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique dans ShopCommand pour " + 
                (sender != null ? sender.getName() : "null"), e);
            if (sender != null) {
                sender.sendMessage("§c✗ Erreur interne! Contactez un administrateur.");
            }
            return true;
        }
    }
    
    /**
     * Ouvre le shop pour le sender lui-même (sender = joueur)
     */
    private boolean handleOpenShop(CommandSender sender) {
        // À ce stade, on sait que sender est un Player
        Player player = (Player) sender;
        
        // Vérifier la permission
        if (!player.hasPermission("aetherialshop.use")) {
            player.sendMessage("§c✗ Vous n'avez pas la permission d'utiliser le shop!");
            return true;
        }

        return openShopForPlayer(sender, player);
    }
    
    /**
     * Ouvre le shop pour un autre joueur
     */
    private boolean handleOpenShopForOther(CommandSender sender, String targetName) {
        // Vérifier la permission pour ouvrir le shop à d'autres
        // La console et les plugins ont automatiquement cette permission
        if (sender instanceof Player && !sender.hasPermission("aetherialshop.open.others")) {
            sender.sendMessage("§c✗ Vous n'avez pas la permission d'ouvrir le shop pour d'autres joueurs!");
            return true;
        }
        
        // Trouver le joueur cible
        Player targetPlayer = Main.getInstance().getServer().getPlayer(targetName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("§c✗ Joueur '" + targetName + "' introuvable ou hors ligne!");
            return true;
        }
        
        // Vérifier que le joueur cible a la permission d'utiliser le shop
        if (!targetPlayer.hasPermission("aetherialshop.use")) {
            sender.sendMessage("§c✗ Le joueur " + targetName + " n'a pas la permission d'utiliser le shop!");
            return true;
        }
        
        boolean success = openShopForPlayer(sender, targetPlayer);
        if (success && !sender.equals(targetPlayer)) {
            sender.sendMessage("§a✓ Shop ouvert pour " + targetPlayer.getName() + " !");
        }
        return success;
    }
    
    /**
     * Logique commune pour ouvrir le shop
     */
    private boolean openShopForPlayer(CommandSender sender, Player player) {
        try {
            // Vérification que le joueur est en ligne
            if (!player.isOnline()) {
                sender.sendMessage("§c✗ Le joueur " + player.getName() + " n'est plus en ligne!");
                logger.warning("Tentative d'ouverture du shop pour un joueur déconnecté: " + player.getName());
                return true;
            }

            // Vérification du service shop
            if (shopService == null) {
                sender.sendMessage("§c✗ Service du shop non disponible! Contactez un administrateur.");
                logger.severe("ShopService est null lors de l'exécution de la commande");
                return true;
            }

            // Vérification des items disponibles
            if (shopService.getCurrentItems() == null) {
                sender.sendMessage("§c✗ Aucun item disponible! Le shop sera bientôt mis à jour.");
                return true;
            }

            if (shopService.getCurrentItems().isEmpty()) {
                sender.sendMessage("§c✗ Le shop n'est pas encore disponible! Réessayez dans quelques instants.");
                return true;
            }

            // Tentative d'ouverture du shop
            ShopGUI gui = new ShopGUI(shopService, Main.getInstance().getEconomyService());
            gui.openShop(player);
            return true;

        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors de l'ouverture du shop! Réessayez dans un instant.");
            logger.log(Level.SEVERE, "Erreur lors de l'ouverture du shop pour " + player.getName(), e);
            return true;
        }
    }
}
