# UltimateInventory Client Mod

Client-side mod for the UltimateInventory plugin. Detects middle-click pick block and searches shulker boxes when the item isn't in your inventory.

## Features

- Detects middle-click pick block in creative mode
- Gets the exact block/material being picked
- Automatically communicates with server via scoreboard when item not found in inventory
- Works with any block - no configuration needed
- Uses efficient scoreboard communication (no command spam)

## Requirements

- Minecraft 1.21.5
- Fabric Loader 0.16.9+
- Fabric API (recommended)
- UltimateInventory plugin on the server

## Installation

1. Install Fabric Loader for Minecraft 1.21.5
2. Install Fabric API (optional but recommended)
3. Place this mod in your `.minecraft/mods/` folder
4. Connect to a server with UltimateInventory plugin installed

## How It Works

1. Player presses **middle-click** on a block in creative mode
2. Mod waits for vanilla pick block to complete
3. If vanilla pick block fails (item not in inventory), mod sets scoreboard values
4. Server plugin monitors scoreboard and searches shulker boxes
5. Server plugin swaps item from shulker box to hotbar

**Note:** Normal middle-click still works for vanilla pick block. The mod only activates when vanilla pick block fails.

## Building

```bash
./gradlew build
```

The mod JAR will be in `build/libs/`

## Development

This mod uses Fabric Loom for development. Import the project into IntelliJ IDEA or Eclipse.

## License

MIT License

