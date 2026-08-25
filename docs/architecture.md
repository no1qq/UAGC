# Architecture

## The central split

UAGC is divided into two halves that never blur into each other.

```
io.github.no1qq.uagc
|-- api        public, versioned surface other plugins compile against
|-- engine     the anti cheat itself, with no Bukkit imports at all
+-- bukkit     the Paper adapter, listeners, commands and samplers
```

`engine` does not import a single Bukkit class. Everything it needs about the world arrives as plain
records, and everything it wants to do to the world goes out through a small interface.

This is not architectural decoration. It is what makes the detection logic testable: the entire test
suite runs movement sequences, combat events and punishment rules through the real engine without
starting a Minecraft server.

## How a movement event flows

```
PlayerMoveEvent
   -> MovementListener            grants lag exemptions, records ping
   -> PlayerSampler               reads attributes, activity and effects from the Player
   -> BlockSampler                reads ground, friction and surface facts from the World
   -> MovementSnapshot            an immutable record of plain doubles and booleans
   -> MovementTracker             derives air ticks, deltas, continuity
   -> CheckEngine                 gates on bypass and exemptions, dispatches to checks
   -> Check                       returns pass, or a flag with a severity in 0..1
   -> ConfidenceModel             multiplies severity by measurement reliability
   -> ViolationTracker            accumulates and decays a violation level
   -> AlertService                notifies staff above the alert threshold
   -> PunishmentService           applies configured rules above the punishment threshold
```

The sampling step is the important one. Because the platform layer flattens the world into a
`MovementSnapshot`, a test can construct any situation directly, including ones that are hard to
reproduce on a live server such as a lagging tick loop or a 600 ms connection.

## The seam between engine and platform

The engine defines what it needs and the Bukkit layer implements it.

| Interface | Purpose | Paper implementation |
| --- | --- | --- |
| `ServerContext` | clock, tick rate, permissions, logging | `BukkitServerContext` |
| `MessageGateway` | alerts, messages, titles, action bars | `BukkitMessageGateway` |
| `EnforcementGateway` | kick, ban, command, setback, freeze | `BukkitEnforcementGateway` |
| `SupportQuery` | lazy entity lookups used only before flagging | `BukkitSupportQuery` |
| `FreezeStore` | persistence for active freezes | `YamlFreezeStore` |
| `UagcClock` | server tick and wall clock | `BukkitServerContext` |

`SupportQuery` deserves a note. Checking whether a player is standing on a boat means scanning nearby
entities, which is far too expensive to do on every movement packet. So the check does all of its
cheap reasoning first and only asks this question in the moment before it would flag. A player who is
behaving normally never triggers the lookup.

## Confidence has a defined meaning

A check does not return a boolean. It returns a severity in `0..1` describing how far the observation
exceeded what the model allows. The engine then computes:

```
confidence  = severity * reliability
reliability = latencyFactor * tickFactor * transitionFactor
```

- `latencyFactor` falls from 1.0 to a floor as ping rises, and drops further when jitter is high
- `tickFactor` falls as the server tick rate degrades
- `transitionFactor` falls when a relevant exemption expired within the last few ticks

Only checks that declare themselves latency or tick sensitive pay those factors. A malformed packet is
just as invalid on a lagging server as on a healthy one, so `invalid_position` ignores both.

Crucially, every factor has a floor. Bad conditions reduce confidence, they never switch detection off.
A cheater on a 500 ms connection still accumulates violations, just more slowly and with more evidence
required before a punishment threshold is crossed.

The violation level is then `increment * confidence`, decaying over time. One questionable event and
sustained impossible behaviour therefore end up in genuinely different places.

## Context, not exceptions

Rather than a growing pile of special cases inside each check, context lives in one place:
`ExemptionState`, a lock free array of typed, expiring grants.

Teleports, knockback, velocity, explosions, vehicles, world changes, elytra, respawns, effect changes,
gamemode changes and lag spikes all grant a scoped exemption from the listener that observed them.
Each `ExemptionType` declares which check categories it affects, so a check never has to know why it
is being skipped.

Exemptions expire in server ticks rather than milliseconds. During a lag spike fewer ticks pass, so an
exemption granted before the spike naturally lasts longer in wall clock time, which is exactly the
behaviour you want when packets arrive late.

## Bypass is visible, not silent

A bypassed player is still evaluated. The check runs and, if it would have flagged, the detection is
written to that player's evidence log as suppressed. No violation is accumulated, no alert is sent and
no punishment fires.

This means a developer testing movement modifications with `uagc.bypass.movement` can see exactly what
would have been detected, and staff inspecting that player see `bypass: active` with the source, scope
and remaining time rather than concluding the anti cheat is broken.

## Error isolation

Every check invocation is wrapped. A check that throws has its failure counted and logged; the rest of
the dispatch continues untouched. After a configurable number of consecutive internal failures the
check is disabled at runtime and stops being invoked, while every other check keeps running. This is
covered by a test that asserts the healthy check keeps being called after the broken one is isolated.

## Performance decisions

- Per player check state lives in an array indexed by check, allocated once and cleared on quit, so a
  disconnecting player cannot leak state
- History uses fixed capacity ring buffers, never growing collections
- Permission lookups are cached and refreshed on an interval rather than per event
- The dispatch table is an identity map from event class to a pre built array, resolved at startup
- Block queries are bounded and skip unloaded chunks so UAGC never triggers a synchronous chunk load
- `MovementSnapshot` and friends are immutable records, deliberately short lived and cheap to collect

## Threading

Everything that touches Bukkit state runs on the server thread. UAGC does not move check evaluation
onto another thread, because the sampling step reads live world and entity state and the correctness
cost of getting that wrong is far higher than the throughput gained.

The pieces that may legitimately be touched from another thread are the ones the integration API
exposes, and those use lock free structures: `ExemptionState` is an `AtomicReferenceArray`,
`BypassState` uses volatile fields and a copy on write list, and `PlayerDataManager` is backed by a
`ConcurrentHashMap`.

## Known limitations

These are real and worth stating plainly.

- **No packet level access.** UAGC uses the Paper event API, not protocol interception. It cannot see
  raw client input keys, so horizontal movement is bounded by an envelope of what is reachable rather
  than simulated exactly. The envelope is deliberately conservative.
- **`PlayerMoveEvent` fires only on change.** A perfectly stationary player produces no events. The
  timer check accounts for this by resetting after an idle gap.
- **Ground detection uses block bounding boxes**, not full voxel shapes, so an unusual collision shape
  is treated slightly generously. That direction of error favours the player.
- **Entity support is sampled lazily**, so a player standing on an entity is verified only at the
  moment a check would otherwise flag.
- **Punishment history is bounded and in memory.** It survives for the server session, not restarts.
  Bans themselves go through Paper's ban list and do persist.
- **MiniMessage is provided by Paper at runtime** rather than shaded. If it were ever missing, message
  formatting degrades to plain text instead of failing.

## Design decisions worth knowing

- **The movement speed attribute is read, not assumed.** Potion effects, plugin modifiers and sprinting
  all show up in `Attribute.MOVEMENT_SPEED`, so a legitimate speed boost raises the allowed envelope
  automatically instead of needing a special case.
- **Interaction range is read from the attribute too.** A server that widens `ENTITY_INTERACTION_RANGE`
  gets a correspondingly wider reach allowance with no configuration.
- **Server applied velocity raises the envelope directly.** UAGC knows the exact vector it applied
  because it observed the event, so knockback is modelled rather than merely excused.
- **Punishments are decided centrally.** Checks report violations and never call kick or ban. This is
  what makes dry run mode, cooldowns and manual staff actions share one code path.
- **Setbacks teleport rather than cancel the move event.** Cancelling a movement at MONITOR priority
  fights with other plugins; a teleport to a known safe position is explicit and observable.
- **An action can be denied outright.** A check returns `CheckResult.deny()` alongside its flag,
  `CheckEngine.process` reports that back to the listener, and the listener cancels the event. `reach`
  and `inventory_move` use it: a hit past the interaction range does no damage, and a click made while
  still walking does not move the item. This is why `CombatListener` and the inventory click handler
  run at `HIGH` rather than `MONITOR`, since a MONITOR handler cannot cancel anything. They are the
  only places UAGC touches an event instead of only reading it.
- **A tick with no movement packet is still a tick.** Paper fires no move event when neither the
  position nor the rotation changed, so a player standing perfectly still is invisible to a check that
  only runs on movement. The plugin's own tick task samples those players where they stand and feeds a
  movement event marked `idle`. Only checks that return true from `Check.handlesIdleTicks` see it,
  because a synthetic tick is meaningless to a speed or a gravity model but is exactly what a knockback
  model needs.
- **The console carries one kind of line.** While alerts are enabled and going to the console, that
  alert line is the only thing written; `log-violations-to-console` is the fallback for a console that
  has turned alerts off, not a second stream running beside them.

## Why `Player.isOnGround` is used despite being deprecated

Paper deprecates `Player.isOnGround` because the value is controlled by the client and is therefore
unreliable and open to spoofing.

That is exactly why UAGC reads it. The client's claim is one of the two inputs the `ground_spoof` and
`no_fall` checks compare; the other is the server side collision test performed by `BlockSampler`. The
deprecation warning is suppressed at the single sampling method that reads it, and the value is never
treated as truth anywhere else in the codebase.
