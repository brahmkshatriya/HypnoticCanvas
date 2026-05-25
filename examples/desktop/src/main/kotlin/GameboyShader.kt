import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import com.mikepenz.hypnoticcanvas.RuntimeEffect
import com.mikepenz.hypnoticcanvas.shaders.Shader

internal object GameboyShader : Shader {
    private var audioTexture = buildGameboyAudioTexture(FloatArray(0), FloatArray(0))
    private var audioShader = ImageShader(audioTexture, TileMode.Clamp, TileMode.Clamp)

    var spectrumProvider: () -> FloatArray = { FloatArray(0) }
    var waveformProvider: () -> FloatArray = { FloatArray(0) }

    override val name: String = "Gameboy"
    override val authorName: String = "iq"
    override val authorUrl: String = "https://www.shadertoy.com/user/iq"
    override val credit: String = "https://www.shadertoy.com/view/XdlGzr"
    override val license: String = "Original Shadertoy license"
    override val licenseUrl: String = "https://www.shadertoy.com/terms"
    override val speedModifier: Float = 0.8f

    override val sksl: String = """
uniform vec3 uResolution;
uniform float uTime;
uniform shader uChannel0;

vec4 sampleChannel0(vec2 uv) {
    float row = mix(0.5, 1.5, step(0.5, uv.y));
    return uChannel0.eval(vec2(clamp(uv.x, 0.0, 1.0) * 511.0, row));
}

float text(vec2 uv) {
    uv.x += 0.2 * floor(10.0 * (0.5 + 0.5 * sin(uTime))) / 10.0;

    float x = floor(uv.x * 100.0) - 23.0;
    float y = floor(uv.y * 100.0) - 82.0;

    if (y < 0.0 || y > 5.0) return 0.0;
    if (x < 0.0 || x > 70.0) return 0.0;

    float v = 0.0;

    if (x > 63.5) {
        v = 12288.0;
        if (y > 2.5) v = 30720.0;
        if (y > 3.5) v = 52224.0;
    } else if (x > 47.5) {
        v = 12408.0;
        if (y > 0.5) v = 12492.0;
        if (y > 4.5) v = 64632.0;
    } else if (x > 31.5) {
        v = 64716.0;
        if (y > 0.5) v = 49360.0;
        if (y > 1.5) v = 49400.0;
        if (y > 2.5) v = 63692.0;
        if (y > 3.5) v = 49356.0;
        if (y > 4.5) v = 64760.0;
    } else if (x > 15.5) {
        v = 40184.0;
        if (y > 0.5) v = 40092.0;
        if (y > 2.5) v = 64668.0;
        if (y > 3.5) v = 40092.0;
        if (y > 4.5) v = 28920.0;
    } else {
        v = 30860.0;
        if (y > 0.5) v = 40076.0;
        if (y > 1.5) v = 7308.0;
        if (y > 2.5) v = 30972.0;
        if (y > 3.5) v = 49292.0;
        if (y > 4.5) v = 30860.0;
    }

    return floor(mod(v / pow(2.0, 15.0 - mod(x, 16.0)), 2.0));
}

vec4 main(vec2 fragCoord) {
    vec2 uv = vec2(fragCoord.x, uResolution.y - fragCoord.y) / uResolution.xy;
    vec2 uvo = uv;
    vec2 res = floor(60.0 * vec2(1.0, uResolution.y / uResolution.x));
    vec3 col = vec3(131.0, 145.0, 0.0);

    if (uv.x > 0.03 && uv.x < 0.97) {
        uv.x = clamp((uv.x - 0.03) / 0.94, 0.0, 1.0);
        vec2 iuv = floor(uv * res) / res;
        float f = 1.0 - abs(-1.0 + 2.0 * fract(uv.x * res.x));
        float g = 1.0 - abs(-1.0 + 2.0 * fract(uv.y * res.y));

        float fft = sampleChannel0(vec2(iuv.x, 0.25)).x;
        fft = 0.8 * fft * fft;
        if (iuv.y < fft) {
            if (f > 0.1 && g > 0.1) col = vec3(40.0, 44.0, 4.0);
            if (f > 0.5 && g > 0.5) col = vec3(74.0, 82.0, 4.0);
        }

        float wave = sampleChannel0(vec2(iuv.x * 0.5, 0.75)).x;
        if (abs(iuv.y - wave) <= (1.0 / res.y)) {
            col = vec3(185.0, 200.0, 90.0);
        }

        float t = text(uvo);
        col = mix(col, vec3(40.0, 44.0, 4.0), t);
    } else {
        float g = 1.0 - abs(-1.0 + 2.0 * fract(uv.y * res.y * 1.5));
        float f = 1.0 - abs(-1.0 + 2.0 * fract(uv.x * res.x + 0.5 * floor(uv.y * res.y * 1.5)));
        if (g < 0.15 || f < 0.15) col = vec3(40.0, 44.0, 4.0);
    }

    return vec4(col / 255.0, 1.0);
}
    """.trimIndent()

    override fun applyUniforms(runtimeEffect: RuntimeEffect, time: Float, width: Float, height: Float) {
        super.applyUniforms(runtimeEffect, time, width, height)
        audioTexture = buildGameboyAudioTexture(
            spectrum = spectrumProvider(),
            waveform = waveformProvider(),
        )
        audioShader = ImageShader(audioTexture, TileMode.Clamp, TileMode.Clamp)
        runtimeEffect.setShaderUniform("uChannel0", audioShader)
    }
}

private fun buildGameboyAudioTexture(spectrum: FloatArray, waveform: FloatArray): ImageBitmap {
    val width = 512
    val height = 2
    val image = ImageBitmap(width, height)
    val canvas = Canvas(image)
    val paint = Paint().apply {
        isAntiAlias = false
    }

    canvas.drawRect(Rect(0f, 0f, width.toFloat(), height.toFloat()), paint.apply {
        color = Color.Black
    })

    for (x in 0 until width) {
        val normalizedX = x / (width - 1).toFloat()
        val fft = if (spectrum.isNotEmpty()) {
            bassWeightedSpectrum(spectrum, normalizedX)
        } else {
            0f
        }
        val wave = if (waveform.isNotEmpty()) {
            0.5f + smoothWaveformSample(waveform, normalizedX) * 0.34f
        } else {
            0.5f
        }

        paint.color = gameboyGrayscale(fft)
        canvas.drawRect(Rect(x.toFloat(), 0f, x + 1f, 1f), paint)
        paint.color = gameboyGrayscale(wave)
        canvas.drawRect(Rect(x.toFloat(), 1f, x + 1f, 2f), paint)
    }

    return image
}

private fun smoothWaveformSample(waveform: FloatArray, x: Float): Float {
    val position = x * waveform.lastIndex
    val base = position.toInt().coerceIn(0, waveform.lastIndex)
    val fraction = position - base
    var sum = 0f
    var totalWeight = 0f

    for (offset in -2..3) {
        val index = (base + offset).coerceIn(0, waveform.lastIndex)
        val distance = kotlin.math.abs(offset - fraction)
        val weight = (1f - distance / 3.5f).coerceAtLeast(0f)
        sum += waveform[index].coerceIn(-1f, 1f) * weight
        totalWeight += weight
    }

    return if (totalWeight > 0f) (sum / totalWeight).coerceIn(-1f, 1f) else 0f
}

private fun bassWeightedSpectrum(spectrum: FloatArray, x: Float): Float {
    val curvedX = x * x * x
    val center = (curvedX * spectrum.lastIndex).toInt().coerceIn(0, spectrum.lastIndex)
    val radius = 3
    var sum = 0f
    var totalWeight = 0f

    for (offset in -radius..radius) {
        val index = (center + offset).coerceIn(0, spectrum.lastIndex)
        val weight = radius + 1 - kotlin.math.abs(offset)
        sum += spectrum[index].coerceIn(0f, 1f) * weight
        totalWeight += weight
    }

    val smoothed = if (totalWeight > 0f) sum / totalWeight else 0f
    val lowBias = kotlin.math.exp(-x * 2.2f)
    return (smoothed * (0.35f + 0.95f * lowBias)).coerceIn(0f, 1f)
}

private fun gameboyGrayscale(value: Float): Color {
    val component = (value.coerceIn(0f, 1f) * 255f).toInt()
    return Color(0xFF000000.toInt() or (component shl 16) or (component shl 8) or component)
}
