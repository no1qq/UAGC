package io.github.no1qq.uagc.bukkit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.punishment.PunishmentRecord;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.entity.Player;

import java.util.List;

public final class InspectionCommands {

    private InspectionCommands() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root, UagcRuntime runtime) {
        root.then(Commands.literal("info")
                .requires(CommandSupport.permission("uagc.command.info"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> {
                            PlayerData data = require(context.getSource(), runtime,
                                    CommandSupport.resolvePlayer(context, "player"));
                            if (data != null) {
                                header(context.getSource(), data, "live state");
                                InspectionRenderer.liveState(context.getSource(), data);
                                InspectionRenderer.bypass(context.getSource(), runtime, data);
                            }
                            return Command.SINGLE_SUCCESS;
                        })));

        root.then(Commands.literal("profile")
                .requires(CommandSupport.permission("uagc.command.profile"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> {
                            PlayerData data = require(context.getSource(), runtime,
                                    CommandSupport.resolvePlayer(context, "player"));
                            if (data != null) {
                                profile(context.getSource(), runtime, data);
                            }
                            return Command.SINGLE_SUCCESS;
                        })));

        root.then(Commands.literal("violations")
                .requires(CommandSupport.permission("uagc.command.violations"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> {
                            PlayerData data = require(context.getSource(), runtime,
                                    CommandSupport.resolvePlayer(context, "player"));
                            if (data != null) {
                                header(context.getSource(), data, "violations");
                                InspectionRenderer.violations(context.getSource(), runtime, data);
                            }
                            return Command.SINGLE_SUCCESS;
                        })));

        registerExemptions(root, runtime);
        registerEvidence(root, runtime);
        AlertCommands.register(root, runtime);
        DebugCommands.register(root, runtime);
    }

    private static void registerExemptions(LiteralArgumentBuilder<CommandSourceStack> root, UagcRuntime runtime) {
        root.then(Commands.literal("exemptions")
                .requires(CommandSupport.permission("uagc.command.exemptions"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> {
                            PlayerData data = require(context.getSource(), runtime,
                                    CommandSupport.resolvePlayer(context, "player"));
                            if (data != null) {
                                header(context.getSource(), data, "active exemptions");
                                InspectionRenderer.exemptions(context.getSource(), runtime, data);
                            }
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    private static void registerEvidence(LiteralArgumentBuilder<CommandSourceStack> root, UagcRuntime runtime) {
        root.then(Commands.literal("evidence")
                .requires(CommandSupport.permission("uagc.command.evidence"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> {
                            PlayerData data = require(context.getSource(), runtime,
                                    CommandSupport.resolvePlayer(context, "player"));
                            if (data != null) {
                                header(context.getSource(), data, "recent evidence");
                                InspectionRenderer.evidence(context.getSource(), data, 10);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 40))
                                .executes(context -> {
                                    PlayerData data = require(context.getSource(), runtime,
                                            CommandSupport.resolvePlayer(context, "player"));
                                    if (data != null) {
                                        header(context.getSource(), data, "recent evidence");
                                        InspectionRenderer.evidence(context.getSource(), data,
                                                IntegerArgumentType.getInteger(context, "limit"));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }

    private static void profile(CommandSourceStack source, UagcRuntime runtime, PlayerData data) {
        header(source, data, "profile");
        CommandSupport.sendRaw(source, "<gray>  tracked for <white>" + (data.ticksOnline() / 20L) + "s</white>");
        CommandSupport.sendRaw(source, "<gray>  frozen: <white>"
                + runtime.freeze().isFrozen(data.uuid()) + "</white>");
        InspectionRenderer.bypass(source, runtime, data);
        InspectionRenderer.violations(source, runtime, data);

        List<PunishmentRecord> punishments = runtime.punishments().recentFor(data.uuid(), 5);
        if (punishments.isEmpty()) {
            CommandSupport.sendRaw(source, "<gray>  no punishments recorded this session</gray>");
            return;
        }
        CommandSupport.sendRaw(source, "<gray>  recent punishments</gray>");
        for (PunishmentRecord record : punishments) {
            CommandSupport.sendRaw(source, "<gray>    <white>" + record.reference() + "</white> "
                    + record.action().id() + " <dark_gray>" + record.reason() + "</dark_gray>");
        }
    }

    private static void header(CommandSourceStack source, PlayerData data, String title) {
        CommandSupport.send(source, "<white>" + data.name() + "</white> <gray>" + title + "</gray>");
    }

    static PlayerData require(CommandSourceStack source, UagcRuntime runtime, Player player) {
        if (player == null) {
            CommandSupport.error(source, "no matching player is online");
            return null;
        }
        PlayerData data = runtime.players().get(player.getUniqueId());
        if (data == null) {
            CommandSupport.error(source, "UAGC is not tracking that player yet");
            return null;
        }
        return data;
    }
}
