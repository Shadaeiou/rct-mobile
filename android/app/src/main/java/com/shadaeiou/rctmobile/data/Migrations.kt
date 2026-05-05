package com.shadaeiou.rctmobile.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Migration chain for [SaveFile]. Each entry takes a raw [JsonObject] at
 * version `key` and returns a [JsonObject] at version `key + 1`.
 *
 * Invariants:
 *  - Migrations operate on raw JSON, never on the typed [SaveFile]. The
 *    typed model evolves; the JSON shape on disk lags behind.
 *  - Every migration must produce a JSON shape that the *current* code
 *    can decode. If a future migration would invalidate this one, that's
 *    a sign you need to chain another migration after it, not edit this
 *    one.
 *  - Never delete the save when migration fails. [ParkRepository] handles
 *    that case by surfacing the error to the user.
 *
 * The current schema is 1; this map is empty. When [CURRENT_SCHEMA_VERSION]
 * is bumped to 2, register a `1 to ::migrate1To2` here.
 */
private val migrations: Map<Int, (JsonObject) -> JsonObject> = emptyMap()

/**
 * Reads `schemaVersion` from [initial] and applies migrations until the
 * version matches [CURRENT_SCHEMA_VERSION].
 *
 * Throws if a needed migration is not registered. The caller (the
 * repository) treats that as "save corrupt or from a newer version" and
 * surfaces it to the player rather than wiping anything.
 */
fun migrate(initial: JsonObject): JsonObject {
    var current = initial
    var version = (current["schemaVersion"] as? JsonPrimitive)?.intOrNull
        ?: 1   // saves predating the schemaVersion field are treated as v1

    if (version > CURRENT_SCHEMA_VERSION) {
        error(
            "Save is from a newer build (schemaVersion=$version, app supports up to " +
                "$CURRENT_SCHEMA_VERSION). Refusing to downgrade — install a newer APK."
        )
    }

    while (version < CURRENT_SCHEMA_VERSION) {
        val step = migrations[version]
            ?: error("No migration registered for schemaVersion $version -> ${version + 1}")
        current = step(current)
        version += 1
    }
    return current
}
