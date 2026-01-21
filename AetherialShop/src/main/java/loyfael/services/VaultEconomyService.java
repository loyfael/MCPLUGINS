package loyfael.services;

import loyfael.interfaces.IEconomyService;
import loyfael.cache.ShopCache;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

public class VaultEconomyService implements IEconomyService {

    private final Economy economy;
    private final Logger logger;
    private final ShopCache cache;

    public VaultEconomyService(Economy economy, ShopCache cache) {
        this.economy = economy;
        this.cache = cache;
        this.logger = Logger.getLogger(this.getClass().getName());
        logger.info("VaultEconomyService initialisé avec cache optimisé");
    }

    @Override
    public boolean hasEnoughMoney(Player player, double amount) {
        try {
            if (player == null || amount < 0 || !player.isOnline()) {
                return false;
            }

            // Tentative d'utiliser le cache d'abord
            ShopCache.CachedPlayerData cached = cache.getPlayerCache(player.getName());
            if (cached != null && cached.lastKnownBalance >= amount) {
                logger.fine("Cache hit pour hasEnoughMoney: " + player.getName());
                return true;
            }

            double balance = economy.getBalance(player);
            boolean hasEnough = balance >= amount;

            // Mettre à jour le cache
            if (cached != null) {
                cache.updatePlayerCache(player.getName(), balance, cached.inventorySlots);
            } else {
                cache.updatePlayerCache(player.getName(), balance, -1);
            }

            logger.fine("Vérification fonds pour " + player.getName() + ": " + hasEnough);
            return hasEnough;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la vérification des fonds pour " +
                (player != null ? player.getName() : "null"), e);
            return false;
        }
    }

    @Override
    public boolean withdrawMoney(Player player, double amount) {
        try {
            if (player == null || amount <= 0 || !player.isOnline()) {
                return false;
            }

            EconomyResponse response = economy.withdrawPlayer(player, amount);

            if (response.transactionSuccess()) {
                // Invalider le cache après transaction réussie
                cache.invalidatePlayerCache(player.getName());
                logger.info("Retrait réussi pour " + player.getName() + ": " + amount + "◎ (nouveau solde=" + response.balance + ")");
                return true;
            } else {
                logger.severe("Échec du retrait pour " + player.getName() + ": " + response.errorMessage);
                return false;
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique lors du retrait", e);
            return false;
        }
    }

    @Override
    public void depositMoney(Player player, double amount) {
        try {
            if (player == null || amount <= 0 || !player.isOnline()) {
                return;
            }

            EconomyResponse response = economy.depositPlayer(player, amount);

            if (response.transactionSuccess()) {
                // Invalider le cache après transaction réussie
                cache.invalidatePlayerCache(player.getName());
                logger.info("Dépôt réussi pour " + player.getName() + ": +" + amount + "◎");
            } else {
                logger.severe("Échec du dépôt: " + response.errorMessage);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique lors du dépôt", e);
        }
    }

    @Override
    public double getBalance(Player player) {
        try {
            if (player == null || !player.isOnline()) {
                return 0.0;
            }

            // Utiliser le cache si disponible et récent
            ShopCache.CachedPlayerData cached = cache.getPlayerCache(player.getName());
            if (cached != null) {
                logger.fine("Cache hit pour getBalance: " + player.getName());
                return cached.lastKnownBalance;
            }

            double balance = economy.getBalance(player);

            // Mettre à jour le cache
            cache.updatePlayerCache(player.getName(), balance, -1);

            return balance;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération du solde", e);
            return 0.0;
        }
    }
}
