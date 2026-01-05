package me.maxadams98.ultimateinventory.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import me.maxadams98.ultimateinventory.client.litematica.LitematicaIntegration;
import me.maxadams98.ultimateinventory.client.util.PickBlockUtils;

/**
 * Ultimate Inventory - Litematica Support Mod
 *
 * This mod provides Litematica pick block support for UltimateInventory plugin.
 * Vanilla pick block is handled server-side by the plugin, so this mod only
 * handles Litematica-specific pick block detection.
 *
 * Also includes safety integration with Litematica Printer mod to prevent
 * conflicts during shulker box operations.
 */
public class UltimateInventoryClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Reset server compatibility check for initial connection
        PickBlockUtils.resetServerCompatibility();

        // Register Litematica pick block listener
        // This will only succeed if Litematica is present
        LitematicaIntegration.register();

        // Register client tick handler for printer integration
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PrinterIntegration.onClientTick();
        });

        System.out.println("[UltimateInventory] Litematica support mod initialized with printer safety integration");
    }
}

