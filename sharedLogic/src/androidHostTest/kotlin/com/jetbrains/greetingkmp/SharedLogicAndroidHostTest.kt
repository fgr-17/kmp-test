package com.jetbrains.greetingkmp

import com.jetbrains.greetingkmp.notes.LocalNoteStore
import com.jetbrains.greetingkmp.notes.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicAndroidHostTest {
    @Test
    fun localStore_worksOnAndroidHost() {
        val store = LocalNoteStore()
        val note = store.add("android host", "t")
        assertEquals(SyncStatus.PENDING, note.syncStatus)
    }
}
