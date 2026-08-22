# Checks

Every check listed here performs real analysis against a model of the game. None of them are
placeholders, and none of them work by comparing a distance against a magic constant.

Each one is registered in `CheckBootstrap` and configured under `checks.<category>.<id>` in
`config.yml`.

## Movement

### vertical_motion

Models vanilla gravity. While a player is airborne, the vertical delta of the next tick is:

```
next = (previous - gravity) * 0.98
```

where `gravity` comes from the `GRAVITY` attribute, or `0.01` while Slow Falling is active. The check
compares the observed delta against that prediction and flags when the player rises above what gravity
permits.

This catches hovering, slow descent, and any form of flight that holds altitude, because the
prediction becomes increasingly negative while the observed delta does not follow.

It does not run when the player is in liquid, on a ladder, gliding, riptiding, flying, in a vehicle,
levitating, on slime, honey, a bed, scaffolding, powder snow, cobweb, a berry bush, a bubble column,
or when the chunk is not loaded. It also skips the first airborne samples after leaving the ground and
anything within a few ticks of server applied velocity or a jump.

Before flagging it asks whether an entity is supporting the player, so standing on a boat is not
mistaken for flight.

| Option | Default | Meaning |
| --- | --- | --- |
| `tolerance` | 0.006 | absolute slack added to the prediction |
| `minimum-airborne-samples` | 2 | airborne samples ignored after leaving ground |
| `required-streak` | 2 | consecutive excesses before flagging |
| `severity-scale` | 0.12 | excess that maps to full severity |

### horizontal_speed

Bounds horizontal movement by an envelope of the fastest speed the current state could produce, rather
than simulating exact input. Each tick:

```
envelope = previousEnvelope * momentum + acceleration
allowed  = max(envelope, terminalGroundSpeed)
```

`momentum` and `acceleration` derive from the surface friction and the live `MOVEMENT_SPEED`
attribute, which already includes potion effects, plugin modifiers and the sprint boost. Sprint jumps
add the vanilla `0.2` impulse on the jump tick, and any velocity the server applied raises the
envelope to that magnitude and lets it decay naturally.

Because the envelope is seeded from the player's own speed and clamped to the allowed value whenever
they exceed it, a cheat cannot ratchet its own allowance upwards.

The test suite verifies that vanilla sprinting, walking, bunny hopping, ice, knockback, a doubled
speed attribute and vehicles all pass, while a sustained 0.75 blocks per tick is detected.

| Option | Default | Meaning |
| --- | --- | --- |
| `relative-tolerance` | 0.03 | proportional slack on the envelope |
| `absolute-tolerance` | 0.005 | absolute slack on the envelope |
| `required-streak` | 2 | consecutive excesses before flagging |
| `severity-scale` | 0.35 | proportional excess mapping to full severity |

### sprint_direction

Catches omni directional sprint, the cheat that lets a player sprint sideways and backwards.

Server side, an omni sprinter is not moving too fast. Vanilla applies the same `1.3` sprint
multiplier to sideways and backward movement as it does to forward, so `horizontal_speed` sees a
perfectly legal speed. The rule a vanilla client actually enforces is that sprint only starts and
continues while forward input is held, and that rule lives entirely on the client. The only thing
left on the server is the angle between where the player is travelling and where they are looking.

So this check takes the horizontal movement vector, takes the horizontal facing vector from the yaw,
and measures the angle between them. Vanilla forward sprinting sits near zero. Sprinting forward
while strafing sits at roughly 45 degrees. Anything past 60 degrees sustained cannot come from a
vanilla client holding forward.

It only measures a player who is sprinting, on the ground, on a normal friction surface, not
colliding horizontally, moving a real distance, not recently given velocity by the server, not within
a second of landing or taking a hit, and not turning sharply this tick. Every one of those bail outs
exists because it produces sideways travel for a legitimate player: ice carries momentum through a
turn, knockback throws you sideways with sprint still latched on, a wall slides you along it, and a
fast mouse turn makes travel lag facing for a tick or two. On top of that it needs six consecutive
ticks before it flags.

The combat bail out is the one that matters most. Landing a hit while sprinting is the most common
action in the game and it does three things this model cannot see: vanilla clears your sprint flag,
multiplies your horizontal motion by `0.6`, and leaves you pressed against a hitbox that shoves you
sideways while you keep holding forward and keep facing the target. That reads as sustained sideways
sprinting and it is completely legitimate, so any tick within `combat-grace-ticks` of an attack landed
or taken is skipped outright. The cost is that a cheat can hide omni sprint by swinging constantly.
That trade is deliberate. A false ban on someone punching a zombie is worse than a missed detection
on someone who has to keep attacking to stay hidden.

The test suite verifies that forward sprinting, diagonal sprinting, walking sideways, ice, knockback,
a stationary player and a brief sideways burst all pass, while sustained sideways and backward
sprinting are detected.

| Option | Default | Meaning |
| --- | --- | --- |
| `maximum-offset-degrees` | 60.0 | how far travel may diverge from facing while sprinting |
| `maximum-turn-degrees` | 40.0 | a sharper turn than this skips the tick |
| `minimum-distance` | 0.08 | movement below this has no reliable direction |
| `velocity-grace-ticks` | 20.0 | ticks after server applied velocity that are ignored |
| `combat-grace-ticks` | 20.0 | ticks after an attack landed or was taken that are ignored |
| `required-streak` | 6 | consecutive offending ticks before flagging |
| `severity-scale` | 0.6 | proportional excess mapping to full severity |

### ground_spoof

Many cheats claim `onGround` while airborne to defeat fall damage and flight limits. This check flags a
client that repeatedly reports standing on ground while the server sees a meaningful drop beneath it.

A fraction of a block of desync is normal and is tolerated. Only a persistent mismatch over a real
distance counts, and entity support is checked before flagging.

| Option | Default | Meaning |
| --- | --- | --- |
| `minimum-distance` | 0.6 | drop below the player before a mismatch counts |
| `required-streak` | 4 | consecutive mismatches before flagging |
| `severity-scale` | 2.0 | distance mapping to full severity |

### no_fall

Independently accumulates the descent the server observed and compares it against the fall distance
the player actually built up. A client that spoofs ground contact keeps resetting its fall distance, so
a large observed descent paired with a near zero fall distance is strong evidence.

Slow Falling resets fall distance legitimately and is excluded, as is any movement following server
applied velocity.

| Option | Default | Meaning |
| --- | --- | --- |
| `minimum-drop` | 4.0 | observed descent before the comparison is meaningful |
| `reported-ratio` | 0.5 | fraction of the descent that must be reflected |
| `required-streak` | 3 | consecutive mismatches before flagging |
| `severity-scale` | 12.0 | shortfall mapping to full severity |

### timer

Maintains a running balance of packet arrival time against real time. Each movement packet adds the
elapsed milliseconds and subtracts one tick of 50 ms. A client running faster than real time drives the
balance negative.

Credit is capped so an idle player cannot bank time and then spend it on a burst, and the balance
resets outright when the server skips ticks or is lagging. That combination is what separates a genuine
timer from a packet burst after a lag spike.

| Option | Default | Meaning |
| --- | --- | --- |
| `drift-threshold-millis` | 600 | accumulated drift before flagging |
| `maximum-credit-millis` | 120 | cap on banked time |
| `idle-reset-millis` | 1000 | gap after which the balance resets |
| `minimum-samples` | 40 | packets required before judging |
| `severity-scale` | 900 | drift mapping to full severity |

## Combat

### reach

Measures the distance from the attacker's eye to the target's bounding box and compares it against the
attacker's own `ENTITY_INTERACTION_RANGE` attribute, scaled by the `SCALE` attribute. A server that
grants extended reach through an attribute is respected automatically.

For player targets the minimum distance across their recent tracked positions is used, so a target who
was closer a moment ago explains the measurement instead of producing a violation. Attackers in a
vehicle or riptiding are skipped because their positions desync.

| Option | Default | Meaning |
| --- | --- | --- |
| `tolerance` | 0.06 | absolute slack |
| `latency-tolerance` | 0.03 | extra slack per 100 ms of ping |
| `severity-scale` | 1.2 | excess distance mapping to full severity |

### attack_rhythm

Looks at the standard deviation of attack intervals. Human clicking always carries variance; automation
does not. The check requires a large sample, a meaningful click rate and a deviation low enough that a
hand could not produce it.

Deliberately conservative: a slow but perfectly steady rhythm is not flagged, and a small sample is
never judged.

| Option | Default | Meaning |
| --- | --- | --- |
| `minimum-samples` | 24 | intervals required before judging |
| `minimum-cps` | 7.0 | click rate below which automation is not claimed |
| `maximum-deviation-millis` | 6.0 | deviation below which a rhythm looks mechanical |

## Interaction

### fast_break

Uses `Block.getDestroySpeed` with the tool actually held, including enchantments, to compute how many
ticks the block should have taken, and compares that against the time between the break starting and
finishing. Creative mode, instant break blocks and breaks without a recorded start are all skipped, and
latency widens the allowance.

| Option | Default | Meaning |
| --- | --- | --- |
| `tolerance-ticks` | 2.0 | absolute slack |
| `severity-scale-ratio` | 0.6 | fraction of expected time mapping to full severity |

### block_reach

The placement equivalent of `reach`, measured against `BLOCK_INTERACTION_RANGE` and the block surface
rather than its centre.

| Option | Default | Meaning |
| --- | --- | --- |
| `tolerance` | 0.1 | absolute slack |
| `latency-tolerance` | 0.04 | extra slack per 100 ms of ping |
| `severity-scale` | 1.5 | excess distance mapping to full severity |

### invalid_placement

Flags a block placed against a face with no supporting block, which cannot happen through normal
placement.

| Option | Default | Meaning |
| --- | --- | --- |
| `severity` | 0.8 | severity assigned to the detection |

## Protocol

### invalid_position

Rejects positions and rotations the protocol can never legitimately produce: non finite coordinates,
positions beyond the world coordinate limit, and a pitch outside the range the client can send.

This is the only check that ignores exemptions, because none of these values are legitimate under any
circumstance. It is also neither latency nor tick sensitive, so bad conditions do not reduce its
confidence.

## Shared configuration

Every check also accepts the standard keys documented in [Configuration](configuration.md):
`enabled`, `violation-increment`, `decay-per-tick`, `max-violation-level`, `minimum-confidence`,
`alert-threshold`, `setback-enabled`, `setback-threshold` and `cancel-enabled`.
