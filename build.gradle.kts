import de.aaschmid.gradle.plugins.cpd.Cpd
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kmpNativeCoroutines) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.cpd)
}

cpd {
    language = "kotlin"
    minimumTokenCount = 60
}

tasks.named<Cpd>("cpdCheck") {
    // WebApp/DesktopApp are intentional platform twins until Compose UI is shared.
    // They still show up if you temporarily include them; keep them out of the gate.
    source = fileTree(rootDir) {
        include(
            "sharedLogic/src/**/*.kt",
            "sharedUI/src/**/*.kt",
            "androidApp/src/**/*.kt",
            "webApp/src/**/*.kt",
            "desktopApp/src/**/*.kt",
        )
        exclude(
            "**/build/**",
            "**/WebApp.kt",
            "**/DesktopApp.kt",
            "**/WebE2EOverlay.kt",
            "**/DesktopE2EBridge.kt",
        )
    }
    reports {
        xml.required.set(true)
        text.required.set(true)
    }
    ignoreFailures = false
}

subprojects {
    fun Project.configureDetekt(vararg sourceDirs: String) {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        extensions.configure<DetektExtension>("detekt") {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            parallel = true
            ignoreFailures = false
            source.setFrom(files(*sourceDirs))
        }
        tasks.withType<Detekt>().configureEach {
            jvmTarget = "11"
            // Compose/KMP entry points are conventionally named main.kt
            exclude("**/main.kt")
            reports {
                html.required.set(true)
                xml.required.set(true)
                txt.required.set(true)
                sarif.required.set(true)
                md.required.set(true)
            }
        }
    }

    fun Project.configureKtlint() {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        extensions.configure<KtlintExtension>("ktlint") {
            version.set("1.5.0")
            android.set(false)
            ignoreFailures.set(false)
            reporters {
                reporter(ReporterType.HTML)
                reporter(ReporterType.CHECKSTYLE)
                reporter(ReporterType.PLAIN)
            }
            filter {
                exclude("**/generated/**")
                exclude("**/build/**")
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        configureDetekt(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/iosMain/kotlin",
            "src/jvmMain/kotlin",
            "src/wasmJsMain/kotlin",
            "src/commonTest/kotlin",
            "src/androidHostTest/kotlin",
            "src/iosTest/kotlin",
        )
        configureKtlint()
    }

    pluginManager.withPlugin("com.android.application") {
        configureDetekt("src/main/kotlin")
        configureKtlint()
    }
}

tasks.register("qualityCheck") {
    group = "verification"
    description =
        "Runs Detekt, ktlint, CPD duplication, and sharedLogic JVM unit tests (JaCoCo HTML/XML)."
    dependsOn(
        ":sharedLogic:detekt",
        ":sharedUI:detekt",
        ":webApp:detekt",
        ":desktopApp:detekt",
        ":androidApp:detekt",
        ":sharedLogic:ktlintCheck",
        ":sharedUI:ktlintCheck",
        ":webApp:ktlintCheck",
        ":desktopApp:ktlintCheck",
        ":androidApp:ktlintCheck",
        "cpdCheck",
        ":sharedLogic:jvmTest",
        ":sharedLogic:jacocoJvmTestReport",
    )
}
