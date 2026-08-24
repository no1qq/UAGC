package io.github.no1qq.uagc.bukkit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.no1qq.uagc.bukkit.UagcPlugin;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.engine.check.RegisteredCheck;
import io.github.no1qq.uagc.engine.platform.ServerConditions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class UagcCommandTree {

    private UagcCommandTree() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(UagcPlugin plugin, UagcRuntime runtime) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("uagc")
                .requires(CommandSupport.permission("uagc.command"))
                .executes(context -> {
                    HelpText.send(context.getSource());
                    return Command.SINGLE_SUCCESS;
                });

        root.then(Commands.literal("help")
                .executes(context -> {
                    HelpText.send(context.getSource());
                    return Command.SINGLE_SUCCESS;
                }));

        root.then(Commands.literal("status")
                .requires(CommandSupport.permission("uagc.command.status"))
                .executes(context -> {
                    status(context.getSource(), runtime);
                    return Command.SINGLE_SUCCESS;
                }));

        root.then(Commands.literal("reload")
                .requires(CommandSupport.permission("uagc.command.reload"))
                .executes(context -> {
                    if (plugin.reloadUagc()) {
                        CommandSupport.send(context.getSource(), "<green>configuration reloaded</green>");
                    } else {
                        CommandSupport.error(context.getSource(), "reload failed, see the server log");
                    }
                    return Command.SINGLE_SUCCESS;
                }));

        root.then(Commands.literal("checks")
                .requires(CommandSupport.permission("uagc.command.checks"))
                .executes(context -> {
                    listChecks(context.getSource(), runtime);
                    return Command.SINGLE_SUCCESS;
                }));

        root.then(Commands.literal("check")
                .requires(CommandSupport.permission("uagc.command.checks"))
                .then(Commands.argument("check", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String id : runtime.registry().ids()) {
                                builder.suggest(id);
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.literal("enable").executes(context ->
                                toggleCheck(context.getSource(), runtime,
                                        StringArgumentType.getString(context, "check"), true)))
                        .then(Commands.literal("disable").executes(context ->
                                toggleCheck(context.getSource(), runtime,
                                        StringArgumentType.getString(context, "check"), false)))));

        SettingsCommands.register(root, plugin, runtime);
        InspectionCommands.register(root, runtime);
        ModerationCommands.register(root, runtime);

        return root.build();
    }

    private static void status(CommandSourceStack source, UagcRuntime runtime) {
        ServerConditions conditions = runtime.server().conditions();
        int active = 0;
        for (RegisteredCheck registered : runtime.registry().all()) {
            if (registered.isActive()) {
                active++;
            }
        }
        CommandSupport.send(source, "<white>UAGC status</white>");
        CommandSupport.sendRaw(source, "<gray>  enabled: <white>" + runtime.config().general().enabled() + "</white>");
        CommandSupport.sendRaw(source, "<gray>  checks: <white>" + active + " active of "
                + runtime.registry().size() + "</white>");
        CommandSupport.sendRaw(source, "<gray>  tracked players: <white>" + runtime.players().size() + "</white>");
        CommandSupport.sendRaw(source, "<gray>  frozen players: <white>" + runtime.freeze().size() + "</white>");
        CommandSupport.sendRaw(source, "<gray>  tps: <white>" + CommandSupport.formatDouble(conditions.tps())
                + "</white> <gray>avg tick: <white>"
                + CommandSupport.formatDouble(conditions.averageTickTimeMillis()) + "ms</white>");
        CommandSupport.sendRaw(source, "<gray>  measurement reliability: <white>"
                + CommandSupport.formatPercent(conditions.reliability()) + "</white>");
        CommandSupport.sendRaw(source, "<gray>  punishments: <white>"
                + (runtime.punishments().config().dryRun() ? "dry run" : "active") + "</white>");
    }

    private static void listChecks(CommandSourceStack source, UagcRuntime runtime) {
        CommandSupport.send(source, "<white>registered checks</white>");
        for (RegisteredCheck registered : runtime.registry().all()) {
            String state = registered.isRuntimeDisabled()
                    ? "<red>failed</red>"
                    : registered.config().enabled() ? "<green>on</green>" : "<gray>off</gray>";
            CommandSupport.sendRaw(source, "<gray>  " + registered.definition().category().id()
                    + "/<white>" + registered.id() + "</white> " + state
                    + " <dark_gray>flags=" + registered.flags() + "</dark_gray>");
        }
    }

    private static int toggleCheck(CommandSourceStack source, UagcRuntime runtime, String checkId, boolean enable) {
        RegisteredCheck registered = runtime.registry().byId(checkId);
        if (registered == null) {
            CommandSupport.error(source, "unknown check: " + checkId);
            return Command.SINGLE_SUCCESS;
        }
        registered.updateConfig(registered.config().withEnabled(enable));
        if (enable) {
            registered.enableAtRuntime();
        }
        CommandSupport.send(source, "<white>" + registered.id() + "</white> is now "
                + (enable ? "<green>enabled</green>" : "<gray>disabled</gray>")
                + " <dark_gray>until the next reload</dark_gray>");
        return Command.SINGLE_SUCCESS;
    }
}
