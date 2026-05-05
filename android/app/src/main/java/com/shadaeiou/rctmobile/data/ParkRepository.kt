package com.shadaeiou.rctmobile.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File

/**
 * Loads and saves the park to `filesDir/park.json`. Treat the file on
 * disk as production data: never silently delete it. The [reset] entry
 * point is fine because the only caller is the user tapping "Reset
 * park" through a confirm dialog.
 *
 * Writes are roughly atomic: encode -> write to `.tmp` -> snapshot the
 * previous file as `.bak` -> rename `.tmp` over the live file. A power
 * loss mid-write leaves the previous good save intact, plus the `.bak`
 * if needed.
 */
class ParkRepository(context: Context) {

    private val saveFile = File(context.filesDir, "park.json")
    private val backupFile = File(context.filesDir, "park.json.bak")
    private val tmpFile = File(context.filesDir, "park.json.tmp")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun load(): LoadResult = withContext(Dispatchers.IO) {
        if (!saveFile.exists()) return@withContext LoadResult.Fresh

        decodeFile(saveFile)?.let { return@withContext LoadResult.Loaded(it) }

        // Primary failed; fall back to last good backup before giving up.
        if (backupFile.exists()) {
            decodeFile(backupFile)?.let { return@withContext LoadResult.Loaded(it) }
        }

        // Both unreadable. Don't delete - a future build's migration may
        // be able to recover. Surface to the user instead.
        LoadResult.Corrupted("Save could not be decoded. Your park is still on disk.")
    }

    private fun decodeFile(file: File): SaveFile? {
        val raw = runCatching { file.readText() }.getOrElse {
            Log.w(TAG, "could not read $file", it); return null
        }
        return runCatching {
            val obj = json.parseToJsonElement(raw) as? JsonObject
                ?: error("save root is not a JSON object")
            val migrated = migrate(obj)
            json.decodeFromJsonElement(SaveFile.serializer(), migrated)
        }.getOrElse {
            Log.w(TAG, "decode failed for $file", it); null
        }
    }

    suspend fun save(state: ParkState) = withContext(Dispatchers.IO) {
        val payload = SaveFile(schemaVersion = CURRENT_SCHEMA_VERSION, park = state)
        val text = json.encodeToString(SaveFile.serializer(), payload)
        runCatching {
            tmpFile.writeText(text)
            if (saveFile.exists()) {
                saveFile.copyTo(backupFile, overwrite = true)
            }
            if (!tmpFile.renameTo(saveFile)) {
                // Rename can fail across some filesystems; fall back to copy.
                tmpFile.copyTo(saveFile, overwrite = true)
                tmpFile.delete()
            }
        }.onFailure {
            Log.w(TAG, "save failed", it)
            throw it
        }
    }

    /**
     * Wipe the save. Only call this from a user-confirmed "Reset park"
     * action — never from update flows, parse-failure handlers, or
     * first-launch hooks.
     */
    suspend fun reset() = withContext(Dispatchers.IO) {
        saveFile.delete()
        backupFile.delete()
        tmpFile.delete()
    }

    companion object {
        private const val TAG = "ParkRepository"
    }
}

sealed class LoadResult {
    data object Fresh : LoadResult()
    data class Loaded(val save: SaveFile) : LoadResult()
    data class Corrupted(val reason: String) : LoadResult()
}
