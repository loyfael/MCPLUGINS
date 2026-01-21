package loyfael.litefish.hooks;

import loyfael.litefish.LiteFish;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Hook for Vault economy integration
 */
public class VaultHook {
    
    private final LiteFish plugin;
    private Economy economy;
    private boolean enabled = false;
    
    public VaultHook(LiteFish plugin) {
        this.plugin = plugin;
    }
    
    public boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager()
                .getRegistration(Economy.class);
        
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        enabled = (economy != null);
        return enabled;
    }
    
    public boolean isEnabled() {
        return enabled && economy != null;
    }
    
    public void disable() {
        economy = null;
        enabled = false;
    }
    
    public boolean hasAccount(Player player) {
        return isEnabled() && economy.hasAccount(player);
    }
    
    public double getBalance(Player player) {
        if (!isEnabled()) return 0.0;
        return economy.getBalance(player);
    }
    
    public boolean withdrawPlayer(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    public boolean depositPlayer(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    public String format(double amount) {
        if (!isEnabled()) return String.valueOf(amount);
        return economy.format(amount);
    }
    
    public String getCurrencyNamePlural() {
        if (!isEnabled()) return "coins";
        return economy.currencyNamePlural();
    }
    
    public String getCurrencyNameSingular() {
        if (!isEnabled()) return "coin";
        return economy.currencyNameSingular();
    }
}
