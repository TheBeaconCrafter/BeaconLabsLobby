package org.bcnlab.beaconlabslobby.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
            this.inventory = Bukkit.createInventory(this, 27, "Server Selector");

            // Example server items
            ItemStack serverItem1 = createServerItem("Server 1", Material.DIAMOND);
            ItemStack serverItem2 = createServerItem("knockbackffa", Material.GOLD_INGOT);
            ItemStack serverItem3 = createServerItem("Server 3", Material.IRON_INGOT);

            // Add items to GUI
            inventory.setItem(11, serverItem1);
            inventory.setItem(13, serverItem2);
            inventory.setItem(15, serverItem3);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    // Method to create server items
    private ItemStack createServerItem(String serverName, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(serverName);
            List<String> lore = new ArrayList<>();
            lore.add("Click to join " + serverName);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
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
            String serverName = clickedItem.getItemMeta().getDisplayName(); // Assuming display name is the server name

            // Example: Send the player to another server using BungeeCord
            sendPlayerToServer(player, serverName);

            // Close the inventory after clicking
            player.closeInventory();
        }
    }

    // Method to send the player to another server using BungeeCord
    private void sendPlayerToServer(Player player, String serverName) {
        plugin.getLogger().info("Connecting player to server");

        // Send player to another server
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName); // Server name as defined in BungeeCord config

        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    // Handle inventory close events
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof ServerSelectorGUI) {
            Player player = (Player) event.getPlayer();
            player.sendMessage("You closed the server selector GUI.");
            plugin.getLogger().info("Holder is Server Selector");
        } else {
            plugin.getLogger().info("Holder is NOT Server Selector");
        }
    }

}
