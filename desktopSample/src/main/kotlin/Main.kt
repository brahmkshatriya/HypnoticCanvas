import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.GlossyGradients

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "HypnoticCanvas",
    ) {
        Box(Modifier.fillMaxSize().shaderBackground(GlossyGradients))
    }
}