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

