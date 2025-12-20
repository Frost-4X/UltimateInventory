package me.Percyqaz.UltimateInventory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardMonitor extends BukkitRunnable {
    private final UltimateInventory plugin;
    private final InventoryListener inventoryListener;
    private final Map<UUID, Integer> lastTriggerValue = new HashMap<>();

    public ScoreboardMonitor(UltimateInventory plugin, InventoryListener inventoryListener) {
        this.plugin = plugin;
        this.inventoryListener = inventoryListener;
    }

    @Override
    public void run() {
        try {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective triggerObj = mainScoreboard.getObjective("ui_pickblock_trigger");
            Objective materialNameObj = mainScoreboard.getObjective("ui_pickblock_material_name");
            
            if (triggerObj == null || materialNameObj == null) {
                return; // Scoreboards not initialized yet
            }

            // Check all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();
                int currentTrigger = triggerObj.getScore(player.getName()).getScore();
                int lastTrigger = lastTriggerValue.getOrDefault(playerId, 0);
                
                // If trigger changed from 0 to 1+, process the pick block request
                if (currentTrigger > 0 && lastTrigger == 0) {
                    String playerName = player.getName();
                    String playerUUID = player.getUniqueId().toString().replace("-", "");
                    String materialName = null;
                    
                    // Method 1: Player-specific entry format: "material:<UUID>:<MATERIAL>"
                    // This is the most reliable for multi-player scenarios
                    String playerPrefix = "material:" + playerUUID + ":";
                    for (String entry : mainScoreboard.getEntries()) {
                        if (entry.startsWith(playerPrefix) && materialNameObj.getScore(entry).getScore() > 0) {
                            materialName = entry.substring(playerPrefix.length());
                            materialNameObj.getScore(entry).setScore(0); // Clear after reading
                            plugin.getLogger().info("[PickBlock] Found player-specific material entry: " + entry);
                            break;
                        }
                    }
                    
                    // Method 2: Player name format: "material:<PLAYER>:<MATERIAL>"
                    if (materialName == null) {
                        String namePrefix = "material:" + playerName + ":";
                        for (String entry : mainScoreboard.getEntries()) {
                            if (entry.startsWith(namePrefix) && materialNameObj.getScore(entry).getScore() > 0) {
                                materialName = entry.substring(namePrefix.length());
                                materialNameObj.getScore(entry).setScore(0);
                                plugin.getLogger().info("[PickBlock] Found name-based material entry: " + entry);
                                break;
                            }
                        }
                    }
                    
                    // Method 3: Global format: "material:<MATERIAL>" (less reliable with multiple players)
                    // Only use if no player-specific entry found
                    if (materialName == null) {
                        for (String entry : mainScoreboard.getEntries()) {
                            if (entry.startsWith("material:") && materialNameObj.getScore(entry).getScore() > 0) {
                                String[] parts = entry.split(":", 3);
                                // If it's just "material:<MATERIAL>" (2 parts, no player identifier)
                                if (parts.length == 2) {
                                    materialName = parts[1];
                                    materialNameObj.getScore(entry).setScore(0);
                                    plugin.getLogger().info("[PickBlock] Found global material entry: " + entry + " (less reliable)");
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (materialName != null && !materialName.isEmpty()) {
                        try {
                            org.bukkit.Material material = org.bukkit.Material.valueOf(materialName.toUpperCase());
                            plugin.getLogger().info("[PickBlock] Scoreboard trigger detected for " + playerName + 
                                ", material: " + materialName);
                            
                            // Process the pick block request
                            boolean success = inventoryListener.handlePickBlockRequest(player, material);
                            
                            if (success) {
                                plugin.getLogger().info("[PickBlock] Successfully processed pick block for " + playerName);
                            }
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("[PickBlock] Invalid material name: " + materialName + " for player " + playerName + 
                                " - Error: " + e.getMessage());
                        }
                    } else {
                        plugin.getLogger().warning("[PickBlock] No material name found in scoreboard for player " + playerName + 
                            ". Client mod should set: scoreboard players set \"material:<UUID>:<MATERIAL>\" ui_pickblock_material_name 1");
                    }
                    
                    // Reset the trigger
                    triggerObj.getScore(playerName).setScore(0);
                }
                
                lastTriggerValue.put(playerId, currentTrigger);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[PickBlock] Error monitoring scoreboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

