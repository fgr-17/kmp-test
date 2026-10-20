package com.jetbrains.greetingkmp

import com.jetbrains.greetingkmp.notes.NotesFacade
import com.jetbrains.greetingkmp.notes.SyncResult
import com.jetbrains.greetingkmp.notes.SyncStatus
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

/**
 * Tiny JDK HttpServer so the Behave desktop driver can drive [NotesFacade]
 * without brittle desktop UI automation. Enabled when `GREETING_E2E=1`.
 */
fun maybeStartDesktopE2EBridge(facade: NotesFacade) {
    if (System.getenv("GREETING_E2E") != "1") return

    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 18081), 0)
    server.createContext("/e2e/health") { exchange ->
        val body = """{"ok":true}"""
        exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
        exchange.responseBody.use { it.write(body.toByteArray()) }
    }
    server.createContext("/e2e/online") { exchange ->
        val online = exchange.requestURI.query?.contains("value=true") == true
        facade.isOnline = online
        exchange.sendResponseHeaders(204, -1)
        exchange.close()
    }
    server.createContext("/e2e/add") { exchange ->
        val text =
            exchange.requestURI.query
                ?.substringAfter("text=", "")
                ?.let { java.net.URLDecoder.decode(it, Charsets.UTF_8) }
                .orEmpty()
        if (text.isNotBlank()) {
            facade.addNote(text)
        }
        exchange.sendResponseHeaders(204, -1)
        exchange.close()
    }
    server.createContext("/e2e/sync") { exchange ->
        val message =
            runBlocking {
                when (val result = facade.sync()) {
                    SyncResult.Offline -> "Still offline"
                    SyncResult.NothingToSync -> "Nothing pending"
                    is SyncResult.Synced -> "Synced ${result.count}"
                    is SyncResult.Failed -> "Sync failed: ${result.message}"
                }
            }
        val body = """{"status":"$message"}"""
        exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
        exchange.responseBody.use { it.write(body.toByteArray()) }
    }
    server.createContext("/e2e/reset") { exchange ->
        facade.clear()
        exchange.sendResponseHeaders(204, -1)
        exchange.close()
    }
    server.createContext("/e2e/notes") { exchange ->
        val body =
            facade.notes.value.joinToString(prefix = "[", postfix = "]") { note ->
                val status = if (note.syncStatus == SyncStatus.PENDING) "PENDING" else "SYNCED"
                """{"text":${jsonString(note.text)},"syncStatus":"$status"}"""
            }
        exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
        exchange.responseBody.use { it.write(body.toByteArray()) }
    }
    server.executor = null
    server.start()
    println("Desktop E2E bridge on http://127.0.0.1:18081")
}

private fun jsonString(value: String): String =
    buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else -> append(ch)
            }
        }
        append('"')
    }
