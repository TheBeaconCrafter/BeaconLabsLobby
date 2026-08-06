package org.bcnlab.beaconlabslobby.commands;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bcnlab.beaconlabslobby.managers.BuildManager;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BuildCommand implements CommandExecutor {

    private final BeaconLabsLobby plugin;
    private final BuildManager buildManager;

    public BuildCommand(BeaconLabsLobby plugin, BuildManager buildManager) {
        this.plugin = plugin;
        this.buildManager = buildManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("<red>Only players can use this command!</red>");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        // Check permissions
        if (args.length > 0) {
            // Permission check for allowing others to build
            if (!player.hasPermission("beaconlabs.lobby.build.others")) {
                plugin.sendMessage(player, "<red>You don't have permission to allow others to build.</red>");
                return true;
            }

            // Execute command to allow others to build
            if (args.length == 1) {
                Player target = plugin.getServer().getPlayer(args[0]); // Corrected index from args[1] to args[0]
                if (target == null) {
                    plugin.sendMessage(player, "<gray>Player not found or offline: <gold>" + args[0] + "</gold></gray>"); // Corrected index from args[1] to args[0]
                    return true;
                }
                UUID targetUUID = target.getUniqueId();
                if(buildManager.isAllowedToBuild(targetUUID)) {
                    buildManager.disallowBuilding(targetUUID);
                    target.setGameMode(GameMode.ADVENTURE);
                    plugin.sendMessage(player, "<gray>You <red>disallowed</red> <gold>" + target.getName() + "</gold> to build.</gray>");
                    plugin.sendMessage(target, "<gray>You are <red>no longer allowed</red> to build.</gray>");
                } else {
                    buildManager.allowBuilding(targetUUID);
                    target.setGameMode(GameMode.CREATIVE);
                    plugin.sendMessage(player, "<gray>You <green>allowed</green> <gold>" + target.getName() + "</gold> to build.</gray>");
                    plugin.sendMessage(target, "<gray>You are <green>now allowed</green> to build.</gray>");
                }
            } else {
                plugin.sendMessage(player, "<gray>Usage: <gold>/build <player></gold></gray>");
            }
        } else {
            // Permission check for self building
            if (!player.hasPermission("beaconlabs.lobby.build.self")) {
                plugin.sendMessage(player, "<red>You don't have permission to toggle your build status.</red>");
                return true;
            }

            // Toggle self build status
            if (buildManager.isAllowedToBuild(playerUUID)) {
                buildManager.disallowBuilding(playerUUID);
                player.setGameMode(GameMode.ADVENTURE);
                plugin.sendMessage(player, "<gray>Building is now <red>disabled</red> for yourself.</gray>");
            } else {
                buildManager.allowBuilding(playerUUID);
                player.setGameMode(GameMode.CREATIVE);
                plugin.sendMessage(player, "<gray>Building is now <green>enabled</green> for yourself.</gray>");
            }
        }

        return true;
    }
}
