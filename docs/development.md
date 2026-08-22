# Development

## Building and testing

```
./gradlew build      compile, test and produce the plugin jar
./gradlew test       run the test suite only
./gradlew compileJava
```

The Gradle wrapper is committed and pinned with a distribution checksum, so no local Gradle install is
needed. Java 21 is required and is selected through a toolchain, so the build works regardless of which
JDK launched Gradle.

Only two dependencies exist, both `compileOnly`: the Paper API and Adventure MiniMessage, which Paper
provides at runtime. Nothing is shaded and there is no relocation step.

`build` also runs `compileAgainstNewestApi`, which compiles the same sources against the newest
supported Paper API. The jar itself is compiled against the oldest one. Both ends of the supported
version range therefore break the build rather than a player's server. See
[compatibility](compatibility.md).

## Adding a check

A check needs one new file and one line of registration. Nothing else has to change: alerts,
violations, evidence, bypass, exemptions, punishments and the command tree all pick it up.

### 1. Write the check

```java
public final class ExampleCheck implements Check<MovementEvent, ExampleCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("example", "Example", CheckCategory.MOVEMENT)
            .description("what this check actually models")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        int consecutive;

        void reset() {
            consecutive = 0;
        }
    }

    @Override
    public CheckDefinition definition() {
        return DEFINITION;
    }

    @Override
    public Class<MovementEvent> eventType() {
        return MovementEvent.class;
    }

    @Override
    public State createState() {
        return new State();
    }

    @Override
    public CheckResult inspect(CheckContext context, MovementEvent event, State state) {
        MovementSnapshot snapshot = event.snapshot();

        if (MovementApplicability.hasAlternateVerticalPhysics(snapshot)
                || !MovementApplicability.isMeasurable(context.player(), snapshot)) {
            state.reset();
            return CheckResult.passed();
        }

        double observed = snapshot.horizontalDistance();
        double allowed = context.config().option("allowed", 0.5D);
        if (observed <= allowed) {
            state.reset();
            return CheckResult.passed();
        }

        state.consecutive++;
        if (state.consecutive < context.config().optionInt("required-streak", 2)) {
            return CheckResult.passed();
        }

        double severity = ConfidenceModel.severity(observed, allowed,
                context.config().option("severity-scale", 0.3D));

        return CheckResult.flag(severity, "movement exceeded the modelled bound")
                .with("observed", observed)
                .with("allowed", allowed)
                .build();
    }
}
```

Per player state is returned by `createState`. It is stored in an array slot on `PlayerData`, indexed
by the check, and cleared when the player disconnects. Never keep a map keyed by player inside a check;
that is how state leaks.

### 2. Register it

Add one line to `CheckBootstrap.registerDefaults`:

```java
registry.register(new ExampleCheck(), config);
```

### 3. Configure it

Add a section under the matching category in `src/main/resources/config.yml`. If you skip this the
check still runs on built in defaults, but administrators cannot tune it.

### 4. Test it

Write both directions. A test that only proves the check catches cheating is half a test.

```java
class ExampleCheckTest {

    @Test
    void legitimateMovementIsNeverFlagged() {
        MovementCheckHarness<ExampleCheck.State> harness =
                new MovementCheckHarness<>(new ExampleCheck());
        Vec3 position = Vec3.ZERO;
        for (int tick = 1; tick <= 40; tick++) {
            Vec3 next = new Vec3(position.x() + 0.28D, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .sprinting(true)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged());
    }
}
```

## Writing checks that do not produce false positives

The rules that the existing checks follow:

- **Model the mechanic, do not threshold the symptom.** A constant that happens to work today breaks
  the first time someone uses ice, a speed potion or a launch pad.
- **Read attributes rather than assuming vanilla.** `MOVEMENT_SPEED`, `GRAVITY`, `JUMP_STRENGTH`,
  `SCALE`, `ENTITY_INTERACTION_RANGE` and `BLOCK_INTERACTION_RANGE` already contain the effects of
  potions and plugin modifiers.
- **Bail out when the model does not apply.** `MovementApplicability` covers liquid, climbing, gliding,
  riptiding, flight, vehicles, and every unusual surface. Use it.
- **Require continuity.** If the tick gap is not one, the measurement spans an unknown amount of time.
  `isMeasurable` checks this.
- **Return a graduated severity.** A marginal excess and an impossible one should not produce the same
  number, because that is what makes the confidence model meaningful.
- **Do expensive checks last.** Ask `context.support()` only in the moment before flagging.
- **Emit useful details.** Whatever you put in the result is what staff see during an investigation and
  what ends up attached to a punishment.

## The test harnesses

Three pieces of support code make the engine testable without a server:

| Harness | Use |
| --- | --- |
| `MovementCheckHarness` | feed movement snapshots to one check, updating the movement tracker |
| `EventCheckHarness` | feed any other event type to one check |
| `EngineHarness` | run the whole engine with recording gateways |

`SnapshotBuilder` and `Surfaces` construct movement states fluently, `TestClock` controls time, and
`RecordingMessageGateway` and `RecordingEnforcementGateway` capture what the engine tried to do so
tests can assert on alerts, kicks, bans, setbacks and commands.

`EngineHarness` is the one to reach for when testing behaviour that spans subsystems: violation
accumulation, bypass suppression, exemption gating, error isolation, alert routing and punishment
thresholds are all covered this way.

## Current coverage

The suite covers player state, movement and gravity models, teleport and velocity transitions,
violation accumulation and decay, the confidence model under high ping and low tick rate, bypass
scoping, exemption expiry, freeze lifecycle, punishment rules including dry run, alert permission
filtering and rate limiting, and check error isolation.

For each detection check there are tests in both directions: legitimate sequences that must never flag,
and cheating sequences that must.

```
./gradlew test
```
