package me.maxadams98.ultimateinventory.client.litematica;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Invocation handler for the dynamic proxy.
 * Routes method calls from Litematica to our implementation.
 */
class LitematicaInvocationHandler implements InvocationHandler {
    private final LitematicaPickBlockListenerImpl impl;

    public LitematicaInvocationHandler(LitematicaPickBlockListenerImpl impl) {
        this.impl = impl;
    }

    /**
     * Get a default success result for unimplemented methods.
     */
    private Object getDefaultResult(Method method) {
        // If method returns void, return null
        if (method.getReturnType() == void.class) {
            return null;
        }

        // Try to get SUCCESS enum from Litematica result class
        try {
            Class<?> resultClass = Class.forName("fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventResult");
            return resultClass.getEnumConstants()[0]; // SUCCESS
        } catch (Exception e) {
            // Return null for any other return type
            return null;
        }
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        // Only log methods we haven't seen before to reduce spam
        if (!methodName.equals("getName")) { // getName is called frequently
            System.out.println("[UltimateInventory] Litematica called: " + methodName);
        }
        
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
                // Method not implemented - return default result instead of crashing
                System.out.println("[UltimateInventory] Method not implemented: " + methodName + " - returning default result");
                return getDefaultResult(method);
            }
        } catch (Exception e) {
            // Any other exception during method invocation - log and return default
            System.out.println("[UltimateInventory] Error invoking method " + methodName + ": " + e.getMessage());
            return getDefaultResult(method);
        }
    }
}

