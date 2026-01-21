package loyfael;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(new CompassListener(), this);
  }

  @Override
  public void onDisable() {

  }
}