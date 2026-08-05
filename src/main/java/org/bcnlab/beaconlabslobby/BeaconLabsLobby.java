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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
import java.util.List;

public final class BeaconLabsLobby extends JavaPlugin implements PluginMessageListener {

    private String pluginPrefix;
    private String pluginVersion = "1.6.0";
    private String noPermsMessage = "&cYou do not have permission to use this command.";
    private BuildManager buildManager;
    private ItemManager itemManager;
    private NPCManager npcManager;
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
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Monster || entity instanceof Animals || entity instanceof Ambient || entity instanceof WaterMob) {
                        entity.remove();
                    }
                }
            }
        }

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(buildManager), this);

        BuildListener buildListener = new BuildListener(this, buildManager);
        getServer().getPluginManager().registerEvents(buildListener, this);

        // Register commands via LifecycleEvents.COMMANDS (Paper API)
        SelectorCommand selectorCmd = new SelectorCommand(this);
        PrivateSelectorCommand privateSelectorCmd = new PrivateSelectorCommand(this);
        LabsLobbyCommand labsLobbyCmd = new LabsLobbyCommand(this);
        BuildCommand buildCmd = new BuildCommand(this, buildManager);
        SpawnCommand spawnCmd = new SpawnCommand(this);
        SetSpawnCommand setSpawnCmd = new SetSpawnCommand(this);
        HiderCommand hiderCmd = new HiderCommand(this);
        LobbyNPCCommand lobbyNpcCmd = npcManager != null ? new LobbyNPCCommand(this) : null;
        RefreshScoreboardCommand refreshScoreboardCmd = new RefreshScoreboardCommand(this);
        RefreshHeightCommand refreshHeightCmd = new RefreshHeightCommand(this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            commands.register(
                Commands.literal("selector")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        selectorCmd.onCommand(sender, null, "selector", new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String argsStr = StringArgumentType.getString(ctx, "args");
                            String[] args = argsStr.split(" ");
                            selectorCmd.onCommand(sender, null, "selector", args);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .build(),
                "Opens the server selector",
                List.of()
            );

            commands.register(
                Commands.literal("privateselector")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        privateSelectorCmd.onCommand(sender, null, "privateselector", new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String argsStr = StringArgumentType.getString(ctx, "args");
                            String[] args = argsStr.split(" ");
                            privateSelectorCmd.onCommand(sender, null, "privateselector", args);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .build(),
                "Opens the private server selector",
                List.of()
            );

            commands.register(
                Commands.literal("labslobby")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        labsLobbyCmd.onCommand(sender, null, "labslobby", new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String argsStr = StringArgumentType.getString(ctx, "args");
                            String[] args = argsStr.split(" ");
                            labsLobbyCmd.onCommand(sender, null, "labslobby", args);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .build(),
                "Returns info about the plugin",
                List.of()
            );

            commands.register(
                Commands.literal("build")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        buildCmd.onCommand(sender, null, "build", new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String argsStr = StringArgumentType.getString(ctx, "args");
                            String[] args = argsStr.split(" ");
                            buildCmd.onCommand(sender, null, "build", args);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .build(),
                "Allows you to build",
                List.of()
            );

            commands.register(
                Commands.literal("spawn")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        spawnCmd.onCommand(sender, null, "spawn", new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String argsStr = StringArgumentType.getString(ctx, "args");
                            String[] args = argsStr.split(" ");
                            spawnCmd.onCommand(sender, null, "spawn", args);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .build(),
                "Teleports the sender to spawn",
                List.of()
            );

            commands.register(
                Commands.literal("setspawn")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        setSpawnCmd.onCommand(sender, null, "setspawn", new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String argsStr = StringArgumentType.getString(ctx, "args");
                            String[] args = argsStr.split(" ");
                            setSpawnCmd.onCommand(sender, null, "setspawn", args);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .build(),
                "Sets the spawn for the lobby",
                List.of()
            );

            commands.register(
                Commands.literal("hider")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        hiderCmd.onCommand(sender, null, "hider", new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String argsStr = StringArgumentType.getString(ctx, "args");
                            String[] args = argsStr.split(" ");
                            hiderCmd.onCommand(sender, null, "hider", args);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .build(),
                "Opens a GUI to hide other players",
                List.of()
            );

            if (lobbyNpcCmd != null) {
                commands.register(
                    Commands.literal("lobbynpc")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            lobbyNpcCmd.onCommand(sender, null, "lobbynpc", new String[0]);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> {
                                String argsStr = builder.getRemaining();
                                String[] args = argsStr.split(" ", -1);
                                
                                // Adjust the builder to only replace the last word
                                int lastSpace = argsStr.lastIndexOf(' ');
                                com.mojang.brigadier.suggestion.SuggestionsBuilder wordBuilder = 
                                    builder.createOffset(builder.getStart() + lastSpace + 1);
                                    
                                if (args.length == 1) {
                                    String[] subs = {"create", "delete", "list", "skin"};
                                    for (String sub : subs) {
                                        if (sub.startsWith(args[0].toLowerCase())) wordBuilder.suggest(sub);
                                    }
                                } else if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("skin"))) {
                                    for (de.oliver.fancynpcs.api.Npc npc : de.oliver.fancynpcs.api.FancyNpcsPlugin.get().getNpcManager().getAllNpcs()) {
                                        if (npc.getData().getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                            wordBuilder.suggest(npc.getData().getName());
                                        }
                                    }
                                }
                                return wordBuilder.buildFuture();
                            })
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                String argsStr = StringArgumentType.getString(ctx, "args");
                                String[] args = argsStr.split(" ");
                                lobbyNpcCmd.onCommand(sender, null, "lobbynpc", args);
                                return Command.SINGLE_SUCCESS;
                            })
                        )
                        .build(),
                    "Manages FancyNpcs",
                    List.of()
                );
            }

            commands.register(
                Commands.literal("refreshscoreboard")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        refreshScoreboardCmd.onCommand(sender, null, "refreshscoreboard", new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String argsStr = StringArgumentType.getString(ctx, "args");
                            String[] args = argsStr.split(" ");
                            refreshScoreboardCmd.onCommand(sender, null, "refreshscoreboard", args);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .build(),
                "Refresh the scoreboard",
                List.of("refreshsb", "resb")
            );

            commands.register(
                Commands.literal("refreshheight")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        refreshHeightCmd.onCommand(sender, null, "refreshheight", new String[0]);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String argsStr = StringArgumentType.getString(ctx, "args");
                            String[] args = argsStr.split(" ");
                            refreshHeightCmd.onCommand(sender, null, "refreshheight", args);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .build(),
                "Refresh the height limit configuration",
                List.of("refreshh", "reh")
            );
        });

        getLogger().info("BeaconLabs Lobby was enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BeaconLabs Lobby was disabled!");
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', pluginPrefix);
    }

    public void sendMessage(CommandSender sender, String miniMessageString) {
        String prefix = getConfig().getString("plugin-prefix", "<gradient:gold:yellow>BeaconLabs</gradient> <dark_gray>»</dark_gray> ");
        sender.sendMessage(miniMessage.deserialize(prefix + miniMessageString));
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
            player.getInventory().clear();
            giveServerSelectorItem(player);
            givePrivateServerSelectorItem(player);
            givePlayerHiderItem(player);
        }
    }

    private void giveServerSelectorItem(Player player) {
        FileConfiguration config = getConfig();

        if (config.contains("server-selector.items")) {
            ConfigurationSection itemConfig = config.getConfigurationSection("server-selector.settings");

            String itemName = itemConfig.getString("name", "Server Selector");
            itemName = ChatColor.translateAlternateColorCodes('&', itemName);
            Material itemType = Material.valueOf(itemConfig.getString("type", "COMPASS"));
            List<String> itemLore = itemConfig.getStringList("lore");
            int itemSlot = itemConfig.getInt("slot", 2);

            ItemStack item = new ItemStack(itemType);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemName);
                List<String> translatedLore = new ArrayList<>();
                for (String line : itemLore) {
                    String loreColor = ChatColor.translateAlternateColorCodes('&', line);
                    translatedLore.add(loreColor);
                }
                meta.setLore(translatedLore);
                item.setItemMeta(meta);
            }

            player.getInventory().setItem(itemSlot, item);
        } else {
            getLogger().warning("Configuration for server selector item not found!");
        }
    }

    private void givePrivateServerSelectorItem(Player player) {
        FileConfiguration config = getConfig();

        if (!config.getBoolean("private-server-selector.enabled", false)) {
            return;
        }

        String permission = config.getString("private-server-selector.permission", "beaconlabslobby.privateselector");
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            return;
        }

        if (config.contains("private-server-selector.settings")) {
            ConfigurationSection itemConfig = config.getConfigurationSection("private-server-selector.settings");

            String itemName = itemConfig.getString("name", "Private Selector");
            itemName = ChatColor.translateAlternateColorCodes('&', itemName);
            String typeName = itemConfig.getString("type", "NETHER_STAR");
            Material itemType = Material.matchMaterial(typeName);
            if (itemType == null) {
                itemType = Material.NETHER_STAR;
            }

            List<String> itemLore = itemConfig.getStringList("lore");
            int itemSlot = itemConfig.getInt("slot", 4);

            ItemStack item = new ItemStack(itemType);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemName);
                List<String> translatedLore = new ArrayList<>();
                for (String line : itemLore) {
                    translatedLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(translatedLore);
                item.setItemMeta(meta);
            }

            player.getInventory().setItem(itemSlot, item);
        }
    }

    private void givePlayerHiderItem(Player player) {
        FileConfiguration config = getConfig();

        if (config.contains("player-hider.settings")) {
            ConfigurationSection itemConfig = config.getConfigurationSection("player-hider.settings");

            String itemName = itemConfig.getString("name", "Player Hider");
            itemName = ChatColor.translateAlternateColorCodes('&', itemName);
            String typeName = itemConfig.getString("type", "BLAZE_ROD");
            Material itemType = Material.matchMaterial(typeName);
            if (itemType == null) {
                itemType = Material.BLAZE_ROD;
            }

            List<String> itemLore = itemConfig.getStringList("lore");
            int itemSlot = itemConfig.getInt("slot", 6);

            ItemStack item = new ItemStack(itemType);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemName);
                List<String> translatedLore = new ArrayList<>();
                for (String line : itemLore) {
                    String loreColor = ChatColor.translateAlternateColorCodes('&', line);
                    translatedLore.add(loreColor);
                }
                meta.setLore(translatedLore);
                item.setItemMeta(meta);
            }

            player.getInventory().setItem(itemSlot, item);
        } else {
            getLogger().warning("Configuration for player hider item not found!");
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
        if (!channel.equalsIgnoreCase("BungeeCord")) return;
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
    }
}
