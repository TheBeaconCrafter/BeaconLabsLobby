package org.bcnlab.beaconlabslobby.commands;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final BeaconLabsLobby plugin;

    public SetSpawnCommand(BeaconLabsLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("beaconlabs.lobby.setspawn")) {
            player.sendMessage(plugin.getPrefix() + "§cYou do not have permission to use this command.");
            return true;
        }

        // Set spawn location to player's current location
        plugin.setSpawnLocation(player.getLocation());
        plugin.getConfig().set("spawn", plugin.serializeLocation(player.getLocation()));
        plugin.saveConfig();

        player.sendMessage(plugin.getPrefix() + "§aSpawn location set!");

        return true;
    }
}
