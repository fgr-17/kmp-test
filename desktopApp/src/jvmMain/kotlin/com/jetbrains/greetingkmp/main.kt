package com.jetbrains.greetingkmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jetbrains.greetingkmp.notes.CloudConfig
import com.jetbrains.greetingkmp.notes.NotesFacade

fun main() =
    application {
        val facade = NotesFacade(CloudConfig.DESKTOP_BASE_URL)
        maybeStartDesktopE2EBridge(facade)
        Window(
            onCloseRequest = ::exitApplication,
            title = "GreetingKMP",
            visible = System.getenv("SHOW_UI") == "1",
        ) {
            DesktopApp(notesFacade = facade)
        }
    }
