package org.bcnlab.beaconlabslobby.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.logging.Level;

public class ScoreboardUtil {

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<String, Integer> scores;
    private final BeaconLabsLobby plugin;
    private final MiniMessage miniMessage;

    public ScoreboardUtil(Player player, BeaconLabsLobby plugin) {
        this.player = player;
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.scores = new HashMap<>();

        loadConfig();

        String rawHeader = plugin.getConfig().getString("Scoreboard.display.header", "<red><bold>Lobby</bold></red>");
        Component header = miniMessage.deserialize(rawHeader);

        this.objective = scoreboard.registerNewObjective("lobby", Criteria.DUMMY, header);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        addScoresFromConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection scoreboardConfig = config.getConfigurationSection("Scoreboard");

        if (scoreboardConfig == null) {
            config.set("Scoreboard.enabled", true);
            config.createSection("Scoreboard.display");
            config.set("Scoreboard.display.header", "<gradient:gold:yellow><bold>BEACONLABS</bold></gradient>");
            config.set("Scoreboard.display.message1", "  ");
            config.set("Scoreboard.display.message2", "<white>Profile</white>");
            config.set("Scoreboard.display.message3", "<dark_gray>▪</dark_gray> <gray>Player:</gray> <yellow>{player}</yellow>");
            config.set("Scoreboard.display.message4", "<dark_gray>▪</dark_gray> <gray>Rank:</gray> {rank}");
            config.set("Scoreboard.display.message5", "   ");
            config.set("Scoreboard.display.message6", "<white>Server</white>");
            config.set("Scoreboard.display.message7", "<dark_gray>▪</dark_gray> <gray>Lobby 1</gray>");
            config.set("Scoreboard.display.message8", "    ");
            config.set("Scoreboard.display.message9", "<gradient:gold:yellow>bcnlab.org</gradient>");

            plugin.saveConfig();
            plugin.getLogger().log(Level.INFO, "Default scoreboard configuration created.");
        }
    }

    private void addScoresFromConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection displayConfig = config.getConfigurationSection("Scoreboard.display");

        if (displayConfig != null) {
            Set<String> keys = displayConfig.getKeys(false);
            Map<Integer, String> messageMap = new HashMap<>();
            for (String key : keys) {
                if (key.startsWith("message")) {
                    try {
                        int index = Integer.parseInt(key.substring(7));
                        messageMap.put(index, displayConfig.getString(key, "<red>Message " + index + " not configured</red>"));
                    } catch (NumberFormatException ignored) {
                        plugin.getLogger().log(Level.WARNING, "Invalid message key: " + key);
                    }
                }
            }

            List<Map.Entry<Integer, String>> sortedMessages = new ArrayList<>(messageMap.entrySet());
            sortedMessages.sort(Map.Entry.comparingByKey());
            int maxIndex = sortedMessages.size();

            for (int i = 0; i < sortedMessages.size(); i++) {
                Map.Entry<Integer, String> entry = sortedMessages.get(i);
                String message = entry.getValue().replace("{player}", player.getName());
                message = message.replace("{rank}", "<rank>");
                
                net.kyori.adventure.text.Component rankComp = net.kyori.adventure.text.Component.empty();
                
                // Fetch from LuckPerms if possible
                org.bukkit.plugin.RegisteredServiceProvider<net.luckperms.api.LuckPerms> provider = Bukkit.getServicesManager().getRegistration(net.luckperms.api.LuckPerms.class);
                if (provider != null && message.contains("<rank>")) {
                    net.luckperms.api.LuckPerms luckPerms = provider.getProvider();
                    net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                    if (user != null) {
                        String primaryGroup = user.getPrimaryGroup();
                        net.luckperms.api.model.group.Group group = luckPerms.getGroupManager().getGroup(primaryGroup);
                        if (group != null) {
                            String prefix = group.getCachedData().getMetaData().getPrefix();
                            if (prefix != null) {
                                if (prefix.contains("&") || prefix.contains("§")) {
                                    rankComp = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(prefix.replace("§", "&"));
                                } else {
                                    rankComp = miniMessage.deserialize(prefix);
                                }
                            } else {
                                String display = group.getDisplayName();
                                if (display == null) display = primaryGroup;
                                display = display.substring(0, 1).toUpperCase() + display.substring(1);
                                rankComp = miniMessage.deserialize("<gray>" + display + "</gray>");
                            }
                        } else {
                            rankComp = miniMessage.deserialize("<gray>Default</gray>");
                        }
                    } else {
                        rankComp = miniMessage.deserialize("<gray>Default</gray>");
                    }
                } else if (message.contains("<rank>")) {
                    rankComp = miniMessage.deserialize("<gray>Default</gray>");
                }
                
                int score = maxIndex - i;
                net.kyori.adventure.text.Component finalComp;
                if (message.contains("<rank>")) {
                    finalComp = miniMessage.deserialize(message, net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("rank", rankComp));
                } else {
                    finalComp = miniMessage.deserialize(message);
                }
                setScore(finalComp, score, i);
            }
        }
    }

    public void setScore(Component text, int score, int uniqueIndex) {
        String entry = ChatColor.COLOR_CHAR + "r" + ChatColor.COLOR_CHAR + Integer.toHexString(uniqueIndex);
        Team team = scoreboard.getTeam("line_" + uniqueIndex);
        if (team == null) {
            team = scoreboard.registerNewTeam("line_" + uniqueIndex);
            team.addEntry(entry);
        }
        team.prefix(text);
        objective.getScore(entry).setScore(score);
        scores.put(entry, score);
    }

    public void removeScore(int uniqueIndex) {
        String entry = ChatColor.COLOR_CHAR + "r" + ChatColor.COLOR_CHAR + Integer.toHexString(uniqueIndex);
        scoreboard.resetScores(entry);
        scores.remove(entry);
    }

    public void updateScoreboard() {
        FileConfiguration config = plugin.getConfig();
        if(config.getBoolean("Scoreboard.enabled", true)) {
            for (String entry : scores.keySet()) {
                objective.getScore(entry).setScore(scores.get(entry));
            }
        }
    }

    public void setPlayerScoreboard() {
        if(plugin.getConfig().getBoolean("Scoreboard.enabled", true)) {
            player.setScoreboard(scoreboard);
        }
    }

    public void clearScores() {
        for (String entry : scores.keySet()) {
            scoreboard.resetScores(entry);
        }
        scores.clear();
    }

    public void removeScoreboard() {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
