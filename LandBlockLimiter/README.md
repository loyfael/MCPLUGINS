# LandsBlockLimiter

A plugin to limit the number of certain blocks per land using Lands.

## Installation

1. Download `LandsBlockLimiter.jar`.
2. Put it in your server's `plugins/` folder (PaperMC). (LANDS NEEDED !)
3. Restart your server.

## Quick Configuration

The `config.yml` file is generated automatically. Example:

```yaml
blocks:
  - "HOPPER:40"
  - "CHEST:100"
  - "BEACON:5"
```

Edit the limits as you want. Use Minecraft block names (e.g. HOPPER, CHEST).

## Main Commands

- `/lbl reload` : reload the config
- `/lbl scan <landId>` : rescan a land

## Permissions

- `landsblocklimiter.reload` : reload config
- `landsblocklimiter.admin` : admin commands

---
Plugin by Loyfael. Requires Lands and PaperMC 1.21+.
