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
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        // Check permissions
        if (args.length > 0) {
            // Permission check for allowing others to build
            if (!player.hasPermission("beaconlabs.lobby.build.others")) {
                player.sendMessage(plugin.getPrefix() + "§cYou don't have permission to allow others to build.");
                return true;
            }

            // Execute command to allow others to build
            if (args.length == 1) {
                Player target = plugin.getServer().getPlayer(args[0]); // Corrected index from args[1] to args[0]
                if (target == null) {
                    player.sendMessage(plugin.getPrefix() + "§cPlayer not found or offline: " + args[0]); // Corrected index from args[1] to args[0]
                    return true;
                }
                UUID targetUUID = target.getUniqueId();
                if(buildManager.isAllowedToBuild(targetUUID)) {
                    buildManager.disallowBuilding(targetUUID);
                    target.setGameMode(GameMode.ADVENTURE);
                    player.sendMessage(plugin.getPrefix() + "§cYou disallowed " + target.getName() + " to build.");
                    target.sendMessage(plugin.getPrefix() + "§cYou are no longer allowed to build.");
                } else {
                    buildManager.allowBuilding(targetUUID);
                    target.setGameMode(GameMode.CREATIVE);
                    player.sendMessage(plugin.getPrefix() + "§aYou allowed " + target.getName() + " to build.");
                    target.sendMessage(plugin.getPrefix() + "§aYou are now allowed to build.");
                }
            } else {
                player.sendMessage(plugin.getPrefix() + "§cUsage: /build <player>");
            }
        } else {
            // Permission check for self building
            if (!player.hasPermission("beaconlabs.lobby.build.self")) {
                player.sendMessage(plugin.getPrefix() + "§cYou don't have permission to toggle your build status.");
                return true;
            }

            // Toggle self build status
            if (buildManager.isAllowedToBuild(playerUUID)) {
                buildManager.disallowBuilding(playerUUID);
                player.setGameMode(GameMode.ADVENTURE);
                player.sendMessage(plugin.getPrefix() + "§cBuilding is now disabled for yourself.");
            } else {
                buildManager.allowBuilding(playerUUID);
                player.setGameMode(GameMode.CREATIVE);
                player.sendMessage(plugin.getPrefix() + "§aBuilding is now enabled for yourself.");
            }
        }

        return true;
    }
}
