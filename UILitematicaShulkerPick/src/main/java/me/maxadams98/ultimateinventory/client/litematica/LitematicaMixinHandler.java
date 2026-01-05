package me.maxadams98.ultimateinventory.client.litematica;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import me.maxadams98.ultimateinventory.client.util.PickBlockUtils;
import me.maxadams98.ultimateinventory.client.PrinterIntegration;

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
        
        // Check if we should trigger a shulker search (includes delay to prevent false positives during item usage)
        if (PickBlockUtils.shouldTriggerShulkerSearch(player, targetItem)) {
            // Item has been missing long enough to warrant a search

            // Check if all hotbar slots are blacklisted (no room for swapping)
            if (PickBlockUtils.areAllHotbarSlotsBlacklisted(player)) {
                System.out.println("[UltimateInventory] All hotbar slots blacklisted - cannot swap");
                PickBlockUtils.sendBlacklistedSlotsError(player);
                return;
            }

            // Check if item exists anywhere in inventory (including blacklisted slots)
            boolean itemExistsAnywhere = PickBlockUtils.itemExistsInInventory(player, targetItem);
            if (itemExistsAnywhere) {
                System.out.println("[UltimateInventory] Item exists but only in blacklisted/main inventory slots - triggering shulker search");
            } else {
                System.out.println("[UltimateInventory] Item not found anywhere in inventory - triggering shulker search");
            }

            // Trigger our shulker box search
            System.out.println("[UltimateInventory] Triggering shulker box search for: " + targetItem.getItem().toString());
            // Pause printer to prevent conflicts during shulker operations
            PrinterIntegration.pausePrinterForShulkerAction(20); // 1 second at 20 TPS
            PickBlockUtils.sendPickBlockCommand(player, targetItem);
        }
    }
}

