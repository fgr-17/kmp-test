package com.jetbrains.greetingkmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jetbrains.greetingkmp.notes.NotesUiTags

private fun Modifier.e2e(tag: String): Modifier =
    this
        .testTag(tag)
        .semantics { contentDescription = tag }

@Composable
@Preview
fun App(mainViewModel: MainViewModel = viewModel()) {
    MaterialTheme {
        val greetings by mainViewModel.greetingList.collectAsStateWithLifecycle()
        val notes by mainViewModel.notes.collectAsStateWithLifecycle()
        val online by mainViewModel.isOnline.collectAsStateWithLifecycle()
        val status by mainViewModel.status.collectAsStateWithLifecycle()
        var draft by remember { mutableStateOf("") }

        Column(
            modifier =
                Modifier
                    .semantics { testTagsAsResourceId = true }
                    .safeContentPadding()
                    .fillMaxSize()
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
                modifier = Modifier.e2e(NotesUiTags.STATUS),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (online) "Online" else "Offline")
                Switch(
                    checked = online,
                    onCheckedChange = mainViewModel::setOnline,
                    modifier = Modifier.e2e(NotesUiTags.ONLINE_SWITCH),
                )
            }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .e2e(NotesUiTags.NOTE_INPUT),
                label = { Text("Note") },
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        mainViewModel.addNote(draft)
                        draft = ""
                    },
                    modifier = Modifier.e2e(NotesUiTags.ADD_NOTE),
                ) {
                    Text("Add note")
                }
                Button(
                    onClick = { mainViewModel.sync() },
                    modifier = Modifier.e2e(NotesUiTags.SYNC),
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
