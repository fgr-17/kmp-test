package com.jetbrains.greetingkmp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.jetbrains.greetingkmp.notes.CloudConfig
import com.jetbrains.greetingkmp.notes.NotesFacade
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = document.body!!
    // ComposeViewport clears its container. Keep the E2E overlay as a sibling.
    val composeHost = document.createElement("div") as HTMLElement
    composeHost.id = "compose-host"
    body.appendChild(composeHost)

    val facade = NotesFacade(CloudConfig.WEB_BASE_URL)
    ComposeViewport(composeHost) {
        WebApp(notesFacade = facade)
    }
    installWebE2EOverlay(facade)
}
