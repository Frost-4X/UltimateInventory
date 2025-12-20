package me.percyqaz.ultimateinventory.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

/**
 * Litematica pick block listener.
 * Uses reflection to check if Litematica is available at runtime.
 */
public class LitematicaPickBlockListener {
    private static Object listenerInstance;
    private static boolean registered = false;
    
    public static boolean isRegistered() {
        return registered;
    }
    
    public static void register() {
        if (registered) {
            System.out.println("[UltimateInventory] Litematica listener already registered");
            return;
        }
        
        try {
            System.out.println("[UltimateInventory] Attempting to register Litematica pick block listener...");
            
            // Check if Litematica is available and get the event handler
            Class<?> eventHandlerClass = Class.forName("fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventHandler");
            System.out.println("[UltimateInventory] Found Litematica event handler class");
            
            Object eventHandler = eventHandlerClass.getMethod("getInstance").invoke(null);
            System.out.println("[UltimateInventory] Got Litematica event handler instance");
            
            // Create our listener implementation
            LitematicaPickBlockListenerImpl impl = new LitematicaPickBlockListenerImpl();
            System.out.println("[UltimateInventory] Created listener implementation");
            
            // Create a dynamic proxy that implements the interface
            Class<?> listenerInterface = Class.forName("fi.dy.masa.litematica.interfaces.ISchematicPickBlockEventListener");
            listenerInstance = Proxy.newProxyInstance(
                listenerInterface.getClassLoader(),
                new Class<?>[] { listenerInterface },
                new LitematicaInvocationHandler(impl)
            );
            System.out.println("[UltimateInventory] Created proxy instance");
            
            // Register it
            eventHandlerClass.getMethod("registerSchematicPickBlockEventListener", listenerInterface)
                .invoke(eventHandler, listenerInstance);
            
            registered = true;
            System.out.println("[UltimateInventory] Successfully registered Litematica pick block listener!");
        } catch (ClassNotFoundException e) {
            System.out.println("[UltimateInventory] Litematica not found: " + e.getMessage());
            registered = false;
        } catch (Exception e) {
            System.out.println("[UltimateInventory] Failed to register Litematica listener: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            registered = false;
        }
    }
    
    /**
     * Implementation of the listener interface.
     * This class implements the interface methods that Litematica will call.
     * We use Object types for parameters we don't need, which works because Java allows
     * method overloading and the actual interface types will be passed at runtime.
     */
    private static class LitematicaPickBlockListenerImpl {
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
                boolean itemFound = itemExistsInInventory(player, lastPickBlockStack);
                System.out.println("[UltimateInventory] Item found in inventory: " + itemFound);
                
                if (!itemFound) {
                    System.out.println("[UltimateInventory] Item not found - checking hotbar slots...");
                    if (areAllHotbarSlotsBlacklisted(player)) {
                        System.out.println("[UltimateInventory] All hotbar slots blacklisted");
                        player.sendMessage(Text.literal("§cCannot pick block: all hotbar slots contain items that cannot be swapped"), false);
                        lastPickBlockStack = ItemStack.EMPTY;
                        return;
                    }
                    
                    System.out.println("[UltimateInventory] Triggering shulker box search for: " + lastPickBlockStack.getItem().toString());
                    handlePickBlock(client, player, lastPickBlockStack);
                } else {
                    System.out.println("[UltimateInventory] Item was successfully picked by Litematica, no action needed");
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
        
        private void handlePickBlock(MinecraftClient client, ClientPlayerEntity player, ItemStack targetItem) {
            var itemId = Registries.ITEM.getId(targetItem.getItem());
            String namespace = itemId.getNamespace();
            String path = itemId.getPath();
            String materialName = path.toUpperCase().replace('/', '_');
            
            if (!namespace.equals("minecraft")) {
                materialName = namespace.toUpperCase() + "_" + materialName;
            }
            
            String command = "uipickblock " + materialName;
            player.networkHandler.sendCommand(command);
        }
        
        private boolean itemExistsInInventory(ClientPlayerEntity player, ItemStack item) {
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
        
        private boolean areAllHotbarSlotsBlacklisted(ClientPlayerEntity player) {
            for (int i = 0; i < 9; i++) {
                ItemStack item = player.getInventory().getStack(i);
                if (item.isEmpty()) {
                    return false;
                }
                if (!isBlacklistedForShulkerSwap(item.getItem())) {
                    return false;
                }
            }
            return true;
        }
        
        private boolean isBlacklistedForShulkerSwap(net.minecraft.item.Item item) {
            var itemId = Registries.ITEM.getId(item);
            String path = itemId.getPath();
            String name = path.toLowerCase();
            
            if (name.contains("pickaxe") || name.contains("axe") || 
                name.contains("shovel") || name.contains("hoe") || 
                name.contains("sword") || name.contains("bow") ||
                name.contains("crossbow") || name.contains("trident") ||
                name.contains("fishing_rod") || name.contains("shears")) {
                return true;
            }
            
            if (name.contains("shulker_box")) {
                return true;
            }
            
            if (name.equals("ender_chest")) {
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * Invocation handler for the dynamic proxy.
     * Routes method calls from Litematica to our implementation.
     */
    private static class LitematicaInvocationHandler implements InvocationHandler {
        private final LitematicaPickBlockListenerImpl impl;
        
        public LitematicaInvocationHandler(LitematicaPickBlockListenerImpl impl) {
            this.impl = impl;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            System.out.println("[UltimateInventory] Proxy invoked method: " + methodName);
            
            // Route to our implementation
            try {
                Method implMethod = impl.getClass().getMethod(methodName, method.getParameterTypes());
                return implMethod.invoke(impl, args);
            } catch (NoSuchMethodException e) {
                // Try with Object types for parameters we don't have classes for
                Class<?>[] paramTypes = method.getParameterTypes();
                Class<?>[] objectParamTypes = new Class<?>[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    // Use Object for types we can't import, keep others as-is
                    String paramTypeName = paramTypes[i].getName();
                    if (paramTypeName.startsWith("net.minecraft.world.level") || 
                        paramTypeName.startsWith("net.minecraft.core")) {
                        objectParamTypes[i] = Object.class;
                    } else {
                        objectParamTypes[i] = paramTypes[i];
                    }
                }
                
                try {
                    Method implMethod = impl.getClass().getMethod(methodName, objectParamTypes);
                    return implMethod.invoke(impl, args);
                } catch (NoSuchMethodException e2) {
                    System.out.println("[UltimateInventory] Method not found: " + methodName + " - " + e2.getMessage());
                    throw e;
                }
            }
        }
    }
}
