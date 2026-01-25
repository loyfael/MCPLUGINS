package loyfael.placeholders;

import loyfael.Main;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for AetherSafeMode
 * Provides %aethersafemode_status% placeholder
 */
public class AetherSafeModeExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public AetherSafeModeExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "aethersafemode";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the expansion on reload
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // %aethersafemode_status%
        if (params.equalsIgnoreCase("status")) {
            boolean isSafeMode = plugin.getSafeModeManager().isSafeMode(player);
            return isSafeMode ? "§aON" : "§cOFF";
        }

        // Return null if placeholder is not recognized
        return null;
    }
}
