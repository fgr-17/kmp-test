package com.jetbrains.greetingkmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jetbrains.greetingkmp.notes.CloudConfig
import com.jetbrains.greetingkmp.notes.NotesFacade
import com.jetbrains.greetingkmp.notes.NotesUiTags
import com.jetbrains.greetingkmp.notes.SyncResult
import kotlinx.coroutines.launch

@Composable
fun WebApp(notesFacade: NotesFacade = NotesFacade(CloudConfig.WEB_BASE_URL)) {
    val greetings = remember { mutableStateListOf<String>() }
    val notes by notesFacade.notes.collectAsState()
    var draft by remember { mutableStateOf("") }
    var online by remember { mutableStateOf(notesFacade.isOnline) }
    var status by remember { mutableStateOf("Offline by default — add notes, then sync.") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Greeting().greet().collect { phrase ->
            greetings.add(phrase)
        }
    }

    MaterialTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Greetings", style = MaterialTheme.typography.titleMedium)
            greetings.forEach { greeting ->
                Text(greeting)
                HorizontalDivider()
            }

            Text("Field notes (offline → cloud)", style = MaterialTheme.typography.titleMedium)
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag(NotesUiTags.STATUS),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (online) "Online" else "Offline")
                Switch(
                    checked = online,
                    onCheckedChange = {
                        online = it
                        notesFacade.isOnline = it
                        status =
                            if (it) {
                                "Online — Sync will reach the Go backend."
                            } else {
                                "Offline — notes stay on device."
                            }
                    },
                    modifier = Modifier.testTag(NotesUiTags.ONLINE_SWITCH),
                )
            }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(NotesUiTags.NOTE_INPUT),
                label = { Text("Note (e.g. Checked barrel B12)") },
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (draft.isBlank()) return@Button
                        notesFacade.addNote(draft)
                        draft = ""
                        status = "Saved locally (${if (online) "online but not synced yet" else "offline"})."
                    },
                    modifier = Modifier.testTag(NotesUiTags.ADD_NOTE),
                ) {
                    Text("Add note")
                }
                Button(
                    onClick = {
                        scope.launch {
                            status =
                                when (val result = notesFacade.sync()) {
                                    SyncResult.Offline -> "Still offline — turn Online on first."
                                    SyncResult.NothingToSync -> "Nothing pending."
                                    is SyncResult.Synced -> "Synced ${result.count} note(s) to cloud."
                                    is SyncResult.Failed -> "Sync failed: ${result.message}"
                                }
                        }
                    },
                    modifier = Modifier.testTag(NotesUiTags.SYNC),
                ) {
                    Text("Sync")
                }
            }

            Column(modifier = Modifier.testTag(NotesUiTags.NOTES_LIST)) {
                notes.forEach { note ->
                    Text("[${note.syncStatus}] ${note.text}")
                    HorizontalDivider()
                }
            }
        }
    }
}
