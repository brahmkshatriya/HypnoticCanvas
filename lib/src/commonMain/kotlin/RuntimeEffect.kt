package com.mikepenz.hypnoticcanvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader as ComposeShader
import androidx.compose.ui.graphics.TileMode
import com.mikepenz.hypnoticcanvas.shaders.Shader

/**
 * Describes a platform-independent runtime effect
 */
interface RuntimeEffect {

    /** Indicates if the current platform is supported*/
    val supported: Boolean

    /** Defines if the effect is ready to be displayed */
    val ready: Boolean

    /** Sets a float array uniform for this shader */
    fun setFloatUniform(name: String, value1: Float) {}

    /** Sets a float array uniform for this shader */
    fun setFloatUniform(name: String, value1: Float, value2: Float) {}

    /** Sets a float array uniform for this shader */
    fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) {}

    /** Sets a float array uniform for this shader */
    fun setFloatUniform(name: String, values: FloatArray) {}

    /** Sets a shader uniform for this shader */
    fun setShaderUniform(name: String, shader: ComposeShader) {}

    /** Sets a texture uniform for this shader from an existing bitmap. */
    fun setTextureUniform(
        name: String,
        image: ImageBitmap,
        tileModeX: TileMode = TileMode.Clamp,
        tileModeY: TileMode = TileMode.Clamp,
    ) {
        setShaderUniform(name, ImageShader(image, tileModeX, tileModeY))
    }

    /** Updates the uniforms for the shader, on changes of the size or time.*/
    fun update(shader: Shader, time: Float, width: Float, height: Float) {}

    /** Builds an updates ShaderBrush*/
    fun build(): Brush
}

internal expect fun buildEffect(shader: Shader): RuntimeEffect