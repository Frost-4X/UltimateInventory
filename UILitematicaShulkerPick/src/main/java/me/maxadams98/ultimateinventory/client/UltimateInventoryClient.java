package me.maxadams98.ultimateinventory.client;

import net.fabricmc.api.ClientModInitializer;
import me.maxadams98.ultimateinventory.client.litematica.LitematicaIntegration;

/**
 * Ultimate Inventory - Litematica Support Mod
 * 
 * This mod provides Litematica pick block support for UltimateInventory plugin.
 * Vanilla pick block is handled server-side by the plugin, so this mod only
 * handles Litematica-specific pick block detection.
 */
public class UltimateInventoryClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // Register Litematica pick block listener
        // This will only succeed if Litematica is present
        LitematicaIntegration.register();
        
        System.out.println("[UltimateInventory] Litematica support mod initialized");
    }
}

