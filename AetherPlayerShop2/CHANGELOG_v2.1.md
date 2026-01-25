# 📋 Améliorations AetherPlayerShop - Version 2.1

## ✅ Modifications implémentées

### 1. 🎨 **Titres de menus modernisés**
- **Tous les titres en gris foncé (§8 / DARK_GRAY)**
- **Titres simples et explicatifs sans émojis**
- Menu principal : "§8Catalogue" (au lieu de "✦ Aether Player Shop ✦")
- Menu d'achat : "§8Achat - [Nom item]"
- Menu d'édition : "§8Édition - [Nom item]"
- Menu de configuration : "§8Configuration du shop"
- Sous-menus : "§8Shops - [Matériau]"

### 2. 🏷️ **Support amélioré des items customisés**
- **Affichage uniquement du nom custom sur les pancartes**
  - Si l'item a un nom custom : affiche UNIQUEMENT ce nom (pas le type de matériau)
  - Si pas de nom custom : affiche le nom du matériau formaté
- **Format sur pancarte** :
  ```
  [VENTE] ou [ACHAT]
  10.50◎
  Épée Légendaire     (← nom custom uniquement)
  Stock: 15
  ```
- **Les items customs sont maintenant correctement reconnus** pour les achats

### 3. 🔢 **Système de permissions de shops personnalisable**
- **Nouvelle permission dynamique** : `aetherplayershop.shops.<nombre>`
- **Exemples** :
  - `aetherplayershop.shops.5` → jusqu'à 5 shops
  - `aetherplayershop.shops.10` → jusqu'à 10 shops
  - `aetherplayershop.shops.100` → jusqu'à 100 shops
- **Supprimées** : Les anciennes permissions fixes (.1, .3, .5, .10, .unlimited)
- Le plugin prend automatiquement le nombre le plus élevé trouvé

### 4. ⏸️ **Système d'inactivité automatique**
- **Désactivation automatique** après X jours sans activité (configurable)
- **Affichage sur pancarte inactive** :
  ```
  [INACTIF]
  ⏸ Désactivé
  [Nom de l'item]
  Clic droit = Réactiver
  ```
- **Retrait automatique du catalogue** (/shop) pour les shops inactifs
- **Réactivation simple** : Clic droit sur la pancarte par le propriétaire
- **Configuration** : `shop.inactivity-days` dans config.yml (défaut: 30 jours)

### 5. 🔄 **Mise à jour automatique du stock sur panneau**
- Le stock sur la pancarte se met automatiquement à jour après chaque achat
- Affichage en temps réel du stock disponible dans le coffre
- Indication visuelle quand le stock est épuisé (croix rouge + "RUPTURE")

### 6. 🔌 **Amélioration de la connexion MySQL**
- **Messages d'erreur détaillés** au démarrage
- **Affichage des paramètres** de connexion pour faciliter le debug
- **Guide de dépannage automatique** en cas d'échec
- **Création automatique des tables** lors de la première connexion

---

## 📝 Configuration mise à jour

### config.yml
```yaml
# Configuration des shops
shop:
  max-shops-per-player: 6
  teleport-enabled: true
  teleport-delay: 3
  inactivity-days: 30  # ← NOUVEAU

# Configuration des menus GUI
gui:
  menu-title: "§8Catalogue"  # ← MODIFIÉ
  items-per-page: 45
```

### plugin.yml
```yaml
permissions:
  # Permission dynamique pour le nombre de shops
  # Format: aetherplayershop.shops.<nombre>
  # Exemple: aetherplayershop.shops.10
  
  aetherplayershop.bypasslimit:
    description: Bypass la limite de shops (illimité)
    default: op
```

---

## 🎯 Exemples d'utilisation

### Permissions LuckPerms
```bash
# Donner 3 shops à un joueur
lp user <joueur> permission set aetherplayershop.shops.3

# Donner 10 shops à un groupe VIP
lp group vip permission set aetherplayershop.shops.10

# Donner 50 shops à un groupe Premium
lp group premium permission set aetherplayershop.shops.50

# Shops illimités pour les admins
lp group admin permission set aetherplayershop.bypasslimit true
```

### Affichage des pancartes

**Shop actif avec stock** :
```
[VENTE]
10.50◎
Épée Légendaire
Stock: 15
```

**Shop avec item non-custom** :
```
[ACHAT]
5.00◎
Diamond Sword
Stock: 32
```

**Shop en rupture de stock** :
```
[VENTE]
✗ RUPTURE
Épée Légendaire
Propriétaire
```

**Shop inactif** :
```
[INACTIF]
⏸ Désactivé
Épée Légendaire
Clic droit = Réactiver
```

---

## 🔄 Migration depuis l'ancienne version

### Pour les serveurs existants :

1. **Mettre à jour le JAR** dans le dossier plugins/
2. **Redémarrer le serveur** pour générer le nouveau config.yml
3. **Migrer les permissions** :
   ```bash
   # Remplacer les anciennes permissions
   aetherplayershop.shops.1  → aetherplayershop.shops.1
   aetherplayershop.shops.3  → aetherplayershop.shops.3
   aetherplayershop.shops.5  → aetherplayershop.shops.5
   aetherplayershop.shops.10 → aetherplayershop.shops.10
   aetherplayershop.shops.unlimited → aetherplayershop.bypasslimit
   ```

4. **Configurer l'inactivité** dans config.yml si besoin

### Base de données
- **Aucune migration nécessaire** pour la structure existante
- Les anciennes données restent compatibles
- Le champ `lastActivity` sera ajouté automatiquement

---

## 🐛 Corrections de bugs

- ✅ Items customisés maintenant reconnus correctement pour les achats
- ✅ Stock mis à jour en temps réel sur les pancartes
- ✅ Meilleurs messages d'erreur pour la connexion MySQL
- ✅ Titres de menus cohérents et lisibles

---

## 🚀 Prochaines étapes recommandées

1. **Tester les items customisés** avec différents plugins (ItemsAdder, Oraxen, etc.)
2. **Configurer l'inactivité** selon vos besoins serveur
3. **Mettre à jour les permissions** des joueurs
4. **Communiquer les changements** aux joueurs via Discord/annonce in-game

---

## 📞 Support

Pour toute question ou problème :
1. Vérifier les logs dans `logs/latest.log`
2. Le plugin affiche maintenant des messages détaillés au démarrage
3. Consulter les fichiers FIX_MYSQL.md et MYSQL_SETUP.md pour les problèmes de connexion

---

**Version** : 2.1.0
**Date** : 04/10/2025
**Compatibilité** : Paper/Bukkit 1.21+
