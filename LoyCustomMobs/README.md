# LoyCustomMobs

A modern Paper plugin for Minecraft 1.21.7 that adds custom mobs with unique abilities and advanced behaviors.

## 📋 Description

LoyCustomMobs is a Minecraft plugin designed to enhance gameplay by adding custom mobs with special abilities, a rarity system, and advanced loot mechanics. The plugin is optimized for performance with an intelligent cache system and asynchronous processing.

## ✨ Features

### 🎯 Mob Abilities
The plugin now ships with more than 25 unique abilities that can be mixed and matched to create wildly different encounters. Voici la liste complète des effets disponibles :

- **arrow_homing** (« Homing Arrows ») – Les flèches tirées par ce mob poursuivent automatiquement leur cible.
- **explosive** (« Explosive ») – Déclenche une explosion et repousse les joueurs quand il est blessé.
- **poisonous** (« Poisonous ») – Inflige un poison de courte durée aux victimes de ses attaques.
- **teleport** (« Teleporter ») – Se téléporte immédiatement derrière sa cible lorsqu'il la repère.
- **withering** (« Withering ») – Applique l'effet Wither à chaque coup porté.
- **blinding** (« Blinding ») – Aveugle les attaquants quand il subit des dégâts.
- **necromancer** (« Necromancer ») – Invoque 2 à 3 sbires morts-vivants lorsque sa vie est basse.
- **sapper** (« Sapper ») – Vole de la vie à sa cible et se soigne en conséquence.
- **confusing** (« Confusing ») – Inflige nausée, lenteur et fatigue aux assaillants.
- **ghastly** (« Ghastly ») – Projette des boules de feu façon Ghast vers sa cible.
- **tosser** (« Tosser ») – Projette violemment les attaquants dans les airs.
- **gravity** (« Gravity ») – Fait léviter les joueurs qu'il touche s'ils sont au sol.
- **ender** (« Ender ») – Se téléporte près de sa cible avec un placement intelligent.
- **freezing** (« Freezing ») – Fige sa cible avec une forte lenteur et des blocs de glace.
- **magnetic** (« Magnetic ») – Attire périodiquement les joueurs proches vers lui.
- **swapper** (« Swapper ») – Échange instantanément sa position avec celle de la cible.
- **prisoner** (« Prisoner ») – Piège l'ennemi dans des toiles et de lourds malus de mobilité.
- **electric** (« Electric ») – Frappe la cible d'un éclair et enchaîne les joueurs voisins.
- **vampiric** (« Vampirisme ») – Draine la vitalité pour se soigner et inflige Wither/Faim.
- **storm_caller** (« Appel de tempête ») – Appelle un éclair sur une nouvelle cible et la ralentit.
- **bulwark** (« Rempart ») – Erige un bouclier, gagne de la résistance et repousse l'assaillant.
- **toxic_cloud** (« Nuage toxique ») – Libère un nuage empoisonné persistant à sa mort.
- **shadowstep** (« Ombre furtive ») – Se téléporte derrière la cible pour la ralentir et la blesser.
- **seismic_slam** (« Onde sismique ») – Déclenche une onde de choc qui repousse et affaiblit autour de lui.
- **sandstorm** (« Tempête de sable ») – Soulève une tempête qui aveugle et ralentit les joueurs proches.
- **frenzy** (« Frénésie ») – Entre en rage à faible vie et gagne force, vitesse et régénération.
- **thorn_burst** (« Explosion d'épines ») – Explose en épines à sa mort, infligeant poison et ralentissement.
- **starlight_veil** (« Voile stellaire ») – Apparaît avec absorption, résistance et un halo lumineux.
- **abyssal_chain** (« Chaînes abyssales ») – Lance des chaînes spectrales qui attirent la cible et l'affaiblissent.
- **umbral_shroud** (« Voile ombral ») – Plonge la victime dans l'obscurité et la fait planer doucement.

Full ability toggles and tuning controls are available in `config.yml` so you can tailor the roster to your server's balance.

### 🏆 Rarity System
6 rarity levels with progressive statistics:

- **COMMON** - 70% spawn chance, 1 life, 1 max ability
- **UNCOMMON** - 50% chance, 2 lives, 2 max abilities
- **RARE** - 30% chance, 3 lives, 3 max abilities
- **EPIC** - 15% chance, 5 lives, 4 max abilities
- **LEGENDARY** - 5% chance, 8 lives, 5 max abilities
- **MYTHIC** - 1% chance, 12 lives, 6 max abilities

### ⚡ Performance Optimizations
- **Intelligent cache** with automatic memory management
- **Asynchronous processing** for heavy operations
- **Configurable limits** to prevent overload
- **Integrated performance monitoring**
- **bStats metrics** for usage tracking

### 🎁 Loot System
- Flexible reward configuration
- Rarity-based loot
- Custom item support
- Configurable drop chances

## 🛠️ Installation

1. **Requirements**:
   - Minecraft Server 1.21.7
   - Paper or compatible fork
   - Java 21 or higher

2. **Installation**:
   - Download the `LoyCustomMobs.jar` file
   - Place it in your server's `plugins/` folder
   - Restart the server
   - Configure the plugin according to your needs

## ⚙️ Configuration

### Main Configuration (`config.yml`)

```yaml
# Global plugin settings
plugin:
  debug: false
  metrics: true
  auto-update-check: false
  language: "en"

# Performance optimizations
performance:
  cache:
    enabled: true
    max-size: 500
    expire-time: 180000 # 3 minutes
    cleanup-interval: 120 # seconds
  
  async:
    core-threads: 1
    max-threads: 2
    queue-size: 50
  
  limits:
    max-mobs-per-world: 20
    max-mobs-per-chunk: 2
```

### Loot Configuration (`loot.yml`)
Configure the rewards that mobs can drop based on their rarity and abilities.

## 🎮 Commands

### Main Command
- **Command**: `/loycustommobs` or `/lcm` or `/custommobs`
- **Permission**: `loycustommobs.admin`

### Subcommands
- `/lcm help` - Display help
- `/lcm reload` - Reload configuration
- `/lcm spawn <mob> [world] [x] [y] [z]` - Spawn a custom mob
- `/lcm remove [radius]` - Remove custom mobs
- `/lcm list` - List available mob types
- `/lcm stats` - Display plugin statistics

## 🔐 Permissions

- `loycustommobs.admin` - Full administrative access (default: op)
- `loycustommobs.use` - Basic usage permission (default: true)

## 📊 Metrics and Monitoring

The plugin includes:
- **bStats metrics** for anonymous usage tracking
- **Real-time performance monitoring**
- **Cache statistics** for optimization
- **Detailed performance reports**

## 🏗️ Project Structure

```
src/main/java/loyfael/
├── LoyCustomMobs.java          # Main plugin class
├── abilities/                  # Mob abilities
├── api/                       # Public API
├── commands/                  # Command handler
├── events/                    # Custom events
├── listeners/                 # Event listeners
├── managers/                  # Managers (Config, Mobs, Loot, GUI)
├── models/                    # Data models
└── utils/                     # Utilities (Cache, Metrics, Monitoring)
```

## 🤝 Contributing

Contributions are welcome! To contribute:

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 🐛 Bug Reports

If you find a bug, please:
1. Check if it hasn't already been reported
2. Provide detailed information:
   - Plugin version
   - Paper/Spigot version
   - Complete error message
   - Steps to reproduce the bug
