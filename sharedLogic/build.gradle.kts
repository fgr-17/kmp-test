@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kmpNativeCoroutines)
    alias(libs.plugins.sqldelight)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
        }
    }

    wasmJs {
        browser()
    }

    jvm()

    android {
        namespace = "com.jetbrains.greetingkmp.sharedLogic"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.experimental.ExperimentalObjCName")
            }
        }
        commonMain.dependencies {
            // put your Multiplatform dependencies here
            // The Kotlin Multiplatform Gradle plugin adds
            // platform-specific artifacts for coroutines and datetime
            // automatically
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.datetime)
            // Main Ktor dependency
            implementation(libs.ktor.client.core)
            // Dependencies that allow Ktor to use serialization
            // with a specific format
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.sqldelight.runtime)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            // Provides the Android engine for Ktor
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            // Provides the Darwin engine for Ktor
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        wasmJsMain.dependencies {
            // Provides the JS/Wasm fetch engine for Ktor
            implementation(libs.ktor.client.js)
        }
        jvmMain.dependencies {
            // Provides the CIO engine for Ktor on desktop JVM
            implementation(libs.ktor.client.cio)
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}