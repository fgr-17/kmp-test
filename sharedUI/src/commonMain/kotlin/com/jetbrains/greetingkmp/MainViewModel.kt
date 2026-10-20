package com.jetbrains.greetingkmp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.greetingkmp.notes.CloudConfig
import com.jetbrains.greetingkmp.notes.FieldNote
import com.jetbrains.greetingkmp.notes.NotesFacade
import com.jetbrains.greetingkmp.notes.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val notesFacade = NotesFacade(CloudConfig.ANDROID_EMULATOR_BASE_URL)

    val greetingList: StateFlow<List<String>>
        field = MutableStateFlow<List<String>>(listOf())

    val notes: StateFlow<List<FieldNote>> = notesFacade.notes

    private val _isOnline = MutableStateFlow(notesFacade.isOnline)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _status = MutableStateFlow("Offline by default — add notes, then sync.")
    val status: StateFlow<String> = _status.asStateFlow()

    init {
        viewModelScope.launch {
            Greeting().greet().collect { phrase ->
                greetingList.update { list -> list + phrase }
            }
        }
    }

    fun setOnline(online: Boolean) {
        notesFacade.isOnline = online
        _isOnline.value = online
        _status.value =
            if (online) {
                "Online — Sync will reach the Go backend."
            } else {
                "Offline — notes stay on device."
            }
    }

    fun addNote(text: String) {
        if (text.isBlank()) return
        notesFacade.addNote(text)
        _status.value = "Saved locally."
    }

    fun sync() {
        _status.value = "Syncing..."
        viewModelScope.launch {
            _status.value =
                when (val result = notesFacade.sync()) {
                    SyncResult.Offline -> "Still offline — turn Online on first."
                    SyncResult.NothingToSync -> "Nothing pending."
                    is SyncResult.Synced -> "Synced ${result.count} note(s) to cloud."
                    is SyncResult.Failed -> "Sync failed: ${result.message}"
                }
        }
    }
}
