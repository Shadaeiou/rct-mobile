# CLAUDE.md — RCT Mobile

Project-specific guidance for Claude (and any other agent) working in this
repo. This is the operating manual; read it before changing anything.

## Branching: push to `main`, always

**No feature branches. No PRs. No staging. `main` is the only branch that
ships.**

The release pipeline is wired so that **`git push origin main` is the only
way to ship**:

1. Push to `main` → `.github/workflows/build-android.yml` runs.
2. The workflow signs `app-release.apk` with the release keystore and
   publishes a GitHub Release tagged `v0.1.<N>+<N>` (where `N` =
   `git rev-list --count HEAD`).
3. Every installed phone polls Releases (and/or receives an FCM push on
   topic `app-updates`) and auto-installs the new APK over the previous
   one.

**If you push to a side branch:**

- The signed APK is **not** built (the keystore-decode step gates on
  `github.ref == 'refs/heads/main'`).
- No Release is published.
- No FCM push goes out.
- Installed phones get nothing.
- The user's "deploy" is a no-op.

So when you're done: commit straight to `main` and `git push -u origin
main`. Don't `git checkout -b`, don't `gh pr create`, don't open a draft.
Local debug builds (`cd android && ./gradlew :app:installDebug`) are the
staging environment; `main` is production.

The version is `0.1.$gitCommits`, so **every commit you push becomes a
numbered release.** Squash WIP commits locally before pushing if you don't
want the version counter to leap.

The narrow exception: pushing a manual tag (`git tag v0.x.y+<code> &&
git push --tags`) also runs the workflow. Don't push branches other than
`main`.

## Never destroy a player's park

Updates install **in place**: every existing player's park save survives
the upgrade. Treat the on-device save as production data you do not own.
**You will not get a second chance to recover from a release that wipes
saves.**

The save lives at `context.filesDir/park.json` and is loaded by
[`data/ParkRepository.kt`](android/app/src/main/java/com/shadaeiou/rctmobile/data/ParkRepository.kt).
Its top-level shape is:

```json
{ "schemaVersion": N, "park": { ...ParkState... } }
```

### Hard rules

1. **Bump `CURRENT_SCHEMA_VERSION` and write a migration** any time you
   change the shape of `ParkState` or any nested model. The migration
   chain lives in
   [`data/Migrations.kt`](android/app/src/main/java/com/shadaeiou/rctmobile/data/Migrations.kt).
   Add a function `migrateNToNPlus1(JsonObject) -> JsonObject` that walks
   the raw JSON, and register it in the chain. Decoding into the typed
   `SaveFile` happens **after** all migrations have run — so the
   migration only deals with `JsonObject`/`JsonElement`, never the Kotlin
   data class.
2. **Do not delete the save on update.** That includes "just resetting"
   if parsing fails, "first launch after upgrade" hooks, or rotating to a
   different filename. If parsing genuinely fails (bad migration, file
   corruption), surface it to the player via a recovery dialog — never
   silently nuke their park.
3. **Migrations must chain forward from any prior version, not just the
   immediately-previous one.** A player updating from `0.1.5` to `0.1.50`
   must apply every migration in between.
4. **Test the upgrade path, not just a clean install.** Install the
   previous release, build a park, then install the new APK over it. A
   "fresh install works" claim is not the same as "upgrade preserves
   progress."
5. **Don't change the release keystore.** Different signing key = Android
   refuses the install entirely; the user has to uninstall, which wipes
   the save. The keystore is `release.jks`, base64-encoded into the
   `KEYSTORE_BASE64` GitHub secret. Lose it and there is no recovery.
6. **Don't rename the application ID.** It's `com.shadaeiou.rctmobile`.
   Renaming it ships a new app to Android; existing installs sit on the
   old version forever. Same effect for the user as losing the keystore.
7. **`fallbackToDestructiveMigration()` is banned.** If we ever migrate
   to Room or another DB, write real migrations. The flag wipes the user
   on schema mismatch.

### Anti-patterns that look fine but aren't

- **Removing a field from `ParkState` because nothing reads it.**
  kotlinx.serialization with `ignoreUnknownKeys = true` makes this
  *appear* safe — old saves still parse — but if you later re-add a
  field with the same name and a different meaning, every existing
  install reads stale data. Either keep the field, or migrate it out
  explicitly.
- **Catching `SerializationException` and writing a fresh save.** That's
  one bad release silently nuking everyone's progress. If parsing
  fails, log it, leave the save on disk, and ask the player whether to
  start a new park or send the broken save somewhere debuggable.
- **Tying a "reset park" code path to anything other than an explicit
  user tap on a confirm dialog.** No build-time flag, no debug-only path
  that survives a misconfig. The Settings screen "Reset park" button is
  the only legitimate path.

### How to ship a save change safely

1. Edit `ParkState` (or whatever nested model).
2. Bump `CURRENT_SCHEMA_VERSION` in
   [`SaveFile.kt`](android/app/src/main/java/com/shadaeiou/rctmobile/data/SaveFile.kt).
3. Add `migrate<old>To<new>(JsonObject): JsonObject` in
   [`Migrations.kt`](android/app/src/main/java/com/shadaeiou/rctmobile/data/Migrations.kt)
   and register it in the `migrations` map.
4. Add an entry to `CHANGELOG` in
   [`Changelog.kt`](android/app/src/main/java/com/shadaeiou/rctmobile/data/Changelog.kt)
   noting the migration so players see something happened.
5. Run the upgrade test by hand: install the previous APK, play, install
   the new APK over it, confirm money/day/grid all preserved.
6. Push to `main`.

## Versioning is automatic, don't fight it

`versionCode = git rev-list --count HEAD` and
`versionName = "0.1.$gitCommits"` in `android/app/build.gradle.kts`.
Don't hard-code either, don't squash-merge after pushing (that orphans
the version code), and don't add a flavor that overrides `versionCode`.

## Local development

```bash
cd android
./gradlew :app:installDebug    # debug-signed; safe to iterate
```

Debug and release share the same `applicationId`, so a debug install
collides with a release install on the same device. Either uninstall the
release first or live with replacing your "real" save with the debug one.
If you want both side by side, add `applicationIdSuffix = ".debug"` to
the debug build type — but make sure the release build still uses the
canonical id.

## When the build breaks on `main`

**Don't push a branch to "test the fix."** Fix it locally, run
`./gradlew :app:assembleRelease` with the release env vars set, and push
the fix to `main`. The release tag for the broken commit will be
superseded by the next push's tag, and clients will pick up the fix on
their next update poll.

If the workflow succeeded but clients aren't updating, check:

1. Was a Release actually published? (Look at the Releases tab.)
2. Is the tag in the form `v<name>+<code>`? The Updater parses that
   shape (see `Updater.kt#parseTag`).
3. Is the APK attached to the release as `app-release.apk`?

## Game design notes (for future Claude)

- The game state is one immutable `ParkState` snapshot. The simulation
  produces a new snapshot every tick (one in-game hour). Don't mutate
  state in place; return new instances and let Compose recompose.
- `Simulation.tick(state)` is pure given the state. Keep it that way so
  it stays testable and so the autosave is always coherent.
- Tile definitions live in `TileCatalog.kt`. To add a ride or shop,
  add a `TileType` enum entry and a `TileDef` row. Make sure the
  migration story holds: an old save that doesn't know about the new
  tile is fine (it won't appear), but renaming an existing
  `TileType` requires a save migration — the enum name is what's
  serialized.
- Park rating, money flow, and guest counts are all derived per tick;
  they're not stored as deltas. Store the *state*, not the *log*.
