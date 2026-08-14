package com.mikepenz.hypnoticcanvas.example

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "HypnoticCanvas — JVM",
    ) {
        HypnoticCanvasExample("JVM · official Compose")
    }
}
