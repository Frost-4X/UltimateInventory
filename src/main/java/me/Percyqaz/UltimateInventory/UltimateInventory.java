package me.Percyqaz.UltimateInventory;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class UltimateInventory extends JavaPlugin {

    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        try {
            boolean isPaper = false;
            try
            {
                Class.forName("com.destroystokyo.paper.utils.PaperPluginLogger");
                isPaper = true;
                this.getLogger().info("You are running PaperMC, some extra features are enabled");
            }
            catch (ClassNotFoundException e)
            {
                //https://www.spigotmc.org/threads/quick-question-about-posting-resources.394544/#post-3543896
                this.getLogger().info("You are not running PaperMC");
            }

            PluginManager pm = getServer().getPluginManager();

            addItemConfig("shulkerbox", true, false, "");
            addItemConfig("enderChest", true, false, "");
            addItemConfig("craftingTable", true, false, "");
            if (isPaper) {
                addItemConfig("smithingTable", true, false, "");
                addItemConfig("stoneCutter", true, false, "");
                addItemConfig("grindstone", true, false, "");
                addItemConfig("cartographyTable", true, false, "");
                addItemConfig("loom", true, false, "");
                addItemConfig("anvil", false, false, "");
            }
            config.addDefault("usePermissions", false);
            config.addDefault("pickBlock.enable", true);
            config.addDefault("pickBlock.requireCreative", false);

            config.options().copyDefaults(true);
            saveConfig();

            boolean isAdvancedEnderchestPresent = pm.getPlugin("AdvancedEnderchest") != null;
            if (isAdvancedEnderchestPresent) {
                this.getLogger().info("AdvancedEnderchest plugin found, enabling related features.");
            }

            InventoryListener inventoryListener = new InventoryListener(this, config, isPaper, isAdvancedEnderchestPresent);
            pm.registerEvents(inventoryListener, this);

            // Register pick block command for client mod communication
            PickBlockCommand pickBlockCommand = new PickBlockCommand(this, inventoryListener);
            org.bukkit.command.PluginCommand cmd = this.getCommand("uipickblock");
            if (cmd != null) {
                cmd.setExecutor(pickBlockCommand);
                cmd.setPermission(null);
                this.getLogger().info("Registered /uipickblock command");
            } else {
                this.getLogger().severe("Failed to register /uipickblock command! Check plugin.yml");
            }

            this.getLogger().info("UltimateInventory has been enabled!");
            this.getLogger().info("Pick Block feature: " + (config.getBoolean("pickBlock.enable", true) ? "ENABLED" : "DISABLED"));
        } catch (Exception e) {
            this.getLogger().severe("CRITICAL ERROR in onEnable()!");
            this.getLogger().severe("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addItemConfig(String itemName, boolean enable, boolean override, String command) {
        config.addDefault(itemName + ".enable", enable);
        config.addDefault(itemName + ".override", override);
        config.addDefault(itemName + ".command", command);
    }
}
