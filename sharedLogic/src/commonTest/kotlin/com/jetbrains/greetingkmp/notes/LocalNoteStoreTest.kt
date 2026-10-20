package com.jetbrains.greetingkmp.notes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalNoteStoreTest {
    @Test
    fun add_trimsText_andMarksPending() {
        val store = LocalNoteStore()
        val note = store.add("  Checked barrel B12  ", "2026-01-01T00:00:00Z")

        assertEquals("Checked barrel B12", note.text)
        assertEquals(SyncStatus.PENDING, note.syncStatus)
        assertEquals(1, store.notes.value.size)
        assertEquals(listOf(note), store.pending())
    }

    @Test
    fun markSynced_updatesOnlyMatchingIds() {
        val store = LocalNoteStore()
        val first = store.add("one", "t1")
        val second = store.add("two", "t2")

        store.markSynced(listOf(first.id))

        assertEquals(
            SyncStatus.SYNCED,
            store.notes.value
                .first { it.id == first.id }
                .syncStatus,
        )
        assertEquals(
            SyncStatus.PENDING,
            store.notes.value
                .first { it.id == second.id }
                .syncStatus,
        )
        assertEquals(1, store.pending().size)
    }

    @Test
    fun clear_dropsEveryNote() {
        val store = LocalNoteStore()
        store.add("one", "t")
        store.clear()
        assertTrue(store.notes.value.isEmpty())
        assertTrue(store.pending().isEmpty())
    }

    @Test
    fun markSynced_emptyIds_isNoOp() {
        val store = LocalNoteStore()
        store.add("keep", "t")
        store.markSynced(emptyList())
        assertTrue(store.pending().isNotEmpty())
    }
}
