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
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, "<red>This command can only be executed by a player.</red>");
            return true;
        }

        Player player = (Player) sender;
        ScoreboardUtil scoreboardUtil = new ScoreboardUtil(player, plugin);
        scoreboardUtil.updateScoreboard();
        scoreboardUtil.setPlayerScoreboard();

        plugin.sendMessage(player, "<gray>Scoreboard <gold>refreshed</gold>!</gray>");

        return true;
    }
}
