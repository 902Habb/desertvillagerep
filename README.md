# DesertVillageRep

`DesertVillageRep` is a Spigot plugin for Java Minecraft servers that adds a server-side reputation system built around a shared village or settlement. It was designed for small multiplayer survival servers that want more personality and recognition without changing vanilla progression, gear balance, loot, or normal gameplay progression.

The plugin tracks three live reputation categories:

- `Builder`
- `Trader`
- `Protector`

It displays those scores on a floating in-world board made from native Minecraft `TextDisplay` entities. The board is intended to sit in front of a physical build, such as a sandstone wall, village noticeboard, or plaza monument.

The plugin stores data in a local SQLite database inside the plugin folder, supports legacy import of vanilla villager-trade stats from existing player stat files, and lets admins define every important area in-game with commands instead of editing coordinates by hand.

## What The Plugin Actually Does

The plugin watches for specific server events and converts them into reputation points.

### Builder Reputation

Builder reputation rewards players for placing blocks inside the configured `village` or `market` regions.

- Every placed block is worth `+1`
- Only block placement is counted
- Breaking blocks does not give points
- There is no premium block system
- There is no anti-loop protection in the current version

Builder reputation can also be awarded manually through admin-defined one-time builder projects such as:

- `fountain`
- `market`
- `gate`
- `watchtower`
- `walls`

Builder reputation also supports a legacy seed. On import, the plugin estimates historical building activity from vanilla `minecraft:used` block-item stats and stores that as a `legacyBuilderSeed`.

That means Builder rep is shown as:

- `live builder rep`
- plus `legacy imported builder rep`

### Trader Reputation

Trader reputation rewards village economy and villager care.

- `+1` for a completed villager trade
- `+3` for successful villager breeding
- `+100` for curing a zombie villager
- `+1` for placing a workstation near an unemployed villager in the `village` region, with a cooldown
- `+1` for placing a bed near villagers in the `village` region, with a cooldown

Trader reputation also supports legacy import. When you import old vanilla stats, the plugin reads historical villager trade counts from the world stats files and uses them as the player's `legacyTraderSeed`.

That means Trader rep is shown as:

- `live trader rep`
- plus `legacy imported trader rep`

### Protector Reputation

Protector reputation rewards players for killing hostile mobs anywhere on the server.

- Any hostile mob killed by a player counts
- It is not limited to a configured region
- It tracks live from normal `EntityDeathEvent` kills
- Every counted hostile mob kill is worth `+1`

The exact hostile mobs counted by this plugin are:

- `BLAZE`
- `BOGGED`
- `BREEZE`
- `CAVE_SPIDER`
- `CREEPER`
- `DROWNED`
- `ELDER_GUARDIAN`
- `ENDERMAN`
- `ENDER_DRAGON`
- `ENDERMITE`
- `EVOKER`
- `GHAST`
- `GIANT`
- `GUARDIAN`
- `HOGLIN`
- `HUSK`
- `ILLUSIONER`
- `MAGMA_CUBE`
- `PHANTOM`
- `PIGLIN`
- `PIGLIN_BRUTE`
- `PILLAGER`
- `RAVAGER`
- `SHULKER`
- `SILVERFISH`
- `SKELETON`
- `SLIME`
- `SPIDER`
- `STRAY`
- `VEX`
- `VINDICATOR`
- `WARDEN`
- `WITCH`
- `WITHER`
- `WITHER_SKELETON`
- `ZOGLIN`
- `ZOMBIE`
- `ZOMBIE_VILLAGER`
- `ZOMBIFIED_PIGLIN`

## Board Behavior

The board is not a GUI and it is not a client mod. It is a physical in-world floating text display.

- You build the physical wall or monument yourself
- The plugin places floating `TextDisplay` text in front of it
- The board shows the top four players in each category
- The footer shows the current category title holders
- The board refreshes automatically and can also be force-refreshed by command

The board position is saved in the plugin database. If you place it incorrectly, you can move it with `/rep moveboard` and the plugin will remove the old floating board and recreate it at the new location. If you want to fully clear the board and start over later, you can use `/rep removeboard`.

## Regions

The plugin uses two admin-defined cuboid regions:

- `village`
- `market`

These are defined entirely in-game by setting two corners and then saving the region.

The corners are block-based and use the world you are currently in. You do not need to edit coordinates in files or send the plugin your world save ahead of time.

## Data Storage

The plugin stores its data in the plugin folder using SQLite. This includes:

- player reputation totals
- imported legacy stats snapshots
- region definitions
- board position
- builder milestone projects
- project completion records
- manual rep adjustment history

## Permissions

- `desertrep.use`: normal player commands
- `desertrep.admin`: admin/setup commands

## Build

This project targets Java 21.

### Local No-Password Toolchain

This repo can be built using the local toolchain unpacked into `.toolchain/`.

```bash
./scripts/build-local.sh
```

That build uses:

- local Java at `.toolchain/jdk/Contents/Home`
- local Maven at `.toolchain/maven`
- local Maven cache at `.m2repo`

### Standard Maven Build

```bash
mvn package
```

The final plugin jar is produced at:

`target/desertvillagerep-0.1.0-SNAPSHOT.jar`

The `original-desertvillagerep-0.1.0-SNAPSHOT.jar` file is the unshaded pre-bundle jar and is not the one you should upload to a server.

## Apex Hosting Deployment

You do not upload the Git repo itself to Apex. You upload the compiled plugin jar.

### What To Upload

Upload this file:

`target/desertvillagerep-0.1.0-SNAPSHOT.jar`

Do not upload:

- source code files
- the `.git` folder
- the `original-...jar`
- the local `.toolchain` folder

### Exact Apex Flow

1. Build the plugin locally with `./scripts/build-local.sh`
2. Log into your Apex Hosting panel
3. Stop the server
4. Open `File Manager`
5. Open the `plugins` folder
6. Upload `desertvillagerep-0.1.0-SNAPSHOT.jar`
7. Start the server again
8. Let the server fully boot
9. Join the server in-game and run the setup commands

### Updating The Plugin Without Losing Stats

You can safely update the plugin and keep all existing scores, regions, board position, project data, and legacy snapshots.

What preserves your stats:

- the plugin data folder
- the SQLite database file
- the plugin config file

Important files and folders on the server:

- `plugins/DesertVillageRep/`
- `plugins/DesertVillageRep/desertvillagerep.db`
- `plugins/DesertVillageRep/config.yml`

Safe update flow for a future `v1.1`-style update:

1. Build the new jar locally
2. Stop the Apex server
3. In Apex `File Manager`, open the `plugins` folder
4. Replace the old `DesertVillageRep` jar with the new one
5. Do not delete the `plugins/DesertVillageRep/` folder
6. Start the server again

As long as you only replace the jar and keep the plugin data folder, your stats should remain intact.

Extra safety tip:

- before updating, download a backup copy of `plugins/DesertVillageRep/`
- if you want maximum safety, also back up the whole server or world before major plugin updates

What would cause data loss:

- deleting `plugins/DesertVillageRep/desertvillagerep.db`
- deleting the entire `plugins/DesertVillageRep/` folder
- installing a broken experimental build that changes the database format without a migration

For normal updates of this plugin, the intended workflow is simple jar replacement, not a fresh reinstall.

## First-Time Setup In Game

### Step 1: Place the Board

Build the physical wall or village board first.

Then stand where you want the floating text to appear and face the direction the board should face.

Run:

```text
/rep createboard
```

### Step 2: Define the Village Region

Look at one corner block and run:

```text
/rep pos1
```

Look at the opposite corner block and run:

```text
/rep pos2
```

Then save it:

```text
/rep setregion village
```

### Step 3: Define the Market Region

Repeat:

```text
/rep pos1
/rep pos2
/rep setregion market
```

### Step 4: Import Legacy Stats

If you want historical villager trades, building activity estimates, and hostile mob kills imported from existing player stat files:

```text
/rep import legacy all
```

## Commands

## Player Commands

### `/rep`

Shows your own current reputation totals.

It displays:

- your Builder score with live and legacy breakdown
- your Trader score with live and legacy breakdown
- your Protector score with live and legacy breakdown

### `/rep stats [player]`

Shows the reputation totals for yourself or another player.

Examples:

```text
/rep stats
/rep stats gigagoogi
```

### `/rep top <builder|trader|protector>`

Shows the top players in a specific category.

Examples:

```text
/rep top builder
/rep top trader
/rep top protector
```

### `/rep legacy [player]`

Shows the imported legacy stats snapshot for yourself or another player.

This includes data pulled from vanilla stats files such as:

- villager trades
- hostile kills
- deaths
- play time ticks
- walked distance in centimeters

Examples:

```text
/rep legacy
/rep legacy HAbbood
```

## Admin Commands

### `/rep pos1`

Saves the first selection point for a region.

Behavior:

- uses the block you are currently looking at if possible
- otherwise uses your current block position
- only works in-game

Use this before `/rep setregion ...`.

### `/rep pos2`

Saves the second selection point for a region.

Behavior is the same as `pos1`, but for the opposite corner.

### `/rep setregion <village|market>`

Converts the saved `pos1` and `pos2` into a real tracked cuboid region.

Examples:

```text
/rep setregion village
/rep setregion market
```

What each region means:

- `village`: used for builder scoring and villager-care actions
- `market`: used for builder scoring in a market or bazaar area

### `/rep createboard`

Creates the floating reputation board at your current location and facing direction.

Use this the first time you place the board.

### `/rep moveboard`

Moves the board to your current location and facing direction.

Use this if:

- you placed it in the wrong spot
- the floating text is too high or too low
- you want it aligned with a different wall or monument

This does not create a second board. It replaces the old floating board with a new one at the new saved location.

### `/rep removeboard`

Removes the floating board entirely and clears the saved board location.

Use this if:

- you want to start over from scratch
- you accidentally placed the board in a bad location
- you want to remove the floating text for a while without moving it somewhere else

After running this, use `/rep createboard` again whenever you want to place the board back down.

### `/rep addproject builder <id> <points>`

Creates a one-time Builder milestone project that can later be awarded to a player.

Examples:

```text
/rep addproject builder fountain 50
/rep addproject builder market 100
/rep addproject builder walls 75
```

The `id` should be a short unique name.

### `/rep completeproject <id> <player>`

Awards a saved builder project to a player once.

Example:

```text
/rep completeproject fountain gigagoogi
```

If that player has already received that project once, it will not award it again.

### `/rep adjust <player> <category> <amount> [reason]`

Manually adds or removes reputation from a player.

Examples:

```text
/rep adjust HAbbood trader 25 cured villagers
/rep adjust sockmonkeybob builder -10 accidental test score
/rep adjust gigagoogi protector 40 raid defense
```

Notes:

- positive numbers add rep
- negative numbers remove rep
- the optional reason is stored in rep history
- scores do not go below zero

### `/rep import legacy <all|player>`

Imports legacy player stats from vanilla world stat files.

Examples:

```text
/rep import legacy all
/rep import legacy gigagoogi
```

What it does:

- reads player stats files from the world stats folder
- saves a snapshot into the plugin database
- seeds Builder rep using estimated historical block-placement activity from vanilla `minecraft:used` block-item stats
- seeds Trader rep using historical villager trade counts
- seeds Protector rep using imported hostile mob kill stats

### `/rep refresh`

Forces the board to redraw immediately.

Useful if:

- you just changed something and want to see it instantly
- the board looks out of date
- you are testing configuration or setup

## Example Setup Order

1. Build the plugin jar
2. Upload the final jar to Apex
3. Restart the server
4. Build the physical board
5. Run `/rep createboard`
6. Define `village`
7. Define `market`
8. Run `/rep import legacy all`
9. Add any builder projects you want

## Git And GitHub

This project is already set up as a Git repository with `origin` pointing to:

`https://github.com/902Habb/desertvillagerep.git`

Normal update flow:

```bash
./scripts/build-local.sh
git status
git add .
git commit -m "Describe the change"
git push
```
