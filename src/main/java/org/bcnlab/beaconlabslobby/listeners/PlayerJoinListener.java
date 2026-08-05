package org.bcnlab.beaconlabslobby.listeners;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bcnlab.beaconlabslobby.utils.ScoreboardUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerJoinListener implements Listener {

    private final BeaconLabsLobby plugin;

    public PlayerJoinListener(BeaconLabsLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        teleportPlayerToSpawn(player);
        player.setGameMode(GameMode.ADVENTURE);

        clearInventory(player);

        // Give configurable items on join via ItemManager
        plugin.getItemManager().giveJoinItems(player);

        //Scoreboard
        ScoreboardUtil scoreboardUtil = new ScoreboardUtil(player, plugin);
        scoreboardUtil.updateScoreboard();
        scoreboardUtil.setPlayerScoreboard();

        // Sound Design
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);

        if(plugin.getHealOnJoin()) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
        }

    }

    private void teleportPlayerToSpawn(Player player) {
        try {
            Location spawnLocation = plugin.getSpawnLocation();
            if (spawnLocation != null) {
                player.teleport(spawnLocation);
            } else {
                plugin.getLogger().warning("Spawn location is not set.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to teleport player " + player.getName() + " to spawn:");
            e.printStackTrace();
        }
    }

    private void clearInventory(Player player) {
        player.getInventory().clear(); // Clear the player's entire inventory
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        String actionId = plugin.getItemManager().getActionId(item);
        
        if (actionId != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            
            // Sound Design for interacting
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            
            switch (actionId) {
                case "selector":
                    player.performCommand("selector");
                    break;
                case "privateselector":
                    player.performCommand("privateselector");
                    break;
                case "hider":
                    player.performCommand("hider");
                    break;
                case "settings":
                    // Use player.chat() so that it acts as a real command input, which allows Velocity or Link plugin to catch it
                    player.chat("/settings");
                    break;
                case "friends":
                    player.chat("/friends");
                    break;
            }
        }
    }
}
