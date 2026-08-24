package io.github.no1qq.uagc.bukkit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.no1qq.uagc.bukkit.UagcPlugin;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.bukkit.config.SettingsWriter;
import io.github.no1qq.uagc.bukkit.gui.SettingsMenu;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.RegisteredCheck;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

final class SettingsCommands {

    private SettingsCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, UagcPlugin plugin, UagcRuntime runtime) {
        root.then(Commands.literal("settings")
                .requires(CommandSupport.permission("uagc.command.settings"))
                .executes(context -> {
                    open(context.getSource(), plugin, runtime);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("chat")
                        .executes(context -> {
                            panel(context.getSource(), plugin, runtime);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("toggle")
                        .then(Commands.argument("path", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (String path : paths(plugin)) {
                                        builder.suggest(path);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> set(context.getSource(), plugin, runtime,
                                        StringArgumentType.getString(context, "path"), "toggle"))))
                .then(Commands.literal("checks")
                        .executes(context -> {
                            checkList(context.getSource(), runtime, null);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (CheckCategory category : CheckCategory.values()) {
                                        builder.suggest(category.id());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    checkList(context.getSource(), runtime,
                                            CheckCategory.fromId(StringArgumentType.getString(context, "category")));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("check")
                        .then(Commands.argument("check", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (String id : runtime.registry().ids()) {
                                        builder.suggest(id);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    checkPanel(context.getSource(), plugin, runtime,
                                            StringArgumentType.getString(context, "check"));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("set")
                        .then(Commands.argument("path", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (String path : paths(plugin)) {
                                        builder.suggest(path);
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .executes(context -> set(context.getSource(), plugin, runtime,
                                                StringArgumentType.getString(context, "path"),
                                                StringArgumentType.getString(context, "value")))))));
    }

    private static void open(CommandSourceStack source, UagcPlugin plugin, UagcRuntime runtime) {
        if (source.getSender() instanceof Player player) {
            new SettingsMenu(plugin, runtime).open(player);
            return;
        }
        panel(source, plugin, runtime);
    }

    private static void panel(CommandSourceStack source, UagcPlugin plugin, UagcRuntime runtime) {
        FileConfiguration config = plugin.getConfig();
        CommandSupport.send(source, "<white>UAGC settings</white> <dark_gray>click a value to change it</dark_gray>");

        CommandSupport.sendRaw(source, "<gold>general</gold>");
        toggle(source, config, "general.enabled", "anti cheat enabled");
        toggle(source, config, "general.log-violations-to-console", "log violations to console");
        toggle(source, config, "general.exempt-on-lag-spike", "exempt on lag spike");

        CommandSupport.sendRaw(source, "<gold>alerts</gold>");
        toggle(source, config, "alerts.enabled", "alerts enabled");
        toggle(source, config, "alerts.flag-on-alert", "flag the player on alert");
        toggle(source, config, "alerts.send-to-console", "send alerts to console");
        number(source, config, "alerts.cooldown-ticks", "alert cooldown ticks", 1.0D, 0.0D, 200.0D);
        number(source, config, "alerts.flag-setback-interval-ticks", "flag interval ticks", 1.0D, 1.0D, 200.0D);
        number(source, config, "alerts.default-minimum-confidence", "minimum confidence", 0.05D, 0.0D, 1.0D);

        CommandSupport.sendRaw(source, "<gold>punishments</gold>");
        toggle(source, config, "punishments.enabled", "punishments enabled");
        toggle(source, config, "punishments.dry-run", "dry run");

        CommandSupport.sendRaw(source, "<gold>debug</gold>");
        toggle(source, config, "debug.enabled", "debug enabled");

        CommandSupport.sendRaw(source, "<gray>  <click:run_command:'/uagc settings checks'>"
                + "<hover:show_text:'open the check list'><aqua>[check settings]</aqua></hover></click>"
                + " <dark_gray>" + runtime.registry().size() + " registered</dark_gray>");
    }

    private static void checkList(CommandSourceStack source, UagcRuntime runtime, CheckCategory filter) {
        CommandSupport.send(source, "<white>check settings</white>"
                + (filter == null ? "" : " <gray>" + filter.id() + "</gray>"));
        for (RegisteredCheck registered : runtime.registry().all()) {
            if (filter != null && registered.definition().category() != filter) {
                continue;
            }
            String state = registered.config().enabled() ? "<green>on</green>" : "<gray>off</gray>";
            CommandSupport.sendRaw(source, "<gray>  " + registered.definition().category().id() + "/"
                    + "<click:run_command:'/uagc settings check " + registered.id() + "'>"
                    + "<hover:show_text:'" + escape(registered.definition().description()) + "'>"
                    + "<white>" + registered.id() + "</white></hover></click> " + state
                    + " <dark_gray>vl threshold " + CommandSupport.formatDouble(registered.config().alertThreshold())
                    + "</dark_gray>");
        }
    }

    private static void checkPanel(CommandSourceStack source, UagcPlugin plugin, UagcRuntime runtime, String checkId) {
        RegisteredCheck registered = runtime.registry().byId(checkId);
        if (registered == null) {
            CommandSupport.error(source, "unknown check: " + checkId);
            return;
        }
        FileConfiguration config = plugin.getConfig();
        String base = "checks." + registered.definition().category().id() + "." + registered.id();
        if (!config.contains(base)) {
            CommandSupport.error(source, "this check has no section in config.yml, add one to edit it");
            return;
        }

        CommandSupport.send(source, "<white>" + registered.definition().displayName() + "</white> <gray>"
                + registered.id() + "</gray>");
        CommandSupport.sendRaw(source, "<dark_gray>  " + escape(registered.definition().description()) + "</dark_gray>");
        toggle(source, config, base + ".enabled", "enabled");
        number(source, config, base + ".violation-increment", "violation increment", 0.5D, 0.0D, 50.0D);
        number(source, config, base + ".decay-per-tick", "decay per tick", 0.01D, 0.0D, 5.0D);
        number(source, config, base + ".minimum-confidence", "minimum confidence", 0.05D, 0.0D, 1.0D);
        number(source, config, base + ".alert-threshold", "alert threshold", 1.0D, 0.0D, 200.0D);
        if (config.contains(base + ".setback-enabled")) {
            toggle(source, config, base + ".setback-enabled", "setback enabled");
            number(source, config, base + ".setback-threshold", "setback threshold", 1.0D, 0.0D, 200.0D);
        }

        ConfigurationSection options = config.getConfigurationSection(base + ".options");
        if (options == null) {
            return;
        }
        CommandSupport.sendRaw(source, "<gold>  options</gold>");
        for (String key : options.getKeys(false)) {
            Object value = options.get(key);
            String path = base + ".options." + key;
            if (value instanceof Boolean) {
                toggle(source, config, path, key);
            } else if (value instanceof Number number) {
                number(source, config, path, key, step(number.doubleValue()), 0.0D, 10_000.0D);
            }
        }
    }

    private static double step(double value) {
        double magnitude = Math.abs(value);
        if (magnitude >= 20.0D) {
            return 5.0D;
        }
        if (magnitude >= 2.0D) {
            return 1.0D;
        }
        if (magnitude >= 0.2D) {
            return 0.05D;
        }
        if (magnitude >= 0.02D) {
            return 0.005D;
        }
        return 0.001D;
    }

    private static void toggle(CommandSourceStack source, FileConfiguration config, String path, String label) {
        if (!config.contains(path)) {
            return;
        }
        boolean current = config.getBoolean(path);
        CommandSupport.sendRaw(source, "<gray>  " + label + ": "
                + (current ? "<green>on</green>" : "<red>off</red>")
                + " <click:run_command:'/uagc settings toggle " + path + "'>"
                + "<hover:show_text:'" + path + "'><aqua>[toggle]</aqua></hover></click>");
    }

    private static void number(CommandSourceStack source, FileConfiguration config, String path, String label,
                               double step, double minimum, double maximum) {
        if (!config.contains(path)) {
            return;
        }
        boolean integral = config.get(path) instanceof Integer;
        double current = config.getDouble(path);
        double down = Math.max(minimum, current - step);
        double up = Math.min(maximum, current + step);
        CommandSupport.sendRaw(source, "<gray>  " + label + ": <white>" + format(current, integral) + "</white> "
                + "<click:run_command:'/uagc settings set " + path + " " + format(down, integral) + "'>"
                + "<hover:show_text:'" + path + "'><red>[-]</red></hover></click> "
                + "<click:run_command:'/uagc settings set " + path + " " + format(up, integral) + "'>"
                + "<hover:show_text:'" + path + "'><green>[+]</green></hover></click>");
    }

    private static String format(double value, boolean integral) {
        return integral ? String.valueOf(Math.round(value)) : String.format(Locale.ROOT, "%.3f", value);
    }

    private static int set(CommandSourceStack source, UagcPlugin plugin, UagcRuntime runtime,
                           String path, String rawValue) {
        SettingsWriter.Result result = SettingsWriter.parseAndSet(plugin, path, rawValue);
        switch (result) {
            case UNKNOWN_PATH -> CommandSupport.error(source, "unknown setting: " + path);
            case WRONG_TYPE -> CommandSupport.error(source, path + " does not take that value");
            case RELOAD_FAILED -> CommandSupport.error(source,
                    "the value was written but the reload failed, see the server log");
            case APPLIED -> {
                String value = SettingsWriter.format(plugin, path);
                CommandSupport.send(source, "<white>" + path + "</white> is now <green>" + value + "</green>");
                runtime.server().info("settings " + path + " set to " + value
                        + " by " + CommandSupport.senderName(source));
                reopen(source, plugin, runtime, path);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void reopen(CommandSourceStack source, UagcPlugin plugin, UagcRuntime runtime, String path) {
        if (!path.startsWith("checks.")) {
            panel(source, plugin, runtime);
            return;
        }
        String[] parts = path.split("\\.");
        if (parts.length >= 3) {
            checkPanel(source, plugin, runtime, parts[2]);
        }
    }

    private static List<String> paths(UagcPlugin plugin) {
        return plugin.getConfig().getKeys(true).stream()
                .filter(key -> {
                    Object value = plugin.getConfig().get(key);
                    return value instanceof Boolean || value instanceof Number || value instanceof String;
                })
                .toList();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("'", "").replace("<", "").replace(">", "");
    }
}
