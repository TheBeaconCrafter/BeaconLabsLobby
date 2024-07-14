package org.bcnlab.beaconlabslobby.listeners;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

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
}
