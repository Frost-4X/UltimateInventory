package me.maxadams98.ultimateinventory.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional integration with Litematica Printer mod.
 *
 * When we trigger shulker-based item retrieval, the printer can
 * accidentally interact with opened shulkers and cause weird behavior.
 *
 * This helper temporarily disables printer's PRINT_MODE for a short time
 * while our shulker logic runs, then restores the original value.
 *
 * Uses reflection so we don't have a hard compile-time dependency on
 * the printer or malilib. If the printer mod isn't present, all methods
 * simply no-op.
 */
public class PrinterIntegration {

    private static boolean printerDetected = false;
    private static boolean detectionAttempted = false;

    private static Object printModeConfig = null;
    private static Method getBooleanMethod = null;
    private static Method setBooleanMethod = null;

    private static Boolean lastPrintMode = null;
    private static int disableTicksRemaining = 0;

    /**
     * Try to detect the Litematica Printer config class and cache the
     * PRINT_MODE field + methods. Safe to call many times.
     */
    private static void detectPrinter() {
        if (detectionAttempted) {
            return;
        }
        detectionAttempted = true;

        try {
            // me.aleksilassila.litematica.printer.config.Configs.PRINT_MODE
            Class<?> configsClass = Class.forName("me.aleksilassila.litematica.printer.config.Configs");
            Field printModeField = configsClass.getField("PRINT_MODE");
            Object cfg = printModeField.get(null);

            Method getMethod = cfg.getClass().getMethod("getBooleanValue");
            Method setMethod = cfg.getClass().getMethod("setBooleanValue", boolean.class);

            printModeConfig = cfg;
            getBooleanMethod = getMethod;
            setBooleanMethod = setMethod;
            printerDetected = true;

            System.out.println("[UltimateInventory] Litematica Printer detected, enabling safety integration");
        } catch (Throwable t) {
            // Printer or malilib not present, or API changed - fail gracefully
            printerDetected = false;
        }
    }

    /**
     * Call every client tick to manage the temporary disable timer
     * and restore the printer state when time is up.
     */
    public static void onClientTick() {
        if (!printerDetected || printModeConfig == null || getBooleanMethod == null || setBooleanMethod == null) {
            return;
        }

        if (disableTicksRemaining <= 0) {
            return;
        }

        disableTicksRemaining--;

        if (disableTicksRemaining == 0 && lastPrintMode != null) {
            try {
                // Restore whatever the user had set before we touched it
                setBooleanMethod.invoke(printModeConfig, lastPrintMode.booleanValue());
            } catch (Throwable ignored) {
                // If restore fails for some reason, just ignore – better to leave printer off
            } finally {
                lastPrintMode = null;
            }
        }
    }

    /**
     * Temporarily disable printer's PRINT_MODE for the given number of ticks.
     *
     * Safe to call multiple times; each call will extend the disable window
     * but will only snapshot the original mode once.
     *
     * @param ticks Number of client ticks to keep printer disabled for.
     */
    public static void pausePrinterForShulkerAction(int ticks) {
        if (!printerDetected && !detectionAttempted) {
            detectPrinter();
        }

        if (!printerDetected || printModeConfig == null || getBooleanMethod == null || setBooleanMethod == null) {
            return; // Printer not installed or API changed
        }

        try {
            boolean current = (Boolean) getBooleanMethod.invoke(printModeConfig);

            // Capture the user's previous setting the first time we disable
            if (disableTicksRemaining <= 0) {
                lastPrintMode = current;
            }

            // If printer is currently enabled, turn it off
            if (current) {
                setBooleanMethod.invoke(printModeConfig, false);
            }

            // Extend the disable window
            if (ticks > disableTicksRemaining) {
                disableTicksRemaining = ticks;
            }
        } catch (Throwable ignored) {
            // Any reflection failure should just turn this into a no-op
        }
    }
}
