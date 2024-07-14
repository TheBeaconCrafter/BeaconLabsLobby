package org.bcnlab.beaconlabslobby.listeners;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
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

        plugin.getLogger().info("Player joined");

        // Clear player's inventory
        clearInventory(player);

        // Give configurable items on join
        giveServerSelectorItem(player);
        // Add more items if needed

        // Teleport player to spawn
        teleportPlayerToSpawn(player);
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
        if (config.contains("items.server-selector")) {
            ConfigurationSection itemConfig = config.getConfigurationSection("items.server-selector");

            // Get item details from configuration
            String itemName = itemConfig.getString("name", "Server Selector");
            Material itemType = Material.valueOf(itemConfig.getString("type", "COMPASS"));
            List<String> itemLore = itemConfig.getStringList("lore");

            // Create the item stack
            ItemStack item = new ItemStack(itemType);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getPrefix() + itemName);
                // Translate lore color codes
                List<String> translatedLore = new ArrayList<>();
                for (String line : itemLore) {
                    translatedLore.add(plugin.getPrefix() + line);
                }
                meta.setLore(translatedLore);
                item.setItemMeta(meta);
            }

            // Give the item to the player
            player.getInventory().addItem(item);
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
        }
    }

    private boolean isServerSelectorItem(ItemStack item) {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("items.server-selector")) {
            String itemType = config.getString("items.server-selector.type", "COMPASS");
            Material expectedType = Material.matchMaterial(itemType);
            if (expectedType != null && item.getType() == expectedType) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String expectedName = plugin.getPrefix() + config.getString("items.server-selector.name", "Server Selector");
                    return meta.hasDisplayName() && meta.getDisplayName().equals(expectedName);
                }
            }
        }
        return false;
    }
}
