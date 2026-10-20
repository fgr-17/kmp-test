package com.jetbrains.greetingkmp.notes

/**
 * Pushes locally pending notes to the cloud when the device is online.
 * If [isOnline] is false, sync is skipped (mimics cellar with no signal).
 */
class NoteSyncService(
    private val store: LocalNoteStore,
    private val api: CloudNotesApi,
) {
    var isOnline: Boolean = false

    suspend fun syncPending(): SyncResult {
        if (!isOnline) {
            return SyncResult.Offline
        }
        val pending = store.pending()
        if (pending.isEmpty()) {
            return SyncResult.NothingToSync
        }
        return try {
            val accepted = api.pushBatch(pending)
            store.markSynced(accepted)
            SyncResult.Synced(accepted.size)
        } catch (e: Exception) {
            SyncResult.Failed(e.message ?: e.toString())
        }
    }
}

sealed interface SyncResult {
    data object Offline : SyncResult

    data object NothingToSync : SyncResult

    data class Synced(
        val count: Int,
    ) : SyncResult

    data class Failed(
        val message: String,
    ) : SyncResult
}
