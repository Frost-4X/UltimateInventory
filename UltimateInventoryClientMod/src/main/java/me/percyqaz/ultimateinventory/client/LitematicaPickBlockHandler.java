package me.percyqaz.ultimateinventory.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

/**
 * Handler for Litematica pick block (for versions without event system).
 * Checks if Litematica's pick block succeeded and triggers shulker box search if not.
 */
public class LitematicaPickBlockHandler {
    
    public static void checkPickBlockResult(MinecraftClient client, ItemStack targetItem) {
        if (client.player == null || targetItem.isEmpty()) {
            return;
        }
        
        ClientPlayerEntity player = client.player;
        
        System.out.println("[UltimateInventory] Checking Litematica pick block result for: " + targetItem.getItem().toString());
        
        // Check if the item actually ended up in the player's inventory
        boolean itemFound = itemExistsInInventory(player, targetItem);
        System.out.println("[UltimateInventory] Item found in inventory: " + itemFound);
        
        if (!itemFound) {
            System.out.println("[UltimateInventory] Litematica pick block failed - item not in inventory");
            
            // Check if all hotbar slots are blacklisted
            if (areAllHotbarSlotsBlacklisted(player)) {
                System.out.println("[UltimateInventory] All hotbar slots blacklisted");
                player.sendMessage(Text.literal("§cCannot pick block: all hotbar slots contain items that cannot be swapped"), false);
                return;
            }
            
            // Trigger our shulker box search
            System.out.println("[UltimateInventory] Triggering shulker box search for: " + targetItem.getItem().toString());
            handlePickBlock(client, player, targetItem);
        } else {
            System.out.println("[UltimateInventory] Item was successfully picked by Litematica, no action needed");
        }
    }
    
    private static void handlePickBlock(MinecraftClient client, ClientPlayerEntity player, ItemStack targetItem) {
        // Get the material/registry name
        var itemId = Registries.ITEM.getId(targetItem.getItem());
        String namespace = itemId.getNamespace();
        String path = itemId.getPath();
        String materialName = path.toUpperCase().replace('/', '_');
        
        if (!namespace.equals("minecraft")) {
            materialName = namespace.toUpperCase() + "_" + materialName;
        }
        
        // Litematica pick block failed - search shulker boxes
        String command = "uipickblock " + materialName;
        player.networkHandler.sendCommand(command);
    }
    
    private static boolean itemExistsInInventory(ClientPlayerEntity player, ItemStack item) {
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
    
    private static boolean areAllHotbarSlotsBlacklisted(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getStack(i);
            if (item.isEmpty()) {
                return false;
            }
            if (!isBlacklistedForShulkerSwap(item.getItem())) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean isBlacklistedForShulkerSwap(net.minecraft.item.Item item) {
        var itemId = Registries.ITEM.getId(item);
        String path = itemId.getPath();
        String name = path.toLowerCase();
        
        if (name.contains("pickaxe") || name.contains("axe") || 
            name.contains("shovel") || name.contains("hoe") || 
            name.contains("sword") || name.contains("bow") ||
            name.contains("crossbow") || name.contains("trident") ||
            name.contains("fishing_rod") || name.contains("shears")) {
            return true;
        }
        
        if (name.contains("shulker_box")) {
            return true;
        }
        
        if (name.equals("ender_chest")) {
            return true;
        }
        
        return false;
    }
}

