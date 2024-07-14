package org.bcnlab.beaconlabslobby.commands;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final BeaconLabsLobby plugin;

    public SpawnCommand(BeaconLabsLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Retrieve spawn location from config
        if (plugin.getConfig().contains("spawn")) {
            player.teleport(plugin.getSpawnLocation());
            player.sendMessage(plugin.getPrefix() + "§aTeleported to spawn!");
        } else {
            player.sendMessage(plugin.getPrefix() + "$cSpawn location is not set.");
        }

        return true;
    }
}
