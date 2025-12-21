package me.maxadams98.ultimateinventory.client.litematica;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import me.maxadams98.ultimateinventory.client.util.PickBlockUtils;

/**
 * Handler for Litematica pick block via Mixin (fallback for older Litematica versions).
 * Checks if Litematica's pick block succeeded and triggers shulker box search if not.
 */
public class LitematicaMixinHandler {
    
    public static void checkPickBlockResult(MinecraftClient client, ItemStack targetItem) {
        if (client.player == null || targetItem.isEmpty()) {
            return;
        }
        
        ClientPlayerEntity player = client.player;
        
        System.out.println("[UltimateInventory] Checking Litematica pick block result for: " + targetItem.getItem().toString());
        
        // Check if the item actually ended up in the player's inventory
        boolean itemFound = PickBlockUtils.itemExistsInInventory(player, targetItem);
        System.out.println("[UltimateInventory] Item found in inventory: " + itemFound);
        
        if (!itemFound) {
            System.out.println("[UltimateInventory] Litematica pick block failed - item not in inventory");
            
            // Check if all hotbar slots are blacklisted
            if (PickBlockUtils.areAllHotbarSlotsBlacklisted(player)) {
                System.out.println("[UltimateInventory] All hotbar slots blacklisted");
                PickBlockUtils.sendBlacklistedSlotsError(player);
                return;
            }
            
            // Trigger our shulker box search
            System.out.println("[UltimateInventory] Triggering shulker box search for: " + targetItem.getItem().toString());
            PickBlockUtils.sendPickBlockCommand(player, targetItem);
        } else {
            System.out.println("[UltimateInventory] Item was successfully picked by Litematica, no action needed");
        }
    }
}

