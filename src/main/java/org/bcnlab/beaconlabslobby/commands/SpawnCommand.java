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
            plugin.sendMessage(sender, "<red>Only players can use this command!</red>");
            return true;
        }

        Player player = (Player) sender;

        // Retrieve spawn location from config
        if (plugin.getConfig().contains("spawn")) {
            player.teleport(plugin.getSpawnLocation());
            plugin.sendMessage(player, "<gray>Teleported to <gold>spawn</gold>!</gray>");
        } else {
            plugin.sendMessage(player, "<red>Spawn location is not set.</red>");
        }

        return true;
    }
}
