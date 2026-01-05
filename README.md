# Ultimate Inventory
A Spigot plugin to streamline inventory management

## Open ender chests, shulker boxes and crafting tables by right-clicking **in the inventory**
![demo1](https://user-images.githubusercontent.com/21290233/232254337-f1f93c3f-a896-418f-9473-ad58645b00f3.gif)

## Open ender chests, shulker boxes and crafting tables by right-clicking **in your hand**
![demo2](https://user-images.githubusercontent.com/21290233/232255429-a0437498-1c26-4fcd-8393-4da04e667f01.gif)

> [!NOTE]
> Bonus! If you are running PaperMC, you can also open anvils, stonecutters, grindstones, looms, smithing tables and cartography tables the same way as crafting tables.

This plugin was made for my private survival server. I've released it to the public spigot forums where others may find it useful.

## Pick Block from Shulker Boxes

When you pick a block (middle-click by default) and vanilla Minecraft fails to find it in your inventory, the plugin automatically searches your shulker boxes and places the item in your hotbar. The item swaps with the previous item in that slot if the slot wasn't empty. Works seamlessly with vanilla pick block - if the item is already in your inventory, vanilla behavior is used. If not, the plugin searches shulker boxes automatically.

**Design:** Tools and shulker boxes cannot be moved/swapped by design to protect your inventory from accidental tool/shulker loss.

**Requirements:**
- **For vanilla pick block:** Works automatically on Paper 1.21.10+ servers - no client mod required! The plugin uses Paper's native `PlayerPickBlockEvent` to detect pick block requests.
- **For Litematica integration:** If you use Litematica's pick block feature, install the [UltimateInventory Litematica Shulker Pick Mod](UILitematicaShulkerPick/) on the client side for automatic detection, or use the `/uipickblock <material>` command manually.

----

## **Caution: while every care has been taken to remove duplication bugs, some may remain.**

I have tested this plugin very thoroughly for duplication exploits and I have had no issues with it in several months on my private server. I trust it and you can safely trust it too.

That being said, please consider your plugins carefully before installing, I can only guarantee no exploits when this plugin is not installed with other inventory-manipulating plugins.

Things to avoid in other plugins:
- Things that rearrange a player's inventory *or ender chest* other than when they are normally clicking/dragging items around
- Things that let one player change another's inventory *or ender chest* while they're online (such as /invsee from Essentials)

### Compatibility list

> [!WARNING]
> If you use plugins other than the ones listed here, please take a moment to think if they fit the description - If so, they may cause duplication bugs!

- This plugin is compatible with [ChestSort](https://www.spigotmc.org/resources/chestsort-api.59773/) as of version 1.4
- This plugin should be compatible with any plugins that allow identical shulker boxes to stack - Tested with [SimpleStack 2.0 development builds](https://github.com/Mikedeejay2/SimpleStackPlugin)

If you find any duplication bugs (or bugs in general) please immediately report them [here](https://github.com/percyqaz/Shulkerbox/issues)

## Known issues:
- This plugin doesn't work in the creative inventory
- You can put your enderchest into your enderchest and lock it in there by mistake. Try not to do that :)

No other known issues

Got a feature request? Ask on the forums or open an issue

## Development

### Requirements

- **Java 17+** (JDK 17 or higher)
- **Maven 3.6+** (for building the Bukkit plugin)
- **Minecraft Server** (Paper or Spigot 1.21.3+) for testing

### Building the Plugin

1. Clone the repository:
   ```bash
   git clone https://github.com/MaxAdams98/UltimateInventory.git
   cd UltimateInventory
   ```

2. Build the plugin (choose one method):

   **Option A: Build and install to server automatically**
   ```bash
   ./build-and-install.sh /path/to/your/server/plugins
   ```
   
   Or set an environment variable for convenience:
   ```bash
   export SERVER_PLUGINS_PATH=/path/to/your/server/plugins
   ./build-and-install.sh
   ```

   **Option B: Build manually**
   ```bash
   mvn clean package
   ```
   The compiled JAR will be in `target/UltimateInventory-1.7.1.jar`
   
   Then manually copy it to your server's `plugins/` folder

### Building the Client Mod

The client mod is located in the `UILitematicaShulkerPick/` directory. See [UILitematicaShulkerPick/README.md](UILitematicaShulkerPick/README.md) for build instructions.

**Requirements for Client Mod:**
- **Java 21+** (JDK 21 or higher)
- **Gradle** (included via Gradle Wrapper)
- **Fabric Loader** and **Fabric API** for testing
