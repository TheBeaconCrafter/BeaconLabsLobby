package org.bcnlab.beaconlabslobby;

import org.bcnlab.beaconlabslobby.commands.*;
import org.bcnlab.beaconlabslobby.listeners.BuildListener;
import org.bcnlab.beaconlabslobby.listeners.LobbyProtectionListener;
import org.bcnlab.beaconlabslobby.listeners.PlayerJoinListener;
import org.bcnlab.beaconlabslobby.managers.BuildManager;
import org.bcnlab.beaconlabslobby.managers.InventoryListener;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.ArrayList;
import java.util.Arrays;

public final class BeaconLabsLobby extends JavaPlugin implements PluginMessageListener {

    private String pluginPrefix;
    private String pluginVersion = "1.1";
    private String noPermsMessage = "&cYou do not have permission to use this command.";
    private BuildManager buildManager;
    private Location spawnLocation;

    private Boolean returnToSpawn;
    private Boolean healOnJoin;

    @Override
    public void onEnable() {
        // Save the default config if it doesn't exist
        createDefaultConfig();
        // Load the configuration
        loadConfig();

        // Initialize BuildManager
        buildManager = new BuildManager();

        if (getConfig().getBoolean("disable-weather", true)) {
            setDefaultWeather();
        }

        if (getConfig().getBoolean("disable-time", true)) {
            setDefaultTime();
        }

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(buildManager), this);

        BuildListener buildListener = new BuildListener(this, buildManager);
        getServer().getPluginManager().registerEvents(buildListener, this);

        getCommand("selector").setExecutor(new SelectorCommand(this));
        getCommand("labslobby").setExecutor(new LabsLobbyCommand(this));
        getCommand("build").setExecutor(new BuildCommand(this, buildManager));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        getCommand("hider").setExecutor(new HiderCommand(this));
        getCommand("refreshscoreboard").setExecutor(new RefreshScoreboardCommand(this));

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

    private void createDefaultConfig() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        config.addDefault("plugin-prefix", "&6BeaconLabs &8» ");
        config.addDefault("disable-damage", true);
        config.addDefault("disable-mob-spawning", true);
        config.addDefault("disable-food-level-change", true);
        config.addDefault("heal-on-join", true);
        config.addDefault("return-spawn-heightlimit", true);
        config.addDefault("disable-weather", true);
        config.addDefault("disable-time", true);
        config.addDefault("default-weather", "clear");
        config.addDefault("default-time", 1000);

        if (!config.contains("server-selector.settings")) {
            ConfigurationSection settingsSection = config.createSection("server-selector.settings");
            settingsSection.set("name", "&6BeaconLabs &8» &4Server Selector");
            settingsSection.set("type", Material.COMPASS.toString());
            settingsSection.set("lore", Arrays.asList("&aLeft-click to select a server", "&4You can find all your favorite gamemodes here!"));
            settingsSection.set("rows", 3);
            settingsSection.set("slot", 2);
        }

        // Set default values for server selector item
        if (!config.contains("server-selector.items")) {
            config.set("server-selector.items", new ArrayList<>());

            // Default item configuration
            ConfigurationSection defaultItem = config.createSection("server-selector.items.server1");
            defaultItem.set("name", "&2Server 1");
            defaultItem.set("type", Material.COMPASS.toString());
            defaultItem.set("lore", Arrays.asList("Left-click to select a server", "&4You can even use color codes!"));
            defaultItem.set("slot", 11);
            defaultItem.set("server", "server_in_bungeeconfig");

            // Additional server selector items
            ConfigurationSection item1 = config.createSection("server-selector.items.server3");
            item1.set("name", "&6Server 2");
            item1.set("type", Material.DIAMOND.toString());
            item1.set("lore", Arrays.asList("&7Click to join Server 1", "&eThis is a lore line for Server 1"));
            item1.set("slot", 13);
            item1.set("server", "server2");

            ConfigurationSection item2 = config.createSection("server-selector.items.server2");
            item2.set("name", "&4Server 3");
            item2.set("type", Material.GOLD_INGOT.toString());
            item2.set("lore", Arrays.asList("&7Click to join Server 2", "&eThis is a lore line for Server 2"));
            item2.set("slot", 15);
            item2.set("server", "server3");
        }

        // Set default values for server selector item
        if (!config.contains("player-hider.settings")) {
            ConfigurationSection settingsSection = config.createSection("player-hider.settings");
            settingsSection.set("name", "&6BeaconLabs &8» &aPlayer Hider");
            settingsSection.set("type", Material.BLAZE_ROD.toString());
            settingsSection.set("lore", Arrays.asList("&aLeft-click to hide players", "&4You can modify your player visibility settings here."));
            settingsSection.set("slot", 6);
        }

        // Save the updated configuration
        saveConfig();
    }


    private void loadConfig() {
        FileConfiguration config = getConfig();

        pluginPrefix = config.getString("plugin-prefix", "&6BeaconLabs &8» ");

        healOnJoin = config.getBoolean("heal-on-join", true);
        returnToSpawn = config.getBoolean("return-spawn-heightlimit", true);

        if (config.contains("spawn")) {
            this.spawnLocation = deserializeLocation(config.getString("spawn"));
        } else {
            getLogger().warning("Config does not contain a 'spawn' section.");
        }
    }

    public Boolean getReturnToSpawn () {
        return returnToSpawn;
    }

    public Boolean getHealOnJoin () {
        return healOnJoin;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public void loadSpawnLocation() {
        if (getConfig().contains("spawn")) {
            this.spawnLocation = deserializeLocation(getConfig().getString("spawn"));
        }
    }

    public void saveSpawnLocation() {
        if (spawnLocation != null) {
            getConfig().set("spawn", serializeLocation(spawnLocation));
            saveConfig();
        }
    }

    private void setDefaultWeather() {
        String weather = getConfig().getString("default-weather", "clear");

        for (World world : Bukkit.getWorlds()) {
            switch (weather.toLowerCase()) {
                case "rain":
                    world.setStorm(true);
                    world.setThundering(false);
                    world.setWeatherDuration(6000);
                    break;
                case "thunder":
                    world.setStorm(true);
                    world.setThundering(true);
                    world.setWeatherDuration(6000);
                    break;
                case "clear":
                default:
                    world.setStorm(false);
                    world.setThundering(false);
                    world.setWeatherDuration(0);
                    break;
            }
            getLogger().info("Weather set to " + weather + " in world: " + world.getName());
        }
    }

    private void setDefaultTime() {
        long defaultTime = getConfig().getLong("default-time", 1000);

        for (World world : Bukkit.getWorlds()) {
            world.setTime(defaultTime);
        }
        getLogger().info("Time set to " + defaultTime + " in all worlds.");
    }

    public String serializeLocation(Location location) {
        if (location == null) return null;
        return location.getWorld().getName() + "," +
                location.getX() + "," +
                location.getY() + "," +
                location.getZ() + "," +
                location.getYaw() + "," +
                location.getPitch();
    }

    public Location deserializeLocation(String serialized) {
        if (serialized == null || serialized.isEmpty()) return null;
        String[] parts = serialized.split(",");
        if (parts.length == 6) {
            return new Location(getServer().getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5]));
        }
        return null;
    }

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        getLogger().info("Btw Bungee just sent you a message :P");
    }
}
