package me.maxadams98.ultimateinventory.client.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

/**
 * Utility methods for pick block functionality.
 * Shared between Litematica event listener and mixin handler.
 */
public class PickBlockUtils {

    // Prevent rapid-fire shulker searches (cooldown in ticks)
    private static long lastShulkerSearchTime = 0;
    private static final int SHULKER_SEARCH_COOLDOWN = 10; // 0.5 seconds at 20 TPS

    // Prevent repetitive searches for the same item
    private static ItemStack lastSearchedItem = ItemStack.EMPTY;
    private static long lastSearchedItemTime = 0;
    private static final int ITEM_SEARCH_COOLDOWN = 100; // 5 seconds at 20 TPS - don't search for same item repeatedly

    // Prevent triggering searches while player is actively using items
    private static ItemStack lastCheckedItem = ItemStack.EMPTY;
    private static int itemMissingTicks = 0;
    private static final int MISSING_ITEM_DELAY = 3; // Wait 3 ticks before triggering search (prevents false positives during item use)

    // Server compatibility tracking
    private static boolean serverHasPlugin = true; // Assume server has plugin until proven otherwise
    private static boolean compatibilityChecked = false;
    private static long lastErrorMessageTime = 0;
    private static final int ERROR_MESSAGE_COOLDOWN = 300; // 15 seconds between error messages
    
    /**
     * Sends a pick block command to the server safely.
     * Includes error handling for servers without the UltimateInventory plugin.
     */
    private static void sendCommandSafely(ClientPlayerEntity player, String command) {
        if (!serverHasPlugin) {
            System.out.println("[UltimateInventory] Skipping command - server doesn't have UltimateInventory plugin: " + command);
            return;
        }

        try {
            System.out.println("[UltimateInventory] Sending command: " + command);
            player.networkHandler.sendCommand(command);

            // If we get here without exception, mark compatibility as checked
            if (!compatibilityChecked) {
                compatibilityChecked = true;
                System.out.println("[UltimateInventory] Server compatibility verified - UltimateInventory plugin detected");
            }
        } catch (Exception e) {
            if (serverHasPlugin) {
                // First failure - disable plugin detection and warn user
                serverHasPlugin = false;
                System.out.println("[UltimateInventory] ERROR: Server doesn't appear to have UltimateInventory plugin!");
                System.out.println("[UltimateInventory] This mod requires the UltimateInventory plugin on the server.");
                System.out.println("[UltimateInventory] Please install the plugin or disable this mod.");

                // Rate limit error messages to avoid spam
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastErrorMessageTime > ERROR_MESSAGE_COOLDOWN * 50) {
                    player.sendMessage(Text.literal("§c[UltimateInventory] Server missing UltimateInventory plugin! Please install it."), false);
                    lastErrorMessageTime = currentTime;
                }
            }
            System.out.println("[UltimateInventory] Command failed: " + e.getMessage());
        }
    }

    /**
     * Sends a pick block command to the server.
     * Converts the item to Bukkit-style material name and sends /uipickblock command.
     * Includes cooldowns to prevent rapid-fire and repetitive searches.
     */
    public static void sendPickBlockCommand(ClientPlayerEntity player, ItemStack targetItem) {
        // Check cooldown to prevent spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShulkerSearchTime < SHULKER_SEARCH_COOLDOWN * 50) { // Convert ticks to ms
            System.out.println("[UltimateInventory] Shulker search on cooldown, skipping");
            return;
        }

        // Check if we're repeatedly searching for the same item
        if (!lastSearchedItem.isEmpty() &&
            ItemStack.areItemsEqual(lastSearchedItem, targetItem) &&
            currentTime - lastSearchedItemTime < ITEM_SEARCH_COOLDOWN * 50) {
            long remainingMs = (ITEM_SEARCH_COOLDOWN * 50) - (currentTime - lastSearchedItemTime);
            int remainingTicks = (int) Math.ceil(remainingMs / 50.0);
            System.out.println("[UltimateInventory] Recently searched for " + targetItem.getItem().toString() +
                             ", skipping to prevent repetitive swapping (" + remainingTicks + " ticks remaining)");
            return;
        }

        // Update tracking
        lastShulkerSearchTime = currentTime;
        lastSearchedItem = targetItem.copy();
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
     */
    public static boolean shouldTriggerShulkerSearch(ClientPlayerEntity player, ItemStack targetItem) {
        boolean itemAvailable = itemExistsInNonBlacklistedHotbarSlot(player, targetItem);

        if (itemAvailable) {
            // Item is available - reset missing counter and allow future searches
            if (itemMissingTicks > 0) {
                System.out.println("[UltimateInventory] Item became available, resetting missing counter");
            }
            itemMissingTicks = 0;
            lastCheckedItem = ItemStack.EMPTY;
            // Also reset search tracking since item is now available
            resetItemSearchTracking(targetItem);
            return false;
        }

        // Item is not available - check if it's the same item we're tracking
        if (lastCheckedItem.isEmpty() || !ItemStack.areItemsEqual(lastCheckedItem, targetItem)) {
            // Different item - start tracking
            lastCheckedItem = targetItem.copy();
            itemMissingTicks = 1;
            System.out.println("[UltimateInventory] Started tracking missing item: " + targetItem.getItem().toString());
            return false;
        }

        // Same item - increment counter
        itemMissingTicks++;

        if (itemMissingTicks >= MISSING_ITEM_DELAY) {
            // Item has been missing long enough - allow search
            System.out.println("[UltimateInventory] Item missing for " + itemMissingTicks + " ticks, triggering shulker search");
            itemMissingTicks = 0; // Reset for next time
            lastCheckedItem = ItemStack.EMPTY;
            return true;
        }

        System.out.println("[UltimateInventory] Item still missing but waiting (" + itemMissingTicks + "/" + MISSING_ITEM_DELAY + " ticks)");
        return false;
    }

    /**
     * Resets server compatibility check when joining a new server.
     * Call this when connecting to a server or when mod initializes.
     */
    public static void resetServerCompatibility() {
        serverHasPlugin = true;
        compatibilityChecked = false;
        System.out.println("[UltimateInventory] Reset server compatibility check for new connection");
    }

    /**
     * Resets the repetitive search tracking for a specific item.
     * Call this when an item becomes successfully available.
     */
    public static void resetItemSearchTracking(ItemStack item) {
        if (!lastSearchedItem.isEmpty() && ItemStack.areItemsEqual(lastSearchedItem, item)) {
            lastSearchedItem = ItemStack.EMPTY;
            lastSearchedItemTime = 0;
            System.out.println("[UltimateInventory] Reset search tracking for item: " + item.getItem().toString());
        }
    }

    /**
     * Sends an error message to the player when all hotbar slots are blacklisted.
     */
    public static void sendBlacklistedSlotsError(ClientPlayerEntity player) {
        player.sendMessage(Text.literal("§cCannot pick block: all hotbar slots contain items that cannot be swapped"), false);
    }
}

