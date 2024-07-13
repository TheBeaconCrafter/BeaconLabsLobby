package org.bcnlab.beaconlabslobby;

import org.bcnlab.beaconlabslobby.commands.SelectorCommand;
import org.bcnlab.beaconlabslobby.gui.ServerSelectorGUI;
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
        getServer().getPluginManager().registerEvents(new ServerSelectorGUI(this), this);

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
            config.set("items.server-selector.name", "Server Selector");
            config.set("items.server-selector.type", Material.COMPASS.toString());

            List<String> lore = new ArrayList<>();
            lore.add("Right-click to select a server");
            lore.add("Use this to navigate between servers");
            config.set("items.server-selector.lore", lore);
        }

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
