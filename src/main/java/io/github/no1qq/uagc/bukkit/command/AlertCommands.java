package io.github.no1qq.uagc.bukkit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.engine.alert.AlertSettings;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

final class AlertCommands {

    private AlertCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, UagcRuntime runtime) {
        root.then(Commands.literal("alerts")
                .requires(CommandSupport.permission("uagc.command.alerts"))
                .executes(context -> toggle(context.getSource(), runtime, null))
                .then(Commands.literal("on").executes(context -> toggle(context.getSource(), runtime, true)))
                .then(Commands.literal("off").executes(context -> toggle(context.getSource(), runtime, false)))
                .then(Commands.literal("verbose").executes(context -> verbose(context.getSource(), runtime)))
                .then(Commands.literal("mute")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (CheckCategory category : CheckCategory.values()) {
                                        builder.suggest(category.id());
                                    }
                                    for (String id : runtime.registry().ids()) {
                                        builder.suggest(id);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> mute(context.getSource(), runtime,
                                        StringArgumentType.getString(context, "target"))))));
    }

    private static AlertSettings settings(CommandSourceStack source, UagcRuntime runtime) {
        if (!(source.getSender() instanceof Player player)) {
            CommandSupport.error(source, "verbose and mute are per player filters, "
                    + "the console can only turn its own alerts on or off");
            return null;
        }
        PlayerData data = runtime.players().get(player.getUniqueId());
        if (data == null) {
            CommandSupport.error(source, "UAGC is not tracking you yet");
            return null;
        }
        return data.alertSettings();
    }

    private static int toggle(CommandSourceStack source, UagcRuntime runtime, Boolean desired) {
        if (!(source.getSender() instanceof Player player)) {
            return console(source, runtime, desired);
        }
        AlertSettings settings = settings(source, runtime);
        if (settings == null) {
            return Command.SINGLE_SUCCESS;
        }
        boolean enabled;
        if (desired == null) {
            enabled = settings.toggle();
        } else {
            settings.setEnabled(desired);
            enabled = desired;
        }
        runtime.alerts().remember(player.getUniqueId(), enabled);
        CommandSupport.send(source, "alerts are now "
                + (enabled ? "<green>enabled</green>" : "<gray>disabled</gray>"));
        return Command.SINGLE_SUCCESS;
    }

    private static int console(CommandSourceStack source, UagcRuntime runtime, Boolean desired) {
        boolean enabled;
        if (desired == null) {
            enabled = runtime.alerts().toggleConsoleAlerts();
        } else {
            runtime.alerts().setConsoleAlerts(desired);
            enabled = desired;
        }
        CommandSupport.send(source, "console alerts are now "
                + (enabled ? "<green>enabled</green>" : "<gray>disabled</gray>"));
        return Command.SINGLE_SUCCESS;
    }

    private static int verbose(CommandSourceStack source, UagcRuntime runtime) {
        AlertSettings settings = settings(source, runtime);
        if (settings == null) {
            return Command.SINGLE_SUCCESS;
        }
        settings.setVerbose(!settings.verbose());
        settings.setMinimumConfidence(settings.verbose() ? 0.0D : runtime.config().alerts().defaultMinimumConfidence());
        settings.setMinimumViolationLevel(settings.verbose()
                ? 0.0D : runtime.config().alerts().defaultMinimumViolationLevel());
        CommandSupport.send(source, "verbose alerts are now "
                + (settings.verbose() ? "<green>enabled</green>" : "<gray>disabled</gray>"));
        return Command.SINGLE_SUCCESS;
    }

    private static int mute(CommandSourceStack source, UagcRuntime runtime, String target) {
        AlertSettings settings = settings(source, runtime);
        if (settings == null) {
            return Command.SINGLE_SUCCESS;
        }
        CheckCategory category = CheckCategory.fromId(target);
        if (category != null) {
            boolean unmuted = settings.toggleCategory(category);
            CommandSupport.send(source, "category <white>" + category.id() + "</white> is now "
                    + (unmuted ? "<green>audible</green>" : "<gray>muted</gray>"));
            return Command.SINGLE_SUCCESS;
        }
        if (runtime.registry().byId(target) == null) {
            CommandSupport.error(source, "unknown category or check: " + target);
            return Command.SINGLE_SUCCESS;
        }
        boolean unmuted = settings.toggleCheck(target.toLowerCase(java.util.Locale.ROOT));
        CommandSupport.send(source, "check <white>" + target + "</white> is now "
                + (unmuted ? "<green>audible</green>" : "<gray>muted</gray>"));
        return Command.SINGLE_SUCCESS;
    }
}
