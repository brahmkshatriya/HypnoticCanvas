import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.Shader
import kotlinx.coroutines.isActive
import kotlin.math.pow

private data class ShaderWindowSpec(
    val id: String,
    val title: String,
    val shader: Shader,
)

private val desktopShaders = listOf(
    ShaderWindowSpec("bass-mesh", "Bass Reactive Mesh Gradient", BassReactiveMeshGradientShader),
    ShaderWindowSpec("waves-remix", "Waves Remix", WavesRemixShader),
    ShaderWindowSpec("gameboy", "Gameboy", GameboyShader),
    ShaderWindowSpec("oscilloscope", "Glowing Oscilloscope", GlowingOscilloscopeShader),
    ShaderWindowSpec("wave-texture", "Wave Texture", WaveTextureShader),
    ShaderWindowSpec("shadertoy-xtl3w2", "Daily: 1/3/15", ShadertoyXtl3W2Shader),
)

fun main() = application {
    val player = remember { DesktopMusicPlayer.create() }
    var musicLevel by remember { mutableStateOf(0f) }
    var bassLevel by remember { mutableStateOf(0f) }
    var waveform by remember { mutableStateOf(FloatArray(512)) }
    var spectrum by remember { mutableStateOf(FloatArray(512)) }
    var isPlaying by remember { mutableStateOf(false) }
    val windowVisibility = remember {
        mutableStateMapOf<String, Boolean>().apply {
            desktopShaders.forEach { put(it.id, true) }
        }
    }

    DisposableEffect(player) {
        player?.play()
        isPlaying = true
        onDispose {
            player?.close()
        }
    }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (isActive) {
            withInfiniteAnimationFrameMillis {
                if (player.isPlaying()) {
                    val currentLevel = player.level().coerceIn(0f, 1f)
                    val currentSpectrum = player.spectrum()
                    musicLevel = musicLevel * 0.88f + currentLevel * 0.12f
                    spectrum = currentSpectrum
                    bassLevel = bassLevel * 0.97f + bassEnergy(currentSpectrum) * 0.03f
                    waveform = waveform.lerpTo(player.waveform(512), 0.18f)
                    isPlaying = true
                }
            }
        }
    }

    SideEffect {
        BassReactiveMeshGradientShader.bassProvider = { bassLevel }
        WavesRemixShader.musicLevelProvider = { musicLevel }
        WavesRemixShader.waveformProvider = { waveform }
        GameboyShader.spectrumProvider = { spectrum }
        GameboyShader.waveformProvider = { waveform }
        GlowingOscilloscopeShader.levelProvider = { musicLevel }
        GlowingOscilloscopeShader.waveformProvider = { waveform }
        WaveTextureShader.musicLevelProvider = { musicLevel }
        ShadertoyXtl3W2Shader.musicLevelProvider = { musicLevel }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "HypnoticCanvas Launcher",
    ) {
        LauncherWindow(
            player = player,
            isPlaying = isPlaying,
            windowVisibility = windowVisibility,
            onTogglePlayback = {
                if (player != null) {
                    isPlaying = player.togglePlayback()
                }
            },
        )
    }

    desktopShaders.forEach { spec ->
        if (windowVisibility[spec.id] != false) {
            Window(
                onCloseRequest = { windowVisibility[spec.id] = false },
                title = spec.title,
            ) {
                ShaderWindow(
                    shader = spec.shader,
                    isPlaying = isPlaying,
                    onTogglePlayback = {
                        if (player != null) {
                            isPlaying = player.togglePlayback()
                        }
                    },
                    onClose = { windowVisibility[spec.id] = false },
                )
            }
        }
    }
}

@Composable
private fun LauncherWindow(
    player: DesktopMusicPlayer?,
    isPlaying: Boolean,
    windowVisibility: MutableMap<String, Boolean>,
    onTogglePlayback: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicText(
                text = "Shaders",
                style = TextStyle(color = Color(0xFFBFFDF1)),
            )
            desktopShaders.forEach { spec ->
                val visible = windowVisibility[spec.id] != false
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { windowVisibility[spec.id] = !visible }
                        .padding(vertical = 6.dp),
                ) {
                    BasicText(
                        text = "${if (visible) "Hide" else "Show"} ${spec.title}",
                        style = TextStyle(color = Color(0xFFD7FFF7)),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clickable {
                        if (player != null) {
                            onTogglePlayback()
                        }
                    }
                    .padding(top = 12.dp),
            ) {
                BasicText(
                    text = if (isPlaying) "Pause audio" else "Play audio",
                    style = TextStyle(color = Color(0xFFFFD9A3)),
                )
            }
        }
    }
}

@Composable
private fun ShaderWindow(
    shader: Shader,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().shaderBackground(shader))
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicText(
                text = shader.name,
                style = TextStyle(color = Color(0xFFBFFDF1)),
            )
            Box(
                modifier = Modifier.clickable { onTogglePlayback() }
            ) {
                BasicText(
                    text = if (isPlaying) "Pause" else "Play",
                    style = TextStyle(color = Color(0xFFFFD9A3)),
                )
            }
            Box(
                modifier = Modifier.clickable { onClose() }
            ) {
                BasicText(
                    text = "Close window",
                    style = TextStyle(color = Color(0xFFD7FFF7)),
                )
            }
        }
    }
}

private fun FloatArray.lerpTo(target: FloatArray, alpha: Float): FloatArray {
    if (this.size != target.size) return target.copyOf()
    val clampedAlpha = alpha.coerceIn(0f, 1f)
    val output = FloatArray(size)
    for (index in indices) {
        output[index] = this[index] * (1f - clampedAlpha) + target[index] * clampedAlpha
    }
    return output
}

private fun bassEnergy(spectrum: FloatArray): Float {
    if (spectrum.isEmpty()) return 0f

    val bassBinCount = spectrum.size.coerceAtMost(12)
    var weightedSum = 0f
    var weightTotal = 0f

    for (index in 0 until bassBinCount) {
        val weight = (bassBinCount - index).toFloat().pow(1.25f)
        weightedSum += spectrum[index].coerceIn(0f, 1f) * weight
        weightTotal += weight
    }

    return if (weightTotal > 0f) {
        (weightedSum / weightTotal).coerceIn(0f, 1f)
    } else {
        0f
    }
}
