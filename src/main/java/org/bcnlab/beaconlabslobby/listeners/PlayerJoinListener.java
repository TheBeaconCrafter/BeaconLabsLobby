package org.bcnlab.beaconlabslobby.listeners;
import org.bcnlab.beaconlabslobby.utils.SoundUtil;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bcnlab.beaconlabslobby.commands.SelectorCommand;
import org.bcnlab.beaconlabslobby.utils.ScoreboardUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

        // Give configurable items on join via ItemManager (it clears the inventory once).
        plugin.getItemManager().giveJoinItems(player);
        // Proxies/compatibility layers can finish sending the join inventory after
        // PlayerJoinEvent. Resync across that short window rather than relying on a
        // single next-tick packet.
        resyncInventoryAfterJoin(player);

        //Scoreboard - Delay by 10 ticks (500ms) to allow ViaVersion plugin messages and LuckPerms API to fully load the user
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                ScoreboardUtil scoreboardUtil = new ScoreboardUtil(player, plugin);
                scoreboardUtil.updateScoreboard();
                scoreboardUtil.setPlayerScoreboard();
            }
        }, 10L);

        // Sound Design
        SoundUtil.playSound(player, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        SoundUtil.playSound(player, org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);

        if(plugin.getHealOnJoin()) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
        }

    }

    private void resyncInventoryAfterJoin(Player player) {
        long[] delays = {1L, 20L, 60L};
        for (long delay : delays) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.updateInventory();
                }
            }, delay);
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        String actionId = plugin.getItemManager().getActionId(event.getPlayer(), event.getHand(), event.getItem());
        
        if (actionId != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            
            // Sound Design for interacting
            SoundUtil.playSound(player, org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            
            switch (actionId) {
                case "selector":
                    SelectorCommand selectorCommand = plugin.getSelectorCommand();
                    if (selectorCommand != null) {
                        // Open directly so the hotbar action does not depend on a
                        // second command dispatch during proxy/server connection.
                        selectorCommand.openSelectorGUI(player);
                    } else {
                        player.performCommand("selector");
                    }
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
