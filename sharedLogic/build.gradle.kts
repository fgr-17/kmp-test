@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kmpNativeCoroutines)
    alias(libs.plugins.sqldelight)
    jacoco
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
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
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

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
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            // OkHttp, not the HttpURLConnection engine: on API 37 the URLConnection
            // client opens an IPv6 socket to 10.0.2.2 and the SYN never completes.
            implementation(libs.ktor.client.okhttp)
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

// JaCoCo on the JVM target only — Kover conflicts with AGP's KMP Android library plugin.
tasks.named<Test>("jvmTest") {
    finalizedBy("jacocoJvmTestReport")
}

tasks.register<JacocoReport>("jacocoJvmTestReport") {
    group = "verification"
    description = "HTML/XML coverage for sharedLogic jvmTest (commonTest on JVM)."
    dependsOn("jvmTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("classes/kotlin/jvm/main")) {
            exclude("**/Platform*.class")
        },
    )
    sourceDirectories.setFrom(
        files(
            "src/commonMain/kotlin",
            "src/jvmMain/kotlin",
        ),
    )
    executionData.setFrom(layout.buildDirectory.file("jacoco/jvmTest.exec"))
}
