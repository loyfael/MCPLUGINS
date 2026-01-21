package loyfael;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("Le plugin SkySyncro est activé !");
    }

    @Override
    public void onDisable() {
        getLogger().info("Le plugin SkySyncro est désactivé.");
    }
}