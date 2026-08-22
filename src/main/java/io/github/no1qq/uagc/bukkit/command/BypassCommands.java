package io.github.no1qq.uagc.bukkit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.engine.bypass.BypassScope;
import io.github.no1qq.uagc.engine.bypass.TemporaryBypass;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.evidence.EvidenceEntry;
import io.github.no1qq.uagc.engine.evidence.EvidenceType;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.util.DurationParser;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.entity.Player;

import java.time.Duration;

final class BypassCommands {

    private BypassCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, UagcRuntime runtime) {
        root.then(Commands.literal("bypass")
                .requires(CommandSupport.permission("uagc.command.bypass"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("scope", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("all");
                                    for (CheckCategory category : CheckCategory.values()) {
                                        builder.suggest(category.id());
                                    }
                                    for (String id : runtime.registry().ids()) {
                                        builder.suggest(id);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> grant(context.getSource(), runtime,
                                        CommandSupport.resolvePlayer(context, "player"),
                                        StringArgumentType.getString(context, "scope"), "10m", ""))
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .executes(context -> grant(context.getSource(), runtime,
                                                CommandSupport.resolvePlayer(context, "player"),
                                                StringArgumentType.getString(context, "scope"),
                                                StringArgumentType.getString(context, "duration"), ""))
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(context -> grant(context.getSource(), runtime,
                                                        CommandSupport.resolvePlayer(context, "player"),
                                                        StringArgumentType.getString(context, "scope"),
                                                        StringArgumentType.getString(context, "duration"),
                                                        StringArgumentType.getString(context, "reason"))))))));

        root.then(Commands.literal("unbypass")
                .requires(CommandSupport.permission("uagc.command.bypass"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> revoke(context.getSource(), runtime,
                                CommandSupport.resolvePlayer(context, "player")))));
    }

    private static BypassScope parseScope(UagcRuntime runtime, String raw) {
        if ("all".equalsIgnoreCase(raw)) {
            return BypassScope.all();
        }
        CheckCategory category = CheckCategory.fromId(raw);
        if (category != null) {
            return BypassScope.category(category);
        }
        if (runtime.registry().byId(raw) != null) {
            return BypassScope.check(raw);
        }
        return null;
    }

    private static int grant(CommandSourceStack source, UagcRuntime runtime, Player player,
                             String rawScope, String rawDuration, String reason) {
        PlayerData data = InspectionCommands.require(source, runtime, player);
        if (data == null) {
            return Command.SINGLE_SUCCESS;
        }
        BypassScope scope = parseScope(runtime, rawScope);
        if (scope == null) {
            CommandSupport.error(source, "unknown bypass scope: " + rawScope);
            return Command.SINGLE_SUCCESS;
        }
        Duration duration = DurationParser.parse(rawDuration, Duration.ofMinutes(10L));
        long tick = runtime.server().currentTick();
        long expires = duration == null ? -1L : tick + Math.max(1L, duration.toMillis() / 50L);

        TemporaryBypass bypass = new TemporaryBypass(scope, tick, expires,
                System.currentTimeMillis(), CommandSupport.senderName(source), reason);
        data.bypass().grantTemporary(bypass);
        data.recordEvidence(EvidenceEntry.of(EvidenceType.BYPASS, "temporary bypass granted")
                .with("scope", scope.describe())
                .with("granted_by", CommandSupport.senderName(source))
                .with("reason", reason));

        CommandSupport.send(source, "<white>" + data.name() + "</white> now bypasses <gold>"
                + scope.describe() + "</gold> <gray>for <white>"
                + (duration == null ? "until revoked" : DurationParser.format(duration)) + "</white>");
        return Command.SINGLE_SUCCESS;
    }

    private static int revoke(CommandSourceStack source, UagcRuntime runtime, Player player) {
        PlayerData data = InspectionCommands.require(source, runtime, player);
        if (data == null) {
            return Command.SINGLE_SUCCESS;
        }
        int removed = data.bypass().revokeAllTemporary();
        data.recordEvidence(EvidenceEntry.of(EvidenceType.BYPASS, "temporary bypasses revoked")
                .with("removed", removed)
                .with("revoked_by", CommandSupport.senderName(source)));
        CommandSupport.send(source, "removed <white>" + removed + "</white> temporary bypasses from <white>"
                + data.name() + "</white> <dark_gray>permission based bypasses are unaffected</dark_gray>");
        return Command.SINGLE_SUCCESS;
    }
}
