package org.bcnlab.beaconlabslobby.commands;

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

import java.util.List;

public class HiderCommand implements CommandExecutor, Listener {

    private final BeaconLabsLobby plugin;

    public HiderCommand(BeaconLabsLobby plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin); // Register events
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        openHiderGUI(player);
        return true;
    }

    // Method to open the GUI for the player
    public void openHiderGUI(Player player) {
        HiderGUI gui = new HiderGUI(player);
        player.openInventory(gui.getInventory());
    }

    // Hider GUI class implementing InventoryHolder
    private class HiderGUI implements InventoryHolder {

        private final Inventory inventory;

        public HiderGUI(Player player) {
            FileConfiguration config = plugin.getConfig();
            ConfigurationSection settings = config.getConfigurationSection("player-hider.settings");

            String title = settings.getString("name", "&6BeaconLabs &8» &aPlayer Hider");
            List<String> lore = settings.getStringList("lore");
            int slot = settings.getInt("slot", 0);

            this.inventory = Bukkit.createInventory(this, 9, ChatColor.translateAlternateColorCodes('&', title));

            // Create red dye (off) item
            ItemStack redDye = new ItemStack(Material.REDSTONE);
            ItemMeta redMeta = redDye.getItemMeta();
            if (redMeta != null) {
                redMeta.setDisplayName(ChatColor.RED + "Hide Players");
                redDye.setItemMeta(redMeta);
            }
            this.inventory.setItem(2, redDye);

            // Create green dye (on) item
            ItemStack greenDye = new ItemStack(Material.EMERALD);
            ItemMeta greenMeta = greenDye.getItemMeta();
            if (greenMeta != null) {
                greenMeta.setDisplayName(ChatColor.GREEN + "Show Players");
                greenDye.setItemMeta(greenMeta);
            }
            this.inventory.setItem(6, greenDye);
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
        if (clickedInventory == null || !(clickedInventory.getHolder() instanceof HiderGUI)) {
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

            if (meta.getDisplayName().equals(ChatColor.RED + "Hide Players")) {
                togglePlayerVisibility(player, true);
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are now hiding other players.");
            } else if (meta.getDisplayName().equals(ChatColor.GREEN + "Show Players")) {
                togglePlayerVisibility(player, false);
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "You are now showing other players.");
            }

            // Close the inventory after clicking
            player.closeInventory();
        }
    }

    // Method to toggle player visibility
    private void togglePlayerVisibility(Player player, boolean hide) {
        if (hide) {
            // Hide other players from the player
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    player.hidePlayer(onlinePlayer);
                }
            }
        } else {
            // Show other players to the player
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    player.showPlayer(onlinePlayer);
                }
            }
        }
    }


    // Handle inventory close events
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof HiderGUI) {
            Player player = (Player) event.getPlayer();
        }
    }
}
