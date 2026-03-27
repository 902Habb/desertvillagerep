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

Builder reputation rewards players for placing approved building and decorative blocks inside the configured `village` or `market` regions.

- Most normal structural blocks are worth `+1`
- Premium decorative blocks are worth `+2`
- Only block placement is counted
- Breaking blocks does not give points
- Obvious same-player place-break-replace loops at the exact same coordinates within the configured anti-loop window are ignored

Builder reputation can also be awarded manually through admin-defined one-time builder projects such as:

- `fountain`
- `market`
- `gate`
- `watchtower`
- `walls`

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

Protector reputation rewards players for defending a specific configured area.

- Only hostile mob kills inside the `protector` region count
- Only player-caused kills count
- Only approved natural-like spawn reasons count
- Mob values vary by threat level

Examples:

- common mobs like zombies and skeletons are worth `+1`
- stronger threats like creepers, witches, pillagers, and phantoms are worth `+2`
- high-threat mobs like ravagers and evokers are worth `+3`

## Board Behavior

The board is not a GUI and it is not a client mod. It is a physical in-world floating text display.

- You build the physical wall or monument yourself
- The plugin places floating `TextDisplay` text in front of it
- The board shows the top four players in each category
- The footer shows the current category title holders
- The board refreshes automatically and can also be force-refreshed by command

The board position is saved in the plugin database. If you place it incorrectly, you can move it with `/rep admin moveboard` and the plugin will remove the old floating board and recreate it at the new location. If you want to fully clear the board and start over later, you can use `/rep admin removeboard`.

## Regions

The plugin uses three admin-defined cuboid regions:

- `village`
- `market`
- `protector`

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

## First-Time Setup In Game

### Step 1: Place the Board

Build the physical wall or village board first.

Then stand where you want the floating text to appear and face the direction the board should face.

Run:

```text
/rep admin createboard
```

### Step 2: Define the Village Region

Look at one corner block and run:

```text
/rep admin pos1
```

Look at the opposite corner block and run:

```text
/rep admin pos2
```

Then save it:

```text
/rep admin setregion village
```

### Step 3: Define the Market Region

Repeat:

```text
/rep admin pos1
/rep admin pos2
/rep admin setregion market
```

### Step 4: Define the Protector Region

Repeat:

```text
/rep admin pos1
/rep admin pos2
/rep admin setregion protector
```

### Step 5: Import Legacy Trader Stats

If you want historical villager trades imported from existing player stat files:

```text
/rep admin import legacy all
```

## Commands

## Player Commands

### `/rep`

Shows your own current reputation totals.

It displays:

- your Builder score
- your Trader score
- the Trader breakdown between live score and imported legacy seed
- your Protector score

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

### `/rep admin pos1`

Saves the first selection point for a region.

Behavior:

- uses the block you are currently looking at if possible
- otherwise uses your current block position
- only works in-game

Use this before `/rep admin setregion ...`.

### `/rep admin pos2`

Saves the second selection point for a region.

Behavior is the same as `pos1`, but for the opposite corner.

### `/rep admin setregion <village|market|protector>`

Converts the saved `pos1` and `pos2` into a real tracked cuboid region.

Examples:

```text
/rep admin setregion village
/rep admin setregion market
/rep admin setregion protector
```

What each region means:

- `village`: used for builder scoring and villager-care actions
- `market`: used for builder scoring in a market or bazaar area
- `protector`: used for hostile mob defense scoring

### `/rep admin createboard`

Creates the floating reputation board at your current location and facing direction.

Use this the first time you place the board.

### `/rep admin moveboard`

Moves the board to your current location and facing direction.

Use this if:

- you placed it in the wrong spot
- the floating text is too high or too low
- you want it aligned with a different wall or monument

This does not create a second board. It replaces the old floating board with a new one at the new saved location.

### `/rep admin removeboard`

Removes the floating board entirely and clears the saved board location.

Use this if:

- you want to start over from scratch
- you accidentally placed the board in a bad location
- you want to remove the floating text for a while without moving it somewhere else

After running this, use `/rep admin createboard` again whenever you want to place the board back down.

### `/rep admin addproject builder <id> <points>`

Creates a one-time Builder milestone project that can later be awarded to a player.

Examples:

```text
/rep admin addproject builder fountain 50
/rep admin addproject builder market 100
/rep admin addproject builder walls 75
```

The `id` should be a short unique name.

### `/rep admin completeproject <id> <player>`

Awards a saved builder project to a player once.

Example:

```text
/rep admin completeproject fountain gigagoogi
```

If that player has already received that project once, it will not award it again.

### `/rep admin adjust <player> <category> <amount> [reason]`

Manually adds or removes reputation from a player.

Examples:

```text
/rep admin adjust HAbbood trader 25 cured villagers
/rep admin adjust sockmonkeybob builder -10 accidental test score
/rep admin adjust gigagoogi protector 40 raid defense
```

Notes:

- positive numbers add rep
- negative numbers remove rep
- the optional reason is stored in rep history
- scores do not go below zero

### `/rep admin import legacy <all|player>`

Imports legacy player stats from vanilla world stat files.

Examples:

```text
/rep admin import legacy all
/rep admin import legacy gigagoogi
```

What it does:

- reads player stats files from the world stats folder
- saves a snapshot into the plugin database
- seeds Trader rep using historical villager trade counts

What it does not do:

- it does not reconstruct historical Builder rep
- it does not reconstruct historical Protector rep by region

### `/rep admin refresh`

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
5. Run `/rep admin createboard`
6. Define `village`
7. Define `market`
8. Define `protector`
9. Run `/rep admin import legacy all`
10. Add any builder projects you want

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
