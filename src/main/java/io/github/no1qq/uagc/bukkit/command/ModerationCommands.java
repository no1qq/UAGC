package io.github.no1qq.uagc.bukkit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.engine.freeze.FreezeRecord;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.util.DurationParser;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class ModerationCommands {

    private ModerationCommands() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root, UagcRuntime runtime) {
        root.then(Commands.literal("freeze")
                .requires(CommandSupport.permission("uagc.command.freeze"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> freeze(context.getSource(), runtime,
                                CommandSupport.resolvePlayer(context, "player"), ""))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> freeze(context.getSource(), runtime,
                                        CommandSupport.resolvePlayer(context, "player"),
                                        StringArgumentType.getString(context, "reason"))))));

        root.then(Commands.literal("unfreeze")
                .requires(CommandSupport.permission("uagc.command.unfreeze"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> {
                            Player player = CommandSupport.resolvePlayer(context, "player");
                            if (player == null) {
                                CommandSupport.error(context.getSource(), "no matching player is online");
                                return Command.SINGLE_SUCCESS;
                            }
                            boolean released = runtime.freeze().release(player.getUniqueId(),
                                    CommandSupport.senderName(context.getSource()));
                            CommandSupport.send(context.getSource(), released
                                    ? "<green>released</green> <white>" + player.getName() + "</white>"
                                    : "<gray>that player was not frozen</gray>");
                            return Command.SINGLE_SUCCESS;
                        })));

        root.then(Commands.literal("frozen")
                .requires(CommandSupport.permission("uagc.command.freeze"))
                .executes(context -> {
                    listFrozen(context.getSource(), runtime);
                    return Command.SINGLE_SUCCESS;
                }));

        BypassCommands.register(root, runtime);
        PunishmentCommands.register(root, runtime);
    }

    private static int freeze(CommandSourceStack source, UagcRuntime runtime, Player player, String reason) {
        if (player == null) {
            CommandSupport.error(source, "no matching player is online");
            return Command.SINGLE_SUCCESS;
        }
        if (player.hasPermission("uagc.freeze.immune")) {
            CommandSupport.error(source, "that player is immune to freezing");
            return Command.SINGLE_SUCCESS;
        }
        PlayerData data = runtime.players().get(player.getUniqueId());
        if (data != null) {
            data.movement().breakContinuity();
        }
        Location location = player.getLocation();
        runtime.freeze().freeze(player.getUniqueId(), player.getName(),
                CommandSupport.senderName(source), reason, 0L,
                location.getWorld() == null ? "unknown" : location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ());
        CommandSupport.send(source, "<white>" + player.getName() + "</white> is now <gold>frozen</gold>"
                + (reason.isBlank() ? "" : " <dark_gray>" + reason + "</dark_gray>"));
        return Command.SINGLE_SUCCESS;
    }

    private static void listFrozen(CommandSourceStack source, UagcRuntime runtime) {
        if (runtime.freeze().all().isEmpty()) {
            CommandSupport.send(source, "<gray>nobody is currently frozen</gray>");
            return;
        }
        CommandSupport.send(source, "<white>frozen players</white>");
        long now = System.currentTimeMillis();
        for (FreezeRecord record : runtime.freeze().all()) {
            CommandSupport.sendRaw(source, "<gray>  <white>" + record.playerName() + "</white> <gray>by <white>"
                    + record.staffName() + "</white> <gray>for <white>"
                    + DurationParser.format(Duration.ofMillis(record.durationMillis(now))) + "</white>"
                    + " <dark_gray>" + record.reason() + "</dark_gray>");
        }
    }
}
