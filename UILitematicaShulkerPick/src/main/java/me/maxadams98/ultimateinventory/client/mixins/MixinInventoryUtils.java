package me.maxadams98.ultimateinventory.client.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.maxadams98.ultimateinventory.client.litematica.LitematicaMixinHandler;

/**
 * Mixin to intercept Litematica's pick block method.
 * This works with Litematica versions that don't have the event system.
 * 
 * Note: This Mixin is optional - it will only apply if Litematica is present at runtime.
 */
@Mixin(targets = "fi.dy.masa.litematica.util.InventoryUtils", remap = false)
public class MixinInventoryUtils {
    
    @Inject(method = "schematicWorldPickBlock", at = @At("RETURN"), remap = false)
    private static void onSchematicWorldPickBlock(ItemStack stack, BlockPos pos, World schematicWorld, MinecraftClient mc, CallbackInfo ci) {
        // Check if Litematica's pick block succeeded
        if (mc.player == null || stack.isEmpty()) {
            return;
        }
        
        // Wait a tick to let Litematica's pick block complete
        mc.execute(() -> {
            LitematicaMixinHandler.checkPickBlockResult(mc, stack);
        });
    }
}

