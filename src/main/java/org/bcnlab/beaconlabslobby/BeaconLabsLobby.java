package org.bcnlab.beaconlabslobby;

import org.bcnlab.beaconlabslobby.commands.*;
import org.bcnlab.beaconlabslobby.listeners.BuildListener;
import org.bcnlab.beaconlabslobby.listeners.LobbyProtectionListener;
import org.bcnlab.beaconlabslobby.commands.LobbyNPCCommand;
import org.bcnlab.beaconlabslobby.listeners.PlayerJoinListener;
import org.bcnlab.beaconlabslobby.managers.BuildManager;
import org.bcnlab.beaconlabslobby.managers.InventoryListener;
import org.bcnlab.beaconlabslobby.managers.ItemManager;
import org.bcnlab.beaconlabslobby.managers.NPCManager;
import org.bcnlab.beaconlabslobby.commands.PrivateSelectorCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.WaterMob;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.util.ArrayList;
import java.util.Arrays;

public final class BeaconLabsLobby extends JavaPlugin implements PluginMessageListener {

    private String pluginPrefix;
    private String legacyPrefix;
    private String pluginVersion = "1.6.4";
    private String noPermsMessage = "&cYou do not have permission to use this command.";
    private BuildManager buildManager;
    private ItemManager itemManager;
    private NPCManager npcManager;
    private SelectorCommand selectorCommand;
    private MiniMessage miniMessage;
    private Location spawnLocation;

    private Integer heightlimitTop;
    private Integer heightlimitBottom;

    private Boolean returnToSpawn;
    private Boolean healOnJoin;

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void onEnable() {
        // Save the default config if it doesn't exist
        createDefaultConfig();
        // Load the configuration
        loadConfig();

        // Initialize variables
        buildManager = new BuildManager();
        itemManager = new ItemManager(this);
        
        if (getServer().getPluginManager().getPlugin("FancyNpcs") != null) {
            npcManager = new NPCManager(this);
            getLogger().info("FancyNpcs detected, enabling NPC functionality.");
        } else {
            getLogger().warning("FancyNpcs not found! NPC functionality will be disabled.");
        }
        
        miniMessage = MiniMessage.miniMessage();

        if (getConfig().getBoolean("disable-weather", true)) {
            setDefaultWeather();
        }

        if (getConfig().getBoolean("disable-time", true)) {
            setDefaultTime();
        }

        if (getConfig().getBoolean("disable-mob-spawning", true)) {
            for (World world : Bukkit.getWorlds()) {
                world.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
                world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Monster || entity instanceof Animals || entity instanceof Ambient || entity instanceof WaterMob || entity instanceof org.bukkit.entity.Slime || entity instanceof org.bukkit.entity.Ghast) {
                        entity.remove();
                    }
                }
            }
        }

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        
        // BeaconLabs server info
        getServer().getMessenger().registerOutgoingPluginChannel(this, "beaconlabs:server_info");
        getServer().getMessenger().registerIncomingPluginChannel(this, "beaconlabs:server_info", this);

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(buildManager), this);

        BuildListener buildListener = new BuildListener(this, buildManager);
        getServer().getPluginManager().registerEvents(buildListener, this);

        // Register commands via LifecycleEvents.COMMANDS (Paper API)
        BuildCommand buildCmd = new BuildCommand(this, buildManager);
        LobbyNPCCommand lobbyNpcCmd = npcManager != null ? new LobbyNPCCommand(this) : null;

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            org.bcnlab.beaconlabslobby.utils.CommandRegistry.registerAll(this, event.registrar());
        });
    }

    public String getPrefix() {
        return pluginPrefix;
    }

    public String getPrefix(CommandSender sender) {
        if (sender instanceof Player) {
            return getPrefix((Player) sender);
        }
        return pluginPrefix;
    }

    public String getPrefix(Player player) {
        boolean useLegacy = false;
        try {
            if (player.hasMetadata("protocol_version")) {
                int protocol = player.getMetadata("protocol_version").get(0).asInt();
                if (protocol <= 47) { // 1.8.x is protocol <= 47
                    useLegacy = true;
                }
            } else if (getServer().getPluginManager().isPluginEnabled("ViaVersion")) {
                int protocol = com.viaversion.viaversion.api.Via.getAPI().getPlayerVersion(player.getUniqueId());
                if (protocol <= 47) {
                    useLegacy = true;
                }
            }
        } catch (Exception ignored) {}
        
        if (useLegacy) {
            return legacyPrefix.replace("&", "§");
        }
        return pluginPrefix;
    }

    public void sendMessage(CommandSender sender, String messageString) {
        String prefixStr = getPrefix(sender);
        net.kyori.adventure.text.Component prefixComp;
        if (prefixStr.contains("§")) {
             prefixComp = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(prefixStr);
        } else {
             prefixComp = miniMessage.deserialize(prefixStr);
        }
        net.kyori.adventure.text.Component msgComp;
        if (messageString.contains("§") || messageString.contains("&")) {
            msgComp = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(messageString.replace("&", "§"));
        } else {
            msgComp = miniMessage.deserialize(messageString);
        }
        sender.sendMessage(prefixComp.append(msgComp));
    }

    public String getVersion() {
        return pluginVersion;
    }

    private void createDefaultConfig() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        config.addDefault("plugin-prefix", "<gradient:gold:yellow>BeaconLabs</gradient> <dark_gray>»</dark_gray> ");
        config.addDefault("disable-damage", true);
        config.addDefault("disable-mob-spawning", true);
        config.addDefault("disable-food-level-change", true);
        config.addDefault("heal-on-join", true);
        config.addDefault("return-spawn-heightlimit", true);
        config.addDefault("heightlimit-top", 256);
        config.addDefault("heightlimit-bottom", 10);
        config.addDefault("disable-weather", true);
        config.addDefault("disable-time", true);
        config.addDefault("default-weather", "clear");
        config.addDefault("default-time", 1000);

        if (!config.contains("server-selector.settings")) {
            ConfigurationSection settingsSection = config.createSection("server-selector.settings");
            settingsSection.set("name", "<gold>Play</gold> <gray>(Right Click)</gray>");
            settingsSection.set("type", Material.COMPASS.toString());
            settingsSection.set("lore", Arrays.asList("<green>Left-click to select a server</green>", "<gray>Find all your favorite gamemodes here!</gray>"));
            settingsSection.set("rows", 3);
            settingsSection.set("slot", 2);
        }

        // Set default values for primary server selector items
        if (!config.contains("server-selector.items")) {
            config.set("server-selector.items", new ArrayList<>());

            // Default item configuration
            ConfigurationSection defaultItem = config.createSection("server-selector.items.server1");
            defaultItem.set("name", "<dark_green><bold>Server 1</bold></dark_green>");
            defaultItem.set("type", Material.COMPASS.toString());
            defaultItem.set("lore", Arrays.asList("<green>Left-click to select a server</green>", "<red>You can use minimessage codes!</red>"));
            defaultItem.set("slot", 11);
            defaultItem.set("server", "server_in_bungeeconfig");

            // Additional server selector items
            ConfigurationSection item1 = config.createSection("server-selector.items.server3");
            item1.set("name", "<gold><bold>Server 2</bold></gold>");
            item1.set("type", Material.DIAMOND.toString());
            item1.set("lore", Arrays.asList("<gray>Click to join Server 1</gray>", "<yellow>This is a lore line for Server 1</yellow>"));
            item1.set("slot", 13);
            item1.set("server", "server2");

            ConfigurationSection item2 = config.createSection("server-selector.items.server2");
            item2.set("name", "<dark_red><bold>Server 3</bold></dark_red>");
            item2.set("type", Material.GOLD_INGOT.toString());
            item2.set("lore", Arrays.asList("<gray>Click to join Server 2</gray>", "<yellow>This is a lore line for Server 2</yellow>"));
            item2.set("slot", 15);
            item2.set("server", "server3");
        }

        // Defaults for private server selector (second selector)
        if (!config.contains("private-server-selector.enabled")) {
            config.set("private-server-selector.enabled", false);
        }
        if (!config.contains("private-server-selector.permission")) {
            config.set("private-server-selector.permission", "beaconlabslobby.privateselector");
        }
        if (!config.contains("private-server-selector.settings")) {
            ConfigurationSection privateSettings = config.createSection("private-server-selector.settings");
            privateSettings.set("name", "<dark_purple>Private Selector</dark_purple> <gray>(Right Click)</gray>");
            privateSettings.set("type", Material.NETHER_STAR.toString());
            privateSettings.set("lore", Arrays.asList("<light_purple>Private server selector</light_purple>", "<gray>Only visible with permission</gray>"));
            privateSettings.set("rows", 3);
            privateSettings.set("slot", 4);
        }

        if (!config.contains("private-server-selector.items")) {
            config.set("private-server-selector.items", new ArrayList<>());

            ConfigurationSection privateItem = config.createSection("private-server-selector.items.private1");
            privateItem.set("name", "<dark_purple><bold>Private Server 1</bold></dark_purple>");
            privateItem.set("type", Material.NETHER_STAR.toString());
            privateItem.set("lore", Arrays.asList("<gray>Click to join</gray>", "<yellow>This server is private</yellow>", "<gray>Status: %online%</gray>"));
            privateItem.set("slot", 11);
            privateItem.set("server", "private_server_1");
        }

        // Set default values for player hider item
        if (!config.contains("player-hider.settings")) {
            ConfigurationSection settingsSection = config.createSection("player-hider.settings");
            settingsSection.set("name", "<green>Visibility</green> <gray>(Right Click)</gray>");
            settingsSection.set("type", Material.BLAZE_ROD.toString());
            settingsSection.set("lore", Arrays.asList("<green>Left-click to toggle player visibility</green>"));
            settingsSection.set("slot", 6);
        }

        // Settings item (SettingsManager proxy fallback or UI)
        if (!config.contains("settings.settings")) {
            ConfigurationSection settingsSection = config.createSection("settings.settings");
            settingsSection.set("name", "<gray>Settings</gray> <gray>(Right Click)</gray>");
            settingsSection.set("type", Material.COMPARATOR.toString());
            settingsSection.set("lore", Arrays.asList("<yellow>Left-click to configure your settings</yellow>"));
            settingsSection.set("slot", 8);
        }

        // Friends item
        if (!config.contains("friends.settings")) {
            ConfigurationSection settingsSection = config.createSection("friends.settings");
            settingsSection.set("name", "<aqua>Friends</aqua> <gray>(Right Click)</gray>");
            settingsSection.set("type", Material.PLAYER_HEAD.toString());
            settingsSection.set("lore", Arrays.asList("<aqua>Left-click to open friends list</aqua>"));
            settingsSection.set("slot", 7);
        }

        // Save the updated configuration
        saveConfig();
    }


    public void loadConfig() {
        FileConfiguration config = getConfig();

        pluginPrefix = config.getString("plugin-prefix", "<gradient:gold:yellow>BeaconLabs</gradient> <dark_gray>»</dark_gray> ");
        legacyPrefix = config.getString("legacy-prefix", "&6BeaconLabs &8» &7");

        healOnJoin = config.getBoolean("heal-on-join", true);
        returnToSpawn = config.getBoolean("return-spawn-heightlimit", true);

        heightlimitBottom = config.getInt("heightlimit-bottom", 10);
        heightlimitTop = config.getInt("heightlimit-top", 256);

        if (config.contains("spawn")) {
            this.spawnLocation = deserializeLocation(config.getString("spawn"));
        } else {
            getLogger().warning("Config does not contain a 'spawn' section.");
        }
    }

    public Integer getHeightlimitTop() {
        return heightlimitTop;
    }

    public Integer getHeightlimitBottom() {
        return heightlimitBottom;
    }

    public void reloadHeightLimits() {
        reloadConfig();
        heightlimitTop = getConfig().getInt("heightlimit-top", 256);
        heightlimitBottom = getConfig().getInt("heightlimit-bottom", 10);
    }

    public BuildManager getBuildManager() {
        return buildManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public SelectorCommand getSelectorCommand() {
        return selectorCommand;
    }

    public void setSelectorCommand(SelectorCommand selectorCommand) {
        this.selectorCommand = selectorCommand;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
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

    public void reapplyLobbyItemsToAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Keep reload behavior identical to the normal join path, including slots,
            // MiniMessage formatting, permissions, and item metadata.
            itemManager.giveJoinItems(player);
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
            // Paper 26.1 requires DO_DAYLIGHT_CYCLE to be enabled for time changes
            if (!world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)) {
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
                getLogger().info("Enabled DO_DAYLIGHT_CYCLE for world: " + world.getName());
            }

            try {
                world.setTime(defaultTime);
                getLogger().info("Time set to " + defaultTime + " in world: " + world.getName());
            } catch (IllegalArgumentException e) {
                getLogger().warning("Could not set time in world " + world.getName() + " - world may not have an active clock: " + e.getMessage());
            }
        }
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
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (channel.equalsIgnoreCase("BungeeCord")) {
            try {
                java.io.DataInputStream in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));
                String subchannel = in.readUTF();
                if (subchannel.equals("ServerIP")) {
                    String server = in.readUTF();
                    String ip = in.readUTF();
                    int port = in.readUnsignedShort();
                    // Try to connect to the server's IP/port using a short socket timeout
                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                        boolean isOnline = false;
                        try (java.net.Socket socket = new java.net.Socket()) {
                            socket.connect(new java.net.InetSocketAddress(ip, port), 400);
                            isOnline = socket.isConnected();
                        } catch (Exception ignored) {}
                        final boolean status = isOnline;
                        Bukkit.getScheduler().runTask(this, () -> {
                            if (status) {
                                org.bcnlab.beaconlabslobby.commands.SelectorCommand.handleOnlineResponse(server);
                            } else {
                                org.bcnlab.beaconlabslobby.commands.SelectorCommand.handleOfflineResponse(server);
                            }
                        });
                    });
                }
            } catch (Exception ex) {
                getLogger().warning("Failed to parse BungeeCord plugin message: " + ex.getMessage());
            }
        } else if (channel.equalsIgnoreCase("beaconlabs:server_info")) {
            try {
                java.io.DataInputStream in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));
                String subchannel = in.readUTF();
                if (subchannel.equals("Response")) {
                    String server = in.readUTF();
                    boolean online = in.readBoolean();
                    int onlineCount = in.readInt();
                    int maxCount = in.readInt();
                    
                    Bukkit.getScheduler().runTask(this, () -> {
                        org.bcnlab.beaconlabslobby.commands.SelectorCommand.handleServerInfoResponse(server, online, onlineCount, maxCount);
                    });
                }
            } catch (Exception ex) {
                getLogger().warning("Failed to parse beaconlabs:server_info message: " + ex.getMessage());
            }
        }
    }
}
