package me.percyqaz.ultimateinventory.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
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
                
                // If vanilla didn't succeed, check if we can swap before searching shulker boxes
                if (!vanillaSucceeded) {
                    // Check if all hotbar slots are blacklisted - if so, abort immediately
                    if (areAllHotbarSlotsBlacklisted(player)) {
                        // Don't send command - all slots are blacklisted
                        // Notify the player
                        player.sendMessage(Text.literal("§cCannot pick block: all hotbar slots contain items that cannot be swapped"), false);
                        return;
                    }
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
        
        // Vanilla pick block failed - search shulker boxes
        // Server plugin will send feedback messages (found/not found)
        String command = "uipickblock " + materialName;
        player.networkHandler.sendCommand(command);
    }
    
    // Check if all hotbar slots (0-8) contain blacklisted items
    // Returns true only if all 9 slots have items AND all items are blacklisted
    private boolean areAllHotbarSlotsBlacklisted(ClientPlayerEntity player) {
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
    
    // Check if an item is blacklisted for shulker box swaps (matches server logic)
    private boolean isBlacklistedForShulkerSwap(net.minecraft.item.Item item) {
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
}

