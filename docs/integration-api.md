# Integration API

Other plugins tell UAGC about behaviour it could not otherwise explain. A launch pad, a custom dash
ability, a minigame teleport or a plugin driven knockback all look like cheating from the outside, and
the API is how a trusted plugin says otherwise.

The API lives in `io.github.no1qq.uagc.api` and contains no implementation classes.

## Getting the API

UAGC registers itself with the Bukkit services manager.

```java
RegisteredServiceProvider<UagcApi> provider =
        Bukkit.getServicesManager().getRegistration(UagcApi.class);

if (provider != null) {
    UagcApi uagc = provider.getProvider();
}
```

Depend on UAGC as `compileOnly` and declare it as a soft dependency so your plugin still loads if UAGC
is absent. Guard every call so a server without UAGC is not a hard failure.

## Everything is attributed

There is no anonymous entry point. You obtain a handle that carries your plugin name, and every grant
made through it is recorded in the target player's evidence log with that name attached.

```java
UagcIntegration integration = uagc.integration("MyAbilities");
```

Staff running `/uagc exemptions <player>` see which plugin granted what, why, and how long is left.
That is deliberate: an integration that silences detection should be auditable.

## Reporting legitimate behaviour

```java
integration.reportTeleport(player.getUniqueId(), "warp to arena");

integration.reportVelocity(player.getUniqueId(), 1.4D, 0.8D, 0.0D, "launch pad");

integration.reportCustomMovement(player.getUniqueId(), Duration.ofSeconds(3), "grapple hook");

integration.reportCustomSpeed(player.getUniqueId(), Duration.ofSeconds(10), "haste ability");
```

`reportVelocity` is the strongest of these, because it hands UAGC the exact vector you applied. The
speed check raises its envelope to that magnitude and lets it decay naturally, so the movement is
modelled rather than merely excused. Prefer it over a blanket movement exemption whenever you know the
vector.

Call these **before** you perform the action, so the exemption is in place when the resulting movement
packets arrive.

## Scoped exemptions

For anything not covered above, grant a scoped exemption directly.

```java
ExemptionHandle handle = integration.exempt(
        player.getUniqueId(),
        ExemptionKind.MOVEMENT,
        Duration.ofSeconds(5),
        "riding the custom elevator");

if (handle != null && handle.isActive()) {
    handle.revoke();
}
```

| Kind | Suppresses |
| --- | --- |
| `TELEPORT` | movement checks, for a position change you caused |
| `VELOCITY` | movement checks, for an impulse you applied |
| `MOVEMENT` | movement checks generally |
| `SPEED` | movement checks, for a sustained speed modifier |
| `COMBAT` | combat checks |
| `INTERACTION` | interaction checks |
| `INVENTORY` | inventory checks |

`exempt` returns `null` if UAGC is not tracking that player, for example if they have already
disconnected. Always null check the handle.

### Durations are bounded

Exemption durations are clamped to a maximum of ten minutes. This is deliberate. There is no API to
disable UAGC for a player indefinitely, and there never will be, because a permanent silent exemption
granted by a third party plugin is indistinguishable from a bug that disables the anti cheat.

If you need something longer lived, re issue the exemption while the condition holds, or use the
permission based bypass system, which is visible to staff and controlled by the server operator rather
than by a plugin.

A duration of zero or a null duration defaults to two seconds.

## Querying state

```java
UagcQuery query = uagc.query();

boolean tracked  = query.isTracked(playerId);
boolean exempt   = query.isExempt(playerId, ExemptionKind.MOVEMENT);
boolean frozen   = query.isFrozen(playerId);
boolean bypassed = query.hasBypass(playerId);

double speedLevel = query.violationLevel(playerId, "horizontal_speed");
Map<String, Double> levels = query.violationLevels(playerId);
List<String> checks = query.checkIds();
List<String> active = query.activeExemptions(playerId);
```

`violationLevel` applies decay before returning, so the value is current rather than the raw stored
number. Unknown check ids return `0.0` rather than throwing.

## Versioning

`UagcApi.API_VERSION` is the API contract version, currently `1.0`, and `uagc.apiVersion()` returns it
at runtime. It is independent of the plugin version. Check it if you rely on something recent.

The interfaces in `io.github.no1qq.uagc.api` are the supported surface. Classes under `engine` and
`bukkit` are internal and will change without notice, so do not compile against them.

## Threading

The query methods and exemption grants are safe to call from any thread. `ExemptionState` is backed by
an atomic array, `BypassState` by volatile fields and a copy on write list, and player lookup by a
concurrent map.

Everything else, including anything you do with the Bukkit API in response, belongs on the server
thread as usual.
