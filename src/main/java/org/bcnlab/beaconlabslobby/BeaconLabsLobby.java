package org.bcnlab.beaconlabslobby;

import org.bcnlab.beaconlabslobby.commands.SelectorCommand;
import org.bcnlab.beaconlabslobby.listeners.PlayerJoinListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BeaconLabsLobby extends JavaPlugin implements PluginMessageListener {

    private String pluginPrefix;
    private String pluginVersion = "1.0";
    private String noPermsMessage = "&cYou do not have permission to use this command.";

    @Override
    public void onEnable() {
        // Save the default config if it doesn't exist
        createDefaultConfig();
        // Load the configuration
        loadConfig();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getCommand("selector").setExecutor(new SelectorCommand(this));

        getLogger().info("BeaconLabs Lobby was enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BeaconLabs Lobby was disabled!");
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', pluginPrefix);
    }

    public String getVersion() {
        return pluginVersion;
    }

    // Method to create the default configuration if it doesn't exist
    private void createDefaultConfig() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        config.addDefault("plugin-prefix", "&6BeaconLabs &8» ");

        // Set default values for server selector item
        if (!config.contains("items.server-selector")) {
            config.set("items.server-selector", new ArrayList<>());

            // Default item configuration
            ConfigurationSection defaultItem = config.createSection("items.server-selector.default");
            defaultItem.set("name", "Server Selector");
            defaultItem.set("type", Material.COMPASS.toString());
            defaultItem.set("lore", Arrays.asList("Right-click to select a server", "Use this to navigate between servers"));
            defaultItem.set("slot", 11);
            defaultItem.set("server", "default_server_name");

            // Additional server selector items
            ConfigurationSection item1 = config.createSection("items.server-selector.item1");
            item1.set("name", "Server 1");
            item1.set("type", Material.DIAMOND.toString());
            item1.set("lore", Arrays.asList("&7Click to join Server 1", "&eThis is a lore line for Server 1"));
            item1.set("slot", 13);
            item1.set("server", "server1");

            ConfigurationSection item2 = config.createSection("items.server-selector.item2");
            item2.set("name", "Server 2");
            item2.set("type", Material.GOLD_INGOT.toString());
            item2.set("lore", Arrays.asList("&7Click to join Server 2", "&eThis is a lore line for Server 2"));
            item2.set("slot", 15);
            item2.set("server", "server2");

            // Add more items as needed
        }

        // Save the updated configuration
        saveConfig();
    }


    // Method to load the configuration
    private void loadConfig() {
        FileConfiguration config = getConfig();

        // Load plugin prefix from config
        pluginPrefix = config.getString("plugin-prefix", "&6BeaconLabs &8» ");
    }

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        getLogger().info("Btw Bungee just sent you a message :P");
    }
}
