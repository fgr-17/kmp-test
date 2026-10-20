package com.jetbrains.greetingkmp.notes

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Small façade the UIs call for the offline-notes playground.
 *
 * Note: Kotlin default parameters are not visible from Swift/ObjC, so the
 * single-arg constructor is what iOS should call.
 */
class NotesFacade(
    baseUrl: String,
    private val store: LocalNoteStore,
) {
    constructor(baseUrl: String) : this(baseUrl, LocalNoteStore())

    private val api = CloudNotesApi(baseUrl)
    private val syncService = NoteSyncService(store, api)

    @NativeCoroutines
    val notes: StateFlow<List<FieldNote>> = store.notes

    var isOnline: Boolean
        get() = syncService.isOnline
        set(value) {
            syncService.isOnline = value
        }

    fun clear() {
        store.clear()
        isOnline = false
    }

    @OptIn(ExperimentalTime::class)
    fun addNote(text: String): FieldNote {
        val createdAt = Clock.System.now().toString()
        return store.add(text, createdAt)
    }

    @NativeCoroutines
    suspend fun sync(): SyncResult = syncService.syncPending()
}

/** Defaults for the local Go server (`backend/`, port 8081). */
object CloudConfig {
    const val DESKTOP_BASE_URL = "http://localhost:8081"
    const val WEB_BASE_URL = "http://localhost:8081"

    /** iOS Simulator can reach the Mac host via localhost. */
    const val IOS_SIMULATOR_BASE_URL = "http://localhost:8081"

    /**
     * Emulator loopback. `adb reverse tcp:8081 tcp:8081` forwards this to the
     * host Go server. 10.0.2.2 stays in SYN_SENT for app traffic on API 37.
     */
    const val ANDROID_EMULATOR_BASE_URL = "http://127.0.0.1:8081"
}
