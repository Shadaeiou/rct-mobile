package com.shadaeiou.rctmobile.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shadaeiou.rctmobile.data.LoadResult
import com.shadaeiou.rctmobile.data.ParkRepository
import com.shadaeiou.rctmobile.data.ParkState
import com.shadaeiou.rctmobile.data.ThemePreference
import com.shadaeiou.rctmobile.data.UserPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TICK_INTERVAL_MS = 1500L
const val TICKS_PER_AUTOSAVE = 8

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ParkRepository(app)
    private val userPreferences = UserPreferences(app)

    private val _ui = MutableStateFlow<UiState>(UiState.Loading)
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _selectedBuild = MutableStateFlow<TileType?>(null)
    val selectedBuild: StateFlow<TileType?> = _selectedBuild.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _themePreference = MutableStateFlow(userPreferences.theme)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private var tickJob: Job? = null
    private var ticksSinceSave = 0

    init {
        viewModelScope.launch {
            when (val r = repo.load()) {
                LoadResult.Fresh -> {
                    val fresh = ParkState()
                    _ui.value = UiState.Playing(fresh)
                    saveNow(fresh)
                }
                is LoadResult.Loaded -> _ui.value = UiState.Playing(r.save.park)
                is LoadResult.Corrupted -> _ui.value = UiState.Corrupted(r.reason)
            }
            startTickLoop()
        }
    }

    fun selectBuild(t: TileType?) {
        _selectedBuild.value = t
    }

    fun tap(x: Int, y: Int) {
        val current = (_ui.value as? UiState.Playing)?.state ?: return
        val build = _selectedBuild.value ?: return
        val target = current.tileAt(x, y)
        if (target == TileType.ENTRANCE) return
        if (target == build) return
        val def = TileCatalog.def(build)
        if (current.moneyCents < def.costCents) return
        val placed = current.withTile(x, y, build)
            .copy(moneyCents = current.moneyCents - def.costCents)
        _ui.value = UiState.Playing(placed)
        saveNow(placed)
    }

    fun longPress(x: Int, y: Int) {
        val current = (_ui.value as? UiState.Playing)?.state ?: return
        val existing = current.tileAt(x, y)
        if (existing == TileType.EMPTY || existing == TileType.ENTRANCE) return
        val refund = TileCatalog.def(existing).costCents / 2
        val cleared = current.withTile(x, y, TileType.EMPTY)
            .copy(moneyCents = current.moneyCents + refund)
        _ui.value = UiState.Playing(cleared)
        saveNow(cleared)
    }

    fun togglePause() {
        val current = (_ui.value as? UiState.Playing)?.state ?: return
        val next = current.copy(paused = !current.paused)
        _ui.value = UiState.Playing(next)
        saveNow(next)
    }

    fun setThemePreference(pref: ThemePreference) {
        userPreferences.theme = pref
        _themePreference.value = pref
    }

    /**
     * Player-confirmed reset. The Settings screen wraps this in a
     * confirmation dialog — no other call site is allowed (see CLAUDE.md).
     */
    fun resetPark() {
        viewModelScope.launch {
            withContext(NonCancellable) { repo.reset() }
            val fresh = ParkState()
            _ui.value = UiState.Playing(fresh)
            saveNow(fresh)
        }
    }

    /**
     * Only callable from the Corrupted-save recovery dialog. Same idea
     * as [resetPark]: requires explicit user intent.
     */
    fun discardCorruptedAndStartFresh() {
        viewModelScope.launch {
            withContext(NonCancellable) { repo.reset() }
            val fresh = ParkState()
            _ui.value = UiState.Playing(fresh)
            saveNow(fresh)
            startTickLoop()
        }
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(TICK_INTERVAL_MS)
                val current = (_ui.value as? UiState.Playing)?.state ?: continue
                val next = Simulation.tick(current)
                if (next !== current) _ui.value = UiState.Playing(next)
                ticksSinceSave += 1
                if (ticksSinceSave >= TICKS_PER_AUTOSAVE) {
                    ticksSinceSave = 0
                    saveNow(next)
                }
            }
        }
    }

    private fun saveNow(state: ParkState) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Saving
            runCatching { withContext(NonCancellable) { repo.save(state) } }
                .onSuccess { _saveStatus.value = SaveStatus.Idle }
                .onFailure {
                    Log.w("GameViewModel", "save failed", it)
                    _saveStatus.value = SaveStatus.Failed
                }
        }
    }
}

sealed class UiState {
    data object Loading : UiState()
    data class Playing(val state: ParkState) : UiState()
    data class Corrupted(val reason: String) : UiState()
}

enum class SaveStatus { Idle, Saving, Failed }
