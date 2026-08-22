package io.github.no1qq.uagc.bukkit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.punishment.PunishmentAction;
import io.github.no1qq.uagc.engine.punishment.PunishmentRecord;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.entity.Player;

final class PunishmentCommands {

    private PunishmentCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, UagcRuntime runtime) {
        root.then(Commands.literal("kick")
                .requires(CommandSupport.permission("uagc.command.kick"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> apply(context.getSource(), runtime,
                                CommandSupport.resolvePlayer(context, "player"), PunishmentAction.KICK, "", ""))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> apply(context.getSource(), runtime,
                                        CommandSupport.resolvePlayer(context, "player"), PunishmentAction.KICK, "",
                                        StringArgumentType.getString(context, "reason"))))));

        root.then(Commands.literal("ban")
                .requires(CommandSupport.permission("uagc.command.ban"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> apply(context.getSource(), runtime,
                                CommandSupport.resolvePlayer(context, "player"), PunishmentAction.BAN, "", ""))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> apply(context.getSource(), runtime,
                                        CommandSupport.resolvePlayer(context, "player"), PunishmentAction.BAN, "",
                                        StringArgumentType.getString(context, "reason"))))));

        root.then(Commands.literal("tempban")
                .requires(CommandSupport.permission("uagc.command.ban"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(context -> apply(context.getSource(), runtime,
                                        CommandSupport.resolvePlayer(context, "player"), PunishmentAction.TEMPBAN,
                                        StringArgumentType.getString(context, "duration"), ""))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> apply(context.getSource(), runtime,
                                                CommandSupport.resolvePlayer(context, "player"),
                                                PunishmentAction.TEMPBAN,
                                                StringArgumentType.getString(context, "duration"),
                                                StringArgumentType.getString(context, "reason")))))));

        root.then(Commands.literal("unban")
                .requires(CommandSupport.permission("uagc.command.unban"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            boolean pardoned = runtime.punishments().unban(name);
                            CommandSupport.send(context.getSource(), pardoned
                                    ? "<green>unbanned</green> <white>" + name + "</white>"
                                    : "<gray>" + name + " is not banned</gray>");
                            return Command.SINGLE_SUCCESS;
                        })));

        registerPunish(root, runtime);
    }

    private static void registerPunish(LiteralArgumentBuilder<CommandSourceStack> root, UagcRuntime runtime) {
        root.then(Commands.literal("punish")
                .requires(CommandSupport.permission("uagc.command.punish"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("action", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (PunishmentAction action : PunishmentAction.values()) {
                                        builder.suggest(action.id());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> punish(context.getSource(), runtime,
                                        CommandSupport.resolvePlayer(context, "player"),
                                        StringArgumentType.getString(context, "action"), ""))
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .executes(context -> punish(context.getSource(), runtime,
                                                CommandSupport.resolvePlayer(context, "player"),
                                                StringArgumentType.getString(context, "action"),
                                                StringArgumentType.getString(context, "value")))))));
    }

    private static int punish(CommandSourceStack source, UagcRuntime runtime, Player player,
                              String rawAction, String value) {
        PunishmentAction action = PunishmentAction.fromId(rawAction);
        if (action == null) {
            CommandSupport.error(source, "unknown punishment action: " + rawAction);
            return Command.SINGLE_SUCCESS;
        }
        return apply(source, runtime, player, action, value, "");
    }

    private static int apply(CommandSourceStack source, UagcRuntime runtime, Player player,
                             PunishmentAction action, String value, String reason) {
        if (player == null) {
            CommandSupport.error(source, "no matching player is online");
            return Command.SINGLE_SUCCESS;
        }
        if (player.hasPermission("uagc.punish.immune")) {
            CommandSupport.error(source, "that player is immune to UAGC punishments");
            return Command.SINGLE_SUCCESS;
        }
        PlayerData data = runtime.players().get(player.getUniqueId());
        PunishmentRecord record = runtime.punishments().punishManually(data, player.getUniqueId(),
                player.getName(), action, value, reason, CommandSupport.senderName(source));
        CommandSupport.send(source, "applied <gold>" + action.id() + "</gold> to <white>"
                + player.getName() + "</white> <dark_gray>reference " + record.reference() + "</dark_gray>");
        return Command.SINGLE_SUCCESS;
    }
}
