import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import com.mikepenz.hypnoticcanvas.RuntimeEffect
import com.mikepenz.hypnoticcanvas.shaders.Shader
import kotlin.math.abs
import kotlin.math.sin

internal object WaveTextureShader : Shader {
    private val waveformTexture = buildWaveformTexture()
    private val waveformShader = ImageShader(waveformTexture, TileMode.Repeated, TileMode.Clamp)

    var musicLevelProvider: () -> Float = { 0f }

    override val name: String = "WaveTextureShader"
    override val authorName: String = "GitHub Copilot"
    override val authorUrl: String = ""
    override val credit: String = ""
    override val license: String = "MIT License"
    override val licenseUrl: String = "https://opensource.org/license/mit"
    override val speedModifier: Float = 0.6f

    override val sksl: String = """
uniform float uTime;
uniform float uBeat;
uniform vec3 uResolution;
uniform shader uWaveform;

vec4 main(vec2 fragCoord) {
    vec2 uv = fragCoord / uResolution.xy;
    float beatPulse = smoothstep(0.08, 0.90, uBeat);
    vec2 waveformCoord = vec2(fract(uv.x * 2.0 + uTime * (0.05 + beatPulse * 0.12)) * 255.0, 48.0);
    float waveform = uWaveform.eval(waveformCoord).r;
    float pulse = smoothstep(0.15, 0.95, waveform);
    float waveGlow = sin((uv.y * 14.0) - uTime * 2.5 + beatPulse * 10.0);
    vec3 base = mix(vec3(0.03, 0.05, 0.10), vec3(0.0, 0.82, 1.0), pulse);
    base += beatPulse * vec3(0.08, 0.22, 0.55);
    base += max(0.0, waveGlow) * beatPulse * vec3(0.32, 0.08, 0.18);
    base += smoothstep(0.80, 1.00, waveform) * vec3(0.95, 0.35, 0.10);
    return vec4(base, 1.0);
}
    """.trimIndent()

    override fun applyUniforms(runtimeEffect: RuntimeEffect, time: Float, width: Float, height: Float) {
        super.applyUniforms(runtimeEffect, time, width, height)
        runtimeEffect.setShaderUniform("uWaveform", waveformShader)
        runtimeEffect.setFloatUniform("uBeat", musicLevelProvider().coerceIn(0f, 1f))
    }
}

private fun buildWaveformTexture(): ImageBitmap {
    val width = 256
    val height = 96
    val image = ImageBitmap(width, height)
    val canvas = Canvas(image)

    val backgroundPaint = Paint().apply {
        color = Color(0xFF06111A)
        isAntiAlias = false
    }
    canvas.drawRect(Rect(0f, 0f, width.toFloat(), height.toFloat()), backgroundPaint)

    val barPaint = Paint().apply {
        color = Color(0xFF00D4FF)
        isAntiAlias = false
    }
    val accentPaint = Paint().apply {
        color = Color(0xFFFF5F36)
        isAntiAlias = false
    }

    val barCount = 64
    val barWidth = width.toFloat() / barCount
    for (index in 0 until barCount) {
        val phase = index / barCount.toFloat()
        val amplitude = 0.25f + 0.75f * abs(sin(phase * 6.2831853f * 4f))
        val barHeight = height * amplitude
        val left = index * barWidth
        val right = left + barWidth * 0.68f
        val top = height - barHeight
        val paint = if (index % 7 == 0) accentPaint else barPaint
        canvas.drawRect(Rect(left, top, right, height.toFloat()), paint)
    }

    return image
}
