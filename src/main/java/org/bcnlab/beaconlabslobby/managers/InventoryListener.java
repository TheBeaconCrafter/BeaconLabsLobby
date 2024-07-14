package org.bcnlab.beaconlabslobby.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public class InventoryListener implements Listener {

    private final BuildManager buildManager;

    public InventoryListener(BuildManager buildManager) {
        this.buildManager = buildManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        // Check if player is allowed to build
        if (!buildManager.isAllowedToBuild(playerUUID)) {
            event.setCancelled(true); // Cancel the event if build mode is not enabled
        }
    }
}
