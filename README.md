# UAGC - UltimateAntiGamingChair

UAGC is a context aware anti cheat platform for PaperMC servers.

It is built around one principle:

> Do not punish behaviour merely because it is unusual. Punish behaviour when the evidence indicates
> it violates the rules of the server and cannot reasonably be explained by legitimate game mechanics,
> server behaviour, authorised plugins, approved modifications, network conditions, or other valid context.

## Requirements

- PaperMC 1.21 through 1.21.11, every patch in between included
- Java 21

One jar covers the whole range. See [compatibility](docs/compatibility.md) for how that is guaranteed
and what is verified on every build.

## Building

The Gradle wrapper is committed, so no local Gradle install is needed.

```
./gradlew build
```

The plugin jar is written to `build/libs/UAGC-<version>.jar`. Drop it into your server `plugins` folder.

To run only the test suite:

```
./gradlew test
```

## What is implemented

| Area | State |
| --- | --- |
| Check framework, violation and confidence model | implemented |
| Exemption and context system | implemented |
| Permission based bypass with staff visibility | implemented |
| Evidence and history | implemented |
| Alerts | implemented |
| Punishment engine, automatic and manual | implemented |
| Freeze system with persistence | implemented |
| Trusted plugin integration API | implemented |
| Movement checks | vertical motion, horizontal speed, ground spoof, no fall, timer |
| Combat checks | reach, attack rhythm |
| Interaction checks | fast break, block reach, invalid placement |
| Protocol checks | invalid position |

Every check listed above performs real analysis. There are no placeholder checks.

## Documentation

- [Architecture](docs/architecture.md) - how the engine and the Paper layer are separated, and why
- [Compatibility](docs/compatibility.md) - the supported Paper versions and how one jar covers them all
- [Checks](docs/checks.md) - what each check models and how it avoids false positives
- [Configuration](docs/configuration.md) - every option in `config.yml`
- [Commands and permissions](docs/commands-and-permissions.md) - the command tree, permission hierarchy, bypass, admin mode and freeze
- [Integration API](docs/integration-api.md) - how other plugins tell UAGC about legitimate behaviour
- [Development](docs/development.md) - adding a check, the test harness, and how to run the suite

## Licence

MIT. See [LICENSE](LICENSE).
