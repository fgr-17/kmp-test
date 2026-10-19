package com.jetbrains.greetingkmp.notes

import kotlinx.serialization.Serializable

@Serializable
data class FieldNote(
    val id: String,
    val text: String,
    val createdAt: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)

@Serializable
enum class SyncStatus {
    PENDING,
    SYNCED,
}
