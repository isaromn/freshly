import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.koin.core)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.ecoalacena.app.MainKt"
        // Name shown in the macOS menu bar and Dock when running with `./gradlew :desktopApp:run`
        // (other JVMs do not know this option and would refuse to start).
        if (System.getProperty("os.name").startsWith("Mac")) {
            jvmArgs += listOf("-Xdock:name=Freshly")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Freshly"
            packageVersion = "1.0.0"
        }
    }
}