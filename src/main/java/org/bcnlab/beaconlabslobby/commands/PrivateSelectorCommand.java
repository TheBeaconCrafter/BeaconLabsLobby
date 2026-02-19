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

public class PrivateSelectorCommand implements CommandExecutor, Listener {

    private final BeaconLabsLobby plugin;

    public PrivateSelectorCommand(BeaconLabsLobby plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("private-server-selector.enabled", false)) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Private selector is disabled.");
            return true;
        }

        String permission = config.getString("private-server-selector.permission", "beaconlabslobby.privateselector");
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this selector.");
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
            String finalName = ChatColor.translateAlternateColorCodes('&', name);

            this.inventory = Bukkit.createInventory(this, rows * 9, finalName);

            loadServerItems(player);
        }

        private void loadServerItems(Player anyPlayer) {
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
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

                    List<String> formattedLore = new ArrayList<>();
                    boolean hasOnlinePlaceholder = false;
                    for (String line : lore) {
                        if (line.contains("%online%")) {
                            hasOnlinePlaceholder = true;
                            formattedLore.add(ChatColor.translateAlternateColorCodes('&', line.replace("%online%", ChatColor.GRAY + "Loading...")));
                        } else {
                            formattedLore.add(ChatColor.translateAlternateColorCodes('&', line));
                        }
                    }
                    meta.setLore(formattedLore);
                    item.setItemMeta(meta);

                    inventory.setItem(slot, item);

                    if (hasOnlinePlaceholder) {
                        Player firstOnline = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
                        if (firstOnline != null) {
                            SelectorCommand.addPendingOnlineRequest(serverName, slot, inventory, firstOnline, plugin, lore, name, type);
                        }
                    }
                }
            }
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
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) {
                return;
            }

            String serverIdentifier = getServerIdentifier(clickedItem);
            String serverName = plugin.getConfig().getString("private-server-selector.items." + serverIdentifier + ".server");

            if (serverName == null) {
                plugin.getLogger().warning("Server name not found for private selector item: " + serverIdentifier);
                return;
            }

            sendPlayerToServer(player, serverName);
            player.closeInventory();
        }
    }

    private String getServerIdentifier(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }

        String displayName = meta.getDisplayName();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection itemsSection = config.getConfigurationSection("private-server-selector.items");

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

    private void sendPlayerToServer(Player player, String serverName) {
        plugin.getLogger().info("Connecting player to private server: " + serverName);
        player.sendMessage(plugin.getPrefix() + "§cYou are being connected to §5" + serverName);

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

