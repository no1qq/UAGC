# Configuration

All settings live in `plugins/UAGC/config.yml`. Run `/uagc reload` to apply changes.

Reload rebuilds the configuration records and pushes them into the live services. Check thresholds,
punishment rules, alert formats, freeze messages and confidence settings all take effect immediately.
Registered checks themselves are not re created, so adding a new check still requires a restart.

## general

| Key | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | master switch, disables all detection when false |
| `bypass-refresh-interval-ticks` | `60` | how often cached permission lookups are refreshed |
| `lag-spike-threshold-millis` | `200` | tick duration above which a lag spike is declared |
| `exempt-on-lag-spike` | `true` | grant a short exemption after a spike |
| `max-check-failures-before-disable` | `12` | consecutive internal errors before a check is isolated |
| `log-punishments` | `true` | write applied punishments to the server log |
| `log-violations-to-console` | `false` | write violations to the log, noisy, for debugging. A violation that alerts is left to the alert line so the console never carries the same flag twice |

## player-data

Controls how much bounded history is kept per player. Larger values give staff more to look at and
cost more memory per online player.

| Key | Default | Meaning |
| --- | --- | --- |
| `movement-history-size` | `40` | movement snapshots retained |
| `latency-sample-size` | `20` | ping samples used for average and jitter |
| `click-sample-size` | `40` | attack intervals retained |
| `evidence-entry-capacity` | `64` | context events retained |
| `evidence-violation-capacity` | `48` | violations retained |
| `alerts-enabled-by-default` | `true` | staff receive alerts without opting in |
| `default-alert-confidence` | `0.35` | minimum confidence for a staff alert |
| `default-alert-violation-level` | `1.0` | minimum violation level for a staff alert |

## confidence

How measurement conditions reduce confidence. Every floor exists so that bad conditions slow detection
down rather than switching it off.

| Key | Default | Meaning |
| --- | --- | --- |
| `ping-comfortable-millis` | `120` | ping below which latency costs nothing |
| `ping-severe-millis` | `450` | ping at which the latency floor is reached |
| `ping-reliability-floor` | `0.45` | lowest multiplier high ping can produce |
| `tick-reliability-floor` | `0.30` | lowest multiplier a lagging server can produce |
| `transition-grace-ticks` | `20` | window after an exemption expires that stays discounted |
| `transition-reliability-floor` | `0.25` | multiplier immediately after a transition |
| `jitter-penalty-threshold` | `90.0` | ping deviation above which jitter is penalised |
| `jitter-reliability-floor` | `0.70` | multiplier applied when jitter is high |

Raising the floors makes UAGC more aggressive under poor conditions. Lowering them makes it more
forgiving. Setting a floor to `0.0` effectively disables the affected checks during bad conditions,
which is not recommended.

## alerts

| Key | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | master switch for staff alerts |
| `enabled-by-default-for-staff` | `true` | alerts start on for players holding the permission, until that player chooses otherwise with `/uagc alerts on/off`, which is remembered in `alert-preferences.yml` |
| `format` | see file | MiniMessage template for the alert line |
| `hover-format` | see file | MiniMessage template shown on hover |
| `click-command` | `/uagc profile <player>` | command suggested when the alert is clicked |
| `default-minimum-confidence` | `0.35` | confidence floor for a new staff member |
| `default-minimum-violation-level` | `0.0` | violation floor for a new staff member, `0` leaves the timing to each check own alert threshold |
| `cooldown-ticks` | `4` | minimum gap between alerts for the same player and check |
| `send-to-console` | `true` | mirror alerts to the console, toggleable live with `/uagc alerts` run from the console, which then overrides this until toggled back |
| `flag-on-alert` | `false` | set a movement category alert back to the last safe position, so a cheating player visibly stutters the moment staff are told |
| `flag-setback-interval-ticks` | `10` | shortest gap between two of those setbacks |

Placeholders available in `format` and `hover-format`: `<player>`, `<uuid>`, `<check>`, `<check_id>`,
`<category>`, `<vl>`, `<confidence>`, `<severity>`, `<ping>`, `<tps>`, `<summary>`, `<repeat>`.

## freeze

| Key | Default | Meaning |
| --- | --- | --- |
| `block-movement` | `true` | hold the player in place, rotation still allowed |
| `block-interaction` | `true` | cancel interaction, breaking, placing and dropping |
| `block-commands` | `true` | restrict commands to the allowed list |
| `block-damage` | `false` | make the frozen player immune to damage |
| `persist-across-reconnect` | `true` | write active freezes to `freezes.yml` |
| `reminder-interval-ticks` | `60` | how often the action bar reminder is sent |
| `frozen-title` | see file | title shown when frozen |
| `frozen-subtitle` | see file | subtitle shown when frozen |
| `frozen-message` | see file | chat message shown when frozen |
| `unfrozen-message` | see file | chat message shown on release |
| `disconnect-action` | `none` | `none` keeps the freeze, `release` clears it on quit |
| `allowed-commands` | `msg, r, reply, tell` | commands usable while frozen |

## debug

| Key | Default | Meaning |
| --- | --- | --- |
| `enabled` | `false` | reserved master switch for debug output |
| `log-internal-check-failures` | `true` | log stack traces when a check throws |
| `max-debug-subscribers` | `8` | how many staff may watch debug output at once |
| `debug-message-interval-ticks` | `1` | minimum gap between debug messages |

Debug output is opt in per staff member through `/uagc debug`, and nothing is written to disk.

## punishments

| Key | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | master switch for automatic punishments |
| `dry-run` | `false` | record what would have happened without enforcing it |
| `ban-source` | `UAGC` | source recorded on the ban entry |
| `default-kick-message` | see file | MiniMessage kick screen |
| `default-ban-message` | see file | MiniMessage permanent ban screen |
| `default-temp-ban-message` | see file | MiniMessage temporary ban screen |
| `rules` | empty | the rule list, see below |

`dry-run` is the recommended setting when first deploying UAGC on a live server. Detection, alerts and
evidence all work normally, and every punishment that would have fired is logged and recorded in the
punishment history, but no player is removed.

Placeholders in punishment messages: `<player>`, `<uuid>`, `<check>`, `<check_id>`, `<reference>`,
`<reason>`, `<expiry>`.

Bans are applied to the profile ban list by UUID, not by name, so they survive a name change.

### Punishment rules

Rules are evaluated in order every time a violation is recorded. The default configuration ships with
an empty rule list so that a fresh install alerts and gathers evidence without punishing anyone.

```yaml
punishments:
  rules:
    - scope: "category:movement"
      violation-level: 25.0
      minimum-confidence: 0.75
      minimum-flags: 10
      action: kick
      reason: "Suspicious movement"
      repeatable: true
      cooldown-ticks: 1200

    - scope: "horizontal_speed"
      violation-level: 60.0
      minimum-confidence: 0.85
      action: tempban
      value: "3d"
      reason: "Movement modification"

    - scope: "*"
      violation-level: 40.0
      minimum-confidence: 0.9
      action: command
      value: "warn <player> UAGC flagged you for <check>"
```

| Key | Meaning |
| --- | --- |
| `scope` | `*` for any check, `category:<name>` for a category, or a check id |
| `violation-level` | violation level the player must have reached |
| `minimum-confidence` | confidence the triggering detection must have |
| `minimum-flags` | how many times the check must have flagged in total |
| `action` | `alert`, `log`, `setback`, `cancel`, `freeze`, `kick`, `tempban`, `ban`, `command` |
| `value` | duration for `tempban` and `freeze`, command line for `command` |
| `reason` | recorded on the punishment and shown to the player |
| `repeatable` | whether the rule can fire more than once for a player |
| `cooldown-ticks` | minimum gap between repeats |

Durations accept compound forms such as `30s`, `10m`, `1h30m`, `3d`, `1w`, or `permanent`.

Commands run from the console on the server thread. Placeholders are substituted before execution.

A rule with an unknown action is skipped with a warning rather than preventing startup.

## checks

Checks are grouped by category, then by check id.

```yaml
checks:
  movement:
    horizontal_speed:
      enabled: true
      violation-increment: 1.0
      decay-per-tick: 0.02
      max-violation-level: 200.0
      minimum-confidence: 0.35
      alert-threshold: 4.0
      setback-enabled: true
      setback-threshold: 8.0
      options:
        relative-tolerance: 0.03
```

| Key | Meaning |
| --- | --- |
| `enabled` | whether the check runs |
| `violation-increment` | violation added at full confidence, scaled down by actual confidence |
| `decay-per-tick` | violation removed per tick, `0.02` is one point per second |
| `max-violation-level` | ceiling so a single check cannot dominate |
| `minimum-confidence` | below this a detection is recorded as evidence only |
| `alert-threshold` | violation level at which staff are alerted |
| `setback-enabled` | whether the check may request a setback |
| `setback-threshold` | violation level at which setbacks begin |
| `cancel-enabled` | reserved for checks that cancel the triggering action |
| `options` | check specific values, documented in [Checks](checks.md) |

A missing or malformed check section falls back to built in defaults and logs a warning rather than
preventing startup.

## Tuning advice

Start with `punishments.dry-run: true` and leave it there for a few days. Watch alerts, and use
`/uagc evidence <player>` on anything that looks wrong.

If a check produces a false positive, resist the urge to raise `minimum-confidence` until it goes
quiet. That hides the problem rather than fixing it. Look at the recorded evidence first: the details
map on each violation carries the measured and allowed values, so you can usually see which part of
the model was wrong and adjust the specific option responsible.
