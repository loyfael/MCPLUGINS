# MCPLUGINS — Nuvalis Workshop

This repository contains **all the work I made for the Minecraft server Nuvalis** (Skyland mode, **1.21.8**), as a monorepo that groups multiple plugins and tools.

> [!WARNING]
> **DISCONTINUED**
> All my Minecraft plugins are discontinued and no longer maintained. I share them as open-source gifts to the community, but I won't provide support or updates. If you need help or want to maintain them, feel free to fork the repository. I may occasionally merge community contributions, but I won't actively work on these projects anymore.

## Repository structure

Each module is independent and typically has its own `pom.xml`.

- Plugins are grouped by folder at the repository root (one folder per plugin).
- Each plugin usually has its own `src/main/resources/plugin.yml` (Paper/Spigot descriptor).
- Most modules also have their own README.

Quick links: [AetherialShop](AetherialShop/), [AntiVillagerLag](AntiVillagerLag/), [KrakenLevels2](KrakenLevels2/), [LoyChatFillCommand](LoyChatFillCommand/), [LoyCustomMobs](LoyCustomMobs/), [LoyFishing](LoyFishing/), [LoyFlightImproved](LoyFlightImproved/), [LandBlockLimiter](LandBlockLimiter/), [LoyCompassMenu](LoyCompassMenu/), [LoyLecternFirstPage](LoyLecternFirstPage/), [NuvaPlayerSync](NuvaPlayerSync/).

## What is the purpose of each plugin?

If you need details (commands, permissions, configuration), open the module README.

- **AetherialShop** ([AetherialShop](AetherialShop/)): Temporary rotating **admin shop** (daily rotation). Provides `/dailyshop` for players and `/ashop` admin commands. (Depends on Vault)
- **AntiVillagerLag** ([AntiVillagerLag](AntiVillagerLag/)): Villager performance helper that **toggles villager behavior/trading based on simple rules** (notably: villagers on an **emerald block** can trade; otherwise they get disabled), with commands to optimize/unoptimize and options to prevent **Mending** trades.
- **KrakenLevels2 / KrakenLevels** ([KrakenLevels2](KrakenLevels2/)): Lightweight **levels + missions** system for modern Paper servers, with MongoDB (or YAML) storage, PlaceholderAPI placeholders, and optional Vault integration.
- **LoyChatFillCommand** ([LoyChatFillCommand](LoyChatFillCommand/)): Sends **clickable messages** that pre-fill the player's chat input with a command/text (Paper 1.21.8).
- **LoyCustomMobs** ([LoyCustomMobs](LoyCustomMobs/)): Custom mobs framework with **abilities**, **rarity tiers**, and configurable **loot**, designed for Paper 1.21.x.
- **NuvaPlayerSynchro** ([NuvaPlayerSync](NuvaPlayerSync/)): MongoDB-based **player data synchronization** plugin (inventory/enderchest/XP/health/hunger…), aimed at high-concurrency setups. (Do not use in production; read the module README.)
- **LoyFishing / LiteFish** ([LoyFishing](LoyFishing/)): Simplified advanced **fishing** plugin: configurable drops (by biome), player stats, and optional hooks for Vault (economy), WorldGuard (regions), and Nexo (custom items).
- **LoyFlightImproved** ([LoyFlightImproved](LoyFlightImproved/)): Elytra boost plugin: **right-click to boost** elytras; consumes hunger per boost.
- **LandsBlockLimiter** ([LandBlockLimiter](LandBlockLimiter/)): Adds per-land limits for configured block types when using the **Lands** plugin (e.g., limit hoppers/chests/beacons). Includes `/lbl reload` and `/lbl scan`.
- **LoyCompassMenu** ([LoyCompassMenu](LoyCompassMenu/)): Quality-of-life shortcut: **right-click with a compass** to execute `/menu`.
- **LoyLecternFirstPage** ([LoyLecternFirstPage](LoyLecternFirstPage/)): Ensures lectern books open on **page 1** (prevents “random page” behavior).

Not production plugins (templates / examples):

- **KotlinPaperMCPluginBoilerPlate** ([KotlinPaperMCPluginBoilerPlate](KotlinPaperMCPluginBoilerPlate/)): Production-ready **Kotlin Paper plugin template** (config/db/commands boilerplate).
- **PaperMC-Plugin-Boilerplate** ([PaperMC-Plugin-Boilerplate](PaperMC-Plugin-Boilerplate/)): Modern **Java Paper plugin template** (Minecraft 1.21.7+).
- **TriumphGUI-Kotlin-Tutorial** ([TriumphGUI-Kotlin-Tutorial](TriumphGUI-Kotlin-Tutorial/)): Kotlin example project demonstrating **Triumph-GUI** usage.

## Development / Build

### Plugins Java/Kotlin (Maven)

Inside the plugin directory:

- Tests: `mvn -q test`
- Build: `mvn clean package`

Jars are usually generated in `target/`.

## Terms of use (credit + non-commercial)

You can copy, use, modify, and redistribute my work **only** if:

1) **Mandatory credit (attribution)**
   - You must credit me in a visible way and preserve author/copyright notices.
   - You must keep the `NOTICE` file in any redistribution (source and/or binary) and any derivative work.

   Minimum expected attribution:
   > “Portions © 2026 ldupasquier (Nuvalis / MCPLUGINS) — credit required.”

2) **No commercial use**
   - You must not use any part of this repository in a **commercial or otherwise remunerated** context, including (non-exhaustive): selling, direct monetization, paid access, sold packs, paid services “based on” this code, integration into a sold product/service.
   - If you are unsure, assume it is commercial and contact me for explicit permission.

3) **The work must stay source-available (public code)**
   - Any redistribution (or derivative work) must keep its source code publicly available.

## License

This repository is licensed under: **Nuvalis Non-Commercial Open Source License (NN-COSL) v1.0** (see `LICENSE`).

Note: “open source” here means **public source code / source-available**. A “non-commercial” clause is not compatible with the OSI definition of Open Source, but it matches the intended rule: free use as long as **credit is given** and **no commercial use**.

### Third-party licenses (important)

Some modules include third-party code (or are derived from templates) and may contain their own `LICENSE` / `LICENSE.txt` files.
When a module includes its own license file, that license applies to that module's contents.
