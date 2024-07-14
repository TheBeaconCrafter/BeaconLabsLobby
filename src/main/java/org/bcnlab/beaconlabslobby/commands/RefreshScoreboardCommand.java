package org.bcnlab.beaconlabslobby.commands;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bcnlab.beaconlabslobby.utils.ScoreboardUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RefreshScoreboardCommand implements CommandExecutor {

    private final BeaconLabsLobby plugin;

    public RefreshScoreboardCommand(BeaconLabsLobby plugin) {
        this.plugin = plugin;
        plugin.getCommand("refreshsb").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        ScoreboardUtil scoreboardUtil = new ScoreboardUtil(player, plugin);
        scoreboardUtil.updateScoreboard();
        scoreboardUtil.setPlayerScoreboard();

        player.sendMessage("Scoreboard refreshed!");

        return true;
    }
}
