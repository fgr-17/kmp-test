package com.jetbrains.greetingkmp

import com.jetbrains.greetingkmp.notes.LocalNoteStore
import com.jetbrains.greetingkmp.notes.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicIOSTest {
    @Test
    fun localStore_worksOnIos() {
        val store = LocalNoteStore()
        val note = store.add("ios", "t")
        assertEquals(SyncStatus.PENDING, note.syncStatus)
    }
}
