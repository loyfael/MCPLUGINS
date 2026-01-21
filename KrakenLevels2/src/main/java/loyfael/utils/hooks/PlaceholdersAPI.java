package loyfael.utils.hooks;

import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import loyfael.Main;
import loyfael.api.interfaces.IPlayerService;
import loyfael.utils.Utils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * Extension PlaceholderAPI modernisée avec architecture SOLID
 * Utilise les nouveaux services au lieu des anciens managers
 */
public class PlaceholdersAPI extends PlaceholderExpansion {

    private final IPlayerService playerService;

    public PlaceholdersAPI() {
        this.playerService = Main.getInstance().getPlayerService();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "krakenlevels";
    }

    @Override
    public @NotNull String getAuthor() {
        return Main.getInstance().getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return Main.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        String playerUuid = player.getUniqueId().toString();

        // Récupérer les données du joueur via le nouveau service
        Optional<IPlayerService.PlayerData> playerDataOpt = playerService.getPlayerData(playerUuid);

        if (playerDataOpt.isEmpty()) {
            // Créer un nouveau profil si nécessaire
            playerService.createPlayer(playerUuid, player.getName());
            playerDataOpt = playerService.getPlayerData(playerUuid);
        }

        if (playerDataOpt.isEmpty()) {
            return "0"; // Fallback si les données ne peuvent pas être créées
        }

        IPlayerService.PlayerData playerData = playerDataOpt.get();
        DecimalFormat df = new DecimalFormat("#,###");

        // Gestion des placeholders
        switch (params.toLowerCase()) {
            case "level":
                return String.valueOf(playerData.getLevel());

            case "level_formatted":
                return df.format(playerData.getLevel());

            case "buttons":
                return String.valueOf(playerData.getButtonAmount());

            case "buttons_formatted":
                return df.format(playerData.getButtonAmount());

            case "name":
                return playerData.getName();

            case "last_seen":
                return formatLastSeen(playerData.getLastSeen());

            // Placeholders pour les rangs
            case "rank":
                return String.valueOf(getPlayerRank(playerUuid));

            case "rank_formatted":
                return df.format(getPlayerRank(playerUuid));

            default:
                return null; // Placeholder non reconnu
        }
    }

    /**
     * Calcule le rang d'un joueur dans le classement
     */
    private int getPlayerRank(String playerUuid) {
        try {
            var topPlayers = playerService.getTopPlayers(1000); // Récupérer un grand nombre pour trouver le rang

            for (int i = 0; i < topPlayers.size(); i++) {
                if (topPlayers.get(i).getUuid().equals(playerUuid)) {
                    return i + 1; // Rang basé sur la position (1-indexé)
                }
            }

            return topPlayers.size() + 1; // Si non trouvé, retourner le rang suivant

        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Erreur lors du calcul du rang pour " + playerUuid + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Formate la date de dernière connexion
     */
    private String formatLastSeen(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (60 * 1000);
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " jour(s)";
        } else if (hours > 0) {
            return hours + " heure(s)";
        } else if (minutes > 0) {
            return minutes + " minute(s)";
        } else {
            return "maintenant";
        }
    }
}
