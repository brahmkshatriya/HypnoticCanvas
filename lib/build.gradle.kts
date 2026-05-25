@file:OptIn(ExperimentalWasmDsl::class)
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)

    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = "com.mikepenz.hypnoticcanvas"
version = "1.0.0"

kotlin {
    jvm()
    android {
        namespace = "com.mikepenz.hypnoticcanvas"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
    }
    iosArm64()
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

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.ui)
            api(libs.compose.foundation)
        }
        val skiaMain by creating { dependsOn(commonMain.get()) }
        iosArm64Main { dependsOn(skiaMain) }
        jsMain { dependsOn(skiaMain) }
        wasmJsMain { dependsOn(skiaMain) }
        jvmMain { dependsOn(skiaMain) }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
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
