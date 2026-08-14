import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

private const val AUDIO_RESOURCE_PATH = "/audio/demo-audio.wav"
private const val ENERGY_WINDOW_FRAMES = 1_024
private const val SPECTRUM_WINDOW_FRAMES = 1_024
private const val SPECTRUM_BINS = 512
private const val WAVEFORM_WINDOW_FRAMES = 2_048

internal class DesktopMusicPlayer private constructor(
    private val clip: Clip,
    private val energyEnvelope: FloatArray,
    private val monoSamples: FloatArray,
    private val spectrumFrames: Array<FloatArray>,
    private val frameCount: Int,
) : AutoCloseable {
    fun play() {
        if (!clip.isRunning) {
            if (clip.framePosition >= clip.frameLength - 1) {
                clip.framePosition = 0
            }
            clip.start()
        }
    }

    fun pause() {
        if (clip.isRunning) {
            clip.stop()
        }
    }

    fun togglePlayback(): Boolean {
        return if (clip.isRunning) {
            pause()
            false
        } else {
            play()
            true
        }
    }

    fun isPlaying(): Boolean = clip.isRunning

    fun level(): Float {
        if (!clip.isRunning || energyEnvelope.isEmpty() || frameCount <= 0) return 0f
        val frame = clip.framePosition.coerceAtLeast(0) % frameCount
        val index = (frame / ENERGY_WINDOW_FRAMES).coerceIn(0, energyEnvelope.lastIndex)
        return energyEnvelope[index]
    }

    fun waveform(sampleCount: Int): FloatArray {
        if (!clip.isRunning || monoSamples.isEmpty() || sampleCount <= 0) return FloatArray(sampleCount)
        val output = FloatArray(sampleCount)
        val startFrame = clip.framePosition.coerceAtLeast(0) % frameCount
        val stride = (WAVEFORM_WINDOW_FRAMES / sampleCount).coerceAtLeast(1)

        for (index in output.indices) {
            val sampleIndex = (startFrame + index * stride) % monoSamples.size
            output[index] = monoSamples[sampleIndex]
        }

        return output
    }

    fun spectrum(): FloatArray {
        if (!clip.isRunning || spectrumFrames.isEmpty() || frameCount <= 0) return FloatArray(SPECTRUM_BINS)
        val frame = clip.framePosition.coerceAtLeast(0) % frameCount
        val index = (frame / SPECTRUM_WINDOW_FRAMES).coerceIn(0, spectrumFrames.lastIndex)
        val nextIndex = (index + 1).coerceAtMost(spectrumFrames.lastIndex)
        val fraction = (frame % SPECTRUM_WINDOW_FRAMES) / SPECTRUM_WINDOW_FRAMES.toFloat()
        val current = spectrumFrames[index]
        val next = spectrumFrames[nextIndex]
        return FloatArray(SPECTRUM_BINS) { bin ->
            current[bin] * (1f - fraction) + next[bin] * fraction
        }
    }

    override fun close() {
        clip.stop()
        clip.close()
    }

    companion object {
        fun create(): DesktopMusicPlayer? = try {
            val resourceBytes = DesktopMusicPlayer::class.java.getResourceAsStream(AUDIO_RESOURCE_PATH)
                ?.use { it.readBytes() }
                ?: return null

            val analysisStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(resourceBytes))
            val playbackStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(resourceBytes))
            val format = analysisStream.format
            val pcmBytes = analysisStream.use { it.readBytes() }
            val energyEnvelope = buildEnergyEnvelope(pcmBytes, format)
            val monoSamples = buildMonoSamples(pcmBytes, format)
            val spectrumFrames = buildSpectrumFrames(monoSamples)
            val frameCount = pcmBytes.size / format.frameSize.coerceAtLeast(1)

            val clipInfo = DataLine.Info(Clip::class.java, format)
            val clip = AudioSystem.getLine(clipInfo) as Clip
            playbackStream.use { clip.open(it) }

            DesktopMusicPlayer(clip, energyEnvelope, monoSamples, spectrumFrames, frameCount)
        } catch (_: Exception) {
            null
        }
    }
}

private fun buildMonoSamples(pcmBytes: ByteArray, format: AudioFormat): FloatArray {
    val frameSize = format.frameSize.coerceAtLeast(1)
    val channels = format.channels.coerceAtLeast(1)
    val frameCount = pcmBytes.size / frameSize
    if (frameCount <= 0) return FloatArray(0)

    val samples = FloatArray(frameCount)
    var frameIndex = 0
    var byteOffset = 0
    while (frameIndex < frameCount && byteOffset + frameSize <= pcmBytes.size) {
        var sum = 0f
        var channelIndex = 0
        while (channelIndex < channels) {
            val sampleOffset = byteOffset + channelIndex * 2
            if (sampleOffset + 1 >= pcmBytes.size) break
            sum += readPcm16Sample(pcmBytes, sampleOffset, format.isBigEndian) / 32768f
            channelIndex++
        }

        samples[frameIndex] = if (channelIndex > 0) {
            (sum / channelIndex).coerceIn(-1f, 1f)
        } else {
            0f
        }

        frameIndex++
        byteOffset += frameSize
    }

    return samples
}

private fun buildSpectrumFrames(samples: FloatArray): Array<FloatArray> {
    if (samples.isEmpty()) return emptyArray()

    val blockCount = ((samples.size + SPECTRUM_WINDOW_FRAMES - 1) / SPECTRUM_WINDOW_FRAMES).coerceAtLeast(1)
    val frames = Array(blockCount) { FloatArray(SPECTRUM_BINS) }
    val window = FloatArray(SPECTRUM_WINDOW_FRAMES) { index ->
        (0.5 - 0.5 * cos(2.0 * PI * index / (SPECTRUM_WINDOW_FRAMES - 1))).toFloat()
    }
    var globalPeak = 0f

    for (block in 0 until blockCount) {
        val real = FloatArray(SPECTRUM_WINDOW_FRAMES)
        val imag = FloatArray(SPECTRUM_WINDOW_FRAMES)
        val start = block * SPECTRUM_WINDOW_FRAMES

        for (index in 0 until SPECTRUM_WINDOW_FRAMES) {
            val sampleIndex = start + index
            real[index] = if (sampleIndex < samples.size) samples[sampleIndex] * window[index] else 0f
        }

        fft(real, imag)

        for (bin in 0 until SPECTRUM_BINS) {
            val magnitude = sqrt(real[bin] * real[bin] + imag[bin] * imag[bin])
            frames[block][bin] = magnitude
            if (magnitude > globalPeak) {
                globalPeak = magnitude
            }
        }
    }

    if (globalPeak > 0f) {
        val logPeak = ln(1.0 + globalPeak * 18.0).toFloat()
        for (frame in frames) {
            for (bin in frame.indices) {
                frame[bin] = (ln(1.0 + frame[bin] * 18.0).toFloat() / logPeak).coerceIn(0f, 1f)
            }
            smoothSpectrumBins(frame)
        }

        for (frameIndex in 1 until frames.size) {
            val previous = frames[frameIndex - 1]
            val current = frames[frameIndex]
            for (bin in current.indices) {
                current[bin] = previous[bin] * 0.58f + current[bin] * 0.42f
            }
        }
    }

    return frames
}

private fun smoothSpectrumBins(frame: FloatArray) {
    if (frame.size < 5) return
    val copy = frame.copyOf()
    for (index in frame.indices) {
        val left2 = copy[(index - 2).coerceAtLeast(0)]
        val left1 = copy[(index - 1).coerceAtLeast(0)]
        val center = copy[index]
        val right1 = copy[(index + 1).coerceAtMost(copy.lastIndex)]
        val right2 = copy[(index + 2).coerceAtMost(copy.lastIndex)]
        frame[index] = (left2 + left1 * 2f + center * 3f + right1 * 2f + right2) / 9f
    }
}

private fun fft(real: FloatArray, imag: FloatArray) {
    val n = real.size
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            val realTemp = real[i]
            real[i] = real[j]
            real[j] = realTemp
            val imagTemp = imag[i]
            imag[i] = imag[j]
            imag[j] = imagTemp
        }
    }

    var length = 2
    while (length <= n) {
        val angle = -2.0 * PI / length
        val wLengthReal = cos(angle).toFloat()
        val wLengthImag = sin(angle).toFloat()
        var i = 0
        while (i < n) {
            var wReal = 1f
            var wImag = 0f
            for (k in 0 until length / 2) {
                val even = i + k
                val odd = even + length / 2
                val oddReal = real[odd] * wReal - imag[odd] * wImag
                val oddImag = real[odd] * wImag + imag[odd] * wReal

                real[odd] = real[even] - oddReal
                imag[odd] = imag[even] - oddImag
                real[even] += oddReal
                imag[even] += oddImag

                val nextReal = wReal * wLengthReal - wImag * wLengthImag
                wImag = wReal * wLengthImag + wImag * wLengthReal
                wReal = nextReal
            }
            i += length
        }
        length = length shl 1
    }
}

private fun buildEnergyEnvelope(pcmBytes: ByteArray, format: AudioFormat): FloatArray {
    val frameSize = format.frameSize.coerceAtLeast(1)
    val channels = format.channels.coerceAtLeast(1)
    val frameCount = pcmBytes.size / frameSize
    if (frameCount <= 0) return floatArrayOf(0f)

    val blockCount = ((frameCount + ENERGY_WINDOW_FRAMES - 1) / ENERGY_WINDOW_FRAMES).coerceAtLeast(1)
    val blockEnergy = DoubleArray(blockCount)
    val blockCounts = IntArray(blockCount)

    var frameIndex = 0
    var byteOffset = 0
    while (frameIndex < frameCount && byteOffset + frameSize <= pcmBytes.size) {
        var sum = 0.0
        var channelIndex = 0
        while (channelIndex < channels) {
            val sampleOffset = byteOffset + channelIndex * 2
            if (sampleOffset + 1 >= pcmBytes.size) break
            val sample = readPcm16Sample(pcmBytes, sampleOffset, format.isBigEndian)
            val normalized = sample / 32768.0
            sum += normalized * normalized
            channelIndex++
        }

        if (channelIndex > 0) {
            val blockIndex = frameIndex / ENERGY_WINDOW_FRAMES
            blockEnergy[blockIndex] += sum / channelIndex
            blockCounts[blockIndex]++
        }

        frameIndex++
        byteOffset += frameSize
    }

    val envelope = FloatArray(blockCount)
    var peak = 0.0
    for (index in envelope.indices) {
        val count = blockCounts[index]
        val rms = if (count > 0) sqrt(blockEnergy[index] / count) else 0.0
        envelope[index] = rms.toFloat()
        if (rms > peak) {
            peak = rms
        }
    }

    if (peak > 0.0) {
        for (index in envelope.indices) {
            envelope[index] = (envelope[index] / peak).toFloat().coerceIn(0f, 1f)
        }
    }

    return envelope
}

private fun readPcm16Sample(bytes: ByteArray, offset: Int, bigEndian: Boolean): Int {
    val first = bytes[offset].toInt()
    val second = bytes[offset + 1].toInt()
    val combined = if (bigEndian) {
        (first shl 8) or (second and 0xFF)
    } else {
        (second shl 8) or (first and 0xFF)
    }
    return combined.toShort().toInt()
}
