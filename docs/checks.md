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
| `relative-tolerance` | 0.012 | proportional slack on the envelope, 0.03 let a 0.30 per tick strafe cheat sit just inside the limit |
| `absolute-tolerance` | 0.005 | absolute slack on the envelope |
| `required-streak` | 2 | consecutive excesses before flagging |
| `severity-scale` | 0.35 | proportional excess mapping to full severity |

The envelope always assumes the player is sprint capable, whatever the server side sprint flag says.
Landing a hit on any entity clears that flag server side and the vanilla client never sends a new
sprint packet, because as far as it is concerned it never stopped sprinting. The server therefore
believes a sprinting player is walking, for as long as they keep holding the key. Bounding them to
walking speed for that whole time is what used to turn punching a cow into a HorizontalSpeed
violation. Sprinting is legitimate at any moment anyway, so allowing it costs the check nothing real,
and the same reasoning applies to the sprint jump boost, which is now allowed on any jump.

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

The bail out list is narrower than the one the vertical checks use. Everything that can put a player on
a surface the server models differently is still excluded, but a cobweb is not: a web slows a player, it
never puts them on the ground, so a ground claim inside one is exactly as impossible as a ground claim
in open air. NoWeb modules that fake ground contact to shed the web live in that gap.

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

Runs a spending account against the wall clock, in the style Grim uses. The balance starts
`clock-drift-millis` behind real time, every movement packet spends 50 ms of it, and the balance is
never allowed to fall further behind than that same drift. A client whose clock runs fast sends packets
closer together than 50 ms apart, spends faster than real time replenishes, and eventually overdraws.
Every overdraw is a flag, and the balance is pulled back by one tick so the next one has to be earned
again.

The earlier version accumulated drift and only flagged past 600 ms of it, while resetting outright on
any gap over a second. A 1.004 timer drifts 0.2 ms per tick, so it needed roughly 3000 uninterrupted
packets to reach that threshold and any pause wiped the progress. It was free in practice. Starting the
balance one drift window behind and flagging the moment it is overdrawn removes that.

The drift window is what a legitimate client lives inside. It cannot be banked: a player who pauses,
lags or stands still has the balance clamped back to exactly one window behind, never further, so a lag
spike cannot be converted into a burst afterwards. Latency is credited separately up to
`maximum-latency-credit-millis`, and the whole check suspends while the server itself is lagging.

One honest limitation. UAGC reads Paper events, not packets, so it only sees movement that changes
position or rotation. A perfectly stationary client sends packets this check never observes. That makes
it weaker than a packet level timer, but it does not weaken it against a moving player, which is when a
timer is worth having.

| Option | Default | Meaning |
| --- | --- | --- |
| `clock-drift-millis` | 120 | how far behind real time the balance sits and may fall |
| `maximum-latency-credit-millis` | 1000 | cap on the extra allowance given for ping |
| `severity-scale` | 200 | overdraw mapping to full severity |

## Combat

### reach

Measures the distance from the attacker's eye to the target's bounding box and compares it against the
attacker's own `ENTITY_INTERACTION_RANGE` attribute, scaled by the `SCALE` attribute. A server that
grants extended reach through an attribute is respected automatically.

For player targets the minimum distance across their recent tracked positions is used, so a target who
was closer a moment ago explains the measurement instead of producing a violation. Attackers in a
vehicle or riptiding are skipped because their positions desync.

Mobs are not tracked that way, and the position the server holds for one is always a little ahead of
where the client drew it when the click was sent. The target is therefore also rewound along its own
velocity for up to `maximum-rewind-ticks`, scaled by the attacker's ping, and the attacker's own last
tick of motion is added to the tolerance because the eye position is sampled after their move for that
tick was applied. Both of those are ordinary desync on a vanilla hit, not evidence.

Even then a single stretched hit means nothing, so `required-streak` consecutive attacks must all sit
past the limit before anything is reported, and any hit inside the limit clears the streak. The worst
distance in the streak is what gets reported.

| Option | Default | Meaning |
| --- | --- | --- |
| `tolerance` | 0.06 | absolute slack |
| `latency-tolerance` | 0.03 | extra slack per 100 ms of ping |
| `severity-scale` | 1.2 | excess distance mapping to full severity |
| `required-streak` | 3 | consecutive attacks past the limit before flagging |
| `streak-window-ticks` | 60.0 | idle ticks between attacks that clear the streak |
| `minimum-rewind-ticks` | 3 | target rewind applied at any ping |
| `maximum-rewind-ticks` | 8 | cap on the ping scaled target rewind |
| `attacker-motion-cap` | 0.4 | cap on the attacker's own last tick of motion added as slack |

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

### velocity

Knockback is the one piece of movement the server hands out rather than observes. When a player is hit,
pushed or blown up, the server sends a velocity and expects the client to travel it. Velocity modules
keep some fraction of it, or none at all, which is how a player stays out of a combo or refuses to be
pushed off a ledge.

The check records the horizontal velocity the server sent and the direction it pointed, then watches the
following ticks and keeps the largest movement the player made *along that direction*. Movement across
or against it is ignored, so running back in afterwards proves nothing either way. At the end of the
window the peak is compared against the magnitude that was sent.

Everything that can legitimately eat knockback ends the measurement instead of counting against the
player: a wall, a liquid, a ladder, a web, slime, honey, a vehicle, gliding, riptiding, flight, an
unloaded chunk, a broken tick sequence, or a second velocity arriving on top of the first. Below
`minimum-magnitude` nothing is judged at all, because a small nudge carries no evidence. On top of that
`required-samples` separate knockbacks have to come up short in a row, and any hit taken properly clears
the count.

| Option | Default | Meaning |
| --- | --- | --- |
| `minimum-magnitude` | 0.2 | horizontal velocity below which nothing is judged |
| `minimum-ratio` | 0.45 | share of the knockback the player must actually travel |
| `response-ratio` | 0.45 | share that counts as having started to move |
| `window-ticks` | 10.0 | ticks watched after the velocity, plus the player's latency |
| `required-samples` | 3 | knockbacks that came up short in a row before flagging |
| `severity-scale` | 0.35 | shortfall that maps to full severity |

### knockback_delay

The same family, one step subtler. The client takes the whole knockback but holds the velocity packet
for a couple of hundred milliseconds first, so the movement lands after the moment it mattered. The
module in the wild advertises this as a delay of 100 to 300 ms with an adjustable chance.

The trick is telling that apart from ordinary latency, and latency is measurable. The check subtracts
the player's full round trip time from the observed response, not half of it, which already hands them
more slack than the network can honestly claim. Whatever is left over is time the client sat on the
packet. Two of those in a row are needed before anything is reported.

A hit that never lands at all is not this check's business, it belongs to `velocity`, so a knockback
with no response is dropped rather than counted here.

| Option | Default | Meaning |
| --- | --- | --- |
| `minimum-magnitude` | 0.2 | horizontal velocity below which nothing is judged |
| `response-ratio` | 0.45 | share of the knockback that counts as the response |
| `window-ticks` | 12.0 | ticks watched before a knockback is treated as never taken |
| `maximum-delay-ticks` | 3.0 | ticks past the round trip time that are still forgiven |
| `required-samples` | 2 | delayed knockbacks in a row before flagging |
| `severity-scale` | 4.0 | ticks of excess delay that map to full severity |

Both of these sit in the combat category rather than movement on purpose. Knockback grants a movement
exemption, so a movement category check would exempt itself out of existence the moment it had something
to look at.

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

### no_slow

Vanilla multiplies movement input by `0.2` while an item is being used, so eating, drinking, drawing a
bow, charging a crossbow, blocking with a shield or looking through a spyglass all crawl. NoSlow
removes that multiplier client side and moves at full speed.

UAGC now samples `isHandRaised` into `ActivitySample.usingItem`, which is what makes this detectable at
all. Before that the engine had no idea an item was in use, so the speed envelope was always the
unrestricted one and NoSlow was inside it by construction, at every delay tick value.

The check runs the same envelope as `horizontal_speed` with the movement speed scaled by
`use-item-multiplier`, so it inherits friction, attributes, potion effects and surface handling for
free. Starting to use an item at a sprint does not snap you to the slow speed, you coast down over
several ticks, so `settle-ticks` passes before anything is measured and the envelope decays naturally
from whatever speed you were carrying.

| Option | Default | Meaning |
| --- | --- | --- |
| `use-item-multiplier` | 0.2 | vanilla input multiplier while an item is in use |
| `relative-tolerance` | 0.05 | proportional slack on the clamped envelope |
| `absolute-tolerance` | 0.005 | absolute slack on the clamped envelope |
| `settle-ticks` | 4.0 | ticks after the item is raised before measuring |
| `velocity-grace-ticks` | 20.0 | ticks after server applied velocity that are ignored |
| `required-streak` | 4 | consecutive excesses before flagging |
| `severity-scale` | 0.3 | proportional excess mapping to full severity |

Modules with a delay tick setting do not keep the item raised. They drop the use for a fixed number of
ticks, sprint through the gap at full speed and raise it again, so every tick the server measures is
either honestly slowed or honestly not using an item. The envelope alone can never see that, which is
why a delay of four ticks or more used to walk straight through the check.

The second half of the check watches the release pattern instead of the speed. It records the length
of every gap between two uses, and the distance covered inside those gaps. A gap only counts while it
stays under `blink-maximum-gap-ticks`, so putting the item away normally clears the record. Once
`blink-required-cycles` gaps have been seen, the gap lengths must agree to within
`blink-gap-jitter-ticks` and the mean speed inside them must be more than `blink-speed-ratio` times the
slowed terminal speed. A hand on a mouse never releases on the same tick count eight times running, a
module with a delay setting does nothing else.

| Option | Default | Meaning |
| --- | --- | --- |
| `blink-minimum-gap-ticks` | 1 | shortest release gap that counts as a cycle |
| `blink-maximum-gap-ticks` | 12 | longest release gap still treated as one pattern |
| `blink-required-cycles` | 8 | release gaps needed before the pattern is judged |
| `blink-gap-jitter-ticks` | 1 | spread allowed between the shortest and longest gap |
| `blink-speed-ratio` | 1.6 | how far past the slowed terminal speed the gaps must run |
| `blink-severity-scale` | 0.4 | proportional excess mapping to full severity |

### no_web

A cobweb sets a stuck speed multiplier on the player. The tick after it is set, vanilla multiplies the
entire movement vector by `0.25` horizontally and `0.05` vertically, and then zeroes the carried delta.
That last part is the one that matters: there is no momentum in a web. Every tick starts from zero and
gets whatever one tick of input can build, times the multiplier. A sprinting player therefore crawls at
about `0.0325` blocks a tick and sinks at about `0.004`.

The first version of this check modelled a web as `RestrictedSpeedModel` with a `0.25` multiplier on the
speed, which kept the momentum term and settled at `0.0716` a tick, more than twice the real clamp.
Anything under that walked through untouched, which is exactly what a module set to a low multiplier
does. The check now models the real thing: the allowance is one tick of input times the multiplier, plus
whatever motion was carried into the web on the entry tick, and the carried term is clamped to the
allowance so a cheat cannot feed its own speed back in.

The clamp is applied to the tick *after* the web is seen, because that is when vanilla consumes the
multiplier. That also means leaving the web does not end the check early: the last tick in the web still
owes one clamped tick, and a module that only unclamps after exiting is measured on it.

Vertical motion is bounded by the same rule, which is what catches the modules that leave horizontal
speed alone and simply drop through. Falling through a web is not slow in vanilla, it is impossible.

The sampling behind all of this was the other half of the problem. Cobwebs used to be read from a single
block column at the player's feet, so a web at chest height, a web the player was falling past, or a web
their box overlapped from an adjacent block simply did not exist as far as the engine was concerned. The
whole player box is scanned now, in one pass that also picks up powder snow, berry bushes, honey,
bubble columns, scaffolding and climbables the same way.

| Option | Default | Meaning |
| --- | --- | --- |
| `web-multiplier` | 0.25 | vanilla cobweb clamp on horizontal movement |
| `relative-tolerance` | 0.05 | proportional slack on the clamp |
| `absolute-tolerance` | 0.005 | absolute slack on the clamp |
| `velocity-grace-ticks` | 20.0 | ticks after server applied velocity that are ignored |
| `required-streak` | 2 | offending speed ticks in one stay before flagging |
| `severity-scale` | 0.3 | proportional excess mapping to full severity |
| `web-vertical-multiplier` | 0.05 | vanilla cobweb clamp on vertical movement |
| `maximum-descent` | 0.02 | floor under the descent allowance, vanilla sits near 0.004 |
| `vertical-required-ticks` | 2 | offending descent ticks in one stay before flagging |
| `vertical-severity-scale` | 1.5 | proportional excess mapping to full severity |

A NoWeb that fakes ground contact to escape the clamp is a `ground_spoof` case rather than this one,
and that check now runs inside webs for exactly that reason.
