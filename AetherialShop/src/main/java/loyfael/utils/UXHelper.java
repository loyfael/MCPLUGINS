package loyfael.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.Particle;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UXHelper {

    private static final Logger logger = org.bukkit.Bukkit.getLogger();

    public static void playSuccessSound(Player player) {
        try {
            if (player == null || !player.isOnline()) {
                logger.warning("playSuccessSound appelé avec joueur null/déconnecté");
                return;
            }

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
            logger.fine("Son de succès joué pour " + player.getName());

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la lecture du son de succès pour " +
                (player != null ? player.getName() : "null"), e);
        }
    }

    public static void playErrorSound(Player player) {
        try {
            if (player == null || !player.isOnline()) {
                logger.warning("playErrorSound appelé avec joueur null/déconnecté");
                return;
            }

            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
            logger.fine("Son d'erreur joué pour " + player.getName());

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la lecture du son d'erreur pour " +
                (player != null ? player.getName() : "null"), e);
        }
    }

    public static void playClickSound(Player player) {
        try {
            if (player == null || !player.isOnline()) {
                logger.warning("playClickSound appelé avec joueur null/déconnecté");
                return;
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            logger.fine("Son de clic joué pour " + player.getName());

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la lecture du son de clic pour " +
                (player != null ? player.getName() : "null"), e);
        }
    }

    public static void playShopOpenSound(Player player) {
        try {
            if (player == null || !player.isOnline()) {
                logger.warning("playShopOpenSound appelé avec joueur null/déconnecté");
                return;
            }

            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);
            logger.fine("Son d'ouverture de shop joué pour " + player.getName());

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la lecture du son d'ouverture pour " +
                (player != null ? player.getName() : "null"), e);
        }
    }

    public static void playMoneySound(Player player) {
        try {
            if (player == null || !player.isOnline()) {
                logger.warning("playMoneySound appelé avec joueur null/déconnecté");
                return;
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la lecture du son d'argent pour " +
                (player != null ? player.getName() : "null"), e);
        }
    }

    public static void sendSuccessMessage(Player player, String message) {
        try {
            if (player == null || !player.isOnline()) {
                logger.warning("sendSuccessMessage appelé avec joueur null/déconnecté");
                return;
            }

            if (message == null) {
                logger.warning("sendSuccessMessage appelé avec message null pour " + player.getName());
                return;
            }

            player.sendMessage("§a✓ " + message);
            playSuccessSound(player);
            logger.fine("Message de succès envoyé à " + player.getName() + ": " + message);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'envoi du message de succès à " +
                (player != null ? player.getName() : "null"), e);
        }
    }

    public static void sendErrorMessage(Player player, String message) {
        try {
            if (player == null || !player.isOnline()) {
                logger.warning("sendErrorMessage appelé avec joueur null/déconnecté");
                return;
            }

            if (message == null) {
                logger.warning("sendErrorMessage appelé avec message null pour " + player.getName());
                return;
            }

            player.sendMessage("§c✗ " + message);
            playErrorSound(player);
            logger.info("Message d'erreur envoyé à " + player.getName() + ": " + message);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'envoi du message d'erreur à " +
                (player != null ? player.getName() : "null"), e);
        }
    }

    public static void sendInfoMessage(Player player, String message) {
        try {
            if (player == null || !player.isOnline()) {
                logger.warning("sendInfoMessage appelé avec joueur null/déconnecté");
                return;
            }

            if (message == null) {
                logger.warning("sendInfoMessage appelé avec message null pour " + player.getName());
                return;
            }

            player.sendMessage("§e⚡ " + message);
            logger.fine("Message d'info envoyé à " + player.getName() + ": " + message);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'envoi du message d'info à " +
                (player != null ? player.getName() : "null"), e);
        }
    }

    public static void sendTransactionMessage(Player player, String action, String item, int quantity, double amount) {
        try {
            if (player == null || !player.isOnline()) {
                logger.warning("sendTransactionMessage appelé avec joueur null/déconnecté");
                return;
            }

            if (action == null || item == null) {
                logger.warning("sendTransactionMessage appelé avec paramètres null pour " + player.getName());
                return;
            }

            if (quantity <= 0 || amount < 0) {
                logger.warning("sendTransactionMessage appelé avec valeurs invalides pour " + player.getName() +
                    ": quantity=" + quantity + ", amount=" + amount);
                return;
            }

            player.sendMessage("§8§m────────────────────────────§r");
            player.sendMessage("§a§l   TRANSACTION VALIDÉE");
            player.sendMessage(" ");
            player.sendMessage("§7Item: §f" + item);
            player.sendMessage("§7Quantité: §e" + quantity + "x");
            player.sendMessage("§7Montant: §6" + String.format("%.2f", amount) + "◎");
            player.sendMessage("§8§m────────────────────────────§r");
            playMoneySound(player);

            logger.info("Message de transaction envoyé à " + player.getName() + ": " + action + " " +
                       quantity + "x " + item + " pour " + amount + "◎");

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'envoi du message de transaction à " +
                (player != null ? player.getName() : "null"), e);
        }
    }

    public static String formatMoney(double amount) {
        try {
            if (amount < 0) {
                logger.warning("formatMoney appelé avec montant négatif: " + amount);
                return "0◎";
            }

            if (amount >= 1000000) {
                return String.format("%.1fM◎", amount / 1000000);
            } else if (amount >= 1000) {
                return String.format("%.1fK◎", amount / 1000);
            } else {
                return String.format("%.0f◎", amount);
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors du formatage du montant: " + amount, e);
            return "0◎";
        }
    }

    public static String getStockColorCode(int current, int max) {
        try {
            if (max <= 0) {
                logger.warning("getStockColorCode appelé avec max invalide: " + max);
                return "§7";
            }

            if (current < 0) {
                logger.warning("getStockColorCode appelé avec current négatif: " + current);
                current = 0;
            }

            double ratio = (double) current / max;
            if (ratio > 0.7) return "§a";
            if (ratio > 0.3) return "§e";
            return "§c";

        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors du calcul du code couleur stock", e);
            return "§7";
        }
    }

}
