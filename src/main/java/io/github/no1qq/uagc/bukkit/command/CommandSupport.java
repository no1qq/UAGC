package io.github.no1qq.uagc.bukkit.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.no1qq.uagc.bukkit.message.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class CommandSupport {

    public static final String PREFIX = "<gray>[<gold>UAGC</gold>]</gray> ";

    private CommandSupport() {
    }

    public static Predicate<CommandSourceStack> permission(String node) {
        return source -> source.getSender().hasPermission(node);
    }

    public static void send(CommandSourceStack source, String message) {
        source.getSender().sendMessage(Messages.parse(PREFIX + message));
    }

    public static void sendRaw(CommandSourceStack source, String message) {
        source.getSender().sendMessage(Messages.parse(message));
    }

    public static void send(CommandSourceStack source, Component component) {
        source.getSender().sendMessage(component);
    }

    public static void error(CommandSourceStack source, String message) {
        source.getSender().sendMessage(Messages.parse(PREFIX + "<red>" + message + "</red>"));
    }

    public static Player resolvePlayer(CommandContext<CommandSourceStack> context, String argument)
            throws CommandSyntaxException {
        PlayerSelectorArgumentResolver resolver = context.getArgument(argument, PlayerSelectorArgumentResolver.class);
        List<Player> players = resolver.resolve(context.getSource());
        return players.isEmpty() ? null : players.getFirst();
    }

    public static String senderName(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        return sender instanceof Player player ? player.getName() : "console";
    }

    public static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0D);
    }

    public static String joinArguments(String[] arguments, int from) {
        if (arguments == null || from >= arguments.length) {
            return "";
        }
        return String.join(" ", java.util.Arrays.copyOfRange(arguments, from, arguments.length));
    }
}
