package com.jetbrains.greetingkmp.notes

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

internal actual fun notesEngine(): HttpClientEngine = Js.create()
