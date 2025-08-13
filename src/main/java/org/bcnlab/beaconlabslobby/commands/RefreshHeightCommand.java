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
            sender.sendMessage(plugin.getPrefix() + "§aHeight configuration refreshed!");
            sender.sendMessage(plugin.getPrefix() + "§7Top limit: §e" + plugin.getHeightlimitTop() + " §7blocks");
            sender.sendMessage(plugin.getPrefix() + "§7Bottom limit: §e" + plugin.getHeightlimitBottom() + " §7blocks");
            return true;
        } else {
            sender.sendMessage(plugin.getPrefix() + "§cYou do not have permission to use this command.");
        }

        return false;
    }
}
