package com.jetbrains.greetingkmp.notes

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun notesEngine(): HttpClientEngine = Darwin.create()
