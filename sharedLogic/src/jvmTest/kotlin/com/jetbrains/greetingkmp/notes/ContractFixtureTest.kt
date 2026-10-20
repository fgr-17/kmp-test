package com.jetbrains.greetingkmp.notes

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Decodes the golden JSON files under contracts/fixtures.
 * Those files are the payload also shown as examples in contracts/openapi.yaml.
 */
class ContractFixtureTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun batchRequest_fixture_matchesDtoShape() {
        val decoded = json.decodeFromString<BatchRequestFixture>(fixture("batch_request.json"))
        assertEquals(1, decoded.notes.size)
        assertEquals("note-1", decoded.notes.single().id)
        assertEquals("Checked barrel B12", decoded.notes.single().text)
    }

    @Test
    fun batchResponse_fixture_matchesDtoShape() {
        val decoded = json.decodeFromString<BatchResponseFixture>(fixture("batch_response.json"))
        assertEquals(listOf("note-1"), decoded.acceptedIds)
    }

    @Serializable
    private data class BatchRequestFixture(
        val notes: List<NoteDto>,
    )

    @Serializable
    private data class BatchResponseFixture(
        val acceptedIds: List<String>,
    )

    private fun fixture(name: String): String {
        val candidates =
            listOf(
                File("contracts/fixtures/$name"),
                File("../contracts/fixtures/$name"),
            )
        val file = candidates.firstOrNull { it.isFile }
        check(file != null) { "Missing contracts/fixtures/$name (looked in $candidates)" }
        return file.readText()
    }
}
