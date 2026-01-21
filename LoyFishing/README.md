# LiteFish - Simplified Advanced Fishing Plugin

Une version simplifiée et modernisée du plugin LiteFish original, conçue pour Minecraft 1.16+ avec support pour Nexo, Vault et WorldGuard.

## 🎣 Fonctionnalités

### Core Features
- **Système de pêche personnalisé** par biome
- **Drops configurables** avec système de rareté
- **Intégration économique** via Vault
- **Support des régions** WorldGuard
- **Items personnalisés** via Nexo
- **Statistiques de joueur** détaillées

### Intégrations
- **Vault** - Système économique pour vendre les poissons
- **WorldGuard** - Respect des permissions de pêche par région
- **Nexo** - Support des cannes à pêche et appâts personnalisés

## 📁 Structure du Projet

```
src/main/java/loyfael/litefish/
├── LiteFish.java                    # Classe principale du plugin
├── commands/
│   └── LiteFishCommand.java         # Gestionnaire des commandes
├── config/
│   └── ConfigManager.java           # Gestionnaire de configuration
├── events/
│   └── FishingListener.java         # Écouteur d'événements de pêche
├── hooks/
│   ├── NexoHook.java               # Intégration Nexo
│   ├── VaultHook.java              # Intégration Vault
│   └── WorldGuardHook.java         # Intégration WorldGuard
├── managers/
│   ├── BiomeManager.java           # Gestionnaire des biomes
│   ├── DropManager.java            # Gestionnaire des drops
│   ├── EconomyManager.java         # Gestionnaire économique
│   └── PlayerDataManager.java      # Données des joueurs
├── models/
│   ├── BiomeData.java              # Modèle de données biome
│   └── FishDrop.java               # Modèle de drop de poisson
└── utils/
    └── MessageUtils.java           # Utilitaires de messages
```

## 🔧 Configuration

Le plugin génère automatiquement plusieurs fichiers de configuration :

### config.yml
Configuration principale du plugin avec paramètres de base, intégrations et mécaniques de pêche.

### biomes.yml
Configuration des biomes avec couleurs et chances de monstres.

### drops.yml
Configuration des drops de poisson par biome avec chances, expérience et prix.

### messages.yml
Messages et traductions du plugin.

### tournaments.yml
Configuration des tournois de pêche (fonctionnalité future).

## 🎮 Commandes

- `/lfish help` - Affiche l'aide
- `/lfish info` - Informations sur le plugin
- `/lfish stats [joueur]` - Statistiques de pêche
- `/lfish biomes` - Liste des biomes configurés
- `/lfish drops [biome]` - Affiche les drops disponibles
- `/lfish sell` - Vend tous les poissons de l'inventaire
- `/lfish reload` - Recharge la configuration (admin)

## 🔑 Permissions

- `litefish.use` - Utilisation de base (défaut: true)
- `litefish.admin` - Permissions administrateur (défaut: op)
- `litefish.admin.reload` - Recharger le plugin
- `litefish.admin.stats` - Voir les stats des autres joueurs

## 🚀 Installation

1. Compilez le plugin avec Maven
2. Placez le fichier JAR dans votre dossier `plugins/`
3. Redémarrez votre serveur
4. Configurez les fichiers générés selon vos besoins

## 📦 Dépendances

### Requises
- Spigot/Paper 1.16+
- Java 8+

### Optionnelles
- Vault (pour l'économie)
- WorldGuard (pour les régions)
- Nexo (pour les items personnalisés)

## 🛠️ Développement

### Compilation
```bash
mvn clean package
```

### Structure Maven
Le projet utilise une structure Maven standard avec les dépendances configurées dans le `pom.xml`.

## 📋 TODO / Fonctionnalités Futures

- [ ] Système de tournois de pêche
- [ ] Interface graphique (GUI)
- [ ] Système de quêtes de pêche
- [ ] Achievements/Succès
- [ ] Base de données MySQL/SQLite
- [ ] API pour développeurs tiers
- [ ] Système de craft de cannes/appâts
- [ ] Mini-jeux de pêche
- [ ] Système de saisons

## 🔄 Différences avec l'Original

Cette version simplifiée se concentre sur les fonctionnalités essentielles :

- **Code plus propre** et mieux organisé
- **Architecture modulaire** avec des managers séparés
- **Support moderne** pour les dernières versions de Minecraft
- **Intégrations optionnelles** mais robustes
- **Configuration simplifiée** mais complète
- **Performance optimisée** sans fonctionnalités complexes inutiles

## 🤝 Contribution

Ce projet est basé sur le LiteFish original d'Azlagor et a été simplifié pour un usage éducatif et de développement.

## 📜 Licence

Ce projet est destiné à un usage éducatif et de développement. Respectez les licences des plugins originaux et des dépendances utilisées.
