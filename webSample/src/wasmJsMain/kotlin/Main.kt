import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.GlossyGradients

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        Box(Modifier.fillMaxSize().shaderBackground(GlossyGradients))
    }
}