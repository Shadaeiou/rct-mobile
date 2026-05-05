# RCT Mobile

A bite-sized RollerCoaster Tycoon-style park builder for Android. Tap-to-build
on a grid, simulate guests, watch the money roll in, expand the park.

Sideload-only — built on the [android-template][template] release pipeline:
push to `main`, the workflow signs an APK, your phone auto-updates.

[template]: https://github.com/Shadaeiou/android-template

## What's in here

- **The game.** A grid-based park sim in Jetpack Compose. Place rides,
  paths, shops, and scenery; each tick a simulation runs guest spawning,
  ride revenue, maintenance costs, and a park rating recompute.
- **Persistent save.** Park state lives at `filesDir/park.json` with a
  `schemaVersion` field and a forward-only migration chain so updates
  never wipe a player's progress.
- **In-app updater.** Pulls the latest signed APK from GitHub Releases
  and installs it; FCM push (optional) prompts every device the moment
  a new release ships.
- **Auto-incrementing version.** `versionCode = git rev-list --count
  HEAD`, `versionName = 0.1.$gitCommits`.

## Gameplay

Start with $20,000 and an empty 10×10 lot with one entrance tile. Goal:
build a park guests will pay to visit.

- **Build palette** (bottom of screen) — pick a tile type, then tap a
  grid cell to place it. Costs deduct from cash. Long-press a placed
  tile to demolish (50% refund).
- **Time advances** at 1.5s per in-game hour, 24 hours per day. Pause
  with the play/pause control on the HUD.
- **Park rating** (0–1000) drives guest demand: more rides, more
  variety, more scenery, more rating. Forget restrooms or food and
  rating tanks.
- **Rides earn money** when guests can reach them via path tiles.
  Stranded rides earn nothing.
- **Maintenance** is paid per tick, per ride, regardless of guests —
  go bankrupt and the park stops earning until you demolish.

## Tile types

| Kind | Tile | What it does |
|---|---|---|
| Entrance | one fixed tile per park | Where guests enter |
| Path | Path | Connects rides/shops to the entrance |
| Rides | Carousel, Bumper Cars, Ferris Wheel, Drop Tower, Coaster | Earn fees per guest, scaled by excitement |
| Shops | Food Stall, Drink Stall, Souvenir Shop | Steady per-guest revenue, raise rating slightly |
| Facilities | Restroom | Required at scale; missing them tanks rating |
| Scenery | Tree, Fountain, Bench | Boost rating, no upkeep |

## Architecture

```
android/app/src/main/java/com/shadaeiou/rctmobile/
├── MainActivity.kt          — single Activity, hosts Compose
├── RctApp.kt                — Application; FCM channel + topic subscribe
├── data/
│   ├── SaveFile.kt          — schemaVersion + ParkState + serialization
│   ├── ParkRepository.kt    — load/save filesDir/park.json (with migrations)
│   ├── Migrations.kt        — JsonObject -> JsonObject migration chain
│   ├── Updater.kt           — GitHub Releases poll + DownloadManager
│   └── Changelog.kt         — single source of truth for "What's new"
├── game/
│   ├── TileCatalog.kt       — TileType enum + per-tile cost/excitement/etc
│   ├── Simulation.kt        — pure tick() function: ParkState -> ParkState
│   └── GameViewModel.kt     — drives the tick loop, autosaves
├── service/
│   └── PushService.kt       — FCM receiver: surfaces "Update available"
└── ui/
    ├── Theme.kt
    ├── GameScreen.kt        — top-level: HUD + canvas + palette
    ├── HudBar.kt            — money / day / rating / pause
    ├── ParkCanvas.kt        — Canvas-based grid renderer + tap routing
    ├── BuildPalette.kt      — bottom build sheet
    └── SettingsScreen.kt    — version, changelog, update, reset
```

## How a release flows

```
git commit && git push origin main
    │
    ▼
GitHub Actions: build-android.yml
    │  decode keystore → gradle assembleRelease → upload artifact
    ▼
GitHub Release tag v0.1.<N>+<N>, app-release.apk attached
    │
    ▼
scripts/send_fcm_update_push.py  (if FCM_SERVICE_ACCOUNT_JSON is set)
    │  POST data-only message to topic "app-updates"
    ▼
Every installed device pulls the new APK and installs it in place
    │
    ▼
Player opens the app → save migrates forward → park resumes
```

## Setup (after forking)

The release pipeline needs four GitHub Actions secrets:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 release.jks` (entire string) |
| `KEYSTORE_PASSWORD` | release keystore store password |
| `KEY_ALIAS` | release key alias |
| `KEY_PASSWORD` | release key password |

Optional, for auto-prompt push:

| Secret | Value |
|---|---|
| `FCM_SERVICE_ACCOUNT_JSON` | full service account JSON from Firebase |

Generate the keystore once (`keytool -genkey -v -keystore release.jks
-keyalg RSA -keysize 2048 -validity 10000 -alias release`) and back it
up — losing it means no future build can update an existing install.

Once secrets are set, push to `main`. The workflow signs and publishes
the first release. Sideload that APK manually onto your phone (the
first install can't come through the in-app updater); every subsequent
install can.

## Local development

```bash
cd android
./gradlew :app:installDebug    # debug-signed, no keystore needed
```

Debug shares the application ID with release, so it'll replace any
release install on the same device.

> **First-time clone:** if `android/gradle/wrapper/gradle-wrapper.jar`
> is missing (it's a binary the seed commit couldn't include over the
> tooling Claude was using), regenerate it once:
>
> ```bash
> cd android
> gradle wrapper --gradle-version 8.10.2
> ```
>
> CI doesn't need the jar — `.github/workflows/build-android.yml`
> uses the gradle action's bundled `gradle` binary directly, not
> `./gradlew`.

## Read this before changing anything

[`CLAUDE.md`](CLAUDE.md). Especially the parts about not deleting the
player's save and pushing only to `main`.

## Stack

- Kotlin 2.0.21, Jetpack Compose (BOM 2024.10.01), Material 3
- AGP 8.7.3, Gradle 8.10.2, JDK 17
- compileSdk 35, minSdk 26, targetSdk 35
- OkHttp 4.12, kotlinx.serialization 1.7.3, kotlinx.coroutines 1.8.1
- Firebase BoM 33.7.0 (`firebase-messaging-ktx`)
