package org.bcnlab.beaconlabslobby.utils;

import net.md_5.bungee.api.ChatColor;
import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.logging.Level;

public class ScoreboardUtil {

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final String title;
    private final Map<String, Integer> scores;
    private final BeaconLabsLobby plugin;

    public ScoreboardUtil(Player player, BeaconLabsLobby plugin) {
        this.player = player;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("lobby", "dummy"); // Register without display name
        this.objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Scoreboard.display.header", "&4&lLobby"))); // Set display name separately
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.scores = new HashMap<>();
        this.plugin = plugin;

        // Load scoreboard settings from configuration
        loadConfig();
        this.title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Scoreboard.display.header", "&4&lLobby"));

        // Add scores from config
        addScoresFromConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection scoreboardConfig = config.getConfigurationSection("Scoreboard");

        if (scoreboardConfig == null) {
            // Create default scoreboard display section
            config.set("Scoreboard.enabled", true);
            config.createSection("Scoreboard.display");
            config.set("Scoreboard.display.header", "&4&lLobby");
            config.set("Scoreboard.display.message1", "&aWelcome:");
            config.set("Scoreboard.display.message2", "&7» &e{player}");
            config.set("Scoreboard.display.message3", "&bServer:");
            config.set("Scoreboard.display.message4", "&7» &eLobby");
            config.set("Scoreboard.display.message5", "&dWebsite:");
            config.set("Scoreboard.display.message6", "&7» bcnlab.org");

            plugin.saveConfig();
            plugin.getLogger().log(Level.INFO, "Default scoreboard configuration created.");
        }
    }

    private void addScoresFromConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection displayConfig = config.getConfigurationSection("Scoreboard.display");

        if (displayConfig != null) {
            // Get all keys under Scoreboard.display
            Set<String> keys = displayConfig.getKeys(false);

            // Filter keys to get only those starting with "message"
            Map<Integer, String> messageMap = new HashMap<>();
            for (String key : keys) {
                if (key.startsWith("message")) {
                    try {
                        int index = Integer.parseInt(key.substring(7));
                        messageMap.put(index, displayConfig.getString(key, "&cMessage " + index + " not configured"));
                    } catch (NumberFormatException ignored) {
                        plugin.getLogger().log(Level.WARNING, "Invalid message key: " + key);
                    }
                }
            }

            // Sort the messageMap by key to process in order
            List<Map.Entry<Integer, String>> sortedMessages = new ArrayList<>(messageMap.entrySet());
            sortedMessages.sort(Map.Entry.comparingByKey());

            // Determine the maximum index for scoring
            int maxIndex = sortedMessages.size();

            // Add scores from config in sorted order
            for (int i = 0; i < sortedMessages.size(); i++) {
                Map.Entry<Integer, String> entry = sortedMessages.get(i);
                String message = entry.getValue().replace("{player}", player.getName()); // Replace {player} with actual player name
                int score = maxIndex - i; // Calculate score from top to bottom
                setScore(message, score, i); // Pass the index to ensure uniqueness
            }
        }
    }

    // Method to set or update a score
    public void setScore(String text, int score, int uniqueIndex) {
        text = ChatColor.translateAlternateColorCodes('&', text) + ChatColor.COLOR_CHAR + "r" + ChatColor.COLOR_CHAR + Integer.toHexString(uniqueIndex);
        objective.getScore(text).setScore(score);
        scores.put(text, score);
    }


    // Method to remove a score
    public void removeScore(String text) {
        text = ChatColor.translateAlternateColorCodes('&', text);
        scoreboard.resetScores(text);
        scores.remove(text);
    }

    // Method to update the scoreboard for the player
    public void updateScoreboard() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection scoreboardConfig = config.getConfigurationSection("Scoreboard");
        Boolean isEnabled = scoreboardConfig.getBoolean("enabled");

        if(isEnabled) {
            for (String entry : scores.keySet()) {
                objective.getScore(entry).setScore(scores.get(entry));
            }
        }

    }

    // Method to set the player's scoreboard
    public void setPlayerScoreboard() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection scoreboardConfig = config.getConfigurationSection("Scoreboard");
        Boolean isEnabled = scoreboardConfig.getBoolean("enabled");

        if(isEnabled) {
            player.setScoreboard(scoreboard);
        }
    }

    // Method to get the current title of the scoreboard
    public String getTitle() {
        return title;
    }

    // Method to clear all scores from the scoreboard
    public void clearScores() {
        for (String entry : scores.keySet()) {
            scoreboard.resetScores(entry);
        }
        scores.clear();
    }

    // Method to remove the scoreboard from the player
    public void removeScoreboard() {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
