pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal {
            content {
                includeGroup("com.materialkolor")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "multiplatform-library-template"
include(":lib")
include(":examples:jvm")
include(":examples:multiplatform")
include(":examples:web")
