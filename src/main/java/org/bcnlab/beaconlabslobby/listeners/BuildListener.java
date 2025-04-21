package org.bcnlab.beaconlabslobby.listeners;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bcnlab.beaconlabslobby.managers.BuildManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class BuildListener implements Listener {

    private final BeaconLabsLobby plugin;
    private final BuildManager buildManager;

    public BuildListener(BeaconLabsLobby plugin, BuildManager buildManager) {
        this.plugin = plugin;
        this.buildManager = buildManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        if (!buildManager.isAllowedToBuild(playerUUID)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        if (!buildManager.isAllowedToBuild(playerUUID)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            UUID playerUUID = event.getPlayer().getUniqueId();
            Material clickedBlockType = event.getClickedBlock().getType();

            // Check if the player is not allowed to build and the interaction is with a block
            if (!buildManager.isAllowedToBuild(playerUUID)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (!buildManager.isAllowedToBuild(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickup(org.bukkit.event.player.PlayerPickupItemEvent event) {
        if (!buildManager.isAllowedToBuild(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
