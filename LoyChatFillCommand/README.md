# LoyChatFillCommand

A PaperMC 1.21.8 plugin that allows creating clickable messages to pre-fill players' chat bar.

## Description

This plugin allows administrators to send interactive messages to players. When a player clicks on the message, their chat bar gets pre-filled with the specified command or text.

## Installation

1. Download the plugin `.jar` file
2. Place it in your PaperMC server's `plugins/` folder
3. Restart the server
4. The plugin will load automatically

## Usage

### Command Syntax

```
/chatfill <player> "<display message>" "<command/message>" "<hover text>"
```

### Parameters

- **`<player>`** : Target player's name
- **`<display message>`** : Text displayed to the player (supports color codes with `&`) - **must be in quotes**
- **`<command/message>`** : Command or message that will be pre-filled in the player's chat - **must be in quotes**
- **`<hover text>`** : Text displayed on hover (supports color codes with `&`) - **must be in quotes**

### Usage Examples

```bash
# Simple example - Pre-fill with a command
/chatfill Steve "Click here" "/msg Admin Hello!" "Send message to admin"

/chatfill Alex "&aClick&r to PM admin" "/msg Admin I need help" "&7Send a private message to the administrator"

/chatfill Bob "&6Teleportation" "/tp spawn" "&eTeleport to spawn"

/chatfill Player "&c&lIMPORTANT MESSAGE" "/say Hello everyone in the server" "&6Click to broadcast a message"
```

## Important Notes

- **All parameters except the player name must be enclosed in double quotes (`"`)** 
- This allows you to use spaces within each parameter
- Color codes are supported in both display message and hover text using `&`

## Supported Color Codes

The plugin supports all Minecraft color codes using the `&` symbol:

- `&0` - Black
- `&1` - Dark Blue
- `&2` - Dark Green
- `&3` - Dark Aqua
- `&4` - Dark Red
- `&5` - Dark Purple
- `&6` - Gold
- `&7` - Gray
- `&8` - Dark Gray
- `&9` - Blue
- `&a` - Green
- `&b` - Aqua
- `&c` - Red
- `&d` - Light Purple
- `&e` - Yellow
- `&f` - White
- `&r` - Reset (default color)

## Permissions

- **`chatfill.use`** : Allows using the `/chatfill` command (default: OP only)

## Configuration

No additional configuration required. The plugin works immediately after installation.

## Compatibility

- **Minecraft Version** : 1.21+
- **Server** : PaperMC only (does not work on Spigot or vanilla Bukkit)
- **API** : Paper API with Adventure support

## How It Works

1. Run the command from console or as an OP player
2. The target player receives a clickable message
3. When the player clicks the message, their chat input gets pre-filled with your specified command/text
4. The player can then modify the text or press Enter to send it
