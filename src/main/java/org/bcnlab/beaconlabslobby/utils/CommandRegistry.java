package org.bcnlab.beaconlabslobby.utils;

import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bcnlab.beaconlabslobby.commands.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;

public class CommandRegistry {

    public static void registerAll(BeaconLabsLobby plugin, Commands commands) {
        SelectorCommand selectorCommand = new SelectorCommand(plugin);
        plugin.setSelectorCommand(selectorCommand);
        registerLegacyCommand(commands, "selector", "Opens the server selector", List.of(), selectorCommand);
        registerLegacyCommand(commands, "privateselector", "Opens the private server selector", List.of(), new PrivateSelectorCommand(plugin));
        registerLegacyCommand(commands, "labslobby", "Returns basic plugin information", List.of(), new LabsLobbyCommand(plugin));
        registerLegacyCommand(commands, "build", "Toggles build mode", List.of("b"), new BuildCommand(plugin, plugin.getBuildManager()));
        registerLegacyCommand(commands, "spawn", "Teleports to spawn", List.of(), new SpawnCommand(plugin));
        registerLegacyCommand(commands, "setspawn", "Sets the spawn location", List.of(), new SetSpawnCommand(plugin));
        registerLegacyCommand(commands, "hider", "Toggles player visibility", List.of(), new HiderCommand(plugin));
        registerLegacyCommand(commands, "lobbynpc", "Manage Lobby NPCs", List.of("lnpc"), new LobbyNPCCommand(plugin));
        registerLegacyCommand(commands, "refreshscoreboard", "Refreshes the scoreboard", List.of("rs"), new RefreshScoreboardCommand(plugin));
        registerLegacyCommand(commands, "refreshheight", "Refreshes void height", List.of("rh"), new RefreshHeightCommand(plugin));
    }

    private static void registerLegacyCommand(Commands commands, String name, String description, List<String> aliases, CommandExecutor executor) {
        LiteralCommandNode<CommandSourceStack> node = Commands.literal(name)
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                executor.onCommand(sender, null, name, new String[0]);
                return Command.SINGLE_SUCCESS;
            })
            .then(Commands.argument("args", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> {
                    if (executor instanceof org.bukkit.command.TabCompleter) {
                        String input = ctx.getInput();
                        int lastSpace = input.lastIndexOf(' ');
                        String[] args = input.substring(input.indexOf(' ') + 1).split(" ", -1);
                        List<String> completions = ((org.bukkit.command.TabCompleter) executor).onTabComplete(ctx.getSource().getSender(), null, name, args);
                        if (completions != null) {
                            String lastArg = args.length > 0 ? args[args.length - 1] : "";
                            com.mojang.brigadier.suggestion.SuggestionsBuilder offsetBuilder = builder.createOffset(builder.getStart() + lastSpace + 1 - builder.getStart());
                            for (String c : completions) {
                                if (c.toLowerCase().startsWith(lastArg.toLowerCase())) {
                                    offsetBuilder.suggest(c);
                                }
                            }
                            return offsetBuilder.buildFuture();
                        }
                    }
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    String argsStr = StringArgumentType.getString(ctx, "args");
                    String[] args = argsStr.split(" ");
                    executor.onCommand(sender, null, name, args);
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
            
        commands.register(node, description, aliases);
    }
}
