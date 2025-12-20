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
    
    // Track last held item for Litematica pick block detection
    private static ItemStack lastHeldItem = null;
    private static int litematicaCheckTicksRemaining = 0;
    private static boolean litematicaDetected = false;
    
    @Override
    public void onInitializeClient() {
        // Monitor vanilla pick block key binding instead of creating our own
        // This allows vanilla to handle it first, then we check if it succeeded
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        // Check for Litematica on first tick (for logging)
        System.out.println("[UltimateInventory] Client mod initialized, will detect Litematica on first tick");
    }
    
    private void onClientTick(MinecraftClient client) {
        // Detect Litematica once (for logging)
        if (!litematicaDetected) {
            try {
                Class.forName("fi.dy.masa.litematica.util.InventoryUtils");
                System.out.println("[UltimateInventory] Litematica detected! Using Mixin-based pick block interception.");
                litematicaDetected = true;
            } catch (ClassNotFoundException e) {
                // Litematica not present, Mixin will just not apply
            }
        }
        
        if (client.player == null || client.world == null) {
            wasVanillaPickBlockPressed = false;
            checkTicksRemaining = 0;
            lastHeldItem = null;
            litematicaCheckTicksRemaining = 0;
            return;
        }
        
        ClientPlayerEntity player = client.player;
        
        GameMode currentMode = client.interactionManager.getCurrentGameMode();
        if (currentMode != GameMode.CREATIVE && currentMode != GameMode.SURVIVAL) {
            wasVanillaPickBlockPressed = false;
            checkTicksRemaining = 0;
            lastHeldItem = null;
            litematicaCheckTicksRemaining = 0;
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
        
        // Detect Litematica pick block: check if held item changed to something not in inventory
        checkForLitematicaPickBlock(client, player);
    }
    
    private void checkForLitematicaPickBlock(MinecraftClient client, ClientPlayerEntity player) {
        // Get current held item
        ItemStack currentHeldItem = player.getMainHandStack();
        
        // If current item is air, just update tracking
        if (currentHeldItem.isEmpty()) {
            lastHeldItem = null;
            litematicaCheckTicksRemaining = 0;
            return;
        }
        
        // Check if item changed
        boolean itemChanged = false;
        if (lastHeldItem == null) {
            // First time tracking - initialize
            lastHeldItem = currentHeldItem.copy();
            return;
        } else if (lastHeldItem.isEmpty()) {
            // Item appeared in hand (was air, now has item)
            itemChanged = true;
        } else if (!ItemStack.areItemsEqual(currentHeldItem, lastHeldItem) || 
                   currentHeldItem.getItem() != lastHeldItem.getItem()) {
            // Item type changed
            itemChanged = true;
        }
        
        // If item didn't change, just update tracking
        if (!itemChanged) {
            lastHeldItem = currentHeldItem.copy();
            return;
        }
        
        // Item changed - wait a tick to see if it's a legitimate change or Litematica pick block
        if (litematicaCheckTicksRemaining == 0) {
            litematicaCheckTicksRemaining = 2; // Check after 2 ticks
            lastHeldItem = currentHeldItem.copy();
            return;
        }
        
        // Check if the new item exists elsewhere in inventory
        if (litematicaCheckTicksRemaining > 0) {
            litematicaCheckTicksRemaining--;
            
            if (litematicaCheckTicksRemaining == 0) {
                // Check if item exists in inventory (excluding current slot)
                // Get the selected hotbar slot (0-8) - in full inventory this is slot 36 + selectedSlot
                // We'll find which hotbar slot the item is in
                int selectedHotbarSlot = -1;
                for (int i = 0; i < 9; i++) {
                    ItemStack hotbarItem = player.getInventory().getStack(i);
                    if (hotbarItem == currentHeldItem || (!hotbarItem.isEmpty() && hotbarItem.getItem() == currentHeldItem.getItem())) {
                        selectedHotbarSlot = i;
                        break;
                    }
                }
                // Convert hotbar slot (0-8) to full inventory slot (36-44)
                int selectedSlot = selectedHotbarSlot >= 0 ? 36 + selectedHotbarSlot : -1;
                boolean existsInInventory = itemExistsInInventory(player, currentHeldItem, selectedSlot);
                
                if (!existsInInventory) {
                    // Item not found in inventory - could be Litematica pick block
                    // Check if all hotbar slots are blacklisted
                    if (areAllHotbarSlotsBlacklisted(player)) {
                        player.sendMessage(Text.literal("§cCannot pick block: all hotbar slots contain items that cannot be swapped"), false);
                        lastHeldItem = currentHeldItem.copy();
                        return;
                    }
                    
                    // Send pick block command to server
                    handlePickBlock(client, player, currentHeldItem);
                }
                
                lastHeldItem = currentHeldItem.copy();
            }
        }
    }
    
    // Check if item exists in inventory (excluding a specific slot)
    private boolean itemExistsInInventory(ClientPlayerEntity player, ItemStack item, int excludeSlot) {
        if (item.isEmpty()) {
            return false;
        }
        
        var itemType = item.getItem();
        var inventory = player.getInventory();
        
        // Check all inventory slots except the excluded one
        for (int i = 0; i < inventory.size(); i++) {
            if (i == excludeSlot) {
                continue; // Skip the excluded slot
            }
            
            ItemStack invItem = inventory.getStack(i);
            if (!invItem.isEmpty() && invItem.getItem() == itemType) {
                return true;
            }
        }
        
        // Also check offhand
        ItemStack offhand = inventory.getStack(40); // Offhand slot
        if (!offhand.isEmpty() && offhand.getItem() == itemType) {
            return true;
        }
        
        return false;
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

