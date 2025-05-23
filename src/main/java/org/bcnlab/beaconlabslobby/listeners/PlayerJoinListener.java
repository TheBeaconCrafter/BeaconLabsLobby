package org.bcnlab.beaconlabslobby.listeners;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bcnlab.beaconlabslobby.utils.ScoreboardUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerJoinListener implements Listener {

    private final BeaconLabsLobby plugin;

    public PlayerJoinListener(BeaconLabsLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        teleportPlayerToSpawn(player);
        player.setGameMode(GameMode.ADVENTURE);

        clearInventory(player);

        // Give configurable items on join
        giveServerSelectorItem(player);
        givePlayerHiderItem(player);

        //Scoreboard
        ScoreboardUtil scoreboardUtil = new ScoreboardUtil(player, plugin);
        scoreboardUtil.updateScoreboard();
        scoreboardUtil.setPlayerScoreboard();

        if(plugin.getHealOnJoin()) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
        }

    }

    private void teleportPlayerToSpawn(Player player) {
        try {
            Location spawnLocation = plugin.getSpawnLocation();
            if (spawnLocation != null) {
                player.teleport(spawnLocation);
            } else {
                plugin.getLogger().warning("Spawn location is not set.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to teleport player " + player.getName() + " to spawn:");
            e.printStackTrace();
        }
    }

    private void clearInventory(Player player) {
        player.getInventory().clear(); // Clear the player's entire inventory
    }

    private void giveServerSelectorItem(Player player) {
        FileConfiguration config = plugin.getConfig();

        // Check if the configuration section for items exists
        if (config.contains("server-selector.items")) {
            ConfigurationSection itemConfig = config.getConfigurationSection("server-selector.settings");

            // Get item details from configuration
            String itemName = itemConfig.getString("name", "Server Selector");
            itemName = ChatColor.translateAlternateColorCodes('&', itemName);
            Material itemType = Material.valueOf(itemConfig.getString("type", "COMPASS"));
            List<String> itemLore = itemConfig.getStringList("lore");
            int itemSlot = itemConfig.getInt("slot", 2);

            // Create the item stack
            ItemStack item = new ItemStack(itemType);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemName);
                // Translate lore color codes
                List<String> translatedLore = new ArrayList<>();
                for (String line : itemLore) {
                    String lore_color = ChatColor.translateAlternateColorCodes('&', line);
                    translatedLore.add(lore_color);
                }
                meta.setLore(translatedLore);
                item.setItemMeta(meta);
            }

            // Give the item to the player
            player.getInventory().setItem(itemSlot, item);
        } else {
            plugin.getLogger().warning("Configuration for server selector item not found!");
        }
    }

    private void givePlayerHiderItem(Player player) {
        FileConfiguration config = plugin.getConfig();

        // Check if the configuration section for items exists
        if (config.contains("player-hider.settings")) {
            ConfigurationSection itemConfig = config.getConfigurationSection("player-hider.settings");

            // Get item details from configuration
            String itemName = itemConfig.getString("name", "Server Selector");
            itemName = ChatColor.translateAlternateColorCodes('&', itemName);
            Material itemType = Material.valueOf(itemConfig.getString("type", "BLAZRE_ROD"));
            List<String> itemLore = itemConfig.getStringList("lore");
            int itemSlot = itemConfig.getInt("slot", 6);

            // Create the item stack
            ItemStack item = new ItemStack(itemType);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemName);
                // Translate lore color codes
                List<String> translatedLore = new ArrayList<>();
                for (String line : itemLore) {
                    String lore_color = ChatColor.translateAlternateColorCodes('&', line);
                    translatedLore.add(lore_color);
                }
                meta.setLore(translatedLore);
                item.setItemMeta(meta);
            }

            // Give the item to the player
            player.getInventory().setItem(itemSlot, item);
        } else {
            plugin.getLogger().warning("Configuration for server selector item not found!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && isServerSelectorItem(item)) {
            event.setCancelled(true); // Prevent normal item use

            Player player = event.getPlayer();
            player.performCommand("selector");
        } else if (item != null && isHiderItem(item)) {
            event.setCancelled(true); // Prevent normal item use

            Player player = event.getPlayer();
            player.performCommand("hider");
        }
    }

    private boolean isServerSelectorItem(ItemStack item) {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("server-selector.settings")) {
            String itemType = config.getString("server-selector.settings.type", "COMPASS");
            Material expectedType = Material.matchMaterial(itemType);
            if (expectedType != null && item.getType() == expectedType) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String expectedName = config.getString("server-selector.settings.name", "Server Selector");
                    expectedName = ChatColor.translateAlternateColorCodes('&', expectedName); // Translate color codes
                    String displayName = meta.getDisplayName();
                    displayName = ChatColor.translateAlternateColorCodes('&', displayName); // Translate color codes
                    return displayName.equals(expectedName);
                }
            }
        }
        return false;
    }

    private boolean isHiderItem(ItemStack item) {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("player-hider.settings")) {
            String itemType = config.getString("player-hider.settings.type", "BLAZE_ROD");
            Material expectedType = Material.matchMaterial(itemType);
            if (expectedType != null && item.getType() == expectedType) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String expectedName = config.getString("player-hider.settings.name", "Player Hider");
                    expectedName = ChatColor.translateAlternateColorCodes('&', expectedName); // Translate color codes
                    String displayName = meta.getDisplayName();
                    displayName = ChatColor.translateAlternateColorCodes('&', displayName); // Translate color codes
                    return displayName.equals(expectedName);
                }
            }
        }
        return false;
    }
}
