# Testing UltimateInventory Client Mod

## Build Output

The mod JAR is located at:
```
build/libs/ultimateinventory-client-1.0.0.jar
```

## Installation Steps

### 1. Install Fabric Loader

1. Download Fabric Installer from: https://fabricmc.net/use/
2. Run the installer
3. Select Minecraft version **1.21.5**
4. Install Fabric Loader
5. Create a new profile or select the Fabric profile

### 2. Install Fabric API (Required)

1. Download Fabric API for 1.21.5 from: https://modrinth.com/mod/fabric-api/version/0.128.2+1.21.5
2. Place the JAR in `.minecraft/mods/` folder

### 3. Install UltimateInventory Client Mod

1. Copy `build/libs/ultimateinventory-client-1.0.0.jar` to `.minecraft/mods/` folder
2. Make sure the server has UltimateInventory plugin installed

### 4. Launch Minecraft

1. Launch Minecraft with the Fabric profile
2. Connect to your server (must have UltimateInventory plugin installed)

## Testing in Game

### Setup

1. **Get into Creative Mode** (required for pick block)
2. **Get some shulker boxes** and place items in them
3. **Put shulker boxes in your inventory** (not in hotbar)
4. **Make sure the items in shulker boxes are NOT in your main inventory**

### Test Steps

1. **Hold Ctrl and middle-click a block** that you know is in a shulker box but not in your inventory
   - **Note:** Use Ctrl+Middle-click (not just middle-click)
   - Normal middle-click still works for vanilla pick block
2. **Expected behavior:**
   - Mod detects middle-click
   - Waits for vanilla pick block to complete
   - If vanilla fails, mod sets scoreboard values
   - Server plugin monitors scoreboard and searches shulker boxes
   - Item appears in your hotbar (swapped from shulker)

### What to Look For

✅ **Success indicators:**
- Message in chat: `[UltimateInventory] Searching shulker boxes for <MATERIAL>`
- Item appears in hotbar
- Item is removed from shulker box
- Sound plays (item pickup sound)

❌ **If it doesn't work:**
- Check server console for errors
- Check client logs (`.minecraft/logs/latest.log`)
- Verify you're in creative mode
- Verify item is actually in a shulker box
- Verify UltimateInventory plugin is loaded on server

## Troubleshooting

### Mod not loading
- Check Fabric Loader is installed correctly
- Check mod is in `.minecraft/mods/` folder
- Check client logs for errors

### Scoreboard communication not working
- Verify server has UltimateInventory plugin
- Check server console for plugin errors
- Verify scoreboard objectives are initialized (plugin should create them automatically)
- Check server logs for scoreboard monitoring messages

### Item not found
- Make sure item is actually in a shulker box
- Make sure shulker box is in your inventory (not in a chest)
- Check server logs for search results

## Quick Test Command

You can also test the plugin directly (without mod):
```
/uipickblock STONE
```

This will search your shulker boxes for stone and swap it to hotbar if found.

