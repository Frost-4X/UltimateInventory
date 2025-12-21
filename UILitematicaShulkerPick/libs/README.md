# Development Dependencies

This folder is for development dependencies needed at compile time but not bundled with the mod.

## Litematica

To build this mod, you need a Litematica JAR file for Mixin target resolution.

**Option 1 (Recommended for Development):**
1. Copy `litematica-fabric-*.jar` into this `libs/` folder
2. The build script will automatically find and use it

**Option 2 (From Source):**
1. Build Litematica from source: `cd ../../Mods/litematica-1.21.5-0.22.1 && ./gradlew build`
2. Copy the JAR from `build/libs/` to this `libs/` folder

**Option 3 (Runtime Scenario):**
- If Litematica is in the same mods folder as this mod, the build script will find it automatically

The JAR file should match the pattern: `litematica-fabric-*.jar` (excluding `-sources` and `-dev` variants).

