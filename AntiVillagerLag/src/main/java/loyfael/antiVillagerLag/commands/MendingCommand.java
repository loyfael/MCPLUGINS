package loyfael.antiVillagerLag.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import loyfael.antiVillagerLag.AntiVillagerLag;
import loyfael.antiVillagerLag.events.EventListener;
import loyfael.antiVillagerLag.utils.VillagerUtilities;

public class MendingCommand implements CommandExecutor {
    
    private final AntiVillagerLag plugin;
    private final EventListener eventListener;
    
    public MendingCommand(AntiVillagerLag plugin, EventListener eventListener) {
        this.plugin = plugin;
        this.eventListener = eventListener;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antivillagerlag.mending")) {
            sender.sendMessage(VillagerUtilities.colorcodes.cm("&c🚫 Vous n'avez pas la permission d'utiliser cette commande."));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(VillagerUtilities.colorcodes.cm("&6📋 Commandes disponibles:"));
            sender.sendMessage(VillagerUtilities.colorcodes.cm("&e/mending clear &7- Supprime tout le Mending du serveur"));
            sender.sendMessage(VillagerUtilities.colorcodes.cm("&e/mending status &7- Affiche le statut de la prévention"));
            sender.sendMessage(VillagerUtilities.colorcodes.cm("&e/mending toggle &7- Active/désactive la prévention du Mending"));
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "clear":
                sender.sendMessage(VillagerUtilities.colorcodes.cm("&6🔄 Suppression de tous les trades avec Mending..."));
                eventListener.removeAllMendingFromServer();
                sender.sendMessage(VillagerUtilities.colorcodes.cm("&a✅ Processus de suppression lancé ! Tous les trades avec Mending seront supprimés."));
                break;
                
            case "status":
                boolean enabled = plugin.getConfig().getBoolean("toggleableoptions.prevent-mending-trades", true);
                sender.sendMessage(VillagerUtilities.colorcodes.cm("&6📊 Statut de la prévention du Mending:"));
                sender.sendMessage(VillagerUtilities.colorcodes.cm("&7- Prévention active: " + (enabled ? "&a✅ OUI" : "&c❌ NON")));
                sender.sendMessage(VillagerUtilities.colorcodes.cm("&7- Debug activé: " + (plugin.getConfig().getBoolean("debug", false) ? "&a✅ OUI" : "&c❌ NON")));
                break;
                
            case "toggle":
                boolean currentState = plugin.getConfig().getBoolean("toggleableoptions.prevent-mending-trades", true);
                plugin.getConfig().set("toggleableoptions.prevent-mending-trades", !currentState);
                plugin.saveConfig();
                
                if (!currentState) {
                    sender.sendMessage(VillagerUtilities.colorcodes.cm("&a✅ Prévention du Mending ACTIVÉE"));
                } else {
                    sender.sendMessage(VillagerUtilities.colorcodes.cm("&c❌ Prévention du Mending DÉSACTIVÉE"));
                }
                break;
                
            default:
                sender.sendMessage(VillagerUtilities.colorcodes.cm("&c❌ Commande inconnue. Utilisez &e/mending &cpour voir l'aide."));
                break;
        }
        
        return true;
    }
}
