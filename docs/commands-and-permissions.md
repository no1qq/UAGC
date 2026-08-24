# Commands, permissions, bypass and admin mode

## The command tree

Everything lives under `/uagc`, registered through Paper's Brigadier command API. Each node carries its
own permission requirement, so a staff member only sees the subcommands they can actually use in tab
completion.

### Inspection

| Command | Permission | Purpose |
| --- | --- | --- |
| `/uagc help` | `uagc.command` | command overview |
| `/uagc status` | `uagc.command.status` | engine state, active checks, tick rate, reliability |
| `/uagc checks` | `uagc.command.checks` | every registered check, its state and flag count |
| `/uagc check (check) enable` | `uagc.command.checks` | toggle a check until the next reload |
| `/uagc info (player)` | `uagc.command.info` | live movement, ground state, surface, latency, bypass |
| `/uagc profile (player)` | `uagc.command.profile` | violations, bypass, freeze state and punishments |
| `/uagc violations (player)` | `uagc.command.violations` | level, peak, flag count and confidence per check |
| `/uagc evidence (player) [limit]` | `uagc.command.evidence` | recent violations and context events |
| `/uagc exemptions (player)` | `uagc.command.exemptions` | active exemptions with source and remaining time |

### Alerts and debugging

| Command | Permission | Purpose |
| --- | --- | --- |
| `/uagc alerts [on/off]` | `uagc.command.alerts` | toggle your own alerts, or the console output when run from the console |
| `/uagc alerts verbose` | `uagc.command.alerts` | drop your confidence and violation floors to zero |
| `/uagc alerts mute (target)` | `uagc.command.alerts` | mute or unmute a category or a single check |
| `/uagc debug (player) [check]` | `uagc.command.debug` | stream live check internals for one player |
| `/uagc debug off` | `uagc.command.debug` | stop receiving debug output |

Alerts are on the moment a staff member first joins holding `uagc.alerts.view`, with no command to
run. Nothing has to be switched on to start seeing detections.

Every one of these settings belongs to the staff member who ran the command and to nobody else.
`/uagc alerts off` silences your own client, it does not turn alerts off for the server or for any
other staff member. Only `alerts.enabled` in `config.yml` does that.

`on` and `off` are remembered in `alert-preferences.yml` in the plugin folder, so the choice survives
relogging and restarts. It is written the moment the command runs. Once a staff member has chosen,
`enabled-by-default-for-staff` no longer applies to them, in either direction. `verbose` and `mute`
are session only and reset on rejoin.

Run the same command from the server console and it toggles the console output instead, because the
console has no personal alert settings to flip. `/uagc alerts`, `/uagc alerts on` and
`/uagc alerts off` all work there. It is the same switch as `alerts.send-to-console` in `config.yml`,
it is stored in the same `alert-preferences.yml`, and once the command has been used it wins over the
config file until it is used again. `verbose` and `mute` remain player only and still refuse.

### Moderation

| Command | Permission | Purpose |
| --- | --- | --- |
| `/uagc freeze (player) [reason]` | `uagc.command.freeze` | freeze a player for investigation |
| `/uagc unfreeze (player)` | `uagc.command.unfreeze` | release a frozen player |
| `/uagc frozen` | `uagc.command.freeze` | list frozen players, who froze them and for how long |
| `/uagc bypass (player) (scope) [duration] [reason]` | `uagc.command.bypass` | grant a temporary bypass |
| `/uagc unbypass (player)` | `uagc.command.bypass` | revoke temporary bypasses |
| `/uagc kick (player) [reason]` | `uagc.command.kick` | kick through the punishment engine |
| `/uagc ban (player) [reason]` | `uagc.command.ban` | permanent ban |
| `/uagc tempban (player) (duration) [reason]` | `uagc.command.ban` | temporary ban |
| `/uagc unban (name)` | `uagc.command.unban` | pardon a ban |
| `/uagc punish (player) (action) [value]` | `uagc.command.punish` | apply any punishment action directly |
| `/uagc settings` | `uagc.command.settings` | open the settings panel, every value is clickable and writes straight to `config.yml` |
| `/uagc settings checks [category]` | `uagc.command.settings` | list checks, click one to open its own panel |
| `/uagc settings check (check)` | `uagc.command.settings` | thresholds and every option of one check |
| `/uagc settings set (path) (value)` | `uagc.command.settings` | set one `config.yml` path, save and reload |
| `/uagc reload` | `uagc.command.reload` | reload configuration and refresh permission caches |

Manual punishments run through the same `PunishmentService` as automatic ones. They receive the same
reference id, appear in the same history and are recorded in the same evidence log.

## Permission hierarchy

UAGC defines permissions and nothing else. It has no rank system, no hardcoded usernames and no
concept of staff groups. LuckPerms or any other permission plugin decides who is what.

```
uagc.admin                     everything below
|-- uagc.command               access to /uagc, parent of every subcommand node
|   |-- uagc.command.status
|   |-- uagc.command.checks
|   |-- uagc.command.info
|   |-- uagc.command.profile
|   |-- uagc.command.violations
|   |-- uagc.command.evidence
|   |-- uagc.command.exemptions
|   |-- uagc.command.alerts
|   |-- uagc.command.debug
|   |-- uagc.command.freeze
|   |-- uagc.command.unfreeze
|   |-- uagc.command.bypass
|   |-- uagc.command.punish
|   |-- uagc.command.kick
|   |-- uagc.command.ban
|   |-- uagc.command.unban
|   +-- uagc.command.reload
|   +-- uagc.command.settings
|-- uagc.alerts
|   +-- uagc.alerts.view       receives detection alerts
+-- uagc.bypass                parent only, grants nothing on its own
    |-- uagc.bypass.all
    |-- uagc.bypass.movement
    |-- uagc.bypass.combat
    |-- uagc.bypass.interaction
    |-- uagc.bypass.inventory
    |-- uagc.bypass.protocol
    +-- uagc.bypass.(check)    one node per registered check

uagc.freeze.immune             cannot be frozen
uagc.punish.immune             cannot be punished by staff commands
```

`uagc.admin` and the command nodes default to operators. **Every bypass node defaults to false**,
including for operators. An operator does not silently bypass detection, which means a compromised or
careless operator account still shows up in alerts. Bypass must always be granted deliberately.

`uagc.bypass` is a parent node that grants nothing by itself, so granting bypass is an explicit and
obvious choice rather than something acquired by accident.

## Building staff roles

These are illustrative. UAGC does not define them.

**Moderator**

```
uagc.command
uagc.command.status
uagc.command.info
uagc.command.profile
uagc.command.violations
uagc.command.alerts
uagc.command.freeze
uagc.command.unfreeze
uagc.command.kick
uagc.alerts.view
```

**Senior staff** adds investigation and punishment depth:

```
uagc.command.evidence
uagc.command.exemptions
uagc.command.checks
uagc.command.punish
uagc.command.ban
uagc.command.unban
uagc.command.bypass
```

**Developer** adds debugging and self testing:

```
uagc.command.debug
uagc.command.reload
uagc.command.settings
uagc.bypass.movement
```

Note the last line. A developer testing a custom movement ability holds only `uagc.bypass.movement`,
so movement checks ignore them while combat, interaction and protocol checks stay fully active. That
granularity is the point of the design.

## The bypass system

A bypass can come from two places:

1. **A permission**, resolved through Bukkit and therefore controllable by LuckPerms. Results are
   cached and refreshed on the interval set by `general.bypass-refresh-interval-ticks`, on join, and
   on `/uagc reload`.
2. **A temporary grant**, issued by `/uagc bypass` with a scope, a duration and a reason.

Scopes are `all`, a category name, or a check id. `/uagc bypass Dev movement 30m testing new ability`
grants exactly that, for exactly that long. A duration of `permanent` lasts until revoked.

### Bypass is never invisible

This matters enough to state on its own. A bypassed player is still evaluated by every check. If a
check would have flagged them, the detection is written to their evidence log as suppressed. What does
not happen is violation accumulation, alerts or punishments.

`/uagc info (player)` and `/uagc profile (player)` show:

- whether a bypass is active at all
- which permission nodes are responsible
- every temporary grant, its scope, who granted it, why, and how long is left

So staff investigating a player who is not being detected immediately see why, instead of concluding
the anti cheat is broken. And a developer can see exactly what would have fired while they test.

`/uagc unbypass (player)` removes temporary grants only. Permission based bypasses are the permission
plugin's business and are deliberately left alone.

## Admin mode

UAGC does not have a mode you toggle. The investigation tooling is the command tree, and what a staff
member can see is decided entirely by their permissions.

A full investigation usually looks like this:

1. An alert arrives, or `/uagc profile (player)` is run directly
2. `/uagc violations (player)` shows which checks fired, how hard and with what confidence
3. `/uagc evidence (player) 20` shows the individual detections with their measured values, plus the
   context events around them such as teleports, knockback and velocity
4. `/uagc exemptions (player)` shows whether something legitimate was in play at the time
5. `/uagc info (player)` shows live state: position, ground agreement, surface, friction, latency and
   jitter
6. `/uagc debug (player) (check)` streams the check internals live if it is still unclear
7. `/uagc freeze (player)` holds them while the decision is made

Every violation carries its own evidence: the measured value, the allowed value, the ping, the tick
rate, the position and any exemptions that were active. The question staff should be able to answer is
not whether UAGC flagged someone but why, and the evidence log is what answers it.

## Freeze system

A frozen player stays connected. The point is to investigate them, not remove them.

Each freeze records the player, the staff member who issued it, the reason, the start time, the
location and the duration if one was given.

While frozen, and depending on configuration:

- movement is held in place, rotation still works so the player can be talked to
- interaction, block breaking, block placing and item dropping are cancelled
- commands are restricted to the configured allow list, so a player can still reply to staff
- damage can optionally be blocked
- an action bar reminder repeats on an interval

Freezes persist to `freezes.yml` and are re applied on rejoin, so disconnecting does not clear one.
`freeze.disconnect-action` decides what happens on quit: `none` keeps the freeze, `release` clears it.

Timed freezes expire on their own. A freeze with no duration lasts until `/uagc unfreeze` is run, and
is verified by a test that advances the clock an hour and asserts the freeze is still in place.

Both the freeze and the release are written to the player's evidence log, so the investigation trail
survives even after they are released.

Players holding `uagc.freeze.immune` cannot be frozen, and players holding `uagc.punish.immune` cannot
be punished by staff commands.

## The settings panel

`/uagc settings` renders the live configuration as a clickable panel. Toggles show `[toggle]`, numbers
show `[-]` and `[+]` stepped by a size that suits their scale, and hovering any of them shows the
`config.yml` path being changed. Clicking one runs `/uagc settings set <path> <value>`, which writes the
value to `config.yml`, saves it, and reloads the runtime the same way `/uagc reload` does. Nothing is
kept in memory only, so a restart keeps whatever was set.

`set` refuses a path that does not already exist in `config.yml` and refuses a value of the wrong shape,
so a typo cannot invent a key or turn a number into a word. Integers stay integers. Every change is
logged to the console with the name of whoever made it.

`/uagc settings checks` lists every registered check with its state and alert threshold, and each one
opens its own panel with the shared thresholds first and then every option the check reads, including
the ones documented in [Checks](checks.md).

The one setting worth calling out is `alerts.flag-on-alert`. With it on, any movement category alert
also sets the player back to their last safe position, so the player who triggered it visibly stutters
at the moment staff are told rather than carrying on untouched. `alerts.flag-setback-interval-ticks`
keeps that from turning into a rubber band loop. Checks that already request their own setback, like
`horizontal_speed`, are unaffected and keep using their own threshold.
