package me.Percyqaz.UltimateInventory;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PickBlockCommand implements CommandExecutor {
    private final UltimateInventory plugin;
    private final InventoryListener inventoryListener;

    public PickBlockCommand(UltimateInventory plugin, InventoryListener inventoryListener) {
        this.plugin = plugin;
        this.inventoryListener = inventoryListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("§cUsage: /uipickblock <material>");
            return true;
        }

        // Parse material from argument
        Material blockType;
        try {
            blockType = Material.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[PickBlock] Invalid material: " + args[0]);
            player.sendMessage("§cInvalid material: " + args[0]);
            return true;
        }

        // Delegate to InventoryListener to handle the pick block search and swap
        inventoryListener.handlePickBlockRequest(player, blockType);
        
        return true;
    }
}

