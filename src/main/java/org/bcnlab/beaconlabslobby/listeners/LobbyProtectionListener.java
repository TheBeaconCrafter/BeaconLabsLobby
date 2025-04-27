package org.bcnlab.beaconlabslobby.listeners;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class LobbyProtectionListener implements Listener {

    private final BeaconLabsLobby plugin;

    public LobbyProtectionListener(BeaconLabsLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (plugin.getConfig().getBoolean("disable-damage", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (plugin.getConfig().getBoolean("disable-damage", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplosion(org.bukkit.event.entity.ExplosionPrimeEvent event) {
        if (plugin.getConfig().getBoolean("disable-damage", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (plugin.getConfig().getBoolean("disable-mob-spawning", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (plugin.getConfig().getBoolean("disable-food-level-change", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeatherChange(org.bukkit.event.weather.WeatherChangeEvent event) {
        if (plugin.getConfig().getBoolean("disable-weather", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTimeChange(org.bukkit.event.world.TimeSkipEvent event) {
        if (plugin.getConfig().getBoolean("disable-time", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        if(!plugin.getReturnToSpawn()) {
            return;
        }

        Player player = event.getPlayer();
        Location spawn = plugin.getSpawnLocation();

        if (event.getFrom().getY() == event.getTo().getY()) return;

        double y = player.getLocation().getY();
        if (y > 255) {
            player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "You are too high! Teleporting you down.");
            player.teleport(spawn);
        } else if (y < 10) {
            player.teleport(spawn);
            player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "You fell too low. Teleporting to spawn!");
        }
    }
}
