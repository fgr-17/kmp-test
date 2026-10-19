package com.jetbrains.greetingkmp.notes

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CloudNotesApi(
    private val baseUrl: String,
    private val httpClient: HttpClient = defaultClient(),
) {
    suspend fun pushBatch(notes: List<FieldNote>): List<String> {
        if (notes.isEmpty()) return emptyList()
        val response: BatchResponse = httpClient.post("$baseUrl/notes/batch") {
            contentType(ContentType.Application.Json)
            setBody(BatchRequest(notes = notes.map { it.toDto() }))
        }.body()
        return response.acceptedIds
    }

    suspend fun fetchAll(): List<NoteDto> =
        httpClient.get("$baseUrl/notes").body()

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }
}

@Serializable
data class NoteDto(
    val id: String,
    val text: String,
    val createdAt: String,
)

@Serializable
private data class BatchRequest(val notes: List<NoteDto>)

@Serializable
private data class BatchResponse(val acceptedIds: List<String>)

private fun FieldNote.toDto() = NoteDto(id = id, text = text, createdAt = createdAt)
