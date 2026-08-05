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
import net.kyori.adventure.text.format.TextDecoration;

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

            String title = settings.getString("name", "<gold>BeaconLabs</gold> <dark_gray>»</dark_gray> <green>Player Hider</green>");
            this.inventory = Bukkit.createInventory(this, 27, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(title).decoration(TextDecoration.ITALIC, false));

            // Create red dye (off) item
            ItemStack redDye = new ItemStack(Material.REDSTONE);
            ItemMeta redMeta = redDye.getItemMeta();
            if (redMeta != null) {
                redMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Hide Players").decoration(TextDecoration.ITALIC, false));
                redDye.setItemMeta(redMeta);
            }
            this.inventory.setItem(11, redDye);

            // Create green dye (on) item
            ItemStack greenDye = new ItemStack(Material.EMERALD);
            ItemMeta greenMeta = greenDye.getItemMeta();
            if (greenMeta != null) {
                greenMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Show Players").decoration(TextDecoration.ITALIC, false));
                greenDye.setItemMeta(greenMeta);
            }
            this.inventory.setItem(15, greenDye);
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

            if (meta.hasDisplayName() && net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName()).equals("Hide Players")) {
                togglePlayerVisibility(player, true);
                plugin.sendMessage(player, "<red>You are now hiding other players.</red>");
            } else if (meta.hasDisplayName() && net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName()).equals("Show Players")) {
                togglePlayerVisibility(player, false);
                plugin.sendMessage(player, "<green>You are now showing other players.</green>");
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
