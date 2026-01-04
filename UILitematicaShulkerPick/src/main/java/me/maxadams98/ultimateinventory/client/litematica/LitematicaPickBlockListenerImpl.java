package me.maxadams98.ultimateinventory.client.litematica;

import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import me.maxadams98.ultimateinventory.client.util.PickBlockUtils;
import me.maxadams98.ultimateinventory.client.PrinterIntegration;

/**
 * Implementation of Litematica's pick block event listener interface.
 * Handles pick block events from Litematica and triggers shulker box search if needed.
 */
class LitematicaPickBlockListenerImpl {
    private ItemStack lastPickBlockStack = ItemStack.EMPTY;
    private static Class<?> levelClass;
    private static Class<?> blockPosClass;
    private static Class<?> blockStateClass;
    
    static {
        try {
            // Try to load the classes if available (for proper type checking)
            levelClass = Class.forName("net.minecraft.world.level.Level");
            blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState");
        } catch (ClassNotFoundException e) {
            // Classes not available, use Object
            levelClass = Object.class;
            blockPosClass = Object.class;
            blockStateClass = Object.class;
        }
    }
    
    public Supplier<String> getName() {
        return () -> "UltimateInventory";
    }
    
    public void onSchematicPickBlockCancelled(Supplier<String> cancelledBy) {
        lastPickBlockStack = ItemStack.EMPTY;
    }
    
    public Object onSchematicPickBlockStart(boolean closest) {
        System.out.println("[UltimateInventory] Litematica pick block START - closest: " + closest);
        return getSuccessResult();
    }
    
    // Use Object for parameters we don't actually use - Java will match the interface at runtime
    public Object onSchematicPickBlockPreGather(Object schematicWorld, Object pos, Object expectedState) {
        System.out.println("[UltimateInventory] Litematica pick block PRE_GATHER");
        return getSuccessResult();
    }
    
    public Object onSchematicPickBlockPrePick(Object schematicWorld, Object pos, Object expectedState, ItemStack stack) {
        System.out.println("[UltimateInventory] Litematica pick block PRE_PICK - item: " + (stack.isEmpty() ? "empty" : stack.getItem().toString()));
        lastPickBlockStack = stack.copy();
        return getSuccessResult();
    }
    
    public void onSchematicPickBlockSuccess() {
        System.out.println("[UltimateInventory] Litematica pick block SUCCESS callback received");
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            System.out.println("[UltimateInventory] Player is null, aborting");
            return;
        }
        if (lastPickBlockStack.isEmpty()) {
            System.out.println("[UltimateInventory] Last pick block stack is empty, aborting");
            return;
        }
        
        ClientPlayerEntity player = client.player;
        
        // Wait a tick to let Litematica's pick block complete
        client.execute(() -> {
            System.out.println("[UltimateInventory] Checking if item was picked: " + (lastPickBlockStack.isEmpty() ? "empty" : lastPickBlockStack.getItem().toString()));

            // Check if we should trigger a shulker search (includes delay to prevent false positives during item usage)
            if (PickBlockUtils.shouldTriggerShulkerSearch(player, lastPickBlockStack)) {
                // Item has been missing long enough to warrant a search
                if (PickBlockUtils.areAllHotbarSlotsBlacklisted(player)) {
                    System.out.println("[UltimateInventory] All hotbar slots blacklisted - cannot swap");
                    PickBlockUtils.sendBlacklistedSlotsError(player);
                    lastPickBlockStack = ItemStack.EMPTY;
                    return;
                }

                // Check if item exists anywhere in inventory (including blacklisted slots)
                boolean itemExistsAnywhere = PickBlockUtils.itemExistsInInventory(player, lastPickBlockStack);
                if (itemExistsAnywhere) {
                    System.out.println("[UltimateInventory] Item exists but only in blacklisted/main inventory slots - triggering shulker search");
                } else {
                    System.out.println("[UltimateInventory] Item not found anywhere in inventory - triggering shulker search");
                }

                System.out.println("[UltimateInventory] Triggering shulker box search for: " + lastPickBlockStack.getItem().toString());
                // Pause printer to prevent conflicts during shulker operations
                PrinterIntegration.pausePrinterForShulkerAction(20); // 1 second at 20 TPS
                PickBlockUtils.sendPickBlockCommand(player, lastPickBlockStack);
            }
            
            lastPickBlockStack = ItemStack.EMPTY;
        });
    }
    
    private Object getSuccessResult() {
        try {
            Class<?> resultClass = Class.forName("fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventResult");
            return resultClass.getEnumConstants()[0]; // SUCCESS
        } catch (Exception e) {
            return null;
        }
    }
}

