package com.jetbrains.greetingkmp.notes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Device-side "local backend" for the offline demo.
 * In-memory on purpose — swap for SQLDelight in a real product.
 */
class LocalNoteStore {
    private val _notes = MutableStateFlow<List<FieldNote>>(emptyList())
    val notes: StateFlow<List<FieldNote>> = _notes.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    fun add(text: String, createdAtIso: String): FieldNote {
        val note = FieldNote(
            id = Uuid.random().toString(),
            text = text.trim(),
            createdAt = createdAtIso,
            syncStatus = SyncStatus.PENDING,
        )
        _notes.update { it + note }
        return note
    }

    fun pending(): List<FieldNote> =
        _notes.value.filter { it.syncStatus == SyncStatus.PENDING }

    fun markSynced(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        _notes.update { list ->
            list.map { note ->
                if (note.id in idSet) note.copy(syncStatus = SyncStatus.SYNCED) else note
            }
        }
    }
}
