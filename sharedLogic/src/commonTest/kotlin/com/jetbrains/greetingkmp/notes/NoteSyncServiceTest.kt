package com.jetbrains.greetingkmp.notes

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NoteSyncServiceTest {
    @Test
    fun syncPending_whenOffline_returnsOffline() =
        runTest {
            val store = LocalNoteStore()
            store.add("note", "t")
            val service = NoteSyncService(store, CloudNotesApi("http://test", silentClient()))
            service.isOnline = false

            assertEquals(SyncResult.Offline, service.syncPending())
            assertEquals(1, store.pending().size)
        }

    @Test
    fun syncPending_whenNothingPending_returnsNothingToSync() =
        runTest {
            val service = NoteSyncService(LocalNoteStore(), CloudNotesApi("http://test", silentClient()))
            service.isOnline = true
            assertEquals(SyncResult.NothingToSync, service.syncPending())
        }

    @Test
    fun syncPending_whenOnline_marksAcceptedNotesSynced() =
        runTest {
            val store = LocalNoteStore()
            val note = store.add("Checked barrel B12", "2026-01-01T00:00:00Z")
            val engine =
                MockEngine { request ->
                    assertEquals("http://test/notes/batch", request.url.toString())
                    respond(
                        content = """{"acceptedIds":["${note.id}"]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val api = CloudNotesApi("http://test", jsonClient(engine))
            val service = NoteSyncService(store, api)
            service.isOnline = true

            val result = service.syncPending()
            assertIs<SyncResult.Synced>(result)
            assertEquals(1, result.count)
            assertEquals(
                SyncStatus.SYNCED,
                store.notes.value
                    .single()
                    .syncStatus,
            )
            assertEquals(0, store.pending().size)
        }

    @Test
    fun syncPending_whenHttpFails_returnsFailed() =
        runTest {
            val store = LocalNoteStore()
            store.add("note", "t")
            val engine =
                MockEngine {
                    respond("boom", HttpStatusCode.InternalServerError)
                }
            val service = NoteSyncService(store, CloudNotesApi("http://test", jsonClient(engine)))
            service.isOnline = true

            assertIs<SyncResult.Failed>(service.syncPending())
            assertEquals(1, store.pending().size)
        }

    private fun silentClient(): HttpClient =
        jsonClient(
            MockEngine { error("HTTP should not be called") },
        )

    private fun jsonClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
}
