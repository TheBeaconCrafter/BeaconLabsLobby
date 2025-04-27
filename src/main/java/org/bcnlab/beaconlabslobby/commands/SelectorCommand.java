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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class SelectorCommand implements CommandExecutor, Listener {
    // Track pending requests per server (supporting multiple requests per server)
    private static final Map<String, List<PendingOnlineRequest>> pendingOnlineRequests = new HashMap<>();
    private static final long ONLINE_REQUEST_TIMEOUT = 2500; // ms

    // Data structure to hold info needed to update the GUI
    private static class PendingOnlineRequest {
        public final int slot;
        public final Inventory inventory;
        public final List<String> lore;
        public final String name;
        public final Material type;
        public final long timestamp;
        public PendingOnlineRequest(int slot, Inventory inventory, List<String> lore, String name, Material type) {
            this.slot = slot;
            this.inventory = inventory;
            this.lore = lore;
            this.name = name;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // Called from loadServerItems
    public static void addPendingOnlineRequest(String serverName, int slot, Inventory inventory, Player player, BeaconLabsLobby plugin, List<String> lore, String name, Material type) {
        // Store the info for later update (support multiple requests per server)
        pendingOnlineRequests.computeIfAbsent(serverName, k -> new ArrayList<>())
            .add(new PendingOnlineRequest(slot, inventory, lore, name, type));
        // Send BungeeCord ServerIP request
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ServerIP");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        // Schedule timeout for offline fallback
        Bukkit.getScheduler().runTaskLater(plugin, () -> handleOnlineTimeout(serverName), ONLINE_REQUEST_TIMEOUT / 50); // convert ms to ticks
    }

    // Called from BeaconLabsLobby.onPluginMessageReceived
    public static void handleOnlineResponse(String serverName) {
        List<PendingOnlineRequest> reqs = pendingOnlineRequests.remove(serverName);
        if (reqs != null) {
            for (PendingOnlineRequest req : reqs) {
                updateItemLoreStatic(req.inventory, req.slot, req.lore, ChatColor.GREEN + "Online");
            }
        }
    }

    // Called from BeaconLabsLobby.onPluginMessageReceived for offline servers
    public static void handleOfflineResponse(String serverName) {
        List<PendingOnlineRequest> reqs = pendingOnlineRequests.remove(serverName);
        if (reqs != null) {
            for (PendingOnlineRequest req : reqs) {
                updateItemLoreStatic(req.inventory, req.slot, req.lore, ChatColor.RED + "Offline");
            }
        }
    }

    // Called on timeout if no response
    private static void handleOnlineTimeout(String serverName) {
        List<PendingOnlineRequest> reqs = pendingOnlineRequests.remove(serverName);
        if (reqs != null) {
            for (PendingOnlineRequest req : reqs) {
                updateItemLoreStatic(req.inventory, req.slot, req.lore, ChatColor.RED + "Offline");
            }
        }
    }

    // Static helper to update lore from outside inner class
    private static void updateItemLoreStatic(Inventory inventory, int slot, List<String> lore, String onlineStatus) {
        ItemStack item = inventory.getItem(slot);
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> updatedLore = new ArrayList<>();
        for (String line : lore) {
            if (line.contains("%online%")) {
                updatedLore.add(ChatColor.translateAlternateColorCodes('&', line.replace("%online%", onlineStatus)));
            } else {
                updatedLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }
        meta.setLore(updatedLore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }


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
            String name = settings.getString("name", "Server Selector");
            String final_name = ChatColor.translateAlternateColorCodes('&', name);

            // Create inventory with specified rows and title
            this.inventory = Bukkit.createInventory(this, rows * 9, final_name);

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
                    boolean hasOnlinePlaceholder = false;
                    for (String line : lore) {
                        if (line.contains("%online%")) {
                            hasOnlinePlaceholder = true;
                            // Temporary placeholder (gray)
                            formattedLore.add(ChatColor.translateAlternateColorCodes('&', line.replace("%online%", ChatColor.GRAY + "Loading...")));
                        } else {
                            formattedLore.add(ChatColor.translateAlternateColorCodes('&', line));
                        }
                    }
                    meta.setLore(formattedLore);
                    item.setItemMeta(meta);

                    inventory.setItem(slot, item);

                    // If %online% is present, send PlayerCount request
                    if (hasOnlinePlaceholder) {
                        Player anyPlayer = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
                        if (anyPlayer != null) {
                            addPendingOnlineRequest(serverName, slot, inventory, anyPlayer, plugin, lore, name, type);
                        }
                    }
                }
            }
        }

        // Helper to update the lore of an item in the inventory
        private void updateItemLore(int slot, List<String> lore, String onlineStatus) {
            ItemStack item = inventory.getItem(slot);
            if (item == null) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            List<String> updatedLore = new ArrayList<>();
            for (String line : lore) {
                if (line.contains("%online%")) {
                    updatedLore.add(ChatColor.translateAlternateColorCodes('&', line.replace("%online%", onlineStatus)));
                } else {
                    updatedLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
            meta.setLore(updatedLore);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
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
