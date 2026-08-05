package org.bcnlab.beaconlabslobby.commands;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class LobbyNPCCommand implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final BeaconLabsLobby plugin;
    private final net.kyori.adventure.text.Component prefix;

    public LobbyNPCCommand(BeaconLabsLobby plugin) {
        this.plugin = plugin;
        String p = plugin.getConfig().getString("plugin-prefix", "<gold>BeaconLabs</gold> <dark_gray>»</dark_gray> ");
        this.prefix = plugin.getMiniMessage().deserialize(p);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("beaconlabslobby.npc")) {
            player.sendMessage(plugin.getMiniMessage().deserialize("<red>You do not have permission to use this command.</red>"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(prefix.append(Component.text("Usage: /lobbynpc <create|delete|list|skin>", NamedTextColor.RED)));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("create")) {
            if (args.length < 2) {
                player.sendMessage(prefix.append(Component.text("Usage: /lobbynpc create <name> [displayName]", NamedTextColor.RED)));
                return true;
            }
            String name = args[1];
            String displayName = args.length > 2 ? String.join(" ", args).substring(args[0].length() + args[1].length() + 2) : name;
            
            plugin.getNpcManager().createNpc(player, name, displayName);
            player.sendMessage(prefix.append(Component.text("Created NPC " + name, NamedTextColor.GREEN)));
        } else if (subCommand.equals("delete")) {
            if (args.length < 2) {
                player.sendMessage(prefix.append(Component.text("Usage: /lobbynpc delete <name>", NamedTextColor.RED)));
                return true;
            }
            String name = args[1];
            plugin.getNpcManager().deleteNpc(name);
            player.sendMessage(prefix.append(Component.text("Deleted NPC " + name, NamedTextColor.GREEN)));
        } else if (subCommand.equals("list")) {
            plugin.getNpcManager().listNpcs(player);
        } else if (subCommand.equals("skin")) {
            if (args.length < 3) {
                player.sendMessage(prefix.append(Component.text("Usage: /lobbynpc skin <name> <playerName>", NamedTextColor.RED)));
                return true;
            }
            String name = args[1];
            String playerName = args[2];
            plugin.getNpcManager().setSkin(name, playerName);
            player.sendMessage(prefix.append(Component.text("Set skin for NPC " + name + " to " + playerName, NamedTextColor.GREEN)));
        } else {
            player.sendMessage(prefix.append(Component.text("Unknown subcommand.", NamedTextColor.RED)));
        }
        return true;
    }
    
    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        java.util.List<String> completions = new java.util.ArrayList<>();
        if (args.length == 1) {
            String[] subs = {"create", "delete", "list", "skin"};
            for (String sub : subs) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("skin"))) {
            for (de.oliver.fancynpcs.api.Npc npc : de.oliver.fancynpcs.api.FancyNpcsPlugin.get().getNpcManager().getAllNpcs()) {
                if (npc.getData().getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(npc.getData().getName());
                }
            }
        }
        return completions;
    }
}
