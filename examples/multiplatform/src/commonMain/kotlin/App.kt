package com.mikepenz.hypnoticcanvas.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.GlossyGradients
import com.mikepenz.hypnoticcanvas.shaders.MeshGradient
import com.mikepenz.hypnoticcanvas.shaders.MesmerizingLens
import com.mikepenz.hypnoticcanvas.shaders.Shader

private data class ShaderShowcase(
    val title: String,
    val description: String,
    val shader: Shader,
    val palette: List<Color>,
) {
    val averageColor: Color = palette.averageColor()
}

private val shaderShowcases = listOf(
    ShaderShowcase(
        title = "Mesmerizing lens",
        description = "A luminous, refracted lens drifting through electric cyan and violet.",
        shader = MesmerizingLens,
        palette = listOf(
            Color(0xFF11162E),
            Color(0xFF315CFF),
            Color(0xFF39DDF5),
            Color(0xFFC95BFF),
        ),
    ),
    ShaderShowcase(
        title = "Glossy gradients",
        description = "Liquid color bands with glossy highlights and a soft chromatic pulse.",
        shader = GlossyGradients,
        palette = listOf(
            Color(0xFF231557),
            Color(0xFF7B5CFA),
            Color(0xFFE968A2),
            Color(0xFF52D9D0),
        ),
    ),
    meshShowcase(
        title = "Aurora mesh",
        description = "Cool aurora ribbons floating above a deep midnight field.",
        colors = listOf(
            Color(0xFF4AE3B5),
            Color(0xFF70A5FF),
            Color(0xFFB96BFF),
            Color(0xFF07152F),
        ),
    ),
    meshShowcase(
        title = "Solar bloom",
        description = "A warm mesh of amber, coral and rose inspired by late sunlight.",
        colors = listOf(
            Color(0xFFFFC857),
            Color(0xFFFF7A59),
            Color(0xFFDF4F8F),
            Color(0xFF3A163E),
        ),
    ),
    meshShowcase(
        title = "Tidal glass",
        description = "Translucent ocean colors rolling through a dark teal canvas.",
        colors = listOf(
            Color(0xFF60E1E0),
            Color(0xFF3A86FF),
            Color(0xFF7FE7C4),
            Color(0xFF092C35),
        ),
    ),
)

@Composable
fun HypnoticCanvasExample(platform: String) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val selected = shaderShowcases.getOrNull(selectedIndex)
    val seedColor = selected?.averageColor ?: shaderShowcases.map { it.averageColor }.averageColor()

    DynamicMaterialTheme(
        seedColor = seedColor,
        isDark = true,
        style = PaletteStyle.Vibrant,
        animate = true,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            if (selected == null) {
                ShaderGallery(
                    platform = platform,
                    onShaderSelected = { selectedIndex = it },
                )
            } else {
                ShaderDetail(
                    showcase = selected,
                    platform = platform,
                    onBack = { selectedIndex = -1 },
                )
            }
        }
    }
}

@Composable
private fun ShaderGallery(
    platform: String,
    onShaderSelected: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Shader gallery",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Choose a shader to open an interactive Material 3 showcase. " +
                        "Each theme is generated by Material Kolor from the shader palette average.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                AssistChip(
                    onClick = {},
                    label = { Text(platform) },
                )
            }
        }

        itemsIndexed(shaderShowcases) { index, showcase ->
            DynamicMaterialTheme(
                seedColor = showcase.averageColor,
                isDark = true,
                style = PaletteStyle.Vibrant,
            ) {
                ShaderGalleryCard(
                    showcase = showcase,
                    onClick = { onShaderSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun ShaderGalleryCard(
    showcase: ShaderShowcase,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .shaderBackground(
                    shader = showcase.shader,
                    fallback = { Brush.horizontalGradient(showcase.palette) },
                ),
        ) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            ) {
                Text(
                    text = "Open  →",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = showcase.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "by ${showcase.shader.authorName}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                PaletteSwatches(showcase.palette)
            }
            Text(
                text = showcase.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ShaderDetail(
    showcase: ShaderShowcase,
    platform: String,
    onBack: () -> Unit,
) {
    var isPlaying by remember(showcase) { mutableStateOf(true) }
    var useHighContrast by remember(showcase) { mutableStateOf(false) }
    var speed by remember(showcase) { mutableFloatStateOf(1f) }

    DynamicMaterialTheme(
        seedColor = showcase.averageColor,
        isDark = true,
        style = if (useHighContrast) PaletteStyle.Content else PaletteStyle.Vibrant,
        contrastLevel = if (useHighContrast) 0.75 else 0.0,
        animate = true,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shaderBackground(
                    shader = showcase.shader,
                    speed = if (isPlaying) speed else 0f,
                    fallback = { Brush.horizontalGradient(showcase.palette) },
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.30f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.50f),
                            )
                        )
                    ),
            )

            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = onBack) {
                    Text("←  All shaders")
                }
                AssistChip(
                    onClick = {},
                    label = { Text(platform) },
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = showcase.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${showcase.shader.authorName} · ${showcase.shader.license}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        PaletteSwatches(showcase.palette)
                    }

                    Text(
                        text = showcase.description,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Material Kolor seed · average of ${showcase.palette.size} shader colors",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = { isPlaying = !isPlaying }) {
                            Text(if (isPlaying) "Pause shader" else "Play shader")
                        }
                        OutlinedButton(onClick = { speed = 1f }) {
                            Text("Reset speed")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("High contrast")
                        Switch(
                            checked = useHighContrast,
                            onCheckedChange = { useHighContrast = it },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Speed",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = speed,
                            onValueChange = { speed = it },
                            modifier = Modifier.weight(1f),
                            valueRange = 0.2f..2f,
                        )
                        Text(
                            text = "${(speed * 10).toInt() / 10f}×",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatches(colors: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = color, shape = CircleShape),
            )
        }
    }
}

private fun meshShowcase(
    title: String,
    description: String,
    colors: List<Color>,
): ShaderShowcase = ShaderShowcase(
    title = title,
    description = description,
    shader = MeshGradient(colors.toTypedArray(), speed = 1.1f, scale = 2.2f),
    palette = colors,
)

private fun List<Color>.averageColor(): Color {
    if (isEmpty()) return Color.Unspecified
    val count = size.toFloat()
    return Color(
        red = sumOf { it.red.toDouble() }.toFloat() / count,
        green = sumOf { it.green.toDouble() }.toFloat() / count,
        blue = sumOf { it.blue.toDouble() }.toFloat() / count,
        alpha = sumOf { it.alpha.toDouble() }.toFloat() / count,
    )
}
