package me.Percyqaz.UltimateInventory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
// AdvancedEnderchest import commented out - requires AdvancedEnderchest.jar in libs/ folder
// import de.chriis.advancedenderchest.manager.EnderchestManager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class InventoryListener implements Listener
{
    UltimateInventory plugin;
    FileConfiguration config;
    boolean isPaper;
    boolean isAdvancedEnderchestPresent;
    Map<UUID, ItemStack> openShulkerBoxes = new HashMap<>();
    Map<UUID, Map.Entry<String, Integer>> playerAdvancedChests = new HashMap<>(); // Map to store chest IDs
    Map<UUID, Boolean> playerOpenedShulker = new HashMap<>(); // Map to store if player opened a shulker box
    Map<UUID, String> playerLastOpenedMenu = new HashMap<>();
    Map<UUID, Integer> playerLastHotbarSlot = new HashMap<>(); // Map to store last hotbar slot
    Map<UUID, AutoSwapData> playerAutoSwapData = new HashMap<>(); // Map to store auto-swap data

    boolean enableShulkerbox;
    boolean overrideShulkerbox;
    String commandShulkerbox;

    boolean enableEnderChest;
    boolean overrideEnderChest;
    String commandEnderChest;

    boolean enableCraftingTable;
    boolean overrideCraftingTable;
    String commandCraftingTable;

    boolean enableSmithingTable;
    boolean overrideSmithingTable;
    String commandSmithingTable;

    boolean enableStoneCutter;
    boolean overrideStoneCutter;
    String commandStoneCutter;

    boolean enableGrindstone;
    boolean overrideGrindstone;
    String commandGrindstone;

    boolean enableCartographyTable;
    boolean overrideCartographyTable;
    String commandCartographyTable;

    boolean enableLoom;
    boolean overrideLoom;
    String commandLoom;

    boolean enableAnvil;
    boolean overrideAnvil;
    String commandAnvil;

    boolean usePermissions;
    boolean enablePickBlock;
    boolean requireCreativeForPickBlock;

    public InventoryListener(UltimateInventory plugin, FileConfiguration config, boolean isPaper, boolean isAdvancedEnderchestPresent) {
        this.config = config;
        this.plugin = plugin;
        this.isPaper = isPaper;
        this.isAdvancedEnderchestPresent = isAdvancedEnderchestPresent;

        enableShulkerbox = config.getBoolean("shulkerbox.enable", true);
        overrideShulkerbox = config.getBoolean("shulkerbox.override", false);
        commandShulkerbox = config.getString("shulkerbox.command", "");

        enableEnderChest = config.getBoolean("enderChest.enable", true);
        overrideEnderChest = config.getBoolean("enderChest.override", false);
        commandEnderChest = config.getString("enderChest.command", "");

        enableCraftingTable = config.getBoolean("craftingTable.enable", true);
        overrideCraftingTable = config.getBoolean("craftingTable.override", false);
        commandCraftingTable = config.getString("craftingTable.command", "");

        if (isPaper) {
            enableSmithingTable = config.getBoolean("smithingTable.enable", true);
            overrideSmithingTable = config.getBoolean("smithingTable.override", false);
            commandSmithingTable = config.getString("smithingTable.command", "");

            enableStoneCutter = config.getBoolean("stoneCutter.enable", true);
            overrideStoneCutter = config.getBoolean("stoneCutter.override", false);
            commandStoneCutter = config.getString("stoneCutter.command", "");

            enableGrindstone = config.getBoolean("grindstone.enable", true);
            overrideGrindstone = config.getBoolean("grindstone.override", false);
            commandGrindstone = config.getString("grindstone.command", "");

            enableCartographyTable = config.getBoolean("cartographyTable.enable", true);
            overrideCartographyTable = config.getBoolean("cartographyTable.override", false);
            commandCartographyTable = config.getString("cartographyTable.command", "");

            enableLoom = config.getBoolean("loom.enable", true);
            overrideLoom = config.getBoolean("loom.override", false);
            commandLoom = config.getString("loom.command", "");

            enableAnvil = config.getBoolean("anvil.enable", false);
            overrideAnvil = config.getBoolean("anvil.override", false);
            commandAnvil = config.getString("anvil.command", "");
        }

        usePermissions = config.getBoolean("usePermissions", false);
        enablePickBlock = config.getBoolean("pickBlock.enable", true);
        requireCreativeForPickBlock = config.getBoolean("pickBlock.requireCreative", false);
    }

    private void executeCommand(Player player, String command) {
        if (command.isEmpty()) {
            return;
        }
        player.performCommand(command);
    }

    private boolean IsShulkerBox(Material material)
    {
        switch (material)
        {
            case SHULKER_BOX:
            case RED_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
            case WHITE_SHULKER_BOX:
                return true;
            default:
                return false;
        }
    }

    private boolean IsAnvil(Material material)
    {
        switch (material)
        {
            case ANVIL:
            case CHIPPED_ANVIL:
            case DAMAGED_ANVIL:
                return true;
            default:
                return false;
        }
    }

    private void ShowEnderchest(HumanEntity player)
    {
        if (player.getOpenInventory().getType() == InventoryType.ENDER_CHEST)
        {
            player.closeInventory();
            Bukkit.getServer().getPlayer(player.getUniqueId()).playSound(player, Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.2f);
        }
        else
        {
            player.openInventory(player.getEnderChest());
            Bukkit.getServer().getPlayer(player.getUniqueId()).playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.2f);
        }
    }

    private void ShowCraftingTable(HumanEntity player)
    {
        if (player.getOpenInventory().getType() == InventoryType.WORKBENCH)
        {
            return;
        }

        player.openWorkbench(null, true);
    }

    private void ShowStoneCutter(HumanEntity player)
    {
        if (player.getOpenInventory().getType() == InventoryType.STONECUTTER)
        {
            return;
        }

        player.openStonecutter(null, true);
    }

    private void ShowCartographyTable(HumanEntity player)
    {
        if (player.getOpenInventory().getType() == InventoryType.CARTOGRAPHY)
        {
            return;
        }

        player.openCartographyTable(null, true);
    }

    private void ShowLoom(HumanEntity player)
    {
        if (player.getOpenInventory().getType() == InventoryType.LOOM)
        {
            return;
        }

        player.openLoom(null, true);
    }

    private void ShowSmithingTable(HumanEntity player)
    {
        if (player.getOpenInventory().getType() == InventoryType.SMITHING)
        {
            return;
        }

        player.openSmithingTable(null, true);
    }

    private void ShowGrindstone(HumanEntity player)
    {
        if (player.getOpenInventory().getType() == InventoryType.GRINDSTONE)
        {
            return;
        }

        player.openGrindstone(null, true);
    }

    private void ShowAnvil(HumanEntity player)
    {
        if (player.getOpenInventory().getType() == InventoryType.ANVIL)
        {
            return;
        }

        player.openAnvil(null, true);
    }

    private void OpenShulkerbox(HumanEntity player, ItemStack shulkerItem)
    {
        // Don't open the box if already open (avoids a duplication bug)
        if (openShulkerBoxes.containsKey(player.getUniqueId()) && openShulkerBoxes.get(player.getUniqueId()).equals(shulkerItem))
        {
            return;
        }

        // Added NBT for "locking" to prevent stacking shulker boxes
        ItemMeta meta = shulkerItem.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey nbtKey = new NamespacedKey(plugin, "__shulkerbox_plugin");
        if(!data.has(nbtKey, PersistentDataType.STRING))
        {
            data.set(nbtKey, PersistentDataType.STRING, String.valueOf(System.currentTimeMillis()));
            shulkerItem.setItemMeta(meta);
        }

        Inventory shulker_inventory = ((ShulkerBox)((BlockStateMeta)meta).getBlockState()).getSnapshotInventory();
        Inventory inventory;
        if (!meta.hasDisplayName())
        {
            inventory = Bukkit.createInventory(null, InventoryType.SHULKER_BOX);
        }
        else
        {
            inventory = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, meta.getDisplayName());
        }
        inventory.setContents(shulker_inventory.getContents());

        player.openInventory(inventory);
        Bukkit.getServer().getPlayer(player.getUniqueId()).playSound(player, Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1.0f, 1.2f);

        openShulkerBoxes.put(player.getUniqueId(), shulkerItem);
    }

    private void CloseShulkerbox(HumanEntity player)
    {
        ItemStack shulkerItem = openShulkerBoxes.get(player.getUniqueId());
        BlockStateMeta meta = (BlockStateMeta)shulkerItem.getItemMeta();
        ShulkerBox shulkerbox = (ShulkerBox)meta.getBlockState();

        // Update the shulker box inventory
        shulkerbox.getInventory().setContents(player.getOpenInventory().getTopInventory().getContents());

        // Apply the updated block state back to the meta, see: https://jd.papermc.io/paper/1.21.3/org/bukkit/inventory/meta/BlockStateMeta.html#setBlockState(org.bukkit.block.BlockState)
        meta.setBlockState(shulkerbox);

        // Delete NBT for "locking" to prevent stacking shulker boxes
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey nbtKey = new NamespacedKey(plugin, "__shulkerbox_plugin");
        if(data.has(nbtKey, PersistentDataType.STRING))
        {
            data.remove(nbtKey);
        }

        // Apply the updated meta to the item
        shulkerItem.setItemMeta(meta);

        // If AdvancedEnderchests is present check if the player was previously using an AdvancedEnderchest
        if (isAdvancedEnderchestPresent && playerAdvancedChests.containsKey(player.getUniqueId())) {
            Map.Entry<String, Integer> chestEntry = playerAdvancedChests.get(player.getUniqueId());
            if (chestEntry != null) {
                String chestId = chestEntry.getKey();
                int slot = chestEntry.getValue();

                // AdvancedEnderchest code commented out - requires AdvancedEnderchest.jar
                // Get the old chest data
                // EnderchestManager.getItemsByChestID(player.getUniqueId(), chestId, (ItemStack[] itemStacks) -> {
                //     // Update the chest data
                //     itemStacks[slot] = shulkerItem;
                //     EnderchestManager.saveEnderchest(player.getUniqueId(), chestId, itemStacks);
                // });
            }
        }

        Bukkit.getServer().getPlayer(player.getUniqueId()).playSound(player, Sound.BLOCK_SHULKER_BOX_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.2f);

        openShulkerBoxes.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void InventoryClick(InventoryClickEvent e) {
        if (e.getAction() == InventoryAction.NOTHING) {
            return;
        }

        if (!e.isRightClick() || e.isShiftClick()) {
            if (openShulkerBoxes.containsKey(e.getWhoClicked().getUniqueId()) &&
                    (e.getAction() == InventoryAction.HOTBAR_SWAP
                            || e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
                            || (e.getCurrentItem() != null && IsShulkerBox(e.getCurrentItem().getType())))) {
                e.setCancelled(true);
            }
            return;
        }

        InventoryType clickedInventory = e.getClickedInventory().getType();
        if (!(clickedInventory == InventoryType.PLAYER || clickedInventory == InventoryType.ENDER_CHEST || clickedInventory == InventoryType.SHULKER_BOX)) {
            // Check if the inventory is a virtual chest from AdvancedEnderchests
            if (clickedInventory == InventoryType.CHEST && isAdvancedEnderchestPresent) {
                Component inventoryTitle = e.getView().title();

                // Check if the title is an AdvancedEnderchest
                if (!inventoryTitle.toString().contains("AEC Multi-EC")) {
                    return;
                }
            } else {
                return;
            }
        }

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getAmount() != 1) {
            return;
        }

        Material itemType = item.getType();
        HumanEntity player = e.getWhoClicked();

        if (clickedInventory != InventoryType.SHULKER_BOX && IsShulkerBox(itemType) && enableShulkerbox && (!usePermissions || player.hasPermission("ultimateinventory.shulkerbox"))) {
            playerOpenedShulker.put(player.getUniqueId(), true);

            if (overrideShulkerbox) {
                executeCommand((Player) player, commandShulkerbox);
            } else {

                // Check if the player was previously using an AdvancedEnderchest
                Component inventoryTitle = e.getView().title();

                // Check if the title is an AdvancedEnderchest
                if (inventoryTitle.toString().contains("AEC Multi-EC") && isAdvancedEnderchestPresent) {
                    Pattern pattern = Pattern.compile("Chest\\s+(\\d+)");
                    Matcher matcher = pattern.matcher(inventoryTitle.toString());

                    if (matcher.find()) {
                        String chestNumber = matcher.group(1);
                        String chestId = "aec.multi.chest." + chestNumber;
                        int slot = e.getRawSlot();
                        if (slot > 0 && slot <= 54) {
                            Map.Entry<String, Integer> chestEntry = new AbstractMap.SimpleEntry<>(chestId, e.getRawSlot());
                            playerAdvancedChests.put(player.getUniqueId(), chestEntry); // Store the chest data
                        }
                    }
                }

                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> OpenShulkerbox(player, item));
            }
            e.setCancelled(true);
        }

        if (itemType == Material.ENDER_CHEST && enableEnderChest && (!usePermissions || player.hasPermission("ultimateinventory.enderchest"))) {
            if (overrideEnderChest) {
                executeCommand((Player) player, commandEnderChest);
            } else {
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowEnderchest(player));
            }
            e.setCancelled(true);
        }

        if (itemType == Material.CRAFTING_TABLE && enableCraftingTable && (!usePermissions || player.hasPermission("ultimateinventory.craftingtable"))) {
            if (overrideCraftingTable) {
                executeCommand((Player) player, commandCraftingTable);
            } else {
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowCraftingTable(player));
            }
            e.setCancelled(true);
        }

        if (isPaper) {
            if (itemType == Material.STONECUTTER && enableStoneCutter && (!usePermissions || player.hasPermission("ultimateinventory.stonecutter"))) {
                if (overrideStoneCutter) {
                    executeCommand((Player) player, commandStoneCutter);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowStoneCutter(player));
                }
                e.setCancelled(true);
            }

            if (itemType == Material.CARTOGRAPHY_TABLE && enableCartographyTable && (!usePermissions || player.hasPermission("ultimateinventory.cartographytable"))) {
                if (overrideCartographyTable) {
                    executeCommand((Player) player, commandCartographyTable);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowCartographyTable(player));
                }
                e.setCancelled(true);
            }

            if (itemType == Material.LOOM && enableLoom && (!usePermissions || player.hasPermission("ultimateinventory.loom"))) {
                if (overrideLoom) {
                    executeCommand((Player) player, commandLoom);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowLoom(player));
                }
                e.setCancelled(true);
            }

            if (itemType == Material.SMITHING_TABLE && enableSmithingTable && (!usePermissions || player.hasPermission("ultimateinventory.smithingtable"))) {
                if (overrideSmithingTable) {
                    executeCommand((Player) player, commandSmithingTable);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowSmithingTable(player));
                }
                e.setCancelled(true);
            }

            if (itemType == Material.GRINDSTONE && enableGrindstone && (!usePermissions || player.hasPermission("ultimateinventory.grindstone"))) {
                if (overrideGrindstone) {
                    executeCommand((Player) player, commandGrindstone);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowGrindstone(player));
                }
                e.setCancelled(true);
            }

            if (IsAnvil(itemType) && enableAnvil && (!usePermissions || player.hasPermission("ultimateinventory.anvil"))) {
                if (overrideAnvil) {
                    executeCommand((Player) player, commandAnvil);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowAnvil(player));
                }
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void RightClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Material itemType = item.getType();

        if (IsShulkerBox(itemType) && item.getAmount() == 1 && enableShulkerbox && (!usePermissions || player.hasPermission("ultimateinventory.shulkerbox"))) {
            if (overrideShulkerbox) {
                executeCommand(player, commandShulkerbox);
            } else {
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> OpenShulkerbox(player, item));
            }
            e.setCancelled(true);
        }

        if (itemType == Material.ENDER_CHEST && enableEnderChest && (!usePermissions || player.hasPermission("ultimateinventory.enderchest"))) {
            if (overrideEnderChest) {
                executeCommand(player, commandEnderChest);
            } else {
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowEnderchest(player));
            }
            e.setCancelled(true);
        }

        if (itemType == Material.CRAFTING_TABLE && enableCraftingTable && (!usePermissions || player.hasPermission("ultimateinventory.craftingtable"))) {
            if (overrideCraftingTable) {
                executeCommand(player, commandCraftingTable);
            } else {
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowCraftingTable(player));
            }
            e.setCancelled(true);
        }

        if (isPaper) {
            if (itemType == Material.STONECUTTER && enableStoneCutter && (!usePermissions || player.hasPermission("ultimateinventory.stonecutter"))) {
                if (overrideStoneCutter) {
                    executeCommand(player, commandStoneCutter);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowStoneCutter(player));
                }
                e.setCancelled(true);
            }

            if (itemType == Material.CARTOGRAPHY_TABLE && enableCartographyTable && (!usePermissions || player.hasPermission("ultimateinventory.cartographytable"))) {
                if (overrideCartographyTable) {
                    executeCommand(player, commandCartographyTable);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowCartographyTable(player));
                }
                e.setCancelled(true);
            }

            if (itemType == Material.LOOM && enableLoom && (!usePermissions || player.hasPermission("ultimateinventory.loom"))) {
                if (overrideLoom) {
                    executeCommand(player, commandLoom);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowLoom(player));
                }
                e.setCancelled(true);
            }

            if (itemType == Material.SMITHING_TABLE && enableSmithingTable && (!usePermissions || player.hasPermission("ultimateinventory.smithingtable"))) {
                if (overrideSmithingTable) {
                    executeCommand(player, commandSmithingTable);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowSmithingTable(player));
                }
                e.setCancelled(true);
            }

            if (itemType == Material.GRINDSTONE && enableGrindstone && (!usePermissions || player.hasPermission("ultimateinventory.grindstone"))) {
                if (overrideGrindstone) {
                    executeCommand(player, commandGrindstone);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowGrindstone(player));
                }
                e.setCancelled(true);
            }

            if (IsAnvil(itemType) && enableAnvil && (!usePermissions || player.hasPermission("ultimateinventory.anvil"))) {
                if (overrideAnvil) {
                    executeCommand(player, commandAnvil);
                } else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> ShowAnvil(player));
                }
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void PickBlock(PlayerInteractEvent e) {
        // Handle pick block: when vanilla pick block fails (item not in inventory), search shulker boxes
        // Note: Middle-click in creative mode is handled client-side and may not trigger server events
        // We'll catch it through other means (hotbar change detection)
        
        // Log all interactions for debugging
        if (e.getClickedBlock() != null) {
            plugin.getLogger().info("[PickBlock] PlayerInteractEvent - Player: " + e.getPlayer().getName() + 
                ", Action: " + e.getAction() + ", Block: " + e.getClickedBlock().getType() + 
                ", GameMode: " + e.getPlayer().getGameMode());
        }
        
        // Try to detect pick block attempts through various actions
        // In creative mode, middle-click might trigger LEFT_CLICK_BLOCK or RIGHT_CLICK_BLOCK
        if (e.getClickedBlock() == null) {
            return;
        }
        
        // Accept both left and right click for pick block detection
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = e.getPlayer();
        plugin.getLogger().info("[PickBlock] Event triggered - Player: " + player.getName() + ", Action: " + e.getAction() + ", GameMode: " + player.getGameMode());
        
        // Check if pick block is enabled
        if (!enablePickBlock) {
            plugin.getLogger().info("[PickBlock] Feature disabled in config");
            return;
        }

        // Check if player needs to be in creative mode
        if (requireCreativeForPickBlock && player.getGameMode() != GameMode.CREATIVE) {
            plugin.getLogger().info("[PickBlock] Player not in creative mode (required by config)");
            return;
        }

        // For pick block detection in creative mode:
        // - Middle-click is handled client-side, but we can catch it via right-click with empty hand
        // - Also accept left-click in creative mode as it might be triggered
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean isHoldingAir = (heldItem == null || heldItem.getType() == Material.AIR);
        
        // In creative mode, be more permissive - accept both left and right click
        // Middle-click pick block might manifest as either action
        if (player.getGameMode() == GameMode.CREATIVE) {
            // In creative mode, if holding the same block type, skip (not a pick block attempt)
            if (!isHoldingAir && heldItem.getType() == e.getClickedBlock().getType()) {
                plugin.getLogger().info("[PickBlock] Player is holding the same block type, likely not a pick block attempt");
                return;
            }
            // Otherwise, proceed - this could be a pick block attempt
            plugin.getLogger().info("[PickBlock] Creative mode detected, treating as potential pick block attempt");
        } else {
            // In survival mode, only trigger on right-click
            if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
        }

        Block clickedBlock = e.getClickedBlock();
        if (clickedBlock == null) {
            plugin.getLogger().warning("[PickBlock] Clicked block is null");
            return;
        }

        Material blockType = clickedBlock.getType();
        plugin.getLogger().info("[PickBlock] Block clicked: " + blockType.name() + " at " + clickedBlock.getLocation());
        
        // Don't pick air or invalid blocks
        if (blockType == Material.AIR || blockType == Material.BARRIER || blockType == Material.BEDROCK) {
            plugin.getLogger().info("[PickBlock] Block type is invalid (AIR/BARRIER/BEDROCK), skipping");
            return;
        }

        // Create an ItemStack representing the block
        ItemStack targetItem = new ItemStack(blockType, 1);
        
        // First check if item exists in hotbar (0-8) - if it does, vanilla pick block will work
        boolean foundInHotbar = false;
        int hotbarSlotFound = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarItem = player.getInventory().getItem(i);
            if (hotbarItem != null && hotbarItem.getType() == blockType) {
                foundInHotbar = true;
                hotbarSlotFound = i;
                break;
            }
        }
        
        if (foundInHotbar) {
            plugin.getLogger().info("[PickBlock] Item found in hotbar at slot " + hotbarSlotFound + ", letting vanilla pick block handle it");
            return;
        }
        
        // Check if item exists elsewhere in inventory - if it does, vanilla pick block will work
        if (itemExistsInInventory(player, targetItem, -1)) {
            plugin.getLogger().info("[PickBlock] Item found in inventory (not hotbar), letting vanilla pick block handle it");
            return;
        }
        
        plugin.getLogger().info("[PickBlock] Item NOT found in inventory, searching shulker boxes...");
        
        // Item not found in inventory - vanilla pick block failed, so we search shulker boxes
        int lastHotbarSlot = playerLastHotbarSlot.getOrDefault(player.getUniqueId(), player.getInventory().getHeldItemSlot());
        plugin.getLogger().info("[PickBlock] Last hotbar slot: " + lastHotbarSlot + ", Current held slot: " + player.getInventory().getHeldItemSlot());
        
        // Find a free hotbar slot or use the last used one
        int targetSlot = findFreeHotbarSlot(player);
        if (targetSlot == -1) {
            // No free slot, use last used slot
            targetSlot = lastHotbarSlot;
            plugin.getLogger().info("[PickBlock] No free hotbar slot found, using last used slot: " + targetSlot);
        } else {
            plugin.getLogger().info("[PickBlock] Found free hotbar slot: " + targetSlot);
        }

        // Search for item in shulker boxes
        plugin.getLogger().info("[PickBlock] Searching shulker boxes for: " + blockType.name());
        AutoSwapData swapData = findItemInShulkers(player, targetItem, lastHotbarSlot, targetSlot);
        
        if (swapData != null) {
            plugin.getLogger().info("[PickBlock] Item found in shulker box! Shulker slot: " + swapData.shulkerSlot + ", Item slot in shulker: " + swapData.shulkerItemSlot);
            
            // Found item in shulker box
            ItemStack itemInTargetSlot = player.getInventory().getItem(targetSlot);
            plugin.getLogger().info("[PickBlock] Target slot " + targetSlot + " currently contains: " + (itemInTargetSlot != null ? itemInTargetSlot.getType().name() : "AIR"));
            
            // Check if we can swap (avoid replacing shulker boxes and tools)
            if (itemInTargetSlot != null && itemInTargetSlot.getType() != Material.AIR) {
                if (IsShulkerBox(itemInTargetSlot.getType()) || isTool(itemInTargetSlot.getType())) {
                    plugin.getLogger().info("[PickBlock] Target slot contains shulker box or tool, finding alternative slot...");
                    // Can't replace shulker box or tool, find another slot
                    targetSlot = findFreeHotbarSlotExcluding(player, targetSlot);
                    if (targetSlot == -1) {
                        // No suitable slot found
                        plugin.getLogger().warning("[PickBlock] No suitable hotbar slot found (avoiding shulker boxes and tools)");
                        player.sendMessage(ChatColor.RED + "Cannot pick block: no available hotbar slot (avoiding shulker boxes and tools)");
                        return;
                    }
                    swapData.targetSlot = targetSlot;
                    plugin.getLogger().info("[PickBlock] Found alternative slot: " + targetSlot);
                }
            }

            // Store swap data and perform the swap
            playerAutoSwapData.put(player.getUniqueId(), swapData);
            
            // Get the shulker box item
            ItemStack shulkerItem;
            if (swapData.shulkerSlot == 40) {
                shulkerItem = player.getInventory().getItemInOffHand();
                plugin.getLogger().info("[PickBlock] Shulker box is in offhand");
            } else {
                shulkerItem = player.getInventory().getItem(swapData.shulkerSlot);
                plugin.getLogger().info("[PickBlock] Shulker box is in inventory slot: " + swapData.shulkerSlot);
            }
            
            if (shulkerItem != null && IsShulkerBox(shulkerItem.getType())) {
                plugin.getLogger().info("[PickBlock] Performing swap - Shulker: " + shulkerItem.getType().name() + ", Target slot: " + swapData.targetSlot);
                // Perform swap directly without opening shulker
                performPickBlockSwap(player, swapData, shulkerItem);
                
                // Play sound
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.2f);
                player.sendMessage(ChatColor.GREEN + "Picked " + blockType.name().toLowerCase().replace("_", " ") + " from shulker box");
                plugin.getLogger().info("[PickBlock] Swap completed successfully!");
            } else {
                plugin.getLogger().warning("[PickBlock] Shulker item is null or not a shulker box! Item: " + (shulkerItem != null ? shulkerItem.getType().name() : "null"));
            }
        } else {
            // Item not found in shulker boxes either
            plugin.getLogger().info("[PickBlock] Item not found in any shulker boxes");
            player.sendMessage(ChatColor.YELLOW + "Block not found in inventory or shulker boxes");
        }
    }

    /**
     * Public method to handle pick block requests from commands (datapack)
     * This method searches shulker boxes and performs the swap
     * @param player The player requesting the pick block
     * @param blockType The material type to search for
     * @return true if item was found and swapped, false otherwise
     */
    public boolean handlePickBlockRequest(Player player, Material blockType) {
        if (!enablePickBlock) {
            player.sendMessage(ChatColor.RED + "Pick block feature is disabled in plugin config");
            return false;
        }

        // Don't pick air or invalid blocks
        if (blockType == Material.AIR || blockType == Material.BARRIER || blockType == Material.BEDROCK) {
            return false;
        }

        // Create an ItemStack representing the block
        ItemStack targetItem = new ItemStack(blockType, 1);
        
        // First check if item exists in hotbar (0-8) - if it does, vanilla pick block will work
        boolean foundInHotbar = false;
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarItem = player.getInventory().getItem(i);
            if (hotbarItem != null && hotbarItem.getType() == blockType) {
                foundInHotbar = true;
                break;
            }
        }
        
        if (foundInHotbar) {
            return false;
        }
        
        // Check if item exists elsewhere in inventory
        if (itemExistsInInventory(player, targetItem, -1)) {
            return false;
        }
        
        // Item not in inventory, search shulker boxes
        int lastHotbarSlot = playerLastHotbarSlot.getOrDefault(player.getUniqueId(), player.getInventory().getHeldItemSlot());
        
        // Find a free hotbar slot or use the last used one
        int targetSlot = findFreeHotbarSlot(player);
        if (targetSlot == -1) {
            targetSlot = lastHotbarSlot;
        }

        // Search for item in shulker boxes
        AutoSwapData swapData = findItemInShulkers(player, targetItem, lastHotbarSlot, targetSlot);
        
        if (swapData != null) {
            // Found item in shulker box
            ItemStack itemInTargetSlot = player.getInventory().getItem(targetSlot);
            
            // Check if we can swap (avoid replacing shulker boxes and tools)
            if (itemInTargetSlot != null && itemInTargetSlot.getType() != Material.AIR) {
                if (IsShulkerBox(itemInTargetSlot.getType()) || isTool(itemInTargetSlot.getType())) {
                    // Can't replace shulker box or tool, find another slot
                    targetSlot = findFreeHotbarSlotExcluding(player, targetSlot);
                    if (targetSlot == -1) {
                        player.sendMessage(ChatColor.RED + "Cannot pick block: no available hotbar slot (avoiding shulker boxes and tools)");
                        return false;
                    }
                    swapData.targetSlot = targetSlot;
                }
            }

            // Get the shulker box item
            ItemStack shulkerItem;
            if (swapData.shulkerSlot == 40) {
                shulkerItem = player.getInventory().getItemInOffHand();
            } else {
                shulkerItem = player.getInventory().getItem(swapData.shulkerSlot);
            }
            
            if (shulkerItem != null && IsShulkerBox(shulkerItem.getType())) {
                // Perform swap directly without opening shulker
                performPickBlockSwap(player, swapData, shulkerItem);
                
                // Switch player's selected hotbar slot to the slot where the item was placed
                if (swapData.targetSlot >= 0 && swapData.targetSlot < 9) {
                    player.getInventory().setHeldItemSlot(swapData.targetSlot);
                }
                
                // Play sound
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.2f);
                player.sendMessage(ChatColor.GREEN + "Picked " + blockType.name().toLowerCase().replace("_", " ") + " from shulker box");
                return true;
            } else {
                plugin.getLogger().warning("[PickBlock] Shulker item is null or not a shulker box");
                player.sendMessage(ChatColor.RED + "Error: Could not access shulker box");
                return false;
            }
        } else {
            // Item not found in shulker boxes either
            player.sendMessage(ChatColor.YELLOW + "Block not found in inventory or shulker boxes");
            return false;
        }
    }

    // Helper method to find a free hotbar slot (0-8)
    private int findFreeHotbarSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                return i;
            }
        }
        return -1;
    }

    // Helper method to find a free hotbar slot excluding a specific slot
    private int findFreeHotbarSlotExcluding(Player player, int excludeSlot) {
        for (int i = 0; i < 9; i++) {
            if (i == excludeSlot) {
                continue;
            }
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                return i;
            }
        }
        return -1;
    }

    // Check if an item is a tool (pickaxe, axe, shovel, hoe, sword, etc.)
    private boolean isTool(Material material) {
        String name = material.name().toLowerCase();
        return name.contains("pickaxe") || name.contains("axe") || 
               name.contains("shovel") || name.contains("hoe") || 
               name.contains("sword") || name.contains("bow") ||
               name.contains("crossbow") || name.contains("trident") ||
               name.contains("fishing_rod") || name.contains("shears");
    }

    // Perform pick block swap: swap item from shulker with item in target slot (or place if empty)
    private void performPickBlockSwap(Player player, AutoSwapData swapData, ItemStack shulkerItem) {
        BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
        if (meta == null || !(meta.getBlockState() instanceof ShulkerBox)) {
            plugin.getLogger().warning("[PickBlock] Invalid shulker item meta or block state");
            playerAutoSwapData.remove(player.getUniqueId());
            return;
        }

        ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
        Inventory shulkerInventory = shulkerBox.getInventory();
        
        // Get the item from shulker
        ItemStack itemFromShulker = shulkerInventory.getItem(swapData.shulkerItemSlot);
        if (itemFromShulker == null || itemFromShulker.getType() == Material.AIR) {
            plugin.getLogger().warning("[PickBlock] Item at shulker slot " + swapData.shulkerItemSlot + " is null or AIR");
            playerAutoSwapData.remove(player.getUniqueId());
            return;
        }
        
        // Get the item from target slot (the one we're swapping with)
        ItemStack itemFromTargetSlot = player.getInventory().getItem(swapData.targetSlot);
        
        // Put shulker item in target slot
        player.getInventory().setItem(swapData.targetSlot, itemFromShulker.clone());
        
        // Put the item from target slot (or air) in shulker
        if (itemFromTargetSlot != null && itemFromTargetSlot.getType() != Material.AIR) {
            shulkerInventory.setItem(swapData.shulkerItemSlot, itemFromTargetSlot.clone());
        } else {
            shulkerInventory.setItem(swapData.shulkerItemSlot, null);
        }
        
        // Update the shulker box
        meta.setBlockState(shulkerBox);
        shulkerItem.setItemMeta(meta);
        
        // Update player inventory with modified shulker
        if (swapData.shulkerSlot == 40) {
            player.getInventory().setItemInOffHand(shulkerItem);
        } else {
            player.getInventory().setItem(swapData.shulkerSlot, shulkerItem);
        }
        
        // If shulker is currently open, update the open inventory
        if (openShulkerBoxes.containsKey(player.getUniqueId()) && 
            openShulkerBoxes.get(player.getUniqueId()).equals(shulkerItem)) {
            player.getOpenInventory().getTopInventory().setContents(shulkerInventory.getContents());
        }
        
        // Clean up
        playerAutoSwapData.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void InventoryClose(InventoryCloseEvent e) {
        this.plugin.getLogger().info("Inventory Close Event");

        // If closed inventory is a shulker box
        if (openShulkerBoxes.containsKey(e.getPlayer().getUniqueId())) {
            this.plugin.getLogger().info("Closing Shulkerbox");
            CloseShulkerbox(e.getPlayer());

            // If AEC present, and player was using a vanilla Enderchest, reopen it
            if (isAdvancedEnderchestPresent && playerLastOpenedMenu.get(e.getPlayer().getUniqueId()).contains("Ender Chest")) {
                if (!playerOpenedShulker.containsKey(e.getPlayer().getUniqueId())) {
                    this.plugin.getLogger().info("Reopening Enderchest");

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        e.getPlayer().openInventory(e.getPlayer().getEnderChest());
                    }, 1L);
                }
            }
            // If AEC present and player was using an AdvancedEnderchest
            else if (isAdvancedEnderchestPresent && playerAdvancedChests.containsKey(e.getPlayer().getUniqueId())) {
                if (!playerOpenedShulker.containsKey(e.getPlayer().getUniqueId())) {
                    this.plugin.getLogger().info("Reopening Enderchest");
                    Map.Entry<String, Integer> chestEntry = playerAdvancedChests.get(e.getPlayer().getUniqueId());
                    String chestId = chestEntry.getKey();

                    // Remove the player from the map
                    playerAdvancedChests.remove(e.getPlayer().getUniqueId());

                    // Clear opened AdvancedEnderchests - commented out, requires AdvancedEnderchest.jar
                    // EnderchestManager.OPENED_ENDERCHESTS.remove((Player) e.getPlayer());

                    // Pop the number from the end of the chest ID
                    String[] chestIdParts = chestId.split("\\.");
                    String chestNumber = chestIdParts[chestIdParts.length - 1];
                    String chestName = "Chest " + chestNumber;

                    if (!playerOpenedShulker.containsKey(e.getPlayer().getUniqueId())) {
                        this.plugin.getLogger().info("Reopening AdvancedEnderchest");

                        // Reopen the advanced enderchest after a short delay - commented out, requires AdvancedEnderchest.jar
                        // Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        //     EnderchestManager.openEnderchest((Player) e.getPlayer(), "&r&5" + chestName, chestId, 54);
                        // }, 3L);
                    }
                }
            }
            playerOpenedShulker.remove(e.getPlayer().getUniqueId());
        }
        // if the closed inventory was the AEC menu clear last opened menu
        else if (isAdvancedEnderchestPresent && Objects.equals(e.getPlayer().getOpenInventory().title().toString(), "AEC Multi-Enderchest Menu")) {
            this.plugin.getLogger().info("Clearing last opened menu");
            playerLastOpenedMenu.remove(e.getPlayer().getUniqueId());
            return;
        }
        // else if the closed inventory is an AdvancedEnderchest
        else if (isAdvancedEnderchestPresent && e.getPlayer().getOpenInventory().title().toString().contains("AEC Multi-EC")) {
            if (!playerOpenedShulker.containsKey(e.getPlayer().getUniqueId())) {
                this.plugin.getLogger().info("Opening AEC Menu");

                // Open the AdvancedEnderchest menu after a short delay - commented out, requires AdvancedEnderchest.jar
                // Bukkit.getScheduler().runTaskLater(plugin, () -> {
                //     EnderchestManager.openEnderchest((Player) e.getPlayer());
                // }, 1L);
            }
        }
        // else if the closed inventory is an Enderchest
        else if (isAdvancedEnderchestPresent && e.getInventory().getType() == InventoryType.ENDER_CHEST) {
            if (!playerOpenedShulker.containsKey(e.getPlayer().getUniqueId())) {
                this.plugin.getLogger().info("Opening AEC Menu");

                // Open the AdvancedEnderchest menu after a short delay - commented out, requires AdvancedEnderchest.jar
                // Bukkit.getScheduler().runTaskLater(plugin, () -> {
                //     EnderchestManager.openEnderchest((Player) e.getPlayer());
                // }, 1L);
            }
        }

        if (e.getPlayer().getOpenInventory().getType() != InventoryType.SHULKER_BOX)
        {
            this.plugin.getLogger().info("Updating last opened menu");
            playerLastOpenedMenu.put(e.getPlayer().getUniqueId(), e.getView().title().toString());
        }
    }

    // Needs to close shulker box before items drop on death to avoid a duplication bug
    @EventHandler(priority = EventPriority.HIGHEST)
    public void Death(PlayerDeathEvent e)
    {
        Player player = e.getEntity();
        if (openShulkerBoxes.containsKey(player.getUniqueId()))
        {
            CloseShulkerbox(player);
        }
    }

    // Auto-swap: When player selects an item not in inventory, find it in shulker and swap
    // This also helps catch pick block scenarios where the item appears in hotbar
    @EventHandler(priority = EventPriority.NORMAL)
    public void HotbarChange(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        int newSlot = e.getNewSlot();
        int oldSlot = e.getPreviousSlot();
        
        plugin.getLogger().info("[PickBlock] HotbarChange - Player: " + player.getName() + 
            ", Slot: " + oldSlot + " -> " + newSlot + ", GameMode: " + player.getGameMode());
        
        // Get the item in the new slot
        ItemStack selectedItem = player.getInventory().getItem(newSlot);
        
        // Only proceed if there's an item selected
        if (selectedItem == null || selectedItem.getType() == Material.AIR) {
            plugin.getLogger().info("[PickBlock] HotbarChange - New slot is empty, updating last slot");
            // Update last slot for next time
            playerLastHotbarSlot.put(player.getUniqueId(), oldSlot);
            return;
        }
        
        plugin.getLogger().info("[PickBlock] HotbarChange - Selected item: " + selectedItem.getType().name());
        
        // Check if this item type exists elsewhere in inventory (excluding current slot)
        // If it only exists in the current slot, it means they don't have it elsewhere
        if (!itemExistsInInventory(player, selectedItem, newSlot)) {
            plugin.getLogger().info("[PickBlock] HotbarChange - Item not found elsewhere in inventory, searching shulker boxes");
            // Item not found elsewhere in inventory, search shulker boxes
            AutoSwapData swapData = findItemInShulkers(player, selectedItem, oldSlot, newSlot);
            if (swapData != null) {
                plugin.getLogger().info("[PickBlock] HotbarChange - Found item in shulker, performing swap");
                // Store swap data and open the shulker
                playerAutoSwapData.put(player.getUniqueId(), swapData);
                // Open the shulker box that contains the item
                ItemStack shulkerItem;
                if (swapData.shulkerSlot == 40) {
                    shulkerItem = player.getInventory().getItemInOffHand();
                } else {
                    shulkerItem = player.getInventory().getItem(swapData.shulkerSlot);
                }
                
                if (shulkerItem != null && IsShulkerBox(shulkerItem.getType())) {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        OpenShulkerbox(player, shulkerItem);
                        // Perform swap after a short delay to ensure shulker is open
                        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            performAutoSwap(player);
                        }, 2L);
                    }, 1L);
                }
            } else {
                plugin.getLogger().info("[PickBlock] HotbarChange - Item not found in shulker boxes either");
            }
        } else {
            plugin.getLogger().info("[PickBlock] HotbarChange - Item found elsewhere in inventory, no action needed");
        }
        
        // Update last slot for next time
        playerLastHotbarSlot.put(player.getUniqueId(), oldSlot);
    }

    // Check if item exists in inventory (excluding a specific slot)
    private boolean itemExistsInInventory(Player player, ItemStack item, int excludeSlot) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        Material itemType = item.getType();
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length; i++) {
            if (i == excludeSlot) {
                continue; // Skip the excluded slot
            }
            
            ItemStack invItem = contents[i];
            if (invItem != null && invItem.getType() == itemType) {
                // Check if items are similar (same type, ignoring stack size)
                if (invItem.isSimilar(item) || invItem.getType() == item.getType()) {
                    return true;
                }
            }
        }
        
        // Also check offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() == itemType) {
            if (offhand.isSimilar(item) || offhand.getType() == item.getType()) {
                return true;
            }
        }
        
        return false;
    }

    // Find item in shulker boxes and return swap data
    private AutoSwapData findItemInShulkers(Player player, ItemStack targetItem, int lastHotbarSlot, int targetSlot) {
        if (targetItem == null || targetItem.getType() == Material.AIR) {
            return null;
        }
        
        Material targetType = targetItem.getType();
        ItemStack[] contents = player.getInventory().getContents();
        
        // Search through inventory for shulker boxes
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && IsShulkerBox(item.getType())) {
                // Check if this shulker box contains the target item
                BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                if (meta != null && meta.getBlockState() instanceof ShulkerBox) {
                    ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
                    Inventory shulkerInventory = shulkerBox.getInventory();
                    
                    // Search through shulker inventory
                    for (int shulkerSlot = 0; shulkerSlot < shulkerInventory.getSize(); shulkerSlot++) {
                        ItemStack shulkerItem = shulkerInventory.getItem(shulkerSlot);
                        if (shulkerItem != null && shulkerItem.getType() != Material.AIR) {
                            if (shulkerItem.getType() == targetType) {
                                // Check if items are similar
                                if (shulkerItem.isSimilar(targetItem) || shulkerItem.getType() == targetType) {
                                    // Found it! Return swap data
                                    return new AutoSwapData(i, shulkerSlot, lastHotbarSlot, targetSlot, targetItem);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Also check offhand for shulker box
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && IsShulkerBox(offhand.getType())) {
            BlockStateMeta meta = (BlockStateMeta) offhand.getItemMeta();
            if (meta != null && meta.getBlockState() instanceof ShulkerBox) {
                ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
                Inventory shulkerInventory = shulkerBox.getInventory();
                
                for (int shulkerSlot = 0; shulkerSlot < shulkerInventory.getSize(); shulkerSlot++) {
                    ItemStack shulkerItem = shulkerInventory.getItem(shulkerSlot);
                    if (shulkerItem != null && shulkerItem.getType() == targetType) {
                        if (shulkerItem.isSimilar(targetItem) || shulkerItem.getType() == targetType) {
                            // Offhand slot is -1 in Bukkit API, but we'll use 40 for consistency
                            return new AutoSwapData(40, shulkerSlot, lastHotbarSlot, targetSlot, targetItem);
                        }
                    }
                }
            }
        }
        
        return null;
    }

    // Perform the auto-swap: swap item from shulker with last hotbar item
    private void performAutoSwap(Player player) {
        AutoSwapData swapData = playerAutoSwapData.get(player.getUniqueId());
        if (swapData == null) {
            return;
        }
        
        // Get the shulker box item
        ItemStack shulkerItem;
        if (swapData.shulkerSlot == 40) {
            shulkerItem = player.getInventory().getItemInOffHand();
        } else {
            shulkerItem = player.getInventory().getItem(swapData.shulkerSlot);
        }
        
        if (shulkerItem == null || !IsShulkerBox(shulkerItem.getType())) {
            playerAutoSwapData.remove(player.getUniqueId());
            return;
        }
        
        BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
        if (meta == null || !(meta.getBlockState() instanceof ShulkerBox)) {
            playerAutoSwapData.remove(player.getUniqueId());
            return;
        }
        
        ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
        Inventory shulkerInventory = shulkerBox.getInventory();
        
        // Get the item from shulker
        ItemStack itemFromShulker = shulkerInventory.getItem(swapData.shulkerItemSlot);
        if (itemFromShulker == null || itemFromShulker.getType() == Material.AIR) {
            playerAutoSwapData.remove(player.getUniqueId());
            return;
        }
        
        // Get the item from last hotbar slot (the one we're swapping with)
        ItemStack itemFromHotbar = player.getInventory().getItem(swapData.lastHotbarSlot);
        
        // Perform the swap
        // Put shulker item in target slot (where player was trying to get the item)
        player.getInventory().setItem(swapData.targetSlot, itemFromShulker.clone());
        
        // Put the item from last hotbar slot (or air) in shulker
        if (itemFromHotbar != null && itemFromHotbar.getType() != Material.AIR) {
            shulkerInventory.setItem(swapData.shulkerItemSlot, itemFromHotbar.clone());
        } else {
            shulkerInventory.setItem(swapData.shulkerItemSlot, null);
        }
        
        // Update the shulker box
        meta.setBlockState(shulkerBox);
        shulkerItem.setItemMeta(meta);
        
        // Update player inventory with modified shulker
        if (swapData.shulkerSlot == 40) {
            player.getInventory().setItemInOffHand(shulkerItem);
        } else {
            player.getInventory().setItem(swapData.shulkerSlot, shulkerItem);
        }
        
        // If shulker is currently open, update the open inventory
        if (openShulkerBoxes.containsKey(player.getUniqueId()) && 
            openShulkerBoxes.get(player.getUniqueId()).equals(shulkerItem)) {
            player.getOpenInventory().getTopInventory().setContents(shulkerInventory.getContents());
        }
        
        // Clean up
        playerAutoSwapData.remove(player.getUniqueId());
        
        // Play swap sound
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.2f);
    }

    // Helper class to store auto-swap data
    private static class AutoSwapData {
        int shulkerSlot; // Slot in player inventory where shulker box is
        int shulkerItemSlot; // Slot inside shulker box where item is
        int lastHotbarSlot; // Last hotbar slot to swap with
        int targetSlot; // Target slot where the item should go
        ItemStack targetItem; // The item we're looking for
        
        AutoSwapData(int shulkerSlot, int shulkerItemSlot, int lastHotbarSlot, int targetSlot, ItemStack targetItem) {
            this.shulkerSlot = shulkerSlot;
            this.shulkerItemSlot = shulkerItemSlot;
            this.lastHotbarSlot = lastHotbarSlot;
            this.targetSlot = targetSlot;
            this.targetItem = targetItem;
        }
    }
}
