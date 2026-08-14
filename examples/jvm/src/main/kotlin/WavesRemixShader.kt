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

internal object WavesRemixShader : Shader {
    private var audioTexture = buildAudioTexture(0f, FloatArray(0))
    private var audioShader = ImageShader(audioTexture, TileMode.Clamp, TileMode.Clamp)

    var musicLevelProvider: () -> Float = { 0f }
    var waveformProvider: () -> FloatArray = { FloatArray(0) }

    override val name: String = "Waves Remix"
    override val authorName: String = "ADOB"
    override val authorUrl: String = "https://www.shadertoy.com/view/4ljGD1"
    override val credit: String = "https://www.shadertoy.com/view/4ljGD1"
    override val license: String = "Original Shadertoy license"
    override val licenseUrl: String = "https://www.shadertoy.com/terms"
    override val speedModifier: Float = 0.8f

    override val sksl: String = """
uniform vec3 iResolution;
uniform float iTime;
uniform shader iChannel0;

float squared(float value) {
    return value * value;
}

float getAmp(float frequency) {
    return iChannel0.eval(vec2(frequency / 512.0 * 511.0, 0.0)).x;
}

float getWeight(float f) {
    return (+ getAmp(f-2.0) + getAmp(f-1.0) + getAmp(f+2.0) + getAmp(f+1.0) + getAmp(f)) / 5.0; }

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{    
	vec2 uvTrue = fragCoord.xy / iResolution.xy;
    vec2 uv = -1.0 + 2.0 * uvTrue;
    
	float lineIntensity;
    float glowWidth;
    vec3 color = vec3(0.0);
    
	for(float i = 0.0; i < 5.0; i++) {
        
        uv.y += (0.2 * sin(uv.x + i/7.0 - iTime * 0.6));
        float Y = uv.y + getWeight(squared(i) * 20.0) *
            (iChannel0.eval(vec2(uvTrue.x * 511.0, 1.0)).x - 0.5);
        lineIntensity = 0.4 + squared(1.6 * abs(mod(uvTrue.x + i / 1.3 + iTime,2.0) - 1.0));
		glowWidth = abs(lineIntensity / (150.0 * Y));
		color += vec3(glowWidth * (2.0 + sin(iTime * 0.13)),
                      glowWidth * (2.0 - sin(iTime * 0.23)),
                      glowWidth * (2.0 - cos(iTime * 0.19)));
	}	
	
	fragColor = vec4(color, 1.0);
}

vec4 main(vec2 fragCoord) {
    vec4 fragColor;
    mainImage(fragColor, fragCoord);
    return fragColor;
}
    """.trimIndent()

    override fun applyUniforms(runtimeEffect: RuntimeEffect, time: Float, width: Float, height: Float) {
        super.applyUniforms(runtimeEffect, time, width, height)
        audioTexture = buildAudioTexture(
            level = musicLevelProvider(),
            waveform = waveformProvider(),
        )

        runtimeEffect.setFloatUniform("iResolution", width, height, width / height)
        runtimeEffect.setFloatUniform("iTime", time)
        runtimeEffect.setTextureUniform("iChannel0", audioTexture)
    }
}

private fun buildAudioTexture(level: Float, waveform: FloatArray): ImageBitmap {
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
        val fft = syntheticSpectrum(normalizedX, level)
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

private fun syntheticSpectrum(x: Float, level: Float): Float {
    val bass = (1f - x).coerceIn(0f, 1f)
    val subBass = if (x < 0.16f) {
        1f
    } else {
        kotlin.math.exp(-(x - 0.16f) * 12f)
    }
    val bassShelf = bass * bass * bass
    return (0.025f + level * (0.58f * subBass + 0.26f * bassShelf)).coerceIn(0f, 0.82f)
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
