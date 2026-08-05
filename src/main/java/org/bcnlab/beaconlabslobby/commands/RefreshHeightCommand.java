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
            plugin.sendMessage(sender, "§aHeight configuration refreshed!");
            plugin.sendMessage(sender, "§7Top limit: §e" + plugin.getHeightlimitTop() + " §7blocks");
            plugin.sendMessage(sender, "§7Bottom limit: §e" + plugin.getHeightlimitBottom() + " §7blocks");
            return true;
        } else {
            plugin.sendMessage(sender, "§cYou do not have permission to use this command.");
        }

        return false;
    }
}
