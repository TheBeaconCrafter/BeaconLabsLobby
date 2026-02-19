package org.bcnlab.beaconlabslobby.commands;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to reload the config.");
                return true;
            }

            plugin.reloadConfig();
            plugin.loadConfig();
            plugin.loadSpawnLocation();
            plugin.reapplyLobbyItemsToAllPlayers();

            sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Configuration reloaded.");
            return true;
        } else {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "BeaconLabsLobby Version " + ChatColor.GOLD + plugin.getVersion() + ChatColor.RED + " by ItsBeacon");
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/labslobby reload" + ChatColor.GRAY + " to reload the configuration.");
            return true;
        }
    }
}
