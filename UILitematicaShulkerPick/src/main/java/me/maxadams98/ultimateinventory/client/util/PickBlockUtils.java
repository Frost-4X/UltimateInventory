package me.maxadams98.ultimateinventory.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

/**
 * Utility methods for pick block functionality.
 * Shared between Litematica event listener and mixin handler.
 */
public class PickBlockUtils {

    /**
     * Check if the player is in survival mode.
     * Uses the interaction manager to get the current game mode.
     */
    public static boolean isPlayerInSurvivalMode() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null) {
            return false;
        }
        return client.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL;
    }

    // Prevent rapid-fire shulker searches (cooldown in ticks)
    private static long lastShulkerSearchTime = 0;
    private static final int SHULKER_SEARCH_COOLDOWN = 10; // 0.5 seconds at 20 TPS

    // Prevent repetitive searches for the same item
    private static net.minecraft.item.Item lastSearchedItemType = null;
    private static long lastSearchedItemTime = 0;
    private static final int ITEM_SEARCH_COOLDOWN = 100; // 5 seconds at 20 TPS - don't search for same item repeatedly

    // Prevent triggering searches while player is actively using items
    private static net.minecraft.item.Item lastCheckedItemType = null;
    private static int itemMissingTicks = 0;
    private static final int MISSING_ITEM_DELAY = 2; // Wait 2 ticks before triggering search (prevents false positives during item use)

    // Server compatibility tracking
    private static boolean serverHasPlugin = true; // Assume server has plugin until proven otherwise
    private static boolean compatibilityChecked = false;
    private static int failedCommandCount = 0;
    private static final int MAX_COMMAND_FAILURES = 3; // Allow some failures before disabling
    private static long lastErrorMessageTime = 0;
    private static final int ERROR_MESSAGE_COOLDOWN = 300; // 15 seconds between error messages
    
    /**
     * Sends a pick block command to the server safely.
     * Includes error handling for servers without the UltimateInventory plugin.
     */
    private static void sendCommandSafely(ClientPlayerEntity player, String command) {
        // Always try to send the command - failures are handled gracefully
        try {
            System.out.println("[UltimateInventory] Attempting to send command: " + command + " (serverHasPlugin=" + serverHasPlugin + ", compatibilityChecked=" + compatibilityChecked + ")");

            // Use reflection to find the correct sendCommand method for this Minecraft version
            var networkHandlerClass = player.networkHandler.getClass();
            java.lang.reflect.Method sendCommandMethod = null;

            // Try to find any method that takes a String as first parameter and returns void
            // These are likely the actual sendCommand methods in Minecraft 1.21.8
            for (var method : networkHandlerClass.getDeclaredMethods()) {
                if (method.getParameterCount() >= 1) {
                    var params = method.getParameterTypes();
                    if (params[0] == String.class && method.getReturnType() == void.class) {
                        // Skip mixin handler methods (they have $ in the name)
                        if (!method.getName().contains("$")) {
                            sendCommandMethod = method;
                            // Make private methods accessible
                            sendCommandMethod.setAccessible(true);
                            break;
                        }
                    }
                }
            }

            if (sendCommandMethod == null) {
                // Try specific known method names based on debug output
                String[] possibleNames = {"sendCommand", "method_45731", "method_45729", "method_45730", "sendChat", "method_44050"};
                for (String methodName : possibleNames) {
                    try {
                        sendCommandMethod = networkHandlerClass.getDeclaredMethod(methodName, String.class);
                        break;
                    } catch (NoSuchMethodException e) {
                        // Try next name
                    }
                }
            }

            if (sendCommandMethod == null) {
                throw new RuntimeException("No suitable sendCommand method found");
            }

            // Call the method with appropriate parameters based on actual method signature
            try {
                var paramTypes = sendCommandMethod.getParameterTypes();
                int paramCount = paramTypes.length;

                if (paramCount == 1 && paramTypes[0] == String.class) {
                    // sendCommand(String)
                    sendCommandMethod.invoke(player.networkHandler, command);
                } else if (paramCount == 2 && paramTypes[0] == String.class && paramTypes[1] == boolean.class) {
                    // sendCommand(String, boolean)
                    sendCommandMethod.invoke(player.networkHandler, command, false);
                } else if (paramCount == 2 && paramTypes[0] == String.class && paramTypes[1] == String.class) {
                    // sendCommand(String, String) - pass command twice?
                    sendCommandMethod.invoke(player.networkHandler, command, command);
                } else if (paramCount == 2 && paramTypes[0] == String.class && paramTypes[1].getName().equals("net.minecraft.class_437")) {
                    // sendCommand(String, Screen) - pass null for Screen
                    sendCommandMethod.invoke(player.networkHandler, command, null);
                } else if (paramCount == 3 && paramTypes[0] == String.class && paramTypes[1] == String.class &&
                          paramTypes[2].getName().equals("net.minecraft.class_437")) {
                    // sendCommand(String, String, Screen) - found in logs!
                    sendCommandMethod.invoke(player.networkHandler, command, command, null);
                } else if (paramCount == 4 && paramTypes[0] == String.class &&
                          paramTypes[1].getName().equals("net.minecraft.class_437") &&
                          paramTypes[2].getName().equals("net.minecraft.class_437") &&
                          paramTypes[3] == boolean.class) {
                    // sendCommand(String, Screen, Screen, boolean) - pass nulls for Screens
                    sendCommandMethod.invoke(player.networkHandler, command, null, null, false);
                } else {
                    // Unknown signature - try with just the command string
                    sendCommandMethod.invoke(player.networkHandler, command);
                }
            } catch (Exception invokeException) {
                throw invokeException;
            }

            // If we get here without exception and haven't checked compatibility yet
            if (!compatibilityChecked) {
                compatibilityChecked = true;
                failedCommandCount = 0; // Reset failure count on success
            }
        } catch (Exception e) {
            failedCommandCount++;

            // Only disable after multiple failures
            if (failedCommandCount >= MAX_COMMAND_FAILURES && serverHasPlugin) {
                serverHasPlugin = false;
                System.out.println("[UltimateInventory] ERROR: Server doesn't appear to have UltimateInventory plugin!");
                System.out.println("[UltimateInventory] This mod requires the UltimateInventory plugin on the server.");

                // Rate limit error messages to avoid spam
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastErrorMessageTime > ERROR_MESSAGE_COOLDOWN * 50) {
                    player.sendMessage(Text.literal("§c[UltimateInventory] Server missing UltimateInventory plugin! Please install it."), false);
                    lastErrorMessageTime = currentTime;
                }
            }
        }
    }

    /**
     * Sends a pick block command to the server.
     * Converts the item to Bukkit-style material name and sends /uipickblock command.
     * Includes cooldowns to prevent rapid-fire and repetitive searches.
     * Only operates for survival mode players - creative/spectator/adventure use default behavior.
     */
    public static void sendPickBlockCommand(ClientPlayerEntity player, ItemStack targetItem) {
        // Only handle pick block for survival mode players
        // Creative/spectator/adventure should use default behavior
        if (!isPlayerInSurvivalMode()) {
            return;
        }

        // Only skip if we've confirmed the server doesn't have the plugin (after multiple failures)
        if (!serverHasPlugin && failedCommandCount >= MAX_COMMAND_FAILURES) {
            System.out.println("[UltimateInventory] Skipping command - server confirmed incompatible: " + targetItem.getItem().toString());
            return;
        }

        // Check cooldown to prevent spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShulkerSearchTime < SHULKER_SEARCH_COOLDOWN * 50) { // Convert ticks to ms
            return;
        }

        // Check if we're repeatedly searching for the same item
        if (lastSearchedItemType != null &&
            lastSearchedItemType == targetItem.getItem() &&
            currentTime - lastSearchedItemTime < ITEM_SEARCH_COOLDOWN * 50) {
            return;
        }

        // Update tracking
        lastShulkerSearchTime = currentTime;
        lastSearchedItemType = targetItem.getItem();
        lastSearchedItemTime = currentTime;
        var itemId = Registries.ITEM.getId(targetItem.getItem());
        String namespace = itemId.getNamespace();
        String path = itemId.getPath();
        String materialName = path.toUpperCase().replace('/', '_');
        
        if (!namespace.equals("minecraft")) {
            materialName = namespace.toUpperCase() + "_" + materialName;
        }
        
        String command = "uipickblock " + materialName;
        sendCommandSafely(player, command);
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
     * Checks if an item exists in a non-blacklisted hotbar slot (0-8).
     * This is the key check - if an item only exists in blacklisted slots,
     * it can't be used effectively and we should trigger shulker search.
     */
    public static boolean itemExistsInNonBlacklistedHotbarSlot(ClientPlayerEntity player, ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }

        var itemType = item.getItem();

        // Check currently held item (main hand)
        ItemStack heldItem = player.getMainHandStack();
        if (!heldItem.isEmpty() && heldItem.getItem() == itemType && !isBlacklistedForShulkerSwap(heldItem.getItem())) {
            return true;
        }

        // Check offhand
        ItemStack offhandItem = player.getOffHandStack();
        if (!offhandItem.isEmpty() && offhandItem.getItem() == itemType && !isBlacklistedForShulkerSwap(offhandItem.getItem())) {
            return true;
        }

        // Only check hotbar slots (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarItem = player.getInventory().getStack(i);
            if (!hotbarItem.isEmpty() && hotbarItem.getItem() == itemType && !isBlacklistedForShulkerSwap(hotbarItem.getItem())) {
                return true; // Found in non-blacklisted hotbar slot
            }
        }

        return false;
    }
    
    /**
     * Checks if all hotbar slots (0-8) contain blacklisted items.
     * Returns true only if all 9 slots have items AND all items are blacklisted.
     * Blacklisted items include tools, weapons, armor, containers, and other items
     * that shouldn't be swapped during building operations.
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
     * Blacklisted items: tools, weapons, armor, shulker boxes, ender chests, and other containers.
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
            name.contains("fishing_rod") || name.contains("shears") ||
            name.contains("shield") || name.contains("elytra")) {
            return true;
        }

        // Check for armor
        if (name.contains("_helmet") || name.contains("_chestplate") ||
            name.contains("_leggings") || name.contains("_boots") ||
            name.contains("_horse_armor")) {
            return true;
        }

        // Check for containers (shulker boxes, ender chest, bundles, etc.)
        if (name.contains("shulker_box") || name.equals("ender_chest") ||
            name.equals("bundle") || name.contains("minecart")) {
            return true;
        }

        // Check for other items that shouldn't be swapped during building
        if (name.equals("totem_of_undying") || name.equals("firework_rocket") ||
            name.contains("potion") || name.contains("tipped_arrow")) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a shulker search should be triggered for a missing item.
     * Includes delay to prevent false positives during item usage.
     * Returns true if search should be triggered, false if we should wait.
     * Only triggers for survival mode players - creative/spectator/adventure use default behavior.
     */
    public static boolean shouldTriggerShulkerSearch(ClientPlayerEntity player, ItemStack targetItem) {
        // Only handle pick block for survival mode players
        if (!isPlayerInSurvivalMode()) {
            return false;
        }

        System.out.println("[UltimateInventory] Checking if shulker search should be triggered for: " + targetItem.getItem().toString());

        boolean itemAvailable = itemExistsInNonBlacklistedHotbarSlot(player, targetItem);

        if (itemAvailable) {
            // Item is available - reset missing counter and allow future searches
            itemMissingTicks = 0;
            lastCheckedItemType = null; // Reset missing item tracking
            // Also reset search tracking since item is now available
            resetItemSearchTracking(targetItem);
            return false;
        }

        // Item is not available - check if it's the same item we're tracking
        if (lastCheckedItemType == null || lastCheckedItemType != targetItem.getItem()) {
            // Different item - start tracking
            lastCheckedItemType = targetItem.getItem();
            itemMissingTicks = 1;
            return false;
        }

        // Same item - increment counter
        itemMissingTicks++;

        if (itemMissingTicks >= MISSING_ITEM_DELAY) {
            // Item has been missing long enough - allow search
            itemMissingTicks = 0; // Reset for next time
            lastCheckedItemType = null;
            return true;
        }

        return false;
    }

    /**
     * Resets server compatibility check when joining a new server.
     * Call this when connecting to a server or when mod initializes.
     */
    public static void resetServerCompatibility() {
        serverHasPlugin = true;
        compatibilityChecked = false;
        failedCommandCount = 0;
        lastSearchedItemType = null;
        lastSearchedItemTime = 0;
    }

    /**
     * Resets the repetitive search tracking for a specific item.
     * Call this when an item becomes successfully available.
     */
    public static void resetItemSearchTracking(ItemStack item) {
        if (lastSearchedItemType != null && lastSearchedItemType == item.getItem()) {
            lastSearchedItemType = null;
            lastSearchedItemTime = 0;
        }
    }

    /**
     * Sends an error message to the player when all hotbar slots are blacklisted.
     */
    public static void sendBlacklistedSlotsError(ClientPlayerEntity player) {
        player.sendMessage(Text.literal("§cCannot pick block: all hotbar slots contain items that cannot be swapped"), false);
    }
}

