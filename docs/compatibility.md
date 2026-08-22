# Compatibility

UAGC targets **PaperMC 1.21 through 1.21.11** with a single jar, on Java 21.

| | |
| --- | --- |
| Supported servers | Paper 1.21, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11 |
| Java | 21 |
| Declared `api-version` | 1.21 |
| Forks | Purpur, Pufferfish and other Paper forks work, they keep the Paper API surface |
| Spigot and CraftBukkit | not supported, UAGC uses `paper-plugin.yml`, the Paper Brigadier command API and Paper only events |

Paper never shipped a 1.21.2 API, that release was superseded by 1.21.3 within days, so it is absent
from the list on purpose.

## How the range is guaranteed

The build compiles the shipped jar against the **oldest** API in the range and then compiles the same
sources a second time against the **newest**.

```
compileJava                 sources against paper-api 1.21     produces the jar that ships
compileAgainstNewestApi     sources against paper-api 1.21.11  verification only, no output shipped
```

`compileAgainstNewestApi` is wired into `check`, so a plain `./gradlew build` fails if either end of
the range stops working.

That pair of compiles proves two different things:

- compiling against 1.21 proves UAGC never touches a method that only exists in a later patch, and it
  makes the bytecode reference the oldest signatures, which every later Paper release still honours
- compiling against 1.21.11 proves nothing UAGC uses was removed or reshaped on the way to the newest
  patch

The MiniMessage artifact is pinned the same way: 4.17.0 for the shipped compile, which is what Paper
1.21 ships, and 4.26.1 for the verification compile.

## The one thing a recompile could not solve

Minecraft 1.21.2 renamed every attribute. `GENERIC_MOVEMENT_SPEED` became `MOVEMENT_SPEED`,
`PLAYER_ENTITY_INTERACTION_RANGE` became `ENTITY_INTERACTION_RANGE`, and so on for all twelve that
UAGC reads. Bukkit also turned `Attribute` from an enum into a registry backed interface.

A jar compiled against either naming crashes with `NoSuchFieldError` on the other, which is why most
plugins that read attributes only work on one side of 1.21.2. Compiling against the older API does
not help here, because the old names were deleted rather than deprecated.

`bukkit/compat/Attributes` resolves all twelve by name at runtime instead, current name first, old
name second, so the same jar reads real attribute values on both sides of the rename:

```java
public static final Attribute MOVEMENT_SPEED = resolve("MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED");
```

If a name cannot be resolved at all, the value falls back to the vanilla constant and UAGC logs one
warning at startup naming exactly which attributes it could not read. That matters, because silently
assuming vanilla numbers on a server that granted a player extra speed or reach is how an anti cheat
starts punishing legitimate players.

## Adding support for a newer Minecraft version

1. Raise `paperApiVersionNewest` and `adventureVersionNewest` in `build.gradle.kts`.
2. Run `./gradlew build`. `compileAgainstNewestApi` reports anything that was removed or renamed.
3. If a symbol moved rather than disappeared, resolve it at runtime in `bukkit/compat/` the way
   `Attributes` does, instead of raising the shipped compile target and dropping older servers.
4. Update the table at the top of this file and the requirements in `README.md`.

Raising `paperApiVersion`, the shipped compile target, is a deliberate decision to drop every server
below it. Do that only when a version is genuinely being dropped from support.
