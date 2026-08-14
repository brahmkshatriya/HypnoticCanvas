package com.mikepenz.hypnoticcanvas.example

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        HypnoticCanvasExample("WebAssembly · official Compose")
    }
}
