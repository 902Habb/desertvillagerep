# DesertVillageRep

`DesertVillageRep` is a Spigot plugin for a Java Minecraft server that tracks three live reputation categories:

- `Builder`
- `Trader`
- `Protector`

It renders a floating `TextDisplay` board in-world, stores data in SQLite, supports legacy villager-trade imports from vanilla stats files, and lets admins define tracked regions entirely in-game.

## Features

- Floating reputation board built from native `TextDisplay` entities
- Region setup with `/rep admin pos1`, `/rep admin pos2`, and `/rep admin setregion ...`
- Builder rep from configured build materials in village and market regions
- Trader rep from villager trades, villager breeding, villager cures, and village-care actions
- Protector rep from player-caused hostile mob kills inside a dedicated protection region
- One-time or repeatable legacy import of vanilla villager trade stats
- Manual admin rep adjustments with optional reasons
- Builder project milestones with one-time completion awards per player

## Build

This project is configured as a Maven plugin project targeting Java 21.

### Local no-password toolchain

This repo can be built with the project-local Java and Maven toolchain that was unpacked into `.toolchain/`.

```bash
./scripts/build-local.sh
```

That command uses:

- local Java at `.toolchain/jdk/Contents/Home`
- local Maven at `.toolchain/maven`
- local Maven cache at `.m2repo`

### Standard Maven build

```bash
mvn package
```

The shaded jar will be produced in `target/`.

## Apex Hosting Deployment

1. Build the jar locally.
2. Upload the jar from `target/` into your server's `plugins/` folder.
3. Start the server once so the plugin creates its data/config files.
4. Build your physical board in-game.
5. Stand where you want the floating text centered and run `/rep admin createboard`.
6. Set `village`, `market`, and `protector` with `pos1`, `pos2`, and `setregion`.
7. Run `/rep admin import legacy all` if you want historical villager trades seeded into Trader rep.

## Core Commands

### Player

- `/rep`
- `/rep top <builder|trader|protector>`
- `/rep stats [player]`
- `/rep legacy [player]`

### Admin

- `/rep admin pos1`
- `/rep admin pos2`
- `/rep admin setregion <village|market|protector>`
- `/rep admin createboard`
- `/rep admin moveboard`
- `/rep admin addproject builder <id> <points>`
- `/rep admin completeproject <id> <player>`
- `/rep admin adjust <player> <category> <amount> [reason]`
- `/rep admin import legacy <all|player>`
- `/rep admin refresh`
