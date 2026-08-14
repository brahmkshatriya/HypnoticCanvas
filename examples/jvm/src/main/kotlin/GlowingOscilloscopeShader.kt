import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import com.mikepenz.hypnoticcanvas.RuntimeEffect
import com.mikepenz.hypnoticcanvas.shaders.Shader

private const val OSCILLOSCOPE_TEXTURE_WIDTH = 4_096

internal object GlowingOscilloscopeShader : Shader {
    private var waveformTexture = buildOscilloscopeWaveformTexture(FloatArray(0))
    private var waveformShader = ImageShader(waveformTexture, TileMode.Clamp, TileMode.Clamp)

    var levelProvider: () -> Float = { 0f }
    var waveformProvider: () -> FloatArray = { FloatArray(0) }

    override val name: String = "Glowing Oscilloscope"
    override val authorName: String = "Local"
    override val authorUrl: String = ""
    override val credit: String = ""
    override val license: String = "MIT License"
    override val licenseUrl: String = "https://opensource.org/license/mit"
    override val speedModifier: Float = 0.9f

    override val sksl: String = """
uniform vec3 uResolution;
uniform float uTime;
uniform float uLevel;
uniform shader uWaveform;

float sampleWave(float x) {
    return uWaveform.eval(vec2(clamp(x, 0.0, 1.0) * 4095.0, 0.5)).r * 2.0 - 1.0;
}

float gridLine(float value, float count, float width) {
    float cell = fract(value * count);
    float line = min(cell, 1.0 - cell);
    return 1.0 - smoothstep(0.0, width, line);
}

vec4 main(vec2 fragCoord) {
    vec2 uv = fragCoord.xy / uResolution.xy;
    float aspect = uResolution.x / uResolution.y;

    vec3 col = vec3(0.008, 0.014, 0.018);
    col += vec3(0.0, 0.018, 0.022) * (1.0 - distance(uv, vec2(0.5, 0.5)));

    float gridX = gridLine(uv.x, 12.0, 0.018);
    float gridY = gridLine(uv.y, 8.0, 0.018);
    float centerLine = 1.0 - smoothstep(0.0, 0.004, abs(uv.y - 0.5));
    float scanline = 0.035 * sin((uv.y * uResolution.y + uTime * 18.0) * 3.14159);

    col += vec3(0.0, 0.18, 0.14) * (gridX + gridY) * 0.15;
    col += vec3(0.0, 0.34, 0.24) * centerLine * 0.32;
    col += scanline;

    float amp = 0.30 + uLevel * 0.14;
    float xStep = 1.0 / 1024.0;
    float y0 = 0.5 - sampleWave(uv.x - xStep) * amp;
    float y1 = 0.5 - sampleWave(uv.x) * amp;
    float y2 = 0.5 - sampleWave(uv.x + xStep) * amp;
    float y3 = 0.5 - sampleWave(uv.x + 2.0 * xStep) * amp;
    float y4 = 0.5 - sampleWave(uv.x - 2.0 * xStep) * amp;
    float lineY = (y0 + y1 + y2 + y3 + y4) / 5.0;

    float d = abs(uv.y - lineY);
    float aa = 1.5 / uResolution.y;
    float core = 1.0 - smoothstep(aa * 0.25, aa * 1.15, d);
    float glowA = 1.0 - smoothstep(aa * 1.25, aa * 10.0, d);
    float bloom = 0.35 + uLevel * 0.35;

    vec3 trace = vec3(0.12, 1.0, 0.76);
    col += trace * core * 1.85;
    col += trace * glowA * 0.30 * bloom;

    float vignette = smoothstep(0.92, 0.25, distance(uv, vec2(0.5, 0.5)));
    col *= vignette;
    col = pow(clamp(col, 0.0, 1.0), vec3(0.82));

    return vec4(col, 1.0);
}
    """.trimIndent()

    override fun applyUniforms(runtimeEffect: RuntimeEffect, time: Float, width: Float, height: Float) {
        super.applyUniforms(runtimeEffect, time, width, height)
        waveformTexture = buildOscilloscopeWaveformTexture(waveformProvider())
        waveformShader = ImageShader(waveformTexture, TileMode.Clamp, TileMode.Clamp)
        runtimeEffect.setShaderUniform("uWaveform", waveformShader)
        runtimeEffect.setFloatUniform("uLevel", levelProvider().coerceIn(0f, 1f))
    }
}

private fun buildOscilloscopeWaveformTexture(waveform: FloatArray): ImageBitmap {
    val image = ImageBitmap(OSCILLOSCOPE_TEXTURE_WIDTH, 1)
    val canvas = Canvas(image)
    val paint = Paint().apply {
        isAntiAlias = false
    }

    for (x in 0 until OSCILLOSCOPE_TEXTURE_WIDTH) {
        val normalizedX = x / (OSCILLOSCOPE_TEXTURE_WIDTH - 1).toFloat()
        val sample = if (waveform.isNotEmpty()) {
            smoothOscilloscopeSample(waveform, normalizedX)
        } else {
            0f
        }
        paint.color = grayscale(0.5f + sample * 0.5f)
        canvas.drawRect(Rect(x.toFloat(), 0f, x + 1f, 1f), paint)
    }

    return image
}

private fun smoothOscilloscopeSample(waveform: FloatArray, x: Float): Float {
    val position = x * waveform.lastIndex
    val base = position.toInt().coerceIn(0, waveform.lastIndex)
    val fraction = position - base
    var sum = 0f
    var totalWeight = 0f

    for (offset in -3..4) {
        val index = (base + offset).coerceIn(0, waveform.lastIndex)
        val distance = kotlin.math.abs(offset - fraction)
        val weight = (1f - distance / 4.5f).coerceAtLeast(0f)
        sum += waveform[index].coerceIn(-1f, 1f) * weight
        totalWeight += weight
    }

    return if (totalWeight > 0f) (sum / totalWeight).coerceIn(-1f, 1f) else 0f
}

private fun grayscale(value: Float): Color {
    val component = (value.coerceIn(0f, 1f) * 255f).toInt()
    return Color(0xFF000000.toInt() or (component shl 16) or (component shl 8) or component)
}
