package org.bcnlab.beaconlabslobby.commands;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RefreshHeightCommand implements CommandExecutor {

    private final BeaconLabsLobby plugin;

    public RefreshHeightCommand(BeaconLabsLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if(sender.hasPermission("beaconlabs.lobby.refreshheight")) {
            plugin.reloadHeightLimits();
            plugin.sendMessage(sender, "<gray>Height configuration <gold>refreshed</gold>!</gray>");
            plugin.sendMessage(sender, "<gray>Top limit: <gold>" + plugin.getHeightlimitTop() + "</gold> blocks</gray>");
            plugin.sendMessage(sender, "<gray>Bottom limit: <gold>" + plugin.getHeightlimitBottom() + "</gold> blocks</gray>");
            return true;
        } else {
            plugin.sendMessage(sender, "<red>You do not have permission to use this command.</red>");
        }

        return false;
    }
}
