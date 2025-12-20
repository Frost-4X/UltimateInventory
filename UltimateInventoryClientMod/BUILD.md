# Building UltimateInventory Client Mod

## Prerequisites

- Java 21 or higher
- Gradle (included via wrapper)

## Building

```bash
./gradlew build
```

The built mod JAR will be in `build/libs/ultimateinventory-client-1.0.0.jar`

## Development Setup

1. **IntelliJ IDEA:**
   - Open the project folder
   - Gradle will sync automatically
   - Run `genSources` task to generate Minecraft sources
   - Run `runClient` to test in development environment

2. **Eclipse:**
   - Import as Gradle project
   - Run `genSources` task
   - Run `runClient` task

## Testing

1. Build the mod: `./gradlew build`
2. Copy the JAR from `build/libs/` to your `.minecraft/mods/` folder
3. Install Fabric Loader for Minecraft 1.21.5
4. Install Fabric API (recommended)
5. Connect to a server with UltimateInventory plugin installed
6. Test in creative mode by middle-clicking blocks

## How It Works

The mod:
1. Detects middle-click (pick block key) in creative mode
2. Gets the block the player is looking at
3. Waits for vanilla pick block to complete
4. If vanilla pick block fails, sets scoreboard values to communicate with server
5. Server plugin monitors scoreboard and searches shulker boxes
6. Server plugin swaps item from shulker box to hotbar

## Troubleshooting

**Mod not loading:**
- Check Fabric Loader version matches (0.16.9+)
- Check Minecraft version (1.21.5)
- Check Java version (21+)

**Scoreboard communication not working:**
- Make sure UltimateInventory plugin is installed on server
- Check server console for errors
- Verify scoreboard objectives are initialized (plugin creates them automatically)

**Material names not matching:**
- The mod converts Minecraft registry IDs to Bukkit-style names
- Example: `minecraft:stone` → `STONE`
- If issues occur, check server logs for the exact material name being sent

