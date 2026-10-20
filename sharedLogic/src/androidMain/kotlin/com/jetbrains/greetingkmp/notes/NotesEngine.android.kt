package com.jetbrains.greetingkmp.notes

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Dns
import java.net.InetAddress

/**
 * Android's resolver opens an IPv6 socket for IPv4 literals such as 127.0.0.1,
 * and that SYN never completes. Notes sync forces an IPv4 address.
 */
internal actual fun notesEngine(): HttpClientEngine =
    OkHttp.create {
        config {
            dns(
                Dns { hostname ->
                    val parts = hostname.split('.')
                    val ipv4 =
                        parts.size == 4 &&
                            parts.all { part -> part.toIntOrNull() in 0..255 }
                    if (ipv4) {
                        val bytes = parts.map { it.toInt().toByte() }.toByteArray()
                        listOf(InetAddress.getByAddress(hostname, bytes))
                    } else {
                        Dns.SYSTEM.lookup(hostname)
                    }
                },
            )
        }
    }
