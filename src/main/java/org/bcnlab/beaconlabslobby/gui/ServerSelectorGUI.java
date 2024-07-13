package org.bcnlab.beaconlabslobby.gui;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ServerSelectorGUI implements Listener {

    private final BeaconLabsLobby plugin;

    public ServerSelectorGUI(BeaconLabsLobby plugin) {
        this.plugin = plugin;
    }

    // Example method to open GUI
    public void openServerSelectorGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, "Server Selector");

        // Example server options (you can load these dynamically from config or other sources)
        ItemStack server1 = createServerItem("Server 1", Material.DIAMOND_SWORD);
        ItemStack server2 = createServerItem("Server 2", Material.GOLDEN_SWORD);
        ItemStack server3 = createServerItem("Server 3", Material.IRON_SWORD);

        // Add server items to the GUI
        gui.setItem(10, server1);
        gui.setItem(12, server2);
        gui.setItem(14, server3);

        player.openInventory(gui);
    }

    // Create an ItemStack for a server option
    private ItemStack createServerItem(String serverName, Material material) {
        ItemStack item = new ItemStack(material);
        item.getItemMeta().setDisplayName(serverName);
        // Add lore or other metadata if needed
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getInventory().getHolder();

        if (event.getView().getTitle().equals("Server Selector")) {
            event.setCancelled(true); // Prevent player from taking items out of the GUI

            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Example: handle clicking on server items
            switch (clickedItem.getType()) {
                case DIAMOND_SWORD:
                    player.sendMessage(plugin.getPrefix() + "Connecting to Server 1...");
                    // Implement your logic to connect player to Server 1
                    break;
                case GOLDEN_SWORD:
                    player.sendMessage(plugin.getPrefix() + "Connecting to Server 2...");
                    // Implement your logic to connect player to Server 2
                    break;
                case IRON_SWORD:
                    player.sendMessage(plugin.getPrefix() + "Connecting to Server 3...");
                    // Implement your logic to connect player to Server 3
                    break;
                default:
                    break;
            }

            player.closeInventory(); // Close the GUI after selection
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("Server Selector")) {
            // Handle actions when the server selector GUI is closed
            // This can include cleanup or additional logic
        }
    }
}
