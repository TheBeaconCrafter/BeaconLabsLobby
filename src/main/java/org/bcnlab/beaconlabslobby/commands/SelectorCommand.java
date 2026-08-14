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
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class SelectorCommand implements CommandExecutor, Listener {
    // Track pending requests per server (supporting multiple requests per server)
    private static final Map<String, List<PendingOnlineRequest>> pendingOnlineRequests = new HashMap<>();
    private static final Set<String> inFlightRequests = new HashSet<>();
    private static final long ONLINE_REQUEST_TIMEOUT = 2500; // ms
    private static final long SERVER_INFO_CACHE_TTL = 2500; // ms
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern ONLINE_PLAYERS_PATTERN = Pattern.compile("%on_players_([^%]+)%");
    private static final Pattern MAX_PLAYERS_PATTERN = Pattern.compile("%max_players_([^%]+)%");

    // Cache responses so an item depending on multiple servers can render them all simultaneously
    private static final Map<String, ServerInfoCache> serverInfoCache = new HashMap<>();

    private static class ServerInfoCache {
        public final boolean online;
        public final int onlineCount;
        public final int maxCount;
        public final long timestamp;
        public ServerInfoCache(boolean online, int onlineCount, int maxCount) {
            this.online = online;
            this.onlineCount = onlineCount;
            this.maxCount = maxCount;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // Data structure to hold info needed to update the GUI
    private static class PendingOnlineRequest {
        public final int slot;
        public final Inventory inventory;
        public final List<String> lore;
        public final String name;
        public final Material type;
        public final String mainServer; // the original server name for %online%
        public final long timestamp;
        public PendingOnlineRequest(int slot, Inventory inventory, List<String> lore, String name, Material type, String mainServer) {
            this.slot = slot;
            this.inventory = inventory;
            this.lore = lore;
            this.name = name;
            this.type = type;
            this.mainServer = mainServer;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // Called from loadServerItems
    public static void addPendingOnlineRequest(String serverName, int slot, Inventory inventory, Player player, BeaconLabsLobby plugin, List<String> lore, String name, Material type, String mainServer) {
        // Store the info for later update (support multiple requests per server)
        pendingOnlineRequests.computeIfAbsent(serverName, k -> new ArrayList<>())
            .add(new PendingOnlineRequest(slot, inventory, lore, name, type, mainServer));

        ServerInfoCache cachedInfo = serverInfoCache.get(serverName);
        if (cachedInfo != null && System.currentTimeMillis() - cachedInfo.timestamp <= SERVER_INFO_CACHE_TTL) {
            flushPendingRequests(serverName);
            return;
        }

        // Only one request and timeout are needed while a server is being queried, even
        // when several selector items or players are waiting for the same response.
        if (!inFlightRequests.add(serverName)) {
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Request");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, "beaconlabs:server_info", out.toByteArray());
        Bukkit.getScheduler().runTaskLater(plugin, () -> handleOnlineTimeout(serverName), ONLINE_REQUEST_TIMEOUT / 50);
    }

    // Legacy handleOnlineResponse (keep if anything still uses it)
    public static void handleOnlineResponse(String serverName) {
        handleServerInfoResponse(serverName, true, 0, 0);
    }

    // Legacy handleOfflineResponse
    public static void handleOfflineResponse(String serverName) {
        handleServerInfoResponse(serverName, false, 0, 0);
    }
    
    // New handler for server info
    public static void handleServerInfoResponse(String serverName, boolean isOnline, int onlineCount, int maxCount) {
        serverInfoCache.put(serverName, new ServerInfoCache(isOnline, onlineCount, maxCount));
        inFlightRequests.remove(serverName);

        flushPendingRequests(serverName);
    }

    private static void flushPendingRequests(String serverName) {
        List<PendingOnlineRequest> reqs = pendingOnlineRequests.remove(serverName);
        if (reqs == null) {
            return;
        }
        for (PendingOnlineRequest req : reqs) {
            updateItemLoreStatic(req.inventory, req.slot, req.lore, req.mainServer);
        }
    }

    // Called on timeout if no response
    private static void handleOnlineTimeout(String serverName) {
        handleServerInfoResponse(serverName, false, 0, 0);
    }

    // Static helper to update lore from outside inner class
    private static void updateItemLoreStatic(Inventory inventory, int slot, List<String> lore, String mainServer) {
        ItemStack item = inventory.getItem(slot);
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<net.kyori.adventure.text.Component> updatedLore = new ArrayList<>(lore.size());
        ServerInfoCache mainInfo = serverInfoCache.get(mainServer);
        for (String line : lore) {
            String processedLine = line;

            if (processedLine.contains("%online%")) {
                if (mainInfo != null) {
                    processedLine = processedLine.replace("%online%", mainInfo.online ? "<green>Online</green>" : "<red>Offline</red>");
                } else {
                    processedLine = processedLine.replace("%online%", "<gray>Loading...</gray>");
                }
            }
            
            if (processedLine.contains("%on_players%")) {
                if (mainInfo != null) {
                    processedLine = processedLine.replace("%on_players%", String.valueOf(mainInfo.online ? mainInfo.onlineCount : 0));
                } else {
                    processedLine = processedLine.replace("%on_players%", "<gray>...</gray>");
                }
            }
            
            if (processedLine.contains("%max_players%")) {
                if (mainInfo != null) {
                    processedLine = processedLine.replace("%max_players%", String.valueOf(mainInfo.online ? mainInfo.maxCount : 0));
                } else {
                    processedLine = processedLine.replace("%max_players%", "<gray>...</gray>");
                }
            }
            
            java.util.regex.Matcher m1 = ONLINE_PLAYERS_PATTERN.matcher(processedLine);
            while (m1.find()) {
                String srv = m1.group(1);
                ServerInfoCache info = serverInfoCache.get(srv);
                if (info != null) {
                    processedLine = processedLine.replace(m1.group(0), String.valueOf(info.online ? info.onlineCount : 0));
                } else {
                    processedLine = processedLine.replace(m1.group(0), "<gray>...</gray>");
                }
            }
            
            java.util.regex.Matcher m2 = MAX_PLAYERS_PATTERN.matcher(processedLine);
            while (m2.find()) {
                String srv = m2.group(1);
                ServerInfoCache info = serverInfoCache.get(srv);
                if (info != null) {
                    processedLine = processedLine.replace(m2.group(0), String.valueOf(info.online ? info.maxCount : 0));
                } else {
                    processedLine = processedLine.replace(m2.group(0), "<gray>...</gray>");
                }
            }
            updatedLore.add(MINI_MESSAGE.deserialize(processedLine).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(updatedLore);
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
            plugin.sendMessage(sender, "<red>Only players can use this command!</red>");
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
            net.kyori.adventure.text.Component title = plugin.getMiniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false);

            // Create inventory with specified rows and title
            this.inventory = Bukkit.createInventory(this, rows * 9, title);

            // Load server items from config
            loadServerItems(player);
        }

        private final Map<Integer, String> serverNamesBySlot = new HashMap<>();

        private void loadServerItems(Player queryPlayer) {
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
                    meta.displayName(plugin.getMiniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false));

                    // Apply color codes and formatting to lore
                    List<net.kyori.adventure.text.Component> formattedLore = new ArrayList<>(lore.size());
                    boolean hasOnlinePlaceholder = false;
                    Set<String> serversToQuery = new HashSet<>();
                    
                    for (String line : lore) {
                        String processedLine = line;
                        if (processedLine.contains("%online%")) {
                            hasOnlinePlaceholder = true;
                            serversToQuery.add(serverName);
                            processedLine = processedLine.replace("%online%", "<gray>Loading...</gray>");
                        }
                        
                        if (processedLine.contains("%on_players%")) {
                            hasOnlinePlaceholder = true;
                            serversToQuery.add(serverName);
                            processedLine = processedLine.replace("%on_players%", "<gray>...</gray>");
                        }
                        
                        if (processedLine.contains("%max_players%")) {
                            hasOnlinePlaceholder = true;
                            serversToQuery.add(serverName);
                            processedLine = processedLine.replace("%max_players%", "<gray>...</gray>");
                        }
                        
                        // Check for %on_players_SERVER% and %max_players_SERVER%
                        java.util.regex.Matcher m1 = ONLINE_PLAYERS_PATTERN.matcher(processedLine);
                        while (m1.find()) {
                            hasOnlinePlaceholder = true;
                            String srv = m1.group(1);
                            serversToQuery.add(srv);
                            processedLine = processedLine.replace(m1.group(0), "<gray>...</gray>");
                        }
                        
                        java.util.regex.Matcher m2 = MAX_PLAYERS_PATTERN.matcher(processedLine);
                        while (m2.find()) {
                            hasOnlinePlaceholder = true;
                            String srv = m2.group(1);
                            serversToQuery.add(srv);
                            processedLine = processedLine.replace(m2.group(0), "<gray>...</gray>");
                        }
                        
                        formattedLore.add(plugin.getMiniMessage().deserialize(processedLine).decoration(TextDecoration.ITALIC, false));
                    }
                    meta.lore(formattedLore);
                    item.setItemMeta(meta);

                    inventory.setItem(slot, item);
                    serverNamesBySlot.put(slot, serverName);

                    // If online placeholders are present, send requests
                    if (hasOnlinePlaceholder) {
                        for (String srv : serversToQuery) {
                            addPendingOnlineRequest(srv, slot, inventory, queryPlayer, plugin, lore, name, type, serverName);
                        }
                    }
                }
            }
        }

        private String getServerName(int slot) {
            return serverNamesBySlot.get(slot);
        }

        // Helper to update the lore of an item in the inventory
        private void updateItemLore(int slot, List<String> lore, String onlineStatus) {
            ItemStack item = inventory.getItem(slot);
            if (item == null) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            List<net.kyori.adventure.text.Component> updatedLore = new ArrayList<>();
            for (String line : lore) {
                if (line.contains("%online%")) {
                    updatedLore.add(plugin.getMiniMessage().deserialize(line.replace("%online%", onlineStatus)).decoration(TextDecoration.ITALIC, false));
                } else {
                    updatedLore.add(plugin.getMiniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
                }
            }
            meta.lore(updatedLore);
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
            ServerSelectorGUI gui = (ServerSelectorGUI) clickedInventory.getHolder();
            String serverName = gui.getServerName(event.getSlot());

            if (serverName == null) {
                return;
            }

            // Example: Send the player to another server using BungeeCord
            sendPlayerToServer(player, serverName);

            // Close the inventory after clicking
            player.closeInventory();
        }
    }

    // Method to send the player to another server using BungeeCord
    private void sendPlayerToServer(Player player, String serverName) {
        plugin.getLogger().info("Connecting player to server: " + serverName);
        plugin.sendMessage(player, "<gray>You are being connected to <gold>" + serverName + "</gold></gray>");

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
