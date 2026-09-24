@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)

    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeNative)
    alias(libs.plugins.composeCompiler)
}

group = "com.mikepenz.hypnoticcanvas"
version = "1.0.5"

kotlin {
    applyHierarchyTemplate(KotlinHierarchyTemplate.default) {
        common {
            group("skia") {
                withJvm()
                withJs()
                withWasmJs()
                withNative()
            }
        }
    }

    android {
        namespace = "com.mikepenz.hypnoticcanvas"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
    }
    wasmJs {
        outputModuleName = "library"
        browser {
            commonWebpackConfig {
                outputFileName = "library.js"
            }
        }
        binaries.executable()
    }
    js { nodejs() }
    iosArm64()
    jvm()
    desktopNative()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.runtime.annotation)
            api(libs.compose.ui)
            api(libs.compose.foundation)
        }
        named("skiaMain") {
            dependencies {
                implementation(libs.skiko)
            }
        }
        desktopNativeMain.dependencies {
            api(libs.compose.native.ui)
            api(libs.compose.native.foundation)
            implementation(libs.skiko.native)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    if (
        providers.gradleProperty("signingInMemoryKey").isPresent ||
            providers.gradleProperty("signing.secretKeyRingFile").isPresent
    ) {
        signAllPublications()
    }
    coordinates("dev.brahmkshatriya.hypnoticcanvas", "lib", version.toString())
    pom {
        name = "HypnoticCanvas"
        description = "A shader modifier for Compose Multiplatform / Jetpack Compose"
        url = "https://github.com/brahmkshatriya/HypnoticCanvas/"
        inceptionYear = "2026"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "mikepenz"
                name = "Mike Penz"
                url = "https://github.com/mikepenz"
            }
            developer {
                id = "brahmkshatriya"
                name = "Shivam Brahmkshatriya"
                url = "https://github.com/brahmkshatriya"
            }
        }
        scm {
            url = "https://github.com/brahmkshatriya/HypnoticCanvas/"
            connection = "scm:git:git://github.com/brahmkshatriya/HypnoticCanvas.git"
            developerConnection = "scm:git:git://github.com/brahmkshatriya/HypnoticCanvas.git"
        }
    }
}
