@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeNative)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    desktopNative {
        binaries.executable {
            entryPoint = "com.mikepenz.hypnoticcanvas.example.main"
        }
    }

    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib"))
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.material.kolor)
        }
        jvmMain.dependencies {
            implementation(libs.compose.desktop.linux.x64)
        }
        desktopNativeMain.dependencies {
            implementation(libs.compose.native.desktop)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.mikepenz.hypnoticcanvas.example.MainKt"
    }
}
