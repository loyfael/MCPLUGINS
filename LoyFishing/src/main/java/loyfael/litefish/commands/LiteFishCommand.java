package loyfael.litefish.commands;

import loyfael.litefish.LiteFish;
import loyfael.litefish.gui.MainMenuGUI;
import loyfael.litefish.managers.PlayerDataManager;
import loyfael.litefish.models.BiomeData;
import loyfael.litefish.models.FishDrop;
import loyfael.litefish.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main command handler for LiteFish
 */
public class LiteFishCommand implements CommandExecutor, TabCompleter {
    
    private final LiteFish plugin;
    
    public LiteFishCommand(LiteFish plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                sendHelpMessage(sender);
                break;
                
            case "menu":
            case "gui":
                if (!(sender instanceof Player)) {
                    MessageUtils.sendMessage(sender, "&cThis command can only be used by players!");
                    return true;
                }
                
                new MainMenuGUI(plugin, (Player) sender).open();
                break;
                
            case "reload":
                if (!sender.hasPermission("litefish.admin.reload")) {
                    MessageUtils.sendConfigMessage(sender, "no-permission");
                    return true;
                }
                
                plugin.reload();
                MessageUtils.sendConfigMessage(sender, "reload-success");
                break;
                
            case "info":
                sendInfoMessage(sender);
                break;
                
            case "stats":
                if (!(sender instanceof Player)) {
                    MessageUtils.sendMessage(sender, "&cThis command can only be used by players!");
                    return true;
                }
                
                Player target = (Player) sender;
                if (args.length > 1 && sender.hasPermission("litefish.admin.stats")) {
                    Player targetPlayer = Bukkit.getPlayer(args[1]);
                    if (targetPlayer != null) {
                        target = targetPlayer;
                    } else {
                        MessageUtils.sendConfigMessage(sender, "player-not-found");
                        return true;
                    }
                }
                
                sendStatsMessage(sender, target);
                break;
                
            case "biomes":
                sendBiomesMessage(sender);
                break;
                
            case "drops":
                if (args.length > 1) {
                    sendDropsForBiome(sender, args[1]);
                } else {
                    sendAllDropsMessage(sender);
                }
                break;
                
            case "sell":
                if (!(sender instanceof Player)) {
                    MessageUtils.sendMessage(sender, "&cThis command can only be used by players!");
                    return true;
                }
                
                sellFish((Player) sender);
                break;
                
            default:
                MessageUtils.sendMessage(sender, "&cCommande inconnue. Utilisez /lfish help pour l'aide.");
                break;
        }
        
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        MessageUtils.sendRawMessage(sender, "&b&l=== Aide LiteFish ===");
        MessageUtils.sendRawMessage(sender, "&e/lfish help &7- Afficher ce message d'aide");
        MessageUtils.sendRawMessage(sender, "&e/lfish info &7- Afficher les informations du plugin");
        MessageUtils.sendRawMessage(sender, "&e/lfish stats [joueur] &7- Afficher les statistiques de pêche");
        MessageUtils.sendRawMessage(sender, "&e/lfish biomes &7- Lister tous les biomes configurés");
        MessageUtils.sendRawMessage(sender, "&e/lfish drops [biome] &7- Afficher les drops de pêche");
        MessageUtils.sendRawMessage(sender, "&e/lfish sell &7- Vendre tous les poissons de l'inventaire");
        
        if (sender.hasPermission("litefish.admin")) {
            MessageUtils.sendRawMessage(sender, "&c&lCommandes Admin :");
            MessageUtils.sendRawMessage(sender, "&e/lfish reload &7- Recharger la configuration du plugin");
        }
    }
    
    private void sendInfoMessage(CommandSender sender) {
        MessageUtils.sendRawMessage(sender, "&b&l=== Informations LiteFish ===");
        MessageUtils.sendRawMessage(sender, "&eVersion : &f" + plugin.getDescription().getVersion());
        MessageUtils.sendRawMessage(sender, "&eAuteur : &f" + String.join(", ", plugin.getDescription().getAuthors()));
        MessageUtils.sendRawMessage(sender, "&eBiomes chargés : &f" + plugin.getBiomeManager().getTotalConfiguredBiomes());
        MessageUtils.sendRawMessage(sender, "&eDrops chargés : &f" + plugin.getDropManager().getTotalDrops());
        
        // Show hooked plugins
        List<String> hooks = new ArrayList<>();
        if (plugin.getVaultHook().isEnabled()) hooks.add("Vault");
        if (plugin.getWorldGuardHook().isEnabled()) hooks.add("WorldGuard");
        if (plugin.getNexoHook().isEnabled()) hooks.add("Nexo");
        
        MessageUtils.sendRawMessage(sender, "&eHooked Plugins: &f" + 
            (hooks.isEmpty() ? "None" : String.join(", ", hooks)));
    }
    
    private void sendStatsMessage(CommandSender sender, Player target) {
        PlayerDataManager.PlayerFishingData playerData = plugin.getPlayerDataManager().getPlayerData(target);
        
        MessageUtils.sendRawMessage(sender, "&b&l=== Fishing Stats for " + target.getName() + " ===");
        MessageUtils.sendRawMessage(sender, "&eTotal Fish Caught: &f" + MessageUtils.formatNumber(playerData.getTotalFishCaught()));
        
        Map<String, Integer> fishCaught = playerData.getFishCaught();
        if (!fishCaught.isEmpty()) {
            MessageUtils.sendRawMessage(sender, "&eTop Fish Types:");
            fishCaught.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .forEach(entry -> {
                    Optional<FishDrop> drop = plugin.getDropManager().getDrop(entry.getKey());
                    String displayName = drop.map(FishDrop::getDisplayName).orElse(entry.getKey());
                    MessageUtils.sendRawMessage(sender, "&f  " + displayName + ": &e" + entry.getValue());
                });
        }
    }
    
    private void sendBiomesMessage(CommandSender sender) {
        MessageUtils.sendRawMessage(sender, "&b&l=== Configured Biomes ===");
        
        Map<Biome, BiomeData> biomes = plugin.getBiomeManager().getAllBiomeData();
        if (biomes.isEmpty()) {
            MessageUtils.sendRawMessage(sender, "&cNo biomes configured!");
            return;
        }
        
        for (Map.Entry<Biome, BiomeData> entry : biomes.entrySet()) {
            BiomeData biomeData = entry.getValue();
            MessageUtils.sendRawMessage(sender, "&e" + biomeData.getName() + " &7(" + entry.getKey() + ")");
            MessageUtils.sendRawMessage(sender, "&f  Monster Chance: &e" + biomeData.getMonsterChance() + "%");
            MessageUtils.sendRawMessage(sender, "&f  Color: &e" + biomeData.getColor());
        }
    }
    
    private void sendAllDropsMessage(CommandSender sender) {
        MessageUtils.sendRawMessage(sender, "&b&l=== All Fishing Drops ===");
        
        Map<String, FishDrop> drops = plugin.getDropManager().getAllDrops();
        if (drops.isEmpty()) {
            MessageUtils.sendRawMessage(sender, "&cNo drops configured!");
            return;
        }
        
        for (FishDrop drop : drops.values()) {
            MessageUtils.sendRawMessage(sender, "&e" + drop.getDisplayName() + " &7(" + drop.getKey() + ")");
            MessageUtils.sendRawMessage(sender, "&f  Chance: &e" + drop.getChance() + "%");
            MessageUtils.sendRawMessage(sender, "&f  Experience: &e" + drop.getExperience());
            if (plugin.getVaultHook().isEnabled()) {
                MessageUtils.sendRawMessage(sender, "&f  Price: &e$" + MessageUtils.formatNumber(drop.getPrice()));
            }
        }
    }
    
    private void sendDropsForBiome(CommandSender sender, String biomeName) {
        try {
            Biome biome = Biome.valueOf(biomeName.toUpperCase());
            List<FishDrop> drops = plugin.getDropManager().getDropsForBiome(biome);
            
            MessageUtils.sendRawMessage(sender, "&b&l=== Drops for " + biomeName + " ===");
            
            if (drops.isEmpty()) {
                MessageUtils.sendRawMessage(sender, "&cNo drops configured for this biome!");
                return;
            }
            
            for (FishDrop drop : drops) {
                MessageUtils.sendRawMessage(sender, "&e" + drop.getDisplayName());
                MessageUtils.sendRawMessage(sender, "&f  Chance: &e" + drop.getChance() + "%");
                MessageUtils.sendRawMessage(sender, "&f  Experience: &e" + drop.getExperience());
                if (plugin.getVaultHook().isEnabled()) {
                    MessageUtils.sendRawMessage(sender, "&f  Price: &e$" + MessageUtils.formatNumber(drop.getPrice()));
                }
            }
            
        } catch (IllegalArgumentException e) {
            MessageUtils.sendMessage(sender, "&cInvalid biome name: " + biomeName);
        }
    }
    
    private void sellFish(Player player) {
        if (!plugin.getVaultHook().isEnabled()) {
            MessageUtils.sendMessage(player, "&cEconomy is not enabled!");
            return;
        }
        
        // This is a simplified version - in a real implementation, 
        // you would check the player's inventory for fish items and sell them
        MessageUtils.sendMessage(player, "&eFish selling feature is not yet implemented!");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "menu", "gui", "info", "stats", "biomes", "drops", "sell");
            if (sender.hasPermission("litefish.admin")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("reload");
            }
            
            return subCommands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "stats":
                    if (sender.hasPermission("litefish.admin.stats")) {
                        return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                    break;
                    
                case "drops":
                    return Arrays.stream(Biome.values())
                        .map(biome -> biome.name().toLowerCase())
                        .filter(name -> name.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}
