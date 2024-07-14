package org.bcnlab.beaconlabslobby.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SelectorCommand implements CommandExecutor, Listener {

    private final BeaconLabsLobby plugin;

    public SelectorCommand(BeaconLabsLobby plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin); // Register events
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        openSelectorGUI(player);
        return true;
    }

    // Method to open the GUI for the player
    public void openSelectorGUI(Player player) {
        ServerSelectorGUI gui = new ServerSelectorGUI(player);
        player.openInventory(gui.getInventory());
    }

    // Server Selector GUI class implementing InventoryHolder
    private class ServerSelectorGUI implements InventoryHolder {

        private final Inventory inventory;

        public ServerSelectorGUI(Player player) {
            // Fetch settings from config
            FileConfiguration config = plugin.getConfig();
            ConfigurationSection settings = config.getConfigurationSection("server-selector.settings");

            // Determine number of rows and title from config
            int rows = settings.getInt("rows", 3);

            // Create inventory with specified rows and title
            this.inventory = Bukkit.createInventory(this, rows * 9, "Server Selector");

            // Load server items from config
            loadServerItems();
        }

        private void loadServerItems() {
            FileConfiguration config = plugin.getConfig();
            ConfigurationSection itemsSection = config.getConfigurationSection("server-selector.items");

            if (itemsSection == null) {
                plugin.getLogger().warning("No server selector items found in the config!");
                return;
            }

            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection == null) continue;

                String name = itemSection.getString("name");
                Material type = Material.matchMaterial(itemSection.getString("type", "COMPASS"));
                List<String> lore = itemSection.getStringList("lore");
                int slot = itemSection.getInt("slot", -1);
                String serverIdentifier = key; // This is the identifier in the config, e.g., "lobby", "knockbackffa"
                String serverName = itemSection.getString("server"); // This is the server name to connect to

                if (name == null || type == null || slot == -1 || serverIdentifier == null || serverName == null) {
                    plugin.getLogger().warning("Invalid configuration for server selector item: " + key);
                    continue;
                }

                ItemStack item = new ItemStack(type);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

                    // Apply color codes and formatting to lore
                    List<String> formattedLore = new ArrayList<>();
                    for (String line : lore) {
                        formattedLore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    meta.setLore(formattedLore);

                    item.setItemMeta(meta);
                }

                inventory.setItem(slot, item);
            }
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    // Handle inventory click events
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !(clickedInventory.getHolder() instanceof ServerSelectorGUI)) {
            return; // Ignore clicks in other inventories or non-GUI inventories
        }

        event.setCancelled(true); // Cancel the event to prevent item moving

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            Player player = (Player) event.getWhoClicked();
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) {
                return;
            }

            // Fetch server name from configuration based on item identifier
            String serverIdentifier = getServerIdentifier(clickedItem);
            String serverName = plugin.getConfig().getString("server-selector.items." + serverIdentifier + ".server");

            if (serverName == null) {
                plugin.getLogger().warning("Server name not found for item: " + serverIdentifier);
                return;
            }

            // Example: Send the player to another server using BungeeCord
            sendPlayerToServer(player, serverName);

            // Close the inventory after clicking
            player.closeInventory();
        }
    }

    // Helper method to get server identifier from clicked item
    private String getServerIdentifier(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }

        String displayName = meta.getDisplayName();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection itemsSection = config.getConfigurationSection("server-selector.items");

        if (itemsSection == null) {
            return null;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            String name = itemSection.getString("name");
            if (name != null && ChatColor.translateAlternateColorCodes('&', name).equals(displayName)) {
                return key;
            }
        }

        return null;
    }

    // Method to send the player to another server using BungeeCord
    private void sendPlayerToServer(Player player, String serverName) {
        plugin.getLogger().info("Connecting player to server: " + serverName);
        player.sendMessage(plugin.getPrefix() + "§cYou are being connected to §6" + serverName);

        // Send player to another server
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName); // Server name as defined in BungeeCord config

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    // Handle inventory close events
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ServerSelectorGUI) {
            Player player = (Player) event.getPlayer();
        }
    }
}
