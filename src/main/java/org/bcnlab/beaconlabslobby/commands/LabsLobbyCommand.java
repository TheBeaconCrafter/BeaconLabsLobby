package org.bcnlab.beaconlabslobby.commands;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class LabsLobbyCommand implements CommandExecutor {

    private final BeaconLabsLobby plugin;

    public LabsLobbyCommand(BeaconLabsLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            String perm = "beaconlabslobby.reload";
            if (!sender.hasPermission(perm)) {
                plugin.sendMessage(sender, "<red>You do not have permission to reload the config.</red>");
                return true;
            }

            plugin.reloadConfig();
            plugin.loadConfig();
            plugin.loadSpawnLocation();
            plugin.reapplyLobbyItemsToAllPlayers();
            plugin.getNpcManager().reloadNpcConfig();

            plugin.sendMessage(sender, "<green>Configuration reloaded.</green>");
            return true;
        } else {
            plugin.sendMessage(sender, "<red>BeaconLabsLobby Version </red><gold>" + plugin.getVersion() + "</gold><red> by ItsBeacon</red>");
            return true;
        }
    }
}
