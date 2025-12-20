package me.percyqaz.ultimateinventory.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.GameMode;

public class UltimateInventoryClient implements ClientModInitializer {
    
    private static boolean wasVanillaPickBlockPressed = false;
    private static ItemStack lastTargetItem = null;
    private static int checkTicksRemaining = 0;
    
    @Override
    public void onInitializeClient() {
        // Monitor vanilla pick block key binding instead of creating our own
        // This allows vanilla to handle it first, then we check if it succeeded
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }
    
    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            wasVanillaPickBlockPressed = false;
            checkTicksRemaining = 0;
            return;
        }
        
        ClientPlayerEntity player = client.player;
        
        GameMode currentMode = client.interactionManager.getCurrentGameMode();
        if (currentMode != GameMode.CREATIVE && currentMode != GameMode.SURVIVAL) {
            wasVanillaPickBlockPressed = false;
            checkTicksRemaining = 0;
            return;
        }
        
        // Monitor vanilla's pick block key binding
        KeyBinding vanillaPickBlock = client.options.pickItemKey;
        boolean isVanillaPickBlockPressed = vanillaPickBlock != null && vanillaPickBlock.isPressed();
        
        // When vanilla pick block is pressed, capture the target block
        if (isVanillaPickBlockPressed && !wasVanillaPickBlockPressed) {
            HitResult hitResult = client.crosshairTarget;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK && client.world != null) {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                var blockState = client.world.getBlockState(blockHit.getBlockPos());
                var block = blockState.getBlock();
                ItemStack itemStack = block.asItem().getDefaultStack();
                
                if (!itemStack.isEmpty()) {
                    // Store the target item and wait a few ticks to see if vanilla succeeded
                    lastTargetItem = itemStack.copy();
                    checkTicksRemaining = 3; // Check after 3 ticks to see if vanilla picked it
                }
            }
        }
        
        // Check if vanilla pick block succeeded
        if (checkTicksRemaining > 0) {
            checkTicksRemaining--;
            
            if (checkTicksRemaining == 0 && lastTargetItem != null) {
                // Check if the item appeared in hotbar (vanilla succeeded)
                boolean vanillaSucceeded = false;
                for (int i = 0; i < 9; i++) {
                    ItemStack hotbarItem = player.getInventory().getStack(i);
                    if (!hotbarItem.isEmpty() && hotbarItem.getItem() == lastTargetItem.getItem()) {
                        vanillaSucceeded = true;
                        break;
                    }
                }
                
                // If vanilla didn't succeed, search shulker boxes
                if (!vanillaSucceeded) {
                    handlePickBlock(client, player, lastTargetItem);
                }
                
                lastTargetItem = null;
            }
        }
        
        wasVanillaPickBlockPressed = isVanillaPickBlockPressed;
    }
    
    private void handlePickBlock(MinecraftClient client, ClientPlayerEntity player, ItemStack targetItem) {
        // Get the material/registry name
        var itemId = Registries.ITEM.getId(targetItem.getItem());
        String namespace = itemId.getNamespace();
        String path = itemId.getPath();
        
        // Convert to Bukkit-style material name
        String materialName = path.toUpperCase().replace('/', '_');
        
        // For modded items, use namespace:path format
        if (!namespace.equals("minecraft")) {
            materialName = namespace.toUpperCase() + "_" + materialName;
        }
        
        // Get player UUID (without dashes, as expected by plugin)
        String playerUUID = player.getUuid().toString().replace("-", "");
        String playerName = player.getName().getString();
        
        // Use scoreboard communication instead of custom command
        // This is more efficient and less prone to abuse
        // Format: "material:<UUID>:<MATERIAL>" (most reliable for multi-player)
        String materialEntry = "material:" + playerUUID + ":" + materialName;
        
        // Set the material name in scoreboard
        String materialCommand = String.format("scoreboard players set \"%s\" ui_pickblock_material_name 1", materialEntry);
        player.networkHandler.sendCommand(materialCommand);
        
        // Set the trigger to activate the plugin's scoreboard monitor
        String triggerCommand = String.format("scoreboard players set %s ui_pickblock_trigger 1", playerName);
        player.networkHandler.sendCommand(triggerCommand);
    }
}

