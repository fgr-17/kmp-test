package com.jetbrains.greetingkmp

class JvmPlatform : Platform {
    override val name: String =
        "Java ${System.getProperty("java.version")} (${System.getProperty("os.name")})"
}

actual fun getPlatform(): Platform = JvmPlatform()
