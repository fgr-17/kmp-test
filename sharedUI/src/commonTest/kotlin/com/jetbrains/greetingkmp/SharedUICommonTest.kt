package com.jetbrains.greetingkmp

import com.jetbrains.greetingkmp.notes.NotesUiTags
import kotlin.test.Test
import kotlin.test.assertTrue

class SharedUICommonTest {
    @Test
    fun notesUiTags_areStableForE2E() {
        assertTrue(NotesUiTags.ADD_NOTE.isNotBlank())
        assertTrue(NotesUiTags.SYNC.isNotBlank())
        assertTrue(NotesUiTags.NOTE_INPUT.isNotBlank())
    }
}
