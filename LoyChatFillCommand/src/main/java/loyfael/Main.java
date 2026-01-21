package loyfael;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Main extends JavaPlugin implements CommandExecutor {

  private YamlConfiguration messages;

  @Override
  public void onEnable() {
    // Create plugin folder and save default messages.yml
    saveResource("messages.yml", false);

    // Load messages
    loadMessages();

    // Check that the command exists before setting the executor
    var command = this.getCommand("chatfill");
    if (command != null) {
      command.setExecutor(this);
      getLogger().info(getMessage("plugin-enabled"));
    } else {
      getLogger().severe(getMessage("command-not-found"));
      this.setEnabled(false);
    }
  }

  @Override
  public void onDisable() {
    getLogger().info(getMessage("plugin-disabled"));
  }

  private void loadMessages() {
    // First try to load from plugin folder
    File messagesFile = new File(getDataFolder(), "messages.yml");
    if (messagesFile.exists()) {
      messages = YamlConfiguration.loadConfiguration(messagesFile);
    } else {
      // Fallback to resource in JAR
      try (InputStream input = getResource("messages.yml")) {
        if (input != null) {
          messages = YamlConfiguration.loadConfiguration(new InputStreamReader(input));
        }
      } catch (Exception e) {
        getLogger().warning(getMessage("config-load-error", "error", e.getMessage()));
        // Fallback: create empty config
        messages = new YamlConfiguration();
      }
    }
  }

  private String getMessage(String key) {
    if (messages != null) {
      return messages.getString("messages." + key, key);
    }
    return key;
  }

  private String getMessage(String key, String placeholder, String value) {
    String message = getMessage(key);
    return message.replace("{" + placeholder + "}", value);
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {

    if (args.length < 3) {
      sender.sendMessage(getMessage("usage"));
      return true;
    }

    Player target = Bukkit.getPlayerExact(args[0]);
    if (target == null) {
      sender.sendMessage(getMessage("player-not-found", "player", args[0]));
      return true;
    }

    String displayMessage;
    String commandToFill;
    String hoverText = getMessage("default-hover");

    // Rebuild displayMessage (args[1] until the next argument that doesn't end with a word)
    StringBuilder displayBuilder = new StringBuilder();
    int currentIndex = 1;

    // Rebuild the display message
    while (currentIndex < args.length) {
      if (currentIndex > 1) displayBuilder.append(" ");
      displayBuilder.append(args[currentIndex]);

      // If the argument ends with " or is not the first one, continue
      if (args[currentIndex].endsWith("\"") ||
          (currentIndex == args.length - 1) ||
          args[currentIndex + 1].startsWith("\"/")) {
        break;
      }
      currentIndex++;
    }
    displayMessage = displayBuilder.toString();

    // Clean quotes from display message if necessary
    if (displayMessage.startsWith("\"") && displayMessage.endsWith("\"")) {
      displayMessage = displayMessage.substring(1, displayMessage.length() - 1);
    }

    currentIndex++; // Move to next argument

    // Rebuild the command (from currentIndex until the next group between quotes)
    StringBuilder commandBuilder = new StringBuilder();
    while (currentIndex < args.length) {
      if (commandBuilder.length() > 0) commandBuilder.append(" ");
      commandBuilder.append(args[currentIndex]);

      if (args[currentIndex].endsWith("\"") && !args[currentIndex].equals("\"")) {
        break;
      }
      currentIndex++;
    }
    commandToFill = commandBuilder.toString();

    // Clean quotes from command
    if (commandToFill.startsWith("\"") && commandToFill.endsWith("\"")) {
      commandToFill = commandToFill.substring(1, commandToFill.length() - 1);
    }

    currentIndex++; // Move to hover text

    // Rebuild hover text (the rest)
    if (currentIndex < args.length) {
      StringBuilder hoverBuilder = new StringBuilder();
      while (currentIndex < args.length) {
        if (hoverBuilder.length() > 0) hoverBuilder.append(" ");
        hoverBuilder.append(args[currentIndex]);
        currentIndex++;
      }
      hoverText = hoverBuilder.toString();

      // Clean quotes from hover text
      if (hoverText.startsWith("\"") && hoverText.endsWith("\"")) {
        hoverText = hoverText.substring(1, hoverText.length() - 1);
      }
    }

    // Process color codes with &
    displayMessage = displayMessage.replace('&', '§');
    hoverText = hoverText.replace('&', '§');


    // Create clickable message with hover text
    Component clickableMessage = Component.text(displayMessage)
        .clickEvent(ClickEvent.suggestCommand(commandToFill))
        .hoverEvent(Component.text(hoverText).asHoverEvent());

    target.sendMessage(clickableMessage);

    return true;
  }
}