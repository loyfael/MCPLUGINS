package loyfael.model;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modèle représentant un item dans un shop avec ses métadonnées
 */
public class ShopItem {

    private final Material material;
    private final int amount;
    private final String customName;
    private final List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private final int customModelData;

    public ShopItem(@NotNull ItemStack itemStack) {
        this.material = itemStack.getType();
        this.amount = itemStack.getAmount();

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            // Utilisation des APIs modernes Adventure Component
            this.customName = meta.hasDisplayName() ?
                PlainTextComponentSerializer.plainText().serialize(meta.displayName()) : null;
            this.lore = meta.hasLore() ?
                meta.lore().stream()
                    .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                    .collect(java.util.stream.Collectors.toList()) : null;
            this.enchantments = new HashMap<>(meta.getEnchants());
            // Utilisation sécurisée pour CustomModelData
            this.customModelData = getCustomModelDataSafe(meta);
        } else {
            this.customName = null;
            this.lore = null;
            this.enchantments = new HashMap<>();
            this.customModelData = 0;
        }
    }

    public ShopItem(@NotNull Document document) {
        this.material = Material.valueOf(document.getString("material"));
        this.amount = document.getInteger("amount", 1);
        this.customName = document.getString("customName");
        this.customModelData = document.getInteger("customModelData", 0);

        // Lore
        @SuppressWarnings("unchecked")
        List<String> loreList = (List<String>) document.get("lore");
        this.lore = loreList;

        // Enchantements avec l'API moderne Registry
        this.enchantments = new HashMap<>();
        Document enchantsDoc = document.get("enchantments", Document.class);
        if (enchantsDoc != null) {
            for (String enchantName : enchantsDoc.keySet()) {
                try {
                    // Utilisation de l'API moderne Registry
                    NamespacedKey key = NamespacedKey.minecraft(enchantName.toLowerCase());
                    Enchantment enchantment = getEnchantmentSafe(key);
                    if (enchantment != null) {
                        enchantments.put(enchantment, enchantsDoc.getInteger(enchantName));
                    }
                } catch (Exception e) {
                    // Enchantement non reconnu, ignoré
                }
            }
        }
    }

    /**
     * Convertit le ShopItem en Document MongoDB
     */
    public Document toDocument() {
        Document document = new Document()
                .append("material", material.name())
                .append("amount", amount)
                .append("customModelData", customModelData);

        if (customName != null) {
            document.append("customName", customName);
        }

        if (lore != null && !lore.isEmpty()) {
            document.append("lore", lore);
        }

        if (!enchantments.isEmpty()) {
            Document enchantsDoc = new Document();
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                // Utilisation de l'API moderne au lieu de getName() déprécié
                String enchantKey = entry.getKey().getKey().getKey();
                enchantsDoc.append(enchantKey, entry.getValue());
            }
            document.append("enchantments", enchantsDoc);
        }

        return document;
    }

    /**
     * Crée un ItemStack à partir de ce ShopItem
     */
    @NotNull
    public ItemStack toItemStack() {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            if (customName != null && !customName.isEmpty()) {
                // Utilisation de l'API moderne Adventure Component
                meta.displayName(Component.text(customName));
            }

            if (lore != null && !lore.isEmpty()) {
                // Conversion de la lore String vers Adventure Components
                List<Component> loreComponents = lore.stream()
                    .map(Component::text)
                    .collect(java.util.stream.Collectors.toList());
                meta.lore(loreComponents);
            }

            if (!enchantments.isEmpty()) {
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            // Utilisation sécurisée pour setCustomModelData
            if (customModelData > 0) {
                setCustomModelDataSafe(meta, customModelData);
            }

            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    /**
     * Crée un ItemStack de démonstration pour l'affichage en GUI
     */
    @NotNull
    public ItemStack toDisplayItemStack(int displayAmount) {
        ItemStack display = toItemStack();
        display.setAmount(Math.min(displayAmount, 64));
        return display;
    }

    /**
     * Vérifie si cet item correspond exactement à un ItemStack donné
     */
    public boolean matches(@NotNull ItemStack other) {
        if (other.getType() != this.material) {
            return false;
        }

        ItemMeta otherMeta = other.getItemMeta();
        if (otherMeta == null && (customName != null || lore != null || !enchantments.isEmpty())) {
            return false;
        }

        if (otherMeta != null) {
            // Vérification du nom personnalisé avec l'API moderne Adventure
            String otherName = otherMeta.hasDisplayName() ?
                PlainTextComponentSerializer.plainText().serialize(otherMeta.displayName()) : null;
            if (!java.util.Objects.equals(customName, otherName)) {
                return false;
            }

            // Vérification de la lore avec l'API moderne Adventure
            List<String> otherLore = otherMeta.hasLore() ?
                otherMeta.lore().stream()
                    .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                    .collect(java.util.stream.Collectors.toList()) : null;
            if (!java.util.Objects.equals(lore, otherLore)) {
                return false;
            }

            // Vérification des enchantements
            if (!enchantments.equals(otherMeta.getEnchants())) {
                return false;
            }

            // Utilisation sécurisée pour hasCustomModelData
            int otherCustomModelData = getCustomModelDataSafe(otherMeta);
            if (customModelData != otherCustomModelData) {
                return false;
            }
        }

        return true;
    }

    /**
     * Obtient le nom d'affichage de l'item
     */
    @NotNull
    public String getDisplayName() {
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }

        // Formatage du nom du matériau
        String materialName = material.name().toLowerCase().replace('_', ' ');
        return materialName.substring(0, 1).toUpperCase() + materialName.substring(1);
    }

    // Getters
    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    @Nullable public String getCustomName() { return customName; }
    @Nullable public List<String> getLore() { return lore; }
    public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
    public int getCustomModelData() { return customModelData; }

    /**
     * Méthodes utilitaires pour encapsuler les API dépréciées
     */
    @SuppressWarnings("deprecation")
    private static int getCustomModelDataSafe(@NotNull ItemMeta meta) {
        try {
            return meta.hasCustomModelData() ? meta.getCustomModelData() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("deprecation")
    private static void setCustomModelDataSafe(@NotNull ItemMeta meta, int customModelData) {
        try {
            meta.setCustomModelData(customModelData);
        } catch (Exception ignored) {
            // API non disponible, ignorer silencieusement
        }
    }

    @SuppressWarnings("deprecation")
    private static Enchantment getEnchantmentSafe(@NotNull NamespacedKey key) {
        try {
            return Registry.ENCHANTMENT.get(key);
        } catch (Exception e) {
            return null;
        }
    }
}
