package me.maxadams98.ultimateinventory.client.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

/**
 * Utility methods for pick block functionality.
 * Shared between Litematica event listener and mixin handler.
 */
public class PickBlockUtils {
    
    /**
     * Sends a pick block command to the server.
     * Converts the item to Bukkit-style material name and sends /uipickblock command.
     */
    public static void sendPickBlockCommand(ClientPlayerEntity player, ItemStack targetItem) {
        var itemId = Registries.ITEM.getId(targetItem.getItem());
        String namespace = itemId.getNamespace();
        String path = itemId.getPath();
        String materialName = path.toUpperCase().replace('/', '_');
        
        if (!namespace.equals("minecraft")) {
            materialName = namespace.toUpperCase() + "_" + materialName;
        }
        
        String command = "uipickblock " + materialName;
        player.networkHandler.sendCommand(command);
    }
    
    /**
     * Checks if an item exists in the player's inventory.
     */
    public static boolean itemExistsInInventory(ClientPlayerEntity player, ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }
        
        var itemType = item.getItem();
        var inventory = player.getInventory();
        
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack invItem = inventory.getStack(i);
            if (!invItem.isEmpty() && invItem.getItem() == itemType) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if all hotbar slots (0-8) contain blacklisted items.
     * Returns true only if all 9 slots have items AND all items are blacklisted.
     */
    public static boolean areAllHotbarSlotsBlacklisted(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getStack(i);
            // Empty slots are fine - we can use them
            if (item.isEmpty()) {
                return false;
            }
            // If any slot has a non-blacklisted item, we can use it
            if (!isBlacklistedForShulkerSwap(item.getItem())) {
                return false;
            }
        }
        // All 9 slots have items and all are blacklisted
        return true;
    }
    
    /**
     * Checks if an item is blacklisted for shulker box swaps (matches server logic).
     * Blacklisted items: tools, shulker boxes, ender chests.
     */
    public static boolean isBlacklistedForShulkerSwap(net.minecraft.item.Item item) {
        var itemId = Registries.ITEM.getId(item);
        String path = itemId.getPath();
        String name = path.toLowerCase();
        
        // Check for tools
        if (name.contains("pickaxe") || name.contains("axe") || 
            name.contains("shovel") || name.contains("hoe") || 
            name.contains("sword") || name.contains("bow") ||
            name.contains("crossbow") || name.contains("trident") ||
            name.contains("fishing_rod") || name.contains("shears")) {
            return true;
        }
        
        // Check for shulker boxes
        if (name.contains("shulker_box")) {
            return true;
        }
        
        // Check for ender chest
        if (name.equals("ender_chest")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Sends an error message to the player when all hotbar slots are blacklisted.
     */
    public static void sendBlacklistedSlotsError(ClientPlayerEntity player) {
        player.sendMessage(Text.literal("§cCannot pick block: all hotbar slots contain items that cannot be swapped"), false);
    }
}

