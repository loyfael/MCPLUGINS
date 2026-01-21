package loyfael.services;

import com.google.gson.JsonObject;
import loyfael.model.ShopItem;
import loyfael.utils.UXHelper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordService {
    
    private final JavaPlugin plugin;
    private final Logger logger;
    private final String webhookUrl;
    private final boolean enabled;
    private final String embedColor;
    
    public DiscordService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        this.webhookUrl = plugin.getConfig().getString("discord.webhook_url", "");
        this.embedColor = plugin.getConfig().getString("discord.embed_color", "#FFD700");
        
        if (enabled && (webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK"))) {
            logger.warning("Discord activé mais webhook URL non configurée !");
        }
    }
    
    public boolean sendRotationEmbed(List<ShopItem> items) {
        if (!enabled || webhookUrl.isEmpty()) {
            return false;
        }
        
        try {
            JsonObject embed = createRotationEmbed(items);
            return sendEmbed(embed);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'envoi de l'embed de rotation", e);
            return false;
        }
    }
    
    public boolean sendTestEmbed() {
        logger.info("=== DIAGNOSTIC DISCORD TEST ===");
        logger.info("Enabled: " + enabled);
        logger.info("Webhook URL vide: " + webhookUrl.isEmpty());
        logger.info("Webhook URL: " + (webhookUrl.isEmpty() ? "VIDE" : "CONFIGURÉ"));
        logger.info("isEnabled(): " + isEnabled());
        
        if (!enabled || webhookUrl.isEmpty()) {
            logger.warning("Discord non configuré ou désactivé");
            return false;
        }
        
        try {
            logger.info("Création de l'embed de test...");
            JsonObject embed = createTestEmbed();
            logger.info("Envoi de l'embed...");
            boolean success = sendEmbed(embed);
            
            if (success) {
                logger.info("✓ Embed de test envoyé avec succès !");
            } else {
                logger.warning("✗ Échec de l'envoi de l'embed de test");
            }
            
            return success;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'envoi de l'embed de test", e);
            return false;
        }
    }
    
    private JsonObject createRotationEmbed(List<ShopItem> items) {
        JsonObject embed = new JsonObject();
        
        // Informations principales avec style RP
        embed.addProperty("title", "🌟 Le Bazaar de Zéphyline s'anime !");
        embed.addProperty("description", "*Zéphyline Bricorne vous invite à découvrir son étal pour acheter et vendre..*");
        embed.addProperty("color", Integer.parseInt(embedColor.replace("#", ""), 16));
        embed.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        
        // Thumbnail
        JsonObject thumbnail = new JsonObject();
        thumbnail.addProperty("url", "https://cdn-icons-png.flaticon.com/512/14425/14425895.png"); // Remplacer par votre image
        embed.add("thumbnail", thumbnail);
        
        // Fields avec TOUS les items
        com.google.gson.JsonArray fields = new com.google.gson.JsonArray();
        
        // Message d'accueil RP
        JsonObject welcomeField = new JsonObject();
        welcomeField.addProperty("name", "Zéphyline vous murmure..");
        welcomeField.addProperty("value", "*\"Approchez-vous, aventuriers ! Venez acheter mes trouvailles ou vendez-moi vos trésors..\"*");
        welcomeField.addProperty("inline", false);
        fields.add(welcomeField);
        
        // Tous les items disponibles
        if (!items.isEmpty()) {
            StringBuilder allItems = new StringBuilder();
            allItems.append("```\n");
            
            for (ShopItem item : items) {
                String itemName = item.getName() != null ? item.getName() : item.getMaterial().name();
                allItems.append(String.format("%-25s %s\n", 
                    itemName,
                    UXHelper.formatMoney(item.getCurrentPrice())
                ));
            }
            
            allItems.append("```");
            
            JsonObject itemsField = new JsonObject();
            itemsField.addProperty("name", "🛍️ Marchandises du Jour");
            itemsField.addProperty("value", allItems.toString());
            itemsField.addProperty("inline", false);
            fields.add(itemsField);
        }
        
        // Message de fin RP
        JsonObject endField = new JsonObject();
        endField.addProperty("name", "⏰ Rappel Important");
        endField.addProperty("value", "*\"Mes trésors ne restent qu'une journée... Demain, d'autres articles prendront leur place !\"*\n\n**Prochaine rotation:** <t:" + (System.currentTimeMillis() / 1000 + 86400) + ":R>");
        endField.addProperty("inline", false);
        fields.add(endField);
        
        embed.add("fields", fields);
        
        // Footer
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Rendez-vous négocier avec Zéphyline au /spawn !");
        embed.add("footer", footer);
        
        return embed;
    }
    
    private JsonObject createTestEmbed() {
        JsonObject embed = new JsonObject();
        
        embed.addProperty("title", "🧪 Test Discord - AetherialShop");
        embed.addProperty("description", "Ceci est un message de test pour vérifier la connexion Discord.");
        embed.addProperty("color", Integer.parseInt(embedColor.replace("#", ""), 16));
        embed.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        
        // Field de test
        com.google.gson.JsonArray fields = new com.google.gson.JsonArray();
        
        JsonObject testField = new JsonObject();
        testField.addProperty("name", "✅ Statut");
        testField.addProperty("value", "Le plugin fonctionne correctement !");
        testField.addProperty("inline", false);
        fields.add(testField);
        
        JsonObject configField = new JsonObject();
        configField.addProperty("name", "⚙️ Configuration");
        configField.addProperty("value", String.format(
            "Webhook: Configuré ✓\nCouleur: %s\nHeure: %s",
            embedColor,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        ));
        configField.addProperty("inline", false);
        fields.add(configField);
        
        embed.add("fields", fields);
        
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Test réussi • AetherialShop");
        embed.add("footer", footer);
        
        return embed;
    }
    
    private boolean sendEmbed(JsonObject embed) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(webhookUrl);
            
            // Créer le payload Discord
            JsonObject payload = new JsonObject();
            com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
            embeds.add(embed);
            payload.add("embeds", embeds);
            
            // Configurer la requête
            StringEntity entity = new StringEntity(payload.toString(), "UTF-8");
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("User-Agent", "AetherialShop-Bot/1.0");
            
            // Envoyer la requête
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode >= 200 && statusCode < 300) {
                    logger.info("✓ Embed Discord envoyé avec succès (code: " + statusCode + ")");
                    return true;
                } else {
                    logger.warning("✗ Erreur Discord: " + statusCode + " - " + response.getStatusLine().getReasonPhrase());
                    return false;
                }
            }
            
        } catch (IOException e) {
            logger.log(Level.WARNING, "Erreur réseau lors de l'envoi Discord", e);
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled && !webhookUrl.isEmpty() && !webhookUrl.contains("YOUR_WEBHOOK");
    }
}
