package me.maxadams98.ultimateinventory.client.litematica;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameMode;
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

        // Only handle pick block for survival mode players
        // Creative/spectator/adventure should use default behavior
        if (client.interactionManager == null || client.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // Check if the item is available in accessible slots
        boolean itemAvailable = PickBlockUtils.itemExistsInNonBlacklistedHotbarSlot(player, targetItem);

        if (!itemAvailable) {
            // Item is not available - check if we can trigger shulker search immediately

            // Check if all hotbar slots are blacklisted (no room for swapping)
            if (PickBlockUtils.areAllHotbarSlotsBlacklisted(player)) {
                PickBlockUtils.sendBlacklistedSlotsError(player);
                return;
            }

            // Send the command immediately since this is called directly by Litematica's pick block
            // Pause printer to prevent conflicts during shulker operations
            PrinterIntegration.pausePrinterForShulkerAction(20); // 1 second at 20 TPS
            PickBlockUtils.sendPickBlockCommand(player, targetItem);
        }
    }
}

