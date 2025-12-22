# UltimateInventory Client Mod

Client-side mod for the UltimateInventory plugin. When you pick a block (middle-click by default) and vanilla Minecraft fails to find it in your inventory, this mod sends a request to the server plugin which searches your shulker boxes and places the item in your hotbar.

## Features

- Automatically detects when you pick a block and vanilla pick block fails
- Sends requests to the UltimateInventory plugin to search shulker boxes
- Works in survival and creative modes
- Works with any block - no configuration needed
- Completely seamless - vanilla pick block behavior is preserved

## Requirements

- Minecraft 1.21.5
- Fabric Loader 0.16.9+
- Fabric API (recommended)
- [UltimateInventory plugin](https://github.com/MaxAdams98/UltimateInventory) on the server

## Installation

1. Install Fabric Loader for Minecraft 1.21.5
2. Install Fabric API (optional but recommended)
3. Place this mod in your `.minecraft/mods/` folder
4. Connect to a server with UltimateInventory plugin installed

## How It Works

1. When you **pick a block** (middle-click by default), vanilla Minecraft's pick block function applies first
2. If vanilla fails to find the item in your inventory, this mod kicks in and sends a request to the UltimateInventory plugin
3. The plugin searches your shulker boxes for the picked block
4. If found, the plugin places the item in your hotbar, swapping with the previous item in that slot if it wasn't empty
5. Tools and shulker boxes cannot be moved/swapped by design to protect your inventory

**Note:** Normal pick block still works exactly as before. The mod only activates when vanilla pick block fails to find the item in your inventory.

## License

MIT License