package loyfael.api.interfaces;

import org.bukkit.entity.Player;

/**
 * Interface pour les services de notification et messaging
 * Principe de responsabilité unique - centralisation des messages
 */
public interface INotificationService {

    /**
     * Envoie un message à un joueur
     */
    void sendMessage(Player player, String messageKey, Object... placeholders);

    /**
     * Envoie un message d'action bar à un joueur
     */
    void sendActionBar(Player player, String messageKey, Object... placeholders);

    /**
     * Envoie un titre à un joueur
     */
    void sendTitle(Player player, String titleKey, String subtitleKey, Object... placeholders);

    /**
     * Diffuse un message à tous les joueurs
     */
    void broadcast(String messageKey, Object... placeholders);

    /**
     * Envoie un message de log dans la console
     */
    void logConsole(String messageKey, Object... placeholders);

    /**
     * Récupère un message formaté sans l'envoyer
     */
    String getMessage(String messageKey, Object... placeholders);

    /**
     * Recharge les messages depuis les fichiers
     */
    void reloadMessages();
}
