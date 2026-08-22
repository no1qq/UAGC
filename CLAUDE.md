# CLAUDE.md

## !!! RULE 0 - NO AI COMMENTS. THIS OVERRIDES EVERYTHING BELOW !!!

! MOST IMPORTANTLY !
If you commit and push to the GitHub, ALWAYS do with business.rakid0y@gmail.com, user no1qq and NOT anyhow make Claude a contributor, NOT ANYHOW!
ALWAYS write the commit titles in lowercase and without them looking like AI slop comments.
REPOSITORY: https://github.com/no1qq/UAGC

**NEVER ADD EXPLANATORY COMMENTS TO CODE.** Not to `.java`, `.json`
not to any file in this repo. No file-header banners, no
section dividers, no "why this works" essays, no restating what the next line
already says.

**WHEN YOU OPEN ANY FILE, DELETE ANY COMMENTS YOU FIND IN IT.** Leftovers from
earlier sessions are bugs to fix, not context to preserve.

**THE ONLY EXCEPTIONS:**
- `.bat` scripts - keep their comments.
- User-facing UI strings text - that is content, not comment.
- `.md` docs - prose is the point.

**IF A COMMENT IS GENUINELY UNAVOIDABLE:** one short line, all lowercase, plain
ascii words only. No em dashes, no box drawing, no `====` or `────` rules, no
emoji. Do not bloat a file with them.

Anything worth explaining goes in this file instead. That is what the
"Gotchas" section below is for.

! ALSO !
Don't launch the latest builds by yourself. I'm the tester, and you shouldn't be the one launching them, just building them!

! NO EM DASHES !
NEVER write an em dash. Not in `.md` files, not in UI strings, not in this file.
It is the single clearest tell that a machine wrote
the text. Use the plain hyphen `-` that sits on a normal Windows keyboard.
Same goes for the en dash and every other fancy unicode dash: only `-` is
allowed. When you open a file that still has one, replace it.

---

## What this project is

UAGC (UltimateAntiGamingChair) is a context aware anti cheat plugin for PaperMC 1.21.11 on Java 21.

Its guiding rule: do not punish behaviour because it is unusual, punish it when the evidence shows it
cannot be explained by legitimate mechanics, plugins, mods, server behaviour or network conditions.

## Build and test

```
./gradlew build      compile, test, produce the jar
./gradlew test       tests only
./gradlew compileJava
```

Jar lands in `build/libs/UAGC-<version>.jar`. The Gradle wrapper is committed and pinned to a
checksum, so no local Gradle install is needed. Java 21 comes from a toolchain.

Only two dependencies exist, both `compileOnly`: `paper-api` and `adventure-text-minimessage`.
Nothing is shaded. Do not add a dependency without a real reason.

## Workflow rules

- **Always commit and push after any change.** Do not stop at a local commit and do not ask first.
  Authorship rules in RULE 0 apply every single time.
- **Never launch a build.** Julian is the tester. Build it, verify it through the test suite, hand it
  over.
- **Always run `./gradlew build` before committing.** The suite is fast and catches real breakage.

## Where things live

```
src/main/java/io/github/no1qq/uagc/
|-- api/                  public API other plugins compile against, versioned, keep stable
|-- engine/               the anti cheat itself, NO bukkit imports anywhere in here
|   |-- alert/            AlertService, AlertSettings, Alert, AlertSeverity
|   |-- bypass/           BypassService, BypassState, BypassScope, TemporaryBypass
|   |-- check/            Check, CheckEngine, CheckRegistry, CheckContext, CheckResult,
|   |                     CheckDefinition, ConfidenceModel, RegisteredCheck, DebugSink
|   |   +-- event/        MovementEvent, AttackEvent, BlockBreakCheckEvent,
|   |                     BlockPlaceCheckEvent, TargetSample
|   |-- checks/           the actual detections
|   |   |-- CheckBootstrap.java     register every new check HERE
|   |   |-- movement/     VerticalMotion, HorizontalSpeed, GroundSpoof, NoFall, Timer,
|   |   |                 MovementApplicability (shared bail out logic)
|   |   |-- combat/       Reach, AttackRhythm
|   |   |-- interaction/  FastBreak, BlockReach, InvalidPlacement
|   |   +-- protocol/     InvalidPosition
|   |-- config/           plain config records, no yaml parsing in here
|   |-- evidence/         EvidenceLog, EvidenceEntry, EvidenceType
|   |-- exemption/        ExemptionType, ExemptionState, ExemptionGrant
|   |-- freeze/           FreezeService, FreezeRecord, FreezeStore
|   |-- movement/         Vec3, Rotation, MovementSnapshot, MovementPredictor,
|   |                     SurfaceSample, ActivitySample, AttributeSample, EffectSample
|   |-- platform/         interfaces the bukkit layer implements
|   |-- player/           PlayerData, PlayerDataManager, MovementTracker, LatencyTracker,
|   |                     CombatState, InteractionState, VelocityState
|   |-- punishment/       PunishmentService, PunishmentRule, PunishmentRecord, PunishmentAction
|   |-- util/             MathUtil, RingBuffer, DurationParser
|   +-- violation/        Violation, ViolationTracker
+-- bukkit/               the Paper adapter
    |-- UagcPlugin.java   entry point, enable/disable, listener and command registration
    |-- UagcRuntime.java  wires every service together, start here to trace anything
    |-- api/              UagcApiImpl, IntegrationImpl
    |-- command/          UagcCommandTree plus Inspection/Moderation/Bypass/Punishment/Alert/Debug
    |-- config/           ConfigLoader, the only place yaml becomes engine records
    |-- debug/            DebugService
    |-- freeze/           YamlFreezeStore
    |-- listener/         Connection, Movement, State, Combat, Interaction, Freeze
    |-- message/          Messages, MiniMessage with a plain text fallback
    |-- platform/         BukkitServerContext, BukkitMessageGateway,
    |                     BukkitEnforcementGateway, BukkitSupportQuery
    +-- sample/           PlayerSampler, BlockSampler, world state becomes plain records here

src/main/resources/
|-- config.yml           default configuration, mirror any new check option here
+-- paper-plugin.yml     plugin descriptor and permission declarations

src/test/java/io/github/no1qq/uagc/
|-- support/             EngineHarness, SnapshotBuilder, Surfaces, TestClock, StubChecks,
|                        RecordingMessageGateway, RecordingEnforcementGateway
+-- engine/              tests mirroring the engine package layout
    +-- check/           MovementCheckHarness and EventCheckHarness live here, same package
                         as CheckContext so they can call its package private prepare method
```

## The one rule that shapes everything

**`engine` must never import a Bukkit class.** Not one.

The Paper layer flattens live world and player state into plain records, the engine reasons over them,
and anything it wants done goes back out through an interface in `engine/platform`. That separation is
the only reason 110 tests can run the real detection logic without starting a server.

If you find yourself wanting a Bukkit type inside `engine`, the answer is either a new field on a
sample record or a new method on a platform interface. Never an import.

Check it after any engine change:

```
grep -rn "org.bukkit\|io.papermc" src/main/java/io/github/no1qq/uagc/engine/
```

That must return nothing.

## Common tasks

**Add a check.** Create the class in the right `engine/checks/<category>/` package, register one line
in `CheckBootstrap.registerDefaults`, add a section to `config.yml`, write tests both ways. Nothing
else needs touching: alerts, violations, evidence, bypass, exemptions, punishments and the command
tree all pick it up automatically. Full walkthrough in `docs/development.md`.

**Change how a detection behaves.** Prefer adding a config option over hardcoding. Read it with
`context.config().option("name", fallback)` and mirror it in `config.yml` plus `docs/checks.md`.

**Add a config setting.** Add it to the record in `engine/config/`, read it in
`bukkit/config/ConfigLoader`, add it to `config.yml`, document it in `docs/configuration.md`.

**Add a command.** Add the node in the matching `bukkit/command/` class, declare its permission in
`paper-plugin.yml`, list it in `docs/commands-and-permissions.md` and in `HelpText`.

**Add an exemption reason.** Add the constant to `ExemptionType` with the categories it affects and a
default duration in ticks, then grant it from the listener that observed the cause.

## Gotchas

Things that already bit us once. Read before changing the related area.

- **`paper-plugin.yml` has a strict schema.** It is not `plugin.yml`. Unknown keys fail plugin load.
  It is `authors:` as a list, never `author:`. The valid keys come from `PluginMeta`: name, main,
  version, description, authors, contributors, website, api-version, permissions, dependencies,
  provides, logger-prefix, default-permission. Commands are NOT declared here, they register through
  Brigadier at runtime.
- **`paper-plugin.yml` is run through Gradle `expand()`.** A literal dollar sign in that file will
  break the build. Only `config.yml` is copied untouched.
- **`Player.isOnGround()` is deprecated on purpose.** It is the client's own claim, which is exactly
  what `ground_spoof` and `no_fall` compare against the server side collision test. The warning is
  suppressed at the single sampling method in `PlayerSampler`. Do not "fix" it and do not treat the
  value as truth anywhere else.
- **MiniMessage is not in `paper-api`.** It is a separate `compileOnly` artifact that Paper provides
  at runtime. `Messages` degrades to plain text if it is ever missing, so keep formatting going
  through that class.
- **Exemptions expire in server ticks, not milliseconds.** During lag fewer ticks pass, so an
  exemption lasts longer in wall clock time. That is deliberate, because late packets need it.
- **Bypassed players are still evaluated.** The check runs, the would be detection is recorded as
  suppressed evidence, and only violations, alerts and punishments are skipped. Staff must always be
  able to see what a bypass is hiding. Do not short circuit this to save cycles.
- **Per player check state goes in `Check.createState()`.** It is stored in an array on `PlayerData`
  and cleared on quit. Never keep a `Map` keyed by player inside a check, that is how state leaks.
- **Checks must bail out before measuring.** Use `MovementApplicability` for liquid, climbing,
  gliding, riptiding, flight, vehicles and unusual surfaces, and `isMeasurable` for tick continuity.
  A check that measures a situation its model does not cover is a false positive waiting to happen.
- **Expensive lookups go last.** `context.support()` scans nearby entities. Ask it only in the moment
  before flagging, never on every packet.
- **Read attributes, do not assume vanilla numbers.** `MOVEMENT_SPEED` already contains potion
  effects, plugin modifiers and sprint. `ENTITY_INTERACTION_RANGE` and `BLOCK_INTERACTION_RANGE`
  already contain whatever the server granted. Hardcoding vanilla constants is how you punish
  legitimate players.
- **Bans go through the profile ban list by UUID.** `BanList.Type.NAME` is deprecated and ambiguous
  against `BanList<String>`. Use `BanListType.PROFILE` with `Bukkit.createProfile`.
- **Setbacks teleport, they do not cancel the move event.** Cancelling at MONITOR priority fights
  with other plugins.
- **The test harnesses live in the same package as what they touch.** `MovementCheckHarness` sits in
  `engine.check` so it can call the package private `CheckContext.prepare`. Keep it there.

## Documentation

Keep these current when behaviour changes. They are the only place explanation belongs, since code
carries no comments.

- `README.md` overview, build, what is implemented
- `docs/architecture.md` engine and platform split, confidence model, limitations, design decisions
- `docs/checks.md` what every check models and its options
- `docs/configuration.md` every `config.yml` key
- `docs/commands-and-permissions.md` command tree, permissions, bypass, admin mode, freeze
- `docs/integration-api.md` the API other plugins use
- `docs/development.md` adding a check, the test harnesses, coverage
