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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class PrivateSelectorCommand implements CommandExecutor, Listener {

    private static final Pattern ONLINE_PLAYERS_PATTERN = Pattern.compile("%on_players_([^%]+)%");
    private static final Pattern MAX_PLAYERS_PATTERN = Pattern.compile("%max_players_([^%]+)%");
    private final BeaconLabsLobby plugin;

    public PrivateSelectorCommand(BeaconLabsLobby plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, "<red>Only players can use this command!</red>");
            return true;
        }

        Player player = (Player) sender;

        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("private-server-selector.enabled", false)) {
            plugin.sendMessage(player, "<red>Private selector is disabled.</red>");
            return true;
        }

        String permission = config.getString("private-server-selector.permission", "beaconlabslobby.privateselector");
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            plugin.sendMessage(player, "<red>You do not have permission to use this selector.</red>");
            return true;
        }

        PrivateServerSelectorGUI gui = new PrivateServerSelectorGUI(player);
        player.openInventory(gui.getInventory());
        return true;
    }

    private class PrivateServerSelectorGUI implements InventoryHolder {

        private final Inventory inventory;

        public PrivateServerSelectorGUI(Player player) {
            FileConfiguration config = plugin.getConfig();
            ConfigurationSection settings = config.getConfigurationSection("private-server-selector.settings");

            int rows = settings != null ? settings.getInt("rows", 3) : 3;
            String name = settings != null ? settings.getString("name", "Private Selector") : "Private Selector";
            net.kyori.adventure.text.Component title = plugin.getMiniMessage().deserialize(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);

            this.inventory = Bukkit.createInventory(this, rows * 9, title);

            loadServerItems(player);
        }

        private final Map<Integer, String> serverNamesBySlot = new HashMap<>();

        private void loadServerItems(Player queryPlayer) {
            FileConfiguration config = plugin.getConfig();
            ConfigurationSection itemsSection = config.getConfigurationSection("private-server-selector.items");

            if (itemsSection == null) {
                plugin.getLogger().warning("No private server selector items found in the config!");
                return;
            }

            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection == null) continue;

                String name = itemSection.getString("name");
                Material type = Material.matchMaterial(itemSection.getString("type", "NETHER_STAR"));
                List<String> lore = itemSection.getStringList("lore");
                int slot = itemSection.getInt("slot", -1);
                String serverIdentifier = key;
                String serverName = itemSection.getString("server");

                if (name == null || type == null || slot == -1 || serverIdentifier == null || serverName == null) {
                    plugin.getLogger().warning("Invalid configuration for private selector item: " + key);
                    continue;
                }

                ItemStack item = new ItemStack(type);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(plugin.getMiniMessage().deserialize(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

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
                        
                        formattedLore.add(plugin.getMiniMessage().deserialize(processedLine).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                    }
                    meta.lore(formattedLore);
                    item.setItemMeta(meta);

                    inventory.setItem(slot, item);
                    serverNamesBySlot.put(slot, serverName);

                    if (hasOnlinePlaceholder) {
                        for (String srv : serversToQuery) {
                            SelectorCommand.addPendingOnlineRequest(srv, slot, inventory, queryPlayer, plugin, lore, name, type, serverName);
                        }
                    }
                }
            }
        }

        private String getServerName(int slot) {
            return serverNamesBySlot.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !(clickedInventory.getHolder() instanceof PrivateServerSelectorGUI)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            Player player = (Player) event.getWhoClicked();
            PrivateServerSelectorGUI gui = (PrivateServerSelectorGUI) clickedInventory.getHolder();
            String serverName = gui.getServerName(event.getSlot());

            if (serverName == null) {
                return;
            }

            sendPlayerToServer(player, serverName);
            player.closeInventory();
        }
    }

    private void sendPlayerToServer(Player player, String serverName) {
        plugin.getLogger().info("Connecting player to private server: " + serverName);
        plugin.sendMessage(player, "<gray>You are being connected to <gold>" + serverName + "</gold></gray>");

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof PrivateServerSelectorGUI) {
            Player player = (Player) event.getPlayer();
        }
    }
}

