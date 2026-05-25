# HypnoticCanvas (fork)

[![Maven Central](https://img.shields.io/maven-central/v/dev.brahmkshatriya.hypnoticcanvas/lib.svg)](https://central.sonatype.com/artifact/dev.brahmkshatriya.hypnoticcanvas/lib)

A fork of https://github.com/mikepenz/HypnoticCanvas with removed fluff and updates to support the newer Compose Skia API.

## Installation

Add the library as a dependency to your Kotlin Multiplatform project. Replace `VERSION` with the desired release (click the badge above to see the latest).

```toml
hypnoticcanvas = { module = "dev.brahmkshatriya.hypnoticcanvas:lib", version = "VERSION" }
```

## Usage

The library provides a `shaderBackground` modifier. Use it.

```kotlin
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.GlossyGradients

//...
Box(Modifier.fillMaxSize().shaderBackground(GlossyGradients))
```
You can also take a look at the [examples](/examples) module for more examples.

Custom shaders can be created by implementing the [Shader](/lib/src/commonMain/kotlin/shaders/Shader.kt) interface.

Example shaders included in the library are:
- [MeshGradient](/lib/src/commonMain/kotlin/shaders/MeshGradient.kt)
- [GlossyGradients](/lib/src/commonMain/kotlin/shaders/GlossyGradients.kt)
- [MesmerizingLens](/lib/src/commonMain/kotlin/shaders/MesmerizingLens.kt)
