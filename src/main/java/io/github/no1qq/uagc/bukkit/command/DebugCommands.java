package io.github.no1qq.uagc.bukkit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.entity.Player;

final class DebugCommands {

    private DebugCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, UagcRuntime runtime) {
        root.then(Commands.literal("debug")
                .requires(CommandSupport.permission("uagc.command.debug"))
                .then(Commands.literal("off").executes(context -> {
                    if (!(context.getSource().getSender() instanceof Player staff)) {
                        CommandSupport.error(context.getSource(), "debug output can only be sent to a player");
                        return Command.SINGLE_SUCCESS;
                    }
                    boolean removed = runtime.debug().unsubscribe(staff.getUniqueId());
                    CommandSupport.send(context.getSource(), removed
                            ? "<gray>debug output stopped</gray>"
                            : "<gray>you were not subscribed to debug output</gray>");
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> subscribe(context.getSource(), runtime,
                                CommandSupport.resolvePlayer(context, "player"), null))
                        .then(Commands.argument("check", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (String id : runtime.registry().ids()) {
                                        builder.suggest(id);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> subscribe(context.getSource(), runtime,
                                        CommandSupport.resolvePlayer(context, "player"),
                                        StringArgumentType.getString(context, "check"))))));
    }

    private static int subscribe(CommandSourceStack source, UagcRuntime runtime, Player target, String checkId) {
        if (!(source.getSender() instanceof Player staff)) {
            CommandSupport.error(source, "debug output can only be sent to a player");
            return Command.SINGLE_SUCCESS;
        }
        if (target == null) {
            CommandSupport.error(source, "no matching player is online");
            return Command.SINGLE_SUCCESS;
        }
        if (checkId != null && runtime.registry().byId(checkId) == null) {
            CommandSupport.error(source, "unknown check: " + checkId);
            return Command.SINGLE_SUCCESS;
        }
        if (runtime.debug().size() >= runtime.config().debug().maxDebugSubscribers()
                && !runtime.debug().isSubscribed(staff.getUniqueId())) {
            CommandSupport.error(source, "the debug subscriber limit has been reached");
            return Command.SINGLE_SUCCESS;
        }
        runtime.debug().subscribe(staff.getUniqueId(), target.getUniqueId(), checkId);
        CommandSupport.send(source, "<gray>debugging <white>" + target.getName() + "</white>"
                + (checkId == null ? "" : " for check <white>" + checkId + "</white>")
                + " <dark_gray>use /uagc debug off to stop</dark_gray>");
        return Command.SINGLE_SUCCESS;
    }
}
