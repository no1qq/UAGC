package io.github.no1qq.uagc.bukkit.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SettingsCatalog {

    public record Label(String name, String description) {
    }

    private static final Map<String, Label> PATHS = new HashMap<>();
    private static final Map<String, Label> CHECK_FIELDS = new HashMap<>();
    private static final Map<String, Label> OPTIONS = new HashMap<>();
    private static final Map<String, String> CATEGORIES = new HashMap<>();

    private SettingsCatalog() {
    }

    public static Label of(String path) {
        if (path == null) {
            return new Label("setting", "");
        }
        Label direct = PATHS.get(path);
        if (direct != null) {
            return direct;
        }
        String[] parts = path.split("\\.");
        if (parts.length >= 4 && parts[0].equals("checks")) {
            if (parts.length >= 5 && parts[3].equals("options")) {
                Label specific = OPTIONS.get(parts[2] + "." + parts[4]);
                if (specific != null) {
                    return specific;
                }
                Label shared = OPTIONS.get(parts[4]);
                if (shared != null) {
                    return shared;
                }
                return new Label(words(parts[4]), "");
            }
            Label field = CHECK_FIELDS.get(parts[parts.length - 1]);
            if (field != null) {
                return field;
            }
        }
        return new Label(words(parts[parts.length - 1]), "");
    }

    public static String name(String path) {
        return of(path).name();
    }

    public static String description(String path) {
        return of(path).description();
    }

    public static String category(String id) {
        String named = CATEGORIES.get(id);
        return named == null ? id + " check" : named;
    }

    private static String words(String key) {
        return key.replace('-', ' ').replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static void path(String path, String name, String description) {
        PATHS.put(path, new Label(name, description));
    }

    private static void field(String key, String name, String description) {
        CHECK_FIELDS.put(key, new Label(name, description));
    }

    private static void option(String key, String name, String description) {
        OPTIONS.put(key, new Label(name, description));
    }

    static {
        CATEGORIES.put("movement", "movement check");
        CATEGORIES.put("combat", "combat check");
        CATEGORIES.put("interaction", "interaction check");
        CATEGORIES.put("inventory", "inventory check");
        CATEGORIES.put("protocol", "protocol check");

        path("config-version", "config version",
                "Which UAGC wrote this file, do not edit.");
        path("general.enabled", "anti cheat enabled",
                "Turns every check on or off at once.");
        path("general.bypass-refresh-interval-ticks", "bypass refresh interval",
                "Ticks between re-reads of who has bypass.");
        path("general.lag-spike-threshold-millis", "lag spike threshold",
                "A tick slower than this many ms counts as a spike.");
        path("general.exempt-on-lag-spike", "exempt on lag spike",
                "Stops measuring while the server stutters.");
        path("general.max-check-failures-before-disable", "check failure limit",
                "A check that throws this often is shut off until reload.");
        path("general.log-punishments", "log punishments",
                "Writes every punishment carried out to the console.");
        path("general.log-violations-to-console", "log violations to console",
                "Writes every violation to the console, not just alerts.");

        path("player-data.movement-history-size", "movement history size",
                "Past movements kept per player for rewinding.");
        path("player-data.latency-sample-size", "latency sample size",
                "Ping readings averaged per player.");
        path("player-data.click-sample-size", "click sample size",
                "Attack intervals kept for rhythm analysis.");
        path("player-data.evidence-entry-capacity", "evidence capacity",
                "Evidence entries kept per player.");
        path("player-data.evidence-violation-capacity", "violation history size",
                "Past violations kept per player.");
        path("player-data.alerts-enabled-by-default", "alerts on by default",
                "Staff see alerts without turning them on first.");
        path("player-data.default-alert-confidence", "default alert confidence",
                "How sure a check must be before a staff member is told.");
        path("player-data.default-alert-violation-level", "default alert level",
                "Violation level a player needs before staff are told.");

        path("confidence.ping-comfortable-millis", "comfortable ping",
                "Ping up to this counts as a clean connection.");
        path("confidence.ping-severe-millis", "severe ping",
                "Ping at or above this makes evidence count for far less.");
        path("confidence.ping-reliability-floor", "ping trust floor",
                "Lowest trust a bad connection can pull a reading down to.");
        path("confidence.tick-reliability-floor", "tick trust floor",
                "Lowest trust a lagging server can pull a reading down to.");
        path("confidence.transition-grace-ticks", "transition grace",
                "Ticks of doubt after a teleport, join or world change.");
        path("confidence.transition-reliability-floor", "transition trust floor",
                "Lowest trust a reading keeps inside that grace window.");
        path("confidence.jitter-penalty-threshold", "jitter threshold",
                "Ping swing in ms that starts costing trust.");
        path("confidence.jitter-reliability-floor", "jitter trust floor",
                "Lowest trust an unstable connection can pull a reading to.");

        path("alerts.enabled", "alerts enabled",
                "Whether checks tell staff about their flags at all.");
        path("alerts.enabled-by-default-for-staff", "alerts on for new staff",
                "Staff receive alerts before running /uagc alerts.");
        path("alerts.format", "alert format",
                "The line staff see for each alert.");
        path("alerts.hover-format", "alert hover text",
                "The text shown when hovering an alert.");
        path("alerts.click-command", "alert click command",
                "The command run when staff click an alert.");
        path("alerts.default-minimum-confidence", "minimum confidence",
                "How sure a check must be before staff are told.");
        path("alerts.default-minimum-violation-level", "minimum violation level",
                "How much a player must build up before staff are told.");
        path("alerts.cooldown-ticks", "alert cooldown",
                "Ticks to wait before the same check alerts again.");
        path("alerts.send-to-console", "send alerts to console",
                "Repeats every alert in the server log.");
        path("alerts.flag-on-alert", "setback on alert",
                "Pulls a flagged player back to their last safe spot.");
        path("alerts.flag-setback-interval-ticks", "setback interval",
                "Shortest gap in ticks between two of those setbacks.");

        path("freeze.block-movement", "freeze blocks movement",
                "A frozen player cannot walk away.");
        path("freeze.block-interaction", "freeze blocks interaction",
                "A frozen player cannot break, place or use anything.");
        path("freeze.block-commands", "freeze blocks commands",
                "A frozen player may only run the allowed commands.");
        path("freeze.block-damage", "freeze blocks damage",
                "A frozen player cannot be hurt while investigated.");
        path("freeze.persist-across-reconnect", "freeze survives relog",
                "A frozen player is still frozen after rejoining.");
        path("freeze.reminder-interval-ticks", "freeze reminder interval",
                "Ticks between the reminders a frozen player sees.");
        path("freeze.frozen-title", "freeze title",
                "The title shown to a player who has been frozen.");
        path("freeze.frozen-subtitle", "freeze subtitle",
                "The line shown under the freeze title.");
        path("freeze.frozen-message", "freeze message",
                "The chat message a player gets when frozen.");
        path("freeze.unfrozen-message", "unfreeze message",
                "The chat message a player gets when released.");
        path("freeze.disconnect-action", "logout action",
                "What happens to a frozen player who logs out.");

        path("punishments.enabled", "punishments enabled",
                "Whether the punishment rules are ever carried out.");
        path("punishments.dry-run", "dry run",
                "Logs what would have happened instead of punishing.");
        path("punishments.ban-source", "ban source",
                "The name recorded as the source of a ban.");
        path("punishments.default-kick-message", "kick message",
                "The reason a kicked player is shown.");
        path("punishments.default-ban-message", "ban message",
                "The reason a banned player is shown.");
        path("punishments.default-temp-ban-message", "temp ban message",
                "The reason a temporarily banned player is shown.");

        path("debug.enabled", "debug enabled",
                "Lets staff subscribe to live check output.");
        path("debug.log-internal-check-failures", "log check failures",
                "Reports a check that throws to the console.");
        path("debug.max-debug-subscribers", "debug subscriber limit",
                "How many staff may watch debug output at once.");
        path("debug.debug-message-interval-ticks", "debug message interval",
                "Ticks between debug lines sent to one subscriber.");

        field("enabled", "check enabled",
                "Whether this check runs at all.");
        field("violation-increment", "violation increment",
                "How much the violation level grows per flag.");
        field("decay-per-tick", "decay per tick",
                "How fast the level falls while the player behaves.");
        field("minimum-confidence", "minimum confidence",
                "How sure this check must be before a flag counts.");
        field("alert-threshold", "alert threshold",
                "Violation level that starts alerting staff.");
        field("max-violation-level", "violation level cap",
                "The highest the violation level may climb.");
        field("setback-enabled", "setback enabled",
                "Whether this check may pull the player back.");
        field("setback-threshold", "setback threshold",
                "Violation level at which setbacks begin.");

        option("required-streak", "required streak",
                "Bad readings in a row needed before flagging.");
        option("severity-scale", "severity scale",
                "How hard the size of the mistake counts.");
        option("tolerance", "tolerance",
                "Slack given before a reading counts as wrong.");
        option("relative-tolerance", "relative tolerance",
                "Slack as a share of the allowed value.");
        option("absolute-tolerance", "absolute tolerance",
                "Flat slack in blocks per tick on top of that.");
        option("latency-tolerance", "latency tolerance",
                "Extra blocks granted for every 100ms of ping.");
        option("minimum-distance", "minimum distance",
                "Movement shorter than this is ignored.");
        option("velocity-grace-ticks", "knockback grace",
                "Ticks after knockback where nothing is judged.");
        option("window-ticks", "sample window",
                "Ticks the readings are collected over.");
        option("required-samples", "required samples",
                "Matching readings needed before flagging.");
        option("minimum-samples", "minimum samples",
                "Readings needed before the pattern is judged.");
        option("minimum-magnitude", "minimum knockback",
                "Knockback smaller than this is ignored.");
        option("response-ratio", "response ratio",
                "Share of the knockback that counts as taken.");
        option("buffer-decay", "clean hit credit",
                "How much of the evidence one clean hit pays back.");

        option("vertical_motion.tolerance", "motion tolerance",
                "Blocks per tick of slack on the predicted fall.");
        option("vertical_motion.minimum-airborne-samples", "airborne samples",
                "Ticks in the air before the fall curve is trusted.");

        option("sprint_direction.maximum-offset-degrees", "maximum offset",
                "Degrees between look and movement before it is odd.");
        option("sprint_direction.maximum-turn-degrees", "maximum turn",
                "A sharper turn than this is ignored as a normal flick.");
        option("sprint_direction.minimum-distance", "minimum distance",
                "Movement shorter than this in blocks is ignored.");
        option("sprint_direction.combat-grace-ticks", "combat grace",
                "Ticks after a hit where direction is not judged.");

        option("ground_spoof.minimum-distance", "minimum air gap",
                "Blocks of air under the player before doubting it.");

        option("no_fall.minimum-drop", "minimum drop",
                "Blocks a player must fall before damage is checked.");
        option("no_fall.reported-ratio", "reported ratio",
                "Share of the real drop the client must admit to.");

        option("no_slow.use-item-multiplier", "item use multiplier",
                "Share of normal speed left while using an item.");
        option("no_slow.settle-ticks", "settle ticks",
                "Ticks after the item goes up before speed is judged.");
        option("no_slow.blink-minimum-gap-ticks", "blink minimum gap",
                "Shortest pause that still counts as a blink cycle.");
        option("no_slow.blink-maximum-gap-ticks", "blink maximum gap",
                "Longest pause that still counts as a blink cycle.");
        option("no_slow.blink-required-cycles", "blink cycles",
                "Even cycles needed before it is called blinking.");
        option("no_slow.blink-gap-jitter-ticks", "blink jitter",
                "How far the gaps may vary and still look automated.");
        option("no_slow.blink-speed-ratio", "blink speed ratio",
                "How much faster than allowed the burst must be.");
        option("no_slow.blink-severity-scale", "blink severity scale",
                "How hard a blink burst counts.");

        option("no_web.web-multiplier", "web speed multiplier",
                "Share of normal speed a cobweb leaves the player.");
        option("no_web.web-vertical-multiplier", "web descent multiplier",
                "Share of normal falling speed a cobweb leaves.");
        option("no_web.maximum-descent", "maximum descent",
                "Blocks per tick a player may sink in a web anyway.");
        option("no_web.vertical-required-ticks", "vertical streak",
                "Ticks of sinking too fast needed before flagging.");
        option("no_web.vertical-severity-scale", "vertical severity scale",
                "How hard sinking through a web counts.");

        option("timer.clock-drift-millis", "clock drift",
                "Milliseconds of clock slack given to every player.");
        option("timer.maximum-latency-credit-millis", "latency credit",
                "Most time a lagging connection may borrow, in ms.");

        option("reach.hard-limit", "reach limit",
                "Attacks past this many blocks are never allowed.");
        option("reach.deny", "cancel the hit",
                "A hit past the limit does no damage at all.");
        option("reach.tolerance", "reach tolerance",
                "Blocks of slack on the server reach limit.");
        option("reach.streak-window-ticks", "streak window",
                "Ticks the long hits must fall inside.");
        option("reach.minimum-rewind-ticks", "minimum rewind",
                "Newest target position a hit is tested against.");
        option("reach.maximum-rewind-ticks", "maximum rewind",
                "Oldest target position a hit is tested against.");
        option("reach.attacker-motion-cap", "attacker motion cap",
                "Attacker speed above which the hit is not judged.");

        option("attack_rhythm.minimum-samples", "minimum clicks",
                "Clicks needed before the rhythm is judged.");
        option("attack_rhythm.minimum-cps", "minimum clicks per second",
                "Slower clicking than this is never judged.");
        option("attack_rhythm.maximum-deviation-millis", "maximum deviation",
                "How evenly spaced clicks must be to look automated.");

        option("velocity.minimum-ratio", "minimum taken ratio",
                "Share of the knockback the player must really take.");
        option("velocity.window-ticks", "response window",
                "Ticks the player has to show the knockback.");
        option("velocity.minimum-observation-ticks", "minimum observation",
                "Ticks a hit must be watched for before it is judged.");
        option("velocity.instant-ratio", "instant flag ratio",
                "Taking less than this share of a hit flags on its own.");
        option("velocity.instant-observation-ticks", "instant observation",
                "Ticks of watching needed before one hit may flag alone.");
        option("velocity.required-samples", "required samples",
                "Ignored knockbacks needed before flagging.");

        option("knockback_delay.window-ticks", "response window",
                "Ticks the player has to show the knockback.");
        option("knockback_delay.maximum-delay-ticks", "maximum delay",
                "Ticks a player may wait before the knockback shows.");
        option("knockback_delay.required-samples", "required samples",
                "Late knockbacks needed before flagging.");

        option("fast_break.tolerance-ticks", "break tolerance",
                "Ticks a break may come early before it counts.");
        option("fast_break.severity-scale-ratio", "severity scale",
                "How hard breaking early counts against the expected time.");

        option("block_reach.hard-limit", "reach limit",
                "Blocks touched past this far away are never allowed.");
        option("block_reach.tolerance", "reach tolerance",
                "Blocks of slack on the block interaction range.");

        option("invalid_placement.severity", "severity",
                "How hard an impossible placement counts.");

        option("inventory_move.check-sprinting", "catch sprinting",
                "A screen releases the sprint key, so sprinting is impossible.");
        option("inventory_move.check-sneaking", "catch sneaking",
                "Only some versions release the sneak key, off by default.");
        option("inventory_move.deny", "cancel the click",
                "The click itself does not go through.");
        option("inventory_move.tolerance", "speed tolerance",
                "Blocks per tick of slack on the coasting curve.");
        option("inventory_move.ground-friction", "ground friction",
                "How fast an unsteered player slows on the ground.");
        option("inventory_move.air-friction", "air friction",
                "How fast an unsteered player slows in the air.");
        option("inventory_move.knockback-grace-ticks", "knockback grace",
                "Ticks after knockback where nothing is judged.");
        option("inventory_move.required-clicks", "required clicks",
                "Clicks while moving needed before flagging.");
        option("inventory_move.state-severity", "sprint severity",
                "How hard sprinting or sneaking at a screen counts.");
        option("inventory_move.session-gap-ticks", "session gap",
                "Quiet ticks that end an inventory session.");

        option("screen_move.check-sprinting", "catch sprinting",
                "A screen releases the sprint key, so sprinting is impossible.");
        option("screen_move.settle-ticks", "settle ticks",
                "Ticks after the screen opens before movement is judged.");
        option("screen_move.knockback-grace-ticks", "knockback grace",
                "Ticks after knockback where nothing is judged.");
        option("screen_move.tolerance", "speed tolerance",
                "Blocks per tick of slack on the coasting curve.");
        option("screen_move.ground-friction", "ground friction",
                "How fast an unsteered player slows on the ground.");
        option("screen_move.air-friction", "air friction",
                "How fast an unsteered player slows in the air.");
        option("screen_move.required-streak", "required streak",
                "Ticks of steering in a row needed before flagging.");
        option("screen_move.state-severity", "sprint severity",
                "How hard sprinting with a screen open counts.");

        option("silent_switch.maximum-return-ticks", "return window",
                "Ticks in which the held slot must snap back.");
        option("silent_switch.action-window-ticks", "action window",
                "Ticks around a switch where the action must land.");
        option("silent_switch.window-ticks", "sample window",
                "Ticks the switches are counted over.");
        option("silent_switch.required-samples", "required samples",
                "Silent switches needed before flagging.");
        option("silent_switch.required-same-tick-samples", "same tick samples",
                "Switch and act in one tick needed before flagging.");
        option("silent_switch.same-tick-severity", "same tick severity",
                "How hard switching and acting in one tick counts.");
    }
}
