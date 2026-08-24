package io.github.no1qq.uagc.bukkit.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;

public final class HelpText {

    private static final String[] LINES = {
            "<gray>  /uagc status <dark_gray>engine and server condition summary</dark_gray>",
            "<gray>  /uagc checks <dark_gray>list checks and their state</dark_gray>",
            "<gray>  /uagc check (check) enable|disable</gray>",
            "<gray>  /uagc info (player) <dark_gray>live movement and network state</dark_gray>",
            "<gray>  /uagc profile (player) <dark_gray>violations, bypass and punishments</dark_gray>",
            "<gray>  /uagc violations (player)</gray>",
            "<gray>  /uagc evidence (player) [limit]</gray>",
            "<gray>  /uagc exemptions (player)</gray>",
            "<gray>  /uagc alerts [on|off|verbose]</gray>",
            "<gray>  /uagc debug (player) [check] <dark_gray>or /uagc debug off</dark_gray></gray>",
            "<gray>  /uagc freeze (player) [reason]</gray>",
            "<gray>  /uagc unfreeze (player)</gray>",
            "<gray>  /uagc frozen</gray>",
            "<gray>  /uagc bypass (player) (scope) [duration] [reason]</gray>",
            "<gray>  /uagc unbypass (player)</gray>",
            "<gray>  /uagc punish (player) (action) [value]</gray>",
            "<gray>  /uagc kick (player) [reason]</gray>",
            "<gray>  /uagc ban (player) [reason]</gray>",
            "<gray>  /uagc tempban (player) (duration) [reason]</gray>",
            "<gray>  /uagc unban (name)</gray>",
            "<gray>  /uagc settings <dark_gray>chest menu that edits config.yml in game</dark_gray></gray>",
            "<gray>  /uagc settings chat <dark_gray>the same thing as chat lines</dark_gray></gray>",
            "<gray>  /uagc reload</gray>"
    };

    private HelpText() {
    }

    public static void send(CommandSourceStack source) {
        CommandSupport.send(source, "<white>UltimateAntiGamingChair</white> <gray>command overview</gray>");
        for (String line : LINES) {
            CommandSupport.sendRaw(source, line);
        }
    }
}
