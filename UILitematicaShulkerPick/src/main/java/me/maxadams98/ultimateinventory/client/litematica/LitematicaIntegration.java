package me.maxadams98.ultimateinventory.client.litematica;

/**
 * Main integration class for Litematica support.
 * Registers the event listener when Litematica is present.
 */
public class LitematicaIntegration {
    private static boolean registered = false;
    
    /**
     * Registers the Litematica pick block listener.
     * This will only succeed if Litematica is present at runtime.
     */
    public static void register() {
        if (registered) {
            System.out.println("[UltimateInventory] Litematica integration already registered");
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
            Object listenerInstance = java.lang.reflect.Proxy.newProxyInstance(
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
    
    public static boolean isRegistered() {
        return registered;
    }
}

