package loyfael.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Utilitaire pour l'envoi de messages ActionBar et de titres
 * Compatible avec l'API Adventure de Minecraft 1.21+
 */
public class ActionBar {

    /**
     * Envoie un message dans la barre d'action du joueur
     */
    public static void sendActionBar(Player player, String message) {
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(Utils.color(message));
        player.sendActionBar(component);
    }

    /**
     * Envoie un titre et sous-titre au joueur avec des durées personnalisées
     */
    public static void sendTitle(Player player, int fadeIn, int stay, int fadeOut, String title, String subtitle) {
        Component titleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(Utils.color(title));
        Component subtitleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(Utils.color(subtitle));

        Title titleObj = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50L), // Conversion en millisecondes
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
            )
        );

        player.showTitle(titleObj);
    }

    /**
     * Envoie un titre simple au joueur avec des durées par défaut
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, 10, 70, 20, title, subtitle);
    }

    /**
     * Efface le titre du joueur
     */
    @SuppressWarnings("unused")
    public static void clearTitle(Player player) {
        player.clearTitle();
    }
}
