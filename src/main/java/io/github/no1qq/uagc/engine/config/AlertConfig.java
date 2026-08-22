package io.github.no1qq.uagc.engine.config;

public record AlertConfig(
        boolean enabled,
        boolean enabledByDefaultForStaff,
        String format,
        String hoverFormat,
        String clickCommand,
        double defaultMinimumConfidence,
        double defaultMinimumViolationLevel,
        int cooldownTicks,
        boolean sendToConsole) {

    public static AlertConfig defaults() {
        return new AlertConfig(true, true,
                "<gray>[<gold>UAGC</gold>]</gray> <white><player></white> failed <aqua><check></aqua> <gray>(vl <yellow><vl></yellow>, <yellow><confidence></yellow>)</gray>",
                "<gray><summary></gray>",
                "/uagc profile <player>",
                0.35D, 1.0D, 20, true);
    }
}
