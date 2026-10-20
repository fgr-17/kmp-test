package com.jetbrains.greetingkmp

import com.jetbrains.greetingkmp.notes.NotesFacade
import com.jetbrains.greetingkmp.notes.NotesUiTags
import com.jetbrains.greetingkmp.notes.SyncResult
import com.jetbrains.greetingkmp.notes.SyncStatus
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

/**
 * Plain DOM controls (outside the Compose canvas) so Playwright can drive the web app
 * with the same [NotesUiTags] ids used by other platforms.
 *
 * Enabled only when the page URL contains `e2e=1`.
 */
fun installWebE2EOverlay(facade: NotesFacade) {
    if (!window.location.search.contains("e2e=1")) return

    val root = document.createElement("div") as HTMLElement
    root.id = "e2e-overlay"
    root.setAttribute(
        "style",
        "position:fixed;z-index:9999;left:8px;bottom:8px;padding:8px;background:#fff;" +
            "border:1px solid #333;display:flex;gap:8px;align-items:center;font-family:sans-serif;",
    )

    val status = document.createElement("span") as HTMLElement
    status.id = NotesUiTags.STATUS
    status.textContent = "e2e ready"

    val online = document.createElement("input") as HTMLInputElement
    online.id = NotesUiTags.ONLINE_SWITCH
    // setAttribute is more reliable than the .type property under Kotlin/Wasm
    online.setAttribute("type", "checkbox")
    online.checked = facade.isOnline
    // addEventListener survives Playwright clicks; the onchange property does not on Wasm.
    online.addEventListener("change", {
        facade.isOnline = online.checked
    })

    val input = document.createElement("input") as HTMLInputElement
    input.id = NotesUiTags.NOTE_INPUT
    input.setAttribute("type", "text")
    input.placeholder = "Note"

    val add = document.createElement("button") as HTMLButtonElement
    add.id = NotesUiTags.ADD_NOTE
    add.textContent = "Add note"
    add.onclick = {
        val text = input.value
        if (text.isNotBlank()) {
            facade.addNote(text)
            input.value = ""
            status.textContent = "Saved locally"
        }
        null
    }

    val sync = document.createElement("button") as HTMLButtonElement
    sync.id = NotesUiTags.SYNC
    sync.textContent = "Sync"
    sync.onclick = {
        CoroutineScope(Dispatchers.Default).launch {
            status.textContent =
                when (val result = facade.sync()) {
                    SyncResult.Offline -> "Still offline"
                    SyncResult.NothingToSync -> "Nothing pending"
                    is SyncResult.Synced -> "Synced ${result.count}"
                    is SyncResult.Failed -> "Sync failed: ${result.message}"
                }
        }
        null
    }

    val list = document.createElement("div") as HTMLElement
    list.id = NotesUiTags.NOTES_LIST

    root.append(status, online, input, add, sync, list)
    document.body?.append(root)

    CoroutineScope(Dispatchers.Default).launch {
        facade.notes.collectLatest { notes ->
            list.textContent =
                notes.joinToString(" | ") { note ->
                    val label = if (note.syncStatus == SyncStatus.PENDING) "PENDING" else "SYNCED"
                    "[$label] ${note.text}"
                }
        }
    }
}
